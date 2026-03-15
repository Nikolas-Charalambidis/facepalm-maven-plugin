package dev.nichar.facepalm;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class FacepalmConfig {

    private FacepalmScanner.EngineConfig engine = new FacepalmScanner.EngineConfig();
    private FacepalmScanner.ScoringConfig scoring = new FacepalmScanner.ScoringConfig();
    private FacepalmScanner.EvaluatorConfig evaluators = new FacepalmScanner.EvaluatorConfig();
    private FacepalmScanner.PostProcessorConfig postProcessing = new FacepalmScanner.PostProcessorConfig();
    private FacepalmScanner.PatternConfig patterns = new FacepalmScanner.PatternConfig();

    public FacepalmConfig get() {
        return this;
    }
}
