package dev.nichar.facepalm.engine;

import lombok.Getter;
import lombok.RequiredArgsConstructor;


@Getter
@RequiredArgsConstructor
public enum Severity {

    INFO("⚪"),
    WARNING("🟡"),
    ERROR("🔴");

    private final String icon;
}
