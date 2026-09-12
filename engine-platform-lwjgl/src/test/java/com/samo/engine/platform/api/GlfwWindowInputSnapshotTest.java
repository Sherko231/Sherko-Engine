package com.samo.engine.platform.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.samo.engine.core.api.EngineLogger;
import com.samo.engine.core.api.NativeResourceRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;

class GlfwWindowInputSnapshotTest {
    @Test
    void pressAndReleaseBetweenSnapshotsRetainsBothEdges() {
        SnapshotBackend backend = new SnapshotBackend();
        NativeResourceRegistry registry = new NativeResourceRegistry();
        GlfwWindow window = window(backend, registry);
        start(window);

        backend.queueKey(GLFW.GLFW_KEY_W, GLFW.GLFW_PRESS);
        backend.queueKey(GLFW.GLFW_KEY_W, GLFW.GLFW_RELEASE);
        backend.queueMouseButton(GLFW.GLFW_MOUSE_BUTTON_LEFT, GLFW.GLFW_PRESS);
        backend.queueMouseButton(GLFW.GLFW_MOUSE_BUTTON_LEFT, GLFW.GLFW_RELEASE);
        window.pollEvents();

        InputSnapshot first = window.captureInputSnapshot(7L);
        assertEquals(7L, first.frameId());
        assertTrue(first.focused());
        assertFalse(first.keyHeld(InputKey.W));
        assertTrue(first.keyPressed(InputKey.W));
        assertTrue(first.keyReleased(InputKey.W));
        assertFalse(first.mouseButtonHeld(InputMouseButton.LEFT));
        assertTrue(first.mouseButtonPressed(InputMouseButton.LEFT));
        assertTrue(first.mouseButtonReleased(InputMouseButton.LEFT));

        InputSnapshot second = window.captureInputSnapshot(8L);
        assertFalse(second.keyPressed(InputKey.W));
        assertFalse(second.keyReleased(InputKey.W));
        assertFalse(second.mouseButtonPressed(InputMouseButton.LEFT));
        assertFalse(second.mouseButtonReleased(InputMouseButton.LEFT));

        cleanup(window, registry);
    }

    @Test
    void repeatPreservesHeldLevelWithoutCreatingAnotherPressEdge() {
        SnapshotBackend backend = new SnapshotBackend();
        NativeResourceRegistry registry = new NativeResourceRegistry();
        GlfwWindow window = window(backend, registry);
        start(window);

        backend.queueKey(GLFW.GLFW_KEY_W, GLFW.GLFW_PRESS);
        window.pollEvents();
        InputSnapshot pressed = window.captureInputSnapshot(1L);
        assertTrue(pressed.keyHeld(InputKey.W));
        assertTrue(pressed.keyPressed(InputKey.W));

        backend.queueKey(GLFW.GLFW_KEY_W, GLFW.GLFW_REPEAT);
        window.pollEvents();
        InputSnapshot repeated = window.captureInputSnapshot(2L);
        assertTrue(repeated.keyHeld(InputKey.W));
        assertFalse(repeated.keyPressed(InputKey.W));
        assertFalse(repeated.keyReleased(InputKey.W));

        cleanup(window, registry);
    }

    @Test
    void mouseDeltaIsConsumedOnceWithoutResettingBaseline() {
        SnapshotBackend backend = new SnapshotBackend();
        NativeResourceRegistry registry = new NativeResourceRegistry();
        GlfwWindow window = window(backend, registry);
        start(window);
        window.setCursorCaptured(true);

        backend.queueCursor(100.0, 100.0);
        backend.queueCursor(106.0, 96.0);
        window.pollEvents();
        InputSnapshot first = window.captureInputSnapshot(10L);
        assertEquals(6.0, first.mouseDeltaX(), 0.0);
        assertEquals(-4.0, first.mouseDeltaY(), 0.0);
        assertTrue(first.cursorCaptured());

        InputSnapshot empty = window.captureInputSnapshot(11L);
        assertEquals(0.0, empty.mouseDeltaX(), 0.0);
        assertEquals(0.0, empty.mouseDeltaY(), 0.0);

        backend.queueCursor(109.0, 101.0);
        window.pollEvents();
        InputSnapshot continued = window.captureInputSnapshot(12L);
        assertEquals(3.0, continued.mouseDeltaX(), 0.0);
        assertEquals(5.0, continued.mouseDeltaY(), 0.0);

        cleanup(window, registry);
    }

