package dev.nichar.facepalm;

import org.apache.maven.plugins.annotations.Parameter;

import dev.nichar.facepalm.config.EngineConfig;
import dev.nichar.facepalm.config.EvaluatorConfig;
import dev.nichar.facepalm.config.PatternConfig;
import dev.nichar.facepalm.config.PostProcessorConfig;
import dev.nichar.facepalm.config.ScoringConfig;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;


/**
 * Aggregates plugin configurations into a single hierarchical structure.
 */
@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class FacepalmConfig {

    /**
     * Scanning engine execution and file filtering settings.
     */
    private EngineConfig engine = new EngineConfig();

    /**
     * Finding scoring and build interruption thresholds.
     */
    private ScoringConfig scoring = new ScoringConfig();

    /**
     * Risk and legitimacy heuristics for discovered secrets.
     */
    private EvaluatorConfig evaluators = new EvaluatorConfig();

    /**
     * Noise reduction and post-scan cleanup settings.
     */
    private PostProcessorConfig postProcessing = new PostProcessorConfig();

    /**
     * Secret detection pattern definitions and overrides.
     */
    private PatternConfig patterns = new PatternConfig();
}
