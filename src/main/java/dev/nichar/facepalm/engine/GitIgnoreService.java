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

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.plugin.logging.Log;


@Named
@Singleton
public class GitIgnoreService {
    private final Log log;

    private final TreeMap<Path, List<PathMatcher>> registry = new TreeMap<>();

    @Inject
    public GitIgnoreService(@Nullable Log log) {
        this.log = log != null ? log : new org.apache.maven.plugin.logging.SystemStreamLog();
    }

    public void loadAllGitIgnores(Path root) {
        try (Stream<Path> paths = Files.walk(root)) {
            paths.filter(p -> p.getFileName() != null && p.getFileName().toString().equals(".gitignore"))
                .forEach(this::parseGitIgnoreFile);
        } catch (IOException e) {
            log.warn("Error walking for .gitignores: " + e.getMessage());
        }
    }

    private void parseGitIgnoreFile(Path ignoreFile) {
        Path dir = ignoreFile.getParent();
        List<PathMatcher> matchers = new ArrayList<>();

        try (Stream<String> lines = Files.lines(ignoreFile)) {
            lines.map(String::trim)
                .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                .forEach(pattern -> {
                    String glob = pattern;
                    if (pattern.endsWith("/")) {
                        glob += "**";
                    }
                    if (!pattern.startsWith("**/") && !pattern.startsWith("/")) {
                        glob = "**/" + glob;
                    }
                    String fullGlob = "glob:" + dir.toString().replace("\\", "/") + "/" + glob;
                    matchers.add(FileSystems.getDefault().getPathMatcher(fullGlob));
                });
            registry.put(dir, matchers);
        } catch (IOException ignored) {
        }
    }

    public boolean isIgnored(Path filePath) {
        return registry.entrySet().stream()
            .filter(entry -> filePath.startsWith(entry.getKey()))
            .flatMap(entry -> entry.getValue().stream())
            .anyMatch(matcher -> matcher.matches(filePath));
    }
}
