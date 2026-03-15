package dev.nichar.facepalm.pattern;

import java.util.regex.Pattern;

import lombok.Getter;
import lombok.RequiredArgsConstructor;


/**
 * Data Transfer Object representing a single secret detection signature.
 * * This class encapsulates the regular expression logic along with the
 * initial scoring weights used by the heuristic engine.
 *
 * @author Nikolas Charalambidis
 * @since 1.0.0
 */
@Getter
@RequiredArgsConstructor
public class SecretPattern {

    /**
     * The human-readable label for the pattern (e.g., "AWS Access Key").
     * Used in logs and reports to identify the type of secret found.
     */
    private final String name;

    /**
     * The compiled regular expression used to scan file content.
     */
    private final Pattern pattern;

    /**
     * The initial risk score (0-100) assigned to a match.
     * Represents the inherent "danger" of this secret type if leaked.
     */
    private final int baseRisk;

    /**
     * The initial confidence level (0-100) that a match is a true positive.
     * High values (90+) are for unique signatures; lower values (40-60) are for generic patterns.
     */
    private final int baseConfidence;

    /**
     * Flag indicating if the pattern should scan across multiple lines.
     * Essential for blocks like PEM private keys.
     */
    private final boolean isMultiLine;
}
