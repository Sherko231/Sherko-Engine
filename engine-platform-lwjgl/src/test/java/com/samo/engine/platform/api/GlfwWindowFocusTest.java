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
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;

class GlfwWindowFocusTest {
    @Test
    void cursorCaptureRequiresStartedOwnerThread() throws Exception {
        FocusBackend backend = new FocusBackend();
        NativeResourceRegistry registry = new NativeResourceRegistry();
        GlfwWindow window = window(backend, registry);

        assertThrows(IllegalStateException.class, () -> window.setCursorCaptured(true));
        assertEquals(List.of(), backend.cursorModes);

        window.initialize();
        window.start();

        AtomicReference<Throwable> result = new AtomicReference<>();
        Thread thread = Thread.ofPlatform().start(() -> {
            try {
                window.setCursorCaptured(true);
            } catch (Throwable failure) {
                result.set(failure);
            }
        });
        thread.join(5_000L);

        assertTrue(result.get() instanceof IllegalStateException);
        assertEquals(List.of(), backend.cursorModes);

        window.stop();
        assertThrows(IllegalStateException.class, () -> window.setCursorCaptured(true));
        window.close();
        registry.assertNoOpenResources();
    }

    @Test
    void focusLossClearsHeldInputReleasesCursorAndRequiresExplicitRecapture() {
        FocusBackend backend = new FocusBackend();
        NativeResourceRegistry registry = new NativeResourceRegistry();
        GlfwWindow window = window(backend, registry);
        window.initialize();
        window.start();

        window.setCursorCaptured(true);
        window.setCursorCaptured(true);
        assertEquals(List.of(GLFW.GLFW_CURSOR_DISABLED), backend.cursorModes);
        assertTrue(window.isCursorEffectivelyCapturedForTest());

        backend.queueKey(GLFW.GLFW_KEY_W, GLFW.GLFW_PRESS);
        backend.queueKey(GLFW.GLFW_KEY_W, GLFW.GLFW_REPEAT);
        backend.queueMouse(GLFW.GLFW_MOUSE_BUTTON_LEFT, GLFW.GLFW_PRESS);
        window.pollEvents();
        assertTrue(window.isKeyHeldForTest(GLFW.GLFW_KEY_W));
        assertTrue(window.isMouseButtonHeldForTest(GLFW.GLFW_MOUSE_BUTTON_LEFT));

        backend.queueFocus(false);
        window.pollEvents();

        assertFalse(window.isFocusedForTest());
        assertFalse(window.isKeyHeldForTest(GLFW.GLFW_KEY_W));
        assertFalse(window.isMouseButtonHeldForTest(GLFW.GLFW_MOUSE_BUTTON_LEFT));
        assertFalse(window.isCursorEffectivelyCapturedForTest());
        assertEquals(List.of(GLFW.GLFW_CURSOR_DISABLED, GLFW.GLFW_CURSOR_NORMAL), backend.cursorModes);
        assertEquals("focus:false", backend.trace.get(backend.trace.indexOf("cursor:" + GLFW.GLFW_CURSOR_NORMAL) - 1));

        backend.queueFocus(true);
        window.pollEvents();
        assertTrue(window.isFocusedForTest());
        assertFalse(window.isCursorEffectivelyCapturedForTest());
        assertEquals(List.of(GLFW.GLFW_CURSOR_DISABLED, GLFW.GLFW_CURSOR_NORMAL), backend.cursorModes);

        window.setCursorCaptured(true);
        assertTrue(window.isCursorEffectivelyCapturedForTest());
        assertEquals(
                List.of(GLFW.GLFW_CURSOR_DISABLED, GLFW.GLFW_CURSOR_NORMAL, GLFW.GLFW_CURSOR_DISABLED),
                backend.cursorModes);

        window.setCursorCaptured(false);
        window.setCursorCaptured(false);
        assertEquals(
                List.of(
                        GLFW.GLFW_CURSOR_DISABLED,
                        GLFW.GLFW_CURSOR_NORMAL,
                        GLFW.GLFW_CURSOR_DISABLED,
                        GLFW.GLFW_CURSOR_NORMAL),
                backend.cursorModes);

        window.stop();
        window.close();
        registry.assertNoOpenResources();
    }

