package dev.nichar.facepalm.engine.evaluator;

import jakarta.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import dev.nichar.facepalm.FacepalmConfig;
import dev.nichar.facepalm.engine.FileContext;
import dev.nichar.facepalm.engine.Finding;


/**
 * Evaluates findings based on their filesystem location.
 * Elevates risk for production paths and de-prioritizes test or mock directories.
 */
@Named
@Singleton
class LocationEvaluator implements FindingEvaluator {

    @Inject
    private FacepalmConfig config;

    @Override
    public void evaluate(@Nonnull final Finding finding, @Nonnull final FileContext context) {
        final var path = context.getPath().toString().toLowerCase();
        final var conf = config.getEvaluators();
        // Increase risk for findings in production-critical paths.
        if (conf.getProdPathMarkers().stream().anyMatch(path::contains)) {
            finding.log("Production Path Marker", 20, 0);
        }

        // Reduce risk for findings in test or mock environments.
        if (conf.getTestPathMarkers().stream().anyMatch(path::contains)) {
            finding.log("Test/Mock Path Marker", -30, -20);
        }
    }
}
