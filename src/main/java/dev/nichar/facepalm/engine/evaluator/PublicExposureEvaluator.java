package dev.nichar.facepalm.engine.evaluator;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import dev.nichar.facepalm.engine.FileContext;
import dev.nichar.facepalm.engine.Finding;
import dev.nichar.facepalm.engine.GitIgnoreService;


@Named
@Singleton
class PublicExposureEvaluator implements FindingEvaluator {

    private final GitIgnoreService gitIgnoreService;

    @Inject
    public PublicExposureEvaluator(GitIgnoreService gitIgnoreService) {
        this.gitIgnoreService = gitIgnoreService;
    }

    @Override
    public void evaluate(Finding finding, FileContext context) {
        if (gitIgnoreService.isIgnored(context.getPath())) {
            finding.log("Recursive .gitignore Match (Low Exposure)", -40, 0);
        }
    }
}
