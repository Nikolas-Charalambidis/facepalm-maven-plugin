package dev.nichar.facepalm;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;

import dev.nichar.facepalm.config.ScoringConfig;
import dev.nichar.facepalm.pattern.SecretPattern;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

public class FacepalmScanner {

    @Named @Singleton
    public static class FacepalmRunner {
        @Inject private @Nullable Log log;
        @Inject private dev.nichar.facepalm.FacepalmConfig context;
        @Inject private FacepalmScanner.ScannerEngine engine;
        @Inject private FacepalmScanner.GitIgnoreService gitIgnoreService;
        @Inject private FacepalmScanner.Reporter reporter;

        private Log getLog() {
            return log != null ? log : new org.apache.maven.plugin.logging.SystemStreamLog();
        }

        public void run(Path root, Path outputDir, String version)
            throws Exception {
            getLog().info("Scanning " + root);

            gitIgnoreService.loadAllGitIgnores(root);
            List<FacepalmScanner.Finding> findings = engine.scan(root);
            FacepalmScanner.ScanStatistics stats = engine.getStats();
            reporter.printResults(findings);
            reporter.printStats(stats);
            reporter.performReporting(findings, stats, root.toString(), version, outputDir);

            final var scoring = context.getScoring();
            long errors   = findings.stream().filter(f -> f.getSeverity(scoring) == FacepalmScanner.Severity.ERROR).count();
            long warnings = findings.stream().filter(f -> f.getSeverity(scoring) == FacepalmScanner.Severity.WARNING).count();
            checkFailureConditions(errors, warnings);
        }

        private void checkFailureConditions(long errors, long warnings)
            throws MojoFailureException {
            final var scoring = context.getScoring();
            if (!scoring.isFailOnError() && scoring.isFailOnWarnings()) {
                getLog().warn("Unusual configuration: failOnError=false with failOnWarnings=true");
            }
            if (scoring.isFailOnError() && errors > 0) {
                throw new MojoFailureException("Facepalm scan failed: " + errors + " critical findings detected.");
            }
            if (scoring.isFailOnWarnings() && warnings > 0) {
                throw new MojoFailureException("Facepalm scan failed: " + warnings + " warnings detected and failOnWarnings is true.");
            }
        }
    }

    @Named
    @Singleton
    public static class ScannerEngine {
        private final Log log;
        @Inject private dev.nichar.facepalm.FacepalmConfig context;
        @Inject private List<SecretExtractor> extractors;
        @Inject private List<FindingEvaluator> evaluators;
        @Inject private List<FileFindingsPostProcessor> fileProcessors;
        @Getter
        private final ScanStatistics stats = new ScanStatistics();

        @Inject
        public ScannerEngine(@Nullable Log log) {
            this.log = log != null ? log : new org.apache.maven.plugin.logging.SystemStreamLog();
        }

        public List<Finding> scan(Path root) throws InterruptedException, IOException {
            // 1. Capture the config on the MAIN thread
            final dev.nichar.facepalm.FacepalmConfig currentConfig = context;
            final var engineConfig = currentConfig.getEngine();

            List<Callable<List<Finding>>> tasks = new ArrayList<>();
            try (Stream<Path> paths = Files.walk(root)) {
                paths.filter(Files::isRegularFile)
                    .peek(path -> stats.recordDiscovery()) // Moved discovery to BEFORE filter
                    .filter(path -> shouldScan(path, stats))
                    .forEach(path -> {
                        stats.recordScan(path);
                        // 2. Wrap the task to "pipe" the context
                        tasks.add(() -> {
                            try {
                                // This happens INSIDE the Executor thread
                                //context.set(currentConfig);
                                return processFile(path);
                            } finally {
                                // CRITICAL: Clean up so the next task on this thread
                                // doesn't start with stale data
                                //context.clear();
                            }
                        });
                    });
            }

            if (tasks.isEmpty()) {
                log.info("No files found to scan.");
                return Collections.emptyList();
            }

            log.info("Starting scan of " + tasks.size() + " files...");

            // Use try-with-resources for ExecutorService if on Java 19+,
            // otherwise manual shutdown is required.
            ExecutorService executor = Executors.newFixedThreadPool(engineConfig.getThreads());
            try {
                CompletionService<List<Finding>> service = new ExecutorCompletionService<>(executor);
                for (Callable<List<Finding>> task : tasks) service.submit(task);

                List<Finding> allFindings = new ArrayList<>();
                for (int i = 0; i < tasks.size(); i++) {
                    try {
                        allFindings.addAll(service.take().get());
                    } catch (Exception e) {
                        log.error("Error processing file", e);
                    }
                }
                return allFindings;
            } finally {
                executor.shutdown();
            }
        }

