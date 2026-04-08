/*
 * Licensed under Apache-2.0.
 * Copyright (c) 2026 Nikolas Charalambidis.
 * All rights reserved.
 */

package dev.nichar.facepalm.engine;

import jakarta.annotation.Nonnull;
import java.nio.file.Path;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Immutable snapshot of a file being analyzed.
 * Provides access to raw content, line-delimited text, and filesystem metadata.
 */
@Getter
@RequiredArgsConstructor
public class FileContext {

    /**
     * Filesystem path to the source file.
     */
    private final Path path;

    /**
     * Unmodified content for multi-line regex matching.
     */
    private final String fullContent;

    /**
     * Line-delimited content for single-line analysis and context extraction.
     */
    private final List<String> lines;

    /**
     * Returns the line at the specified index, or an empty string if out of bounds.
     */
    @Nonnull
    public String getLineOrEmpty(final int index) {
        return (index >= 0 && index < lines.size()) ? lines.get(index) : "";
    }
}
