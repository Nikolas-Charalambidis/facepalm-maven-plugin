package dev.nichar.facepalm;

import com.google.inject.Guice;
import dev.nichar.facepalm.config.EngineConfig;
import dev.nichar.facepalm.config.EvaluatorConfig;
import dev.nichar.facepalm.config.PatternConfig;
import dev.nichar.facepalm.config.PostProcessorConfig;
import dev.nichar.facepalm.config.ScoringConfig;
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
 * Command-line interface entry point for executing Facepalm scans outside a Maven lifecycle.
 * This class bootstraps the Sisu/Guice container manually to provide the same dependency injection environment as the Maven plugin.
 *
 * @author Nikolas Charalambidis
 * @since 1.0.0
 */
public class FacepalmCLI {

    /**
     * Main entry point for the CLI application.
     *
     * @param args Command line arguments; args[0] is optionally the target directory to scan.
     * @throws MojoFailureException If the scan identifies critical security findings.
     * @throws MojoExecutionException If an unexpected error occurs during execution.
     */
    public static void main(String[] args) throws MojoFailureException, MojoExecutionException {

        // Initializes the default configuration suite for the scanner engine and evaluators.
        final var effectiveConfig = new FacepalmConfig(
            new EngineConfig(),
            new ScoringConfig(),
            new EvaluatorConfig(),
            new PostProcessorConfig(),
            new PatternConfig());

        // Creates a Sisu ClassSpace to index classes and resources from the current ClassLoader.
        // This enables the scanner to discover components like {@code @Named} evaluators within the plugin.
        final var space = new URLClassSpace(FacepalmCLI.class.getClassLoader());

        // Initializes the Guice injector to orchestrate dependency injection.
        final var injector = Guice.createInjector(
            // Wraps modules to enable Sisu's advanced wiring, such as automatic List aggregation.
            new WireModule(
                // Scans the ClassSpace for @Named components to register them in the container.
                // Uses CACHE to perform a runtime brute-force scan of the classpath for @Named components.
                // This provides a "fail-safe" mode for IDE execution (e.g., IntelliJ/Eclipse).
                // The reason is the Maven-generated index might be missing or out of sync with the compiled classes.
                new SpaceModule(space, BeanScanning.CACHE),
                // Bridges the Maven Log interface to a standard ConsoleLogger for CLI output so injected services can perform plugin logging.
                new FacepalmLogModule(
                    new DefaultLog(
                        new ConsoleLogger(Logger.LEVEL_DEBUG, "facepalm-cli"))),
                // Binds the immutable configuration into the Guice context to be available for injection.
                new FacepalmConfigModule(effectiveConfig)
            )
        );

        // Maven (Plexus/Sisu) instantiates the Mojo before your custom Guice injector exists, preventing @Inject from recognizing your local modules.
        // Manually bootstrapping the injector with @Parameter fields is necessary to bridge the gap between Maven's lifecycle and your engine's dependencies.

        final var runner = injector.getInstance(FacepalmScanner.FacepalmRunner.class);
        final var config = injector.getInstance(FacepalmConfig.class);
        final var log = injector.getInstance(Log.class);

        log.info("Configuration: " + config);

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
