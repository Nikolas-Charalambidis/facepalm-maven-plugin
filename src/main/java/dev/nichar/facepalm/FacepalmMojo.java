package dev.nichar.facepalm;

import lombok.Data;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import javax.inject.Inject;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

@Mojo(name = "scan", defaultPhase = LifecyclePhase.VERIFY)
public class FacepalmMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project.basedir}", readonly = true)
    private File baseDir;

    @Parameter(defaultValue = "${project.build.directory}", readonly = true)
    private File outputDirectory;

    // @Parameter types are now the canonical Scanner configs — no more duplicates.
    // Maven instantiates these via reflection (new FacepalmScanner.EngineConfig()),
    // so the @Singleton annotation on Sisu-managed instances doesn't interfere.
    @Parameter
    private FacepalmScanner.EngineConfig engine = new FacepalmScanner.EngineConfig();

    @Parameter
    private FacepalmScanner.ScoringConfig scoring = new FacepalmScanner.ScoringConfig();

    @Parameter
    private FacepalmScanner.EvaluatorConfig evaluators = new FacepalmScanner.EvaluatorConfig();

    @Parameter
    private FacepalmScanner.PostProcessorConfig postProcessing = new FacepalmScanner.PostProcessorConfig();

    @Parameter
    private FacepalmScanner.PatternConfig patterns = new FacepalmScanner.PatternConfig();

    // These flags don't live on the config classes because they're Mojo-lifecycle concerns
    @Parameter(defaultValue = "false") private boolean showDetails;
    @Parameter(defaultValue = "false") private boolean showProcessed;
    @Parameter(defaultValue = "false") private boolean showSkipped;
    @Parameter(defaultValue = "true")  private boolean failOnError;
    @Parameter(defaultValue = "false") private boolean failOnWarnings;

    @Inject private FacepalmScanner.FacepalmRunner sisuFacepalmRunner;
    @Inject private FacepalmScanner.EngineConfig sisuEngine;
    @Inject private FacepalmScanner.ScoringConfig sisuScoring;
    @Inject private FacepalmScanner.EvaluatorConfig sisuEval;
    @Inject private FacepalmScanner.PostProcessorConfig sisuPost;
    @Inject private FacepalmScanner.PatternConfig sisuPatterns;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Path root = baseDir.toPath().toAbsolutePath().normalize();

        applyConfig();
        try {
            sisuFacepalmRunner.run(root, sisuScoring, outputDirectory.toPath(), "1.0.0");
        } catch (MojoFailureException e) {
            throw e;
        } catch (Exception e) {
            throw new MojoExecutionException("Error during facepalm scan", e);
        }
    }

    /**
     * Bridges Maven @Parameter-populated instances into the Sisu-managed singletons
     * that the engine and evaluators hold references to.
     */
    private void applyConfig() {
        sisuEngine.setThreads(engine.getThreads());
        sisuEngine.setMaxFileSizeBytes(engine.getMaxFileSizeBytes());
        sisuEngine.setSkipBinaryRegex(engine.getSkipBinaryRegex());
        sisuEngine.setSkipDirs(engine.getEffectiveSkipDirs());
        sisuEngine.setShowProcessed(showProcessed);
        sisuEngine.setShowSkipped(showSkipped);

        sisuScoring.setErrorThreshold(scoring.getErrorThreshold());
        sisuScoring.setWarningThreshold(scoring.getWarningThreshold());
        sisuScoring.setShowDetails(showDetails);
        sisuScoring.setFailOnError(failOnError);
        sisuScoring.setFailOnWarnings(failOnWarnings);

        if (evaluators.getInterpolationPatternRegex() != null) sisuEval.setInterpolationPatternRegex(evaluators.getInterpolationPatternRegex());
        if (evaluators.getHighRiskExtensions() != null)        sisuEval.setHighRiskExtensions(evaluators.getHighRiskExtensions());
        if (evaluators.getLowRiskExtensions() != null)         sisuEval.setLowRiskExtensions(evaluators.getLowRiskExtensions());
        if (evaluators.getDummyKeywords() != null)             sisuEval.setDummyKeywords(evaluators.getDummyKeywords());

        sisuPost.setHighVolumeThreshold(postProcessing.getHighVolumeThreshold());

        if (patterns.getOverrides() != null) sisuPatterns.setOverrides(patterns.getOverrides());
    }
}
