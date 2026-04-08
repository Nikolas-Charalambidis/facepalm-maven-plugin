/*
 * Licensed under Apache-2.0.
 * Copyright (c) 2026 Nikolas Charalambidis.
 * All rights reserved.
 */

package dev.nichar.facepalm.pattern;

import java.util.regex.Pattern;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Data Transfer Object for a secret detection signature.
 * Encapsulates discovery regex logic and initial heuristic weights.
 *
 * @author Nikolas Charalambidis
 * @since 1.0.0
 */
@Getter
@RequiredArgsConstructor
public class SecretPattern {

    /**
     * Label for the detection pattern (e.g., "AWS Access Key").
     */
    private final String name;

    /**
     * Compiled regex for scanning file content.
     */
    private final Pattern pattern;

    /**
     * Initial threat score (0-100) based on the secret's sensitivity.
     */
    private final int baseRisk;

    /**
     * Initial confidence level (0-100) reflecting signature reliability.
     */
    private final int baseConfidence;

    /**
     * True if the pattern matches across multiple lines (e.g., PEM blocks).
     */
    private final boolean isMultiLine;
}
