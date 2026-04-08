/*
 * Licensed under Apache-2.0.
 * Copyright (c) 2026 Nikolas Charalambidis.
 * All rights reserved.
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
