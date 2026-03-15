package dev.nichar.facepalm;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.sisu.launch.SisuExtensions;
import org.eclipse.sisu.plexus.Strategies;
import org.eclipse.sisu.space.ClassSpace;
import org.eclipse.sisu.space.SpaceModule;
import org.eclipse.sisu.space.URLClassSpace;
import org.eclipse.sisu.wire.WireModule;

import java.io.File;
import java.nio.file.Path;

import com.google.inject.Guice;
import com.google.inject.Injector;


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
    //@Inject private FacepalmScanner.FacepalmContext context;
    //@Inject private FacepalmScanner.FacepalmRunner runner;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        // 1️⃣ Create the config object
        FacepalmConfig config = new FacepalmConfig(engine, scoring, evaluators, postProcessing, patterns);

        // 2️⃣ Create the ClassSpace for Sisu scanning
        ClassSpace space = new URLClassSpace(getClass().getClassLoader());
        // 3️⃣ Build the injector
        Injector injector = Guice.createInjector(
            new WireModule(
                new SpaceModule(space),
                new FacepalmScanner.LogModule(getLog()),
                new FacepalmScanner.FacepalmConfigModule(config)
            )
        );
        // 4️⃣ Get runner and config
        FacepalmScanner.FacepalmRunner runner = injector.getInstance(FacepalmScanner.FacepalmRunner.class);
        FacepalmConfig context = injector.getInstance(FacepalmConfig.class);

        getLog().info("Configuration " + context.get());
        Path root = baseDir.toPath().toAbsolutePath().normalize();
        try {
            // Pass the configuration down the stack. Do not rely on injected state.
            runner.run(root, outputDirectory.toPath(), "1.0.0");
        } catch (MojoFailureException e) {
            throw e;
        } catch (Exception e) {
            throw new MojoExecutionException("Error during facepalm scan", e);
        } finally {
            //context.clear();
        }
    }

}
