/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2026 Nikolas Charalambidis
 */

package dev.nichar.facepalm.engine;

import dev.nichar.facepalm.FacepalmConfig;
import dev.nichar.facepalm.config.EngineConfig;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.LongAdder;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ScannerEngineTest {

    /**
     * Verifies that files located under a hidden directory inside the scan root are excluded,
     * while sibling non-hidden files are still scanned.
     */
    @Test
    void scan_records_hidden_path_exclusion_for_hidden_ancestor(@TempDir final Path tempDir) throws Exception {
        final Path root = tempDir.resolve("project");
        final Path hiddenDir = createHiddenDirectory(root);
        Files.createDirectories(hiddenDir);
        Files.writeString(hiddenDir.resolve("secrets.txt"), "password = super-secret");

        final Path visibleDir = root.resolve("src/main/resources");
        Files.createDirectories(visibleDir);
        Files.writeString(visibleDir.resolve("application.properties"), "app.name=test");

        final ScannerEngine scanner = scannerWithMinimalContext();
        scanner.scan(root);

        final long hiddenCount = scanner.getStats().getExclusionBreakdown().get(ExclusionReason.HIDDEN_PATH).sum();
        Assertions.assertEquals(1L, hiddenCount);
        Assertions.assertEquals(1L, scanner.getStats().getFilesScanned().sum());
    }

    private Path createHiddenDirectory(final Path root) throws Exception {
        final Path dotPrefixed = root.resolve(".cache");
        Files.createDirectories(dotPrefixed);
        if (Files.isHidden(dotPrefixed)) {
            return dotPrefixed;
        }

        final Path dosHidden = root.resolve("cache-hidden-attr");
        Files.createDirectories(dosHidden);
        try {
            Files.setAttribute(dosHidden, "dos:hidden", true);
        } catch (UnsupportedOperationException | IOException ignored) {
        }

        Assumptions.assumeTrue(Files.isHidden(dosHidden),
            "No supported hidden-directory mechanism on this filesystem");
        return dosHidden;
    }

    @Test
    void scan_scans_non_hidden_files_without_hidden_exclusion(@TempDir final Path tempDir) throws Exception {
        final Path root = tempDir.resolve("project");
        final Path sourceDir = root.resolve("src");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("app.properties"), "token=abc123");

        final ScannerEngine scanner = scannerWithMinimalContext();
        scanner.scan(root);

        Assertions.assertEquals(1L, scanner.getStats().getFilesScanned().sum());
        Assertions.assertEquals(0L,
            scanner.getStats().getExclusionBreakdown().getOrDefault(ExclusionReason.HIDDEN_PATH, new LongAdder())
                .sum());
    }

    @Test
    void scan_ignores_hidden_ancestor_outside_scan_root(@TempDir final Path tempDir) throws Exception {
        final Path root = tempDir.resolve(".workspace").resolve("project");
        final Path sourceDir = root.resolve("src");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("app.properties"), "token=abc123");

        final ScannerEngine scanner = scannerWithMinimalContext();
        scanner.scan(root);

        Assertions.assertEquals(1L, scanner.getStats().getFilesScanned().sum());
        Assertions.assertEquals(0L,
            scanner.getStats().getExclusionBreakdown().getOrDefault(ExclusionReason.HIDDEN_PATH, new LongAdder())
                .sum());
    }

    private ScannerEngine scannerWithMinimalContext() throws Exception {
        final ScannerEngine scanner = new ScannerEngine(new SystemStreamLog());
        final FacepalmConfig config = new FacepalmConfig();
        final EngineConfig engine = new EngineConfig();
        engine.setThreads(1);
        config.setEngine(engine);
        setField(scanner, "context", config);
        setField(scanner, "extractors", List.of());
        setField(scanner, "evaluators", List.of());
        setField(scanner, "fileProcessors", List.of());
        return scanner;
    }

    private static void setField(final Object instance, final String fieldName, final Object value)
            throws ReflectiveOperationException {
        final Field field = instance.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(instance, value);
    }
}
