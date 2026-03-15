package dev.nichar.facepalm.module;

import org.apache.maven.plugin.logging.Log;

import com.google.inject.AbstractModule;
import lombok.RequiredArgsConstructor;


/**
 * Bridges the external logging environment into the DI container.
 * Manually binds the provided Maven {@link Log} instance.
 * Internal components can perform logging without being coupled to the specific execution context.
 *
 * @author Nikolas Charalambidis
 * @since 1.0.0
 */
@RequiredArgsConstructor
public class FacepalmLogModule extends AbstractModule {

    private final Log log;

    @Override
    protected void configure() {
        // Best for simple interface-to-instance bindings.
        bind(Log.class).toInstance(log);
    }
}
