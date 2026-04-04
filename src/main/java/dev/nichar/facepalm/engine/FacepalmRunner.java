package dev.nichar.facepalm.engine;

import java.nio.file.Path;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;

import dev.nichar.facepalm.FacepalmConfig;
import dev.nichar.facepalm.report.Reporter;


/**
 * Orchestrates the full lifecycle of a Facepalm scan, from gitignore loading and scanning to reporting.
 * It manages component execution and determines if the Maven build should fail based on finding severity.
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
     * Executes the end-to-end scanning workflow and evaluates results against configured failure conditions.
     *
     * @param root The base directory path to start the scan.
     * @param outputDir The directory where report artifacts should be saved.
     * @param version The version string of the project being scanned.
     * @throws Exception If any step in the scanning or reporting process fails.
     */
    public void run(@Nonnull final Path root,
                    @Nonnull final Path outputDir,
                    @Nonnull final String version) throws Exception {

        getLog().info("Scanning " + root);

        gitIgnoreService.loadAllGitIgnores(root);
        List<Finding> findings = engine.scan(root);
        ScanStatistics stats = engine.getStats();
        reporter.printLogs(stats, findings);
        reporter.performReporting(findings, stats, root.toString(), version, outputDir);

        final var scoring = context.getScoring();
        long errors = findings.stream().filter(f -> f.getSeverity(scoring) == Severity.ERROR).count();
        long warnings = findings.stream().filter(f -> f.getSeverity(scoring) == Severity.WARNING).count();
        checkFailureConditions(errors, warnings);
    }

    /**
     * Analyzes finding counts and throws a MojoFailureException if the project exceeds allowed thresholds.
     *
     * @param errors The total count of critical findings.
     * @param warnings The total count of warning-level findings.
     * @throws MojoFailureException If the scan results violate the project's failure configuration.
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
