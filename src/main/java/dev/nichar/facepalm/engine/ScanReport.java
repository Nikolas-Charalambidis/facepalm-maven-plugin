package dev.nichar.facepalm.engine;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * Represents the final structured output of a security scan.
 * This class acts as a Data Transfer Object (DTO) that aggregates all findings,
 * metadata, and rule definitions into a single report suitable for JSON serialization,
 * console printing, or HTML dashboard generation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScanReport {

    /**
     * High-level environment data such as scanner version, execution time, and scan duration.
     */
    private RunMetadata metadata;

    /**
     * Statistical overview of the scan, including total leak counts and severity breakdowns.
     */
    private ScanSummary summary;

    /**
     * A lookup map for rule details, allowing the report to remain compact by referencing rule ID.
     .*/
    private Map<String, RuleDefinition> ruleDictionary;

    /**
     * The list of unique security leaks discovered, deduplicated across the entire project.
     **/
    private List<UniqueLeak> leaks;

    /**
     * Represents a single unique secret or vulnerability that may appear in multiple locations.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UniqueLeak {

        /**
         * The primary identification ID of the rule that triggered this finding.
         */
        private String primaryRuleId;

        /**
         * Additional rules that also flagged this specific secret.
         */
        private List<String> supplementalRuleIds;

        /**
         * The final calculated risk level (0-100).
         **/
        private int totalRisk;

        /**
         * The final calculated confidence level (0-100).
         */
        private int totalConfidence;

        /**
         * The composite threat level calculated via a {@link ScoringStrategy}
         */
        private double aggregateScore;

        private String secret;

        private String maskedSecret;

        /**
         * A unique hash used to track this specific leak across different scans.
         * */
        private String hash;

        /**
         * A list of every file and line where this exact secret was found.
         */
        private List<Occurrence> occurrences;

        /**
         * A log of how the score changed during evaluation (e.g., "+10 for High Entropy").
         */
        private List<String> scoreHistory;
    }

    /**
     * Provides descriptive metadata for a specific security rule.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RuleDefinition {

        private String id;

        private String name;

        private String description;

        /**
         * Guidance on how to rotate or invalidate the leaked secret.
         */
        private String remediation;

        /**
         * Metadata tags for filtering (e.g., "cloud", "aws", "pci-dss").
         */
        private List<String> tags;

        private String severity;
    }

    /**
     * Pinpoints exactly where a leak was found in the filesystem.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Occurrence {

        /**
         * Path relative to the project root for portable reporting.
         */
        private String relativePath;

        /**
         * Full system path for local developer triage.
         */
        private String absolutePath;

        private int lineNumber;

        /**
         * A code snippet surrounding the finding to provide visual context.
         */
        private String snippet;
    }

    /**
     * Captured data about the environment and timing of the scan execution.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RunMetadata {

        private String scannerVersion;

        private Instant timestamp;

        private long durationMs;

        private String rootPath;
    }

    /**
     * A "management-level" summary of the scan results.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScanSummary {

        /**
         * Represents the total number of unique secrets or vulnerabilities identified during a scan.
         */
        private int totalLeaksFound;

        /**
         * Represents the cumulative count of every instance where a secret was detected.
         */
        private int totalOccurrences;

        private int filesScanned;

        private int criticalCount;

        private int warningCount;
    }
}
