package com.samo.engine.platform.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.lwjgl.glfw.GLFW.glfwGetCurrentContext;
import static org.lwjgl.glfw.GLFW.glfwGetPrimaryMonitor;
import static org.lwjgl.glfw.GLFW.glfwGetVideoMode;
import static org.lwjgl.glfw.GLFW.glfwGetWindowAttrib;
import static org.lwjgl.glfw.GLFW.glfwGetWindowMonitor;
import static org.lwjgl.glfw.GLFW.glfwGetWindowPos;
import static org.lwjgl.glfw.GLFW.glfwGetWindowSize;
import static org.lwjgl.glfw.GLFW.glfwGetMonitorPos;

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
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryStack;

class GlfwWindowModeNativeTest {
    private static final String ENABLE_ENV = "SHERKO_P3_T03_NATIVE";
    private static final Path REPORT_PATH = Path.of("build", "reports", "p3", "p3-t03-window-modes.txt");
    private static final List<WindowMode> CYCLE = List.of(WindowMode.BORDERLESS_FULLSCREEN, WindowMode.WINDOWED, WindowMode.EXCLUSIVE_FULLSCREEN, WindowMode.WINDOWED);

    @Test
    void productionWindowSurvivesTwentyModeTransitionsWithSameContext() throws Exception {
        assumeTrue(Boolean.parseBoolean(System.getenv(ENABLE_ENV)), () -> "Set " + ENABLE_ENV + "=true to run the P3-T03 native acceptance");
        assertTrue(System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("windows"), "P3-T03 native acceptance targets Windows x64");

        NativeResourceRegistry registry = new NativeResourceRegistry();
        List<EngineLogger.Event> logEvents = new ArrayList<>();
        GlfwWindow window = new GlfwWindow(960, 540, "Sherko Engine P3-T03 Native Acceptance", new EngineLogger(logEvents::add), registry);

        boolean started = false;
        boolean stopAttempted = false;
        boolean closed = false;
        WindowGeometry initialGeometry = null;
        MonitorGeometry monitorGeometry = null;
        String glVersion = null;
        int transitions = 0;
        try {
            window.initialize();
            window.start();
            started = true;
            window.pollEvents();

            long handle = glfwGetCurrentContext();
            assertTrue(handle != 0L, "production window must own a current context while started");
            initialGeometry = windowGeometry(handle);
            assertTrue(initialGeometry.width() > 0 && initialGeometry.height() > 0, "initial windowed geometry must be positive");

            long primaryMonitor = glfwGetPrimaryMonitor();
            assertTrue(primaryMonitor != 0L, "primary monitor must exist for P3-T03 acceptance");
            GLFWVidMode mode = glfwGetVideoMode(primaryMonitor);
            assertNotNull(mode, "primary monitor video mode must exist");
            Position monitorPosition = monitorPosition(primaryMonitor);
            monitorGeometry = new MonitorGeometry(primaryMonitor, monitorPosition.x(), monitorPosition.y(), mode.width(), mode.height(), mode.refreshRate());
            assertTrue(monitorGeometry.width() > 0 && monitorGeometry.height() > 0 && monitorGeometry.refreshRate() > 0, "primary monitor video mode must be valid");

            glVersion = GL11.glGetString(GL11.GL_VERSION);
            assertNotNull(glVersion);
            assertTrue(!glVersion.isBlank(), "GL_VERSION must remain available");

            for (int cycle = 0; cycle < 5; cycle++) {
                for (WindowMode requested : CYCLE) {
                    window.setWindowMode(requested);
                    transitions++;
                    window.pollEvents();
                    assertEquals(handle, glfwGetCurrentContext(), "mode transition must preserve the original current context");
                    String currentVersion = GL11.glGetString(GL11.GL_VERSION);
                    assertNotNull(currentVersion);
                    assertTrue(!currentVersion.isBlank(), "OpenGL must remain usable after transition");
                    assertMode(handle, requested, initialGeometry, monitorGeometry);
                }
            }

            assertEquals(20, transitions);
            assertEquals(initialGeometry, windowGeometry(handle), "final windowed geometry must match the original captured geometry");

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

        writeReport(initialGeometry, monitorGeometry, glVersion, transitions);
    }

    private static void assertMode(long handle, WindowMode requested, WindowGeometry initial, MonitorGeometry monitor) {
        WindowGeometry actual = windowGeometry(handle);
        assertTrue(actual.width() > 0 && actual.height() > 0, "logical window dimensions must stay positive after each transition");
        switch (requested) {
            case WINDOWED -> {
                assertEquals(0L, glfwGetWindowMonitor(handle));
                assertEquals(GLFW.GLFW_TRUE, glfwGetWindowAttrib(handle, GLFW.GLFW_DECORATED));
                assertEquals(initial, actual);
            }
            case BORDERLESS_FULLSCREEN -> {
                assertEquals(0L, glfwGetWindowMonitor(handle));
                assertEquals(GLFW.GLFW_FALSE, glfwGetWindowAttrib(handle, GLFW.GLFW_DECORATED));
                assertEquals(new WindowGeometry(monitor.x(), monitor.y(), monitor.width(), monitor.height()), actual);
            }
            case EXCLUSIVE_FULLSCREEN -> {
                assertEquals(monitor.handle(), glfwGetWindowMonitor(handle));
                assertEquals(monitor.width(), actual.width());
                assertEquals(monitor.height(), actual.height());
            }
        }
    }

    private static WindowGeometry windowGeometry(long handle) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer x = stack.mallocInt(1);
            IntBuffer y = stack.mallocInt(1);
            IntBuffer width = stack.mallocInt(1);
            IntBuffer height = stack.mallocInt(1);
            glfwGetWindowPos(handle, x, y);
            glfwGetWindowSize(handle, width, height);
            return new WindowGeometry(x.get(0), y.get(0), width.get(0), height.get(0));
        }
    }

    private static Position monitorPosition(long monitor) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer x = stack.mallocInt(1);
            IntBuffer y = stack.mallocInt(1);
            glfwGetMonitorPos(monitor, x, y);
            return new Position(x.get(0), y.get(0));
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

    private static void writeReport(WindowGeometry initial, MonitorGeometry monitor, String glVersion, int transitions) throws IOException {
        Files.createDirectories(REPORT_PATH.getParent());
        List<String> lines = List.of("task=P3-T03", "result=PASS", "transition.count=" + transitions,
            "transition.sequence=BORDERLESS_FULLSCREEN,WINDOWED,EXCLUSIVE_FULLSCREEN,WINDOWED x5", "initial.window.x=" + initial.x(), "initial.window.y=" + initial.y(),
            "initial.window.width=" + initial.width(), "initial.window.height=" + initial.height(), "restored.window.geometry.matches.initial=true",
            "primary.monitor.handle.nonzero=" + (monitor.handle() != 0L), "primary.monitor.x=" + monitor.x(), "primary.monitor.y=" + monitor.y(),
            "primary.monitor.width=" + monitor.width(), "primary.monitor.height=" + monitor.height(), "primary.monitor.refresh.hz=" + monitor.refreshRate(),
            "context.preserved.all.transitions=true", "gl.version=" + glVersion, "engine.commit=" + environmentOr("GITHUB_SHA", "unknown"),
            "java.version=" + System.getProperty("java.version"), "os.name=" + System.getProperty("os.name"), "os.arch=" + System.getProperty("os.arch"),
            "native.resource.registry.empty.after.cleanup=true",
            "evidence.scope=one production 20-transition window-mode acceptance run; not P0-T13 soak or P0-T14 repeated lifecycle evidence");
        Files.write(REPORT_PATH, lines, StandardCharsets.UTF_8);
    }

    private static String environmentOr(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value;
    }

    private record Position(int x, int y) {
    }

    private record WindowGeometry(int x, int y, int width, int height) {
    }

    private record MonitorGeometry(long handle, int x, int y, int width, int height, int refreshRate) {
    }
}
