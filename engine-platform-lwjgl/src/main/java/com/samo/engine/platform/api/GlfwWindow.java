package com.samo.engine.platform.api;

import com.samo.engine.core.api.EngineLogger;
import com.samo.engine.core.api.EngineSubsystem;
import com.samo.engine.core.api.NativeResourceRegistry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL43;
import org.lwjgl.system.MemoryUtil;

/** Owns one GLFW window and OpenGL 4.6 Core context for one subsystem lifetime. */
public final class GlfwWindow extends EngineSubsystem {
    private static final String RESOURCE_TYPE = "GLFW window";
    private static final EngineLogger.Context LOG_CONTEXT =
            new EngineLogger.Context(null, null, "platform", null, null);
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
    private final WindowSizeListener sizeListener;
    private final OpenGlDebugMode openGlDebugMode;
    private final GlfwNativeBackend backend;
    private final boolean[] heldKeys = new boolean[GLFW.GLFW_KEY_LAST + 1];
    private final boolean[] heldMouseButtons = new boolean[GLFW.GLFW_MOUSE_BUTTON_LAST + 1];
    private final boolean[] pendingPressedKeys = new boolean[InputKey.values().length];
    private final boolean[] pendingReleasedKeys = new boolean[InputKey.values().length];
    private final boolean[] pendingPressedMouseButtons = new boolean[InputMouseButton.values().length];
    private final boolean[] pendingReleasedMouseButtons = new boolean[InputMouseButton.values().length];

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
    private boolean logicalSizePending;
    private int pendingLogicalWidth;
    private int pendingLogicalHeight;
    private boolean framebufferSizePending;
    private int pendingFramebufferWidth;
    private int pendingFramebufferHeight;
    private WindowMode windowMode = WindowMode.WINDOWED;
    private WindowGeometry windowedRestoreGeometry;
    private boolean windowFocused;
    private boolean cursorCaptureRequested;
    private boolean cursorCaptureEffective;
    private boolean cursorCaptureNeedsExplicitRearm;
    private boolean cursorNormalizationPending;
    private boolean rawMouseMotionEnabled;
    private boolean mouseMotionBaselineValid;
    private double previousMouseX;
    private double previousMouseY;
    private double accumulatedMouseDeltaX;
    private double accumulatedMouseDeltaY;
    private Throwable pendingInputFailure;
    private Throwable pendingOpenGlDebugFailure;

    public GlfwWindow(
            int width,
            int height,
            String title,
            EngineLogger logger,
            NativeResourceRegistry nativeResources) {
        this(width, height, title, logger, nativeResources, NO_OP_SIZE_LISTENER, OpenGlDebugMode.DISABLED);
    }

    public GlfwWindow(
            int width,
            int height,
            String title,
            EngineLogger logger,
            NativeResourceRegistry nativeResources,
            OpenGlDebugMode openGlDebugMode) {
        this(width, height, title, logger, nativeResources, NO_OP_SIZE_LISTENER, openGlDebugMode);
    }

    public GlfwWindow(
            int width,
            int height,
            String title,
            EngineLogger logger,
            NativeResourceRegistry nativeResources,
            WindowSizeListener sizeListener) {
        this(width, height, title, logger, nativeResources, sizeListener, OpenGlDebugMode.DISABLED);
    }

    public GlfwWindow(
            int width,
            int height,
            String title,
            EngineLogger logger,
            NativeResourceRegistry nativeResources,
            WindowSizeListener sizeListener,
            OpenGlDebugMode openGlDebugMode) {
        this(width, height, title, logger, nativeResources, sizeListener, openGlDebugMode, new LwjglGlfwNativeBackend());
    }

    GlfwWindow(
            int width,
            int height,
            String title,
            EngineLogger logger,
            NativeResourceRegistry nativeResources,
            GlfwNativeBackend backend) {
        this(width, height, title, logger, nativeResources, NO_OP_SIZE_LISTENER, OpenGlDebugMode.DISABLED, backend);
    }

