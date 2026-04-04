package dev.nichar.facepalm.engine.evaluator;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import dev.nichar.facepalm.FacepalmConfig;
import dev.nichar.facepalm.engine.FileContext;
import dev.nichar.facepalm.engine.Finding;


/**
 * Detects and suppresses findings that appear to be placeholders, templates, or dummy data.
 * Prevents build failures caused by non-functional secrets like {@code ${VARIABLE_NAME}} or "your_password_here".
 */
@Named
@Singleton
class PlaceholderSuppressorEvaluator implements FindingEvaluator {

    @Inject
    private FacepalmConfig config;

    @Override
    public void evaluate(@Nonnull final Finding finding, @Nonnull final FileContext context) {
        final var value = finding.getSecretValue().trim().replaceAll("[,;\"']+$", "");

        final var conf = config.getEvaluators();
        // Penalize property interpolation patterns (e.g., ${API_KEY} or {{SECRET}}).
        if (conf.getInterpolationPattern().matcher(value).matches()) {
            finding.log("Interpolation/Placeholder Shield", -50, -100);
            return;
        }

        final var lowerVal = value.toLowerCase().replace("-", "_").replace(" ", "_");
        // Penalize known "fake" data keywords (e.g., "dummy", "replace_me").
        if (conf.getDummyKeywords().stream().anyMatch(lowerVal::contains)) {
            finding.log("Dummy Keyword Penalty", 0, -80);
        }
    }
}
