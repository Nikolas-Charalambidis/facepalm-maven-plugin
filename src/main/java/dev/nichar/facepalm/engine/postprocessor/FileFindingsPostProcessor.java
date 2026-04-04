package dev.nichar.facepalm.engine.postprocessor;

import java.util.List;

import jakarta.annotation.Nonnull;

import dev.nichar.facepalm.engine.FileContext;
import dev.nichar.facepalm.engine.Finding;


/**
 * Contract for components that perform final adjustments on a file's findings.
 * Analyzes the entire set of findings for a file to identify high-volume noise or composite risks.
 */
public interface FileFindingsPostProcessor {

    /**
     * Executes post-scan logic on all findings discovered within a single file.
     */
    void process(@Nonnull List<Finding> fileFindings, @Nonnull FileContext context);
}
