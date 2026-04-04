package dev.nichar.facepalm.report;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;


@Data
@Builder
@Jacksonized // Helps Jackson work with Lombok's @Value and @Builder
public class FindingReport {

    private String patternName;
    private String fileAbsolutePath;
    private int lineNumber;
    private String maskedSecret;
    private String contextSnippet;

    // Pre-calculated values
    private double finalScore;
    private String finalSeverity;

    // Optional: Include raw scores for transparency in the report
    private int riskScore;
    private int confidenceScore;
}
