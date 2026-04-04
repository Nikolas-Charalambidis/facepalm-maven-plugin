package dev.nichar.facepalm.engine.evaluator;

import jakarta.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import dev.nichar.facepalm.FacepalmConfig;
import dev.nichar.facepalm.engine.FileContext;
import dev.nichar.facepalm.engine.Finding;


/**
 * Adjusts finding scores based on the file extension.
 * Prioritizes sensitive configuration files and discounts findings in low-risk files like logs or documentation.
 */
@Named
@Singleton
class FileExtensionEvaluator implements FindingEvaluator {

    @Inject
    private FacepalmConfig config;

    @Override
    public void evaluate(@Nonnull final Finding finding, @Nonnull final FileContext context) {
        final var fileName = context.getPath().getFileName().toString().toLowerCase();

        // Increase score for sensitive configuration files (e.g., .env, .properties).
        if (config.getEvaluators().getHighRiskExtensions().stream().anyMatch(fileName::endsWith)) {
            finding.log("High Risk Configuration File", 15, 20);
        }
        // Decrease score for lower-priority files (e.g., .md, .txt, .log).
        else if (config.getEvaluators().getLowRiskExtensions().stream().anyMatch(fileName::endsWith)) {
            finding.log("Documentation/Log File", -30, -40);
        }
    }
}
