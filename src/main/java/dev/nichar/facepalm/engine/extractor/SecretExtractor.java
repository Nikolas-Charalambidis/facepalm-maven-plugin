package dev.nichar.facepalm.engine.extractor;

import java.util.List;

import jakarta.annotation.Nonnull;

import dev.nichar.facepalm.engine.FileContext;
import dev.nichar.facepalm.engine.Finding;


/**
 * Interface for components that identify potential secrets within file content.
 */
@FunctionalInterface
public interface SecretExtractor {

    /**
     * Analyzes the file context and returns a list of discovered sensitive patterns.
     */
    @Nonnull
    List<Finding> extract(@Nonnull final FileContext context);
}
