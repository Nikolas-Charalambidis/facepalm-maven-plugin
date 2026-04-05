package dev.nichar.facepalm.report;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;


/**
 * Data Transfer Object (DTO) for findings, used to serialize scan results to JSON
 * and then deserialize them for the reporting phase.
 */
@Data
@Builder
@Jacksonized // Ensures seamless Jackson interoperability with Lombok's Builder pattern.
public class FindingReport {

    /**
     * The name of the pattern that identified this potential secret.
     */
    private String patternName;

    /**
     * Absolute filesystem path to the file containing the finding.
     */
    private String fileAbsolutePath;

    /**
     * 1-based line number where the finding was located.
     */
    private int lineNumber;

    /**
     * A partially obfuscated version of the secret for safe display in reports.
     */
    private String maskedSecret;

    /**
     * A snippet of the surrounding code to provide context for the developer.
     */
    private String contextSnippet;

    /**
     * The final threat score calculated by the engine's scoring strategy.
     */
    private double finalScore;

    /**
     * The human-readable severity level (e.g., ERROR, WARNING).
     */
    private String finalSeverity;

    /**
     * The raw risk score before confidence weighting.
     */
    private int riskScore;

    /**
     * The confidence level of the detection.
     */
    private int confidenceScore;
}
