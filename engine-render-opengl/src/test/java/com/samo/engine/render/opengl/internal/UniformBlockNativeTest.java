package com.samo.engine.render.opengl.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.samo.engine.core.api.CameraMatrices;
import com.samo.engine.core.api.EngineLogger;
import com.samo.engine.core.api.NativeResourceRegistry;
import com.samo.engine.platform.api.GlfwWindow;
import com.samo.engine.platform.api.OpenGlDebugMode;
import com.samo.engine.platform.api.OpenGlThreadGuard;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class UniformBlockNativeTest {
    private static final String ENABLE_ENV = "SHERKO_P5_T06_NATIVE";
    private static final Path REPORT_PATH = Path.of("build", "reports", "p5", "p5-t06-uniform-blocks.txt");

    @Test
    void reflectsExpectedBlocksAndPacksCameraDataOnRealOpenGl46() throws Exception {
        assumeTrue(Boolean.parseBoolean(System.getenv(ENABLE_ENV)), () -> "Set " + ENABLE_ENV + "=true to run the P5-T06 native acceptance");
        assertTrue(System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("windows"), "P5-T06 native acceptance targets Windows x64");

        String vertexSource = resource("shaders/p5/basic.vert");
        String fragmentSource = resource("shaders/p5/basic.frag");

        NativeResourceRegistry registry = new NativeResourceRegistry();
        GlfwWindow window = new GlfwWindow(640, 360, "Sherko Engine P5-T06 Native Acceptance", new EngineLogger(event -> {
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
                UniformBlockLayoutVerifier.verify(program.handle(), guard, new LwjglOpenGlUniformBlockReflectionBackend());

                Matrix4f view = CameraMatrices.view(new Vector3f(2.0f, 3.0f, 4.0f), new Vector3f(0.0f, 0.0f, -1.0f), new Vector3f(0.0f, 1.0f, 0.0f), new Matrix4f());
                Matrix4f projection = CameraMatrices.perspective((float) Math.toRadians(70.0), 16.0f / 9.0f, 0.1f, 100.0f, new Matrix4f());

                ByteBuffer cameraBytes = ByteBuffer.allocateDirect(CameraMatricesUniformBlock.SIZE_BYTES).order(ByteOrder.nativeOrder());
                CameraMatricesUniformBlock.write(view, projection, cameraBytes);

                assertEquals(CameraMatricesUniformBlock.SIZE_BYTES, cameraBytes.position());
                assertEquals(view.m00(), cameraBytes.getFloat(CameraMatricesUniformBlock.VIEW_OFFSET_BYTES));
                assertEquals(view.m30(), cameraBytes.getFloat(CameraMatricesUniformBlock.VIEW_OFFSET_BYTES + 48));
                assertEquals(projection.m00(), cameraBytes.getFloat(CameraMatricesUniformBlock.PROJECTION_OFFSET_BYTES));
                assertEquals(projection.m32(), cameraBytes.getFloat(CameraMatricesUniformBlock.PROJECTION_OFFSET_BYTES + 56));

                ByteBuffer frameBytes = ByteBuffer.allocateDirect(FramebufferMetricsUniformBlock.SIZE_BYTES).order(ByteOrder.nativeOrder());
                FramebufferMetricsUniformBlock.write(1920, 1080, frameBytes);

                assertEquals(FramebufferMetricsUniformBlock.SIZE_BYTES, frameBytes.position());
                assertEquals(1920.0f, frameBytes.getFloat(0));
                assertEquals(1080.0f, frameBytes.getFloat(4));
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
        try (InputStream stream = UniformBlockNativeTest.class.getClassLoader().getResourceAsStream(path)) {
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
            List.of("task=P5-T06", "result=PASS", "camera.block.name=CameraBlock", "camera.block.binding=0", "camera.block.size.bytes=128", "camera.view.offset.bytes=0",
                "camera.projection.offset.bytes=64", "perframe.block.name=PerFrameBlock", "perframe.block.binding=1", "perframe.block.size.bytes=16", "cpu.column.major.pack=PASS",
                "runtime.reflection=PASS", "spatial.convention=D-041/D-045 unchanged", "native.resource.registry.empty.after.cleanup=true",
                "engine.commit=" + environmentOr("GITHUB_SHA", "unknown"), "java.version=" + System.getProperty("java.version"), "os.name=" + System.getProperty("os.name"),
                "os.arch=" + System.getProperty("os.arch"), "evidence.scope=uniform block ABI/reflection only; no draw, world camera, material, or UBO allocator claim"),
            StandardCharsets.UTF_8);
    }

    private static String environmentOr(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value;
    }
}
