package dev.nichar.facepalm.engine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.plugin.logging.Log;

import dev.nichar.facepalm.engine.evaluator.FindingEvaluator;
import dev.nichar.facepalm.engine.extractor.SecretExtractor;
import dev.nichar.facepalm.engine.postprocessor.FileFindingsPostProcessor;
import lombok.Getter;


@Named
@Singleton
public class ScannerEngine {
    private final Log log;

    @Inject
    private dev.nichar.facepalm.FacepalmConfig context;

    @Inject
    private List<SecretExtractor> extractors;

    @Inject
    private List<FindingEvaluator> evaluators;

    @Inject
    private List<FileFindingsPostProcessor> fileProcessors;

    @Getter
    private final ScanStatistics stats = new ScanStatistics();

    @Inject
    public ScannerEngine(@Nullable Log log) {
        this.log = log != null ? log : new org.apache.maven.plugin.logging.SystemStreamLog();
    }

    public List<Finding> scan(Path root) throws InterruptedException, IOException {
        // 1. Capture the config on the MAIN thread
        final dev.nichar.facepalm.FacepalmConfig currentConfig = context;
        final var engineConfig = currentConfig.getEngine();

        List<Callable<List<Finding>>> tasks = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(root)) {
            final List<Path> discoveredPaths = paths.filter(Files::isRegularFile).toList();
            log.info("Discovered " + discoveredPaths.size() + " files...");
            for (var path : discoveredPaths) {
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

        // Use try-with-resources for ExecutorService if on Java 19+,
        // otherwise manual shutdown is required.
        ExecutorService executor = Executors.newFixedThreadPool(engineConfig.getThreads());
        try {
            CompletionService<List<Finding>> service = new ExecutorCompletionService<>(executor);
            for (Callable<List<Finding>> task : tasks) {
                service.submit(task);
            }

            List<Finding> allFindings = new ArrayList<>();
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

    private boolean shouldScan(Path path, ScanStatistics stats) {
        final var engineConfig = context.getEngine();
        Set<String> skipDirs = engineConfig.getSkipDirs();
        for (Path element : path) {
            if (skipDirs.contains(element.toString())) {
                if (engineConfig.isShowSkipped()) {
                    log.debug("Skipping file in excluded directory [" + element + "]: " + path);
                }
                stats.recordExclusion(ExclusionReason.REGEX_MATCH);
                return false;
            }
        }
        String fileName = path.getFileName().toString();
        if (fileName.toLowerCase().matches(engineConfig.getSkipBinaryRegex())) {
            if (engineConfig.isShowSkipped()) {
                log.debug("Skipping binary file: " + path);
            }
            stats.recordExclusion(ExclusionReason.BINARY_FILE);
            return false;
        }
        try {
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

    private List<Finding> processFile(Path path) {
        final var engineConfig = context.getEngine();

        if (engineConfig.isShowProcessed()) {
            log.debug("Processing file: " + path);
        }
        try {
            String content = Files.readString(path);
            FileContext context = new FileContext(path, content, Arrays.asList(content.split("\\R")));
            List<Finding> fileFindings = new ArrayList<>();
            for (SecretExtractor ex : extractors) {
                fileFindings.addAll(ex.extract(context));
            }
            for (Finding f : fileFindings) {
                for (FindingEvaluator ev : evaluators) {
                    ev.evaluate(f, context);
                }
            }
            for (FileFindingsPostProcessor pp : fileProcessors) {
                pp.process(fileFindings, context);
            }
            return fileFindings;
        } catch (java.nio.charset.MalformedInputException e) {
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
