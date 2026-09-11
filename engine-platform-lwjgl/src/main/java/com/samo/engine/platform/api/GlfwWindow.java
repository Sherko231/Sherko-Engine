package com.samo.engine.platform.api;

import com.samo.engine.core.api.EngineLogger;
import com.samo.engine.core.api.EngineSubsystem;
import com.samo.engine.core.api.NativeResourceRegistry;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWErrorCallbackI;
import org.lwjgl.glfw.GLFWFramebufferSizeCallback;
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

    private Thread ownerThread;
    private CallbackState callbackState;
    private SizeCallbackState sizeCallbackState;
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

    /**
     * Creates an uninitialized window description without performing native work.
     *
     * @param width positive logical window width
     * @param height positive logical window height
     * @param title nonblank title, preserved as supplied
     * @param logger structured runtime logger
     * @param nativeResources caller-owned native-resource registry
     */
    public GlfwWindow(
            int width,
            int height,
            String title,
            EngineLogger logger,
            NativeResourceRegistry nativeResources) {
        this(width, height, title, logger, nativeResources, NO_OP_SIZE_LISTENER);
    }

    /**
     * Creates an uninitialized window description with a renderer-neutral size receiver.
     *
     * @param width positive logical window width
     * @param height positive logical window height
     * @param title nonblank title, preserved as supplied
     * @param logger structured runtime logger
     * @param nativeResources caller-owned native-resource registry
     * @param sizeListener receiver for logical and framebuffer pixel dimensions
     */
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

    /**
     * Polls one GLFW event batch and then delivers the latest pending size notifications.
     *
     * <p>This operation is available only while the subsystem is started and must run on the
     * initializing thread. Native callbacks only stage values; consumer callbacks run here after
     * GLFW polling returns.</p>
     */
    public void pollEvents() {
        if (!eventPollingEnabled) {
            throw new IllegalStateException("GLFW event polling requires a started window");
        }
        requireOwnerThread();
        backend.pollEvents();
        dispatchPendingSizes();
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

            Dimensions logicalSize = backend.queryLogicalSize(windowHandle);
            validatePlatformDimensions("logical window", logicalSize);
            stageLogicalSize(logicalSize.width(), logicalSize.height());

            Dimensions framebufferSize = backend.queryFramebufferSize(windowHandle);
            validatePlatformDimensions("framebuffer", framebufferSize);
            stageFramebufferSize(framebufferSize.width(), framebufferSize.height());

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

        List<Throwable> failures = new ArrayList<>();
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

        List<Throwable> failures = new ArrayList<>();
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
        List<Throwable> failures = new ArrayList<>();
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

    private void releaseSizeCallbacks(List<Throwable> failures) {
        if (sizeCallbackState == null) {
            return;
        }
        SizeCallbackState state = sizeCallbackState;
        if (runCleanup(failures, () -> backend.releaseSizeCallbacks(windowHandle, state))) {
            sizeCallbackState = null;
        }
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

    record Dimensions(int width, int height) {
    }

    interface SizeEventSink {
        void onLogicalSize(int width, int height);

        void onFramebufferSize(int width, int height);
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

        Dimensions queryLogicalSize(long handle);

        Dimensions queryFramebufferSize(long handle);

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
