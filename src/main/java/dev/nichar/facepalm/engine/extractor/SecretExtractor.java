/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2026 Nikolas Charalambidis
 */

package dev.nichar.facepalm.engine.extractor;

import dev.nichar.facepalm.engine.FileContext;
import dev.nichar.facepalm.engine.Finding;
import jakarta.annotation.Nonnull;
import java.util.List;

/**
 * Interface for components that identify potential secrets within file content.
 */
@FunctionalInterface
public interface SecretExtractor {

    /**
     * Analyzes the file context and returns a list of discovered sensitive patterns.
     */
    @Nonnull
    List<Finding> extract(@Nonnull final FileContext context);
}
