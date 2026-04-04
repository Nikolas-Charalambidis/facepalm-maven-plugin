package dev.nichar.facepalm.engine.evaluator;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Named;
import javax.inject.Singleton;

import dev.nichar.facepalm.engine.FileContext;
import dev.nichar.facepalm.engine.Finding;


/**
 * Evaluates the randomness (entropy) of a discovered secret to distinguish between
 * genuine random credentials and low-entropy false positives like placeholders.
 * High Shannon entropy suggests a more "random" and therefore more suspicious string.
 */
@Named
@Singleton
class EntropyEvaluator implements FindingEvaluator {

    @Override
    public void evaluate(@Nonnull final Finding finding, @Nonnull final FileContext context) {
        // Skips entropy analysis for Private Keys, as their structured format is already high-signal.
        if (finding.getPatternName().contains("Private Key") || finding.getSecretValue().contains("-----")) {
            return;
        }

        final var entropy = getShannonEntropy(finding.getSecretValue());
        if (entropy > 4.5) {
            // Increases the score for highly random strings, which are typical for API keys or passwords.
            finding.log(String.format("High Entropy (%.2f)", entropy), 10, 20);
        } else if (entropy < 3.0) {
            // Lowers the score for repetitive or predictable strings (e.g., "password123"), reducing noise.
            finding.log(String.format("Low Entropy (%.2f)", entropy), 0, -40);
        }
    }

    /**
     * Calculates the Shannon Entropy of a string, representing the average information rate.
     *
     * @param string The string to evaluate.
     * @return The entropy value; higher values indicate higher randomness.
     *
     * @see <a href="https://en.wikipedia.org/wiki/Entropy_(information_theory)">Wikipedia</a>
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
        for (int count : freq.values()) {
            double p = (double) count / string.length();
            entropy -= p * (Math.log(p) / Math.log(2));
        }
        return entropy;
    }
}
