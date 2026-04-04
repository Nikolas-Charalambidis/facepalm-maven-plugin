package dev.nichar.facepalm.engine;

import lombok.Getter;
import lombok.RequiredArgsConstructor;


/**
 * Defines the specific reasons why a file was excluded from the scanning process.
 * These constants are used to populate {@link ScanStatistics} for detailed reporting
 * on why certain files were skipped (e.g., being too large or recognized as binary).
 */
@Getter
@RequiredArgsConstructor
public enum ExclusionReason {

    /**
     * File matches binary regex (e.g., .exe, .jpg).
     */
    BINARY_FILE("Binary file detected"),

    /**
     * File/Directory matches a user-defined skip pattern.
     */
    REGEX_MATCH("Path matched exclusion regex"),

    /**
     * File exceeds the maximum byte threshold to prevent {@link java.lang.OutOfMemoryError}.
     */
    SIZE_EXCEEDED("File size exceeds limit"),

    /**
     * File starts with a dot or is marked hidden by the OS.
     */
    HIDDEN_PATH("Hidden file or directory"),

    /**
     * File could not be accessed due to permissions or locking.
     */
    IO_ERROR("Unreadable/Access denied");

    /**
     * Human-readable explanation for logs and reports.
     */
    private final String description;
}
