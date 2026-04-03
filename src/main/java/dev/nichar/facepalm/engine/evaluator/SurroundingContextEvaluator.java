package dev.nichar.facepalm.engine.evaluator;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import dev.nichar.facepalm.engine.FileContext;
import dev.nichar.facepalm.engine.Finding;


@Named
@Singleton
class SurroundingContextEvaluator implements FindingEvaluator {

    @Inject
    private dev.nichar.facepalm.FacepalmConfig config;

    @Override
    public void evaluate(Finding finding, FileContext context) {
        int idx = finding.getLineNumber() - 1;
        String chunk = (context.getLineOrEmpty(idx - 1) + " " +
            context.getLineOrEmpty(idx) + " " +
            context.getLineOrEmpty(idx + 1)).toLowerCase();

        final var conf = config.getEvaluators();
        if (conf.getMockContextMarkers().stream().anyMatch(chunk::contains)) {
            finding.log("Mock Context Keywords Found", 0, -40);
        }
        if (conf.getProdContextMarkers().stream().anyMatch(chunk::contains)) {
            finding.log("Production Context Keywords Found", 20, 0);
        }
    }
}
