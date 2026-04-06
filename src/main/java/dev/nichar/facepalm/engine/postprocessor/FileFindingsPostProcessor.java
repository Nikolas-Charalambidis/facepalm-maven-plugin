package dev.nichar.facepalm.engine.postprocessor;

import java.util.List;

import jakarta.annotation.Nonnull;

import dev.nichar.facepalm.engine.FileContext;
import dev.nichar.facepalm.engine.Finding;


/**
 * Interface for components that perform final adjustments on discovery findings at the file level.
 * Identifies high-volume noise or composite risks by analyzing all findings within a single file.
 */
@FunctionalInterface
public interface FileFindingsPostProcessor {

    /**
     * Executes post-scan logic on the collected findings for a specific file.
     */
    void process(@Nonnull List<Finding> fileFindings, @Nonnull FileContext context);
}