    GlfwWindow(
            int width,
            int height,
            String title,
            EngineLogger logger,
            NativeResourceRegistry nativeResources,
            OpenGlDebugMode openGlDebugMode,
            GlfwNativeBackend backend) {
        this(width, height, title, logger, nativeResources, NO_OP_SIZE_LISTENER, openGlDebugMode, backend);
    }

    GlfwWindow(
            int width,
            int height,
            String title,
            EngineLogger logger,
            NativeResourceRegistry nativeResources,
            WindowSizeListener sizeListener,
            GlfwNativeBackend backend) {
        this(width, height, title, logger, nativeResources, sizeListener, OpenGlDebugMode.DISABLED, backend);
    }

    GlfwWindow(
            int width,
            int height,
            String title,
            EngineLogger logger,
            NativeResourceRegistry nativeResources,
            WindowSizeListener sizeListener,
            OpenGlDebugMode openGlDebugMode,
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
        this.sizeListener = Objects.requireNonNull(sizeListener, "sizeListener");
        this.openGlDebugMode = Objects.requireNonNull(openGlDebugMode, "openGlDebugMode");
        this.backend = Objects.requireNonNull(backend, "backend");
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
        dispatchPendingSizes();
    }

    /** Captures and consumes the pending per-frame hardware edges and mouse motion. */
    public InputSnapshot captureInputSnapshot(long frameId) {
        if (!eventPollingEnabled) {
            throw new IllegalStateException("GLFW input snapshots require a started window");
        }
        requireOwnerThread();
        if (frameId < 0L) {
            throw new IllegalArgumentException("frameId must be non-negative");
        }

        EnumSet<InputKey> heldKeySet = EnumSet.noneOf(InputKey.class);
        EnumSet<InputKey> pressedKeySet = EnumSet.noneOf(InputKey.class);
        EnumSet<InputKey> releasedKeySet = EnumSet.noneOf(InputKey.class);
        for (InputKey key : InputKey.values()) {
            if (heldKeys[glfwKey(key)]) {
                heldKeySet.add(key);
            }
            if (pendingPressedKeys[key.ordinal()]) {
                pressedKeySet.add(key);
            }
            if (pendingReleasedKeys[key.ordinal()]) {
                releasedKeySet.add(key);
            }
        }

        EnumSet<InputMouseButton> heldButtonSet = EnumSet.noneOf(InputMouseButton.class);
        EnumSet<InputMouseButton> pressedButtonSet = EnumSet.noneOf(InputMouseButton.class);
        EnumSet<InputMouseButton> releasedButtonSet = EnumSet.noneOf(InputMouseButton.class);
        for (InputMouseButton button : InputMouseButton.values()) {
            if (heldMouseButtons[glfwMouseButton(button)]) {
                heldButtonSet.add(button);
            }
            if (pendingPressedMouseButtons[button.ordinal()]) {
                pressedButtonSet.add(button);
            }
            if (pendingReleasedMouseButtons[button.ordinal()]) {
                releasedButtonSet.add(button);
            }
        }

        InputSnapshot snapshot = new InputSnapshot(
                frameId,
                windowFocused,
                cursorCaptureEffective,
                heldKeySet,
                pressedKeySet,
                releasedKeySet,
                heldButtonSet,
                pressedButtonSet,
                releasedButtonSet,
                accumulatedMouseDeltaX,
                accumulatedMouseDeltaY);
        clearPendingInputEdges();
        clearAccumulatedMouseDelta();
        return snapshot;
    }

    /** Requests or releases gameplay cursor capture for this started window. */
    public void setCursorCaptured(boolean captured) {
        if (!eventPollingEnabled) {
            throw new IllegalStateException("GLFW cursor capture requires a started window");
        }
        requireOwnerThread();

        if (captured) {
            if (!windowFocused) {
                clearMouseMotionState();
                cursorCaptureRequested = true;
                cursorCaptureEffective = false;
                cursorCaptureNeedsExplicitRearm = true;
                return;
            }
            if (cursorCaptureRequested && cursorCaptureEffective && !cursorCaptureNeedsExplicitRearm) {
                return;
            }
            captureCursorWithRawMotion();
            return;
        }

        if (!cursorCaptureRequested
                && !cursorCaptureEffective
                && !cursorNormalizationPending
                && !rawMouseMotionEnabled) {
            clearMouseMotionState();
            cursorCaptureNeedsExplicitRearm = false;
            return;
        }

        releaseCursorDirect();
    }

