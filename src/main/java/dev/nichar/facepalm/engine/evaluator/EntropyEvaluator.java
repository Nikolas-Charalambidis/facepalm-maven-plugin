/*
 * Licensed under Apache-2.0.
 * Copyright (c) 2026 Nikolas Charalambidis.
 * All rights reserved.
 */

package dev.nichar.facepalm.engine.evaluator;

import dev.nichar.facepalm.engine.FileContext;
import dev.nichar.facepalm.engine.Finding;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Evaluates a secret's randomness using Shannon entropy.
 * High entropy often signals genuine credentials, while low entropy indicates repetitive or dummy data.
 */
@Named
@Singleton
class EntropyEvaluator implements FindingEvaluator {

    @Override
    public void evaluate(@Nonnull final Finding finding, @Nonnull final FileContext context) {
        // Skip entropy checks for high-signal formats like Private Keys.
        if (finding.getPatternName().contains("Private Key") || finding.getSecretValue().contains("-----")) {
            return;
        }

        final var entropy = getShannonEntropy(finding.getSecretValue());
        if (entropy > 4.5) {
            // Random strings typical of API keys or passwords.
            finding.log(String.format("High Entropy (%.2f)", entropy), 10, 20);
        } else if (entropy < 3.0) {
            // Repetitive or predictable strings likely to be noise.
            finding.log(String.format("Low Entropy (%.2f)", entropy), 0, -40);
        }
    }

    /**
     * Calculates Shannon entropy for the given string.
     * Values above 4.5 typically indicate high randomness.
     */
    private double getShannonEntropy(@Nullable final String string) {
        if (string == null || string.isEmpty()) {
            return 0;
        }
        final Map<Character, Integer> freq = new HashMap<>();
        for (final var ch : string.toCharArray()) {
            freq.merge(ch, 1, Integer::sum);
        }
        var entropy = 0.0;
        for (final int count : freq.values()) {
            final double p = (double) count / string.length();
            entropy -= p * (Math.log(p) / Math.log(2));
        }
        return entropy;
    }
}
