package dev.nichar.facepalm.engine;

import java.io.IOException;
import java.nio.charset.MalformedInputException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;

import dev.nichar.facepalm.FacepalmConfig;
import dev.nichar.facepalm.engine.evaluator.FindingEvaluator;
import dev.nichar.facepalm.engine.extractor.SecretExtractor;
import dev.nichar.facepalm.engine.postprocessor.FileFindingsPostProcessor;
import lombok.Getter;


/**
 * Concurrent engine for file discovery, filtering, and automated secret extraction.
 * Orchestrates parallel processing of candidate files through the detection pipeline.
 */
@Named
@Singleton
public class ScannerEngine {

    private final Log log;

    @Inject
    private FacepalmConfig context;

    @Inject
    private List<SecretExtractor> extractors;

    @Inject
    private List<FindingEvaluator> evaluators;

    @Inject
    private List<FileFindingsPostProcessor> fileProcessors;

    @Getter
    private final ScanStatistics stats = new ScanStatistics();

    @Inject
    public ScannerEngine(@Nullable final Log log) {
        this.log = log != null ? log : new SystemStreamLog();
    }

    /**
     * Recursively traverses the root directory and executes parallel scans on qualified files.
     *
     * @param root Root directory to scan.
     * @return List of identified findings.
     */
    @Nonnull
    public List<Finding> scan(@Nonnull final Path root) throws InterruptedException, IOException {
        // Capture the config on the MAIN thread
        final var currentConfig = context;
        final var engineConfig = currentConfig.getEngine();

        final List<Callable<List<Finding>>> tasks = new ArrayList<>();
        try (final var paths = Files.walk(root)) {
            final var discoveredPaths = paths.filter(Files::isRegularFile).toList();
            log.info("Discovered " + discoveredPaths.size() + " files...");
            for (final var path : discoveredPaths) {
                stats.recordDiscovery();
                if (!shouldScan(path, stats)) {
                    continue;
                }
                stats.recordScan(path);
                tasks.add(() -> processFile(path));
            }
        }

        if (tasks.isEmpty()) {
            log.info("No files qualified to scan.");
            return Collections.emptyList();
        }

        log.info("Starting scan of " + tasks.size() + " files...");

        // Process files in parallel.
        final var executor = Executors.newFixedThreadPool(engineConfig.getThreads());
        try {
            final CompletionService<List<Finding>> service = new ExecutorCompletionService<>(executor);
            for (final var task : tasks) {
                service.submit(task);
            }

            final List<Finding> allFindings = new ArrayList<>();
            for (int i = 0; i < tasks.size(); i++) {
                try {
                    allFindings.addAll(service.take().get());
                } catch (Exception e) {
                    log.error("Error processing file", e);
                }
            }
            return allFindings;
        } finally {
            executor.shutdown();
        }
    }

    /**
     * Checks if a file should be scanned based on size, extension, and exclusion rules.
     */
    private boolean shouldScan(@Nonnull final Path path, @Nonnull final ScanStatistics stats) {
        final var engineConfig = context.getEngine();
        final var skipDirs = engineConfig.getSkipDirs();
        for (final var element : path) {
            if (skipDirs.contains(element.toString())) {
                if (engineConfig.isShowSkipped()) {
                    log.debug("Skipping excluded directory: " + path);
                }
                stats.recordExclusion(ExclusionReason.REGEX_MATCH);
                return false;
            }
        }

        final var fileName = path.getFileName().toString();
        // Filters out images, zip files, etc.
        if (fileName.toLowerCase().matches(engineConfig.getSkipBinaryRegex())) {
            if (engineConfig.isShowSkipped()) {
                log.debug("Skipping binary file: " + path);
            }
            stats.recordExclusion(ExclusionReason.BINARY_FILE);
            return false;
        }

        try {
            // Skips large files to avoid OOM.
            if (Files.size(path) > engineConfig.getMaxFileSizeBytes()) {
                if (engineConfig.isShowSkipped()) {
                    log.debug("Skipping large file: " + path);
                }
                stats.recordExclusion(ExclusionReason.SIZE_EXCEEDED);
                return false;
            }
        } catch (IOException e) {
            log.error("Could not determine size for: " + path);
            stats.recordExclusion(ExclusionReason.IO_ERROR);
            return false;
        }
        return true;
    }

    /**
     * Processes a single file through the extraction, evaluation, and post-processing pipeline.
     */
    @Nonnull
    private List<Finding> processFile(@Nonnull final Path path) {
        final var engineConfig = context.getEngine();

        if (engineConfig.isShowProcessed()) {
            log.debug("Processing file: " + path);
        }
        try {
            // Loads file into memory; size limit in shouldScan() protects 
            final var content = Files.readString(path);
            // Creates a snapshot of the file for plugins, splitting by line for easy reporting
            final var context = new FileContext(path, content, Arrays.asList(content.split("\\R")));
            final List<Finding> fileFindings = new ArrayList<>();

            // Run regex/entropy extractors.
            for (final var extractor : extractors) {
                fileFindings.addAll(extractor.extract(context));
            }

            // Decorate findings with severity or false-positive checks
            for (final var finding : fileFindings) {
                for (final var evaluator : evaluators) {
                    evaluator.evaluate(finding, context);
                }
            }

            // Global file-level modifications (e.g. deduplication).
            for (final var processor : fileProcessors) {
                processor.process(fileFindings, context);
            }

            return fileFindings;
        } catch (MalformedInputException e) {
            if (engineConfig.isShowSkipped()) {
                log.warn("Skipping " + path + " - file is not valid UTF-8 text:" + e.getMessage());
            }
            return Collections.emptyList();
        } catch (IOException e) {
            log.error("Failed to read " + path + ": " + e.getMessage());
            return Collections.emptyList();
        }
    }
}
