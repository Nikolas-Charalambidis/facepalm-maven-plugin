package dev.nichar.facepalm.config;

import java.util.List;

import dev.nichar.facepalm.pattern.SecretPattern;
import dev.nichar.facepalm.pattern.SecretPatternRegistry;
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
     * User-defined list of patterns to supplement or replace built-in logic.
     */
    private List<SecretPattern> overrides;

    /**
     * Returns the active list of detection patterns, falling back to defaults if none are provided.
     */
    public List<SecretPattern> getOverrides() {
        return overrides != null ? overrides : SecretPatternRegistry.DEFAULT_PATTERNS;
    }
}
