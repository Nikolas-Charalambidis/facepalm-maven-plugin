package dev.nichar.facepalm.engine;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.nichar.facepalm.FacepalmConfig;
import dev.nichar.facepalm.config.ScoringConfig;
import dev.nichar.facepalm.report.FindingReport;
import dev.nichar.facepalm.report.Reporter;


/**
 * Orchestrates the scanning lifecycle.
 * Manages gitignore discovery, extraction, scoring, and report serialization.
 */
@Named
@Singleton
public class FacepalmRunner {

    @Inject
    private @Nullable Log log;

    @Inject
    private FacepalmConfig context;

    @Inject
    private ScannerEngine engine;

    @Inject
    private GitIgnoreService gitIgnoreService;

    @Inject
    private Reporter reporter;

    private Log getLog() {
        return log != null ? log : new SystemStreamLog();
    }

    /**
     * Executes the end-to-end scanning workflow.
     * Evaluates build failure conditions based on discovery results.
     *
     * @param root Base directory to scan.
     * @param outputDir Target directory for findings and reports.
     * @param version Scanner version for metadata.
     */
    public void run(@Nonnull final Path root,
                    @Nonnull final Path outputDir,
                    @Nonnull final String version) throws Exception {

        getLog().info("Scanning " + root);

        gitIgnoreService.loadAllGitIgnores(root);
        List<Finding> findings = engine.scan(root);
        ScanStatistics stats = engine.getStats();

        reporter.printLogs(stats, findings);

        // Persist findings for downstream report generation.
        final var resultsFile = new File(outputDir.toFile(), "facepalm-findings.json");
        saveFindingsToJson(findings, resultsFile);

        // Enforce build failure policies based on finding severity.
        final var scoring = context.getScoring();
        final long errors = findings.stream().filter(f -> f.getSeverity(scoring) == Severity.ERROR).count();
        final long warnings = findings.stream().filter(f -> f.getSeverity(scoring) == Severity.WARNING).count();
        checkFailureConditions(errors, warnings);
    }

    /**
     * Serializes findings to a machine-readable JSON format for the reporting phase.
     */
    private void saveFindingsToJson(List<Finding> findings, File outputFile) throws Exception {
        if (!outputFile.getParentFile().exists()) {
            outputFile.getParentFile().mkdirs();
        }

        final var findingsDto = findings.stream()
            .map(finding -> mapToDto(finding, context.getScoring()))
            .toList();

        final var mapper = new ObjectMapper();
        mapper.writerWithDefaultPrettyPrinter().writeValue(outputFile, findingsDto);
    }

    /**
     * Maps a raw scan finding to a serializable DTO.
     */
    public FindingReport mapToDto(Finding finding, ScoringConfig config) {
        return FindingReport.builder()
            .patternName(finding.getPatternName())
            .fileAbsolutePath(finding.getContext().getPath().toAbsolutePath().toString())
            .lineNumber(finding.getLineNumber())
            .maskedSecret(finding.getMaskedSecret())
            .contextSnippet(finding.getContextSnippet())
            .finalScore(finding.getNumericScore())
            .finalSeverity(finding.getSeverity(config).name())
            .riskScore(finding.getRiskScore())
            .confidenceScore(finding.getConfidenceScore())
            .build();
    }

    /**
     * Terminates the build if findings exceed configured threat thresholds.
     */
    private void checkFailureConditions(long errors, long warnings) throws MojoFailureException {
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
