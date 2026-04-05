package dev.nichar.facepalm.config;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * Configuration for heuristic evaluators that refine discovery risk and confidence.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EvaluatorConfig {

    /**
     * Regex for detecting interpolated values or variable placeholders.
     */
    private String interpolationPatternRegex = ".*?(?:\\$\\{.*}|\\{\\{.*}|<.*>|%.*%|\\[.*]).*";

    /**
     * Compiled pattern for efficient placeholder detection.
     */
    private transient Pattern interpolationPattern;

    /**
     * Extensions that typically contain production credentials.
     */
    private Set<String> highRiskExtensions = Set.of(".env", ".properties", ".yml", ".yaml", ".conf", ".ini");

    /**
     * Extensions that usually contain non-sensitive documentation or data.
     */
    private Set<String> lowRiskExtensions = Set.of(".md", ".txt", ".csv", ".log", ".example", ".sample");

    /**
     * Keywords signaling a discovery is likely dummy or template data.
     */
    private Set<String> dummyKeywords = Set.of(
        "dummy", "your_api_key", "insert_here", "placeholder", "place_holder", "replace_me", "changeme", "change_me");

    /**
     * Path segments suggesting production-critical code.
     */
    private List<String> prodPathMarkers = List.of("src/main/", ".env", "config");

    /**
     * Path segments suggesting test or non-production environments.
     */
    private List<String> testPathMarkers = List.of("test", "mock", "spec");

    /**
     * Surrounding keywords indicating live environment secrets.
     */
    private List<String> prodContextMarkers = List.of("prod", "live");

    /**
     * Surrounding keywords indicating mock or example credentials.
     */
    private List<String> mockContextMarkers = List.of("example", "dummy", "fake", "mock");

    /**
     * Resolves the compiled interpolation pattern for discovery validation.
     */
    public Pattern getInterpolationPattern() {
        if (interpolationPattern == null) {
            interpolationPattern = Pattern.compile(interpolationPatternRegex);
        }
        return interpolationPattern;
    }
}