        private boolean shouldScan(Path path, ScanStatistics stats) {
            final var engineConfig = context.getEngine();
            Set<String> skipDirs = engineConfig.getEffectiveSkipDirs();
            for (Path element : path) {
                if (skipDirs.contains(element.toString())) {
                    if (engineConfig.isShowSkipped()) {
                        log.debug("Skipping file in excluded directory [" + element + "]: " + path);
                    }
                    stats.recordExclusion(ExclusionReason.REGEX_MATCH);
                    return false;
                }
            }
            String fileName = path.getFileName().toString();
            if (fileName.toLowerCase().matches(engineConfig.getSkipBinaryRegex())) {
                if (engineConfig.isShowSkipped()) {
                    log.debug("Skipping binary file: " + path);
                }
                stats.recordExclusion(ExclusionReason.BINARY_FILE);
                return false;
            }
            try {
                if (Files.size(path) > engineConfig.getMaxFileSizeBytes()) {
                    if (engineConfig.isShowSkipped()) {
                        log.debug("Skipping large file (> " + engineConfig.getMaxFileSizeBytes() + " bytes): " + path);
                    }
                    stats.recordExclusion(ExclusionReason.SIZE_EXCEEDED);
                    return false;
                }
            } catch (IOException e) {
                log.error("Could not determine size for: " + path);
                stats.recordExclusion(ExclusionReason.IO_ERROR);
                return false;
            }
            return true;
        }

