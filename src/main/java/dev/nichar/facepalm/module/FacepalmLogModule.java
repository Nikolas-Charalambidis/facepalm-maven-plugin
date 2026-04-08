/*
 * Licensed under Apache-2.0.
 * Copyright (c) 2026 Nikolas Charalambidis.
 * All rights reserved.
 */

package dev.nichar.facepalm.module;

import com.google.inject.AbstractModule;
import lombok.RequiredArgsConstructor;
import org.apache.maven.plugin.logging.Log;

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
