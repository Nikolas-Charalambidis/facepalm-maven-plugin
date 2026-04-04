package dev.nichar.facepalm.engine.postprocessor;

import java.util.List;

import javax.annotation.Nonnull;

import dev.nichar.facepalm.engine.FileContext;
import dev.nichar.facepalm.engine.Finding;


/**
 * Defines a contract for components that perform final adjustments on a collection of findings.
 * Unlike evaluators that look at single findings, post-processors analyze the entire set
 * for a specific file to identify patterns like high-volume noise or composite risks.
 */
public interface FileFindingsPostProcessor {

    /**
     * Executes post-scan logic on all findings discovered within a single file.
     *
     * @param fileFindings The complete list of findings identified in the current file.
     * @param context The metadata and content of the file being processed.
     */
    void process(@Nonnull List<Finding> fileFindings, @Nonnull FileContext context);
}
