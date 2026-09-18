package com.samo.engine.platform.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.samo.engine.core.api.EngineLogger;
import com.samo.engine.core.api.NativeResourceRegistry;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;

class GlfwWindowMouseMotionTest {
    @Test
    void fallbackAccumulatesRelativeMotionIndependentOfAbsolutePosition() {
        MotionBackend backend = new MotionBackend();
        NativeResourceRegistry registry = new NativeResourceRegistry();
        GlfwWindow window = window(backend, registry);
        start(window);

        backend.queueCursor(40.0, 50.0);
        window.pollEvents();
        assertMotion(window.drainMouseMotionForTest(), 0.0, 0.0);

        window.setCursorCaptured(true);
        backend.queueCursor(100.0, 200.0);
        backend.queueCursor(106.5, 196.0);
        backend.queueCursor(103.0, 201.0);
        window.pollEvents();
        assertMotion(window.drainMouseMotionForTest(), 3.0, 1.0);
        assertEquals(List.of(), backend.rawTransitions);

        window.setCursorCaptured(false);
        window.setCursorCaptured(true);
        backend.queueCursor(900.0, 700.0);
        backend.queueCursor(903.0, 701.0);
        window.pollEvents();
        assertMotion(window.drainMouseMotionForTest(), 3.0, 1.0);

        cleanup(window, registry);
    }

    @Test
    void firstEligibleSampleAfterCaptureAndRecaptureContributesZero() {
        MotionBackend backend = new MotionBackend();
        NativeResourceRegistry registry = new NativeResourceRegistry();
        GlfwWindow window = window(backend, registry);
        start(window);

        window.setCursorCaptured(true);
        backend.queueCursor(10.0, 20.0);
        window.pollEvents();
        assertMotion(window.drainMouseMotionForTest(), 0.0, 0.0);

        backend.queueCursor(12.0, 25.0);
        window.pollEvents();
        assertMotion(window.drainMouseMotionForTest(), 2.0, 5.0);

        window.setCursorCaptured(false);
        window.setCursorCaptured(true);
        backend.queueCursor(-500.0, 1000.0);
        window.pollEvents();
        assertMotion(window.drainMouseMotionForTest(), 0.0, 0.0);

        cleanup(window, registry);
    }

    @Test
    void rawSupportedCaptureEnablesRawAndReleaseDisablesIt() {
        MotionBackend backend = new MotionBackend();
        backend.rawSupported = true;
        NativeResourceRegistry registry = new NativeResourceRegistry();
        GlfwWindow window = window(backend, registry);
        start(window);

        window.setCursorCaptured(true);
        assertTrue(window.isRawMouseMotionEnabledForTest());
        assertEquals(List.of(true), backend.rawTransitions);
        assertEquals(List.of(GLFW.GLFW_CURSOR_DISABLED), backend.cursorModes);

        window.setCursorCaptured(false);
        assertFalse(window.isRawMouseMotionEnabledForTest());
        assertFalse(window.isCursorEffectivelyCapturedForTest());
        assertEquals(List.of(true, false), backend.rawTransitions);
        assertEquals(
                List.of(GLFW.GLFW_CURSOR_DISABLED, GLFW.GLFW_CURSOR_NORMAL),
                backend.cursorModes);

        cleanup(window, registry);
    }

    @Test
    void rawUnsupportedCaptureUsesFallbackWithoutRawToggle() {
        MotionBackend backend = new MotionBackend();
        backend.rawSupported = false;
        NativeResourceRegistry registry = new NativeResourceRegistry();
        GlfwWindow window = window(backend, registry);
        start(window);

        window.setCursorCaptured(true);
        assertFalse(window.isRawMouseMotionEnabledForTest());
        assertEquals(List.of(), backend.rawTransitions);

        backend.queueCursor(1.0, 1.0);
        backend.queueCursor(4.0, -2.0);
        window.pollEvents();
        assertMotion(window.drainMouseMotionForTest(), 3.0, -3.0);

        cleanup(window, registry);
    }

