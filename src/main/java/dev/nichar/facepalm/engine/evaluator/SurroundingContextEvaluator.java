package dev.nichar.facepalm.engine.evaluator;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import dev.nichar.facepalm.FacepalmConfig;
import dev.nichar.facepalm.engine.FileContext;
import dev.nichar.facepalm.engine.Finding;


/**
 * Analyzes the lines immediately surrounding a finding to identify contextual clues about its environment.
 * By checking the lines above and below a secret for keywords like "mock", "test", or "prod",
 * the evaluator can more accurately determine if a secret is a functional credential or just test data.
 */
@Named
@Singleton
class SurroundingContextEvaluator implements FindingEvaluator {

    @Inject
    private FacepalmConfig config;

    @Override
    public void evaluate(@Nonnull final Finding finding, @Nonnull final FileContext context) {
        final int idx = finding.getLineNumber() - 1;
        // Concatenates the previous, current, and next lines into a single searchable string.
        final var chunk = (context.getLineOrEmpty(idx - 1) + " " +
            context.getLineOrEmpty(idx) + " " +
            context.getLineOrEmpty(idx + 1)).toLowerCase();

        final var conf = config.getEvaluators();
        // Searches for markers indicating the code is part of a test suite or mock setup (e.g., "@Test", "Mockito").
        if (conf.getMockContextMarkers().stream().anyMatch(chunk::contains)) {
            // Reduces severity as the secret is likely used only within a controlled testing environment.
            finding.log("Mock Context Keywords Found", 0, -40);
        }
        // Searches for markers indicating production infrastructure (e.g., "db.production.url", "main-cluster").
        if (conf.getProdContextMarkers().stream().anyMatch(chunk::contains)) {
            // Increases the score because secrets near production-specific keywords are high-value targets.
            finding.log("Production Context Keywords Found", 20, 0);
        }
    }
}
