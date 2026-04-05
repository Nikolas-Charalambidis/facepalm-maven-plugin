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
 * Root configuration object for the Facepalm plugin.
 * Aggregates engine, scoring, and heuristic settings into a single hierarchy.
 */
@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class FacepalmConfig {

    /**
     * Settings for engine execution and file filtering.
     */
    private EngineConfig engine = new EngineConfig();

    /**
     * Rules for scoring findings and failing the build.
     */
    private ScoringConfig scoring = new ScoringConfig();

    /**
     * Heuristics for validating and weighting discovered secrets.
     */
    private EvaluatorConfig evaluators = new EvaluatorConfig();

    /**
     * Logic for noise reduction and post-scan analysis.
     */
    private PostProcessorConfig postProcessing = new PostProcessorConfig();

    /**
     * Definitions for regex-based secret detection.
     */
    private PatternConfig patterns = new PatternConfig();
}
