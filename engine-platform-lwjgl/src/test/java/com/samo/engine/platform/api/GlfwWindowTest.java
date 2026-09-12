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
        RecordingSizeListener listener = new RecordingSizeListener();

        assertThrows(IllegalArgumentException.class,
                () -> new GlfwWindow(0, 720, "window", logger, registry, listener, backend));
        assertThrows(IllegalArgumentException.class,
                () -> new GlfwWindow(1280, -1, "window", logger, registry, listener, backend));
        assertThrows(NullPointerException.class,
                () -> new GlfwWindow(1280, 720, null, logger, registry, listener, backend));
        assertThrows(IllegalArgumentException.class,
                () -> new GlfwWindow(1280, 720, "   ", logger, registry, listener, backend));
        assertThrows(NullPointerException.class,
                () -> new GlfwWindow(1280, 720, "window", null, registry, listener, backend));
        assertThrows(NullPointerException.class,
                () -> new GlfwWindow(1280, 720, "window", logger, null, listener, backend));
        assertThrows(NullPointerException.class,
                () -> new GlfwWindow(1280, 720, "window", logger, registry, null, backend));
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
                "size-callbacks-install:101",
                "logical-query:101",
                "framebuffer-query:101",
                "show:101",
                "size-callbacks-release:101",
                "hide:101",
                "context:0",
                "capabilities-clear",
                "destroy:101",
                "glfw-terminate",
                "callback-restore",
                "callback-free"), backend.trace);
    }

    @Test
    void initialLogicalAndFramebufferSizesStayIndependentUntilPoll() {
        FakeBackend backend = new FakeBackend();
        backend.logicalSize = new GlfwWindow.Dimensions(800, 600);
        backend.framebufferSize = new GlfwWindow.Dimensions(1200, 900);
        RecordingSizeListener listener = new RecordingSizeListener();
        NativeResourceRegistry registry = new NativeResourceRegistry();
        GlfwWindow window = window(backend, registry, new ArrayList<>(), listener);

        window.initialize();
        window.start();
        assertEquals(List.of(), listener.events);

        window.pollEvents();

        assertEquals(List.of("logical:800x600", "framebuffer:1200x900"), listener.events);
        assertEquals(1, backend.pollCount);
        window.stop();
        window.close();
        registry.assertNoOpenResources();
    }

    @Test
    void nativeSizeCallbacksCoalesceIndependentlyAndAllowZeroFramebuffer() {
        FakeBackend backend = new FakeBackend();
        RecordingSizeListener listener = new RecordingSizeListener();
        NativeResourceRegistry registry = new NativeResourceRegistry();
        GlfwWindow window = window(backend, registry, new ArrayList<>(), listener);
        window.initialize();
        window.start();
        window.pollEvents();
        listener.events.clear();

        backend.queueLogicalSize(810, 610);
        backend.queueFramebufferSize(1620, 1220);
        backend.queueLogicalSize(820, 620);
        backend.queueFramebufferSize(0, 0);
        window.pollEvents();

        assertEquals(List.of("logical:820x620", "framebuffer:0x0"), listener.events);
        window.stop();
        window.close();
        registry.assertNoOpenResources();
    }

    @Test
    void negativeNativeDimensionsFailBeforePublicReceiver() {
        FakeBackend backend = new FakeBackend();
        RecordingSizeListener listener = new RecordingSizeListener();
        NativeResourceRegistry registry = new NativeResourceRegistry();
        GlfwWindow window = window(backend, registry, new ArrayList<>(), listener);
        window.initialize();
        window.start();
        window.pollEvents();
        listener.events.clear();

        backend.queueLogicalSize(-1, 600);
        IllegalStateException failure = assertThrows(IllegalStateException.class, window::pollEvents);

        assertTrue(failure.getMessage().contains("negative logical window dimensions"));
        assertEquals(List.of(), listener.events);
        window.stop();
        window.close();
        registry.assertNoOpenResources();
    }

    @Test
    void negativeInitialDimensionsFailStartAndReleaseCallbacks() {
        FakeBackend backend = new FakeBackend();
        backend.framebufferSize = new GlfwWindow.Dimensions(-1, 720);
        NativeResourceRegistry registry = new NativeResourceRegistry();
        GlfwWindow window = window(backend, registry, new ArrayList<>());
        window.initialize();

        IllegalStateException failure = assertThrows(IllegalStateException.class, window::start);

        assertTrue(failure.getMessage().contains("negative framebuffer dimensions"));
        assertTrue(backend.trace.contains("size-callbacks-release:101"));
        assertTrue(backend.trace.contains("context:0"));
        assertTrue(backend.trace.contains("capabilities-clear"));
        window.close();
        registry.assertNoOpenResources();
    }

    @Test
    void pollEventsRequiresStartedOwnerThreadAndNeverPollsWhenIllegal() throws Exception {
        FakeBackend backend = new FakeBackend();
        NativeResourceRegistry registry = new NativeResourceRegistry();
        GlfwWindow window = window(backend, registry, new ArrayList<>());

        assertThrows(IllegalStateException.class, window::pollEvents);
        assertEquals(0, backend.pollCount);

        window.initialize();
        assertThrows(IllegalStateException.class, window::pollEvents);
        window.start();

        AtomicReference<Throwable> result = new AtomicReference<>();
        Thread thread = Thread.ofPlatform().start(() -> {
            try {
                window.pollEvents();
            } catch (Throwable failure) {
                result.set(failure);
            }
        });
        thread.join(5_000L);

        assertTrue(result.get() instanceof IllegalStateException);
        assertEquals(0, backend.pollCount);

        window.pollEvents();
        assertEquals(1, backend.pollCount);
        window.stop();
        assertThrows(IllegalStateException.class, window::pollEvents);
        assertEquals(1, backend.pollCount);
        window.close();
        registry.assertNoOpenResources();
    }

    @Test
    void receiverFailurePropagatesUnchangedAfterNativePoll() {
        FakeBackend backend = new FakeBackend();
        RuntimeException receiverFailure = new IllegalStateException("receiver failed");
        WindowSizeListener listener = new WindowSizeListener() {
            @Override
            public void onLogicalWindowSizeChanged(int width, int height) {
                throw receiverFailure;
            }

            @Override
            public void onFramebufferSizeChanged(int width, int height) {
                throw new AssertionError("framebuffer delivery must not run after logical failure");
            }
        };
        NativeResourceRegistry registry = new NativeResourceRegistry();
        GlfwWindow window = window(backend, registry, new ArrayList<>(), listener);
        window.initialize();
        window.start();

        RuntimeException actual = assertThrows(RuntimeException.class, window::pollEvents);

        assertSame(receiverFailure, actual);
        assertEquals(1, backend.pollCount);
        window.stop();
        window.close();
        registry.assertNoOpenResources();
    }

    @Test
    void windowModeRejectsIllegalCallsBeforeTransitionActivity() throws Exception {
        FakeBackend backend = new FakeBackend();
        NativeResourceRegistry registry = new NativeResourceRegistry();
        GlfwWindow window = window(backend, registry, new ArrayList<>());

        assertThrows(NullPointerException.class, () -> window.setWindowMode(null));
        assertThrows(IllegalStateException.class,
                () -> window.setWindowMode(WindowMode.BORDERLESS_FULLSCREEN));
        assertEquals(0, backend.modeTransitionCount);

        window.initialize();
        window.start();

        AtomicReference<Throwable> result = new AtomicReference<>();
        Thread thread = Thread.ofPlatform().start(() -> {
            try {
                window.setWindowMode(WindowMode.BORDERLESS_FULLSCREEN);
            } catch (Throwable failure) {
                result.set(failure);
            }
        });
        thread.join(5_000L);
        assertTrue(result.get() instanceof IllegalStateException);
        assertEquals(0, backend.modeTransitionCount);

        int traceSize = backend.trace.size();
        window.setWindowMode(WindowMode.WINDOWED);
        assertEquals(traceSize, backend.trace.size());

        window.stop();
        assertThrows(IllegalStateException.class,
                () -> window.setWindowMode(WindowMode.BORDERLESS_FULLSCREEN));
        assertEquals(0, backend.modeTransitionCount);
        window.close();
        registry.assertNoOpenResources();
    }

    @Test
    void windowModeCycleCapturesAndRestoresOriginalGeometry() {
        FakeBackend backend = new FakeBackend();
        backend.windowPosition = new GlfwWindow.Position(-120, 75);
        backend.logicalSize = new GlfwWindow.Dimensions(1111, 777);
        backend.monitorPosition = new GlfwWindow.Position(1920, 0);
        backend.videoMode = new GlfwWindow.VideoMode(2560, 1440, 165);
        NativeResourceRegistry registry = new NativeResourceRegistry();
        GlfwWindow window = window(backend, registry, new ArrayList<>());
        window.initialize();
        window.start();

        window.setWindowMode(WindowMode.BORDERLESS_FULLSCREEN);
        assertEquals(0L, backend.attachedMonitor);
        assertEquals(new GlfwWindow.Position(1920, 0), backend.windowPosition);
        assertEquals(new GlfwWindow.Dimensions(2560, 1440), backend.logicalSize);
        assertTrue(backend.trace.contains("decorated:false"));

        window.setWindowMode(WindowMode.EXCLUSIVE_FULLSCREEN);
        assertEquals(202L, backend.attachedMonitor);
        assertEquals(new GlfwWindow.Dimensions(2560, 1440), backend.logicalSize);

        window.setWindowMode(WindowMode.BORDERLESS_FULLSCREEN);
        assertEquals(0L, backend.attachedMonitor);

        window.setWindowMode(WindowMode.WINDOWED);
        assertEquals(0L, backend.attachedMonitor);
        assertEquals(new GlfwWindow.Position(-120, 75), backend.windowPosition);
        assertEquals(new GlfwWindow.Dimensions(1111, 777), backend.logicalSize);
        assertTrue(backend.trace.contains("decorated:true"));

        backend.windowPosition = new GlfwWindow.Position(300, 200);
        backend.logicalSize = new GlfwWindow.Dimensions(900, 700);
        window.setWindowMode(WindowMode.EXCLUSIVE_FULLSCREEN);
        window.setWindowMode(WindowMode.WINDOWED);
        assertEquals(new GlfwWindow.Position(300, 200), backend.windowPosition);
        assertEquals(new GlfwWindow.Dimensions(900, 700), backend.logicalSize);

        window.stop();
        window.close();
        registry.assertNoOpenResources();
    }

    @Test
    void borderlessAndExclusiveUsePrimaryMonitorCurrentMode() {
        FakeBackend backend = new FakeBackend();
        backend.monitorPosition = new GlfwWindow.Position(-2560, 40);
        backend.videoMode = new GlfwWindow.VideoMode(2560, 1440, 144);
        NativeResourceRegistry registry = new NativeResourceRegistry();
        GlfwWindow window = window(backend, registry, new ArrayList<>());
        window.initialize();
        window.start();

        window.setWindowMode(WindowMode.BORDERLESS_FULLSCREEN);
        assertTrue(backend.trace.contains("window-monitor:101:0:-2560,40:2560x1440@-1"));

        window.setWindowMode(WindowMode.EXCLUSIVE_FULLSCREEN);
        assertTrue(backend.trace.contains("window-monitor:101:202:0,0:2560x1440@144"));

        window.setWindowMode(WindowMode.WINDOWED);
        window.stop();
        window.close();
        registry.assertNoOpenResources();
    }

    @Test
    void invalidMonitorStateFailsBeforeModeTransition() {
        FakeBackend backend = new FakeBackend();
        NativeResourceRegistry registry = new NativeResourceRegistry();
        GlfwWindow window = window(backend, registry, new ArrayList<>());
        window.initialize();
        window.start();

        backend.primaryMonitor = 0L;
        IllegalStateException missing = assertThrows(
                IllegalStateException.class,
                () -> window.setWindowMode(WindowMode.BORDERLESS_FULLSCREEN));
        assertTrue(missing.getMessage().contains("primary monitor"));
        assertEquals(0, backend.modeTransitionCount);

        backend.primaryMonitor = 202L;
        backend.videoMode = new GlfwWindow.VideoMode(0, 1080, 60);
        IllegalStateException invalid = assertThrows(
                IllegalStateException.class,
                () -> window.setWindowMode(WindowMode.EXCLUSIVE_FULLSCREEN));
        assertTrue(invalid.getMessage().contains("invalid primary monitor video mode"));
        assertEquals(0, backend.modeTransitionCount);

        window.stop();
        window.close();
        registry.assertNoOpenResources();
    }

    @Test
    void transitionFailurePropagatesAndAttemptsRollbackWithSuppressedFailure() {
        FakeBackend backend = new FakeBackend();
        RuntimeException transitionFailure = new IllegalStateException("transition failed");
        RuntimeException rollbackFailure = new IllegalArgumentException("rollback failed");
        backend.transitionFailures.add(transitionFailure);
        backend.transitionFailures.add(rollbackFailure);
        NativeResourceRegistry registry = new NativeResourceRegistry();
        GlfwWindow window = window(backend, registry, new ArrayList<>());
        window.initialize();
        window.start();

        RuntimeException actual = assertThrows(
                RuntimeException.class,
                () -> window.setWindowMode(WindowMode.BORDERLESS_FULLSCREEN));

        assertSame(transitionFailure, actual);
        assertEquals(1, actual.getSuppressed().length);
        assertSame(rollbackFailure, actual.getSuppressed()[0]);
        assertEquals(2, backend.modeTransitionCount);

        backend.transitionFailures.clear();
        window.setWindowMode(WindowMode.WINDOWED);
        assertEquals(2, backend.modeTransitionCount);
        window.setWindowMode(WindowMode.EXCLUSIVE_FULLSCREEN);
        assertEquals(3, backend.modeTransitionCount);

        window.setWindowMode(WindowMode.WINDOWED);
        window.stop();
        window.close();
        registry.assertNoOpenResources();
    }

    @Test
    void sizeSetupFailureCleansContextAndLaterCloseDestroysOnce() {
        FakeBackend backend = new FakeBackend();
        RuntimeException sizeFailure = new IllegalStateException("size setup failed");
        backend.sizeInstallFailure = sizeFailure;
        NativeResourceRegistry registry = new NativeResourceRegistry();
        GlfwWindow window = window(backend, registry, new ArrayList<>());
        window.initialize();

        RuntimeException actual = assertThrows(RuntimeException.class, window::start);

        assertSame(sizeFailure, actual);
        assertTrue(backend.trace.contains("context:0"));
        assertTrue(backend.trace.contains("capabilities-clear"));
        window.close();
        registry.assertNoOpenResources();
        assertEquals(1, backend.destroyCount);
    }

    @Test
    void stopContinuesCleanupAndDoesNotRetryFailedSizeCallbackRelease() {
        FakeBackend backend = new FakeBackend();
        RuntimeException releaseFailure = new IllegalStateException("callback release failed");
        backend.sizeReleaseFailure = releaseFailure;
        NativeResourceRegistry registry = new NativeResourceRegistry();
        GlfwWindow window = window(backend, registry, new ArrayList<>());
        window.initialize();
        window.start();

        RuntimeException actual = assertThrows(RuntimeException.class, window::stop);

        assertSame(releaseFailure, actual);
        assertTrue(backend.trace.contains("hide:101"));
        assertTrue(backend.trace.contains("context:0"));
        assertTrue(backend.trace.contains("capabilities-clear"));

        backend.sizeReleaseFailure = null;
        window.close();
        registry.assertNoOpenResources();
        assertEquals(1, backend.sizeReleaseCount);
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
        return window(backend, registry, events, new RecordingSizeListener());
    }

    private static GlfwWindow window(
            FakeBackend backend,
            NativeResourceRegistry registry,
            List<EngineLogger.Event> events,
            WindowSizeListener listener) {
        return new GlfwWindow(
                1280,
                720,
                "  title  ",
                new EngineLogger(events::add),
                registry,
                listener,
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

    private static final class RecordingSizeListener implements WindowSizeListener {
        private final List<String> events = new ArrayList<>();

        @Override
        public void onLogicalWindowSizeChanged(int width, int height) {
            events.add("logical:" + width + "x" + height);
        }

        @Override
        public void onFramebufferSizeChanged(int width, int height) {
            events.add("framebuffer:" + width + "x" + height);
        }
    }

    private static final class FakeBackend implements GlfwWindow.Backend {
        private final List<String> trace = new ArrayList<>();
        private final List<Runnable> queuedEvents = new ArrayList<>();
        private final List<RuntimeException> transitionFailures = new ArrayList<>();
        private boolean initResult = true;
        private long windowHandle = 101L;
        private boolean openGl46 = true;
        private String version = "4.6 fixture";
        private String renderer = "fixture renderer";
        private GlfwWindow.Dimensions logicalSize = new GlfwWindow.Dimensions(1280, 720);
        private GlfwWindow.Dimensions framebufferSize = new GlfwWindow.Dimensions(1280, 720);
        private GlfwWindow.Position windowPosition = new GlfwWindow.Position(100, 80);
        private long primaryMonitor = 202L;
        private GlfwWindow.VideoMode videoMode = new GlfwWindow.VideoMode(1920, 1080, 120);
        private GlfwWindow.Position monitorPosition = new GlfwWindow.Position(0, 0);
        private long attachedMonitor;
        private RuntimeException sizeInstallFailure;
        private RuntimeException sizeReleaseFailure;
        private RuntimeException hideFailure;
        private GlfwWindow.SizeEventSink sizeEventSink;
        private int pollCount;
        private int sizeReleaseCount;
        private int modeTransitionCount;
        private int destroyCount;
        private int terminateCount;

        void queueLogicalSize(int width, int height) {
            queuedEvents.add(() -> sizeEventSink.onLogicalSize(width, height));
        }

        void queueFramebufferSize(int width, int height) {
            queuedEvents.add(() -> sizeEventSink.onFramebufferSize(width, height));
        }

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
        public GlfwWindow.SizeCallbackState installSizeCallbacks(long handle, GlfwWindow.SizeEventSink sink) {
            trace.add("size-callbacks-install:" + handle);
            if (sizeInstallFailure != null) {
                throw sizeInstallFailure;
            }
            sizeEventSink = sink;
            return new GlfwWindow.SizeCallbackState(new Object(), new Object());
        }

        @Override
        public void releaseSizeCallbacks(long handle, GlfwWindow.SizeCallbackState state) {
            trace.add("size-callbacks-release:" + handle);
            sizeReleaseCount++;
            if (sizeReleaseFailure != null) {
                throw sizeReleaseFailure;
            }
            sizeEventSink = null;
        }

        @Override
        public GlfwWindow.Dimensions queryLogicalSize(long handle) {
            trace.add("logical-query:" + handle);
            return logicalSize;
        }

        @Override
        public GlfwWindow.Dimensions queryFramebufferSize(long handle) {
            trace.add("framebuffer-query:" + handle);
            return framebufferSize;
        }

        @Override
        public GlfwWindow.Position queryWindowPosition(long handle) {
            trace.add("position-query:" + handle);
            return windowPosition;
        }

        @Override
        public long primaryMonitor() {
            trace.add("primary-monitor");
            return primaryMonitor;
        }

        @Override
        public GlfwWindow.VideoMode queryVideoMode(long monitor) {
            trace.add("video-mode:" + monitor);
            return videoMode;
        }

        @Override
        public GlfwWindow.Position queryMonitorPosition(long monitor) {
            trace.add("monitor-position:" + monitor);
            return monitorPosition;
        }

        @Override
        public void setDecorated(long handle, boolean decorated) {
            trace.add("decorated:" + decorated);
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
            trace.add("window-monitor:" + handle + ":" + monitor + ":"
                    + x + "," + y + ":" + width + "x" + height + "@" + refreshRate);
            modeTransitionCount++;
            if (!transitionFailures.isEmpty()) {
                throw transitionFailures.removeFirst();
            }
            attachedMonitor = monitor;
            windowPosition = new GlfwWindow.Position(x, y);
            logicalSize = new GlfwWindow.Dimensions(width, height);
        }

        @Override
        public void pollEvents() {
            trace.add("poll-events");
            pollCount++;
            List<Runnable> current = List.copyOf(queuedEvents);
            queuedEvents.clear();
            current.forEach(Runnable::run);
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
