/*
 * Licensed under Apache-2.0.
 * Copyright (c) 2026 Nikolas Charalambidis.
 * All rights reserved.
 */

package dev.nichar.facepalm.config;

import dev.nichar.facepalm.pattern.SecretPattern;
import dev.nichar.facepalm.pattern.SecretPatternRegistry;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Configuration for defining or overriding secret detection patterns.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PatternConfig {

    /**
     * User-defined patterns to supplement or replace built-in logic.
     */
    private List<SecretPattern> overrides;

    /**
     * Returns the active detection patterns, falling back to defaults if none are defined.
     */
    public List<SecretPattern> getOverrides() {
        return overrides != null ? overrides : SecretPatternRegistry.DEFAULT_PATTERNS;
    }
}
