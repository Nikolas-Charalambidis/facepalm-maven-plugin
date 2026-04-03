package dev.nichar.facepalm.engine.evaluator;


import dev.nichar.facepalm.engine.FileContext;
import dev.nichar.facepalm.engine.Finding;


public interface FindingEvaluator {

    void evaluate(Finding finding, FileContext context);
}
