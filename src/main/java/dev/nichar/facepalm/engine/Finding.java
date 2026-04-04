package dev.nichar.facepalm.engine;

import java.util.ArrayList;
import java.util.List;

import dev.nichar.facepalm.config.ScoringConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;


@Data
@Builder
@AllArgsConstructor
public class Finding {

    private final String patternName;

    @EqualsAndHashCode.Include
    private final String deduplicationHash;

    private final String secretValue;

    private final int lineNumber;

    private final FileContext context;

    private final String contextSnippet;

    private int riskScore;

    private int confidenceScore;

    @Builder.Default
    private final List<String> scoreHistory = new ArrayList<>();

    public void log(String rule, int rDelta, int cDelta) {
        riskScore = Math.max(0, Math.min(100, riskScore + rDelta));
        confidenceScore = Math.max(0, Math.min(100, confidenceScore + cDelta));
        scoreHistory.add(String.format("%s (%+d/%+d)", rule, rDelta, cDelta));
    }

    public Severity getSeverity(ScoringConfig config) {
        double score = getNumericScore();
        if (score >= config.getErrorThreshold()) {
            return Severity.ERROR;
        }
        if (score >= config.getWarningThreshold()) {
            return Severity.WARNING;
        }
        return Severity.INFO;
    }

    public double getNumericScore() {
        return ScoringStrategy.WEIGHTED_QUADRATIC.calculate(riskScore, confidenceScore);
    }

    public String getMaskedSecret() {
        return secretValue.length() <= 8 ? "****" : secretValue.substring(0, 4) + "..." + secretValue.substring(secretValue.length() - 4);
    }
}
