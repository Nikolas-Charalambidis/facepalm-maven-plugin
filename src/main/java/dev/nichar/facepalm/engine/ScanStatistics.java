package dev.nichar.facepalm.engine;

import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

import lombok.Data;


@Data
public class ScanStatistics {
    private final long startTimeMillis = System.currentTimeMillis();

    // Summary Counters
    private final LongAdder filesDiscovered = new LongAdder();

    private final LongAdder filesScanned = new LongAdder();

    // Breakdowns
    private final ConcurrentHashMap<String, LongAdder> suffixCounts = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<ExclusionReason, LongAdder> exclusionBreakdown = new ConcurrentHashMap<>();

    /**
     * Called for every file found during the walk
     */
    public void recordDiscovery() {
        filesDiscovered.increment();
    }

    /**
     * Called when a file is actually processed
     */
    public void recordScan(Path path) {
        filesScanned.increment();
        String fileName = path.getFileName().toString();
        int lastDot = fileName.lastIndexOf('.');
        String suffix = (lastDot == -1) ? "no-extension" : fileName.substring(lastDot).toLowerCase();
        suffixCounts.computeIfAbsent(suffix, k -> new LongAdder()).increment();
    }

    /**
     * Called when a file is skipped
     */
    public void recordExclusion(ExclusionReason reason) {
        exclusionBreakdown.computeIfAbsent(reason, k -> new LongAdder()).increment();
    }

    public long getDuration() {
        return System.currentTimeMillis() - startTimeMillis;
    }
}
