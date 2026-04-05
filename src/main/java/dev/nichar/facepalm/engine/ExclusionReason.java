package dev.nichar.facepalm.engine;

import lombok.Getter;
import lombok.RequiredArgsConstructor;


/**
 * Categorizes reasons for file exclusion from the scanning process.
 */
@Getter
@RequiredArgsConstructor
public enum ExclusionReason {

    /**
     * Non-text asset detected.
     */
    BINARY_FILE("Binary file detected"),

    /**
     * File path matches a configured exclusion filter.
     */
    REGEX_MATCH("Path matched exclusion regex"),

    /**
     * File size exceeds the safety threshold for in-memory analysis.
     */
    SIZE_EXCEEDED("File size exceeds limit"),

    /**
     * Hidden file or directory skipped by default.
     */
    HIDDEN_PATH("Hidden file or directory"),

    /**
     * File could not be accessed due to filesystem constraints.
     */
    IO_ERROR("Unreadable/Access denied");

    /**
     * Localized description for reporting and diagnostic logging.
     */
    private final String description;
}
