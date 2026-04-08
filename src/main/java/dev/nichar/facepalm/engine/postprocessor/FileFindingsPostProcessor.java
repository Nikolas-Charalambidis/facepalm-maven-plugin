/*
 * Licensed under Apache-2.0.
 * Copyright (c) 2026 Nikolas Charalambidis.
 * All rights reserved.
 */

package dev.nichar.facepalm.engine.postprocessor;

import dev.nichar.facepalm.engine.FileContext;
import dev.nichar.facepalm.engine.Finding;
import jakarta.annotation.Nonnull;
import java.util.List;

/**
 * Interface for components that perform final adjustments on discovery findings at the file level.
 * Identifies high-volume noise or composite risks by analyzing all findings within a single file.
 */
@FunctionalInterface
public interface FileFindingsPostProcessor {

    /**
     * Executes post-scan logic on the collected findings for a specific file.
     */
    void process(@Nonnull List<Finding> fileFindings, @Nonnull FileContext context);
}
