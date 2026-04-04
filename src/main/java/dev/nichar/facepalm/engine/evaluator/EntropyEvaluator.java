package dev.nichar.facepalm.engine.evaluator;

import java.util.HashMap;
import java.util.Map;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.inject.Named;
import javax.inject.Singleton;

import dev.nichar.facepalm.engine.FileContext;
import dev.nichar.facepalm.engine.Finding;


/**
 * Evaluates a secret's randomness (Shannon entropy) to distinguish between genuine credentials and false positives.
 */
@Named
@Singleton
class EntropyEvaluator implements FindingEvaluator {

    @Override
    public void evaluate(@Nonnull final Finding finding, @Nonnull final FileContext context) {
        // Skip entropy check for Private Keys; the format itself is high-signal.
        if (finding.getPatternName().contains("Private Key") || finding.getSecretValue().contains("-----")) {
            return;
        }

        final var entropy = getShannonEntropy(finding.getSecretValue());
        if (entropy > 4.5) {
            // Increases the score for highly random strings typical for API keys or passwords.
            finding.log(String.format("High Entropy (%.2f)", entropy), 10, 20);
        } else if (entropy < 3.0) {
            // Lowers the score for repetitive or predictable strings to reduce noise.
            finding.log(String.format("Low Entropy (%.2f)", entropy), 0, -40);
        }
    }

    /**
     * Calculates Shannon Entropy; higher values indicate higher randomness.
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
