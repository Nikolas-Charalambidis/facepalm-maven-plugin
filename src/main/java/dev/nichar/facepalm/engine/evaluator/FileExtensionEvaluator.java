package dev.nichar.facepalm.engine.evaluator;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import dev.nichar.facepalm.FacepalmConfig;
import dev.nichar.facepalm.engine.FileContext;
import dev.nichar.facepalm.engine.Finding;


/**
 * Adjusts finding scores based on the file extension to prioritize sensitive configuration files.
 * It increases the risk level for high-exposure files (like .env or .yaml) and penalizes
 * findings found in low-risk files (like documentation or logs) to reduce noise.
 */
@Named
@Singleton
class FileExtensionEvaluator implements FindingEvaluator {

    @Inject
    private FacepalmConfig config;

    @Override
    public void evaluate(@Nonnull final Finding finding, @Nonnull final FileContext context) {
        final var fileName = context.getPath().getFileName().toString().toLowerCase();

        // Checks if the file matches extensions known for sensitive data (e.g., .tfvars, .properties).
        if (config.getEvaluators().getHighRiskExtensions().stream().anyMatch(fileName::endsWith)) {
            // Increases both the raw score and severity because secrets in config files are highly exploitable.
            finding.log("High Risk Configuration File", 15, 20);
        }
        // Checks if the file belongs to a lower-priority category (e.g., .md, .txt, .log).
        else if (config.getEvaluators().getLowRiskExtensions().stream().anyMatch(fileName::endsWith)) {
            // Significantly drops the score, as secrets in documentation are often examples or false positives.
            finding.log("Documentation/Log File", -30, -40);
        }
    }
}
