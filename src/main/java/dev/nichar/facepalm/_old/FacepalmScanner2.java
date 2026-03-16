package dev.nichar.facepalm._old;

import lombok.*;

import java.io.IOException;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.LongAdder;
import java.util.regex.*;
import java.util.stream.Stream;

public class FacepalmScanner2 {

    public static void main(String[] args) throws Exception {

        final var now = System.currentTimeMillis();

        Path root = Paths.get(args.length > 0 ? args[0] : ".").toAbsolutePath().normalize();

        // 1. Initialize Context & Services
        GitIgnoreService gitIgnoreService = new GitIgnoreService();
        gitIgnoreService.loadAllGitIgnores(root);

        // 2. Build Pipeline
        List<SecretExtractor> extractors = List.of(
            new RegexSecretExtractor()
        );

        List<FindingEvaluator> evaluators = List.of(
            new PlaceholderSuppressorEvaluator(),
            new PublicExposureEvaluator(gitIgnoreService),
            new FileExtensionEvaluator(),
            new LocationEvaluator(),
            new SurroundingContextEvaluator(),
            new EntropyEvaluator()
        );

        List<FileFindingsPostProcessor> fileProcessors = List.of(
            new CompositeScoringPostProcessor()
        );

        // 3. Initialize Engine
        ScannerEngine engine = new ScannerEngine(extractors, evaluators, fileProcessors);

        // 4. Run Scan (Scatter-Gather)
        List<Finding> finalResults = engine.scan(root);

        // 5. Report
        Reporter.printResults(finalResults);

        System.out.println(System.currentTimeMillis() - now);
    }

    // =====================================================
    // Core Engine (Scatter-Gather Concurrency)
    // =====================================================
    public static class ScannerEngine {
        private static final int THREADS = Runtime.getRuntime().availableProcessors();
        private static final long MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024;
        private static final Set<String> SKIP_DIRS = Set.of(
            ".git", ".idea", "target", "build", "node_modules", "temp", "dist", "out", "cid", "vendor"
        );

        private final List<SecretExtractor> extractors;
        private final List<FindingEvaluator> evaluators;
        private final List<FileFindingsPostProcessor> fileProcessors;

        public ScannerEngine(List<SecretExtractor> extractors,
                             List<FindingEvaluator> evaluators,
                             List<FileFindingsPostProcessor> fileProcessors) {
            this.extractors = extractors;
            this.evaluators = evaluators;
            this.fileProcessors = fileProcessors;
        }

        public List<Finding> scan(Path root) throws InterruptedException, IOException {
            ScanStatistics stats = new ScanStatistics();
            List<Callable<List<Finding>>> tasks = new ArrayList<>();

            // 1. Scatter
            try (Stream<Path> paths = Files.walk(root)) {
                paths.filter(Files::isRegularFile)
                    .filter(this::shouldScan)
                    .forEach(path -> {
                        stats.recordFile(path);
                        tasks.add(() -> processFile(path));
                    });
            }

            if (tasks.isEmpty()) {
                Reporter.printStats(stats);
                return Collections.emptyList();
            }

            List<Finding> allFindings = new ArrayList<>();
            int totalFiles = tasks.size();

            // 2. Performance Threshold
            if (totalFiles < 100) {
                for (Callable<List<Finding>> task : tasks) {
                    try { allFindings.addAll(task.call()); } catch (Exception ignored) {}
                }
            } else {
                ExecutorService executor = Executors.newFixedThreadPool(THREADS);
                CompletionService<List<Finding>> completionService = new ExecutorCompletionService<>(executor);

                // Submit all tasks
                for (Callable<List<Finding>> task : tasks) {
                    completionService.submit(task);
                }

                // 3. Gather with Progress Bar
                for (int i = 0; i < totalFiles; i++) {
                    try {
                        Future<List<Finding>> future = completionService.take(); // Blocks until next is ready
                        allFindings.addAll(future.get());
                        updateProgressBar(i + 1, totalFiles);
                    } catch (ExecutionException e) {
                        // Silently handle per-file failures
                    }
                }
                executor.shutdown();
                System.out.println(); // New line after progress bar completes
            }

            // 4. Finalize
            Reporter.printStats(stats);
            return allFindings;
        }