    public void setWindowMode(WindowMode mode) {
        WindowMode requestedMode = Objects.requireNonNull(mode, "mode");
        if (!eventPollingEnabled) {
            throw new IllegalStateException("GLFW window mode changes require a started window");
        }
        requireOwnerThread();
        if (requestedMode == windowMode) {
            return;
        }

        WindowMode previousMode = windowMode;
        WindowGeometry previousRestoreGeometry = windowedRestoreGeometry;
        WindowGeometry candidateRestoreGeometry = previousRestoreGeometry;
        if (previousMode == WindowMode.WINDOWED) {
            candidateRestoreGeometry = captureWindowedGeometry();
        }

        TransitionPlan requestedPlan = planTransition(requestedMode, candidateRestoreGeometry);
        try {
            applyTransition(requestedPlan);
            windowMode = requestedMode;
            if (requestedMode == WindowMode.WINDOWED) {
                windowedRestoreGeometry = null;
            } else if (previousMode == WindowMode.WINDOWED) {
                windowedRestoreGeometry = candidateRestoreGeometry;
            }
        } catch (RuntimeException | Error failure) {
            try {
                applyTransition(planTransition(previousMode, candidateRestoreGeometry));
            } catch (RuntimeException | Error rollbackFailure) {
                addSuppressedUnlessSame(failure, rollbackFailure);
            }
            windowMode = previousMode;
            windowedRestoreGeometry = previousRestoreGeometry;
            throw failure;
        }
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
                windowRegistration = nativeResources.register(
                        RESOURCE_TYPE,
                        registeredHandle,
                        () -> backend.destroyWindow(registeredHandle));
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
                    stageLogicalSize(logicalWidth, logicalHeight);
                }

