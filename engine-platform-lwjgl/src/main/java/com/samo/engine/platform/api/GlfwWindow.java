package com.samo.engine.platform.api;

import com.samo.engine.core.api.EngineLogger;
import com.samo.engine.core.api.EngineSubsystem;
import com.samo.engine.core.api.NativeResourceRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL43;

/** Owns one GLFW window and OpenGL 4.6 Core context for one subsystem lifetime. */
public final class GlfwWindow extends EngineSubsystem {
    private static final String RESOURCE_TYPE = "GLFW window";
    private static final EngineLogger.Context LOG_CONTEXT = new EngineLogger.Context(null, null, "platform", null, null);
    private static final WindowSizeListener NO_OP_SIZE_LISTENER = new WindowSizeListener() {
        @Override
        public void onLogicalWindowSizeChanged(int width, int height) {
        }

        @Override
        public void onFramebufferSizeChanged(int width, int height) {
        }
    };

    private final int width;
    private final int height;
    private final String title;
    private final EngineLogger logger;
    private final NativeResourceRegistry nativeResources;
    private final OpenGlDebugMode openGlDebugMode;
    private final GlfwNativeBackend backend;
    private final GlfwDeferredSizeDelivery sizeDelivery;
    private final GlfwWindowModeController windowModeController;
    private final GlfwInputState inputState = new GlfwInputState();
    private final GlfwMouseMotionTracker mouseMotion = new GlfwMouseMotionTracker();
    private final GlfwCursorCaptureController cursorCapture;

    private final OpenGlThreadGuard openGlThreadGuard = new OpenGlThreadGuard();

    private GlfwErrorCallbackRegistration callbackState;
    private GlfwSizeCallbackRegistration sizeCallbackState;
    private GlfwInputCallbackRegistration inputCallbackState;
    private GlfwCursorPositionCallbackRegistration motionCallbackState;
    private OpenGlDebugCallbackRegistration debugCallbackState;
    private boolean glfwInitialized;
    private long windowHandle;
    private NativeResourceRegistry.Registration windowRegistration;
    private boolean contextCurrent;
    private boolean capabilitiesCreated;
    private boolean eventPollingEnabled;
    private Throwable pendingInputFailure;
    private Throwable pendingOpenGlDebugFailure;

    public GlfwWindow(int width, int height, String title, EngineLogger logger, NativeResourceRegistry nativeResources) {
        this(width, height, title, logger, nativeResources, NO_OP_SIZE_LISTENER, OpenGlDebugMode.DISABLED);
    }

    public GlfwWindow(int width, int height, String title, EngineLogger logger, NativeResourceRegistry nativeResources, OpenGlDebugMode openGlDebugMode) {
        this(width, height, title, logger, nativeResources, NO_OP_SIZE_LISTENER, openGlDebugMode);
    }

    public GlfwWindow(int width, int height, String title, EngineLogger logger, NativeResourceRegistry nativeResources, WindowSizeListener sizeListener) {
        this(width, height, title, logger, nativeResources, sizeListener, OpenGlDebugMode.DISABLED);
    }

    public GlfwWindow(int width, int height, String title, EngineLogger logger, NativeResourceRegistry nativeResources, WindowSizeListener sizeListener,
        OpenGlDebugMode openGlDebugMode) {
        this(width, height, title, logger, nativeResources, sizeListener, openGlDebugMode, new LwjglGlfwNativeBackend());
    }

    GlfwWindow(int width, int height, String title, EngineLogger logger, NativeResourceRegistry nativeResources, GlfwNativeBackend backend) {
        this(width, height, title, logger, nativeResources, NO_OP_SIZE_LISTENER, OpenGlDebugMode.DISABLED, backend);
    }

    GlfwWindow(int width, int height, String title, EngineLogger logger, NativeResourceRegistry nativeResources, OpenGlDebugMode openGlDebugMode, GlfwNativeBackend backend) {
        this(width, height, title, logger, nativeResources, NO_OP_SIZE_LISTENER, openGlDebugMode, backend);
    }

    GlfwWindow(int width, int height, String title, EngineLogger logger, NativeResourceRegistry nativeResources, WindowSizeListener sizeListener, GlfwNativeBackend backend) {
        this(width, height, title, logger, nativeResources, sizeListener, OpenGlDebugMode.DISABLED, backend);
    }

