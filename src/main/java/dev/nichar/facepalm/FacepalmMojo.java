package dev.nichar.facepalm;

import static dev.nichar.facepalm.configurator.CommaSeparatedConfigurator.COMMA_SEPARATED_CONFIGURATOR;

import java.io.File;
import java.util.Set;

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
 * Maven Mojo that runs Facepalm security scans during the {@code verify} phase.
 */
@Mojo(name = "scan", defaultPhase = LifecyclePhase.VERIFY, threadSafe = true, configurator = COMMA_SEPARATED_CONFIGURATOR)
public class FacepalmMojo extends AbstractMojo {

    /**
     * Plugin descriptor for version access.
     */
    @Parameter(defaultValue = "${plugin}", readonly = true)
    private PluginDescriptor pluginDescriptor;

    /**
     * Project base directory.
     */
    @Parameter(defaultValue = "${project.basedir}", readonly = true)
    private File baseDir;

    /**
     * Directory for scan reports.
     */
    @Parameter(defaultValue = "${project.build.directory}", readonly = true)
    private File outputDirectory;

    /**
     * Root directory for scanning.
     */
    @Parameter(property = "root")
    private File root;

    /**
     * Concurrent threads for scanning; defaults to available processors.
     */
    @Parameter(property = "threads")
    private Integer threads;

    /**
     * Maximum file size in bytes (default 5MB).
     */
    @Parameter(property = "maxFileSizeBytes", defaultValue = "5242880")
    private long maxFileSizeBytes;

    /**
     * Regex to skip binary files.
     */
    @Parameter(property = "skipBinaryRegex", defaultValue = ".*\\.(png|jpg|jpeg|gif|pdf|zip|jar|class|tar|gz|exe|dll)$")
    private String skipBinaryRegex;

    /**
     * Directories to exclude from scanning.
     */
    @Parameter(property = "skipDirs")
    private Set<String> skipDirs;

    /**
     * Log every processed file.
     */
    @Parameter(property = "showProcessed", defaultValue = "false")
    private boolean showProcessed;

    /**
     * Log every skipped file.
     */
    @Parameter(property = "showSkipped", defaultValue = "false")
    private boolean showSkipped;

    /**
     * Score threshold (0-100) for high-risk findings.
     */
    @Parameter(property = "errorThreshold", defaultValue = "80")
    private int errorThreshold;

    /**
     * Score threshold (0-100) for moderate-risk findings.
     */
    @Parameter(property = "warningThreshold", defaultValue = "40")
    private int warningThreshold;

    /**
     * Log detailed scoring breakdowns.
     */
    @Parameter(property = "showScoring", defaultValue = "false")
    private boolean showScoring;

    /**
     * Log discovery and exclusion details for auditing scan coverage.
     */
    @Parameter(property = "showDetails", defaultValue = "false")
    private boolean showDetails;

    /**
     * Fail build if any finding reaches the error threshold.
     */
    @Parameter(property = "failOnError", defaultValue = "true")
    private boolean failOnError;

    /**
     * Fail build if any finding reaches the warning threshold.
     */
    @Parameter(property = "failOnWarnings", defaultValue = "false")
    private boolean failOnWarnings;

    /**
     * Configuration for risk and legitimacy evaluators.
     */
    @Parameter
    private EvaluatorConfig evaluators = new EvaluatorConfig();

    /**
     * Custom or override secret detection patterns.
     */
    @Parameter
    private PatternConfig patterns = new PatternConfig();

    /**
     * Post-processing configuration for noise reduction.
     */
    @Parameter
    private PostProcessorConfig postProcessing = new PostProcessorConfig();

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        // Assemble configuration from Maven parameters.
        final var engine = new EngineConfig(threads, maxFileSizeBytes, skipBinaryRegex, skipDirs, showProcessed, showSkipped);
        final var scoring = new ScoringConfig(errorThreshold, warningThreshold, showScoring, showDetails, failOnError, failOnWarnings);
        final var effectiveConfig = new FacepalmConfig(engine, scoring, evaluators, postProcessing, patterns);

        // Index classes and resources to discover components like {@code @Named} evaluators.
        final var space = new URLClassSpace(getClass().getClassLoader());

        // Initialize Guice for dependency injection.
        final var injector = Guice.createInjector(
            new WireModule(
                // Use pre-compiled JSR330 metadata for rapid startup.
                new SpaceModule(space, BeanScanning.INDEX),
                // Bind Maven Log and configuration.
                new FacepalmLogModule(getLog()),
                new FacepalmConfigModule(effectiveConfig)
            )
        );

        // Manual bootstrapping is needed because Maven instantiates the Mojo before the injector exists.
        final var runner = injector.getInstance(FacepalmRunner.class);
        final var config = injector.getInstance(FacepalmConfig.class);
        final var log = injector.getInstance(Log.class);

        if (runner == null || config == null || log == null) {
            throw new MojoExecutionException("Facepalm Mojo initialization failed: " +
                "runner=" + runner + ", config=" + config + ", log=" + log);
        }

        log.info("Starting facepalm-maven-plugin " + pluginDescriptor.getVersion());

        final var rootFile = root != null ? root : baseDir;
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
