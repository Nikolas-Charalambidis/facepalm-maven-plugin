package dev.nichar.facepalm.engine.evaluator;


import jakarta.annotation.Nonnull;

import dev.nichar.facepalm.engine.FileContext;
import dev.nichar.facepalm.engine.Finding;


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
