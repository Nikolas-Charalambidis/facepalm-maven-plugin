package dev.nichar.facepalm.engine.postprocessor;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import dev.nichar.facepalm.engine.FileContext;
import dev.nichar.facepalm.engine.Finding;


@Named
@Singleton
class CompositeScoringPostProcessor implements FileFindingsPostProcessor {

    @Inject
    private dev.nichar.facepalm.FacepalmConfig config;

    @Override
    public void process(List<Finding> fileFindings, FileContext context) {
        if (fileFindings.isEmpty()) {
            return;
        }

        int totalInFile = fileFindings.size();
        long uniquePatterns = fileFindings.stream()
            .map(Finding::getPatternName)
            .distinct()
            .count();

        final var conf = config.getPostProcessing();
        for (Finding f : fileFindings) {
            if (totalInFile > conf.getHighVolumeThreshold()) {
                f.log("High Volume File (Threshold: " + conf.getHighVolumeThreshold() + ")", -25, -30);
            } else if (uniquePatterns > 1) {
                f.log("Composite Risk: Multiple distinct secrets in one file", 15, 10);
            }
        }
    }
}
