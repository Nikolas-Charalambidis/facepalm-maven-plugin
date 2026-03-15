package dev.nichar.facepalm.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;


/**
 * Configuration for noise reduction and cleanup after the initial scan.
 *
 * @author Nikolas Charalambidis
 * @since 1.0.0
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PostProcessorConfig {

    /**
     * The limit at which a specific secret value is considered "noise."
     * If the same match is found more than this many times across the project,
     * it is likely a false positive (e.g., a common variable name or public constant) and will be suppressed in the final report.
     */
    private int highVolumeThreshold = 15;
}