    GlfwWindow(int width, int height, String title, EngineLogger logger, NativeResourceRegistry nativeResources, WindowSizeListener sizeListener, OpenGlDebugMode openGlDebugMode,
        GlfwNativeBackend backend) {
        if (width <= 0) {
            throw new IllegalArgumentException("width must be positive");
        }
        if (height <= 0) {
            throw new IllegalArgumentException("height must be positive");
        }
        String suppliedTitle = Objects.requireNonNull(title, "title");
        if (suppliedTitle.isBlank()) {
            throw new IllegalArgumentException("title must not be blank");
        }
        this.width = width;
        this.height = height;
        this.title = suppliedTitle;
        this.logger = Objects.requireNonNull(logger, "logger");
        this.nativeResources = Objects.requireNonNull(nativeResources, "nativeResources");
        WindowSizeListener suppliedSizeListener = Objects.requireNonNull(sizeListener, "sizeListener");
        this.openGlDebugMode = Objects.requireNonNull(openGlDebugMode, "openGlDebugMode");
        this.backend = Objects.requireNonNull(backend, "backend");
        this.sizeDelivery = new GlfwDeferredSizeDelivery(suppliedSizeListener);
        this.windowModeController = new GlfwWindowModeController(this.backend);
        this.cursorCapture = new GlfwCursorCaptureController(this.backend, mouseMotion);
    }

    /** Returns the stable non-owning guard for production OpenGL thread affinity. */
    public OpenGlThreadGuard openGlThreadGuard() {
        return openGlThreadGuard;
    }

    /** Presents the current OpenGL back buffer for this started window. */
    public void present() {
        if (!eventPollingEnabled) {
            throw new IllegalStateException("GLFW presentation requires a started window");
        }
        requireOwnerThread();
        backend.swapBuffers(windowHandle);
    }

    public void pollEvents() {
        if (!eventPollingEnabled) {
            throw new IllegalStateException("GLFW event polling requires a started window");
        }
        requireOwnerThread();
        try {
            backend.pollEvents();
        } catch (RuntimeException | Error failure) {
            Throwable debugFailure = takePendingOpenGlDebugFailure();
            if (debugFailure != null) {
                addSuppressedUnlessSame(failure, debugFailure);
            }
            Throwable inputFailure = takePendingInputFailure();
            if (inputFailure != null) {
                addSuppressedUnlessSame(failure, inputFailure);
            }
            throw failure;
        }
        throwPendingOpenGlDebugFailure();
        throwPendingInputFailure();
        sizeDelivery.dispatchPending();
    }

    /** Captures and consumes the pending per-frame hardware edges and mouse motion. */
    public InputSnapshot captureInputSnapshot(long frameId) {
        if (!eventPollingEnabled) {
            throw new IllegalStateException("GLFW input snapshots require a started window");
        }
        requireOwnerThread();

        InputSnapshot snapshot = inputState.captureSnapshot(frameId, cursorCapture.effectivelyCaptured(), mouseMotion.accumulatedDeltaX(), mouseMotion.accumulatedDeltaY());
        mouseMotion.clearAccumulatedDelta();
        return snapshot;
    }

    /** Requests or releases gameplay cursor capture for this started window. */
    public void setCursorCaptured(boolean captured) {
        if (!eventPollingEnabled) {
            throw new IllegalStateException("GLFW cursor capture requires a started window");
        }
        requireOwnerThread();
        cursorCapture.setCaptured(windowHandle, inputState.focused(), captured);
    }

    public void setWindowMode(WindowMode mode) {
        WindowMode requestedMode = Objects.requireNonNull(mode, "mode");
        if (!eventPollingEnabled) {
            throw new IllegalStateException("GLFW window mode changes require a started window");
        }
        requireOwnerThread();
        windowModeController.setMode(windowHandle, requestedMode);
    }

