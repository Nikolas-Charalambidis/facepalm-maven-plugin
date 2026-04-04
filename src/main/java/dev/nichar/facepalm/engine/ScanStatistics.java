package dev.nichar.facepalm.engine;

import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

import javax.annotation.Nonnull;

import lombok.Data;


/**
 * Thread-safe container for real-time metrics during a scanning session.
 */
@Data
public class ScanStatistics {

    private final long startTimeMillis = System.currentTimeMillis();

    /**
     * Total number of files found during directory walking.
     */
    private final LongAdder filesDiscovered = new LongAdder();

    /**
     * Total number of files that passed filters and were scanned.
     */
    private final LongAdder filesScanned = new LongAdder();

    /**
     * Count of scans per file extension.
     */
    private final ConcurrentHashMap<String, LongAdder> suffixCounts = new ConcurrentHashMap<>();

    /**
     * Breakdown of file exclusions by reason.
     */
    private final ConcurrentHashMap<ExclusionReason, LongAdder> exclusionBreakdown = new ConcurrentHashMap<>();

    /**
     * Increments the total discovery counter.
     */
    public void recordDiscovery() {
        filesDiscovered.increment();
    }

    /**
     * Records a successful scan and tracks the file's extension.
     */
    public void recordScan(@Nonnull final Path path) {
        filesScanned.increment();
        final var fileName = path.getFileName().toString();
        final var lastDot = fileName.lastIndexOf('.');
        final var suffix = (lastDot == -1) ? "no-extension" : fileName.substring(lastDot).toLowerCase();
        suffixCounts.computeIfAbsent(suffix, k -> new LongAdder()).increment();
    }

    /**
     * Records a file exclusion reason.
     */
    public void recordExclusion(ExclusionReason reason) {
        exclusionBreakdown.computeIfAbsent(reason, k -> new LongAdder()).increment();
    }

    /**
     * Returns the scan duration in milliseconds.
     */
    public long getDuration() {
        return System.currentTimeMillis() - startTimeMillis;
    }
}
