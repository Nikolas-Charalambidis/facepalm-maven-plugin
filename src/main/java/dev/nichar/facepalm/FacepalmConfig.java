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
 * Aggregates plugin nested {@link Parameter} configurations into a single hierarchical configuration structure.
 *
 * @author Nikolas Charalambidis
 * @since 1.0.0
 */
@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class FacepalmConfig {

    /**
     * Configuration for the scanning engine execution and file filtering.
     */
    private EngineConfig engine = new EngineConfig();

    /**
     * Configuration for how findings are scored and when the build should be interrupted.
     */
    private ScoringConfig scoring = new ScoringConfig();

    /**
     * Heuristic configuration used to evaluate the risk and legitimacy of discovered secrets.
     */
    private EvaluatorConfig evaluators = new EvaluatorConfig();

    /**
     * Configuration for noise reduction and cleanup after the initial scan.
     */
    private PostProcessorConfig postProcessing = new PostProcessorConfig();

    /**
     * Configuration for defining or overriding secret detection patterns.
     */
    private PatternConfig patterns = new PatternConfig();
}
