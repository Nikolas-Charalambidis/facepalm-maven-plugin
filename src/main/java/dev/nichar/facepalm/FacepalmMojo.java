package dev.nichar.facepalm;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import javax.inject.Inject;
import java.io.File;
import java.nio.file.Path;

@Mojo(name = "scan", defaultPhase = LifecyclePhase.VERIFY, threadSafe = true)
public class FacepalmMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project.basedir}", readonly = true)
    private File baseDir;

    @Parameter(defaultValue = "${project.build.directory}", readonly = true)
    private File outputDirectory;

    // Maven natively populates this complex object if you use nested XML
    // tags like <config><engine><threads>4</threads></engine></config>
    @Parameter private FacepalmScanner.EngineConfig engine = new FacepalmScanner.EngineConfig();
    @Parameter private FacepalmScanner.ScoringConfig scoring = new FacepalmScanner.ScoringConfig();
    @Parameter private FacepalmScanner.EvaluatorConfig evaluators = new FacepalmScanner.EvaluatorConfig();
    @Parameter private FacepalmScanner.PostProcessorConfig postProcessing = new FacepalmScanner.PostProcessorConfig();
    @Parameter private FacepalmScanner.PatternConfig patterns = new FacepalmScanner.PatternConfig();

    // Only inject stateless services
    @Inject private FacepalmScanner.FacepalmContext context;
    @Inject private FacepalmScanner.FacepalmRunner runner;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Path root = baseDir.toPath().toAbsolutePath().normalize();
        final var config = new FacepalmConfig(engine, scoring, evaluators, postProcessing, patterns);
        getLog().info("Configuration " + config);
        context.set(config);

        try {
            // Pass the configuration down the stack. Do not rely on injected state.
            runner.run(root, outputDirectory.toPath(), "1.0.0");
        } catch (MojoFailureException e) {
            throw e;
        } catch (Exception e) {
            throw new MojoExecutionException("Error during facepalm scan", e);
        } finally {
            context.clear();
        }
    }
}
