package dev.nichar.facepalm;

import dev.nichar.facepalm.config.EngineConfig;
import dev.nichar.facepalm.config.EvaluatorConfig;
import dev.nichar.facepalm.config.PatternConfig;
import dev.nichar.facepalm.config.PostProcessorConfig;
import dev.nichar.facepalm.config.ScoringConfig;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;


@Getter
@AllArgsConstructor
@RequiredArgsConstructor
public class FacepalmConfig {

    private EngineConfig engine = new EngineConfig();
    private ScoringConfig scoring = new ScoringConfig();
    private EvaluatorConfig evaluators = new EvaluatorConfig();
    private PostProcessorConfig postProcessing = new PostProcessorConfig();
    private PatternConfig patterns = new PatternConfig();
}
