package dev.nichar.facepalm.engine.evaluator;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import dev.nichar.facepalm.engine.FileContext;
import dev.nichar.facepalm.engine.Finding;
import dev.nichar.facepalm.engine.GitIgnoreService;
import lombok.RequiredArgsConstructor;


/**
 * Reduces the risk score if the file is ignored by Git, suggesting lower public exposure.
 */
@Named
@Singleton
@RequiredArgsConstructor
class PublicExposureEvaluator implements FindingEvaluator {

    private final GitIgnoreService gitIgnoreService;

    @Override
    public void evaluate(@Nonnull final Finding finding, @Nonnull final FileContext context) {
        // Lower priority if the file matches .gitignore patterns.
        if (gitIgnoreService.isIgnored(context.getPath())) {
            finding.log("Recursive .gitignore Match (Low Exposure)", -40, 0);
        }
    }
}
