/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2026 Nikolas Charalambidis
 */

package dev.nichar.facepalm.pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * Tests for the default secret detection pattern registry.
 */
class SecretPatternRegistryTest {

    @Test
    void default_patterns_do_not_register_duplicate_regexes() {
        final var duplicatePatterns = SecretPatternRegistry.DEFAULT_PATTERNS.stream()
            .collect(Collectors.groupingBy(pattern -> pattern.getPattern().pattern(), Collectors.counting()))
            .entrySet().stream()
            .filter(entry -> entry.getValue() > 1)
            .toList();

        assertTrue(duplicatePatterns.isEmpty(), () -> "Duplicate default secret patterns: " + duplicatePatterns);
    }
}
