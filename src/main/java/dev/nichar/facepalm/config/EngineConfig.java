package dev.nichar.facepalm.config;

import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;


/**
 * Configuration for the scanning engine execution and file filtering.
 *
 * @author Nikolas Charalambidis
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EngineConfig {

    /**
     * Default directory names to ignore during file traversal if no overrides are provided.
     */
    public static final Set<String> SKIP_DIRS = Set.of(".git", ".idea");

    /**
     * The number of concurrent threads used for scanning.
     * Defaults to the number of available processors on the host system.
     */
    private Integer threads = Runtime.getRuntime().availableProcessors();

    /**
     * The maximum allowed size of a file (in bytes) to be scanned.
     * Files exceeding this limit are ignored to prevent memory exhaustion.
     * Default is 5MB.
     */
    @Getter
    private long maxFileSizeBytes = 5 * 1024 * 1024;

    /**
     * A regular expression used to identify binary or non-text files based on their extension.
     * Matching files are automatically excluded from the scan.
     */
    @Getter
    private String skipBinaryRegex = ".*\\.(png|jpg|jpeg|gif|pdf|zip|jar|class|tar|gz|exe|dll)$";

    /**
     * A set of directory names to be skipped during the recursive file scan.
     * If null, the engine uses the internal defaults defined in {@link #SKIP_DIRS}.
     */
    private Set<String> skipDirs;

    /**
     * When true, the engine logs a confirmation message for every file that was successfully analyzed.
     */
    @Getter
    private boolean showProcessed = false;

    /**
     * When true, the engine logs details about files that were skipped due to size or type filters.
     */
    @Getter
    private boolean showSkipped = false;

    /**
     * Resolves the number of threads to be used for scanning.
     * <p>
     * Returns the user-configured value if provided via Maven configuration;
     * otherwise falls back to the number of available processors on the host system.
     *
     * @return The effective number of threads to use, always greater than zero.
     */
    public Integer getThreads() {
        return threads != null ? threads : Runtime.getRuntime().availableProcessors();
    }

    /**
     * Resolves the directory skip-list, returning user-defined overrides or falling back to defaults.
     *
     * @return A non-null set of directory names to ignore during the scan.
     */
    public Set<String> getSkipDirs() {
        return skipDirs != null ? skipDirs : SKIP_DIRS;
    }
}
