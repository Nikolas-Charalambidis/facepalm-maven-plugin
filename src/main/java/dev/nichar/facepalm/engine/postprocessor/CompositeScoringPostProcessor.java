package dev.nichar.facepalm.engine.postprocessor;

import java.util.List;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import dev.nichar.facepalm.FacepalmConfig;
import dev.nichar.facepalm.engine.FileContext;
import dev.nichar.facepalm.engine.Finding;


/**
 * Refines individual finding scores based on the collective context of all findings in a file.
 * Boosts risk for files containing multiple distinct secret types, while discounting high-volume
 * matches (like logs or datasets) to reduce noise.
 */
@Named
@Singleton
class CompositeScoringPostProcessor implements FileFindingsPostProcessor {

    @Inject
    private FacepalmConfig config;

    @Override
    public void process(@Nonnull final List<Finding> fileFindings, @Nonnull final FileContext context) {
        if (fileFindings.isEmpty()) {
            return;
        }

        final var totalInFile = fileFindings.size();
        final var uniquePatterns = fileFindings.stream()
            .map(Finding::getPatternName)
            .distinct()
            .count();

        final var conf = config.getPostProcessing();
        for (final var finding : fileFindings) {
            // Checks if the file is "noisy" by exceeding the high-volume threshold (e.g., 50+ hits).
            if (totalInFile > conf.getHighVolumeThreshold()) {
                // Lowers score/severity as high volume often indicates a false positive, log dump, or public dataset.
                finding.log("High Volume File (Threshold: " + conf.getHighVolumeThreshold() + ")", -25, -30);
            }
            // If the volume is low but there are multiple DIFFERENT types of secrets.
            else if (uniquePatterns > 1) {
                // Increases risk because a file containing multiple secret types is statistically more likely to be a real credential leak.
                finding.log("Composite Risk: Multiple distinct secrets in one file", 15, 10);
            }
        }
    }
}