        private void updateProgressBar(int current, int total) {
            int width = 50; // Character width of the bar
            double progress = (double) current / total;
            int filledLength = (int) (width * progress);

            StringBuilder bar = new StringBuilder("[");
            for (int i = 0; i < width; i++) {
                if (i < filledLength) bar.append("#");
                else bar.append("-");
            }
            bar.append("]");

            // Use \r to return to start of line, then print
            System.out.print(String.format("\rScanning: %s %.0f%% (%d/%d)", bar, progress * 100, current, total));
        }

        private List<Finding> processFile(Path path) {
            try {
                String content = Files.readString(path);
                List<String> lines = Arrays.asList(content.split("\\R"));
                FileContext context = new FileContext(path, content, lines);

                List<Finding> fileFindings = new ArrayList<>();
                for (SecretExtractor extractor : extractors) {
                    fileFindings.addAll(extractor.extract(context));
                }

                for (Finding finding : fileFindings) {
                    for (FindingEvaluator evaluator : evaluators) {
                        evaluator.evaluate(finding, context);
                    }
                }

                for (FileFindingsPostProcessor processor : fileProcessors) {
                    processor.process(fileFindings, context);
                }

                return fileFindings;
            } catch (IOException e) {
                return Collections.emptyList();
            }
        }

        private boolean shouldScan(Path path) {
            for (Path element : path) {
                if (SKIP_DIRS.contains(element.toString())) return false;
            }
            String name = path.getFileName().toString().toLowerCase();
            if (name.matches(".*\\.(png|jpg|jpeg|gif|pdf|zip|jar|class|tar|gz|exe|dll)$")) return false;
            try { return Files.size(path) <= MAX_FILE_SIZE_BYTES; }
            catch (IOException e) { return false; }
        }
    }

    // =====================================================
    // Interfaces (The Pipeline Lifecycle)
    // =====================================================
    public interface SecretExtractor {
        List<Finding> extract(FileContext context);
    }

    public interface FindingEvaluator {
        void evaluate(Finding finding, FileContext context);
    }

    public interface FileFindingsPostProcessor {
        void process(List<Finding> fileFindings, FileContext context);
    }

    // =====================================================
    // Implementations: Extractors (Discovery)
    // =====================================================
    public static class RegexSecretExtractor implements SecretExtractor {
        @Override
        public List<Finding> extract(FileContext context) {
            List<Finding> findings = new ArrayList<>();
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
            return findings;
        }

