package dev.nichar.facepalm.engine.evaluator;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;

import dev.nichar.facepalm.engine.FileContext;
import dev.nichar.facepalm.engine.Finding;


@Named
@Singleton
class EntropyEvaluator implements FindingEvaluator {

    @Override
    public void evaluate(Finding finding, FileContext context) {
        if (finding.getPatternName().contains("Private Key") || finding.getSecretValue().contains("-----")) {
            return;
        }

        double entropy = getShannonEntropy(finding.getSecretValue());
        if (entropy > 4.5) {
            finding.log(String.format("High Entropy (%.2f)", entropy), 10, 20);
        } else if (entropy < 3.0) {
            finding.log(String.format("Low Entropy (%.2f)", entropy), 0, -40);
        }
    }

    private double getShannonEntropy(String s) {
        if (s == null || s.isEmpty()) {
            return 0;
        }
        Map<Character, Integer> freq = new HashMap<>();
        for (char c : s.toCharArray()) {
            freq.merge(c, 1, Integer::sum);
        }
        double entropy = 0.0;
        for (int count : freq.values()) {
            double p = (double) count / s.length();
            entropy -= p * (Math.log(p) / Math.log(2));
        }
        return entropy;
    }
}
