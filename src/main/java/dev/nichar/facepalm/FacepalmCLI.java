package dev.nichar.facepalm;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.multibindings.Multibinder;

import javax.inject.Inject;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.apache.maven.monitor.logging.DefaultLog;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.eclipse.sisu.space.BeanScanning;
import org.eclipse.sisu.space.SpaceModule;
import org.eclipse.sisu.space.URLClassSpace;
import org.eclipse.sisu.wire.WireModule;


public class FacepalmCLI {

    @Inject
    private FacepalmScanner.FacepalmRunner runner;

    @Inject
    private FacepalmScanner.ScannerEngine engine;

    @Inject
    private FacepalmScanner.GitIgnoreService gitIgnoreService;

    @Inject
    private FacepalmScanner.Reporter reporter;

    @Inject
    private FacepalmScanner.ScoringConfig scoring;

    public static void main(String[] args) {
        Path root = Paths.get(args.length > 0 ? args[0] : ".").toAbsolutePath().normalize();

        // 1. Automatic Bootstrap
        // Changed to BeanScanning.CACHE so Sisu gracefully falls back to dynamic
        // classpath scanning when running inside an IDE without the Maven Sisu index.
        Injector injector = Guice.createInjector(
            new WireModule(
                new SpaceModule(new URLClassSpace(FacepalmCLI.class.getClassLoader()), BeanScanning.CACHE),
                new CliModule()
            )
        );

        // 2. Get your entry point
        FacepalmCLI cli = injector.getInstance(FacepalmCLI.class);

        // 3. Execute
        cli.run(root);
    }
    private void run(Path root) {
        try {
            runner.run(root, scoring, Paths.get("target"), "1.0.0-CLI");
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void run2(Path root) {
        gitIgnoreService.loadAllGitIgnores(root);
        try {
            List<FacepalmScanner.Finding> findings = engine.scan(root);
            FacepalmScanner.ScanStatistics stats = engine.getStats();

            reporter.printResults(findings, scoring);
            reporter.printStats(stats);

            Path outputDir = Paths.get("target");
            reporter.performReporting(findings, stats, root.toString(), "1.0.0-CLI", outputDir);

            long errors = findings.stream().filter(f -> f.getSeverity(scoring) == FacepalmScanner.Severity.ERROR).count();
            long warnings = findings.stream().filter(f -> f.getSeverity(scoring) == FacepalmScanner.Severity.WARNING).count();

            if (!scoring.isFailOnError() && scoring.isFailOnWarnings()) {
                //log.warn("So weird configuraiton");
            }
            if (scoring.isFailOnError() && errors > 0) {
                throw new MojoFailureException("TODO");
            }
            if (scoring.isFailOnWarnings() && warnings > 0) {
                throw new MojoFailureException("TODO");
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static class CliModule extends AbstractModule {
        @Override
        protected void configure() {

            // 1. Initialize the "Old Guard": The Plexus ConsoleLogger.
            // This is the underlying engine that Maven uses for its internal infrastructure.
            // We set the name to "facepalm-cli" so the output looks like a real Maven component.
            final var plexusLogger = new ConsoleLogger(Logger.LEVEL_DEBUG, "facepalm-cli");

            // 2. Wire the infrastructure: Satisfy classes that depend on the raw Plexus Logger.
            // Even if we don't use it directly, Sisu/Plexus components in the background expect this.
            bind(Logger.class).toInstance(plexusLogger);

            // 3. The Bridge: Map the modern Maven Log API to the Plexus engine using DefaultLog.
            // DefaultLog acts as the "translator," allowing our scanning logic to use the
            // standard Maven Log interface while achieving the authentic Maven "look-and-feel"
            // by delegating to the Plexus engine we set up above.
            bind(Log.class).toInstance(new DefaultLog(plexusLogger));
        }
    }
}
