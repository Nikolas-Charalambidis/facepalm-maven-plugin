package dev.nichar.facepalm.engine.evaluator;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import dev.nichar.facepalm.engine.FileContext;
import dev.nichar.facepalm.engine.Finding;


@Named
@Singleton
class FileExtensionEvaluator implements FindingEvaluator {

    @Inject
    private dev.nichar.facepalm.FacepalmConfig config;

    @Override
    public void evaluate(Finding finding, FileContext context) {
        String fileName = context.getPath().getFileName().toString().toLowerCase();

        if (config.getEvaluators().getHighRiskExtensions().stream().anyMatch(fileName::endsWith)) {
            finding.log("High Risk Configuration File", 15, 20);
        } else if (config.getEvaluators().getLowRiskExtensions().stream().anyMatch(fileName::endsWith)) {
            finding.log("Documentation/Log File", -30, -40);
        }
    }
}
