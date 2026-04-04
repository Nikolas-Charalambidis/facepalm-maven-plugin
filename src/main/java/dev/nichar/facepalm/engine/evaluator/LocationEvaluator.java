package dev.nichar.facepalm.engine.evaluator;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import dev.nichar.facepalm.FacepalmConfig;
import dev.nichar.facepalm.engine.FileContext;
import dev.nichar.facepalm.engine.Finding;


/**
 * Adjusts finding scores based on the file's location within the project structure.
 * It prioritizes secrets found in production-related paths (e.g., /src/main/resources)
 * while de-prioritizing findings in test or mock directories (e.g., /src/test/java).
 */
@Named
@Singleton
class LocationEvaluator implements FindingEvaluator {

    @Inject
    private FacepalmConfig config;

    @Override
    public void evaluate(@Nonnull final Finding finding, @Nonnull final FileContext context) {
        String path = context.getPath().toString().toLowerCase();

        final var conf = config.getEvaluators();
        // Checks if the path contains keywords indicating production deployment (e.g., "prod", "main", "deploy").
        if (conf.getProdPathMarkers().stream().anyMatch(path::contains)) {
            // Increases the score as secrets in production-bound code represent a critical security leak.
            finding.log("Production Path Marker", 20, 0);
        }

        // Checks if the path contains keywords indicating testing or local development (e.g., "test", "mock", "example").
        if (conf.getTestPathMarkers().stream().anyMatch(path::contains)) {
            // Lowers the score significantly because secrets in tests are often non-functional mock data.
            finding.log("Test/Mock Path Marker", -30, -20);
        }
    }
}
