package com.samo.engine.platform.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

class GlfwWindowTest {
    @Test
    void constructorRejectsInvalidInputsBeforeBackendActivity() {
        FakeBackend backend = new FakeBackend();
        EngineLogger logger = new EngineLogger(event -> { });
        NativeResourceRegistry registry = new NativeResourceRegistry();

        assertThrows(IllegalArgumentException.class,
                () -> new GlfwWindow(0, 720, "window", logger, registry, backend));
        assertThrows(IllegalArgumentException.class,
                () -> new GlfwWindow(1280, -1, "window", logger, registry, backend));
        assertThrows(NullPointerException.class,
                () -> new GlfwWindow(1280, 720, null, logger, registry, backend));
        assertThrows(IllegalArgumentException.class,
                () -> new GlfwWindow(1280, 720, "   ", logger, registry, backend));
        assertThrows(NullPointerException.class,
                () -> new GlfwWindow(1280, 720, "window", null, registry, backend));
        assertThrows(NullPointerException.class,
                () -> new GlfwWindow(1280, 720, "window", logger, null, backend));
        assertEquals(List.of(), backend.trace);
    }

    @Test
    void initializeAppliesExactHintsAndRegistersOwnedWindow() {
        FakeBackend backend = new FakeBackend();
        NativeResourceRegistry registry = new NativeResourceRegistry();
        GlfwWindow window = window(backend, registry, new ArrayList<>());

        window.initialize();

        assertEquals(List.of(
                "callback-install",
                "glfw-init",
                "hints-default",
                hint(GLFW.GLFW_CLIENT_API, GLFW.GLFW_OPENGL_API),
                hint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 4),
                hint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 6),
                hint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE),
                hint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GLFW.GLFW_TRUE),
                hint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE),
                hint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE),
                "create:1280x720:  title  "), backend.trace);

        assertThrows(IllegalStateException.class, registry::assertNoOpenResources);
        window.close();
        registry.assertNoOpenResources();
        assertEquals(1, backend.destroyCount);
    }

    @Test
    void successfulLifecycleLogsActualStringsAndCleansInOwnershipOrder() {
        FakeBackend backend = new FakeBackend();
        backend.version = "4.6 fixture";
        backend.renderer = "fixture renderer";
        NativeResourceRegistry registry = new NativeResourceRegistry();
        List<EngineLogger.Event> events = new ArrayList<>();
        GlfwWindow window = window(backend, registry, events);

        window.initialize();
        window.start();
        window.stop();
        window.close();
        registry.assertNoOpenResources();

        assertEquals(2, events.size());
        assertEvent(events.get(0), "OpenGL version: 4.6 fixture");
        assertEvent(events.get(1), "OpenGL renderer: fixture renderer");
        assertEquals(List.of(
                "callback-install",
                "glfw-init",
                "hints-default",
                hint(GLFW.GLFW_CLIENT_API, GLFW.GLFW_OPENGL_API),
                hint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 4),
                hint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 6),
                hint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE),
                hint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GLFW.GLFW_TRUE),
                hint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE),
                hint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE),
                "create:1280x720:  title  ",
                "context:101",
                "capabilities-create",
                "show:101",
                "hide:101",
                "context:0",
                "capabilities-clear",
                "destroy:101",
                "glfw-terminate",
                "callback-restore",
                "callback-free"), backend.trace);
    }

    @Test
    void initFailureRestoresCallbackWithoutPretendingGlfwWasInitialized() {
        FakeBackend backend = new FakeBackend();
        backend.initResult = false;
        NativeResourceRegistry registry = new NativeResourceRegistry();
        GlfwWindow window = window(backend, registry, new ArrayList<>());

        IllegalStateException failure = assertThrows(IllegalStateException.class, window::initialize);
        assertTrue(failure.getMessage().contains("initialize GLFW"));
        assertEquals(List.of("callback-install", "glfw-init", "callback-restore", "callback-free"), backend.trace);

        window.close();
        registry.assertNoOpenResources();
        assertEquals(0, backend.destroyCount);
        assertEquals(0, backend.terminateCount);
    }

    @Test
    void zeroWindowRollsBackGlfwAndCallback() {
        FakeBackend backend = new FakeBackend();
        backend.windowHandle = 0L;
        NativeResourceRegistry registry = new NativeResourceRegistry();
        GlfwWindow window = window(backend, registry, new ArrayList<>());

        assertThrows(IllegalStateException.class, window::initialize);

        assertEquals(1, backend.terminateCount);
        assertEquals(0, backend.destroyCount);
        assertEquals("callback-free", backend.trace.getLast());
        window.close();
        registry.assertNoOpenResources();
    }

    @Test
    void registryFailureDestroysUnregisteredWindowDirectlyThenRollsBack() {
        FakeBackend backend = new FakeBackend();
        NativeResourceRegistry registry = new NativeResourceRegistry();
        NativeResourceRegistry.Registration existing =
                registry.register("GLFW window", 101L, () -> { });
        GlfwWindow window = window(backend, registry, new ArrayList<>());

        assertThrows(IllegalStateException.class, window::initialize);

        assertEquals(1, backend.destroyCount);
        assertEquals(1, backend.terminateCount);
        assertTrue(backend.trace.indexOf("destroy:101") < backend.trace.indexOf("glfw-terminate"));
        window.close();
        existing.close();
        registry.assertNoOpenResources();
        assertEquals(1, backend.destroyCount);
    }

    @Test
    void startCapabilityFailureDetachesAndLaterCloseDestroysOnce() {
        FakeBackend backend = new FakeBackend();
        backend.openGl46 = false;
        NativeResourceRegistry registry = new NativeResourceRegistry();
        GlfwWindow window = window(backend, registry, new ArrayList<>());
        window.initialize();

        assertThrows(IllegalStateException.class, window::start);

        assertTrue(backend.trace.contains("context:0"));
        assertTrue(backend.trace.contains("capabilities-clear"));
        assertEquals(0, backend.destroyCount);
        window.close();
        registry.assertNoOpenResources();
        assertEquals(1, backend.destroyCount);
        assertEquals(1, backend.terminateCount);
    }

    @Test
    void loggingFailuresDoNotLeakAfterOwnerCloses() {
        verifyLoggingFailure(new IllegalStateException("log runtime"));
        verifyLoggingFailure(new AssertionError("log error"));
    }

    @Test
    void stopContinuesCleanupWhenHideFails() {
        FakeBackend backend = new FakeBackend();
        RuntimeException hideFailure = new IllegalStateException("hide failed");
        backend.hideFailure = hideFailure;
        NativeResourceRegistry registry = new NativeResourceRegistry();
        GlfwWindow window = window(backend, registry, new ArrayList<>());
        window.initialize();
        window.start();

        RuntimeException actual = assertThrows(RuntimeException.class, window::stop);
        assertSame(hideFailure, actual);
        assertTrue(backend.trace.contains("context:0"));
        assertTrue(backend.trace.contains("capabilities-clear"));

        window.close();
        registry.assertNoOpenResources();
        assertEquals(1, backend.destroyCount);
    }

    @Test
    void wrongThreadStartFailsBeforeBackendStartAndOwnerCanClose() throws Exception {
        FakeBackend backend = new FakeBackend();
        NativeResourceRegistry registry = new NativeResourceRegistry();
        GlfwWindow window = window(backend, registry, new ArrayList<>());
        window.initialize();
        int traceSizeBeforeStart = backend.trace.size();
        AtomicReference<Throwable> result = new AtomicReference<>();

        Thread thread = Thread.ofPlatform().start(() -> {
            try {
                window.start();
            } catch (Throwable failure) {
                result.set(failure);
            }
        });
        thread.join(5_000L);

        assertTrue(result.get() instanceof IllegalStateException);
        assertEquals(traceSizeBeforeStart, backend.trace.size());
        window.close();
        registry.assertNoOpenResources();
        assertEquals(1, backend.destroyCount);
    }

    private static void verifyLoggingFailure(Throwable loggingFailure) {
        FakeBackend backend = new FakeBackend();
        NativeResourceRegistry registry = new NativeResourceRegistry();
        EngineLogger logger = new EngineLogger(event -> throwUnchecked(loggingFailure));
        GlfwWindow window = new GlfwWindow(1280, 720, "window", logger, registry, backend);
        window.initialize();

        Throwable actual;
        if (loggingFailure instanceof Error) {
            actual = assertThrows(Error.class, window::start);
        } else {
            actual = assertThrows(RuntimeException.class, window::start);
        }
        assertSame(loggingFailure, actual);
        assertTrue(backend.trace.contains("context:0"));
        assertTrue(backend.trace.contains("capabilities-clear"));

        window.close();
        registry.assertNoOpenResources();
        assertEquals(1, backend.destroyCount);
    }

    private static GlfwWindow window(
            FakeBackend backend,
            NativeResourceRegistry registry,
            List<EngineLogger.Event> events) {
        return new GlfwWindow(
                1280,
                720,
                "  title  ",
                new EngineLogger(events::add),
                registry,
                backend);
    }

    private static void assertEvent(EngineLogger.Event event, String expectedMessage) {
        assertEquals(EngineLogger.Level.INFO, event.level());
        assertEquals(expectedMessage, event.message());
        assertEquals("platform", event.context().subsystem());
        assertEquals(null, event.context().frame());
        assertEquals(null, event.context().simulationTick());
        assertEquals(null, event.context().connection());
        assertEquals(null, event.context().entity());
    }

    private static String hint(int hint, int value) {
        return "hint:" + hint + "=" + value;
    }

    private static void throwUnchecked(Throwable failure) {
        if (failure instanceof RuntimeException runtime) {
            throw runtime;
        }
        throw (Error) failure;
    }

    private static final class FakeBackend implements GlfwWindow.Backend {
        private final List<String> trace = new ArrayList<>();
        private boolean initResult = true;
        private long windowHandle = 101L;
        private boolean openGl46 = true;
        private String version = "4.6 fixture";
        private String renderer = "fixture renderer";
        private RuntimeException hideFailure;
        private int destroyCount;
        private int terminateCount;

        @Override
        public GlfwWindow.CallbackState installErrorCallback() {
            trace.add("callback-install");
            return new GlfwWindow.CallbackState(new Object(), new Object());
        }

        @Override
        public void restoreErrorCallback(GlfwWindow.CallbackState state) {
            trace.add("callback-restore");
        }

        @Override
        public void freeOwnedErrorCallback(GlfwWindow.CallbackState state) {
            trace.add("callback-free");
        }

        @Override
        public boolean initGlfw() {
            trace.add("glfw-init");
            return initResult;
        }

        @Override
        public void terminateGlfw() {
            trace.add("glfw-terminate");
            terminateCount++;
        }

        @Override
        public void defaultWindowHints() {
            trace.add("hints-default");
        }

        @Override
        public void windowHint(int hint, int value) {
            trace.add(GlfwWindowTest.hint(hint, value));
        }

        @Override
        public long createWindow(int width, int height, String title) {
            trace.add("create:" + width + "x" + height + ":" + title);
            return windowHandle;
        }

        @Override
        public void destroyWindow(long handle) {
            trace.add("destroy:" + handle);
            destroyCount++;
        }

        @Override
        public void makeContextCurrent(long handle) {
            trace.add("context:" + handle);
        }

        @Override
        public void createCapabilities() {
            trace.add("capabilities-create");
        }

        @Override
        public void clearCapabilities() {
            trace.add("capabilities-clear");
        }

        @Override
        public boolean openGl46Supported() {
            return openGl46;
        }

        @Override
        public String glVersion() {
            return version;
        }

        @Override
        public String glRenderer() {
            return renderer;
        }

        @Override
        public void showWindow(long handle) {
            trace.add("show:" + handle);
        }

        @Override
        public void hideWindow(long handle) {
            trace.add("hide:" + handle);
            if (hideFailure != null) {
                throw hideFailure;
            }
        }
    }
}
