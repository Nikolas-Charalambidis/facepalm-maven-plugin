package dev.nichar.facepalm.engine;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScanReport {
    private RunMetadata metadata;

    private ScanSummary summary;

    private Map<String, RuleDefinition> ruleDictionary;

    private List<UniqueLeak> leaks;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UniqueLeak {
        private String primaryRuleId;

        private List<String> supplementalRuleIds;

        private int totalRisk;

        private int totalConfidence;

        private double aggregateScore;

        private String secret;

        private String maskedSecret;

        private String fingerprint;

        private List<Occurrence> occurrences;

        private List<String> scoreHistory;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RuleDefinition {
        private String id;

        private String name;

        private String description;

        private String remediation;

        private List<String> tags;

        private String severity;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Occurrence {
        private String relativePath;

        private String absolutePath;

        private int lineNumber;

        private String snippet;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RunMetadata {
        private String scannerVersion;

        private String timestamp;

        private long durationMs;

        private String rootPath;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScanSummary {
        private int totalLeaksFound;

        private int totalOccurrences;

        private int filesScanned;

        private int criticalCount;

        private int warningCount;
    }
}
