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
import java.util.Set;

import com.google.inject.Guice;
import dev.nichar.facepalm.config.EngineConfig;
import dev.nichar.facepalm.config.EvaluatorConfig;
import dev.nichar.facepalm.config.PatternConfig;
import dev.nichar.facepalm.config.PostProcessorConfig;
import dev.nichar.facepalm.config.ScoringConfig;
import dev.nichar.facepalm.engine.FacepalmRunner;
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
     * The project base directory.
     */
    @Parameter(defaultValue = "${project.basedir}", readonly = true)
    private File baseDir;

    /**
     * The directory where scan reports and results will be generated.
     */
    @Parameter(defaultValue = "${project.build.directory}", readonly = true)
    private File outputDirectory;

    /**
     * The root directory for scanning operations.
     */
    @Parameter(property = "root")
    private File root;

    /**
     * The number of concurrent threads used for scanning.
     * Defaults to the number of available processors on the host system.
     */
    @Parameter(property = "threads")
    private Integer threads;

    /**
     * The maximum allowed size of a file (in bytes) to be scanned.
     * Default is 5MB.
     */
    @Parameter(property = "maxFileSizeBytes", defaultValue = "5242880")
    private long maxFileSizeBytes;

    /**
     * Regex to identify binary files.
     */
    @Parameter(
        property = "skipBinaryRegex",
        defaultValue = ".*\\.(png|jpg|jpeg|gif|pdf|zip|jar|class|tar|gz|exe|dll)$"
    )
    private String skipBinaryRegex;

    /**
     * Directories to skip.
     */
    @Parameter(property = "skipDirs")
    private Set<String> skipDirs;

    /**
     * Log processed files.
     */
    @Parameter(property = "showProcessed", defaultValue = "false")
    private boolean showProcessed;

    /**
     * Log skipped files.
     */
    @Parameter(property = "showSkipped", defaultValue = "false")
    private boolean showSkipped;

    /**
     * The score threshold (0-100) at or above which a finding is classified as a high-risk error.
     */
    @Parameter(property = "errorThreshold", defaultValue = "80")
    private int errorThreshold;

    /**
     * The score threshold (0-100) at or above which a finding is classified as a moderate-risk warning.
     */
    @Parameter(property = "warningThreshold", defaultValue = "40")
    private int warningThreshold;

    /**
     * When true, logs detailed scoring breakdown.
     */
    @Parameter(property = "showScoring", defaultValue = "false")
    private boolean showScoring;

    /**
     * When true, logs a detailed breakdown of files discovered, excluded,
     * and scanned, including per-extension counts and binary file detection.
     * Useful for debugging or auditing scan coverage. Defaults to false.
     */
    @Parameter(property = "showDetails", defaultValue = "false")
    private boolean showDetails;

    /**
     * Fail build if errorThreshold is reached.
     */
    @Parameter(property = "failOnError", defaultValue = "true")
    private boolean failOnError;

    /**
     * Fail build if warningThreshold is reached.
     */
    @Parameter(property = "failOnWarnings", defaultValue = "false")
    private boolean failOnWarnings;

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
        final var engine = new EngineConfig(threads, maxFileSizeBytes, skipBinaryRegex, skipDirs, showProcessed, showSkipped);
        final var scoring = new ScoringConfig(errorThreshold, warningThreshold, showScoring, showDetails, failOnError, failOnWarnings);
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

        final var runner = injector.getInstance(FacepalmRunner.class);
        final var config = injector.getInstance(FacepalmConfig.class);
        final var log = injector.getInstance(Log.class);

        if (runner == null || config == null || log == null) {
            throw new MojoExecutionException("Facepalm Mojo initialization failed: " +
                "runner=" + runner + ", config=" + config + ", log=" + log);
        }

        log.info("Starting facepalm-maven-plugin " + pluginDescriptor.getVersion());

        final var rootFile = this.root != null ? this.root : baseDir;
        final var rootPath = rootFile.toPath().toAbsolutePath().normalize();

        try {
            runner.run(rootPath, outputDirectory.toPath(), pluginDescriptor.getVersion());
        } catch (MojoFailureException e) {
            throw e;
        } catch (Exception e) {
            throw new MojoExecutionException("Unknown exception", e);
        }
    }
}
