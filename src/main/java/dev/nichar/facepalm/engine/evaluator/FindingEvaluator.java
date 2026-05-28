/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2026 Nikolas Charalambidis
 */

package dev.nichar.facepalm.engine.evaluator;

import dev.nichar.facepalm.engine.FileContext;
import dev.nichar.facepalm.engine.Finding;
import jakarta.annotation.Nonnull;

/**
 * Interface for components that refine discovery findings.
 * Implementations analyze the file context to adjust risk and confidence scores.
 */
@FunctionalInterface
public interface FindingEvaluator {

    /**
     * Evaluates a finding to decorate it with additional metadata or score adjustments.
     */
    void evaluate(@Nonnull Finding finding, @Nonnull FileContext context);
}
