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

import jakarta.annotation.Nullable;
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


/**
 * Handles the generation of security reports and console logging.
 * Supports HTML and SARIF formats for integration with CI/CD and developer workflows.
 */
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
        cfg = new Configuration(Configuration.VERSION_2_3_32);
        cfg.setClassForTemplateLoading(Reporter.class, "/templates");
        cfg.setDefaultEncoding("UTF-8");
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        cfg.setLogTemplateExceptions(false);
        cfg.setWrapUncheckedExceptions(true);
        cfg.setFallbackOnNullLoopVariable(false);
    }

    /**
     * Executes the full reporting pipeline, including console logs and file generation.
     */
    @SuppressWarnings("unused") // TODO: SARIF + HTML reporting.
    public void performReporting(List<Finding> findings,
                                 ScanStatistics stats,
                                 String rootPath,
                                 String version,
                                 Path outputPathBase) throws Exception {
        ScanReport report = buildReport(findings, stats, rootPath, version);
        generateHtml(report, outputPathBase.resolve("facepalm-report.html"));
        generateSarif(report, outputPathBase.resolve("facepalm-report.sarif").toFile());
    }

    /**
     * Generates an HTML report using Freemarker templates.
     */
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

    /**
     * Generates a SARIF report for integration with standard security analysis tools.
     */
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

    /**
     * Aggregates raw discovery findings into a structured report model.
     */
    public ScanReport buildReport(List<Finding> findings,
                                  ScanStatistics stats,
                                  String rootPath,
                                  String version) {
        // Group findings by unique secret fingerprints for deduplicated reporting.
        Map<String, List<Finding>> grouped = findings.stream()
            .collect(Collectors.groupingBy(f ->
                Base64.getEncoder().encodeToString((f.getPatternName() + ":" + f.getMaskedSecret()).getBytes())
            ));

        Map<String, ScanReport.RuleDefinition> ruleDict = new HashMap<>();

        List<ScanReport.UniqueLeak> leaks = grouped.entrySet().stream().map(entry -> {
                String fingerprint = entry.getKey();
                List<Finding> occs = entry.getValue();
                Finding primary = occs.get(0);

                // Initialize metadata for the primary detection pattern.
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
                    .hash(fingerprint)
                    .scoreHistory(primary.getScoreHistory())
                    .occurrences(occs.stream().map(f -> ScanReport.Occurrence.builder()
                        .relativePath(f.getContext().getPath().toString())
                        .absolutePath(f.getContext().getPath().toAbsolutePath().toString())
                        .lineNumber(f.getLineNumber())
                        .snippet(f.getContextSnippet())
                        .build()).collect(Collectors.toList()))
                    .build();
            })
            // Sort discoveries by descending threat score.
            .sorted(Comparator.comparing(ScanReport.UniqueLeak::getAggregateScore).reversed())
            .collect(Collectors.toList());

        return ScanReport.builder()
            .metadata(ScanReport.RunMetadata.builder()
                .scannerVersion(version)
                .timestamp(Instant.now())
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

    /**
     * Logs discovery findings and scan statistics to the Maven console.
     */
    public void printLogs(ScanStatistics stats, List<Finding> findings) {
        final var scoringConfig = context.getScoring();

        if (scoringConfig.isShowScoring()) {
            log.info(SEPARATOR);
            findings.stream()
                .filter(f -> f.getSeverity(scoringConfig) != Severity.INFO)
                .sorted(Comparator.comparing(Finding::getNumericScore).reversed())
                .forEach(f -> {
                    Severity sev = f.getSeverity(scoringConfig);

                    String message = String.format(
                        "[%s] Score: %.1f (R:%d/C:%d) - %s",
                        f.getPatternName(), f.getNumericScore(),
                        f.getRiskScore(), f.getConfidenceScore(),
                        f.getSeverity(scoringConfig).getIcon());

                    // Emit high-risk findings at the error level for visibility.
                    if (sev == Severity.ERROR) {
                        log.error(message);
                    } else {
                        log.warn(message);
                    }

                    log.info("  Location: " + f.getContext().getPath() + ":" + f.getLineNumber());
                    log.info("");
                });
        }

        long info = findings.stream().filter(f -> f.getSeverity(scoringConfig) == Severity.INFO).count();
        long errors = findings.stream().filter(f -> f.getSeverity(scoringConfig) == Severity.ERROR).count();
        long warnings = findings.stream().filter(f -> f.getSeverity(scoringConfig) == Severity.WARNING).count();

        // Emit summary statistics if verbose detail is enabled.
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

        // Resolve scan status based on meeting configured thresholds.
        final String scanResult;
        if (errors > 0) {
            scanResult = "FAILURE";
        } else if (warnings > 0) {
            scanResult = "WARNINGS";
        } else {
            scanResult = "SUCCESS";
        }

        log.info("SCAN RESULT : " + scanResult);
        log.info(SEPARATOR);
        log.info(statusMessage);
    }

    private String formatDuration(long millis) {
        return String.format("%.3f s", millis / 1000.0);
    }
}
