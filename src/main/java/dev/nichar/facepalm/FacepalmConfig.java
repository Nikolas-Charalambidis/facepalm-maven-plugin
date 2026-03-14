package dev.nichar.facepalm;

import lombok.Data;

@Data
public class FacepalmConfig {
    private FacepalmScanner.EngineConfig engine = new FacepalmScanner.EngineConfig();
    private FacepalmScanner.ScoringConfig scoring = new FacepalmScanner.ScoringConfig();
    private FacepalmScanner.EvaluatorConfig evaluators = new FacepalmScanner.EvaluatorConfig();
    private FacepalmScanner.PostProcessorConfig postProcessing = new FacepalmScanner.PostProcessorConfig();
    private FacepalmScanner.PatternConfig patterns = new FacepalmScanner.PatternConfig();

    // Lifecycle flags
    private boolean showDetails = false;
    private boolean showProcessed = false;
    private boolean showSkipped = false;
    private boolean failOnError = true;
    private boolean failOnWarnings = false;
}