    @Test
    void focusLossClearsHeldStateDiscardsPressesAndRetainsSyntheticReleases() {
        SnapshotBackend backend = new SnapshotBackend();
        NativeResourceRegistry registry = new NativeResourceRegistry();
        GlfwWindow window = window(backend, registry);
        start(window);
        window.setCursorCaptured(true);

        backend.queueKey(GLFW.GLFW_KEY_W, GLFW.GLFW_PRESS);
        backend.queueMouseButton(GLFW.GLFW_MOUSE_BUTTON_RIGHT, GLFW.GLFW_PRESS);
        backend.queueCursor(1.0, 1.0);
        backend.queueCursor(4.0, 5.0);
        window.pollEvents();

        backend.queueFocus(false);
        window.pollEvents();
        InputSnapshot lost = window.captureInputSnapshot(20L);

        assertFalse(lost.focused());
        assertFalse(lost.cursorCaptured());
        assertFalse(lost.keyHeld(InputKey.W));
        assertFalse(lost.keyPressed(InputKey.W));
        assertTrue(lost.keyReleased(InputKey.W));
        assertFalse(lost.mouseButtonHeld(InputMouseButton.RIGHT));
        assertFalse(lost.mouseButtonPressed(InputMouseButton.RIGHT));
        assertTrue(lost.mouseButtonReleased(InputMouseButton.RIGHT));
        assertEquals(0.0, lost.mouseDeltaX(), 0.0);
        assertEquals(0.0, lost.mouseDeltaY(), 0.0);

        backend.queueFocus(true);
        window.pollEvents();
        InputSnapshot regained = window.captureInputSnapshot(21L);
        assertTrue(regained.focused());
        assertFalse(regained.cursorCaptured());
        assertFalse(regained.keyPressed(InputKey.W));
        assertFalse(regained.keyReleased(InputKey.W));
        assertFalse(regained.keyHeld(InputKey.W));

        cleanup(window, registry);
    }

    @Test
    void latePressAndRepeatEventsWhileUnfocusedCannotRestoreHeldState() {
        SnapshotBackend backend = new SnapshotBackend();
        NativeResourceRegistry registry = new NativeResourceRegistry();
        GlfwWindow window = window(backend, registry);
        start(window);

        backend.queueFocus(false);
        backend.queueKey(GLFW.GLFW_KEY_A, GLFW.GLFW_PRESS);
        backend.queueKey(GLFW.GLFW_KEY_W, GLFW.GLFW_REPEAT);
        backend.queueMouseButton(GLFW.GLFW_MOUSE_BUTTON_LEFT, GLFW.GLFW_PRESS);
        window.pollEvents();

        InputSnapshot unfocused = window.captureInputSnapshot(22L);
        assertFalse(unfocused.focused());
        assertFalse(unfocused.keyHeld(InputKey.A));
        assertFalse(unfocused.keyHeld(InputKey.W));
        assertFalse(unfocused.keyPressed(InputKey.A));
        assertFalse(unfocused.mouseButtonHeld(InputMouseButton.LEFT));
        assertFalse(unfocused.mouseButtonPressed(InputMouseButton.LEFT));

        backend.queueFocus(true);
        window.pollEvents();
        InputSnapshot regained = window.captureInputSnapshot(23L);
        assertTrue(regained.focused());
        assertFalse(regained.keyHeld(InputKey.A));
        assertFalse(regained.keyHeld(InputKey.W));
        assertFalse(regained.mouseButtonHeld(InputMouseButton.LEFT));

        cleanup(window, registry);
    }

