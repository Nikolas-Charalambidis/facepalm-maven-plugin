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
 * Configuration for post-scan noise reduction and discovery refinement.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostProcessorConfig {

    /**
     * Maximum detections allowed per file before suppression as high-volume noise.
     */
    private int highVolumeThreshold = 15;
}
