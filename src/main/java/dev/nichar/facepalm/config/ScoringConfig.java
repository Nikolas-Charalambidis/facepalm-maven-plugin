/*
 * Licensed under Apache-2.0.
 * Copyright (c) 2026 Nikolas Charalambidis.
 * All rights reserved.
 */

package dev.nichar.facepalm.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Configuration for discovery scoring and build interruption thresholds.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScoringConfig {

    /**
     * Minimum score to classify a finding as high-risk (Critical).
     */
    private int errorThreshold = 80;

    /**
     * Minimum score to classify a finding as moderate-risk (Warning).
     */
    private int warningThreshold = 40;

    /**
     * Enable verbose logging for heuristic scoring decisions.
     */
    private boolean showScoring = false;

    /**
     * Enable detailed logging for scan coverage and discovery metrics.
     */
    private boolean showDetails = false;

    /**
     * Terminate the build on high-risk findings.
     */
    private boolean failOnError = true;

    /**
     * Terminate the build on moderate-risk findings.
     */
    private boolean failOnWarnings = false;
}
