package dev.nichar.facepalm.config;

import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;


/**
 * Configuration for the scanning engine execution and file filtering.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EngineConfig {

    /**
     * Default directories to ignore during file traversal.
     */
    public static final Set<String> SKIP_DIRS = Set.of(".git", ".idea");

    /**
     * Number of concurrent threads used for scanning.
     */
    private Integer threads = Runtime.getRuntime().availableProcessors();

    /**
     * Maximum file size in bytes; larger files are ignored to prevent memory exhaustion.
     */
    @Getter
    private long maxFileSizeBytes = 5 * 1024 * 1024;

    /**
     * Regex for identifying binary or non-text files to exclude from the scan.
     */
    @Getter
    private String skipBinaryRegex = ".*\\.(png|jpg|jpeg|gif|pdf|zip|jar|class|tar|gz|exe|dll)$";

    /**
     * Directories to skip during the recursive scan.
     */
    private Set<String> skipDirs;

    /**
     * If true, logs every file that was successfully analyzed.
     */
    @Getter
    private boolean showProcessed = false;

    /**
     * If true, logs details about skipped files.
     */
    @Getter
    private boolean showSkipped = false;

    /**
     * Returns the configured thread count or defaults to available processors.
     */
    public Integer getThreads() {
        return threads != null ? threads : Runtime.getRuntime().availableProcessors();
    }

    /**
     * Returns the user-defined skip list or falls back to internal defaults.
     */
    public Set<String> getSkipDirs() {
        return skipDirs != null ? skipDirs : SKIP_DIRS;
    }
}
