package dev.nichar.facepalm.engine;

import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.Nonnull;

import dev.nichar.facepalm.config.ScoringConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;


/**
 * Represents a discovered secret and its associated threat metadata.
 * Tracks risk, confidence, and scoring history throughout the evaluation pipeline.
 */
@Data
@Builder
@AllArgsConstructor
public class Finding {

    private final String patternName;

    /**
     * Unique hash for file-level deduplication.
     */
    @EqualsAndHashCode.Include
    private final String deduplicationHash;

    private final String secretValue;

    private final int lineNumber;

    private final FileContext context;

    private final String contextSnippet;

    private int riskScore;

    private int confidenceScore;

    /**
     * Historical log of scoring adjustments.
     */
    @Builder.Default
    private final List<String> scoreHistory = new ArrayList<>();

    /**
     * Updates threat scores and records the rule responsible for the adjustment.
     */
    public void log(@Nonnull final String rule, final int rDelta, final int cDelta) {
        riskScore = Math.max(0, Math.min(100, riskScore + rDelta));
        confidenceScore = Math.max(0, Math.min(100, confidenceScore + cDelta));
        scoreHistory.add(String.format("%s (%+d/%+d)", rule, rDelta, cDelta));
    }

    /**
     * Resolves the finding's severity based on configured threat thresholds.
     */
    @Nonnull
    public Severity getSeverity(@Nonnull final ScoringConfig config) {
        final double score = getNumericScore();
        if (score >= config.getErrorThreshold()) {
            return Severity.ERROR;
        }
        if (score >= config.getWarningThreshold()) {
            return Severity.WARNING;
        }
        return Severity.INFO;
    }

    /**
     * Calculates the composite threat score using the active scoring strategy.
     */
    public double getNumericScore() {
        return ScoringStrategy.WEIGHTED_QUADRATIC.calculate(riskScore, confidenceScore);
    }

    /**
     * Returns a masked version of the secret for safe logging and reporting.
     */
    @Nonnull
    public String getMaskedSecret() {
        return secretValue.length() <= 8 ? "****" : secretValue.substring(0, 4) + "..." + secretValue.substring(secretValue.length() - 4);
    }
}
