/*
 * Licensed under Apache-2.0.
 * Copyright (c) 2026 Nikolas Charalambidis.
 * All rights reserved.
 */

package dev.nichar.facepalm.engine.evaluator;

import dev.nichar.facepalm.engine.FileContext;
import dev.nichar.facepalm.engine.Finding;
import dev.nichar.facepalm.engine.GitIgnoreService;
import jakarta.annotation.Nonnull;
import javax.inject.Named;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;

/**
 * Evaluates findings based on their visibility in Git history.
 * Reduces risk for files matching .gitignore patterns, suggesting lower public exposure.
 */
@Named
@Singleton
@RequiredArgsConstructor
class PublicExposureEvaluator implements FindingEvaluator {

    private final GitIgnoreService gitIgnoreService;

    @Override
    public void evaluate(@Nonnull final Finding finding, @Nonnull final FileContext context) {
        // De-prioritize findings in files that are explicitly ignored by Git.
        if (gitIgnoreService.isIgnored(context.getPath())) {
            finding.log("Recursive .gitignore Match (Low Exposure)", -40, 0);
        }
    }
}
