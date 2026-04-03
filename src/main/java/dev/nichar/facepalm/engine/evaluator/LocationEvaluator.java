package dev.nichar.facepalm.engine.evaluator;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import dev.nichar.facepalm.engine.FileContext;
import dev.nichar.facepalm.engine.Finding;


@Named
@Singleton
class LocationEvaluator implements FindingEvaluator {

    @Inject
    private dev.nichar.facepalm.FacepalmConfig config;

    @Override
    public void evaluate(Finding finding, FileContext context) {
        String path = context.getPath().toString().toLowerCase();

        final var conf = config.getEvaluators();
        if (conf.getProdPathMarkers().stream().anyMatch(path::contains)) {
            finding.log("Production Path Marker", 20, 0);
        }

        if (conf.getTestPathMarkers().stream().anyMatch(path::contains)) {
            finding.log("Test/Mock Path Marker", -30, -20);
        }
    }
}
