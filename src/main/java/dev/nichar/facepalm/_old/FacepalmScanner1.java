package dev.nichar.facepalm._old;

import lombok.*;
import java.io.IOException;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;
import java.util.stream.Stream;

public class FacepalmScanner1 {

    // =====================================================
    // Configuration & Setup
    // =====================================================
    private static final int THREADS = Runtime.getRuntime().availableProcessors();
    private static final int ERROR_THRESHOLD = 80;
    private static final int WARNING_THRESHOLD = 40;
    private static final long MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024;

    // Key: Directory path, Value: List of matchers defined in that directory's .gitignore
    private static final TreeMap<Path, List<PathMatcher>> GITIGNORE_REGISTRY = new TreeMap<>();
    private static final List<PathMatcher> GITIGNORE_MATCHERS = new ArrayList<>();
    private static final Set<String> IGNORED_PATHS_CACHE = ConcurrentHashMap.newKeySet();

    private static final Set<String> SKIP_DIRS = Set.of(
        ".git", ".idea", "target", "build", "node_modules", "temp", "dist", "out", "cid", "vendor"
    );

    private static final ConcurrentLinkedQueue<Finding> RESULTS = new ConcurrentLinkedQueue<>();

    // Standard Pipeline (runs per file)
    private static final List<Analyzer> PIPELINE = List.of(
        new SecretDiscoveryAnalyzer(),       // 1. Find
        new PlaceholderSuppressorAnalyzer(), // 2. Filter {{vars}}
        new PublicExposureAnalyzer(),        // 3. .gitignore Check
        new FileExtensionAnalyzer(),         // 4. .env vs .md logic
        new LocationAnalyzer(),              // 5. Path logic
        new SurroundingContextAnalyzer(),    // 6. Keywords
        new EntropyAnalyzer(),               // 7. Randomness
        new CompositeScoringAnalyzer()       // 8. Multiple secrets per file
    );

    public static void main(String[] args) throws Exception {
        final var now = System.currentTimeMillis();

        Path root = Paths.get(args.length > 0 ? args[0] : ".").toAbsolutePath().normalize();

        // 1. Setup
        loadAllGitIgnores(root);
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);

        // 2. Scan
        try (Stream<Path> paths = Files.walk(root)) {
            paths.filter(Files::isRegularFile)
                .filter(FacepalmScanner1::shouldScan) // Now uses the OS-agnostic path segment check
                .forEach(path -> executor.submit(() -> scanFile(path)));
        }