    @Test
    void keyAndMouseStateHandleReleaseAndInvalidIndicesSafely() {
        FocusBackend backend = new FocusBackend();
        NativeResourceRegistry registry = new NativeResourceRegistry();
        GlfwWindow window = window(backend, registry);
        window.initialize();
        window.start();

        backend.queueKey(-1, GLFW.GLFW_PRESS);
        backend.queueKey(GLFW.GLFW_KEY_LAST + 1, GLFW.GLFW_PRESS);
        backend.queueMouse(-1, GLFW.GLFW_PRESS);
        backend.queueMouse(GLFW.GLFW_MOUSE_BUTTON_LAST + 1, GLFW.GLFW_PRESS);
        backend.queueKey(GLFW.GLFW_KEY_A, GLFW.GLFW_PRESS);
        backend.queueMouse(GLFW.GLFW_MOUSE_BUTTON_RIGHT, GLFW.GLFW_PRESS);
        window.pollEvents();
        assertTrue(window.isKeyHeldForTest(GLFW.GLFW_KEY_A));
        assertTrue(window.isMouseButtonHeldForTest(GLFW.GLFW_MOUSE_BUTTON_RIGHT));

        backend.queueKey(GLFW.GLFW_KEY_A, GLFW.GLFW_RELEASE);
        backend.queueMouse(GLFW.GLFW_MOUSE_BUTTON_RIGHT, GLFW.GLFW_RELEASE);
        window.pollEvents();
        assertFalse(window.isKeyHeldForTest(GLFW.GLFW_KEY_A));
        assertFalse(window.isMouseButtonHeldForTest(GLFW.GLFW_MOUSE_BUTTON_RIGHT));

        window.stop();
        window.close();
        registry.assertNoOpenResources();
    }

    @Test
    void focusLossWithoutCaptureDoesNotChangeCursorMode() {
        FocusBackend backend = new FocusBackend();
        NativeResourceRegistry registry = new NativeResourceRegistry();
        GlfwWindow window = window(backend, registry);
        window.initialize();
        window.start();

        backend.queueFocus(false);
        window.pollEvents();
        assertEquals(List.of(), backend.cursorModes);

        window.stop();
        window.close();
        registry.assertNoOpenResources();
    }

    @Test
    void focusLossCursorFailureIsThrownOnceFromPollAfterStateClears() {
        FocusBackend backend = new FocusBackend();
        NativeResourceRegistry registry = new NativeResourceRegistry();
        GlfwWindow window = window(backend, registry);
        window.initialize();
        window.start();
        window.setCursorCaptured(true);

        backend.queueKey(GLFW.GLFW_KEY_W, GLFW.GLFW_PRESS);
        window.pollEvents();
        assertTrue(window.isKeyHeldForTest(GLFW.GLFW_KEY_W));

        RuntimeException releaseFailure = new IllegalStateException("cursor release failed");
        backend.cursorFailure = releaseFailure;
        backend.queueFocus(false);

        RuntimeException actual = assertThrows(RuntimeException.class, window::pollEvents);
        assertSame(releaseFailure, actual);
        assertFalse(window.isKeyHeldForTest(GLFW.GLFW_KEY_W));
        assertFalse(window.isCursorEffectivelyCapturedForTest());
        assertEquals(
                List.of(GLFW.GLFW_CURSOR_DISABLED, GLFW.GLFW_CURSOR_NORMAL),
                backend.cursorModes);

        backend.cursorFailure = null;
        window.pollEvents();
        assertEquals(
                List.of(GLFW.GLFW_CURSOR_DISABLED, GLFW.GLFW_CURSOR_NORMAL),
                backend.cursorModes);

        window.setCursorCaptured(false);
        assertFalse(window.isCursorEffectivelyCapturedForTest());
        assertEquals(
                List.of(
                        GLFW.GLFW_CURSOR_DISABLED,
                        GLFW.GLFW_CURSOR_NORMAL,
                        GLFW.GLFW_CURSOR_NORMAL),
                backend.cursorModes);

        window.setCursorCaptured(false);
        assertEquals(3, backend.cursorModes.size());

        window.stop();
        assertEquals(3, backend.cursorModes.size());
        window.close();
        registry.assertNoOpenResources();
    }

