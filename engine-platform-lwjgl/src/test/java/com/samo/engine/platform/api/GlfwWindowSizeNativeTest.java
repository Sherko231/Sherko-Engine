package com.samo.engine.platform.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.lwjgl.glfw.GLFW.glfwGetCurrentContext;
import static org.lwjgl.glfw.GLFW.glfwGetFramebufferSize;
import static org.lwjgl.glfw.GLFW.glfwGetWindowContentScale;
import static org.lwjgl.glfw.GLFW.glfwGetWindowSize;
import static org.lwjgl.glfw.GLFW.glfwSetWindowSize;

import com.samo.engine.core.api.EngineLogger;
import com.samo.engine.core.api.NativeResourceRegistry;
import java.io.IOException;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.lwjgl.system.MemoryStack;

class GlfwWindowSizeNativeTest {
    private static final String ENABLE_ENV = "SHERKO_P3_T02_NATIVE";
    private static final Path REPORT_PATH =
            Path.of("build", "reports", "p3", "p3-t02-window-size.txt");

    @Test
    void productionWindowDeliversLogicalAndFramebufferSizesFromRealGlfw() throws Exception {
        assumeTrue(Boolean.parseBoolean(System.getenv(ENABLE_ENV)),
                () -> "Set " + ENABLE_ENV + "=true to run the P3-T02 native acceptance");
        assertTrue(System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("windows"),
                "P3-T02 native acceptance targets Windows x64");

        List<EngineLogger.Event> logEvents = new ArrayList<>();
        RecordingSizeListener sizeListener = new RecordingSizeListener();
        NativeResourceRegistry registry = new NativeResourceRegistry();
        GlfwWindow window = new GlfwWindow(
                640,
                360,
                "Sherko Engine P3-T02 Native Acceptance",
                new EngineLogger(logEvents::add),
                registry,
                sizeListener);

        boolean started = false;
        boolean stopAttempted = false;
        boolean closed = false;
        Dimensions logical = null;
        Dimensions framebuffer = null;
        float scaleX = Float.NaN;
        float scaleY = Float.NaN;
        try {
            window.initialize();
            window.start();
            started = true;

            long handle = glfwGetCurrentContext();
            assertTrue(handle != 0L, "production window must own a current context while started");

            window.pollEvents();
            assertMatchesDirectGlfw(handle, sizeListener);

            glfwSetWindowSize(handle, 800, 600);
            boolean observedTarget = false;
            for (int attempt = 0; attempt < 100; attempt++) {
                window.pollEvents();
                logical = logicalSize(handle);
                framebuffer = framebufferSize(handle);
                if (logical.width() == 800 && logical.height() == 600
                        && sizeListener.logicalWidth == logical.width()
                        && sizeListener.logicalHeight == logical.height()
                        && sizeListener.framebufferWidth == framebuffer.width()
                        && sizeListener.framebufferHeight == framebuffer.height()) {
                    observedTarget = true;
                    break;
                }
                Thread.sleep(10L);
            }
            assertTrue(observedTarget, "production size receiver did not converge to direct GLFW observations");

            logical = logicalSize(handle);
            framebuffer = framebufferSize(handle);
            assertEquals(logical.width(), sizeListener.logicalWidth);
            assertEquals(logical.height(), sizeListener.logicalHeight);
            assertEquals(framebuffer.width(), sizeListener.framebufferWidth);
            assertEquals(framebuffer.height(), sizeListener.framebufferHeight);

            float[] scale = contentScale(handle);
            scaleX = scale[0];
            scaleY = scale[1];
            assertTrue(scaleX > 0.0f && scaleY > 0.0f, "GLFW content scale must be positive");

            stopAttempted = true;
            window.stop();
            window.close();
            closed = true;
            assertEquals(0L, glfwGetCurrentContext());
            registry.assertNoOpenResources();
        } finally {
            if (!closed) {
                if (started && !stopAttempted) {
                    attemptCleanup(window::stop);
                }
                attemptCleanup(window::close);
            }
        }

        writeReport(logical, framebuffer, scaleX, scaleY);
    }

    private static void assertMatchesDirectGlfw(long handle, RecordingSizeListener listener) {
        Dimensions logical = logicalSize(handle);
        Dimensions framebuffer = framebufferSize(handle);
        assertEquals(logical.width(), listener.logicalWidth);
        assertEquals(logical.height(), listener.logicalHeight);
        assertEquals(framebuffer.width(), listener.framebufferWidth);
        assertEquals(framebuffer.height(), listener.framebufferHeight);
    }

    private static Dimensions logicalSize(long handle) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer width = stack.mallocInt(1);
            IntBuffer height = stack.mallocInt(1);
            glfwGetWindowSize(handle, width, height);
            return new Dimensions(width.get(0), height.get(0));
        }
    }

    private static Dimensions framebufferSize(long handle) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer width = stack.mallocInt(1);
            IntBuffer height = stack.mallocInt(1);
            glfwGetFramebufferSize(handle, width, height);
            return new Dimensions(width.get(0), height.get(0));
        }
    }

    private static float[] contentScale(long handle) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            var xScale = stack.mallocFloat(1);
            var yScale = stack.mallocFloat(1);
            glfwGetWindowContentScale(handle, xScale, yScale);
            return new float[] {xScale.get(0), yScale.get(0)};
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

    private static void writeReport(
            Dimensions logical,
            Dimensions framebuffer,
            float scaleX,
            float scaleY) throws IOException {
        Files.createDirectories(REPORT_PATH.getParent());
        boolean distinct = logical.width() != framebuffer.width() || logical.height() != framebuffer.height();
        List<String> lines = List.of(
                "task=P3-T02",
                "result=PASS",
                "logical.width=" + logical.width(),
                "logical.height=" + logical.height(),
                "framebuffer.width=" + framebuffer.width(),
                "framebuffer.height=" + framebuffer.height(),
                "logical.framebuffer.distinct=" + distinct,
                "content.scale.x=" + scaleX,
                "content.scale.y=" + scaleY,
                "engine.commit=" + environmentOr("GITHUB_SHA", "unknown"),
                "java.version=" + System.getProperty("java.version"),
                "os.name=" + System.getProperty("os.name"),
                "os.arch=" + System.getProperty("os.arch"),
                "native.resource.registry.empty.after.cleanup=true",
                "evidence.scope=production GLFW logical/framebuffer size separation; not renderer, P0-T13 soak, or P0-T14 repeated lifecycle evidence");
        Files.write(REPORT_PATH, lines, StandardCharsets.UTF_8);
    }

    private static String environmentOr(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value;
    }

    private record Dimensions(int width, int height) {
    }

    private static final class RecordingSizeListener implements WindowSizeListener {
        private int logicalWidth = -1;
        private int logicalHeight = -1;
        private int framebufferWidth = -1;
        private int framebufferHeight = -1;

        @Override
        public void onLogicalWindowSizeChanged(int width, int height) {
            logicalWidth = width;
            logicalHeight = height;
        }

        @Override
        public void onFramebufferSizeChanged(int width, int height) {
            framebufferWidth = width;
            framebufferHeight = height;
        }
    }
}
