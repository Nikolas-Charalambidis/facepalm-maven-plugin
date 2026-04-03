package dev.nichar.facepalm.engine.evaluator;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import dev.nichar.facepalm.engine.FileContext;
import dev.nichar.facepalm.engine.Finding;


@Named
@Singleton
class PlaceholderSuppressorEvaluator implements FindingEvaluator {

    @Inject
    private dev.nichar.facepalm.FacepalmConfig config;

    @Override
    public void evaluate(Finding finding, FileContext context) {
        String val = finding.getSecretValue().trim().replaceAll("[,;\"']+$", "");

        final var conf = config.getEvaluators();
        if (conf.getInterpolationPattern().matcher(val).matches()) {
            finding.log("Interpolation/Placeholder Shield", -50, -100);
            return;
        }

        String lowerVal = val.toLowerCase().replace("-", "_").replace(" ", "_");
        if (conf.getDummyKeywords().stream().anyMatch(lowerVal::contains)) {
            finding.log("Dummy Keyword Penalty", 0, -80);
        }
    }
}
