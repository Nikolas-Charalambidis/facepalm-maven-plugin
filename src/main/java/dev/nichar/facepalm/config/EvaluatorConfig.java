package dev.nichar.facepalm.config;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * Heuristic configuration for evaluating the risk and legitimacy of discovered secrets.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EvaluatorConfig {

    /**
     * Regex to detect interpolated values or placeholders like ${API_KEY}.
     */
    private String interpolationPatternRegex = ".*?(?:\\$\\{.*}|\\{\\{.*}|<.*>|%.*%|\\[.*]).*";

    /**
     * Compiled interpolation regex for performance.
     */
    private transient Pattern interpolationPattern;

    /**
     * File extensions that increase the risk score.
     */
    private Set<String> highRiskExtensions = Set.of(".env", ".properties", ".yml", ".yaml", ".conf", ".ini");

    /**
     * File extensions that decrease the risk score.
     */
    private Set<String> lowRiskExtensions = Set.of(".md", ".txt", ".csv", ".log", ".example", ".sample");

    /**
     * Keywords indicating a match is likely a placeholder or fake data.
     */
    private Set<String> dummyKeywords = Set.of(
        "dummy", "your_api_key", "insert_here", "placeholder", "place_holder", "replace_me", "changeme", "change_me");

    /**
     * Path segments indicating production-like environments.
     */
    private List<String> prodPathMarkers = List.of("src/main/", ".env", "config");

    /**
     * Path segments indicating test or mock environments.
     */
    private List<String> testPathMarkers = List.of("test", "mock", "spec");

    /**
     * Surrounding code keywords indicating production use.
     */
    private List<String> prodContextMarkers = List.of("prod", "live");

    /**
     * Surrounding code keywords indicating mock or example use.
     */
    private List<String> mockContextMarkers = List.of("example", "dummy", "fake", "mock");

    /**
     * Returns the compiled interpolation pattern, initializing it if necessary.
     */
    public Pattern getInterpolationPattern() {
        if (interpolationPattern == null) {
            interpolationPattern = Pattern.compile(interpolationPatternRegex);
        }
        return interpolationPattern;
    }
}
