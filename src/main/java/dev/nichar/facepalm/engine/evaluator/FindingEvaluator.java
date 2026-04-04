package dev.nichar.facepalm.engine.evaluator;


import javax.annotation.Nonnull;

import dev.nichar.facepalm.engine.FileContext;
import dev.nichar.facepalm.engine.Finding;


/**
 * Contract for components that analyze and decorate scan findings.
 * Implementations can modify a finding's risk score or severity based on file context.
 */
public interface FindingEvaluator {

    /**
     * Evaluates a finding and potentially modifies its scores or metadata.
     */
    void evaluate(@Nonnull Finding finding, @Nonnull FileContext context);
}