                @Override
                public void onFramebufferSize(int framebufferWidth, int framebufferHeight) {
                    stageFramebufferSize(framebufferWidth, framebufferHeight);
                }
            });

            inputCallbackState = backend.installInputCallbacks(windowHandle, new GlfwInputEventSink() {
                @Override
                public void onFocus(boolean focused) {
                    handleFocusChanged(focused);
                }

                @Override
                public void onKey(int key, int action) {
                    handleKeyChanged(key, action);
                }

                @Override
                public void onMouseButton(int button, int action) {
                    handleMouseButtonChanged(button, action);
                }
            });
            motionCallbackState = backend.installCursorPositionCallback(
                    windowHandle,
                    this::handleCursorPositionChanged);
            windowFocused = backend.queryWindowFocused(windowHandle);

            Dimensions logicalSize = backend.queryLogicalSize(windowHandle);
            validatePlatformDimensions("logical window", logicalSize);
            stageLogicalSize(logicalSize.width(), logicalSize.height());

            Dimensions framebufferSize = backend.queryFramebufferSize(windowHandle);
            validatePlatformDimensions("framebuffer", framebufferSize);
            stageFramebufferSize(framebufferSize.width(), framebufferSize.height());

            windowMode = WindowMode.WINDOWED;
            windowedRestoreGeometry = null;
            pendingOpenGlDebugFailure = null;
            resetInputState();
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
        clearPendingSizes();
        windowedRestoreGeometry = null;
        clearHeldInput();
        clearPendingInputEdges();
        clearMouseMotionState();
        pendingInputFailure = null;
        pendingOpenGlDebugFailure = null;

        List<Throwable> failures = new ArrayList<>();
        releaseCursorForCleanup(failures);
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
        clearPendingSizes();
        windowedRestoreGeometry = null;
        clearHeldInput();
        clearPendingInputEdges();
        clearMouseMotionState();
        pendingInputFailure = null;
        pendingOpenGlDebugFailure = null;

        List<Throwable> failures = new ArrayList<>();
        releaseCursorForCleanup(failures);
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
        return key >= 0 && key < heldKeys.length && heldKeys[key];
    }

    boolean isMouseButtonHeldForTest(int button) {
        return button >= 0 && button < heldMouseButtons.length && heldMouseButtons[button];
    }

    boolean isFocusedForTest() {
        return windowFocused;
    }

    boolean isCursorEffectivelyCapturedForTest() {
        return cursorCaptureEffective;
    }

    boolean isRawMouseMotionEnabledForTest() {
        return rawMouseMotionEnabled;
    }

    boolean isRawMouseMotionSupportedForTest() {
        return backend.rawMouseMotionSupported();
    }

    MouseMotion drainMouseMotionForTest() {
        MouseMotion motion = new MouseMotion(accumulatedMouseDeltaX, accumulatedMouseDeltaY);
        clearAccumulatedMouseDelta();
        return motion;
    }

    private void captureCursorWithRawMotion() {
        boolean previousRequested = cursorCaptureRequested;
        boolean previousEffective = cursorCaptureEffective;
        boolean previousRearm = cursorCaptureNeedsExplicitRearm;
        boolean previousNormalizationPending = cursorNormalizationPending;
        boolean previousRaw = rawMouseMotionEnabled;
        boolean cursorDisabled = false;
        boolean rawEnableAttempted = false;
        boolean rawEnabledThisAttempt = false;

        try {
            backend.setCursorMode(windowHandle, GLFW.GLFW_CURSOR_DISABLED);
            cursorDisabled = true;
            cursorNormalizationPending = true;
            if (backend.rawMouseMotionSupported()) {
                rawEnableAttempted = true;
                backend.setRawMouseMotion(windowHandle, true);
                rawEnabledThisAttempt = true;
            }
            clearMouseMotionState();
            cursorCaptureRequested = true;
            cursorCaptureEffective = true;
            cursorCaptureNeedsExplicitRearm = false;
            rawMouseMotionEnabled = rawEnabledThisAttempt;
        } catch (RuntimeException | Error failure) {
            clearMouseMotionState();
            if (rawEnableAttempted) {
                try {
                    backend.setRawMouseMotion(windowHandle, false);
                } catch (RuntimeException | Error rollbackFailure) {
                    addSuppressedUnlessSame(failure, rollbackFailure);
                }
            }
            if (cursorDisabled) {
                try {
                    backend.setCursorMode(windowHandle, GLFW.GLFW_CURSOR_NORMAL);
                    cursorNormalizationPending = false;
                } catch (RuntimeException | Error rollbackFailure) {
                    addSuppressedUnlessSame(failure, rollbackFailure);
                }
            } else {
                cursorNormalizationPending = previousNormalizationPending;
            }
            cursorCaptureRequested = previousRequested;
            cursorCaptureEffective = previousEffective;
            cursorCaptureNeedsExplicitRearm = previousRearm;
            rawMouseMotionEnabled = previousRaw;
            throw failure;
        }
    }

    private void releaseCursorDirect() {
        clearMouseMotionState();
        cursorCaptureRequested = false;
        cursorCaptureEffective = false;
        cursorCaptureNeedsExplicitRearm = false;

        List<Throwable> failures = new ArrayList<>();
        if (rawMouseMotionEnabled
                && runCleanup(failures, () -> backend.setRawMouseMotion(windowHandle, false))) {
            rawMouseMotionEnabled = false;
        }
        if (cursorNormalizationPending
                && runCleanup(failures, () -> backend.setCursorMode(windowHandle, GLFW.GLFW_CURSOR_NORMAL))) {
            cursorNormalizationPending = false;
        }
        throwCleanupFailure(failures);
    }

    private void handleFocusChanged(boolean focused) {
        windowFocused = focused;
        if (focused) {
            return;
        }

        clearHeldInputForFocusLoss();
        clearMouseMotionState();
        if (cursorCaptureRequested) {
            cursorCaptureNeedsExplicitRearm = true;
        }
        if (!cursorCaptureEffective && !rawMouseMotionEnabled && !cursorNormalizationPending) {
            return;
        }

        cursorCaptureEffective = false;
        if (rawMouseMotionEnabled) {
            try {
                backend.setRawMouseMotion(windowHandle, false);
                rawMouseMotionEnabled = false;
            } catch (RuntimeException | Error failure) {
                stageInputFailure(failure);
            }
        }
        if (cursorNormalizationPending) {
            try {
                backend.setCursorMode(windowHandle, GLFW.GLFW_CURSOR_NORMAL);
                cursorNormalizationPending = false;
            } catch (RuntimeException | Error failure) {
                stageInputFailure(failure);
            }
        }
    }

    private void handleKeyChanged(int key, int action) {
        if (!windowFocused || key < 0 || key >= heldKeys.length) {
            return;
        }
        InputKey inputKey = inputKey(key);
        if (action == GLFW.GLFW_PRESS) {
            heldKeys[key] = true;
            if (inputKey != null) {
                pendingPressedKeys[inputKey.ordinal()] = true;
            }
        } else if (action == GLFW.GLFW_REPEAT) {
            heldKeys[key] = true;
        } else if (action == GLFW.GLFW_RELEASE) {
            heldKeys[key] = false;
            if (inputKey != null) {
                pendingReleasedKeys[inputKey.ordinal()] = true;
            }
        }
    }

    private void handleMouseButtonChanged(int button, int action) {
        if (!windowFocused || button < 0 || button >= heldMouseButtons.length) {
            return;
        }
        InputMouseButton inputButton = inputMouseButton(button);
        if (action == GLFW.GLFW_PRESS) {
            heldMouseButtons[button] = true;
            if (inputButton != null) {
                pendingPressedMouseButtons[inputButton.ordinal()] = true;
            }
        } else if (action == GLFW.GLFW_RELEASE) {
            heldMouseButtons[button] = false;
            if (inputButton != null) {
                pendingReleasedMouseButtons[inputButton.ordinal()] = true;
            }
        }
    }

    private void handleCursorPositionChanged(double x, double y) {
        if (!windowFocused || !cursorCaptureEffective) {
            return;
        }
        if (!mouseMotionBaselineValid) {
            previousMouseX = x;
            previousMouseY = y;
            mouseMotionBaselineValid = true;
            return;
        }
        accumulatedMouseDeltaX += x - previousMouseX;
        accumulatedMouseDeltaY += y - previousMouseY;
        previousMouseX = x;
        previousMouseY = y;
    }

    private void handleOpenGlDebugMessage(int source, int type, int id, int severity, String message) {
        String diagnostic = "OpenGL debug [source=" + debugSourceName(source)
                + ", type=" + debugTypeName(type)
                + ", severity=" + debugSeverityName(severity)
                + ", id=" + id + "]: " + message;
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

        EngineLogger.Level level = severity == GL43.GL_DEBUG_SEVERITY_MEDIUM
                ? EngineLogger.Level.WARN
                : EngineLogger.Level.DEBUG;
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

    private void resetInputState() {
        clearHeldInput();
        clearPendingInputEdges();
        clearMouseMotionState();
        cursorCaptureRequested = false;
        cursorCaptureEffective = false;
        cursorCaptureNeedsExplicitRearm = false;
        cursorNormalizationPending = false;
        rawMouseMotionEnabled = false;
        pendingInputFailure = null;
    }

    private void clearHeldInput() {
        Arrays.fill(heldKeys, false);
        Arrays.fill(heldMouseButtons, false);
    }

    private void clearHeldInputForFocusLoss() {
        Arrays.fill(pendingPressedKeys, false);
        Arrays.fill(pendingPressedMouseButtons, false);
        for (InputKey key : InputKey.values()) {
            int nativeKey = glfwKey(key);
            if (heldKeys[nativeKey]) {
                pendingReleasedKeys[key.ordinal()] = true;
            }
        }
        for (InputMouseButton button : InputMouseButton.values()) {
            int nativeButton = glfwMouseButton(button);
            if (heldMouseButtons[nativeButton]) {
                pendingReleasedMouseButtons[button.ordinal()] = true;
            }
        }
        clearHeldInput();
    }

    private void clearPendingInputEdges() {
        Arrays.fill(pendingPressedKeys, false);
        Arrays.fill(pendingReleasedKeys, false);
        Arrays.fill(pendingPressedMouseButtons, false);
        Arrays.fill(pendingReleasedMouseButtons, false);
    }

    private void clearAccumulatedMouseDelta() {
        accumulatedMouseDeltaX = 0.0;
        accumulatedMouseDeltaY = 0.0;
    }

    private void clearMouseMotionState() {
        mouseMotionBaselineValid = false;
        previousMouseX = 0.0;
        previousMouseY = 0.0;
        clearAccumulatedMouseDelta();
    }

    private void releaseCursorForCleanup(List<Throwable> failures) {
        clearMouseMotionState();
        cursorCaptureEffective = false;
        if (rawMouseMotionEnabled) {
            if (runCleanup(failures, () -> backend.setRawMouseMotion(windowHandle, false))) {
                rawMouseMotionEnabled = false;
            }
        }
        if (cursorNormalizationPending) {
            if (runCleanup(failures, () -> backend.setCursorMode(windowHandle, GLFW.GLFW_CURSOR_NORMAL))) {
                cursorNormalizationPending = false;
            }
        }
        cursorCaptureRequested = false;
        cursorCaptureNeedsExplicitRearm = false;
    }

    private static InputKey inputKey(int glfwKey) {
        return switch (glfwKey) {
            case GLFW.GLFW_KEY_W -> InputKey.W;
            case GLFW.GLFW_KEY_A -> InputKey.A;
            case GLFW.GLFW_KEY_S -> InputKey.S;
            case GLFW.GLFW_KEY_D -> InputKey.D;
            case GLFW.GLFW_KEY_SPACE -> InputKey.SPACE;
            case GLFW.GLFW_KEY_LEFT_SHIFT -> InputKey.LEFT_SHIFT;
            case GLFW.GLFW_KEY_RIGHT_SHIFT -> InputKey.RIGHT_SHIFT;
            case GLFW.GLFW_KEY_LEFT_CONTROL -> InputKey.LEFT_CONTROL;
            case GLFW.GLFW_KEY_RIGHT_CONTROL -> InputKey.RIGHT_CONTROL;
            case GLFW.GLFW_KEY_LEFT_ALT -> InputKey.LEFT_ALT;
            case GLFW.GLFW_KEY_RIGHT_ALT -> InputKey.RIGHT_ALT;
            case GLFW.GLFW_KEY_ESCAPE -> InputKey.ESCAPE;
            case GLFW.GLFW_KEY_E -> InputKey.E;
            case GLFW.GLFW_KEY_Q -> InputKey.Q;
            case GLFW.GLFW_KEY_R -> InputKey.R;
            case GLFW.GLFW_KEY_F -> InputKey.F;
            default -> null;
        };
    }

    private static int glfwKey(InputKey key) {
        return switch (key) {
            case W -> GLFW.GLFW_KEY_W;
            case A -> GLFW.GLFW_KEY_A;
            case S -> GLFW.GLFW_KEY_S;
            case D -> GLFW.GLFW_KEY_D;
            case SPACE -> GLFW.GLFW_KEY_SPACE;
            case LEFT_SHIFT -> GLFW.GLFW_KEY_LEFT_SHIFT;
            case RIGHT_SHIFT -> GLFW.GLFW_KEY_RIGHT_SHIFT;
            case LEFT_CONTROL -> GLFW.GLFW_KEY_LEFT_CONTROL;
            case RIGHT_CONTROL -> GLFW.GLFW_KEY_RIGHT_CONTROL;
            case LEFT_ALT -> GLFW.GLFW_KEY_LEFT_ALT;
            case RIGHT_ALT -> GLFW.GLFW_KEY_RIGHT_ALT;
            case ESCAPE -> GLFW.GLFW_KEY_ESCAPE;
            case E -> GLFW.GLFW_KEY_E;
            case Q -> GLFW.GLFW_KEY_Q;
            case R -> GLFW.GLFW_KEY_R;
            case F -> GLFW.GLFW_KEY_F;
        };
    }

    private static InputMouseButton inputMouseButton(int glfwButton) {
        return switch (glfwButton) {
            case GLFW.GLFW_MOUSE_BUTTON_LEFT -> InputMouseButton.LEFT;
            case GLFW.GLFW_MOUSE_BUTTON_RIGHT -> InputMouseButton.RIGHT;
            case GLFW.GLFW_MOUSE_BUTTON_MIDDLE -> InputMouseButton.MIDDLE;
            case GLFW.GLFW_MOUSE_BUTTON_4 -> InputMouseButton.BUTTON_4;
            case GLFW.GLFW_MOUSE_BUTTON_5 -> InputMouseButton.BUTTON_5;
            default -> null;
        };
    }

    private static int glfwMouseButton(InputMouseButton button) {
        return switch (button) {
            case LEFT -> GLFW.GLFW_MOUSE_BUTTON_LEFT;
            case RIGHT -> GLFW.GLFW_MOUSE_BUTTON_RIGHT;
            case MIDDLE -> GLFW.GLFW_MOUSE_BUTTON_MIDDLE;
            case BUTTON_4 -> GLFW.GLFW_MOUSE_BUTTON_4;
            case BUTTON_5 -> GLFW.GLFW_MOUSE_BUTTON_5;
        };
    }

    private WindowGeometry captureWindowedGeometry() {
        Position position = backend.queryWindowPosition(windowHandle);
        Dimensions logicalSize = backend.queryLogicalSize(windowHandle);
        if (logicalSize.width() <= 0 || logicalSize.height() <= 0) {
            throw new IllegalStateException(
                    "GLFW reported non-positive windowed restore dimensions: "
                            + logicalSize.width() + "x" + logicalSize.height());
        }
        return new WindowGeometry(position.x(), position.y(), logicalSize.width(), logicalSize.height());
    }

    private TransitionPlan planTransition(WindowMode mode, WindowGeometry restoreGeometry) {
        return switch (mode) {
            case WINDOWED -> {
                if (restoreGeometry == null) {
                    throw new IllegalStateException("Windowed restore geometry is unavailable");
                }
                validateRestoreGeometry(restoreGeometry);
                yield new TransitionPlan(
                        WindowMode.WINDOWED,
                        true,
                        MemoryUtil.NULL,
                        restoreGeometry.x(),
                        restoreGeometry.y(),
                        restoreGeometry.width(),
                        restoreGeometry.height(),
                        GLFW.GLFW_DONT_CARE);
            }
            case BORDERLESS_FULLSCREEN -> {
                MonitorTarget monitor = queryPrimaryMonitorTarget();
                yield new TransitionPlan(
                        WindowMode.BORDERLESS_FULLSCREEN,
                        false,
                        MemoryUtil.NULL,
                        monitor.position().x(),
                        monitor.position().y(),
                        monitor.videoMode().width(),
                        monitor.videoMode().height(),
                        GLFW.GLFW_DONT_CARE);
            }
            case EXCLUSIVE_FULLSCREEN -> {
                MonitorTarget monitor = queryPrimaryMonitorTarget();
                yield new TransitionPlan(
                        WindowMode.EXCLUSIVE_FULLSCREEN,
                        null,
                        monitor.handle(),
                        0,
                        0,
                        monitor.videoMode().width(),
                        monitor.videoMode().height(),
                        monitor.videoMode().refreshRate());
            }
        };
    }

    private MonitorTarget queryPrimaryMonitorTarget() {
        long monitor = backend.primaryMonitor();
        if (monitor == MemoryUtil.NULL) {
            throw new IllegalStateException("GLFW primary monitor is unavailable");
        }
        VideoMode videoMode = backend.queryVideoMode(monitor);
        if (videoMode == null) {
            throw new IllegalStateException("GLFW primary monitor video mode is unavailable");
        }
        if (videoMode.width() <= 0 || videoMode.height() <= 0 || videoMode.refreshRate() <= 0) {
            throw new IllegalStateException(
                    "GLFW reported invalid primary monitor video mode: "
                            + videoMode.width() + "x" + videoMode.height() + "@" + videoMode.refreshRate());
        }
        Position position = backend.queryMonitorPosition(monitor);
        return new MonitorTarget(monitor, position, videoMode);
    }

    private void applyTransition(TransitionPlan plan) {
        if (plan.decorated() != null) {
            backend.setDecorated(windowHandle, plan.decorated());
        }
        backend.setWindowMonitor(
                windowHandle,
                plan.monitor(),
                plan.x(),
                plan.y(),
                plan.width(),
                plan.height(),
                plan.refreshRate());
    }

    private void dispatchPendingSizes() {
        if (logicalSizePending) {
            int logicalWidth = pendingLogicalWidth;
            int logicalHeight = pendingLogicalHeight;
            logicalSizePending = false;
            validatePlatformDimensions("logical window", new Dimensions(logicalWidth, logicalHeight));
            sizeListener.onLogicalWindowSizeChanged(logicalWidth, logicalHeight);
        }
        if (framebufferSizePending) {
            int framebufferWidth = pendingFramebufferWidth;
            int framebufferHeight = pendingFramebufferHeight;
            framebufferSizePending = false;
            validatePlatformDimensions("framebuffer", new Dimensions(framebufferWidth, framebufferHeight));
            sizeListener.onFramebufferSizeChanged(framebufferWidth, framebufferHeight);
        }
    }

    private void stageLogicalSize(int logicalWidth, int logicalHeight) {
        pendingLogicalWidth = logicalWidth;
        pendingLogicalHeight = logicalHeight;
        logicalSizePending = true;
    }

    private void stageFramebufferSize(int framebufferWidth, int framebufferHeight) {
        pendingFramebufferWidth = framebufferWidth;
        pendingFramebufferHeight = framebufferHeight;
        framebufferSizePending = true;
    }

    private void clearPendingSizes() {
        logicalSizePending = false;
        framebufferSizePending = false;
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
        clearPendingSizes();
        windowedRestoreGeometry = null;
        clearHeldInput();
        clearPendingInputEdges();
        clearMouseMotionState();
        pendingInputFailure = null;
        pendingOpenGlDebugFailure = null;
        List<Throwable> failures = new ArrayList<>();
        releaseCursorForCleanup(failures);
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
        return callbackState != null
                || debugCallbackState != null
                || sizeCallbackState != null
                || inputCallbackState != null
                || motionCallbackState != null
                || glfwInitialized
                || windowHandle != 0L
                || windowRegistration != null
                || contextCurrent
                || capabilitiesCreated;
    }

    private void requireOwnerThread() {
        openGlThreadGuard.assertOwnerThread();
    }

    private static void validatePlatformDimensions(String kind, Dimensions dimensions) {
        if (dimensions.width() < 0 || dimensions.height() < 0) {
            throw new IllegalStateException(
                    "GLFW reported negative " + kind + " dimensions: "
                            + dimensions.width() + "x" + dimensions.height());
        }
    }

    private static void validateRestoreGeometry(WindowGeometry geometry) {
        if (geometry.width() <= 0 || geometry.height() <= 0) {
            throw new IllegalStateException(
                    "Windowed restore dimensions must be positive: "
                            + geometry.width() + "x" + geometry.height());
        }
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

    record MouseMotion(double x, double y) {
    }

    record Dimensions(int width, int height) {
    }

    record Position(int x, int y) {
    }

    record VideoMode(int width, int height, int refreshRate) {
    }

    record WindowGeometry(int x, int y, int width, int height) {
    }

    record MonitorTarget(long handle, Position position, VideoMode videoMode) {
    }

    record TransitionPlan(
            WindowMode mode,
            Boolean decorated,
            long monitor,
            int x,
            int y,
            int width,
            int height,
            int refreshRate) {
    }

}
