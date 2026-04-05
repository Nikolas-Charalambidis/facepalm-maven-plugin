package dev.nichar.facepalm.engine;

import lombok.Getter;
import lombok.RequiredArgsConstructor;


/**
 * Classifies findings into actionable risk levels.
 */
@Getter
@RequiredArgsConstructor
public enum Severity {

    /**
     * Low-risk detections or informational insights.
     */
    INFO("⚪"),

    /**
     * Moderate-risk findings requiring review.
     */
    WARNING("🟡"),

    /**
     * Critical findings that typically trigger build failure.
     */
    ERROR("🔴");

    private final String icon;
}