    @Test
    void focusLossClearsMotionDisablesRawAndRequiresExplicitRecapture() {
        MotionBackend backend = new MotionBackend();
        backend.rawSupported = true;
        NativeResourceRegistry registry = new NativeResourceRegistry();
        GlfwWindow window = window(backend, registry);
        start(window);

        window.setCursorCaptured(true);
        backend.queueCursor(10.0, 10.0);
        backend.queueCursor(15.0, 8.0);
        window.pollEvents();
        assertMotion(window.drainMouseMotionForTest(), 5.0, -2.0);

        backend.queueCursor(20.0, 20.0);
        backend.queueFocus(false);
        window.pollEvents();

        assertFalse(window.isFocusedForTest());
        assertFalse(window.isCursorEffectivelyCapturedForTest());
        assertFalse(window.isRawMouseMotionEnabledForTest());
        assertMotion(window.drainMouseMotionForTest(), 0.0, 0.0);
        assertEquals(List.of(true, false), backend.rawTransitions);

        backend.queueFocus(true);
        window.pollEvents();
        assertFalse(window.isCursorEffectivelyCapturedForTest());
        assertEquals(List.of(true, false), backend.rawTransitions);

        window.setCursorCaptured(true);
        assertTrue(window.isRawMouseMotionEnabledForTest());
        assertEquals(List.of(true, false, true), backend.rawTransitions);
        backend.queueCursor(500.0, 500.0);
        window.pollEvents();
        assertMotion(window.drainMouseMotionForTest(), 0.0, 0.0);

        cleanup(window, registry);
    }

    @Test
    void rawEnableFailureRollsBackCaptureAndPreservesOriginalThrowable() {
        MotionBackend backend = new MotionBackend();
        backend.rawSupported = true;
        RuntimeException failure = new IllegalStateException("raw enable failed");
        backend.rawEnableFailure = failure;
        NativeResourceRegistry registry = new NativeResourceRegistry();
        GlfwWindow window = window(backend, registry);
        start(window);

        RuntimeException actual = assertThrows(RuntimeException.class, () -> window.setCursorCaptured(true));
        assertSame(failure, actual);
        assertFalse(window.isCursorEffectivelyCapturedForTest());
        assertFalse(window.isRawMouseMotionEnabledForTest());
        assertEquals(
                List.of(GLFW.GLFW_CURSOR_DISABLED, GLFW.GLFW_CURSOR_NORMAL),
                backend.cursorModes);
        assertEquals(List.of(true, false), backend.rawTransitions);
        assertMotion(window.drainMouseMotionForTest(), 0.0, 0.0);

        backend.rawEnableFailure = null;
        cleanup(window, registry);
    }

    @Test
    void rawEnableFailureSuppressesRollbackFailures() {
        MotionBackend backend = new MotionBackend();
        backend.rawSupported = true;
        RuntimeException failure = new IllegalStateException("raw enable failed");
        RuntimeException rawRollback = new IllegalStateException("raw rollback failed");
        RuntimeException cursorRollback = new IllegalStateException("cursor rollback failed");
        backend.rawEnableFailure = failure;
        backend.rawDisableFailure = rawRollback;
        backend.cursorNormalFailure = cursorRollback;
        NativeResourceRegistry registry = new NativeResourceRegistry();
        GlfwWindow window = window(backend, registry);
        start(window);

        RuntimeException actual = assertThrows(RuntimeException.class, () -> window.setCursorCaptured(true));
        assertSame(failure, actual);
        assertEquals(2, actual.getSuppressed().length);
        assertSame(rawRollback, actual.getSuppressed()[0]);
        assertSame(cursorRollback, actual.getSuppressed()[1]);
        assertFalse(window.isCursorEffectivelyCapturedForTest());
        assertFalse(window.isRawMouseMotionEnabledForTest());

        backend.rawEnableFailure = null;
        backend.rawDisableFailure = null;
        backend.cursorNormalFailure = null;
        cleanup(window, registry);
    }

