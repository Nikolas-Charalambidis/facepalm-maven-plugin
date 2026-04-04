package dev.nichar.facepalm.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * Configuration for post-scan noise reduction and cleanup.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostProcessorConfig {

    /**
     * Maximum occurrences allowed before a secret is suppressed as noise.
     */
    private int highVolumeThreshold = 15;
}
