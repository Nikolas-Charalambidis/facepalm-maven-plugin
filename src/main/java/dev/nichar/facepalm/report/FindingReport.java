package dev.nichar.facepalm.report;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;


/**
 * Data Transfer Object for scan findings.
 * Used to serialize discovery results to JSON for the reporting phase.
 */
@Data
@Builder
@Jacksonized // Ensures seamless Jackson interoperability with Lombok's Builder pattern.
public class FindingReport {

    /**
     * Name of the pattern that identified the potential secret.
     */
    private String patternName;

    /**
     * Absolute filesystem path to the source file.
     */
    private String fileAbsolutePath;

    /**
     * 1-based line number of the detection.
     */
    private int lineNumber;

    /**
     * Partially obfuscated secret for safe display.
     */
    private String maskedSecret;

    /**
     * Snippet of surrounding code for context.
     */
    private String contextSnippet;

    /**
     * Composite threat score calculated by the engine.
     */
    private double finalScore;

    /**
     * Resolved severity level (e.g., ERROR, WARNING).
     */
    private String finalSeverity;

    /**
     * Raw risk score before weighting.
     */
    private int riskScore;

    /**
     * Confidence level of the detection.
     */
    private int confidenceScore;
}