    @Test
    void failedCaptureValidationConsumesNothing() throws Exception {
        SnapshotBackend backend = new SnapshotBackend();
        NativeResourceRegistry registry = new NativeResourceRegistry();
        GlfwWindow window = window(backend, registry);
        start(window);

        backend.queueKey(GLFW.GLFW_KEY_A, GLFW.GLFW_PRESS);
        window.pollEvents();

        assertThrows(IllegalArgumentException.class, () -> window.captureInputSnapshot(-1L));

        AtomicReference<Throwable> wrongThreadFailure = new AtomicReference<>();
        Thread thread = new Thread(() -> {
            try {
                window.captureInputSnapshot(1L);
            } catch (Throwable failure) {
                wrongThreadFailure.set(failure);
            }
        });
        thread.start();
        thread.join();
        assertInstanceOf(IllegalStateException.class, wrongThreadFailure.get());

        InputSnapshot valid = window.captureInputSnapshot(2L);
        assertTrue(valid.keyHeld(InputKey.A));
        assertTrue(valid.keyPressed(InputKey.A));

        cleanup(window, registry);
    }

    @Test
    void captureDoesNotPollAndReturnedSnapshotRemainsStable() {
        SnapshotBackend backend = new SnapshotBackend();
        NativeResourceRegistry registry = new NativeResourceRegistry();
        GlfwWindow window = window(backend, registry);
        start(window);

        backend.queueKey(GLFW.GLFW_KEY_D, GLFW.GLFW_PRESS);
        window.pollEvents();
        assertEquals(1, backend.pollCount);

        InputSnapshot first = window.captureInputSnapshot(30L);
        assertEquals(1, backend.pollCount);
        assertTrue(first.keyHeld(InputKey.D));
        assertTrue(first.keyPressed(InputKey.D));

        backend.queueKey(GLFW.GLFW_KEY_D, GLFW.GLFW_RELEASE);
        window.pollEvents();
        InputSnapshot second = window.captureInputSnapshot(31L);

        assertTrue(first.keyHeld(InputKey.D));
        assertTrue(first.keyPressed(InputKey.D));
        assertFalse(first.keyReleased(InputKey.D));
        assertFalse(second.keyHeld(InputKey.D));
        assertFalse(second.keyPressed(InputKey.D));
        assertTrue(second.keyReleased(InputKey.D));

        cleanup(window, registry);
    }

    @Test
    void notStartedCaptureFailsBeforeAnyStateCanBeConsumed() {
        SnapshotBackend backend = new SnapshotBackend();
        NativeResourceRegistry registry = new NativeResourceRegistry();
        GlfwWindow window = window(backend, registry);

        assertThrows(IllegalStateException.class, () -> window.captureInputSnapshot(0L));

        window.initialize();
        window.close();
        registry.assertNoOpenResources();
    }

    private static GlfwWindow window(SnapshotBackend backend, NativeResourceRegistry registry) {
        return new GlfwWindow(
                800,
                600,
                "P3-T06 input snapshot test",
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

    private static final class SnapshotBackend implements GlfwWindow.Backend {
        private final List<Runnable> queuedEvents = new ArrayList<>();
        private GlfwWindow.InputEventSink inputSink;
        private GlfwWindow.MotionEventSink motionSink;
        private boolean focused = true;
        private int pollCount;

        void queueKey(int key, int action) {
            queuedEvents.add(() -> inputSink.onKey(key, action));
        }

        void queueMouseButton(int button, int action) {
            queuedEvents.add(() -> inputSink.onMouseButton(button, action));
        }

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
        }

        @Override
        public GlfwWindow.InputCallbackState installInputCallbacks(long handle, GlfwWindow.InputEventSink sink) {
            inputSink = sink;
            return new GlfwWindow.InputCallbackState(new Object(), new Object(), new Object());
        }

        @Override
        public void releaseInputCallbacks(long handle, GlfwWindow.InputCallbackState state) {
            inputSink = null;
        }

        @Override
        public GlfwWindow.MotionCallbackState installCursorPositionCallback(
                long handle,
                GlfwWindow.MotionEventSink sink) {
            motionSink = sink;
            return new GlfwWindow.MotionCallbackState(new Object());
        }

        @Override
        public void releaseCursorPositionCallback(long handle, GlfwWindow.MotionCallbackState state) {
            motionSink = null;
        }

        @Override
        public boolean queryWindowFocused(long handle) {
            return focused;
        }

        @Override
        public void setCursorMode(long handle, int mode) {
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
        public void pollEvents() {
            pollCount++;
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
