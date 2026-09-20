package com.samo.engine.render.opengl.internal;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class OpenGlBackendSetTest {
    @Test
    void preservesInjectedAdapterIdentities() {

        OpenGlResourceBackend resources = new LwjglOpenGlResourceBackend();
        OpenGlDrawBackend draw = new LwjglOpenGlDrawBackend();
        OpenGlUniformBlockReflectionBackend reflection = new LwjglOpenGlUniformBlockReflectionBackend();

        OpenGlBackendSet backends = new OpenGlBackendSet(resources, draw, reflection);

        assertSame(resources, backends.resourceBackend());
        assertSame(draw, backends.drawBackend());
        assertSame(reflection, backends.reflectionBackend());

    }

    @Test
    void rejectsNullAdapters() {

        OpenGlResourceBackend resources = new LwjglOpenGlResourceBackend();
        OpenGlDrawBackend draw = new LwjglOpenGlDrawBackend();
        OpenGlUniformBlockReflectionBackend reflection = new LwjglOpenGlUniformBlockReflectionBackend();

        assertThrows(NullPointerException.class, () -> new OpenGlBackendSet(null, draw, reflection));
        assertThrows(NullPointerException.class, () -> new OpenGlBackendSet(resources, null, reflection));
        assertThrows(NullPointerException.class, () -> new OpenGlBackendSet(resources, draw, null));

    }

    @Test
    void productionUsesTheCurrentLwjglAdapters() {

        OpenGlBackendSet backends = OpenGlBackendSet.production();

        assertInstanceOf(LwjglOpenGlResourceBackend.class, backends.resourceBackend());
        assertInstanceOf(LwjglOpenGlDrawBackend.class, backends.drawBackend());
        assertInstanceOf(LwjglOpenGlUniformBlockReflectionBackend.class, backends.reflectionBackend());

    }
}