        private List<Finding> processFile(Path path) {
            final var engineConfig = context.getEngine();

            if (engineConfig.isShowProcessed()) {
                log.debug("Processing file: " + path);
            }
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
            } catch (java.nio.charset.MalformedInputException e) {
                if (engineConfig.isShowSkipped()) {
                    log.warn("Skipping " + path + " - file is not valid UTF-8 text:" + e.getMessage());
                }
                return Collections.emptyList();
            } catch (IOException e) {
                log.error("Failed to read " + path + ": " + e.getMessage());
                return Collections.emptyList();
            }
        }
    }

    public interface SecretExtractor {
        List<Finding> extract(FileContext context);
    }

    public interface FindingEvaluator {
        void evaluate(Finding finding, FileContext context);
    }

    public interface FileFindingsPostProcessor {
        void process(List<Finding> fileFindings, FileContext context);
    }

    @Named
    @Singleton
    public static class RegexSecretExtractor implements SecretExtractor {
        @Inject private dev.nichar.facepalm.FacepalmConfig config;

        @Override
        public List<Finding> extract(FileContext context) {
            List<Finding> findings = new ArrayList<>();
            Set<String> localDedup = new HashSet<>();
            List<SecretPattern> activePatterns = config.getPatterns().getEffective();

            for (int i = 0; i < context.getLines().size(); i++) {
                String rawLine = context.getLines().get(i);
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
                    .riskScore(sp.getBaseRisk())
                    .confidenceScore(sp.getBaseConfidence())
                    .build();
                f.log("Base Pattern Match", 0, 0);
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

    @Named
    @Singleton
    public static class FileExtensionEvaluator implements FindingEvaluator {
        @Inject private dev.nichar.facepalm.FacepalmConfig config;

        @Override
        public void evaluate(Finding finding, FileContext context) {
            String fileName = context.getPath().getFileName().toString().toLowerCase();

            if (config.getEvaluators().getHighRiskExtensions().stream().anyMatch(fileName::endsWith)) {
                finding.log("High Risk Configuration File", 15, 20);
            } else if (config.getEvaluators().getLowRiskExtensions().stream().anyMatch(fileName::endsWith)) {
                finding.log("Documentation/Log File", -30, -40);
            }
        }
    }

    @Named
    @Singleton
    public static class PublicExposureEvaluator implements FindingEvaluator {
        private final GitIgnoreService gitIgnoreService;

        @Inject
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

    @Named
    @Singleton
    public static class PlaceholderSuppressorEvaluator implements FindingEvaluator {
        @Inject private dev.nichar.facepalm.FacepalmConfig config;

        @Override
        public void evaluate(Finding finding, FileContext context) {
            String val = finding.getSecretValue().trim().replaceAll("[,;\"']+$", "");

            final var conf = config.getEvaluators();
            if (conf.getInterpolationPattern().matcher(val).matches()) {
                finding.log("Interpolation/Placeholder Shield", -50, -100);
                return;
            }

            String lowerVal = val.toLowerCase().replace("-", "_").replace(" ", "_");
            if (conf.getDummyKeywords().stream().anyMatch(lowerVal::contains)) {
                finding.log("Dummy Keyword Penalty", 0, -80);
            }
        }
    }

    @Named
    @Singleton
    public static class LocationEvaluator implements FindingEvaluator {
        @Inject private dev.nichar.facepalm.FacepalmConfig config;

        @Override
        public void evaluate(Finding finding, FileContext context) {
            String path = context.getPath().toString().toLowerCase();

            final var conf = config.getEvaluators();
            if (conf.getProdPathMarkers().stream().anyMatch(path::contains)) {
                finding.log("Production Path Marker", 20, 0);
            }

            if (conf.getTestPathMarkers().stream().anyMatch(path::contains)) {
                finding.log("Test/Mock Path Marker", -30, -20);
            }
        }
    }

    @Named
    @Singleton
    public static class SurroundingContextEvaluator implements FindingEvaluator {
        @Inject private dev.nichar.facepalm.FacepalmConfig config;

        @Override
        public void evaluate(Finding finding, FileContext context) {
            int idx = finding.getLineNumber() - 1;
            String chunk = (context.getLineOrEmpty(idx - 1) + " " +
                context.getLineOrEmpty(idx) + " " +
                context.getLineOrEmpty(idx + 1)).toLowerCase();

            final var conf = config.getEvaluators();
            if (conf.getMockContextMarkers().stream().anyMatch(chunk::contains)) {
                finding.log("Mock Context Keywords Found", 0, -40);
            }
            if (conf.getProdContextMarkers().stream().anyMatch(chunk::contains)) {
                finding.log("Production Context Keywords Found", 20, 0);
            }
        }
    }

    @Named
    @Singleton
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

    @Named
    @Singleton
    public static class CompositeScoringPostProcessor implements FileFindingsPostProcessor {
        @Inject private dev.nichar.facepalm.FacepalmConfig config;

        @Override
        public void process(List<Finding> fileFindings, FileContext context) {
            if (fileFindings.isEmpty()) return;

            int totalInFile = fileFindings.size();
            long uniquePatterns = fileFindings.stream()
                .map(Finding::getPatternName)
                .distinct()
                .count();

            final var conf = config.getPostProcessing();
            for (Finding f : fileFindings) {
                if (totalInFile > conf.getHighVolumeThreshold()) {
                    f.log("High Volume File (Threshold: " + conf.getHighVolumeThreshold() + ")", -25, -30);
                }
                else if (uniquePatterns > 1) {
                    f.log("Composite Risk: Multiple distinct secrets in one file", 15, 10);
                }
            }
        }
    }

    @Named
    @Singleton
    public static class GitIgnoreService {
        private final Log log;
        private final TreeMap<Path, List<PathMatcher>> registry = new TreeMap<>();

        @Inject
        public GitIgnoreService(@Nullable Log log) {
            this.log = log != null ? log : new org.apache.maven.plugin.logging.SystemStreamLog();
        }

        public void loadAllGitIgnores(Path root) {
            try (Stream<Path> paths = Files.walk(root)) {
                paths.filter(p -> p.getFileName() != null && p.getFileName().toString().equals(".gitignore"))
                    .forEach(this::parseGitIgnoreFile);
            } catch (IOException e) {
                log.warn("⚠️ Error walking for .gitignores: " + e.getMessage());
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

        public Severity getSeverity(ScoringConfig config) {
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

    @Named
    @Singleton
    public static class Reporter {

        @Inject private dev.nichar.facepalm.FacepalmConfig context;

        private final Log log;
        private final Configuration cfg;

        @Inject
        public Reporter(@Nullable Log log, FacepalmConfig context) {
            this.log = log != null ? log : new org.apache.maven.plugin.logging.SystemStreamLog();
            this.cfg = new Configuration(Configuration.VERSION_2_3_32);
            this.cfg.setClassForTemplateLoading(FacepalmScanner.class, "/templates");
            this.cfg.setDefaultEncoding("UTF-8");
            this.cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
            this.cfg.setLogTemplateExceptions(false);
            this.cfg.setWrapUncheckedExceptions(true);
            this.cfg.setFallbackOnNullLoopVariable(false);
        }

        public void performReporting(List<Finding> findings, ScanStatistics stats, String rootPath, String version, Path outputPathBase) throws Exception {
            ScanReport report = buildReport(findings, stats, rootPath, version);
            generateHtml(report, outputPathBase.resolve("facepalm-report.html"));
            generateSarif(report, outputPathBase.resolve("facepalm-report.sarif").toFile());
        }

        public void generateHtml(ScanReport report, Path outputPath) throws Exception {
            Template temp = cfg.getTemplate("report.html.ftl");
            File outputFile = outputPath.toFile();
            if (outputFile.getParentFile() != null) {
                outputFile.getParentFile().mkdirs();
            }
            try (Writer out = new OutputStreamWriter(new FileOutputStream(outputFile), StandardCharsets.UTF_8)) {
                temp.process(report, out);
            }
            log.info("📊  HTML Report generated at: " + outputPath.toAbsolutePath());
        }

        public void generateSarif(ScanReport report, File outputFile) throws Exception {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode sarif = mapper.createObjectNode();
            sarif.put("$schema", "https://schemastore.azurewebsites.net/schemas/json/sarif-2.1.0-rtm.5.json");
            sarif.put("version", "2.1.0");

            ArrayNode runs = sarif.putArray("runs");
            ObjectNode run = runs.addObject();

            ObjectNode tool = run.putObject("tool");
            ObjectNode driver = tool.putObject("driver");
            driver.put("name", "Facepalm");
            driver.put("version", report.getMetadata().getScannerVersion());

            ArrayNode results = run.putArray("results");
            for (ScanReport.UniqueLeak leak : report.getLeaks()) {
                for (ScanReport.Occurrence occ : leak.getOccurrences()) {
                    ObjectNode result = results.addObject();
                    result.put("ruleId", leak.getPrimaryRuleId());
                    result.put("level", leak.getTotalRisk() > 80 ? "error" : "warning");

                    ObjectNode message = result.putObject("message");
                    message.put("text", "Secret detected: " + leak.getSecret());

                    ArrayNode locations = result.putArray("locations");
                    ObjectNode loc = locations.addObject();
                    ObjectNode phys = loc.putObject("physicalLocation");
                    phys.putObject("artifactLocation").put("uri", occ.getRelativePath().replace("\\", "/"));
                    phys.putObject("region").put("startLine", occ.getLineNumber());
                }
            }
            mapper.writerWithDefaultPrettyPrinter().writeValue(outputFile, sarif);
            log.info("📊 Sarif Report generated at: " + outputFile.toPath().toAbsolutePath());
        }

        public ScanReport buildReport(List<Finding> findings, ScanStatistics stats, String rootPath, String version) {
            Map<String, List<Finding>> grouped = findings.stream()
                .collect(Collectors.groupingBy(f ->
                    Base64.getEncoder().encodeToString((f.getPatternName() + ":" + f.getMaskedSecret()).getBytes())
                ));

            Map<String, ScanReport.RuleDefinition> ruleDict = new HashMap<>();

            List<ScanReport.UniqueLeak> leaks = grouped.entrySet().stream().map(entry -> {
                    String fingerprint = entry.getKey();
                    List<Finding> occs = entry.getValue();
                    Finding primary = occs.get(0);

                    ruleDict.putIfAbsent(primary.getPatternName(), ScanReport.RuleDefinition.builder()
                        .id(primary.getPatternName())
                        .name(primary.getPatternName())
                        .description("Automated detection for " + primary.getPatternName())
                        .remediation("Revoke the secret immediately and update configuration.")
                        .build());

                    return ScanReport.UniqueLeak.builder()
                        .primaryRuleId(primary.getPatternName())
                        .totalRisk(primary.getRiskScore())
                        .totalConfidence(primary.getConfidenceScore())
                        .aggregateScore(primary.getNumericScore())
                        .secret(primary.getSecretValue())
                        .maskedSecret(primary.getMaskedSecret())
                        .fingerprint(fingerprint)
                        .scoreHistory(primary.getScoreHistory())
                        .occurrences(occs.stream().map(f -> ScanReport.Occurrence.builder()
                            .relativePath(f.getContext().getPath().toString())
                            .absolutePath(f.getContext().getPath().toAbsolutePath().toString())
                            .lineNumber(f.getLineNumber())
                            .snippet(f.getContextSnippet())
                            .build()).collect(Collectors.toList()))
                        .build();
                }).sorted(Comparator.comparing(ScanReport.UniqueLeak::getAggregateScore).reversed())
                .collect(Collectors.toList());

            return ScanReport.builder()
                .metadata(ScanReport.RunMetadata.builder()
                    .scannerVersion(version)
                    .timestamp(Instant.now().toString())
                    .rootPath(rootPath)
                    .build())
                .summary(ScanReport.ScanSummary.builder()
                    .totalLeaksFound(leaks.size())
                    .totalOccurrences(findings.size())
                    .filesScanned((int) stats.getFilesScanned().sum())
                    .criticalCount((int) leaks.stream().filter(l -> l.getAggregateScore() > 80).count())
                    .warningCount((int) leaks.stream().filter(l -> l.getAggregateScore() <= 80 && l.getAggregateScore() > 40).count())
                    .build())
                .ruleDictionary(ruleDict)
                .leaks(leaks)
                .build();
        }

        public void printResults(List<Finding> findings) {
            // 1. Maven uses a standard 72-character line for separators
            final String SEPARATOR = "------------------------------------------------------------------------";

            log.info(SEPARATOR);

            if (findings.isEmpty()) {
                log.info("No secrets or sensitive patterns detected. Your secrets are safe.");
                log.info(SEPARATOR);
            }

            final var scoringConfig = context.getScoring();
            if (scoringConfig.isShowDetails()) {
                findings.stream()
                    .filter(f -> f.getSeverity(scoringConfig) != Severity.INFO)
                    .sorted(Comparator.comparing(Finding::getNumericScore).reversed())
                    .forEach(f -> {
                        Severity sev = f.getSeverity(scoringConfig);

                        // 2. Use the log level that matches the finding severity for proper coloring
                        String message = String.format(
                            "[%s] Score: %.1f (R:%d/C:%d) - %s",
                            f.getPatternName(), f.getNumericScore(),
                            f.getRiskScore(), f.getConfidenceScore(),
                            f.getSeverity(scoringConfig).getIcon());

                        if (sev == Severity.ERROR) {
                            log.error(message);
                        } else {
                            log.warn(message);
                        }

                        // Indented info details following the header
                        log.info("  Location: " + f.getContext().getPath() + ":" + f.getLineNumber());

                        String snippet = f.getContextSnippet().trim();
                        log.info("  Context : " + (snippet.length() > 80
                            ? snippet.substring(0, 77) + "..."
                            : snippet));

                        if (log.isDebugEnabled()) {
                            log.debug("  Secret  : " + f.getMaskedSecret());
                            log.debug("  Audit   : " + f.getRiskScore() + "/" + f.getConfidenceScore());
                        }

                        // Empty line between findings for readability, similar to test run logs
                        log.info("");
                    });
            }

            // 4. Summary section mimicking the BUILD SUCCESS/FAILURE block
            long errors = findings.stream().filter(f -> f.getSeverity(scoringConfig) == Severity.ERROR).count();
            long warnings = findings.stream().filter(f -> f.getSeverity(scoringConfig) == Severity.WARNING).count();

            // Determine status text based on findings
            log.info(errors > 0 ? "SCAN FAILURE" : "SCAN SUCCESS");
            log.info(SEPARATOR);
            log.info(String.format("Total findings:  %d", findings.size()));
            log.info(String.format("Critical:        %d", errors));
            log.info(String.format("Warnings:        %d", warnings));
            log.info(SEPARATOR);
        }

        public void printStats(ScanStatistics stats) {
            final String SEPARATOR = "------------------------------------------------------------------------";

            // Maven usually logs timing and stats within the standard line format
            log.info("Total time:       " + formatDuration(stats.getDuration()));
            log.info("Finished at:      " + java.time.ZonedDateTime.now().format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME));
            log.info("Files discovered: " + stats.getFilesDiscovered().sum());
            log.info("Files excluded:   " + stats.getExclusionBreakdown().values().stream().mapToLong(LongAdder::sum).sum());
            if (log.isDebugEnabled()) {
                stats.getExclusionBreakdown().forEach(
                    (key, value) -> log.debug(String.format("  %-12s: %d", key.getDescription(), value.sum())));
            }
            log.info("Files scanned:    " + stats.getFilesScanned().sum());
            if (log.isDebugEnabled()) {
                stats.getSuffixCounts().entrySet().stream()
                    .sorted((e1, e2) -> Long.compare(e2.getValue().sum(), e1.getValue().sum()))
                    .limit(6)
                    .forEach(entry -> log.debug(String.format("  %-12s: %d", entry.getKey(), entry.getValue().sum())));
            }
            log.info(SEPARATOR);
        }

        // Helper for Maven-style duration (e.g., 1.234 s)
        private String formatDuration(long millis) {
            return String.format("%.3f s", millis / 1000.0);
        }
    }

    public enum ExclusionReason {
        BINARY_FILE("Binary file detected"),
        REGEX_MATCH("Path matched exclusion regex"),
        SIZE_EXCEEDED("File size exceeds limit"),
        HIDDEN_PATH("Hidden file or directory"),
        IO_ERROR("Unreadable/Access denied");

        private final String description;
        ExclusionReason(String description) { this.description = description; }
        public String getDescription() { return description; }
    }

    @Data
    public static class ScanStatistics {
        private final long startTimeMillis = System.currentTimeMillis();

        // Summary Counters
        private final LongAdder filesDiscovered = new LongAdder();
        private final LongAdder filesScanned = new LongAdder();

        // Breakdowns
        private final ConcurrentHashMap<String, LongAdder> suffixCounts = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<ExclusionReason, LongAdder> exclusionBreakdown = new ConcurrentHashMap<>();

        /** Called for every file found during the walk */
        public void recordDiscovery() {
            filesDiscovered.increment();
        }

        /** Called when a file is actually processed */
        public void recordScan(Path path) {
            filesScanned.increment();
            String fileName = path.getFileName().toString();
            int lastDot = fileName.lastIndexOf('.');
            String suffix = (lastDot == -1) ? "no-extension" : fileName.substring(lastDot).toLowerCase();
            suffixCounts.computeIfAbsent(suffix, k -> new LongAdder()).increment();
        }

        /** Called when a file is skipped */
        public void recordExclusion(ExclusionReason reason) {
            exclusionBreakdown.computeIfAbsent(reason, k -> new LongAdder()).increment();
        }

        public long getDuration() {
            return System.currentTimeMillis() - startTimeMillis;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScanReport {
        private RunMetadata metadata;
        private ScanSummary summary;
        private Map<String, RuleDefinition> ruleDictionary;
        private List<UniqueLeak> leaks;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class UniqueLeak {
            private String primaryRuleId;
            private List<String> supplementalRuleIds;
            private int totalRisk;
            private int totalConfidence;
            private double aggregateScore;
            private String secret;
            private String maskedSecret;
            private String fingerprint;
            private List<Occurrence> occurrences;
            private List<String> scoreHistory;
        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class RuleDefinition {
            private String id;
            private String name;
            private String description;
            private String remediation;
            private List<String> tags;
            private String severity;
        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Occurrence {
            private String relativePath;
            private String absolutePath;
            private int lineNumber;
            private String snippet;
        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class RunMetadata {
            private String scannerVersion;
            private String timestamp;
            private long durationMs;
            private String rootPath;
        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class ScanSummary {
            private int totalLeaksFound;
            private int totalOccurrences;
            private int filesScanned;
            private int criticalCount;
            private int warningCount;
        }
    }
}