        // 3. Thread-Safe Shutdown
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.MINUTES)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        // 4. Thread-Safe Snapshot & Post-Processing
        List<Finding> finalResults = new ArrayList<>(RESULTS);
        //for (PostProcessor pp : POST_PROCESSORS) {
        //    pp.process(finalResults);
        //}

        // 5. Final Report
        printResults(finalResults);

        System.out.println(System.currentTimeMillis() - now);
    }

    private static boolean shouldScan(Path path) {
        // Native, OS-agnostic path segment checking. Matches exactly the directory name.
        for (Path element : path) {
            if (SKIP_DIRS.contains(element.toString())) return false;
        }

        String name = path.getFileName().toString().toLowerCase();
        if (name.matches(".*\\.(png|jpg|jpeg|gif|pdf|zip|jar|class|tar|gz|exe|dll)$")) return false;

        try { return Files.size(path) <= MAX_FILE_SIZE_BYTES; }
        catch (IOException e) { return false; }
    }

    private static void scanFile(Path path) {
        try {
            String content = Files.readString(path);
            List<String> lines = Arrays.asList(content.split("\\R"));
            FileContext context = new FileContext(path, content, lines);
            List<Finding> fileFindings = new ArrayList<>();

            for (Analyzer analyzer : PIPELINE) {
                analyzer.analyze(context, fileFindings);
            }
            RESULTS.addAll(fileFindings);
        } catch (IOException ignored) {}
    }

    // =====================================================
    // Scoring Strategy Enum
    // =====================================================

    public enum ScoringStrategy {
        /** (Risk + Conf) / 2 - Standard average. */
        AVERAGE {
            public double calculate(int r, int c) { return (r + c) / 2.0; }
        },
        /** Geometric Mean - Sensitive to zero values. */
        GEOMETRIC {
            public double calculate(int r, int c) { return Math.sqrt(r * (double)c); }
        },
        /** Root Mean Square - Favors the higher of the two numbers. */
        RMS {
            public double calculate(int r, int c) { return Math.sqrt((r*r + c*c) / 2.0); }
        },
        /** DEFAULT: Quadratic Risk Bias - Squares the risk to make high-impact leaks explode. */
        WEIGHTED_QUADRATIC {
            public double calculate(int r, int c) {
                double rScaled = (r * r) / 100.0;
                return (rScaled * 0.8) + (c * 0.2);
            }
        },
        /** The Gatekeeper - Critical risk triggers error immediately. */
        GATEKEEPER {
            public double calculate(int r, int c) {
                if (r >= 90) return Math.max(r, c);
                return (r * 0.5) + (c * 0.5);
            }
        };

        public abstract double calculate(int risk, int confidence);
    }

    public enum Severity { INFO, WARNING, ERROR }

    // =====================================================
    // Models (Lombok)
    // =====================================================
    @Value
    public static class SecretPattern {
        String name;
        Pattern pattern;
        int baseRisk;
        int baseConfidence;
        boolean isMultiLine;
    }

    @Value
    public static class FileContext {
        Path path;
        String fullContent;
        List<String> lines;

        public String getLineOrEmpty(int index) {
            return (index >= 0 && index < lines.size()) ? lines.get(index) : "";
        }
    }

    @Data
    @Builder
    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    public static class Finding {
        private final String patternName;

        @EqualsAndHashCode.Include
        private final String deduplicationHash;

        private final String secretValue;
        private final int lineNumber;
        private final FileContext context;
        private final String contextSnippet; // The surrounding code for the report

        private int riskScore;
        private int confidenceScore;

        @Builder.Default
        private final List<String> scoreHistory = new ArrayList<>();

        public void log(String rule, int rDelta, int cDelta) {
            if (rDelta == 0 && cDelta == 0) return; // Don't log empty changes

            this.riskScore = Math.max(0, Math.min(100, this.riskScore + rDelta));
            this.confidenceScore = Math.max(0, Math.min(100, this.confidenceScore + cDelta));

            String rSign = rDelta >= 0 ? "+" : "";
            String cSign = cDelta >= 0 ? "+" : "";
            scoreHistory.add(String.format("%s (Risk: %s%d, Conf: %s%d)", rule, rSign, rDelta, cSign, cDelta));
        }

        public Severity getSeverity() {
            double score = ScoringStrategy.WEIGHTED_QUADRATIC.calculate(riskScore, confidenceScore);
            if (score >= ERROR_THRESHOLD) return Severity.ERROR;
            if (score >= WARNING_THRESHOLD) return Severity.WARNING;
            return Severity.INFO;
        }

        public double getNumericScore() {
            return ScoringStrategy.WEIGHTED_QUADRATIC.calculate(riskScore, confidenceScore);
        }

        public String getMaskedSecret() {
            if (secretValue.length() <= 8) return "****";
            return secretValue.substring(0, 4) + "..." + secretValue.substring(secretValue.length() - 4);
        }
    }

    // =====================================================
    // Registry (The Exhaustive List with Shields)
    // =====================================================
    public static class Registry {
        public static final List<SecretPattern> PATTERNS = List.of(
            // --- 1. CLOUD & INFRA ---
            new SecretPattern("AWS Access Key", Pattern.compile("\\bAKIA[0-9A-Z]{16}\\b"), 70, 95, false),
            new SecretPattern("AWS Secret Key", Pattern.compile("(?i)aws_(?:secret|access|key).{0,10}[=:]\\s*([^\\s]{40})"), 85, 80, false),
            new SecretPattern("Google API Key", Pattern.compile("\\bAIza[0-9A-Za-z\\-_]{35}\\b"), 60, 90, false),
            new SecretPattern("Firebase Key", Pattern.compile("AAAA[A-Za-z0-9_\\-]{7}:[A-Za-z0-9_\\-]{140,}"), 75, 95, false),
            new SecretPattern("Heroku API Key", Pattern.compile("(?i)heroku.*[=:]\\s*([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})"), 70, 85, false),
            new SecretPattern("Azure OpenAI Key", Pattern.compile("(?i)(?:azure|openai).*?key.{0,10}[=:]\\s*([a-zA-Z0-9]{32})"), 80, 85, false),

            // --- 2. DATABASES ---
            new SecretPattern("MongoDB Connection", Pattern.compile("(?i)mongodb(?:\\+srv)?://([^/\\s]+:[^/\\s]+)@"), 90, 95, false),
            new SecretPattern("PostgreSQL Connection", Pattern.compile("(?i)postgres(?:ql)?://([^/\\s]+:[^/\\s]+)@"), 90, 95, false),
            new SecretPattern("MySQL Connection", Pattern.compile("(?i)mysql://([^/\\s]+:[^/\\s]+)@"), 90, 95, false),
            new SecretPattern("Redis Password", Pattern.compile("(?i)redis://(?:[^/\\s]*):([^/\\s]+)@"), 80, 90, false),
            new SecretPattern("SQL Server Password", Pattern.compile("(?i)(?:sql[_-]?server|mssql).*password\\s*[=:]\\s*([^\\s;]{8,})"), 80, 85, false),

            // --- 3. AI SERVICES ---
            new SecretPattern("OpenAI Project Key", Pattern.compile("sk-proj-[A-Za-z0-9]{48,}"), 90, 100, false),
            new SecretPattern("OpenAI API Key", Pattern.compile("\\bsk-[A-Za-z0-9]{48}\\b"), 80, 90, false),
            new SecretPattern("Anthropic API Key", Pattern.compile("sk-ant-api03-[A-Za-z0-9_\\-]{93,95}"), 85, 100, false),
            new SecretPattern("Google Gemini API Key", Pattern.compile("\\bAIza[0-9A-Za-z\\-_]{35}\\b"), 60, 85, false),
            new SecretPattern("HuggingFace Token", Pattern.compile("hf_[A-Za-z0-9]{34}"), 75, 95, false),
            new SecretPattern("Replicate API Token", Pattern.compile("r8_[A-Za-z0-9]{30,40}"), 75, 95, false),
            new SecretPattern("Cohere API Key", Pattern.compile("\\b[cC]ohere[_-]?api[_-]?key.{0,10}[=:]\\s*([A-Za-z0-9]{40})"), 75, 90, false),
            new SecretPattern("Mistral API Key", Pattern.compile("\\b[mM]istral[_-]?api[_-]?key.{0,10}[=:]\\s*([A-Za-z0-9]{32})"), 75, 90, false),
            new SecretPattern("Groq API Key", Pattern.compile("gsk_[A-Za-z0-9]{52}"), 80, 95, false),

            // --- 4. SAAS, PAYMENTS & CI/CD ---
            new SecretPattern("Stripe Live Secret", Pattern.compile("sk_live_[0-9a-zA-Z]{24,}"), 85, 95, false),
            new SecretPattern("Stripe Test Secret", Pattern.compile("sk_test_[0-9a-zA-Z]{24,}"), 30, 95, false),
            new SecretPattern("GitHub Fine-Grained Token", Pattern.compile("github_pat_[0-9A-Za-z_]{82,}"), 85, 100, false),
            new SecretPattern("GitHub Token", Pattern.compile("ghp_[0-9A-Za-z]{36}"), 80, 100, false),
            new SecretPattern("Slack Token", Pattern.compile("xox[baprs]-[0-9a-zA-Z-]{10,48}"), 75, 95, false),
            new SecretPattern("Twilio API Key", Pattern.compile("SK[0-9a-fA-F]{32}"), 70, 90, false),
            new SecretPattern("Mailgun API Key", Pattern.compile("key-[0-9a-zA-Z]{32}"), 70, 90, false),
            new SecretPattern("GitLab Token", Pattern.compile("glpat-[0-9a-zA-Z\\-_]{20}"), 85, 100, false),
            new SecretPattern("Slack Webhook", Pattern.compile("https://hooks\\.slack\\.com/services/T[A-Z0-9]{8}/B[A-Z0-9]{8}/[A-Za-z0-9]{24}"), 80, 100, false),
            new SecretPattern("Discord Webhook", Pattern.compile("https://discord\\.com/api/webhooks/[0-9]{18,19}/[A-Za-z0-9\\-_]{68}"), 70, 100, false),

            // --- 5. SECURITY BLOCKS (Multi-line) ---
            new SecretPattern("Private Key Block", Pattern.compile("(?s)-----BEGIN (?:RSA|EC|OPENSSH|PGP)? PRIVATE KEY-----.*?-----END (?:RSA|EC|OPENSSH|PGP)? PRIVATE KEY-----"), 100, 100, true),
            new SecretPattern("Basic Auth Header", Pattern.compile("(?i)Authorization:\\s*Basic\\s+(?!\\{\\{|\\$\\{)([A-Za-z0-9+/=]{20,})"), 60, 75, false),
            new SecretPattern("JWT Token", Pattern.compile("eyJ[A-Za-z0-9_\\-]{10,}\\.[A-Za-z0-9._\\-]{10,}\\.[A-Za-z0-9._\\-]{10,}"), 30, 40, false),

            // --- 6. PROPERTIES / ENV GENERIC (Safe Normalized Matchers with lookaheads) ---
            new SecretPattern("Generic API Key Assignment", Pattern.compile("(?i)(?:api[_-]?key|secret|token|client[_-]?secret)\\s*[=:]\\s*['\"]?(?!\\{\\{|\\$\\{)([^'\"\\s\\}]{16,})['\"]?"), 65, 70, false),
            new SecretPattern("Generic Password Assignment", Pattern.compile("(?i)(?:password|passwd|pwd)\\s*[=:]\\s*['\"]?(?!\\{\\{|\\$\\{)([^'\"\\s\\}]{8,})['\"]?"), 50, 40, false)
        );
    }

    // =====================================================
    // Analyzers (Chain of Responsibility)
    // =====================================================
    public interface Analyzer {
        void analyze(FileContext context, List<Finding> findings);
    }

    public static class SecretDiscoveryAnalyzer implements Analyzer {
        @Override
        public void analyze(FileContext context, List<Finding> findings) {
            Set<String> localDedup = new HashSet<>();

            for (int i = 0; i < context.getLines().size(); i++) {
                String rawLine = context.getLines().get(i);
                String normalizedLine = rawLine.replace("\"", "").replace("'", "");

                for (SecretPattern sp : Registry.PATTERNS) {
                    if (sp.isMultiLine()) continue;
                    Matcher m = sp.getPattern().matcher(normalizedLine);
                    while (m.find()) {
                        String secretValue = m.groupCount() >= 1 ? m.group(1) : m.group();
                        registerFinding(findings, localDedup, context, sp, secretValue, i + 1, rawLine);
                    }
                }
            }

            for (SecretPattern sp : Registry.PATTERNS) {
                if (!sp.isMultiLine()) continue;
                Matcher m = sp.getPattern().matcher(context.getFullContent());
                while (m.find()) {
                    String secretValue = m.group();
                    int lineNum = getLineNumberFromOffset(context.getFullContent(), m.start());
                    registerFinding(findings, localDedup, context, sp, secretValue, lineNum, "[MULTI-LINE BLOCK]");
                }
            }
        }

        private void registerFinding(List<Finding> findings, Set<String> dedup, FileContext ctx,
                                     SecretPattern sp, String value, int lineNum, String snippet) {
            String hash = hashString(sp.getName() + value + lineNum);
            if (dedup.add(hash)) {
                // Initialize with zero score, then log the initial match to track history
                Finding f = Finding.builder()
                    .patternName(sp.getName())
                    .deduplicationHash(hash)
                    .secretValue(value)
                    .lineNumber(lineNum)
                    .context(ctx)
                    .contextSnippet(snippet.trim())
                    .riskScore(0)
                    .confidenceScore(0)
                    .build();

                f.log("Base Pattern Match : " + sp.getName(), sp.getBaseRisk(), sp.getBaseConfidence());
                findings.add(f);
            }
        }

        private int getLineNumberFromOffset(String content, int offset) {
            return (int) content.substring(0, offset).chars().filter(ch -> ch == '\n').count() + 1;
        }

        private String hashString(String input) {
            try {
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                byte[] hash = md.digest(input.getBytes());
                StringBuilder hexString = new StringBuilder();
                for (byte b : hash) hexString.append(String.format("%02x", b));
                return hexString.toString();
            } catch (NoSuchAlgorithmException e) { return String.valueOf(input.hashCode()); }
        }
    }

    public static class FileExtensionAnalyzer implements Analyzer {
        private static final Set<String> HIGH_RISK_EXTS = Set.of(".env", ".properties", ".yml", ".yaml", ".conf", ".ini");
        private static final Set<String> LOW_RISK_EXTS = Set.of(".md", ".txt", ".csv", ".log", ".example", ".sample");

        @Override
        public void analyze(FileContext context, List<Finding> findings) {
            String fileName = context.getPath().getFileName().toString().toLowerCase();

            for (Finding f : findings) {
                if (HIGH_RISK_EXTS.stream().anyMatch(fileName::endsWith)) {
                    f.log("High Risk Configuration File", 15, 20);
                } else if (LOW_RISK_EXTS.stream().anyMatch(fileName::endsWith)) {
                    f.log("Documentation/Log File", -30, -40);
                }
            }
        }
    }

    public static class CompositeScoringAnalyzer implements Analyzer {
        @Override
        public void analyze(FileContext context, List<Finding> findings) {
            if (findings.size() <= 1) return;

            // Count unique pattern types in this file
            long uniquePatterns = findings.stream().map(Finding::getPatternName).distinct().count();

            for (Finding f : findings) {
                if (findings.size() > 15) {
                    // Massive amount of secrets usually means mock data / seed script
                    f.log("High Volume File (Likely Mock/Seed Data)", -20, -30);
                } else if (uniquePatterns > 1) {
                    // A file with a password AND an API key is highly suspicious
                    f.log("Composite Risk: Multiple distinct secret types in file", 15, 10);
                }
            }
        }
    }

    public static class PublicExposureAnalyzer implements Analyzer {
        @Override
        public void analyze(FileContext context, List<Finding> findings) {
            Path filePath = context.getPath();

            // Find all .gitignores that are parents of this file
            boolean isIgnored = GITIGNORE_REGISTRY.entrySet().stream()
                .filter(entry -> filePath.startsWith(entry.getKey()))
                .flatMap(entry -> entry.getValue().stream())
                .anyMatch(matcher -> matcher.matches(filePath));

            if (isIgnored) {
                for (Finding f : findings) {
                    f.log("Recursive .gitignore Match (Low Exposure)", -40, 0);
                }
            }
        }
    }

    private static void loadAllGitIgnores(Path root) {
        try (Stream<Path> paths = Files.walk(root)) {
            paths.filter(p -> p.getFileName() != null && p.getFileName().toString().equals(".gitignore"))
                .forEach(FacepalmScanner1::parseGitIgnoreFile);
        } catch (IOException e) {
            System.err.println("⚠️ Error walking for .gitignores: " + e.getMessage());
        }
    }

    private static void parseGitIgnoreFile(Path ignoreFile) {
        Path dir = ignoreFile.getParent();
        List<PathMatcher> matchers = new ArrayList<>();

        try (Stream<String> lines = Files.lines(ignoreFile)) {
            lines.map(String::trim)
                .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                .forEach(pattern -> {
                    // Convert gitignore pattern to Glob
                    String glob = pattern;
                    if (pattern.endsWith("/")) glob += "**";
                    if (!pattern.startsWith("**/") && !pattern.startsWith("/")) glob = "**/" + glob;

                    // Create matcher restricted to the directory where the .gitignore lives
                    String fullGlob = "glob:" + dir.toString().replace("\\", "/") + "/" + glob;
                    matchers.add(FileSystems.getDefault().getPathMatcher(fullGlob));
                });
            GITIGNORE_REGISTRY.put(dir, matchers);
        } catch (IOException ignored) {}
    }

    public static class PlaceholderSuppressorAnalyzer implements Analyzer {
        private static final Pattern INTERPOLATION_PATTERN = Pattern.compile(
            ".*?(?:\\$\\{.*\\}|\\{\\{.*\\}|<.*>|%.*%|\\[.*\\]).*"
        );

        private static final Set<String> DUMMY_KEYWORDS = Set.of(
            "dummy", "your_api_key", "insert_here", "placeholder", "place_holder", "replace_me", "changeme", "change_me"
        );

        @Override
        public void analyze(FileContext context, List<Finding> findings) {
            for (Finding f : findings) {
                String val = f.getSecretValue().trim().replaceAll("[,;\"']+$", "");

                if (INTERPOLATION_PATTERN.matcher(val).matches()) {
                    f.log("Interpolation/Placeholder Shield", -50, -100);
                    continue;
                }

                String lowerVal = val.toLowerCase().replace("-", "_").replace(" ", "_");
                if (DUMMY_KEYWORDS.stream().anyMatch(lowerVal::contains)) {
                    f.log("Dummy Keyword Penalty", 0, -80);
                }

                if (val.equalsIgnoreCase(f.getPatternName().split(" ")[0])) {
                    f.log("Value Matches Key Name", 0, -30);
                }
            }
        }
    }

    public static class LocationAnalyzer implements Analyzer {
        @Override
        public void analyze(FileContext context, List<Finding> findings) {
            String path = context.getPath().toString().toLowerCase();
            for (Finding f : findings) {
                if (path.contains("src/main/") || path.endsWith(".env") || path.contains("config")) {
                    f.log("Production/Config File Path", 20, 0);
                }
                if (path.contains("test") || path.contains("mock") || path.contains("spec")) {
                    f.log("Test/Mock File Path", -30, -20);
                }
            }
        }
    }

    public static class SurroundingContextAnalyzer implements Analyzer {
        @Override
        public void analyze(FileContext context, List<Finding> findings) {
            for (Finding f : findings) {
                int idx = f.getLineNumber() - 1;
                String chunk = (context.getLineOrEmpty(idx - 1) + " " +
                    context.getLineOrEmpty(idx) + " " +
                    context.getLineOrEmpty(idx + 1)).toLowerCase();

                if (chunk.contains("example") || chunk.contains("dummy") || chunk.contains("fake") || chunk.contains("mock")) {
                    f.log("Mock Context Keywords Found", 0, -40);
                }
                if (chunk.contains("prod") || chunk.contains("live")) {
                    f.log("Production Context Keywords Found", 20, 0);
                }
            }
        }
    }

    public static class EntropyAnalyzer implements Analyzer {
        @Override
        public void analyze(FileContext context, List<Finding> findings) {
            for (Finding f : findings) {
                if (f.getPatternName().contains("Private Key") || f.getSecretValue().contains("-----")) continue;

                double entropy = getShannonEntropy(f.getSecretValue());
                if (entropy > 4.5) {
                    f.log(String.format("High Entropy (%.2f)", entropy), 10, 20);
                } else if (entropy < 3.0) {
                    f.log(String.format("Low Entropy (%.2f)", entropy), 0, -40);
                }
            }
        }

        private double getShannonEntropy(String s) {
            if (s == null || s.isEmpty()) return 0;
            Map<Character, Integer> freq = new HashMap<>();
            for (char c : s.toCharArray()) freq.merge(c, 1, Integer::sum);
            double entropy = 0.0;
            for (int count : freq.values()) {
                double p = (double) count / s.length();
                entropy -= p * (Math.log(p) / Math.log(2));
            }
            return entropy;
        }
    }

    // =====================================================
    // Presentation / Output
    // =====================================================
    private static void printResults(List<Finding> safeSnapshot) {
        System.out.println("\n=======================================================");
        System.out.println("                 FACEPALM SCAN REPORT                  ");
        System.out.println("=======================================================\n");

        safeSnapshot.stream()
            .filter(f -> f.getSeverity() != Severity.INFO) // Hide noise from report
            .sorted(Comparator.comparing(Finding::getNumericScore).reversed())
            .forEach(f -> {
                String icon = f.getSeverity() == Severity.ERROR ? "🔴" : "🟡";
                System.out.printf("%s %s [%s] - Score: %.1f (R:%d/C:%d)%n",
                    icon, f.getSeverity(), f.getPatternName(),
                    f.getNumericScore(), f.getRiskScore(), f.getConfidenceScore());
                System.out.printf("   Location  : %s:%d%n", f.getContext().getPath(), f.getLineNumber());
                System.out.printf("   Secret    : %s%n", f.getMaskedSecret());

                System.out.println("   Audit Log :");
                f.getScoreHistory().forEach(log -> System.out.println("     └─ " + log));

                System.out.printf("   Context   : %s%n", f.getContextSnippet().length() > 80
                    ? f.getContextSnippet().substring(0, 77) + "..."
                    : f.getContextSnippet());
                System.out.println("-------------------------------------------------------");
            });

        long errors = RESULTS.stream().filter(f -> f.getSeverity() == Severity.ERROR).count();
        long warnings = RESULTS.stream().filter(f -> f.getSeverity() == Severity.WARNING).count();
        System.out.println("\nScan Complete. Total Filtered Findings: " + RESULTS.size() + " | Critical: " + errors + " | Warnings: " + warnings);
    }
}
