package dev.nichar.facepalm.engine;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Stream;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;


/**
 * Manages {@code .gitignore} files to filter paths from the scan.
 */
@Named
@Singleton
public class GitIgnoreService {

    private final Log log;

    /**
     * Maps directory paths to their compiled glob patterns.
     */
    private final TreeMap<Path, List<PathMatcher>> registry = new TreeMap<>();

    @Inject
    public GitIgnoreService(@Nullable final Log log) {
        this.log = log != null ? log : new SystemStreamLog();
    }

    /**
     * Discovers and parses all {@code .gitignore} files under the root directory.
     */
    public void loadAllGitIgnores(@Nonnull final Path root) {
        try (Stream<Path> paths = Files.walk(root)) {
            paths.filter(p -> p.getFileName() != null && p.getFileName().toString().equals(".gitignore"))
                .forEach(this::parseGitIgnoreFile);
        } catch (IOException e) {
            log.warn("Error walking for .gitignores: " + e.getMessage());
        }
    }

    /**
     * Checks if a path matches any discovered ignore patterns.
     */
    public boolean isIgnored(@Nonnull final Path filePath) {
        return registry.entrySet().stream()
            .filter(entry -> filePath.startsWith(entry.getKey()))
            .flatMap(entry -> entry.getValue().stream())
            .anyMatch(matcher -> matcher.matches(filePath));
    }

    /**
     * Converts ignore patterns from a file into functional {@link PathMatcher}s.
     */
    private void parseGitIgnoreFile(@Nonnull final Path ignoreFile) {
        final var directory = ignoreFile.getParent();
        final List<PathMatcher> matchers = new ArrayList<>();

        try (final var lines = Files.lines(ignoreFile)) {
            lines.map(String::trim)
                // Skips empty lines and comments.
                .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                .forEach(pattern -> {
                    var glob = pattern;
                    if (pattern.endsWith("/")) {
                        // Converts directory-only match to a recursive glob.
                        glob += "**";
                    }
                    if (!pattern.startsWith("**/") && !pattern.startsWith("/")) {
                        // Ensures relative patterns match at any depth within the subdirectory.
                        glob = "**/" + glob;
                    }
                    // Constructs a full OS-agnostic glob string using forward slashes.
                    // See: https://en.wikipedia.org/wiki/Glob_(programming)
                    final var fullGlob = "glob:" + directory.toString().replace("\\", "/") + "/" + glob;
                    matchers.add(FileSystems.getDefault().getPathMatcher(fullGlob));
                });
            registry.put(directory, matchers);
        } catch (IOException ignored) {
        }
    }
}
