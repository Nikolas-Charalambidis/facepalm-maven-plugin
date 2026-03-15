package dev.nichar.facepalm.config;

import java.util.List;

import dev.nichar.facepalm.FacepalmScanner;
import dev.nichar.facepalm.pattern.SecretPattern;
import dev.nichar.facepalm.pattern.SecretPatternRegistry;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;


/**
 * Configuration for defining or overriding secret detection patterns.
 *
 * @author Nikolas Charalambidis
 * @since 1.0.0
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PatternConfig {

    /**
     * User-defined list of patterns used to detect secrets.
     * If provided, these can either supplement or replace the built-in detection logic.
     */
    private List<SecretPattern> overrides;

    /**
     * Resolves the active list of patterns for the scanning engine.
     * If no overrides are provided via Mojo or CLI, it falls back to the internal
     * pattern registry.
     *
     * @return A non-null list of {@link SecretPattern} instances.
     */
    public List<SecretPattern> getEffective() {
        return overrides != null ? overrides : SecretPatternRegistry.DEFAULT_PATTERNS;
    }
}
