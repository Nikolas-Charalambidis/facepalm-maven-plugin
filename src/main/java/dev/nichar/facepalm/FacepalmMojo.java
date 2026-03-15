package dev.nichar.facepalm;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.sisu.space.BeanScanning;
import org.eclipse.sisu.space.SpaceModule;
import org.eclipse.sisu.space.URLClassSpace;
import org.eclipse.sisu.wire.WireModule;

import java.io.File;

import com.google.inject.Guice;
import dev.nichar.facepalm.config.EngineConfig;
import dev.nichar.facepalm.config.EvaluatorConfig;
import dev.nichar.facepalm.config.PatternConfig;
import dev.nichar.facepalm.config.PostProcessorConfig;
import dev.nichar.facepalm.config.ScoringConfig;
import dev.nichar.facepalm.module.FacepalmConfigModule;
import dev.nichar.facepalm.module.FacepalmLogModule;


/**
 * Maven Mojo that executes the Facepalm security scan during the {@code verify} lifecycle phase.
 * It bootstraps a custom Guice/Sisu environment to perform dependency injection for the scanning engine.
 *
 * @author Nikolas Charalambidis
 * @since 1.0.0
 */
@Mojo(name = "scan", defaultPhase = LifecyclePhase.VERIFY, threadSafe = true)
public class FacepalmMojo extends AbstractMojo {

    /**
     * Descriptor of the current plugin, providing access to the artifact version defined in pom.xml.
     */
    @Parameter(defaultValue = "${plugin}", readonly = true)
    private PluginDescriptor pluginDescriptor;

    /**
     * The project base directory used as the root for scanning operations.
     */
    @Parameter(defaultValue = "${project.basedir}", readonly = true)
    private File baseDir;

    /**
     * The directory where scan reports and results will be generated.
     */
    @Parameter(defaultValue = "${project.build.directory}", readonly = true)
    private File outputDirectory;

    /**
     * Configuration for the scanning engine execution and file filtering.
     */
    @Parameter
    private EngineConfig engine = new EngineConfig();

    /**
     * Configuration for how findings are scored and when the build should be interrupted.
     */
    @Parameter
    private ScoringConfig scoring = new ScoringConfig();

    /**
     * Heuristic configuration used to evaluate the risk and legitimacy of discovered secrets.
     */
    @Parameter
    private EvaluatorConfig evaluators = new EvaluatorConfig();

    /**
     * Configuration for defining or overriding secret detection patterns.
     */
    @Parameter
    private PatternConfig patterns = new PatternConfig();

    /**
     * Configuration for noise reduction and cleanup after the initial scan.
     */
    @Parameter
    private PostProcessorConfig postProcessing = new PostProcessorConfig();

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        // Assembles the validated configuration suite from Maven parameters into an immutable DTO for the engine.
        final var effectiveConfig = new FacepalmConfig(engine, scoring, evaluators, postProcessing, patterns);

        // Creates a Sisu ClassSpace to index classes and resources from the current ClassLoader.
        // This enables the scanner to discover components like {@code @Named} evaluators within the plugin.
        final var space = new URLClassSpace(getClass().getClassLoader());

        // Initializes the Guice injector to orchestrate dependency injection.
        final var injector = Guice.createInjector(
            // Wraps modules to enable Sisu's advanced wiring, such as automatic List aggregation.
            new WireModule(
                // Scans the ClassSpace for @Named components to register them in the container.
                // Uses INDEX to leverage the pre-compiled JSR330 metadata generated during the Maven build.
                // This is the most efficient strategy for production plugins.
                // It avoids expensive classpath scanning by reading a static map of components, ensuring rapid startup.
                new SpaceModule(space, BeanScanning.INDEX),
                // Binds the Maven Log instance so injected services can perform plugin logging.
                new FacepalmLogModule(getLog()),
                // Binds the immutable configuration into the Guice context to be available for injection.
                new FacepalmConfigModule(effectiveConfig)
            )
        );

        // Maven (Plexus/Sisu) instantiates the Mojo before your custom Guice injector exists, preventing @Inject from recognizing your local modules.
        // Manually bootstrapping the injector with @Parameter fields is necessary to bridge the gap between Maven's lifecycle and your engine's dependencies.

        final var runner = injector.getInstance(FacepalmScanner.FacepalmRunner.class);
        final var config = injector.getInstance(FacepalmConfig.class);
        final var log = injector.getInstance(Log.class);

        log.info("Version: " + pluginDescriptor.getVersion());
        log.info("Configuration: " + config);

        final var root = baseDir.toPath().toAbsolutePath().normalize();

        try {
            runner.run(root, outputDirectory.toPath(), pluginDescriptor.getVersion());
        } catch (MojoFailureException e) {
            throw e;
        } catch (Exception e) {
            throw new MojoExecutionException("Unknown exception", e);
        }
    }
}
