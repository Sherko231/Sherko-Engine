package com.samo.engine.render.opengl.internal;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class GlslOfflineValidationTest {
    @Test
    void validatesCommittedRuntimeShaders() throws Exception {
        GlslOfflineValidator.validate(
                GlslOfflineValidator.Stage.VERTEX,
                "shaders/p5/basic.vert",
                resource("shaders/p5/basic.vert"));
        GlslOfflineValidator.validate(
                GlslOfflineValidator.Stage.FRAGMENT,
                "shaders/p5/basic.frag",
                resource("shaders/p5/basic.frag"));
        GlslOfflineValidator.validate(
                GlslOfflineValidator.Stage.VERTEX,
                "shaders/p5/debug-lines.vert",
                resource("shaders/p5/debug-lines.vert"));
        GlslOfflineValidator.validate(
                GlslOfflineValidator.Stage.FRAGMENT,
                "shaders/p5/debug-lines.frag",
                resource("shaders/p5/debug-lines.frag"));
    }

    @Test
    void validatesManualSrgbFallbackVariant() throws Exception {
        String source = resource("shaders/p5/basic.frag");
        GlslOfflineValidator.validate(
                GlslOfflineValidator.Stage.FRAGMENT,
                "shaders/p5/basic.frag[manual-srgb]",
                PresentationMode.MANUAL_SRGB.fragmentSource(source));
        String debugSource = resource("shaders/p5/debug-lines.frag");
        GlslOfflineValidator.validate(
                GlslOfflineValidator.Stage.FRAGMENT,
                "shaders/p5/debug-lines.frag[manual-srgb]",
                PresentationMode.MANUAL_SRGB.fragmentSource(debugSource));
    }

    @Test
    void rejectsBrokenFixtureWithDiagnostics() throws Exception {
        IllegalStateException failure = assertThrows(
                IllegalStateException.class,
                () -> GlslOfflineValidator.validate(
                        GlslOfflineValidator.Stage.FRAGMENT,
                        "shaders/p5/broken.frag",
                        resource("shaders/p5/broken.frag")));

        assertTrue(failure.getMessage().contains("shaders/p5/broken.frag"));
        assertTrue(failure.getMessage().contains("FRAGMENT"));
        assertTrue(failure.getMessage().toLowerCase().contains("error"));
    }

    private static String resource(String path) throws IOException {
        try (InputStream stream = GlslOfflineValidationTest.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                throw new IOException("Missing classpath resource: " + path);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
