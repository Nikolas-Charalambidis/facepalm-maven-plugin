package dev.nichar.facepalm.module;

import javax.inject.Singleton;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import dev.nichar.facepalm.FacepalmConfig;
import lombok.RequiredArgsConstructor;


/**
 * Configures the injection of the plugin's parameters.
 * Uses a provider method to expose the {@link FacepalmConfig} as a {@link Singleton},
 * ensuring all components share the same immutable configuration state.
 *
 * @author Nikolas Charalambidis
 * @since 1.0.0
 */
@RequiredArgsConstructor
public class FacepalmConfigModule extends AbstractModule {

    private final FacepalmConfig config;

    /**
     * Provides the effective configuration instance to the container.
     *
     * @return pre-assembled configuration DTO used for the current execution.
     */
    @Provides
    @Singleton
    FacepalmConfig provideConfig() {
        return config;
    }
}
