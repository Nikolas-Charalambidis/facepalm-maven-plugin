package dev.nichar.facepalm.engine.postprocessor;

import java.util.List;

import dev.nichar.facepalm.engine.FileContext;
import dev.nichar.facepalm.engine.Finding;


public interface FileFindingsPostProcessor {
    void process(List<Finding> fileFindings, FileContext context);
}
