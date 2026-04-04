package dev.nichar.facepalm.engine;

import lombok.Getter;
import lombok.RequiredArgsConstructor;


/**
 * Defines finding risk levels and their reporting icons.
 */
@Getter
@RequiredArgsConstructor
public enum Severity {

    /**
     * Low-risk secrets or informational findings.
     */
    INFO("⚪"),

    /**
     * Moderate-risk findings that may require attention.
     */
    WARNING("🟡"),

    /**
     * High-risk, high-confidence secrets.
     */
    ERROR("🔴");

    private final String icon;
}
