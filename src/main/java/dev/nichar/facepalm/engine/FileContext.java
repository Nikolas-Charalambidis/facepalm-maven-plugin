package dev.nichar.facepalm.engine;

import java.nio.file.Path;
import java.util.List;

import javax.annotation.Nonnull;

import dev.nichar.facepalm.engine.evaluator.FindingEvaluator;
import dev.nichar.facepalm.engine.extractor.SecretExtractor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Value;


/**
 * A read-only data container representing a file being scanned.
 * It holds the file path, the full raw string content, and a pre-split list of lines
 * to allow {@link SecretExtractor} and {@link FindingEvaluator} implementations
 * to perform efficient lookups and context analysis.
 */
@Getter
@RequiredArgsConstructor
public class FileContext {

    /**
     * The filesystem path of the file.
     */
    private final Path path;

    /**
     * The full, unmodified content of the file. Used for multi-line regex matching.
     */
    private final String fullContent;

    /**
     * The content split into individual lines. Used for single-line scanning and context windowing.
     */
    private final List<String> lines;

    /**
     * Safely retrieves a specific line from the file by its index.
     * Prevents {@link IndexOutOfBoundsException} when checking context around a finding  (e.g., at the very start or end of a file).
     *
     * @param index The 0-based line index to retrieve.
     * @return The text of the line if the index is valid; otherwise an empty string.
     */
    @Nonnull
    public String getLineOrEmpty(int index) {
        return (index >= 0 && index < lines.size()) ? lines.get(index) : "";
    }
}
