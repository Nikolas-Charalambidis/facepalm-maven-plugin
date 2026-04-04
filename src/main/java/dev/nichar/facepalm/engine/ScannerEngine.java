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
 * The core multithreaded engine responsible for file discovery, filtering, and secret extraction.
 * It manages a pool of workers to process files in parallel, applying extractors, evaluators, and post-processors.
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
     * Performs a full directory scan, discovering files and processing them across multiple threads.
     *
     * @param root The root directory to begin the recursive file walk.
     * @return A list of all identified {@link Finding} objects across all processed files.
     * @throws InterruptedException If the thread pool is interrupted during execution.
     * @throws IOException If an error occurs while accessing the file system.
     */
    @Nonnull
    public List<Finding> scan(@Nonnull final Path root) throws InterruptedException, IOException {
        // 1. Capture the config on the MAIN thread
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

        // Configures a thread pool based on the user-defined thread count in the POM.
        // TODO: Java 19:  Use try-with-resources for ExecutorService if on Java 19+, otherwise manual shutdown is required.
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
     * Evaluates a file against exclusion criteria such as directory blacklists, binary regex, and size limits.
     *
     * @param path The path of the file to check.
     * @param stats The statistics object to record the specific reason for exclusion.
     * @return {@code true} if the file should be scanned, {@code false} otherwise.
     */
    private boolean shouldScan(@Nonnull final Path path, @Nonnull final ScanStatistics stats) {
        final var engineConfig = context.getEngine();
        final var skipDirs = engineConfig.getSkipDirs();
        for (final var element : path) {
            if (skipDirs.contains(element.toString())) {
                if (engineConfig.isShowSkipped()) {
                    log.debug("Skipping file in excluded directory [" + element + "]: " + path);
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
            // Avoids java.lang.OutOfMemoryError by skipping massive logs or data dumps.
            if (Files.size(path) > engineConfig.getMaxFileSizeBytes()) {
                if (engineConfig.isShowSkipped()) {
                    log.debug("Skipping large file (> " + engineConfig.getMaxFileSizeBytes() + " bytes): " + path);
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
     * Reads the file content and executes the extraction pipeline (extract -> evaluate -> post-process).
     *
     * @param path The path of the file to process.
     * @return A list of findings discovered within the file.
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
