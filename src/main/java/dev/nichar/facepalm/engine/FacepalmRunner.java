package dev.nichar.facepalm.engine;

import java.nio.file.Path;
import java.util.List;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;

import dev.nichar.facepalm.report.Reporter;


@Named
@Singleton
public class FacepalmRunner {

    @Inject
    private @Nullable Log log;

    @Inject
    private dev.nichar.facepalm.FacepalmConfig context;

    @Inject
    private ScannerEngine engine;

    @Inject
    private GitIgnoreService gitIgnoreService;

    @Inject
    private Reporter reporter;

    private Log getLog() {
        return log != null ? log : new org.apache.maven.plugin.logging.SystemStreamLog();
    }

    public void run(Path root, Path outputDir, String version) throws Exception {
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
