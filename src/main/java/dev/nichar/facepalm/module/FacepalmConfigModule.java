package dev.nichar.facepalm.module;

import javax.inject.Singleton;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import dev.nichar.facepalm.FacepalmConfig;
import lombok.RequiredArgsConstructor;


/**
 * Guice module for injecting plugin configuration.
 * Exposes {@link FacepalmConfig} as a singleton to ensure consistent state across all components.
 *
 * @author Nikolas Charalambidis
 * @since 1.0.0
 */
@RequiredArgsConstructor
public class FacepalmConfigModule extends AbstractModule {

    private final FacepalmConfig config;

    /**
     * Provides the pre-assembled configuration instance to the DI container.
     *
     * @return pre-assembled configuration DTO used for the current execution.
     */
    @Provides
    @Singleton
    FacepalmConfig provideConfig() {
        return config;
    }
}
