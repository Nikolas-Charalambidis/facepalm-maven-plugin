package dev.nichar.facepalm.module;

import org.apache.maven.plugin.logging.Log;

import com.google.inject.AbstractModule;
import lombok.RequiredArgsConstructor;


/**
 * Guice module for bridging the Maven logging environment.
 * Binds the active {@link Log} instance to the DI container for internal component use.
 *
 * @author Nikolas Charalambidis
 * @since 1.0.0
 */
@RequiredArgsConstructor
public class FacepalmLogModule extends AbstractModule {

    private final Log log;

    @Override
    protected void configure() {
        // Bind the Maven logger to the container for simple interface-to-instance resolution.
        bind(Log.class).toInstance(log);
    }
}