        private void registerFinding(List<Finding> findings, Set<String> dedup, FileContext ctx,
                                     SecretPattern sp, String value, int lineNum, String snippet) {
            String hash = hashString(sp.getName() + value + lineNum);
            if (dedup.add(hash)) {
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

    // =====================================================
    // Implementations: Evaluators (Scoring/Enriching)
    // =====================================================
    public static class FileExtensionEvaluator implements FindingEvaluator {
        private static final Set<String> HIGH_RISK_EXTS = Set.of(".env", ".properties", ".yml", ".yaml", ".conf", ".ini");
        private static final Set<String> LOW_RISK_EXTS = Set.of(".md", ".txt", ".csv", ".log", ".example", ".sample");

        @Override
        public void evaluate(Finding finding, FileContext context) {
            String fileName = context.getPath().getFileName().toString().toLowerCase();
            if (HIGH_RISK_EXTS.stream().anyMatch(fileName::endsWith)) {
                finding.log("High Risk Configuration File", 15, 20);
            } else if (LOW_RISK_EXTS.stream().anyMatch(fileName::endsWith)) {
                finding.log("Documentation/Log File", -30, -40);
            }
        }
    }

    public static class PublicExposureEvaluator implements FindingEvaluator {
        private final GitIgnoreService gitIgnoreService;

        public PublicExposureEvaluator(GitIgnoreService gitIgnoreService) {
            this.gitIgnoreService = gitIgnoreService;
        }

        @Override
        public void evaluate(Finding finding, FileContext context) {
            if (gitIgnoreService.isIgnored(context.getPath())) {
                finding.log("Recursive .gitignore Match (Low Exposure)", -40, 0);
            }
        }
    }

    public static class PlaceholderSuppressorEvaluator implements FindingEvaluator {
        private static final Pattern INTERPOLATION_PATTERN = Pattern.compile(".*?(?:\\$\\{.*\\}|\\{\\{.*\\}|<.*>|%.*%|\\[.*\\]).*");
        private static final Set<String> DUMMY_KEYWORDS = Set.of("dummy", "your_api_key", "insert_here", "placeholder", "place_holder", "replace_me", "changeme", "change_me");

        @Override
        public void evaluate(Finding finding, FileContext context) {
            String val = finding.getSecretValue().trim().replaceAll("[,;\"']+$", "");

            if (INTERPOLATION_PATTERN.matcher(val).matches()) {
                finding.log("Interpolation/Placeholder Shield", -50, -100);
                return;
            }

            String lowerVal = val.toLowerCase().replace("-", "_").replace(" ", "_");
            if (DUMMY_KEYWORDS.stream().anyMatch(lowerVal::contains)) {
                finding.log("Dummy Keyword Penalty", 0, -80);
            }

            if (val.equalsIgnoreCase(finding.getPatternName().split(" ")[0])) {
                finding.log("Value Matches Key Name", 0, -30);
            }
        }
    }

    public static class LocationEvaluator implements FindingEvaluator {
        @Override
        public void evaluate(Finding finding, FileContext context) {
            String path = context.getPath().toString().toLowerCase();
            if (path.contains("src/main/") || path.endsWith(".env") || path.contains("config")) {
                finding.log("Production/Config File Path", 20, 0);
            }
            if (path.contains("test") || path.contains("mock") || path.contains("spec")) {
                finding.log("Test/Mock File Path", -30, -20);
            }
        }
    }

    public static class SurroundingContextEvaluator implements FindingEvaluator {
        @Override
        public void evaluate(Finding finding, FileContext context) {
            int idx = finding.getLineNumber() - 1;
            String chunk = (context.getLineOrEmpty(idx - 1) + " " +
                context.getLineOrEmpty(idx) + " " +
                context.getLineOrEmpty(idx + 1)).toLowerCase();

            if (chunk.contains("example") || chunk.contains("dummy") || chunk.contains("fake") || chunk.contains("mock")) {
                finding.log("Mock Context Keywords Found", 0, -40);
            }
            if (chunk.contains("prod") || chunk.contains("live")) {
                finding.log("Production Context Keywords Found", 20, 0);
            }
        }
    }

    public static class EntropyEvaluator implements FindingEvaluator {
        @Override
        public void evaluate(Finding finding, FileContext context) {
            if (finding.getPatternName().contains("Private Key") || finding.getSecretValue().contains("-----")) return;

            double entropy = getShannonEntropy(finding.getSecretValue());
            if (entropy > 4.5) {
                finding.log(String.format("High Entropy (%.2f)", entropy), 10, 20);
            } else if (entropy < 3.0) {
                finding.log(String.format("Low Entropy (%.2f)", entropy), 0, -40);
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
    // Implementations: Post-Processors (File Context)
    // =====================================================
    public static class CompositeScoringPostProcessor implements FileFindingsPostProcessor {
        @Override
        public void process(List<Finding> fileFindings, FileContext context) {
            if (fileFindings.size() <= 1) return;

            long uniquePatterns = fileFindings.stream().map(Finding::getPatternName).distinct().count();

            for (Finding f : fileFindings) {
                if (fileFindings.size() > 15) {
                    f.log("High Volume File (Likely Mock/Seed Data)", -20, -30);
                } else if (uniquePatterns > 1) {
                    f.log("Composite Risk: Multiple distinct secret types in file", 15, 10);
                }
            }
        }
    }

    // =====================================================
    // Services
    // =====================================================
    public static class GitIgnoreService {
        private final TreeMap<Path, List<PathMatcher>> registry = new TreeMap<>();

        public void loadAllGitIgnores(Path root) {
            try (Stream<Path> paths = Files.walk(root)) {
                paths.filter(p -> p.getFileName() != null && p.getFileName().toString().equals(".gitignore"))
                    .forEach(this::parseGitIgnoreFile);
            } catch (IOException e) {
                System.err.println("⚠️ Error walking for .gitignores: " + e.getMessage());
            }
        }

        private void parseGitIgnoreFile(Path ignoreFile) {
            Path dir = ignoreFile.getParent();
            List<PathMatcher> matchers = new ArrayList<>();

            try (Stream<String> lines = Files.lines(ignoreFile)) {
                lines.map(String::trim)
                    .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                    .forEach(pattern -> {
                        String glob = pattern;
                        if (pattern.endsWith("/")) glob += "**";
                        if (!pattern.startsWith("**/") && !pattern.startsWith("/")) glob = "**/" + glob;
                        String fullGlob = "glob:" + dir.toString().replace("\\", "/") + "/" + glob;
                        matchers.add(FileSystems.getDefault().getPathMatcher(fullGlob));
                    });
                registry.put(dir, matchers);
            } catch (IOException ignored) {}
        }

        public boolean isIgnored(Path filePath) {
            return registry.entrySet().stream()
                .filter(entry -> filePath.startsWith(entry.getKey()))
                .flatMap(entry -> entry.getValue().stream())
                .anyMatch(matcher -> matcher.matches(filePath));
        }
    }

    // =====================================================
    // Models & Configuration
    // =====================================================
    public enum ScoringStrategy {
        AVERAGE { public double calculate(int r, int c) { return (r + c) / 2.0; } },
        GEOMETRIC { public double calculate(int r, int c) { return Math.sqrt(r * (double)c); } },
        RMS { public double calculate(int r, int c) { return Math.sqrt((r*r + c*c) / 2.0); } },
        WEIGHTED_QUADRATIC {
            public double calculate(int r, int c) {
                double rScaled = (r * r) / 100.0;
                return (rScaled * 0.8) + (c * 0.2);
            }
        },
        GATEKEEPER {
            public double calculate(int r, int c) {
                if (r >= 90) return Math.max(r, c);
                return (r * 0.5) + (c * 0.5);
            }
        };
        public abstract double calculate(int risk, int confidence);
    }

    @Getter
    @RequiredArgsConstructor
    public enum Severity {
        INFO("⚪"),
        WARNING("🟡"),
        ERROR("🔴");

        private final String icon;
    }


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
        private static final int ERROR_THRESHOLD = 80;
        private static final int WARNING_THRESHOLD = 40;

        private final String patternName;
        @EqualsAndHashCode.Include private final String deduplicationHash;
        private final String secretValue;
        private final int lineNumber;
        private final FileContext context;
        private final String contextSnippet;

        private int riskScore;
        private int confidenceScore;

        @Builder.Default
        private final List<String> scoreHistory = new ArrayList<>();

        public void log(String rule, int rDelta, int cDelta) {
            if (rDelta == 0 && cDelta == 0) return;
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
    // Presentation / Output
    // =====================================================
    public static class Reporter {
        public static void printResults(List<Finding> safeSnapshot) {
            System.out.println("\n=======================================================");
            System.out.println("                 FACEPALM SCAN REPORT                  ");
            System.out.println("=======================================================\n");

            safeSnapshot.stream()
                .filter(f -> f.getSeverity() != Severity.INFO)
                .sorted(Comparator.comparing(Finding::getNumericScore).reversed())
                .forEach(f -> {
                    String icon = f.getSeverity().getIcon();
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

            long errors = safeSnapshot.stream().filter(f -> f.getSeverity() == Severity.ERROR).count();
            long warnings = safeSnapshot.stream().filter(f -> f.getSeverity() == Severity.WARNING).count();
            System.out.println("\nScan Complete. Total Filtered Findings: " + safeSnapshot.size() + " | Critical: " + errors + " | Warnings: " + warnings);
        }

        private static void printStats(ScanStatistics stats) {
            System.out.println("\n=======================================================");
            System.out.println("                SCAN PERFORMANCE & STATS               ");
            System.out.println("=======================================================");
            System.out.printf("Total Time      : %d ms%n", stats.getDuration());
            System.out.printf("Files Traversed : %d%n", stats.getFilesTraversed().sum());
            System.out.println("File Extensions :");

            stats.getSuffixCounts().entrySet().stream()
                .sorted((e1, e2) -> Long.compare(e2.getValue().sum(), e1.getValue().sum()))
                .limit(10) // Show top 10 most frequent
                .forEach(entry -> System.out.printf("  └─ %-12s: %d%n", entry.getKey(), entry.getValue().sum()));
            System.out.println("=======================================================\n");
        }
    }

    @Data
    public static class ScanStatistics {
        private final long startTimeMillis = System.currentTimeMillis();
        private final LongAdder filesTraversed = new LongAdder();
        private final ConcurrentHashMap<String, LongAdder> suffixCounts = new ConcurrentHashMap<>();

        public void recordFile(Path path) {
            filesTraversed.increment();
            String fileName = path.getFileName().toString();
            int lastDot = fileName.lastIndexOf('.');
            String suffix = (lastDot == -1) ? "no-extension" : fileName.substring(lastDot).toLowerCase();
            suffixCounts.computeIfAbsent(suffix, k -> new LongAdder()).increment();
        }

        public long getDuration() {
            return System.currentTimeMillis() - startTimeMillis;
        }
    }
}
