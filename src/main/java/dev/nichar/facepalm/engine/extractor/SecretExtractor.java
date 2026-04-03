package dev.nichar.facepalm.engine.extractor;

import java.util.List;

import dev.nichar.facepalm.engine.FileContext;
import dev.nichar.facepalm.engine.Finding;


public interface SecretExtractor {

    List<Finding> extract(FileContext context);
}
