package dev.nichar.facepalm.engine.evaluator;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import dev.nichar.facepalm.FacepalmConfig;
import dev.nichar.facepalm.engine.FileContext;
import dev.nichar.facepalm.engine.Finding;


/**
 * Adjusts finding scores based on the file's location within the project.
 * Prioritizes secrets in production-related paths and de-prioritizes findings in test or mock directories.
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
        // Increase score for secrets in production-bound paths.
        if (conf.getProdPathMarkers().stream().anyMatch(path::contains)) {
            finding.log("Production Path Marker", 20, 0);
        }

        // Lower score for secrets in test or mock paths.
        if (conf.getTestPathMarkers().stream().anyMatch(path::contains)) {
            finding.log("Test/Mock Path Marker", -30, -20);
        }
    }
}
