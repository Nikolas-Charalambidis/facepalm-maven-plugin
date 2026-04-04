package dev.nichar.facepalm.engine;

import lombok.Getter;
import lombok.RequiredArgsConstructor;


/**
 * Defines the critical levels of a finding, influencing how it is rendered in reports.
 */
@Getter
@RequiredArgsConstructor
public enum Severity {

    /**
     * Informational findings or low-risk secrets that don't break the build.
     */
    INFO("⚪"),

    /**
     * Informational findings or low-risk secrets that don't break the build.
     */
    WARNING("🟡"),

    /**
     * High-confidence, high-risk secrets in production or configuration files.
     */
    ERROR("🔴");

    private final String icon;
}
