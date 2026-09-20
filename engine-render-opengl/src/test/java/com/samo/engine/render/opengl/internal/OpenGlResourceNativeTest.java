package com.samo.engine.render.opengl.internal;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.samo.engine.core.api.EngineLogger;
import com.samo.engine.core.api.NativeResourceRegistry;
import com.samo.engine.platform.api.GlfwWindow;
import com.samo.engine.platform.api.OpenGlDebugMode;
import com.samo.engine.platform.api.OpenGlThreadGuard;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class OpenGlResourceNativeTest {
    private static final String ENABLE_ENV = "SHERKO_P5_T03_NATIVE";
    private static final Path REPORT_PATH = Path.of("build", "reports", "p5", "p5-t03-opengl-resources.txt");

    @Test
    void createsAndDestroysAllOwnedResourceTypesOnRealOpenGl46() throws Exception {

        assumeTrue(Boolean.parseBoolean(System.getenv(ENABLE_ENV)), () -> "Set " + ENABLE_ENV + "=true to run the P5-T03 native acceptance");
        assertTrue(System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("windows"), "P5-T03 native acceptance targets Windows x64");

        NativeResourceRegistry registry = new NativeResourceRegistry();
        GlfwWindow window = new GlfwWindow(640, 360, "Sherko Engine P5-T03 Native Acceptance", new EngineLogger(event -> {
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

            try (OpenGlBuffer buffer = OpenGlBuffer.create(guard, registry, backend);
                OpenGlVertexArray vertexArray = OpenGlVertexArray.create(guard, registry, backend);
                OpenGlTexture texture = OpenGlTexture.create(guard, registry, backend);
                OpenGlSampler sampler = OpenGlSampler.create(guard, registry, backend);
                OpenGlFramebuffer framebuffer = OpenGlFramebuffer.create(guard, registry, backend);
                OpenGlShader vertex = OpenGlShader.compile(OpenGlShader.Stage.VERTEX,
                    "#version 460 core\n" + "void main() {\n" + "    gl_Position = vec4(0.0, 0.0, 0.0, 1.0);\n" + "}\n", guard, registry, backend);
                OpenGlShader fragment = OpenGlShader.compile(OpenGlShader.Stage.FRAGMENT,
                    "#version 460 core\n" + "layout(location = 0) out vec4 color;\n" + "void main() {\n" + "    color = vec4(1.0);\n" + "}\n", guard, registry, backend);
                OpenGlProgram program = OpenGlProgram.link(vertex, fragment, guard, registry, backend)) {
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
            java.util.List.of("task=P5-T03", "result=PASS", "resources=buffer,vertex-array,texture,sampler,framebuffer,vertex-shader,fragment-shader,program",
                "shader.compile=PASS", "program.link=PASS", "thread.affinity=PASS", "native.resource.registry.empty.after.cleanup=true",
                "engine.commit=" + environmentOr("GITHUB_SHA", "unknown"), "java.version=" + System.getProperty("java.version"), "os.name=" + System.getProperty("os.name"),
                "os.arch=" + System.getProperty("os.arch"), "evidence.scope=resource ownership lifecycle only; no draw, upload, material, asset, or renderer-loop claim"),
            StandardCharsets.UTF_8);

    }

    private static String environmentOr(String key, String fallback) {

        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value;

    }
}
