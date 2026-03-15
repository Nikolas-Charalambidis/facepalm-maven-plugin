package dev.nichar.facepalm.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;


/**
 * Configuration for how findings are scored and when the build should be interrupted.
 *
 * @author Nikolas Charalambidis
 * @since 1.0.0
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ScoringConfig {

    /**
     * The score threshold (0-100) at or above which a finding is classified as a high-risk error.
     */
    private int errorThreshold = 80;

    /**
     * The score threshold (0-100) at or above which a finding is classified as a moderate-risk warning.
     * Findings scoring below this value are suppressed or treated as informational.
     */
    private int warningThreshold = 40;

    /**
     * When true, logs the breakdown of how the heuristic score was calculated
     * for each finding to help debug false positives.
     */
    private boolean showDetails = false;

    /**
     * Whether the execution (Maven build or CLI process) should exit with a
     * failure code if any findings meet the {@link #errorThreshold}.
     */
    private boolean failOnError = true;

    /**
     * Whether the execution should exit with a failure code if findings
     * meet the {@link #warningThreshold} but stay below the error threshold.
     */
    private boolean failOnWarnings = false;
}
