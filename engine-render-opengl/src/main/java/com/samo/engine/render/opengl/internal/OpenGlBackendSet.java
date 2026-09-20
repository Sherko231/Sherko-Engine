package com.samo.engine.render.opengl.internal;

import java.util.Objects;

/**
 * Cohesive renderer composition of the three replaceable OpenGL adapter boundaries.
 *
 * <p>
 * The individual interfaces remain responsibility-specific. This value only keeps their
 * construction/injection synchronized so renderer composition does not grow one positional
 * parameter per backend concern.
 */
record OpenGlBackendSet(OpenGlResourceBackend resourceBackend, OpenGlDrawBackend drawBackend, OpenGlUniformBlockReflectionBackend reflectionBackend) {

    OpenGlBackendSet {

        Objects.requireNonNull(resourceBackend, "resourceBackend");
        Objects.requireNonNull(drawBackend, "drawBackend");
        Objects.requireNonNull(reflectionBackend, "reflectionBackend");

    }

    static OpenGlBackendSet production() {

        return new OpenGlBackendSet(new LwjglOpenGlResourceBackend(), new LwjglOpenGlDrawBackend(), new LwjglOpenGlUniformBlockReflectionBackend());

    }
}
