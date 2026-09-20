package com.samo.engine.platform.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

class GlfwWindowMouseMotionNativeTest {
    private static final String ENABLE_ENV = "SHERKO_P3_T05_NATIVE";
    private static final Path REPORT_PATH = Path.of("build", "reports", "p3", "p3-t05-mouse-motion.txt");

    @Test
    void productionWindowTogglesRawMotionAcrossCaptureAndRealFocusTransitions() throws Exception {

        assumeTrue(Boolean.parseBoolean(System.getenv(ENABLE_ENV)), () -> "Set " + ENABLE_ENV + "=true to run the P3-T05 native acceptance");
        assertTrue(System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("windows"), "P3-T05 native acceptance targets Windows x64");

        NativeResourceRegistry registry = new NativeResourceRegistry();
        GlfwWindow window = new GlfwWindow(960, 540, "Sherko Engine P3-T05 Native Acceptance", new EngineLogger(event -> {
        }), registry);

        boolean started = false;
        boolean stopAttempted = false;
        boolean closed = false;
        long helper = MemoryUtil.NULL;
        boolean rawSupported = false;
        int rawBeforeCapture = -1;
        int rawDuringCapture = -1;
        int rawAfterRelease = -1;
        int rawAfterFocusLoss = -1;
        int rawAfterFocusRegain = -1;
        int rawAfterExplicitRecapture = -1;
        try {
            window.initialize();
            window.start();
            started = true;
            window.pollEvents();

            long production = glfwGetCurrentContext();
            assertTrue(production != MemoryUtil.NULL, "production context must be current");
            glfwFocusWindow(production);
            pumpUntil(window, () -> glfwGetWindowAttrib(production, GLFW.GLFW_FOCUSED) == GLFW.GLFW_TRUE);

            rawSupported = window.isRawMouseMotionSupportedForTest();
            rawBeforeCapture = glfwGetInputMode(production, GLFW.GLFW_RAW_MOUSE_MOTION);
            assertEquals(GLFW.GLFW_FALSE, rawBeforeCapture);

            window.setCursorCaptured(true);
            rawDuringCapture = glfwGetInputMode(production, GLFW.GLFW_RAW_MOUSE_MOTION);
            assertEquals(rawSupported ? GLFW.GLFW_TRUE : GLFW.GLFW_FALSE, rawDuringCapture);
            assertEquals(rawSupported, window.isRawMouseMotionEnabledForTest());
            assertMotionCleared(window);

            window.setCursorCaptured(false);
            rawAfterRelease = glfwGetInputMode(production, GLFW.GLFW_RAW_MOUSE_MOTION);
            assertEquals(GLFW.GLFW_FALSE, rawAfterRelease);
            assertFalse(window.isRawMouseMotionEnabledForTest());
            assertMotionCleared(window);

            window.setCursorCaptured(true);
            assertEquals(rawSupported, window.isRawMouseMotionEnabledForTest());
            assertMotionCleared(window);

            GLFW.glfwDefaultWindowHints();
            GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_TRUE);
            helper = glfwCreateWindow(320, 200, "P3-T05 focus helper", MemoryUtil.NULL, MemoryUtil.NULL);
            assertTrue(helper != MemoryUtil.NULL, "focus helper window must be created");
            glfwShowWindow(helper);
            glfwFocusWindow(helper);

            pumpUntil(window, () -> glfwGetWindowAttrib(production, GLFW.GLFW_FOCUSED) == GLFW.GLFW_FALSE);
            rawAfterFocusLoss = glfwGetInputMode(production, GLFW.GLFW_RAW_MOUSE_MOTION);
            assertEquals(GLFW.GLFW_FALSE, rawAfterFocusLoss);
            assertFalse(window.isCursorEffectivelyCapturedForTest());
            assertFalse(window.isRawMouseMotionEnabledForTest());
            assertMotionCleared(window);

            glfwFocusWindow(production);
            pumpUntil(window, () -> glfwGetWindowAttrib(production, GLFW.GLFW_FOCUSED) == GLFW.GLFW_TRUE);
            rawAfterFocusRegain = glfwGetInputMode(production, GLFW.GLFW_RAW_MOUSE_MOTION);
            assertEquals(GLFW.GLFW_FALSE, rawAfterFocusRegain, "focus regain must not auto-enable raw motion");
            assertFalse(window.isCursorEffectivelyCapturedForTest());
            assertMotionCleared(window);

            window.setCursorCaptured(true);
            rawAfterExplicitRecapture = glfwGetInputMode(production, GLFW.GLFW_RAW_MOUSE_MOTION);
            assertEquals(rawSupported ? GLFW.GLFW_TRUE : GLFW.GLFW_FALSE, rawAfterExplicitRecapture);
            assertMotionCleared(window);

            window.setCursorCaptured(false);
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

        writeReport(rawSupported, rawBeforeCapture, rawDuringCapture, rawAfterRelease, rawAfterFocusLoss, rawAfterFocusRegain, rawAfterExplicitRecapture);

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

    private static void assertMotionCleared(GlfwWindow window) {

        GlfwMouseMotionTracker.MouseDelta motion = window.drainMouseMotionForTest();
        assertEquals(0.0, motion.x(), 0.0);
        assertEquals(0.0, motion.y(), 0.0);

    }

    private static boolean attemptCleanup(Runnable cleanup) {

        try {
            cleanup.run();
            return true;
        } catch (RuntimeException | Error cleanupFailure) {
            return false;
        }

    }

    private static void writeReport(boolean rawSupported, int rawBeforeCapture, int rawDuringCapture, int rawAfterRelease, int rawAfterFocusLoss, int rawAfterFocusRegain,
        int rawAfterExplicitRecapture) throws IOException {

        Files.createDirectories(REPORT_PATH.getParent());
        List<String> lines = List.of("task=P3-T05", "result=PASS", "raw.mouse.supported=" + rawSupported, "raw.mode.before.capture=" + rawBeforeCapture,
            "raw.mode.during.capture=" + rawDuringCapture, "raw.mode.after.release=" + rawAfterRelease, "raw.mode.after.focus.loss=" + rawAfterFocusLoss,
            "raw.mode.after.focus.regain=" + rawAfterFocusRegain, "raw.mode.after.explicit.recapture=" + rawAfterExplicitRecapture,
            "baseline.cleared.across.capture.focus.transitions=true", "focus.regain.auto.raw.enable=false", "relative.delta.deterministic.coverage=GlfwWindowMouseMotionTest",
            "fallback.forced.unsupported.coverage=GlfwWindowMouseMotionTest", "raw.hardware.motion.synthetic.oracle=not-used",
            "raw.hardware.motion.note=glfwSetCursorPos is not treated as physical raw mouse input", "engine.commit=" + environmentOr("GITHUB_SHA", "unknown"),
            "java.version=" + System.getProperty("java.version"), "os.name=" + System.getProperty("os.name"), "os.arch=" + System.getProperty("os.arch"),
            "native.resource.registry.empty.after.cleanup=true",
            "fallback.limit=disabled-cursor position deltas are screen-bound independent but do not claim OS pointer acceleration bypass",
            "evidence.limit=native raw-mode selection/focus lifecycle proven; delta arithmetic/fallback are deterministic-test evidence; public InputSnapshot remains P3-T06");
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