    @Test
    void focusLossNativeFailuresAreStagedOnceAfterMotionClears() {
        MotionBackend backend = new MotionBackend();
        backend.rawSupported = true;
        NativeResourceRegistry registry = new NativeResourceRegistry();
        GlfwWindow window = window(backend, registry);
        start(window);
        window.setCursorCaptured(true);

        backend.queueCursor(10.0, 10.0);
        backend.queueCursor(20.0, 30.0);
        window.pollEvents();
        assertMotion(window.drainMouseMotionForTest(), 10.0, 20.0);

        RuntimeException rawFailure = new IllegalStateException("raw disable failed");
        RuntimeException cursorFailure = new IllegalStateException("cursor release failed");
        backend.rawDisableFailure = rawFailure;
        backend.cursorNormalFailure = cursorFailure;
        backend.queueCursor(30.0, 40.0);
        backend.queueFocus(false);

        RuntimeException actual = assertThrows(RuntimeException.class, window::pollEvents);
        assertSame(rawFailure, actual);
        assertEquals(1, actual.getSuppressed().length);
        assertSame(cursorFailure, actual.getSuppressed()[0]);
        assertFalse(window.isCursorEffectivelyCapturedForTest());
        assertTrue(window.isRawMouseMotionEnabledForTest());
        assertMotion(window.drainMouseMotionForTest(), 0.0, 0.0);

        backend.rawDisableFailure = null;
        backend.cursorNormalFailure = null;
        window.pollEvents();

        window.stop();
        window.close();
        registry.assertNoOpenResources();
        assertFalse(window.isRawMouseMotionEnabledForTest());
    }

    @Test
    void motionCallbackInstallFailureCleansPreviouslyInstalledInputState() {
        MotionBackend backend = new MotionBackend();
        RuntimeException failure = new IllegalStateException("motion callback install failed");
        backend.motionInstallFailure = failure;
        NativeResourceRegistry registry = new NativeResourceRegistry();
        GlfwWindow window = window(backend, registry);
        window.initialize();

        RuntimeException actual = assertThrows(RuntimeException.class, window::start);
        assertSame(failure, actual);
        assertEquals(1, backend.inputReleaseCount);
        assertEquals(1, backend.sizeReleaseCount);
        assertEquals(0, backend.motionReleaseCount);

        window.close();
        registry.assertNoOpenResources();
    }

    @Test
    void stopClearsMotionAndReleasesMotionCallbackExactlyOnce() {
        MotionBackend backend = new MotionBackend();
        NativeResourceRegistry registry = new NativeResourceRegistry();
        GlfwWindow window = window(backend, registry);
        start(window);
        window.setCursorCaptured(true);
        backend.queueCursor(1.0, 2.0);
        backend.queueCursor(4.0, 8.0);
        window.pollEvents();

        window.stop();
        assertMotion(window.drainMouseMotionForTest(), 0.0, 0.0);
        window.close();

        assertEquals(1, backend.motionReleaseCount);
        registry.assertNoOpenResources();
    }

    private static GlfwWindow window(MotionBackend backend, NativeResourceRegistry registry) {
        return new GlfwWindow(
                800,
                600,
                "P3-T05 mouse motion test",
                new EngineLogger(event -> { }),
                registry,
                backend);
    }

    private static void start(GlfwWindow window) {
        window.initialize();
        window.start();
    }

    private static void cleanup(GlfwWindow window, NativeResourceRegistry registry) {
        window.stop();
        window.close();
        registry.assertNoOpenResources();
    }

    private static void assertMotion(GlfwWindow.MouseMotion motion, double x, double y) {
        assertEquals(x, motion.x(), 0.0);
        assertEquals(y, motion.y(), 0.0);
    }

    private static final class MotionBackend implements GlfwWindow.Backend {
        private final List<Runnable> queuedEvents = new ArrayList<>();
        private final List<Integer> cursorModes = new ArrayList<>();
        private final List<Boolean> rawTransitions = new ArrayList<>();
        private GlfwWindow.InputEventSink inputSink;
        private GlfwWindow.MotionEventSink motionSink;
        private boolean focused = true;
        private boolean rawSupported;
        private RuntimeException rawEnableFailure;
        private RuntimeException rawDisableFailure;
        private RuntimeException cursorNormalFailure;
        private RuntimeException motionInstallFailure;
        private int inputReleaseCount;
        private int motionReleaseCount;
        private int sizeReleaseCount;

        void queueFocus(boolean value) {
            queuedEvents.add(() -> {
                focused = value;
                inputSink.onFocus(value);
            });
        }

        void queueCursor(double x, double y) {
            queuedEvents.add(() -> motionSink.onCursorPosition(x, y));
        }

        @Override
        public GlfwWindow.CallbackState installErrorCallback() {
            return new GlfwWindow.CallbackState(new Object(), new Object());
        }

        @Override
        public void restoreErrorCallback(GlfwWindow.CallbackState state) {
        }

