package dev.nichar.facepalm.engine;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * Structured output of a security scan, aggregating findings, metadata, and rule definitions.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScanReport {

    /**
     * Environment data including scanner version and execution duration.
     */
    private RunMetadata metadata;

    /**
     * Statistical overview of scan results and severity breakdowns.
     */
    private ScanSummary summary;

    /**
     * Lookup map for rule details referenced in the findings.
     */
    private Map<String, RuleDefinition> ruleDictionary;

    /**
     * List of unique security leaks discovered and deduplicated.
     */
    private List<UniqueLeak> leaks;

    /**
     * Represents a unique secret or vulnerability found in one or more locations.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UniqueLeak {

        /**
         * ID of the primary rule that flagged this finding.
         */
        private String primaryRuleId;

        /**
         * IDs of additional rules that also flagged this secret.
         */
        private List<String> supplementalRuleIds;

        /**
         * Final calculated risk level (0-100).
         */
        private int totalRisk;

        /**
         * Final calculated confidence level (0-100).
         */
        private int totalConfidence;

        /**
         * Composite threat level calculated via a {@link ScoringStrategy}.
         */
        private double aggregateScore;

        private String secret;

        private String maskedSecret;

        /**
         * Unique hash for tracking this leak across scans.
         */
        private String hash;

        /**
         * List of every file and line where this exact secret occurs.
         */
        private List<Occurrence> occurrences;

        /**
         * Log of score adjustments during evaluation.
         */
        private List<String> scoreHistory;
    }

    /**
     * Descriptive metadata for a security rule.
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
         * Guidance on rotating or invalidating the leaked secret.
         */
        private String remediation;

        /**
         * Metadata tags for filtering (e.g., "cloud", "aws").
         */
        private List<String> tags;

        private String severity;
    }

    /**
     * Location of a leak within the filesystem.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Occurrence {

        /**
         * Path relative to the project root.
         */
        private String relativePath;

        /**
         * Full system path for local triage.
         */
        private String absolutePath;

        private int lineNumber;

        /**
         * Code snippet surrounding the finding for context.
         */
        private String snippet;
    }

    /**
     * Environment and timing data for the scan execution.
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
     * High-level summary of the scan results.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScanSummary {

        /**
         * Total number of unique secrets identified.
         */
        private int totalLeaksFound;

        /**
         * Cumulative count of all secret detections.
         */
        private int totalOccurrences;

        private int filesScanned;

        private int criticalCount;

        private int warningCount;
    }
}
