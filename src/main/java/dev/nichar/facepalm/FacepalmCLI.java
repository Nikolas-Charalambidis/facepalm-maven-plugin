package dev.nichar.facepalm;

import com.google.inject.Guice;
import dev.nichar.facepalm.config.EngineConfig;
import dev.nichar.facepalm.config.EvaluatorConfig;
import dev.nichar.facepalm.config.PatternConfig;
import dev.nichar.facepalm.config.PostProcessorConfig;
import dev.nichar.facepalm.config.ScoringConfig;
import dev.nichar.facepalm.engine.FacepalmRunner;
import dev.nichar.facepalm.module.FacepalmConfigModule;
import dev.nichar.facepalm.module.FacepalmLogModule;

import org.apache.maven.monitor.logging.DefaultLog;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.eclipse.sisu.space.BeanScanning;
import org.eclipse.sisu.space.SpaceModule;
import org.eclipse.sisu.space.URLClassSpace;
import org.eclipse.sisu.wire.WireModule;

import java.nio.file.Paths;


/**
 * CLI entry point for running Facepalm scans outside of Maven.
 */
public class FacepalmCLI {

    /**
     * Bootstraps the Guice container and executes a scan on the target directory.
     *
     * @param args Command line arguments; args[0] is optionally the target directory.
     */
    public static void main(final String[] args) throws MojoFailureException, MojoExecutionException {

        // Initialize default configuration.
        final var engineConfig = new EngineConfig();
        final var scoringConfig = new ScoringConfig();
        final var evaluatorConfig = new EvaluatorConfig();
        final var postProcessorConfig = new PostProcessorConfig();
        final var patternConfig = new PatternConfig();

        final var effectiveConfig = new FacepalmConfig(engineConfig, scoringConfig, evaluatorConfig, postProcessorConfig, patternConfig);

        // Index classes and resources to discover components like {@code @Named} evaluators.
        final var space = new URLClassSpace(FacepalmCLI.class.getClassLoader());

        // Initialize Guice for dependency injection.
        final var injector = Guice.createInjector(
            new WireModule(
                // Perform a runtime scan of the classpath for @Named components.
                new SpaceModule(space, BeanScanning.CACHE),
                // Bridge Maven Log to a ConsoleLogger for CLI output.
                new FacepalmLogModule(
                    new DefaultLog(
                        new ConsoleLogger(Logger.LEVEL_DEBUG, "facepalm-cli"))),
                // Bind the configuration into the Guice context.
                new FacepalmConfigModule(effectiveConfig)
            )
        );

        // Manually retrieve instances as the CLI environment lacks automatic injection.
        final var runner = injector.getInstance(FacepalmRunner.class);
        final var config = injector.getInstance(FacepalmConfig.class);
        final var log = injector.getInstance(Log.class);

        if (runner == null || config == null || log == null) {
            throw new MojoExecutionException("Facepalm Mojo initialization failed: " +
                "runner=" + runner + ", config=" + config + ", log=" + log);
        }

        final var root = Paths.get(args.length > 0 ? args[0] : ".").toAbsolutePath().normalize();

        try {
            runner.run(root,  Paths.get("target"), "SNAPSHOT");
        } catch (MojoFailureException e) {
            throw e;
        } catch (Exception e) {
            throw new MojoExecutionException("Unknown exception", e);
        }
    }
}
