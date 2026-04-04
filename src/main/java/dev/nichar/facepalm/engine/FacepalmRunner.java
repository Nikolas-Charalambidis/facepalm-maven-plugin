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
 * Orchestrates the scan lifecycle from gitignore loading to reporting.
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
     * Runs the end-to-end scanning workflow and evaluates results.
     *
     * @param root Base directory to scan.
     * @param outputDir Directory for report artifacts.
     * @param version Project version being scanned.
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
     * Fails the build if findings exceed configured thresholds.
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
