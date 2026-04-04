package dev.nichar.facepalm.engine.extractor;

import java.util.List;

import javax.annotation.Nonnull;

import dev.nichar.facepalm.engine.FileContext;
import dev.nichar.facepalm.engine.Finding;


/**
 * Contract for components that scan file content for potential secrets.
 * Implementations process a {@link FileContext} and return identified findings.
 */
public interface SecretExtractor {

    /**
     * Scans the file context and extracts sensitive information.
     */
    @Nonnull
    List<Finding> extract(@Nonnull final FileContext context);
}
