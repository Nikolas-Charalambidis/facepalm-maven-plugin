package dev.nichar.facepalm.engine.evaluator;


import javax.annotation.Nonnull;

import dev.nichar.facepalm.engine.FileContext;
import dev.nichar.facepalm.engine.Finding;


/**
 * Defines a strategic contract for components that analyze and decorate scan results.
 * Implementations can modify a finding's severity, add context, or lower its risk score
 * based on the specific environmental data provided by the {@link FileContext}.
 */
public interface FindingEvaluator {

    /**
     * Processes an individual finding to apply rules-based logic or scoring adjustments.
     *
     * @param finding The identified secret or issue to be evaluated and potentially modified.
     * @param context Metadata and content of the file where the finding was discovered.
     */
    void evaluate(@Nonnull Finding finding, @Nonnull FileContext context);
}
