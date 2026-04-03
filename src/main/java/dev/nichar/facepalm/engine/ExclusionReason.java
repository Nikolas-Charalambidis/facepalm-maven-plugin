package dev.nichar.facepalm.engine;

import lombok.Getter;
import lombok.RequiredArgsConstructor;


@Getter
@RequiredArgsConstructor
public enum ExclusionReason {

    BINARY_FILE("Binary file detected"),
    REGEX_MATCH("Path matched exclusion regex"),
    SIZE_EXCEEDED("File size exceeds limit"),
    HIDDEN_PATH("Hidden file or directory"),
    IO_ERROR("Unreadable/Access denied");

    private final String description;
}