        @Override
        public void freeOwnedErrorCallback(GlfwWindow.CallbackState state) {
        }

        @Override
        public boolean initGlfw() {
            return true;
        }

        @Override
        public void terminateGlfw() {
        }

        @Override
        public void defaultWindowHints() {
        }

        @Override
        public void windowHint(int hint, int value) {
        }

        @Override
        public long createWindow(int width, int height, String title) {
            return 101L;
        }

        @Override
        public void destroyWindow(long handle) {
        }

        @Override
        public void makeContextCurrent(long handle) {
        }

        @Override
        public void createCapabilities() {
        }

        @Override
        public void clearCapabilities() {
        }

        @Override
        public boolean openGl46Supported() {
            return true;
        }

        @Override
        public String glVersion() {
            return "4.6 fixture";
        }

        @Override
        public String glRenderer() {
            return "fixture renderer";
        }

        @Override
        public GlfwWindow.SizeCallbackState installSizeCallbacks(long handle, GlfwWindow.SizeEventSink sink) {
            return new GlfwWindow.SizeCallbackState(new Object(), new Object());
        }

        @Override
        public void releaseSizeCallbacks(long handle, GlfwWindow.SizeCallbackState state) {
            sizeReleaseCount++;
        }

        @Override
        public GlfwWindow.InputCallbackState installInputCallbacks(long handle, GlfwWindow.InputEventSink sink) {
            inputSink = sink;
            return new GlfwWindow.InputCallbackState(new Object(), new Object(), new Object());
        }

        @Override
        public void releaseInputCallbacks(long handle, GlfwWindow.InputCallbackState state) {
            inputReleaseCount++;
            inputSink = null;
        }

        @Override
        public GlfwWindow.MotionCallbackState installCursorPositionCallback(
                long handle,
                GlfwWindow.MotionEventSink sink) {
            if (motionInstallFailure != null) {
                throw motionInstallFailure;
            }
            motionSink = sink;
            return new GlfwWindow.MotionCallbackState(new Object());
        }

        @Override
        public void releaseCursorPositionCallback(long handle, GlfwWindow.MotionCallbackState state) {
            motionReleaseCount++;
            motionSink = null;
        }

        @Override
        public boolean queryWindowFocused(long handle) {
            return focused;
        }

        @Override
        public void setCursorMode(long handle, int mode) {
            cursorModes.add(mode);
            if (mode == GLFW.GLFW_CURSOR_NORMAL && cursorNormalFailure != null) {
                throw cursorNormalFailure;
            }
        }

        @Override
        public boolean rawMouseMotionSupported() {
            return rawSupported;
        }

        @Override
        public void setRawMouseMotion(long handle, boolean enabled) {
            rawTransitions.add(enabled);
            if (enabled && rawEnableFailure != null) {
                throw rawEnableFailure;
            }
            if (!enabled && rawDisableFailure != null) {
                throw rawDisableFailure;
            }
        }

        @Override
        public GlfwWindow.Dimensions queryLogicalSize(long handle) {
            return new GlfwWindow.Dimensions(800, 600);
        }

        @Override
        public GlfwWindow.Dimensions queryFramebufferSize(long handle) {
            return new GlfwWindow.Dimensions(800, 600);
        }

        @Override
        public GlfwWindow.Position queryWindowPosition(long handle) {
            return new GlfwWindow.Position(100, 100);
        }

        @Override
        public long primaryMonitor() {
            return 202L;
        }

        @Override
        public GlfwWindow.VideoMode queryVideoMode(long monitor) {
            return new GlfwWindow.VideoMode(1920, 1080, 120);
        }

        @Override
        public GlfwWindow.Position queryMonitorPosition(long monitor) {
            return new GlfwWindow.Position(0, 0);
        }

        @Override
        public void setDecorated(long handle, boolean decorated) {
        }

        @Override
        public void setWindowMonitor(
                long handle,
                long monitor,
                int x,
                int y,
                int width,
                int height,
                int refreshRate) {
        }

        @Override
        public void swapBuffers(long handle) {
        }

        @Override
        public void pollEvents() {
            List<Runnable> events = List.copyOf(queuedEvents);
            queuedEvents.clear();
            events.forEach(Runnable::run);
        }

        @Override
        public void showWindow(long handle) {
        }

        @Override
        public void hideWindow(long handle) {
        }
    }
}
