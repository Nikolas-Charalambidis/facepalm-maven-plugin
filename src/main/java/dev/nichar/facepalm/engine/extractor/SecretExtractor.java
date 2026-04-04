package dev.nichar.facepalm.engine.extractor;

import java.util.List;

import javax.annotation.Nonnull;

import dev.nichar.facepalm.engine.FileContext;
import dev.nichar.facepalm.engine.Finding;


/**
 * Defines the core contract for components that scan file content to identify potential secrets.
 * Implementations (such as regex or entropy-based scanners) process a {@link FileContext}
 * and return a list of findings representing suspected credentials or sensitive data.
 */
public interface SecretExtractor {

    /**
     * Analyzes the provided file context to locate and extract sensitive information.
     *
     * @param context The object containing the file's metadata, path, and raw string content.
     * @return A {@link List} of {@link Finding} objects discovered within the file; returns an empty list if no secrets are detected.
     */
    @Nonnull
    List<Finding> extract(@Nonnull final FileContext context);
}
