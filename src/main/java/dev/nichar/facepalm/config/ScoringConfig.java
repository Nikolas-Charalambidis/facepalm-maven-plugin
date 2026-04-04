package dev.nichar.facepalm.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * Configuration for finding scores and build interruption thresholds.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScoringConfig {

    /**
     * Score threshold (0-100) for high-risk errors.
     */
    private int errorThreshold = 80;

    /**
     * Score threshold (0-100) for moderate-risk warnings.
     */
    private int warningThreshold = 40;

    /**
     * If true, logs the heuristic score breakdown for each finding.
     */
    private boolean showScoring = false;

    /**
     * If true, logs a detailed breakdown of file discovery and scan coverage.
     */
    private boolean showDetails = false;

    /**
     * If true, fails the build when findings meet the error threshold.
     */
    private boolean failOnError = true;

    /**
     * If true, fails the build when findings meet the warning threshold.
     */
    private boolean failOnWarnings = false;
}
