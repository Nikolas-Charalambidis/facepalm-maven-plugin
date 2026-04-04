package dev.nichar.facepalm.engine;

import java.nio.file.Path;
import java.util.List;

import jakarta.annotation.Nonnull;

import dev.nichar.facepalm.engine.evaluator.FindingEvaluator;
import dev.nichar.facepalm.engine.extractor.SecretExtractor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Value;


/**
 * Read-only container for a file being scanned, including its path, raw content, and split lines.
 */
@Getter
@RequiredArgsConstructor
public class FileContext {

    /**
     * Filesystem path of the file.
     */
    private final Path path;

    /**
     * Raw content of the file for multi-line regex matching.
     */
    private final String fullContent;

    /**
     * Content split into lines for single-line scanning and context windowing.
     */
    private final List<String> lines;

    /**
     * Safely retrieves a line by index, returning an empty string if out of bounds.
     */
    @Nonnull
    public String getLineOrEmpty(int index) {
        return (index >= 0 && index < lines.size()) ? lines.get(index) : "";
    }
}
