package com.samo.engine.render.opengl.internal;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.samo.engine.core.api.EngineLogger;
import com.samo.engine.core.api.NativeResourceRegistry;
import com.samo.engine.platform.api.GlfwWindow;
import com.samo.engine.platform.api.OpenGlDebugMode;
import com.samo.engine.platform.api.OpenGlThreadGuard;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class GlslRuntimeValidationNativeTest {
    private static final String ENABLE_ENV = "SHERKO_P5_T05_NATIVE";
    private static final Path REPORT_PATH = Path.of("build", "reports", "p5", "p5-t05-glsl-runtime.txt");

    @Test
    void compilesAndLinksCommittedShadersOnRealOpenGl46() throws Exception {

        assumeTrue(Boolean.parseBoolean(System.getenv(ENABLE_ENV)), () -> "Set " + ENABLE_ENV + "=true to run the P5-T05 native acceptance");
        assertTrue(System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("windows"), "P5-T05 native acceptance targets Windows x64");

        String vertexSource = resource("shaders/p5/basic.vert");
        String fragmentSource = resource("shaders/p5/basic.frag");

        NativeResourceRegistry registry = new NativeResourceRegistry();
        GlfwWindow window = new GlfwWindow(640, 360, "Sherko Engine P5-T05 Native Acceptance", new EngineLogger(event -> {
        }), registry, OpenGlDebugMode.FAIL_ON_HIGH_SEVERITY);

        boolean started = false;
        boolean stopped = false;
        boolean closed = false;
        try {
            window.initialize();
            window.start();
            started = true;

            OpenGlThreadGuard guard = window.openGlThreadGuard();
            OpenGlResourceBackend backend = new LwjglOpenGlResourceBackend();

            try (OpenGlShader vertex = OpenGlShader.compile(OpenGlShader.Stage.VERTEX, "shaders/p5/basic.vert", vertexSource, guard, registry, backend);
                OpenGlShader fragment = OpenGlShader.compile(OpenGlShader.Stage.FRAGMENT, "shaders/p5/basic.frag", fragmentSource, guard, registry, backend);
                OpenGlProgram program = OpenGlProgram.link("p5-basic-program", vertex, fragment, guard, registry, backend)) {
                guard.assertOwnerThread();
            }

            window.stop();
            stopped = true;
            window.close();
            closed = true;
            registry.assertNoOpenResources();
        } finally {
            if (!closed) {
                if (started && !stopped) {
                    attemptCleanup(window::stop);
                }
                attemptCleanup(window::close);
            }
        }

        registry.assertNoOpenResources();
        writeReport();

    }

    private static String resource(String path) throws IOException {

        try (InputStream stream = GlslRuntimeValidationNativeTest.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                throw new IOException("Missing classpath resource: " + path);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }

    }

    private static boolean attemptCleanup(Runnable cleanup) {

        try {
            cleanup.run();
            return true;
        } catch (RuntimeException | Error cleanupFailure) {
            return false;
        }

    }

    private static void writeReport() throws IOException {

        Files.createDirectories(REPORT_PATH.getParent());
        Files.write(REPORT_PATH,
            List.of("task=P5-T05", "result=PASS", "vertex.source=shaders/p5/basic.vert", "fragment.source=shaders/p5/basic.frag", "runtime.compile=PASS", "runtime.link=PASS",
                "native.resource.registry.empty.after.cleanup=true", "engine.commit=" + environmentOr("GITHUB_SHA", "unknown"),
                "java.version=" + System.getProperty("java.version"), "os.name=" + System.getProperty("os.name"), "os.arch=" + System.getProperty("os.arch"),
                "evidence.scope=GLSL offline/runtime validation only; no material, reflection, variant, hot-reload, or draw claim"),
            StandardCharsets.UTF_8);

    }

    private static String environmentOr(String key, String fallback) {

        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value;

    }
}
