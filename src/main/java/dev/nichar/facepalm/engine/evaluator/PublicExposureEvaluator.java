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
 * Adjusts the risk score of findings based on whether the containing file is ignored by Git.
 * If a file is matched by a .gitignore rule, it is considered to have lower public exposure,
 * and its overall security priority is significantly reduced.
 */
@Named
@Singleton
@RequiredArgsConstructor
class PublicExposureEvaluator implements FindingEvaluator {

    private final GitIgnoreService gitIgnoreService;

    @Override
    public void evaluate(@Nonnull final Finding finding, @Nonnull final FileContext context) {
        // Checks if the current file path matches any .gitignore patterns discovered in the project.
        if (gitIgnoreService.isIgnored(context.getPath())) {
            // Lowers the finding's priority score, because ignored files are less likely to be committed.
            finding.log("Recursive .gitignore Match (Low Exposure)", -40, 0);
        }
    }
}
