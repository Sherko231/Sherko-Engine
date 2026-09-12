package com.samo.engine.platform.api;

import com.samo.engine.core.api.EngineLogger;
import com.samo.engine.core.api.EngineSubsystem;
import com.samo.engine.core.api.NativeResourceRegistry;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWErrorCallbackI;
import org.lwjgl.glfw.GLFWFramebufferSizeCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.glfw.GLFWWindowFocusCallback;
import org.lwjgl.glfw.GLFWWindowSizeCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryStack;
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
    private final Backend backend;
    private final boolean[] heldKeys = new boolean[GLFW.GLFW_KEY_LAST + 1];
    private final boolean[] heldMouseButtons = new boolean[GLFW.GLFW_MOUSE_BUTTON_LAST + 1];

    private Thread ownerThread;
    private CallbackState callbackState;
    private SizeCallbackState sizeCallbackState;
    private InputCallbackState inputCallbackState;
    private MotionCallbackState motionCallbackState;
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
    private boolean rawMouseMotionEnabled;
    private boolean mouseMotionBaselineValid;
    private double previousMouseX;
    private double previousMouseY;
    private double accumulatedMouseDeltaX;
    private double accumulatedMouseDeltaY;
    private Throwable pendingInputFailure;

    public GlfwWindow(
            int width,
            int height,
            String title,
            EngineLogger logger,
            NativeResourceRegistry nativeResources) {
        this(width, height, title, logger, nativeResources, NO_OP_SIZE_LISTENER);
    }

    public GlfwWindow(
            int width,
            int height,
            String title,
            EngineLogger logger,
            NativeResourceRegistry nativeResources,
            WindowSizeListener sizeListener) {
        this(width, height, title, logger, nativeResources, sizeListener, new LwjglBackend());
    }

    GlfwWindow(
            int width,
            int height,
            String title,
            EngineLogger logger,
            NativeResourceRegistry nativeResources,
            Backend backend) {
        this(width, height, title, logger, nativeResources, NO_OP_SIZE_LISTENER, backend);
    }

    GlfwWindow(
            int width,
            int height,
            String title,
            EngineLogger logger,
            NativeResourceRegistry nativeResources,
            WindowSizeListener sizeListener,
            Backend backend) {
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
        this.backend = Objects.requireNonNull(backend, "backend");
    }

    public void pollEvents() {
        if (!eventPollingEnabled) {
            throw new IllegalStateException("GLFW event polling requires a started window");
        }
        requireOwnerThread();
        try {
            backend.pollEvents();
        } catch (RuntimeException | Error failure) {
            Throwable callbackFailure = takePendingInputFailure();
            if (callbackFailure != null) {
                addSuppressedUnlessSame(failure, callbackFailure);
            }
            throw failure;
        }
        throwPendingInputFailure();
        dispatchPendingSizes();
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

        if (!cursorCaptureRequested && !cursorCaptureEffective) {
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
        ownerThread = Thread.currentThread();
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

            sizeCallbackState = backend.installSizeCallbacks(windowHandle, new SizeEventSink() {
                @Override
                public void onLogicalSize(int logicalWidth, int logicalHeight) {
                    stageLogicalSize(logicalWidth, logicalHeight);
                }

                @Override
                public void onFramebufferSize(int framebufferWidth, int framebufferHeight) {
                    stageFramebufferSize(framebufferWidth, framebufferHeight);
                }
            });

            inputCallbackState = backend.installInputCallbacks(windowHandle, new InputEventSink() {
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
        clearMouseMotionState();
        pendingInputFailure = null;

        List<Throwable> failures = new ArrayList<>();
        releaseCursorForCleanup(failures);
        releaseMotionCallback(failures);
        releaseInputCallbacks(failures);
        releaseSizeCallbacks(failures);
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
        clearMouseMotionState();
        pendingInputFailure = null;

        List<Throwable> failures = new ArrayList<>();
        releaseCursorForCleanup(failures);
        releaseMotionCallback(failures);
        releaseInputCallbacks(failures);
        releaseSizeCallbacks(failures);
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
        accumulatedMouseDeltaX = 0.0;
        accumulatedMouseDeltaY = 0.0;
        return motion;
    }

    private void captureCursorWithRawMotion() {
        boolean previousRequested = cursorCaptureRequested;
        boolean previousEffective = cursorCaptureEffective;
        boolean previousRearm = cursorCaptureNeedsExplicitRearm;
        boolean previousRaw = rawMouseMotionEnabled;
        boolean cursorDisabled = false;
        boolean rawEnabledThisAttempt = false;

        try {
            backend.setCursorMode(windowHandle, GLFW.GLFW_CURSOR_DISABLED);
            cursorDisabled = true;
            if (backend.rawMouseMotionSupported()) {
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
            if (rawEnabledThisAttempt) {
                try {
                    backend.setRawMouseMotion(windowHandle, false);
                } catch (RuntimeException | Error rollbackFailure) {
                    addSuppressedUnlessSame(failure, rollbackFailure);
                }
            }
            if (cursorDisabled) {
                try {
                    backend.setCursorMode(windowHandle, GLFW.GLFW_CURSOR_NORMAL);
                } catch (RuntimeException | Error rollbackFailure) {
                    addSuppressedUnlessSame(failure, rollbackFailure);
                }
            }
            cursorCaptureRequested = previousRequested;
            cursorCaptureEffective = previousEffective;
            cursorCaptureNeedsExplicitRearm = previousRearm;
            rawMouseMotionEnabled = previousRaw;
            throw failure;
        }
    }

    private void releaseCursorDirect() {
        if (rawMouseMotionEnabled) {
            backend.setRawMouseMotion(windowHandle, false);
            rawMouseMotionEnabled = false;
        }
        if (cursorCaptureEffective) {
            backend.setCursorMode(windowHandle, GLFW.GLFW_CURSOR_NORMAL);
        }
        clearMouseMotionState();
        cursorCaptureRequested = false;
        cursorCaptureEffective = false;
        cursorCaptureNeedsExplicitRearm = false;
    }

    private void handleFocusChanged(boolean focused) {
        windowFocused = focused;
        if (focused) {
            return;
        }

        clearHeldInput();
        clearMouseMotionState();
        if (cursorCaptureRequested) {
            cursorCaptureNeedsExplicitRearm = true;
        }
        if (!cursorCaptureEffective && !rawMouseMotionEnabled) {
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
        try {
            backend.setCursorMode(windowHandle, GLFW.GLFW_CURSOR_NORMAL);
        } catch (RuntimeException | Error failure) {
            stageInputFailure(failure);
        }
    }

    private void handleKeyChanged(int key, int action) {
        if (key < 0 || key >= heldKeys.length) {
            return;
        }
        if (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT) {
            heldKeys[key] = true;
        } else if (action == GLFW.GLFW_RELEASE) {
            heldKeys[key] = false;
        }
    }

    private void handleMouseButtonChanged(int button, int action) {
        if (button < 0 || button >= heldMouseButtons.length) {
            return;
        }
        if (action == GLFW.GLFW_PRESS) {
            heldMouseButtons[button] = true;
        } else if (action == GLFW.GLFW_RELEASE) {
            heldMouseButtons[button] = false;
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
        clearMouseMotionState();
        cursorCaptureRequested = false;
        cursorCaptureEffective = false;
        cursorCaptureNeedsExplicitRearm = false;
        rawMouseMotionEnabled = false;
        pendingInputFailure = null;
    }

    private void clearHeldInput() {
        Arrays.fill(heldKeys, false);
        Arrays.fill(heldMouseButtons, false);
    }

    private void clearMouseMotionState() {
        mouseMotionBaselineValid = false;
        previousMouseX = 0.0;
        previousMouseY = 0.0;
        accumulatedMouseDeltaX = 0.0;
        accumulatedMouseDeltaY = 0.0;
    }

    private void releaseCursorForCleanup(List<Throwable> failures) {
        clearMouseMotionState();
        if (rawMouseMotionEnabled) {
            if (runCleanup(failures, () -> backend.setRawMouseMotion(windowHandle, false))) {
                rawMouseMotionEnabled = false;
            }
        }
        if (cursorCaptureEffective) {
            if (runCleanup(failures, () -> backend.setCursorMode(windowHandle, GLFW.GLFW_CURSOR_NORMAL))) {
                cursorCaptureEffective = false;
            }
        }
        cursorCaptureRequested = false;
        cursorCaptureNeedsExplicitRearm = false;
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
        clearMouseMotionState();
        pendingInputFailure = null;
        List<Throwable> failures = new ArrayList<>();
        releaseCursorForCleanup(failures);
        releaseMotionCallback(failures);
        releaseInputCallbacks(failures);
        releaseSizeCallbacks(failures);
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

    private void releaseMotionCallback(List<Throwable> failures) {
        if (motionCallbackState == null) {
            return;
        }
        MotionCallbackState state = motionCallbackState;
        motionCallbackState = null;
        runCleanup(failures, () -> backend.releaseCursorPositionCallback(windowHandle, state));
    }

    private void releaseInputCallbacks(List<Throwable> failures) {
        if (inputCallbackState == null) {
            return;
        }
        InputCallbackState state = inputCallbackState;
        inputCallbackState = null;
        runCleanup(failures, () -> backend.releaseInputCallbacks(windowHandle, state));
    }

    private void releaseSizeCallbacks(List<Throwable> failures) {
        if (sizeCallbackState == null) {
            return;
        }
        SizeCallbackState state = sizeCallbackState;
        sizeCallbackState = null;
        runCleanup(failures, () -> backend.releaseSizeCallbacks(windowHandle, state));
    }

    private void releaseCallback(List<Throwable> failures) {
        if (callbackState == null) {
            return;
        }
        CallbackState state = callbackState;
        callbackState = null;
        runCleanup(failures, () -> backend.restoreErrorCallback(state));
        runCleanup(failures, () -> backend.freeOwnedErrorCallback(state));
    }

    private boolean hasOwnedNativeState() {
        return callbackState != null
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
        if (ownerThread != Thread.currentThread()) {
            throw new IllegalStateException("GLFW window lifecycle must run on the initializing thread");
        }
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

    record CallbackState(Object owned, Object previous) {
    }

    record SizeCallbackState(Object logical, Object framebuffer) {
    }

    record InputCallbackState(Object focus, Object key, Object mouseButton) {
    }

    record MotionCallbackState(Object cursorPosition) {
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

    interface SizeEventSink {
        void onLogicalSize(int width, int height);

        void onFramebufferSize(int width, int height);
    }

    interface InputEventSink {
        void onFocus(boolean focused);

        void onKey(int key, int action);

        void onMouseButton(int button, int action);
    }

    @FunctionalInterface
    interface MotionEventSink {
        void onCursorPosition(double x, double y);
    }

    interface Backend {
        CallbackState installErrorCallback();

        void restoreErrorCallback(CallbackState state);

        void freeOwnedErrorCallback(CallbackState state);

        boolean initGlfw();

        void terminateGlfw();

        void defaultWindowHints();

        void windowHint(int hint, int value);

        long createWindow(int width, int height, String title);

        void destroyWindow(long handle);

        void makeContextCurrent(long handle);

        void createCapabilities();

        void clearCapabilities();

        boolean openGl46Supported();

        String glVersion();

        String glRenderer();

        SizeCallbackState installSizeCallbacks(long handle, SizeEventSink sink);

        void releaseSizeCallbacks(long handle, SizeCallbackState state);

        default InputCallbackState installInputCallbacks(long handle, InputEventSink sink) {
            return null;
        }

        default void releaseInputCallbacks(long handle, InputCallbackState state) {
        }

        default MotionCallbackState installCursorPositionCallback(long handle, MotionEventSink sink) {
            return null;
        }

        default void releaseCursorPositionCallback(long handle, MotionCallbackState state) {
        }

        default boolean queryWindowFocused(long handle) {
            return true;
        }

        default void setCursorMode(long handle, int mode) {
        }

        default boolean rawMouseMotionSupported() {
            return false;
        }

        default void setRawMouseMotion(long handle, boolean enabled) {
        }

        Dimensions queryLogicalSize(long handle);

        Dimensions queryFramebufferSize(long handle);

        Position queryWindowPosition(long handle);

        long primaryMonitor();

        VideoMode queryVideoMode(long monitor);

        Position queryMonitorPosition(long monitor);

        void setDecorated(long handle, boolean decorated);

        void setWindowMonitor(
                long handle,
                long monitor,
                int x,
                int y,
                int width,
                int height,
                int refreshRate);

        void pollEvents();

        void showWindow(long handle);

        void hideWindow(long handle);
    }

    private static final class LwjglBackend implements Backend {
        @Override
        public CallbackState installErrorCallback() {
            GLFWErrorCallback owned = GLFWErrorCallback.createPrint(System.err);
            try {
                GLFWErrorCallback previous = GLFW.glfwSetErrorCallback(owned);
                return new CallbackState(owned, previous);
            } catch (RuntimeException | Error failure) {
                try {
                    owned.free();
                } catch (RuntimeException | Error cleanupFailure) {
                    addSuppressedUnlessSame(failure, cleanupFailure);
                }
                throw failure;
            }
        }

        @Override
        public void restoreErrorCallback(CallbackState state) {
            GLFWErrorCallbackI previous = (GLFWErrorCallbackI) state.previous();
            GLFW.glfwSetErrorCallback(previous);
        }

        @Override
        public void freeOwnedErrorCallback(CallbackState state) {
            ((GLFWErrorCallback) state.owned()).free();
        }

        @Override
        public boolean initGlfw() {
            return GLFW.glfwInit();
        }

        @Override
        public void terminateGlfw() {
            GLFW.glfwTerminate();
        }

        @Override
        public void defaultWindowHints() {
            GLFW.glfwDefaultWindowHints();
        }

        @Override
        public void windowHint(int hint, int value) {
            GLFW.glfwWindowHint(hint, value);
        }

        @Override
        public long createWindow(int width, int height, String title) {
            return GLFW.glfwCreateWindow(width, height, title, MemoryUtil.NULL, MemoryUtil.NULL);
        }

        @Override
        public void destroyWindow(long handle) {
            GLFW.glfwDestroyWindow(handle);
        }

        @Override
        public void makeContextCurrent(long handle) {
            GLFW.glfwMakeContextCurrent(handle);
        }

        @Override
        public void createCapabilities() {
            GL.createCapabilities();
        }

        @Override
        public void clearCapabilities() {
            GL.setCapabilities(null);
        }

        @Override
        public boolean openGl46Supported() {
            GLCapabilities capabilities = GL.getCapabilities();
            return capabilities.OpenGL46;
        }

        @Override
        public String glVersion() {
            return GL11.glGetString(GL11.GL_VERSION);
        }

        @Override
        public String glRenderer() {
            return GL11.glGetString(GL11.GL_RENDERER);
        }

        @Override
        public SizeCallbackState installSizeCallbacks(long handle, SizeEventSink sink) {
            GLFWWindowSizeCallback logical = GLFWWindowSizeCallback.create(
                    (window, callbackWidth, callbackHeight) ->
                            sink.onLogicalSize(callbackWidth, callbackHeight));
            GLFWFramebufferSizeCallback framebuffer = GLFWFramebufferSizeCallback.create(
                    (window, callbackWidth, callbackHeight) ->
                            sink.onFramebufferSize(callbackWidth, callbackHeight));
            boolean logicalInstalled = false;
            try {
                GLFW.glfwSetWindowSizeCallback(handle, logical);
                logicalInstalled = true;
                GLFW.glfwSetFramebufferSizeCallback(handle, framebuffer);
                return new SizeCallbackState(logical, framebuffer);
            } catch (RuntimeException | Error failure) {
                if (logicalInstalled) {
                    try {
                        GLFW.glfwSetWindowSizeCallback(handle, null);
                    } catch (RuntimeException | Error cleanupFailure) {
                        addSuppressedUnlessSame(failure, cleanupFailure);
                    }
                }
                try {
                    logical.free();
                } catch (RuntimeException | Error cleanupFailure) {
                    addSuppressedUnlessSame(failure, cleanupFailure);
                }
                try {
                    framebuffer.free();
                } catch (RuntimeException | Error cleanupFailure) {
                    addSuppressedUnlessSame(failure, cleanupFailure);
                }
                throw failure;
            }
        }

        @Override
        public void releaseSizeCallbacks(long handle, SizeCallbackState state) {
            List<Throwable> failures = new ArrayList<>();
            runCleanup(failures, () -> GLFW.glfwSetWindowSizeCallback(handle, null));
            runCleanup(failures, () -> GLFW.glfwSetFramebufferSizeCallback(handle, null));
            runCleanup(failures, () -> ((GLFWWindowSizeCallback) state.logical()).free());
            runCleanup(failures, () -> ((GLFWFramebufferSizeCallback) state.framebuffer()).free());
            throwCleanupFailure(failures);
        }

        @Override
        public InputCallbackState installInputCallbacks(long handle, InputEventSink sink) {
            GLFWWindowFocusCallback focus = GLFWWindowFocusCallback.create(
                    (window, focused) -> sink.onFocus(focused));
            GLFWKeyCallback key = GLFWKeyCallback.create(
                    (window, callbackKey, scancode, action, mods) -> sink.onKey(callbackKey, action));
            GLFWMouseButtonCallback mouseButton = GLFWMouseButtonCallback.create(
                    (window, button, action, mods) -> sink.onMouseButton(button, action));
            boolean focusInstalled = false;
            boolean keyInstalled = false;
            try {
                GLFW.glfwSetWindowFocusCallback(handle, focus);
                focusInstalled = true;
                GLFW.glfwSetKeyCallback(handle, key);
                keyInstalled = true;
                GLFW.glfwSetMouseButtonCallback(handle, mouseButton);
                return new InputCallbackState(focus, key, mouseButton);
            } catch (RuntimeException | Error failure) {
                if (keyInstalled) {
                    try {
                        GLFW.glfwSetKeyCallback(handle, null);
                    } catch (RuntimeException | Error cleanupFailure) {
                        addSuppressedUnlessSame(failure, cleanupFailure);
                    }
                }
                if (focusInstalled) {
                    try {
                        GLFW.glfwSetWindowFocusCallback(handle, null);
                    } catch (RuntimeException | Error cleanupFailure) {
                        addSuppressedUnlessSame(failure, cleanupFailure);
                    }
                }
                try {
                    focus.free();
                } catch (RuntimeException | Error cleanupFailure) {
                    addSuppressedUnlessSame(failure, cleanupFailure);
                }
                try {
                    key.free();
                } catch (RuntimeException | Error cleanupFailure) {
                    addSuppressedUnlessSame(failure, cleanupFailure);
                }
                try {
                    mouseButton.free();
                } catch (RuntimeException | Error cleanupFailure) {
                    addSuppressedUnlessSame(failure, cleanupFailure);
                }
                throw failure;
            }
        }

        @Override
        public void releaseInputCallbacks(long handle, InputCallbackState state) {
            List<Throwable> failures = new ArrayList<>();
            runCleanup(failures, () -> GLFW.glfwSetWindowFocusCallback(handle, null));
            runCleanup(failures, () -> GLFW.glfwSetKeyCallback(handle, null));
            runCleanup(failures, () -> GLFW.glfwSetMouseButtonCallback(handle, null));
            runCleanup(failures, () -> ((GLFWWindowFocusCallback) state.focus()).free());
            runCleanup(failures, () -> ((GLFWKeyCallback) state.key()).free());
            runCleanup(failures, () -> ((GLFWMouseButtonCallback) state.mouseButton()).free());
            throwCleanupFailure(failures);
        }

        @Override
        public MotionCallbackState installCursorPositionCallback(long handle, MotionEventSink sink) {
            GLFWCursorPosCallback cursorPosition = GLFWCursorPosCallback.create(
                    (window, x, y) -> sink.onCursorPosition(x, y));
            try {
                GLFW.glfwSetCursorPosCallback(handle, cursorPosition);
                return new MotionCallbackState(cursorPosition);
            } catch (RuntimeException | Error failure) {
                try {
                    cursorPosition.free();
                } catch (RuntimeException | Error cleanupFailure) {
                    addSuppressedUnlessSame(failure, cleanupFailure);
                }
                throw failure;
            }
        }

        @Override
        public void releaseCursorPositionCallback(long handle, MotionCallbackState state) {
            List<Throwable> failures = new ArrayList<>();
            runCleanup(failures, () -> GLFW.glfwSetCursorPosCallback(handle, null));
            runCleanup(failures, () -> ((GLFWCursorPosCallback) state.cursorPosition()).free());
            throwCleanupFailure(failures);
        }

        @Override
        public boolean queryWindowFocused(long handle) {
            return GLFW.glfwGetWindowAttrib(handle, GLFW.GLFW_FOCUSED) == GLFW.GLFW_TRUE;
        }

        @Override
        public void setCursorMode(long handle, int mode) {
            GLFW.glfwSetInputMode(handle, GLFW.GLFW_CURSOR, mode);
        }

        @Override
        public boolean rawMouseMotionSupported() {
            return GLFW.glfwRawMouseMotionSupported();
        }

        @Override
        public void setRawMouseMotion(long handle, boolean enabled) {
            GLFW.glfwSetInputMode(
                    handle,
                    GLFW.GLFW_RAW_MOUSE_MOTION,
                    enabled ? GLFW.GLFW_TRUE : GLFW.GLFW_FALSE);
        }

        @Override
        public Dimensions queryLogicalSize(long handle) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer sizeWidth = stack.mallocInt(1);
                IntBuffer sizeHeight = stack.mallocInt(1);
                GLFW.glfwGetWindowSize(handle, sizeWidth, sizeHeight);
                return new Dimensions(sizeWidth.get(0), sizeHeight.get(0));
            }
        }

        @Override
        public Dimensions queryFramebufferSize(long handle) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer sizeWidth = stack.mallocInt(1);
                IntBuffer sizeHeight = stack.mallocInt(1);
                GLFW.glfwGetFramebufferSize(handle, sizeWidth, sizeHeight);
                return new Dimensions(sizeWidth.get(0), sizeHeight.get(0));
            }
        }

        @Override
        public Position queryWindowPosition(long handle) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer x = stack.mallocInt(1);
                IntBuffer y = stack.mallocInt(1);
                GLFW.glfwGetWindowPos(handle, x, y);
                return new Position(x.get(0), y.get(0));
            }
        }

        @Override
        public long primaryMonitor() {
            return GLFW.glfwGetPrimaryMonitor();
        }

        @Override
        public VideoMode queryVideoMode(long monitor) {
            GLFWVidMode mode = GLFW.glfwGetVideoMode(monitor);
            if (mode == null) {
                return null;
            }
            return new VideoMode(mode.width(), mode.height(), mode.refreshRate());
        }

        @Override
        public Position queryMonitorPosition(long monitor) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer x = stack.mallocInt(1);
                IntBuffer y = stack.mallocInt(1);
                GLFW.glfwGetMonitorPos(monitor, x, y);
                return new Position(x.get(0), y.get(0));
            }
        }

        @Override
        public void setDecorated(long handle, boolean decorated) {
            GLFW.glfwSetWindowAttrib(handle, GLFW.GLFW_DECORATED, decorated ? GLFW.GLFW_TRUE : GLFW.GLFW_FALSE);
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
            GLFW.glfwSetWindowMonitor(handle, monitor, x, y, width, height, refreshRate);
        }

        @Override
        public void pollEvents() {
            GLFW.glfwPollEvents();
        }

        @Override
        public void showWindow(long handle) {
            GLFW.glfwShowWindow(handle);
        }

        @Override
        public void hideWindow(long handle) {
            GLFW.glfwHideWindow(handle);
        }
    }
}
