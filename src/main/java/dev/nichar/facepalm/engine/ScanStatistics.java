package dev.nichar.facepalm.engine;

import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

import javax.annotation.Nonnull;

import lombok.Data;


/**
 * A thread-safe container for real-time metrics during a scanning session.
 * It uses {@link LongAdder} and {@link ConcurrentHashMap} to ensure high performance
 * and accurate counting across multiple worker threads without significant contention.
 */
@Data
public class ScanStatistics {

    private final long startTimeMillis = System.currentTimeMillis();

    /**
     * Total number of files found during the initial directory walk.
     */
    private final LongAdder filesDiscovered = new LongAdder();

    /**
     * Total number of files that passed all filters and were actually scanned for secrets.
     */
    private final LongAdder filesScanned = new LongAdder();

    /**
     * Maps file extensions (e.g., ".java", ".env") to the number of times they were scanned.
     */
    private final ConcurrentHashMap<String, LongAdder> suffixCounts = new ConcurrentHashMap<>();

    /**
     * Maps {@link ExclusionReason} to the count of files skipped for that specific reason.
     */
    private final ConcurrentHashMap<ExclusionReason, LongAdder> exclusionBreakdown = new ConcurrentHashMap<>();

    /**
     * Increments the total discovery counter.
     * Called for every file encountered during the recursive file walk.
     */
    public void recordDiscovery() {
        filesDiscovered.increment();
    }

    /**
     * Records a successful file scan and tracks the file's extension.
     *
     * @param path The path of the file that was successfully processed.
     */
    public void recordScan(@Nonnull final Path path) {
        filesScanned.increment();
        final var fileName = path.getFileName().toString();
        final var lastDot = fileName.lastIndexOf('.');
        final var suffix = (lastDot == -1) ? "no-extension" : fileName.substring(lastDot).toLowerCase();
        suffixCounts.computeIfAbsent(suffix, k -> new LongAdder()).increment();
    }

    /**
     * Increments the counter for a specific exclusion reason (e.g., Binary or Large File).
     *
     * @param reason The specific reason the file was excluded from the scan.
     */
    public void recordExclusion(ExclusionReason reason) {
        exclusionBreakdown.computeIfAbsent(reason, k -> new LongAdder()).increment();
    }

    /**
     * Calculates the total execution time of the scan.
     *
     * @return The duration in milliseconds from the creation of this object to the current call.
     */
    public long getDuration() {
        return System.currentTimeMillis() - startTimeMillis;
    }
}
