package dev.nichar.facepalm._old;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.LongAdder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Value;


public class FacepalmScanner3 {

    public static void main(String[] args) throws Exception {
        // 1. Initial configuration (Now uses verified internal defaults)
        // 1. Fully Exploded Configuration (Explicitly matching all internal defaults)
        ScanConfig config = ScanConfig.builder()
            .engine(ScanConfig.EngineConfig.builder()
                .threads(Runtime.getRuntime().availableProcessors())
                .maxFileSizeBytes(5 * 1024 * 1024) // 5MB
                .skipBinaryRegex(".*\\.(png|jpg|jpeg|gif|pdf|zip|jar|class|tar|gz|exe|dll)$")
                // Note: We leave skipDirs null to trigger the internal DEFAULTS set
                .build())
            .scoring(ScanConfig.ScoringConfig.builder()
                .errorThreshold(80)
                .warningThreshold(40)
                .build())
            .evaluators(ScanConfig.EvaluatorConfig.builder()
                .interpolationPattern(Pattern.compile(".*?(?:\\$\\{.*}|\\{\\{.*}|<.*>|%.*%|\\[.*]).*"))
                .prodPathMarkers(List.of("src/main/", ".env", "config"))
                .testPathMarkers(List.of("test", "mock", "spec"))
                .prodContextMarkers(List.of("prod", "live"))
                .mockContextMarkers(List.of("example", "dummy", "fake", "mock"))
                // Note: Leaving high/low risk extensions and dummy keywords null
                // to trigger the DEF_ constants in the class logic
                .build())
            .postProcessing(ScanConfig.PostProcessorConfig.builder()
                .highVolumeThreshold(15)
                .build())
            .patterns(ScanConfig.PatternConfig.builder()
                // Uses Registry.DEFAULT_PATTERNS by default
                .build())
            .build();

        Path root = Paths.get(args.length > 0 ? args[0] : ".").toAbsolutePath().normalize();

        // 2. Initialize Services (Restored .gitignore loading)
        GitIgnoreService gitIgnoreService = new GitIgnoreService();
        gitIgnoreService.loadAllGitIgnores(root);

        // 3. Build Pipeline using sub-configs
        List<SecretExtractor> extractors = List.of(
            new RegexSecretExtractor(config.getPatterns())
        );

        List<FindingEvaluator> evaluators = List.of(
            new PlaceholderSuppressorEvaluator(config.getEvaluators()),
            new PublicExposureEvaluator(gitIgnoreService),
            new FileExtensionEvaluator(config.getEvaluators()),
            new LocationEvaluator(config.getEvaluators()),
            new SurroundingContextEvaluator(config.getEvaluators()),
            new EntropyEvaluator() // Verified: Matches V2 logic
        );

        List<FileFindingsPostProcessor> processors = List.of(
            new CompositeScoringPostProcessor(config.getPostProcessing())
        );

        // 4. Execute
        ScannerEngine engine = new ScannerEngine(config.getEngine(), extractors, evaluators, processors);
        List<Finding> finalResults = engine.scan(root);

        // 5. Report (Now passes the threshold config)
        Reporter.printResults(finalResults, config.getScoring());
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScanConfig {
        // These assignments ensure that .build() results in non-null default sub-configs
        @Builder.Default private EngineConfig engine = EngineConfig.builder().build();
        @Builder.Default private ScoringConfig scoring = ScoringConfig.builder().build();
        @Builder.Default private EvaluatorConfig evaluators = EvaluatorConfig.builder().build();
        @Builder.Default private PostProcessorConfig postProcessing = PostProcessorConfig.builder().build();
        @Builder.Default private PatternConfig patterns = PatternConfig.builder().build();

        // =====================================================
        // Sub-Configs with Public No-Args Constructors
        // =====================================================

        @Data @Builder @NoArgsConstructor @AllArgsConstructor
        public static class EngineConfig {
            @Builder.Default private int threads = Runtime.getRuntime().availableProcessors();
            @Builder.Default private long maxFileSizeBytes = 5 * 1024 * 1024;
            @Builder.Default private String skipBinaryRegex = ".*\\.(png|jpg|jpeg|gif|pdf|zip|jar|class|tar|gz|exe|dll)$";

            private Set<String> skipDirs;
            private static final Set<String> DEFAULTS = Set.of(".git", ".idea", "target", "build", "node_modules", "temp", "dist", "out", "cid", "vendor");

            public Set<String> getEffectiveSkipDirs() {
                return skipDirs != null ? skipDirs : DEFAULTS;
            }
        }

        @Data @Builder @NoArgsConstructor @AllArgsConstructor
        public static class ScoringConfig {
            @Builder.Default private int errorThreshold = 80;
            @Builder.Default private int warningThreshold = 40;
        }

        @Data @Builder @NoArgsConstructor @AllArgsConstructor
        public static class EvaluatorConfig {
            @Builder.Default private Pattern interpolationPattern = Pattern.compile(".*?(?:\\$\\{.*}|\\{\\{.*}|<.*>|%.*%|\\[.*]).*");

            private Set<String> highRiskExtensions;
            private static final Set<String> DEF_HIGH = Set.of(".env", ".properties", ".yml", ".yaml", ".conf", ".ini");

            private Set<String> lowRiskExtensions;
            private static final Set<String> DEF_LOW = Set.of(".md", ".txt", ".csv", ".log", ".example", ".sample");

            private Set<String> dummyKeywords;
            private static final Set<String> DEF_DUMMY = Set.of(
                "dummy", "your_api_key", "insert_here", "placeholder",
                "place_holder", "replace_me", "changeme", "change_me"
            );

            @Builder.Default private List<String> prodPathMarkers = List.of("src/main/", ".env", "config");
            @Builder.Default private List<String> testPathMarkers = List.of("test", "mock", "spec");
            @Builder.Default private List<String> prodContextMarkers = List.of("prod", "live");
            @Builder.Default private List<String> mockContextMarkers = List.of("example", "dummy", "fake", "mock");

            public Set<String> getHighRiskExts() { return highRiskExtensions != null ? highRiskExtensions : DEF_HIGH; }
            public Set<String> getLowRiskExts() { return lowRiskExtensions != null ? lowRiskExtensions : DEF_LOW; }
            public Set<String> getDummyKeywords() { return dummyKeywords != null ? dummyKeywords : DEF_DUMMY; }
        }

        @Data @Builder @NoArgsConstructor @AllArgsConstructor
        public static class PostProcessorConfig {
            @Builder.Default private int highVolumeThreshold = 15;
        }

        @Data @Builder @NoArgsConstructor @AllArgsConstructor
        public static class PatternConfig {
            private List<SecretPattern> overrides;
            public List<SecretPattern> getEffective() {
                return overrides != null ? overrides : Registry.DEFAULT_PATTERNS;
            }
        }
    }

    // =====================================================
    // Core Engine (Scatter-Gather Concurrency)
    // =====================================================
    public static class ScannerEngine {
        private final ScanConfig.EngineConfig engineConfig;
        private final List<SecretExtractor> extractors;
        private final List<FindingEvaluator> evaluators;
        private final List<FileFindingsPostProcessor> fileProcessors;

        public ScannerEngine(ScanConfig.EngineConfig engineConfig,
                             List<SecretExtractor> extractors,
                             List<FindingEvaluator> evaluators,
                             List<FileFindingsPostProcessor> fileProcessors) {
            this.engineConfig = engineConfig;
            this.extractors = extractors;
            this.evaluators = evaluators;
            this.fileProcessors = fileProcessors;
        }

        public List<Finding> scan(Path root) throws InterruptedException, IOException {
            ScanStatistics stats = new ScanStatistics();
            List<Callable<List<Finding>>> tasks = new ArrayList<>();

            try (Stream<Path> paths = Files.walk(root)) {
                paths.filter(Files::isRegularFile)
                    .filter(this::shouldScan)
                    .forEach(path -> {
                        stats.recordFile(path);
                        tasks.add(() -> processFile(path));
                    });
            }

            if (tasks.isEmpty()) return Collections.emptyList();

            ExecutorService executor = Executors.newFixedThreadPool(engineConfig.getThreads());
            CompletionService<List<Finding>> service = new ExecutorCompletionService<>(executor);
            for (Callable<List<Finding>> task : tasks) service.submit(task);

            List<Finding> allFindings = new ArrayList<>();
            for (int i = 0; i < tasks.size(); i++) {
                try {
                    allFindings.addAll(service.take().get());
                    updateProgressBar(i + 1, tasks.size());
                } catch (Exception ignored) {}
            }
            executor.shutdown();
            System.out.println(); // needed to jump on the next line after \r
            Reporter.printStats(stats);
            return allFindings;
        }

        private boolean shouldScan(Path path) {
            // Now uses the smart merge from the engine sub-config
            Set<String> skipDirs = engineConfig.getEffectiveSkipDirs();
            for (Path element : path) {
                if (skipDirs.contains(element.toString())) return false;
            }
            if (path.getFileName().toString().toLowerCase().matches(engineConfig.getSkipBinaryRegex())) return false;
            try { return Files.size(path) <= engineConfig.getMaxFileSizeBytes(); }
            catch (IOException e) { return false; }
        }

        private List<Finding> processFile(Path path) {
            try {
                String content = Files.readString(path);
                FileContext context = new FileContext(path, content, Arrays.asList(content.split("\\R")));
                List<Finding> fileFindings = new ArrayList<>();
                for (SecretExtractor ex : extractors) fileFindings.addAll(ex.extract(context));
                for (Finding f : fileFindings) {
                    for (FindingEvaluator ev : evaluators) ev.evaluate(f, context);
                }
                for (FileFindingsPostProcessor pp : fileProcessors) pp.process(fileFindings, context);
                return fileFindings;
            } catch (IOException e) { return Collections.emptyList(); }
        }

        private void updateProgressBar(int current, int total) {
            int width = 50;
            double progress = (double) current / total;
            int filled = (int) (width * progress);
            String bar = "[" + "#".repeat(filled) + "-".repeat(Math.max(0, width - filled)) + "]";
            System.out.printf("\rScanning: %s %.0f%% (%d/%d)", bar, progress * 100, current, total);
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
        private final ScanConfig.PatternConfig patternConfig;

        public RegexSecretExtractor(ScanConfig.PatternConfig patternConfig) {
            this.patternConfig = patternConfig;
        }

        @Override
        public List<Finding> extract(FileContext context) {
            List<Finding> findings = new ArrayList<>();
            Set<String> localDedup = new HashSet<>();
            List<SecretPattern> activePatterns = patternConfig.getEffective();

            // Pass 1: Single-line patterns with Quote Normalization
            for (int i = 0; i < context.getLines().size(); i++) {
                String rawLine = context.getLines().get(i);
                // V2 logic restored: cleaning quotes allows regex to match the inner value
                String normalizedLine = rawLine.replace("\"", "").replace("'", "");

                for (SecretPattern sp : activePatterns) {
                    if (sp.isMultiLine()) continue;
                    Matcher m = sp.getPattern().matcher(normalizedLine);
                    while (m.find()) {
                        String secretValue = m.groupCount() >= 1 ? m.group(1) : m.group();
                        registerFinding(findings, localDedup, context, sp, secretValue, i + 1, rawLine);
                    }
                }
            }

            // Pass 2: Multi-line patterns (Private Keys, etc.)
            for (SecretPattern sp : activePatterns) {
                if (!sp.isMultiLine()) continue;
                Matcher m = sp.getPattern().matcher(context.getFullContent());
                while (m.find()) {
                    String secretValue = m.group();
                    int lineNum = (int) context.getFullContent().substring(0, m.start())
                        .chars().filter(ch -> ch == '\n').count() + 1;
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
                    .build();
                f.log("Base Pattern Match", sp.getBaseRisk(), sp.getBaseConfidence());
                findings.add(f);
            }
        }

        private String hashString(String input) {
            try {
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                byte[] hash = md.digest(input.getBytes());
                StringBuilder hexString = new StringBuilder();
                for (byte b : hash) hexString.append(String.format("%02x", b));
                return hexString.toString();
            } catch (NoSuchAlgorithmException e) {
                return String.valueOf(input.hashCode());
            }
        }
    }

    // =====================================================
    // Implementations: Evaluators (Scoring/Enriching)
    // =====================================================
    @RequiredArgsConstructor
    public static class FileExtensionEvaluator implements FindingEvaluator {
        private final ScanConfig.EvaluatorConfig config;

        @Override
        public void evaluate(Finding finding, FileContext context) {
            String fileName = context.getPath().getFileName().toString().toLowerCase();

            if (config.getHighRiskExts().stream().anyMatch(fileName::endsWith)) {
                finding.log("High Risk Configuration File", 15, 20);
            } else if (config.getLowRiskExts().stream().anyMatch(fileName::endsWith)) {
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

    @RequiredArgsConstructor
    public static class PlaceholderSuppressorEvaluator implements FindingEvaluator {
        private final ScanConfig.EvaluatorConfig config;

        @Override
        public void evaluate(Finding finding, FileContext context) {
            String val = finding.getSecretValue().trim().replaceAll("[,;\"']+$", "");

            // Uses configurable Regex for ${VAR} or {{VAR}}
            if (config.getInterpolationPattern().matcher(val).matches()) {
                finding.log("Interpolation/Placeholder Shield", -50, -100);
                return;
            }

            // Uses configurable dummy keywords list
            String lowerVal = val.toLowerCase().replace("-", "_").replace(" ", "_");
            if (config.getDummyKeywords().stream().anyMatch(lowerVal::contains)) {
                finding.log("Dummy Keyword Penalty", 0, -80);
            }
        }
    }

    public static class LocationEvaluator implements FindingEvaluator {
        private final ScanConfig.EvaluatorConfig config;

        public LocationEvaluator(ScanConfig.EvaluatorConfig config) {
            this.config = config;
        }

        @Override
        public void evaluate(Finding finding, FileContext context) {
            String path = context.getPath().toString().toLowerCase();

            // Use markers from config instead of hardcoded strings
            if (config.getProdPathMarkers().stream().anyMatch(path::contains)) {
                finding.log("Production Path Marker", 20, 0);
            }

            if (config.getTestPathMarkers().stream().anyMatch(path::contains)) {
                finding.log("Test/Mock Path Marker", -30, -20);
            }
        }
    }

    @RequiredArgsConstructor
    public static class SurroundingContextEvaluator implements FindingEvaluator {
        private final ScanConfig.EvaluatorConfig config;

        @Override
        public void evaluate(Finding finding, FileContext context) {
            int idx = finding.getLineNumber() - 1;
            // Check line before, current line, and line after
            String chunk = (context.getLineOrEmpty(idx - 1) + " " +
                context.getLineOrEmpty(idx) + " " +
                context.getLineOrEmpty(idx + 1)).toLowerCase();

            if (config.getMockContextMarkers().stream().anyMatch(chunk::contains)) {
                finding.log("Mock Context Keywords Found", 0, -40);
            }
            if (config.getProdContextMarkers().stream().anyMatch(chunk::contains)) {
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
    @RequiredArgsConstructor
    public static class CompositeScoringPostProcessor implements FileFindingsPostProcessor {
        private final ScanConfig.PostProcessorConfig config;

        @Override
        public void process(List<Finding> fileFindings, FileContext context) {
            if (fileFindings.isEmpty()) return;

            int totalInFile = fileFindings.size();
            long uniquePatterns = fileFindings.stream()
                .map(Finding::getPatternName)
                .distinct()
                .count();

            for (Finding f : fileFindings) {
                // If a file has too many "hits", it's likely a false positive (test data/seed)
                if (totalInFile > config.getHighVolumeThreshold()) {
                    f.log("High Volume File (Threshold: " + config.getHighVolumeThreshold() + ")", -25, -30);
                }
                // If a file has multiple types of secrets, the risk of it being real increases
                else if (uniquePatterns > 1) {
                    f.log("Composite Risk: Multiple distinct secrets in one file", 15, 10);
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
    @AllArgsConstructor
    public static class Finding {
        private final String patternName;
        @EqualsAndHashCode.Include private final String deduplicationHash;
        private final String secretValue;
        private final int lineNumber;
        private final FileContext context;
        private final String contextSnippet;

        private int riskScore;
        private int confidenceScore;
        @Builder.Default private final List<String> scoreHistory = new ArrayList<>();

        public void log(String rule, int rDelta, int cDelta) {
            this.riskScore = Math.max(0, Math.min(100, this.riskScore + rDelta));
            this.confidenceScore = Math.max(0, Math.min(100, this.confidenceScore + cDelta));
            scoreHistory.add(String.format("%s (%+d/%+d)", rule, rDelta, cDelta));
        }

        // Now accepts the scoring sub-config
        public Severity getSeverity(ScanConfig.ScoringConfig config) {
            double score = getNumericScore();
            if (score >= config.getErrorThreshold()) return Severity.ERROR;
            if (score >= config.getWarningThreshold()) return Severity.WARNING;
            return Severity.INFO;
        }

        public double getNumericScore() {
            return ScoringStrategy.WEIGHTED_QUADRATIC.calculate(riskScore, confidenceScore);
        }

        public String getMaskedSecret() {
            return secretValue.length() <= 8 ? "****" : secretValue.substring(0, 4) + "..." + secretValue.substring(secretValue.length() - 4);
        }
    }

    public static class Registry {
        public static final List<SecretPattern> DEFAULT_PATTERNS = List.of(
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
            new SecretPattern("Redis Password", Pattern.compile("(?i)redis://[^/\\s]*:([^/\\s]+)@"), 80, 90, false),
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
            new SecretPattern("Generic API Key Assignment", Pattern.compile("(?i)(?:api[_-]?key|secret|token|client[_-]?secret)\\s*[=:]\\s*['\"]?(?!\\{\\{|\\$\\{)([^'\"\\s}]{16,})['\"]?"), 65, 70, false),
            new SecretPattern("Generic Password Assignment", Pattern.compile("(?i)(?:password|passwd|pwd)\\s*[=:]\\s*['\"]?(?!\\{\\{|\\$\\{)([^'\"\\s}]{8,})['\"]?"), 50, 40, false)
        );
    }

    // =====================================================
    // Presentation / Output
    // =====================================================
    public static class Reporter {

        public static void printResults(List<Finding> findings, ScanConfig.ScoringConfig scoring) {
            System.out.println("\n=======================================================");
            System.out.println("                 FACEPALM SCAN REPORT                  ");
            System.out.println("=======================================================\n");

            findings.stream()
                .filter(f -> f.getSeverity(scoring) != Severity.INFO)
                .sorted(Comparator.comparing(Finding::getNumericScore).reversed())
                .forEach(f -> {
                    String icon = f.getSeverity(scoring).getIcon();
                    System.out.printf("%s %s [%s] - Score: %.1f (R:%d/C:%d)%n",
                        icon, f.getSeverity(scoring), f.getPatternName(),
                        f.getNumericScore(), f.getRiskScore(), f.getConfidenceScore());

                    System.out.printf("   Location : %s:%d%n", f.getContext().getPath(), f.getLineNumber());
                    System.out.printf("   Secret   : %s%n", f.getMaskedSecret());

                    System.out.println("   Audit Log:");
                    f.getScoreHistory().forEach(log -> System.out.println("     └─ " + log));

                    String snippet = f.getContextSnippet();
                    System.out.printf("   Context  : %s%n", snippet.length() > 80
                        ? snippet.substring(0, 77) + "..."
                        : snippet);
                    System.out.println("-------------------------------------------------------");
                });

            long errors = findings.stream().filter(f -> f.getSeverity(scoring) == Severity.ERROR).count();
            long warnings = findings.stream().filter(f -> f.getSeverity(scoring) == Severity.WARNING).count();

            System.out.println("\nScan Complete.");
            System.out.printf("Filtered Findings: %d | 🔴 Critical: %d | 🟡 Warnings: %d%n",
                findings.size(), errors, warnings);
        }

        public static void printStats(ScanStatistics stats) {
            System.out.println("\n=======================================================");
            System.out.println("                SCAN PERFORMANCE & STATS               ");
            System.out.println("=======================================================");
            System.out.printf("Total Time      : %d ms%n", stats.getDuration());
            System.out.printf("Files Traversed : %d%n", stats.getFilesTraversed().sum());
            System.out.println("File Extensions :");

            stats.getSuffixCounts().entrySet().stream()
                .sorted((e1, e2) -> Long.compare(e2.getValue().sum(), e1.getValue().sum()))
                .limit(10)
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
