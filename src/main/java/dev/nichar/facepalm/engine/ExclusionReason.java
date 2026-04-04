package dev.nichar.facepalm.engine;

import lombok.Getter;
import lombok.RequiredArgsConstructor;


/**
 * Reasons why a file was excluded from scanning.
 */
@Getter
@RequiredArgsConstructor
public enum ExclusionReason {

    /**
     * File identified as binary (e.g., .exe, .jpg).
     */
    BINARY_FILE("Binary file detected"),

    /**
     * Path matches a user-defined exclusion pattern.
     */
    REGEX_MATCH("Path matched exclusion regex"),

    /**
     * File size exceeds the configured maximum.
     */
    SIZE_EXCEEDED("File size exceeds limit"),

    /**
     * Path is hidden or starts with a dot.
     */
    HIDDEN_PATH("Hidden file or directory"),

    /**
     * File is unreadable due to permissions or I/O errors.
     */
    IO_ERROR("Unreadable/Access denied");

    /**
     * Human-readable description for logs and reports.
     */
    private final String description;
}
