package dev.nichar.facepalm.config;

import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


/**
 * Configuration for the scanning engine execution and file filtering.
 *
 * @author Nikolas Charalambidis
 * @since 1.0.0
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class EngineConfig {

    /**
     * Default directory names to ignore during file traversal if no overrides are provided.
     */
    private static final Set<String> DEFAULTS = Set.of(".git", ".idea");

    /**
     * The number of concurrent threads used for scanning.
     * Defaults to the number of available processors on the host system.
     */
    private int threads = Runtime.getRuntime().availableProcessors();

    /**
     * The maximum allowed size of a file (in bytes) to be scanned.
     * Files exceeding this limit are ignored to prevent memory exhaustion.
     * Default is 5MB.
     */
    private long maxFileSizeBytes = 5 * 1024 * 1024;

    /**
     * A regular expression used to identify binary or non-text files based on their extension.
     * Matching files are automatically excluded from the scan.
     */
    private String skipBinaryRegex = ".*\\.(png|jpg|jpeg|gif|pdf|zip|jar|class|tar|gz|exe|dll)$";

    /**
     * A set of directory names to be skipped during the recursive file scan.
     * If null, the engine uses the internal defaults defined in {@link #DEFAULTS}.
     */
    private Set<String> skipDirs;

    /**
     * When true, the engine logs a confirmation message for every file that was successfully analyzed.
     */
    private boolean showProcessed = false;

    /**
     * When true, the engine logs details about files that were skipped due to size or type filters.
     */
    private boolean showSkipped = false;

    /**
     * Resolves the directory skip-list, returning user-defined overrides or falling back to defaults.
     *
     * @return A non-null set of directory names to ignore during the scan.
     */
    public Set<String> getEffectiveSkipDirs() {
        return skipDirs != null ? skipDirs : DEFAULTS;
    }
}