    @Test
    void stopRetriesFailedFocusLossCursorNormalization() {
        FocusBackend backend = new FocusBackend();
        NativeResourceRegistry registry = new NativeResourceRegistry();
        GlfwWindow window = window(backend, registry);
        window.initialize();
        window.start();
        window.setCursorCaptured(true);

        RuntimeException releaseFailure = new IllegalStateException("cursor release failed");
        backend.cursorFailure = releaseFailure;
        backend.queueFocus(false);

        RuntimeException actual = assertThrows(RuntimeException.class, window::pollEvents);
        assertSame(releaseFailure, actual);
        assertFalse(window.isCursorEffectivelyCapturedForTest());
        assertEquals(
                List.of(GLFW.GLFW_CURSOR_DISABLED, GLFW.GLFW_CURSOR_NORMAL),
                backend.cursorModes);

        backend.cursorFailure = null;
        window.stop();

        assertEquals(
                List.of(
                        GLFW.GLFW_CURSOR_DISABLED,
                        GLFW.GLFW_CURSOR_NORMAL,
                        GLFW.GLFW_CURSOR_NORMAL),
                backend.cursorModes);

        window.close();
        registry.assertNoOpenResources();
    }

    @Test
    void inputCallbackInstallFailureCleansEarlierStartedState() {
        FocusBackend backend = new FocusBackend();
        RuntimeException failure = new IllegalStateException("input callback install failed");
        backend.inputInstallFailure = failure;
        NativeResourceRegistry registry = new NativeResourceRegistry();
        GlfwWindow window = window(backend, registry);
        window.initialize();

        RuntimeException actual = assertThrows(RuntimeException.class, window::start);
        assertSame(failure, actual);
        assertEquals(1, backend.sizeReleaseCount);
        assertTrue(backend.trace.contains("context:0"));
        assertTrue(backend.trace.contains("capabilities-clear"));

        window.close();
        registry.assertNoOpenResources();
    }

    @Test
    void stopReleasesInputCallbacksExactlyOnce() {
        FocusBackend backend = new FocusBackend();
        NativeResourceRegistry registry = new NativeResourceRegistry();
        GlfwWindow window = window(backend, registry);
        window.initialize();
        window.start();

        window.stop();
        window.close();

        assertEquals(1, backend.inputReleaseCount);
        registry.assertNoOpenResources();
    }

    private static GlfwWindow window(FocusBackend backend, NativeResourceRegistry registry) {
        return new GlfwWindow(
                800,
                600,
                "P3-T04 focus test",
                new EngineLogger(event -> { }),
                registry,
                backend);
    }

    private static final class FocusBackend implements GlfwWindow.Backend {
        private final List<String> trace = new ArrayList<>();
        private final List<Integer> cursorModes = new ArrayList<>();
        private final List<Runnable> queuedEvents = new ArrayList<>();
        private GlfwWindow.InputEventSink inputSink;
        private RuntimeException inputInstallFailure;
        private RuntimeException cursorFailure;
        private int inputReleaseCount;
        private int sizeReleaseCount;
        private boolean focused = true;

        void queueFocus(boolean value) {
            queuedEvents.add(() -> {
                trace.add("focus:" + value);
                focused = value;
                inputSink.onFocus(value);
            });
        }

        void queueKey(int key, int action) {
            queuedEvents.add(() -> inputSink.onKey(key, action));
        }

        void queueMouse(int button, int action) {
            queuedEvents.add(() -> inputSink.onMouseButton(button, action));
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
            trace.add("context:" + handle);
        }

        @Override
        public void createCapabilities() {
        }

        @Override
        public void clearCapabilities() {
            trace.add("capabilities-clear");
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
            trace.add("input-callbacks-install");
            if (inputInstallFailure != null) {
                throw inputInstallFailure;
            }
            inputSink = sink;
            return new GlfwWindow.InputCallbackState(new Object(), new Object(), new Object());
        }

        @Override
        public void releaseInputCallbacks(long handle, GlfwWindow.InputCallbackState state) {
            trace.add("input-callbacks-release");
            inputReleaseCount++;
            inputSink = null;
        }

        @Override
        public boolean queryWindowFocused(long handle) {
            return focused;
        }

        @Override
        public void setCursorMode(long handle, int mode) {
            trace.add("cursor:" + mode);
            cursorModes.add(mode);
            if (cursorFailure != null) {
                throw cursorFailure;
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
