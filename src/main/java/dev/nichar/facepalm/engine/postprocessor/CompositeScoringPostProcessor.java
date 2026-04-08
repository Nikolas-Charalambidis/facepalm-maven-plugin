/*
 * Licensed under Apache-2.0.
 * Copyright (c) 2026 Nikolas Charalambidis.
 * All rights reserved.
 */

package dev.nichar.facepalm.engine.postprocessor;

import dev.nichar.facepalm.FacepalmConfig;
import dev.nichar.facepalm.engine.FileContext;
import dev.nichar.facepalm.engine.Finding;
import jakarta.annotation.Nonnull;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Refines discovery findings based on the collective context of a file.
 * Elevates risk for files with multiple distinct secret types and discounts high-volume noise.
 */
@Named
@Singleton
class CompositeScoringPostProcessor implements FileFindingsPostProcessor {

    @Inject
    private FacepalmConfig config;

    @Override
    public void process(@Nonnull final List<Finding> fileFindings, @Nonnull final FileContext context) {
        if (fileFindings.isEmpty()) {
            return;
        }

        final var totalInFile = fileFindings.size();
        final var uniquePatterns = fileFindings.stream()
            .map(Finding::getPatternName)
            .distinct()
            .count();

        final var conf = config.getPostProcessing();
        for (final var finding : fileFindings) {
            // Categorize file as "noisy" if detections exceed the high-volume threshold.
            if (totalInFile > conf.getHighVolumeThreshold()) {
                // High volume often indicates log dumps, public datasets, or false positives.
                finding.log("High Volume File (Threshold: " + conf.getHighVolumeThreshold() + ")", -25, -30);
            }
            // Elevate risk if a single file contains multiple distinct secret types.
            else if (uniquePatterns > 1) {
                // Increases risk because a file containing multiple secret types is statistically more likely to be a
                // real credential leak.
                finding.log("Composite Risk: Multiple distinct secrets in one file", 15, 10);
            }
        }
    }
}
