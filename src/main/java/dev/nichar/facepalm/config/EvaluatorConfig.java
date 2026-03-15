package dev.nichar.facepalm.config;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;


/**
 * Heuristic configuration used to evaluate the risk and legitimacy of discovered secrets.
 *
 * @author Nikolas Charalambidis
 * @since 1.0.0
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class EvaluatorConfig {

    /**
     * Regex used to detect interpolated values or placeholders.
     * Helps identify non-sensitive templates like ${API_KEY}.
     */
    private String interpolationPatternRegex = ".*?(?:\\$\\{.*}|\\{\\{.*}|<.*>|%.*%|\\[.*]).*";

    /**
     * Compiled version of the interpolation regex for performance.
     * Marked transient to indicate it is computed at runtime, not injected.
     */
    private transient Pattern interpolationPattern;

    /**
     * Extensions that increase the risk score. Defaults to common sensitive formats.
     */
    private Set<String> highRiskExtensions = Set.of(".env", ".properties", ".yml", ".yaml", ".conf", ".ini");

    /**
     * Extensions that decrease the risk score. Defaults to documentation and logs.
     */
    private Set<String> lowRiskExtensions = Set.of(".md", ".txt", ".csv", ".log", ".example", ".sample");

    /**
     * Keywords indicating that a match is likely a placeholder or fake data.
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
     * Keywords found in surrounding code context indicating production use.
     */
    private List<String> prodContextMarkers = List.of("prod", "live");

    /**
     * Keywords found in surrounding code context indicating mock or example use.
     */
    private List<String> mockContextMarkers = List.of("example", "dummy", "fake", "mock");

    /**
     * Lazily compiles the pattern using the current regex value.
     * This ensures that user overrides from Mojo or CLI are captured before the pattern is locked in.
     *
     * @return The compiled {@link Pattern} for template detection.
     */
    public Pattern getInterpolationPattern() {
        if (interpolationPattern == null) {
            interpolationPattern = Pattern.compile(interpolationPatternRegex);
        }
        return interpolationPattern;
    }
}
