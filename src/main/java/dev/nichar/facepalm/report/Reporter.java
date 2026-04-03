package dev.nichar.facepalm.report;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.plugin.logging.Log;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.nichar.facepalm.FacepalmConfig;

import dev.nichar.facepalm.engine.Finding;
import dev.nichar.facepalm.engine.ScanReport;
import dev.nichar.facepalm.engine.ScanStatistics;
import dev.nichar.facepalm.engine.Severity;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;


@Named
@Singleton
public class Reporter {

    // Maven uses a standard 72-character line for separators.
    private static final String SEPARATOR = "-".repeat(72);

    @Inject
    private dev.nichar.facepalm.FacepalmConfig context;

    private final Log log;

    private final Configuration cfg;

    @Inject
    public Reporter(@Nullable Log log, FacepalmConfig context) {
        this.log = log != null ? log : new org.apache.maven.plugin.logging.SystemStreamLog();
        this.cfg = new Configuration(Configuration.VERSION_2_3_32);
        this.cfg.setClassForTemplateLoading(Reporter.class, "/templates");
        this.cfg.setDefaultEncoding("UTF-8");
        this.cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        this.cfg.setLogTemplateExceptions(false);
        this.cfg.setWrapUncheckedExceptions(true);
        this.cfg.setFallbackOnNullLoopVariable(false);
    }

    public void performReporting(List<Finding> findings,
                                 ScanStatistics stats,
                                 String rootPath,
                                 String version,
                                 Path outputPathBase) throws Exception {
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
        if (context.getScoring().isShowDetails()) {
            log.info("HTML Report generated at: " + outputPath.toAbsolutePath());
        }
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
        if (context.getScoring().isShowDetails()) {
            log.info("Sarif Report generated at: " + outputFile.toPath().toAbsolutePath());
        }
    }

    public ScanReport buildReport(List<Finding> findings,
                                  ScanStatistics stats,
                                  String rootPath,
                                  String version) {
        Map<String, List<Finding>> grouped = findings.stream()
            .collect(Collectors.groupingBy(f ->
                Base64.getEncoder().encodeToString((f.getPatternName() + ":" + f.getMaskedSecret()).getBytes())
            ));

        Map<String, ScanReport.RuleDefinition> ruleDict = new HashMap<>();

        List<ScanReport.UniqueLeak> leaks = grouped.entrySet().stream().map(entry -> {
                String fingerprint = entry.getKey();
                List<Finding> occs = entry.getValue();
                Finding primary = occs.get(0);

                ruleDict.putIfAbsent(
                    primary.getPatternName(), ScanReport.RuleDefinition.builder()
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

    public void printLogs(ScanStatistics stats, List<Finding> findings) {
        final var scoringConfig = context.getScoring();

        if (scoringConfig.isShowScoring()) {
            log.info(SEPARATOR);
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

                    //String snippet = f.getContextSnippet().trim();
                    //log.info("  Context : " + (snippet.length() > 80
                    //    ? snippet.substring(0, 77) + "..."
                    //    : snippet));

                    // Empty line between findings for readability, similar to test run logs
                    log.info("");
                });
        }

        long info = findings.stream().filter(f -> f.getSeverity(scoringConfig) == Severity.INFO).count();
        long errors = findings.stream().filter(f -> f.getSeverity(scoringConfig) == Severity.ERROR).count();
        long warnings = findings.stream().filter(f -> f.getSeverity(scoringConfig) == Severity.WARNING).count();

        if (scoringConfig.isShowDetails()) {
            log.info(SEPARATOR);

            log.info("Files discovered: " + stats.getFilesDiscovered().sum());
            log.info("Files excluded:   " + stats.getExclusionBreakdown().values().stream().mapToLong(LongAdder::sum).sum());
            if (log.isDebugEnabled()) {
                stats.getExclusionBreakdown().forEach(
                    (key, value) -> log.debug(String.format("  %s: %d", key.getDescription(), value.sum())));
            }
            log.info("Files scanned:    " + stats.getFilesScanned().sum());

            log.info(SEPARATOR);

            log.info("Total findings:   " + findings.size());
            log.info("Critical:         " + errors);
            log.info("Warnings:         " + warnings);
        }

        log.info(SEPARATOR);

        // Summary message based on severity
        String statusMessage;
        if (errors > 0) {
            statusMessage = "High-risk issues detected! Action required.";
        } else if (warnings > 0) {
            statusMessage = "Warnings detected. Review recommended.";
        } else if (info > 0) {
            statusMessage = "Informational findings detected.";
        } else {
            statusMessage = "No secrets or sensitive patterns detected. Your secrets are safe.";
        }

        // Determine scan result text
        final String scanResult;
        if (errors > 0) {
            scanResult = "FAILURE";
        } else if (warnings > 0) {
            scanResult = "WARNINGS";
        } else {
            scanResult = "SUCCESS";
        }

        // Determine status text based on findings

        log.info("SCAN RESULT : " + scanResult);
        log.info(SEPARATOR);
        log.info(statusMessage);
    }

    // Helper for Maven-style duration (e.g., 1.234 s)
    private String formatDuration(long millis) {
        return String.format("%.3f s", millis / 1000.0);
    }
}
