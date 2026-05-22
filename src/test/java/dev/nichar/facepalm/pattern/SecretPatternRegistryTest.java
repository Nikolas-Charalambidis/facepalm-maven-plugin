/*
 * Licensed under Apache-2.0.
 * Copyright (c) 2026 Nikolas Charalambidis.
 * All rights reserved.
 */

package dev.nichar.facepalm.pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Tests for the default secret detection pattern registry.
 */
class SecretPatternRegistryTest {

    @Test
    void default_patterns_register_google_api_keys_once() {
        final var googleApiKeyRegex = "\\bAIza[0-9A-Za-z\\-_]{35}\\b";

        final var matchingPatterns = SecretPatternRegistry.DEFAULT_PATTERNS.stream()
            .filter(pattern -> googleApiKeyRegex.equals(pattern.getPattern().pattern()))
            .count();

        assertEquals(1, matchingPatterns);
    }
}
