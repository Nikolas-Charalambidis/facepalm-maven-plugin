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
 * Manages {@code .gitignore} files to provide context-aware path filtering.
 * Parses project-level ignore rules to identify files with reduced public exposure.
 */
@Named
@Singleton
public class GitIgnoreService {

    private final Log log;

    /**
     * Map of directory paths to their compiled glob matchers.
     */
    private final TreeMap<Path, List<PathMatcher>> registry = new TreeMap<>();

    @Inject
    public GitIgnoreService(@Nullable final Log log) {
        this.log = log != null ? log : new SystemStreamLog();
    }

    /**
     * Discovers and parses all {@code .gitignore} files within the target project.
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
     * Returns true if the specified path matches any registered ignore patterns.
     */
    public boolean isIgnored(@Nonnull final Path filePath) {
        return registry.entrySet().stream()
            .filter(entry -> filePath.startsWith(entry.getKey()))
            .flatMap(entry -> entry.getValue().stream())
            .anyMatch(matcher -> matcher.matches(filePath));
    }

    /**
     * Translates raw ignore patterns into functional {@link PathMatcher}s.
     */
    private void parseGitIgnoreFile(@Nonnull final Path ignoreFile) {
        final var directory = ignoreFile.getParent();
        final List<PathMatcher> matchers = new ArrayList<>();

        try (final var lines = Files.lines(ignoreFile)) {
            lines.map(String::trim)
                // Skip empty lines and comments per Git specification.
                .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                .forEach(pattern -> {
                    var glob = pattern;
                    if (pattern.endsWith("/")) {
                        // Convert directory-only matches to recursive globs.
                        glob += "**";
                    }
                    if (!pattern.startsWith("**/") && !pattern.startsWith("/")) {
                        // Ensure relative patterns match at any depth within the directory.
                        glob = "**/" + glob;
                    }
                    // Construct OS-agnostic glob strings.
                    // See: https://en.wikipedia.org/wiki/Glob_(programming)
                    final var fullGlob = "glob:" + directory.toString().replace("\\", "/") + "/" + glob;
                    matchers.add(FileSystems.getDefault().getPathMatcher(fullGlob));
                });
            registry.put(directory, matchers);
        } catch (IOException ignored) {
        }
    }
}