    @Override
    protected void onInitialize() {
        openGlThreadGuard.bindOwnerThread(Thread.currentThread());
        try {
            callbackState = backend.installErrorCallback();
            if (!backend.initGlfw()) {
                throw new IllegalStateException("Failed to initialize GLFW");
            }
            glfwInitialized = true;

            backend.defaultWindowHints();
            backend.windowHint(GLFW.GLFW_CLIENT_API, GLFW.GLFW_OPENGL_API);
            backend.windowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 4);
            backend.windowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 6);
            backend.windowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
            backend.windowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GLFW.GLFW_TRUE);
            backend.windowHint(GLFW.GLFW_SRGB_CAPABLE, GLFW.GLFW_TRUE);
            if (openGlDebugMode == OpenGlDebugMode.FAIL_ON_HIGH_SEVERITY) {
                backend.windowHint(GLFW.GLFW_OPENGL_DEBUG_CONTEXT, GLFW.GLFW_TRUE);
            }
            backend.windowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
            backend.windowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE);

            windowHandle = backend.createWindow(width, height, title);
            if (windowHandle == 0L) {
                throw new IllegalStateException("Failed to create GLFW window with an OpenGL 4.6 Core context");
            }

            long registeredHandle = windowHandle;
            try {
                windowRegistration = nativeResources.register(RESOURCE_TYPE, registeredHandle, () -> backend.destroyWindow(registeredHandle));
            } catch (RuntimeException | Error failure) {
                windowHandle = 0L;
                try {
                    backend.destroyWindow(registeredHandle);
                } catch (RuntimeException | Error cleanupFailure) {
                    addSuppressedUnlessSame(failure, cleanupFailure);
                }
                throw failure;
            }
        } catch (RuntimeException | Error failure) {
            rollbackInitialization(failure);
            throw failure;
        }
    }

    @Override
    protected void onStart() {
        requireOwnerThread();
        try {
            backend.makeContextCurrent(windowHandle);
            contextCurrent = true;
            backend.createCapabilities();
            capabilitiesCreated = true;

            if (!backend.openGl46Supported()) {
                throw new IllegalStateException("Created OpenGL context does not support OpenGL 4.6");
            }

            String version = requireGlString("GL_VERSION", backend.glVersion());
            String renderer = requireGlString("GL_RENDERER", backend.glRenderer());
            logger.log(EngineLogger.Level.INFO, "OpenGL version: " + version, LOG_CONTEXT);
            logger.log(EngineLogger.Level.INFO, "OpenGL renderer: " + renderer, LOG_CONTEXT);

            if (openGlDebugMode == OpenGlDebugMode.FAIL_ON_HIGH_SEVERITY) {
                if (!backend.openGlDebugContext()) {
                    throw new IllegalStateException("Requested OpenGL debug context flag is unavailable");
                }
                debugCallbackState = backend.installOpenGlDebugCallback(this::handleOpenGlDebugMessage);
            }

            sizeCallbackState = backend.installSizeCallbacks(windowHandle, new GlfwSizeEventSink() {
                @Override
                public void onLogicalSize(int logicalWidth, int logicalHeight) {
                    sizeDelivery.stageLogical(logicalWidth, logicalHeight);
                }

                @Override
                public void onFramebufferSize(int framebufferWidth, int framebufferHeight) {
                    sizeDelivery.stageFramebuffer(framebufferWidth, framebufferHeight);
                }
            });

            inputCallbackState = backend.installInputCallbacks(windowHandle, new GlfwInputEventSink() {
                @Override
                public void onFocus(boolean focused) {
                    handleFocusChanged(focused);
                }

                @Override
                public void onKey(int key, int action) {
                    inputState.onKeyChanged(key, action);
                }

                @Override
                public void onMouseButton(int button, int action) {
                    inputState.onMouseButtonChanged(button, action);
                }
            });
            motionCallbackState = backend.installCursorPositionCallback(windowHandle,
                (x, y) -> mouseMotion.onCursorPosition(inputState.focused(), cursorCapture.effectivelyCaptured(), x, y));
            inputState.onFocusChanged(backend.queryWindowFocused(windowHandle));

            GlfwDimensions logicalSize = backend.queryLogicalSize(windowHandle);
            GlfwDeferredSizeDelivery.validateDimensions("logical window", logicalSize);
            sizeDelivery.stageLogical(logicalSize.width(), logicalSize.height());

            GlfwDimensions framebufferSize = backend.queryFramebufferSize(windowHandle);
            GlfwDeferredSizeDelivery.validateDimensions("framebuffer", framebufferSize);
            sizeDelivery.stageFramebuffer(framebufferSize.width(), framebufferSize.height());

            windowModeController.reset();
            pendingOpenGlDebugFailure = null;
            inputState.clearForLifecycle();
            cursorCapture.reset();
            pendingInputFailure = null;
            eventPollingEnabled = true;
            backend.showWindow(windowHandle);
        } catch (RuntimeException | Error failure) {
            cleanupStartedContext(failure);
            throw failure;
        }
    }

    @Override
    protected void onStop() {
        requireOwnerThread();
        eventPollingEnabled = false;
        sizeDelivery.clear();
        windowModeController.clearRestoreGeometry();
        inputState.clearForLifecycle();
        mouseMotion.reset();
        pendingInputFailure = null;
        pendingOpenGlDebugFailure = null;

        List<Throwable> failures = new ArrayList<>();
        cursorCapture.releaseForCleanup(windowHandle, failures);
        releaseMotionCallback(failures);
        releaseInputCallbacks(failures);
        releaseSizeCallbacks(failures);
        releaseOpenGlDebugCallback(failures);
        runCleanup(failures, () -> backend.hideWindow(windowHandle));
        if (contextCurrent && runCleanup(failures, () -> backend.makeContextCurrent(0L))) {
            contextCurrent = false;
        }
        if (capabilitiesCreated && runCleanup(failures, backend::clearCapabilities)) {
            capabilitiesCreated = false;
        }
        throwCleanupFailure(failures);
    }

    @Override
    protected void onClose() {
        if (!hasOwnedNativeState()) {
            return;
        }
        requireOwnerThread();
        eventPollingEnabled = false;
        sizeDelivery.clear();
        windowModeController.clearRestoreGeometry();
        inputState.clearForLifecycle();
        mouseMotion.reset();
        pendingInputFailure = null;
        pendingOpenGlDebugFailure = null;

        List<Throwable> failures = new ArrayList<>();
        cursorCapture.releaseForCleanup(windowHandle, failures);
        releaseMotionCallback(failures);
        releaseInputCallbacks(failures);
        releaseSizeCallbacks(failures);
        releaseOpenGlDebugCallback(failures);
        if (contextCurrent && runCleanup(failures, () -> backend.makeContextCurrent(0L))) {
            contextCurrent = false;
        }
        if (capabilitiesCreated && runCleanup(failures, backend::clearCapabilities)) {
            capabilitiesCreated = false;
        }
        if (windowRegistration != null) {
            NativeResourceRegistry.Registration registration = windowRegistration;
            if (runCleanup(failures, registration::close)) {
                windowRegistration = null;
                windowHandle = 0L;
            }
        } else if (windowHandle != 0L) {
            long orphanedHandle = windowHandle;
            windowHandle = 0L;
            runCleanup(failures, () -> backend.destroyWindow(orphanedHandle));
        }
        if (glfwInitialized) {
            runCleanup(failures, backend::terminateGlfw);
            glfwInitialized = false;
        }
        releaseCallback(failures);
        throwCleanupFailure(failures);
    }

    boolean isKeyHeldForTest(int key) {
        return inputState.isKeyHeld(key);
    }

    boolean isMouseButtonHeldForTest(int button) {
        return inputState.isMouseButtonHeld(button);
    }

    boolean isFocusedForTest() {
        return inputState.focused();
    }

    boolean isCursorEffectivelyCapturedForTest() {
        return cursorCapture.effectivelyCaptured();
    }

    boolean isRawMouseMotionEnabledForTest() {
        return cursorCapture.rawMouseMotionEnabled();
    }

    boolean isRawMouseMotionSupportedForTest() {
        return cursorCapture.rawMouseMotionSupported();
    }

    GlfwMouseMotionTracker.MouseDelta drainMouseMotionForTest() {
        return mouseMotion.drainForTest();
    }

    private void handleFocusChanged(boolean focused) {
        inputState.onFocusChanged(focused);
        if (!focused) {
            cursorCapture.onFocusLost(windowHandle, this::stageInputFailure);
        }
    }

    private void handleOpenGlDebugMessage(int source, int type, int id, int severity, String message) {
        String diagnostic = "OpenGL debug [source=" + debugSourceName(source) + ", type=" + debugTypeName(type) + ", severity=" + debugSeverityName(severity) + ", id=" + id + "]: "
            + message;
        if (severity == GL43.GL_DEBUG_SEVERITY_HIGH) {
            IllegalStateException highSeverityFailure = new IllegalStateException(diagnostic);
            try {
                logger.log(EngineLogger.Level.ERROR, diagnostic, LOG_CONTEXT);
            } catch (RuntimeException | Error loggingFailure) {
                addSuppressedUnlessSame(highSeverityFailure, loggingFailure);
            }
            stageOpenGlDebugFailure(highSeverityFailure);
            return;
        }

        EngineLogger.Level level = severity == GL43.GL_DEBUG_SEVERITY_MEDIUM ? EngineLogger.Level.WARN : EngineLogger.Level.DEBUG;
        try {
            logger.log(level, diagnostic, LOG_CONTEXT);
        } catch (RuntimeException | Error loggingFailure) {
            stageOpenGlDebugFailure(loggingFailure);
        }
    }

    private void stageOpenGlDebugFailure(Throwable failure) {
        if (pendingOpenGlDebugFailure == null) {
            pendingOpenGlDebugFailure = failure;
        } else {
            addSuppressedUnlessSame(pendingOpenGlDebugFailure, failure);
        }
    }

    private void throwPendingOpenGlDebugFailure() {
        Throwable failure = takePendingOpenGlDebugFailure();
        if (failure == null) {
            return;
        }
        if (failure instanceof RuntimeException runtimeFailure) {
            throw runtimeFailure;
        }
        throw (Error) failure;
    }

    private Throwable takePendingOpenGlDebugFailure() {
        Throwable failure = pendingOpenGlDebugFailure;
        pendingOpenGlDebugFailure = null;
        return failure;
    }

    private static String debugSourceName(int source) {
        return switch (source) {
            case GL43.GL_DEBUG_SOURCE_API -> "API";
            case GL43.GL_DEBUG_SOURCE_WINDOW_SYSTEM -> "WINDOW_SYSTEM";
            case GL43.GL_DEBUG_SOURCE_SHADER_COMPILER -> "SHADER_COMPILER";
            case GL43.GL_DEBUG_SOURCE_THIRD_PARTY -> "THIRD_PARTY";
            case GL43.GL_DEBUG_SOURCE_APPLICATION -> "APPLICATION";
            case GL43.GL_DEBUG_SOURCE_OTHER -> "OTHER";
            default -> "UNKNOWN(" + source + ")";
        };
    }

    private static String debugTypeName(int type) {
        return switch (type) {
            case GL43.GL_DEBUG_TYPE_ERROR -> "ERROR";
            case GL43.GL_DEBUG_TYPE_DEPRECATED_BEHAVIOR -> "DEPRECATED_BEHAVIOR";
            case GL43.GL_DEBUG_TYPE_UNDEFINED_BEHAVIOR -> "UNDEFINED_BEHAVIOR";
            case GL43.GL_DEBUG_TYPE_PORTABILITY -> "PORTABILITY";
            case GL43.GL_DEBUG_TYPE_PERFORMANCE -> "PERFORMANCE";
            case GL43.GL_DEBUG_TYPE_MARKER -> "MARKER";
            case GL43.GL_DEBUG_TYPE_PUSH_GROUP -> "PUSH_GROUP";
            case GL43.GL_DEBUG_TYPE_POP_GROUP -> "POP_GROUP";
            case GL43.GL_DEBUG_TYPE_OTHER -> "OTHER";
            default -> "UNKNOWN(" + type + ")";
        };
    }

    private static String debugSeverityName(int severity) {
        return switch (severity) {
            case GL43.GL_DEBUG_SEVERITY_HIGH -> "HIGH";
            case GL43.GL_DEBUG_SEVERITY_MEDIUM -> "MEDIUM";
            case GL43.GL_DEBUG_SEVERITY_LOW -> "LOW";
            case GL43.GL_DEBUG_SEVERITY_NOTIFICATION -> "NOTIFICATION";
            default -> "UNKNOWN(" + severity + ")";
        };
    }

    private void stageInputFailure(Throwable failure) {
        if (pendingInputFailure == null) {
            pendingInputFailure = failure;
        } else {
            addSuppressedUnlessSame(pendingInputFailure, failure);
        }
    }

    private void throwPendingInputFailure() {
        Throwable failure = takePendingInputFailure();
        if (failure == null) {
            return;
        }
        if (failure instanceof RuntimeException runtimeFailure) {
            throw runtimeFailure;
        }
        throw (Error) failure;
    }

    private Throwable takePendingInputFailure() {
        Throwable failure = pendingInputFailure;
        pendingInputFailure = null;
        return failure;
    }

    private void rollbackInitialization(Throwable primary) {
        List<Throwable> failures = new ArrayList<>();
        if (windowRegistration != null) {
            NativeResourceRegistry.Registration registration = windowRegistration;
            if (runCleanup(failures, registration::close)) {
                windowRegistration = null;
                windowHandle = 0L;
            }
        } else if (windowHandle != 0L) {
            long orphanedHandle = windowHandle;
            windowHandle = 0L;
            runCleanup(failures, () -> backend.destroyWindow(orphanedHandle));
        }
        if (glfwInitialized) {
            runCleanup(failures, backend::terminateGlfw);
            glfwInitialized = false;
        }
        releaseCallback(failures);
        for (Throwable failure : failures) {
            addSuppressedUnlessSame(primary, failure);
        }
    }

    private void cleanupStartedContext(Throwable primary) {
        eventPollingEnabled = false;
        sizeDelivery.clear();
        windowModeController.clearRestoreGeometry();
        inputState.clearForLifecycle();
        mouseMotion.reset();
        pendingInputFailure = null;
        pendingOpenGlDebugFailure = null;
        List<Throwable> failures = new ArrayList<>();
        cursorCapture.releaseForCleanup(windowHandle, failures);
        releaseMotionCallback(failures);
        releaseInputCallbacks(failures);
        releaseSizeCallbacks(failures);
        releaseOpenGlDebugCallback(failures);
        if (contextCurrent && runCleanup(failures, () -> backend.makeContextCurrent(0L))) {
            contextCurrent = false;
        }
        if (capabilitiesCreated && runCleanup(failures, backend::clearCapabilities)) {
            capabilitiesCreated = false;
        }
        for (Throwable failure : failures) {
            addSuppressedUnlessSame(primary, failure);
        }
    }

    private void releaseOpenGlDebugCallback(List<Throwable> failures) {
        if (debugCallbackState == null) {
            return;
        }
        OpenGlDebugCallbackRegistration state = debugCallbackState;
        debugCallbackState = null;
        runCleanup(failures, () -> backend.releaseOpenGlDebugCallback(state));
    }

    private void releaseMotionCallback(List<Throwable> failures) {
        if (motionCallbackState == null) {
            return;
        }
        GlfwCursorPositionCallbackRegistration state = motionCallbackState;
        motionCallbackState = null;
        runCleanup(failures, () -> backend.releaseCursorPositionCallback(windowHandle, state));
    }

    private void releaseInputCallbacks(List<Throwable> failures) {
        if (inputCallbackState == null) {
            return;
        }
        GlfwInputCallbackRegistration state = inputCallbackState;
        inputCallbackState = null;
        runCleanup(failures, () -> backend.releaseInputCallbacks(windowHandle, state));
    }

    private void releaseSizeCallbacks(List<Throwable> failures) {
        if (sizeCallbackState == null) {
            return;
        }
        GlfwSizeCallbackRegistration state = sizeCallbackState;
        sizeCallbackState = null;
        runCleanup(failures, () -> backend.releaseSizeCallbacks(windowHandle, state));
    }

    private void releaseCallback(List<Throwable> failures) {
        if (callbackState == null) {
            return;
        }
        GlfwErrorCallbackRegistration state = callbackState;
        callbackState = null;
        runCleanup(failures, () -> backend.restoreErrorCallback(state));
        runCleanup(failures, () -> backend.freeOwnedErrorCallback(state));
    }

    private boolean hasOwnedNativeState() {
        return callbackState != null || debugCallbackState != null || sizeCallbackState != null || inputCallbackState != null || motionCallbackState != null || glfwInitialized
            || windowHandle != 0L || windowRegistration != null || contextCurrent || capabilitiesCreated;
    }

    private void requireOwnerThread() {
        openGlThreadGuard.assertOwnerThread();
    }

    private static String requireGlString(String name, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " is unavailable for the current OpenGL context");
        }
        return value;
    }

    private static boolean runCleanup(List<Throwable> failures, Runnable cleanup) {
        try {
            cleanup.run();
            return true;
        } catch (RuntimeException | Error failure) {
            failures.add(failure);
            return false;
        }
    }

    private static void throwCleanupFailure(List<Throwable> failures) {
        if (failures.isEmpty()) {
            return;
        }
        Throwable primary = failures.getFirst();
        for (int index = 1; index < failures.size(); index++) {
            addSuppressedUnlessSame(primary, failures.get(index));
        }
        if (primary instanceof RuntimeException runtimeFailure) {
            throw runtimeFailure;
        }
        throw (Error) primary;
    }

    private static void addSuppressedUnlessSame(Throwable primary, Throwable suppressed) {
        if (primary != suppressed) {
            primary.addSuppressed(suppressed);
        }
    }

}
