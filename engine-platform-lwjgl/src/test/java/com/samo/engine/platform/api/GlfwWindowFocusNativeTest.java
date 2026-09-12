package com.samo.engine.platform.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.lwjgl.glfw.GLFW.glfwCreateWindow;
import static org.lwjgl.glfw.GLFW.glfwDestroyWindow;
import static org.lwjgl.glfw.GLFW.glfwFocusWindow;
import static org.lwjgl.glfw.GLFW.glfwGetCurrentContext;
import static org.lwjgl.glfw.GLFW.glfwGetInputMode;
import static org.lwjgl.glfw.GLFW.glfwGetWindowAttrib;
import static org.lwjgl.glfw.GLFW.glfwShowWindow;

import com.samo.engine.core.api.EngineLogger;
import com.samo.engine.core.api.NativeResourceRegistry;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryUtil;

class GlfwWindowFocusNativeTest {
    private static final String ENABLE_ENV = "SHERKO_P3_T04_NATIVE";
    private static final Path REPORT_PATH =
            Path.of("build", "reports", "p3", "p3-t04-focus-loss.txt");

    @Test
    void productionWindowReleasesCaptureOnRealFocusTransferAndRequiresExplicitRecapture()
            throws Exception {
        assumeTrue(Boolean.parseBoolean(System.getenv(ENABLE_ENV)),
                () -> "Set " + ENABLE_ENV + "=true to run the P3-T04 native acceptance");
        assertTrue(System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("windows"),
                "P3-T04 native acceptance targets Windows x64");

        NativeResourceRegistry registry = new NativeResourceRegistry();
        GlfwWindow window = new GlfwWindow(
                960,
                540,
                "Sherko Engine P3-T04 Native Acceptance",
                new EngineLogger(event -> { }),
                registry);

        boolean started = false;
        boolean stopAttempted = false;
        boolean closed = false;
        long helper = MemoryUtil.NULL;
        int capturedMode = -1;
        int lostMode = -1;
        int regainedMode = -1;
        int recapturedMode = -1;
        try {
            window.initialize();
            window.start();
            started = true;
            window.pollEvents();

            long production = glfwGetCurrentContext();
            assertTrue(production != MemoryUtil.NULL, "production context must be current");

            glfwFocusWindow(production);
            pumpUntil(window, () -> glfwGetWindowAttrib(production, GLFW.GLFW_FOCUSED) == GLFW.GLFW_TRUE);

            window.setCursorCaptured(true);
            capturedMode = glfwGetInputMode(production, GLFW.GLFW_CURSOR);
            assertEquals(GLFW.GLFW_CURSOR_DISABLED, capturedMode);

            GLFW.glfwDefaultWindowHints();
            GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_TRUE);
            helper = glfwCreateWindow(320, 200, "P3-T04 focus helper", MemoryUtil.NULL, MemoryUtil.NULL);
            assertTrue(helper != MemoryUtil.NULL, "focus helper window must be created");
            glfwShowWindow(helper);
            glfwFocusWindow(helper);

            pumpUntil(window, () -> glfwGetWindowAttrib(production, GLFW.GLFW_FOCUSED) == GLFW.GLFW_FALSE);
            lostMode = glfwGetInputMode(production, GLFW.GLFW_CURSOR);
            assertEquals(GLFW.GLFW_CURSOR_NORMAL, lostMode);

            glfwFocusWindow(production);
            pumpUntil(window, () -> glfwGetWindowAttrib(production, GLFW.GLFW_FOCUSED) == GLFW.GLFW_TRUE);
            regainedMode = glfwGetInputMode(production, GLFW.GLFW_CURSOR);
            assertEquals(GLFW.GLFW_CURSOR_NORMAL, regainedMode, "focus regain must not auto-recapture");

            window.setCursorCaptured(true);
            recapturedMode = glfwGetInputMode(production, GLFW.GLFW_CURSOR);
            assertEquals(GLFW.GLFW_CURSOR_DISABLED, recapturedMode);

            glfwDestroyWindow(helper);
            helper = MemoryUtil.NULL;

            stopAttempted = true;
            window.stop();
            window.close();
            closed = true;
            registry.assertNoOpenResources();
        } finally {
            if (helper != MemoryUtil.NULL) {
                glfwDestroyWindow(helper);
            }
            if (!closed) {
                if (started && !stopAttempted) {
                    attemptCleanup(window::stop);
                }
                attemptCleanup(window::close);
            }
        }

        writeReport(capturedMode, lostMode, regainedMode, recapturedMode);
    }

    private static void pumpUntil(GlfwWindow window, Condition condition) throws InterruptedException {
        for (int attempt = 0; attempt < 100; attempt++) {
            window.pollEvents();
            if (condition.test()) {
                return;
            }
            Thread.sleep(10L);
        }
        assertTrue(condition.test(), "focus transition did not complete within bounded polling");
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
            int capturedMode,
            int lostMode,
            int regainedMode,
            int recapturedMode) throws IOException {
        Files.createDirectories(REPORT_PATH.getParent());
        List<String> lines = List.of(
                "task=P3-T04",
                "result=PASS",
                "focus.transfer=test-only second GLFW helper window via glfwFocusWindow",
                "cursor.mode.initial.capture=" + capturedMode,
                "cursor.mode.after.focus.loss=" + lostMode,
                "cursor.mode.after.focus.regain=" + regainedMode,
                "cursor.mode.after.explicit.recapture=" + recapturedMode,
                "no.auto.recapture=true",
                "engine.commit=" + environmentOr("GITHUB_SHA", "unknown"),
                "java.version=" + System.getProperty("java.version"),
                "os.name=" + System.getProperty("os.name"),
                "os.arch=" + System.getProperty("os.arch"),
                "native.resource.registry.empty.after.cleanup=true",
                "manual.alt.tab.scenario=Capture cursor, hold movement key, Alt+Tab away, release key while unfocused, Alt+Tab back; P3-T06 must later observe released state rather than a stuck key.",
                "evidence.limit=focus/cursor production path proven; public InputSnapshot is not implemented by P3-T04");
        Files.write(REPORT_PATH, lines, StandardCharsets.UTF_8);
    }

    private static String environmentOr(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value;
    }

    @FunctionalInterface
    private interface Condition {
        boolean test();
    }
}
