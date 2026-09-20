package com.samo.engine.platform.api;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
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
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL43;
import org.lwjgl.opengl.GLDebugMessageCallback;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

final class LwjglGlfwNativeBackend implements GlfwNativeBackend {
    @Override
    public GlfwErrorCallbackRegistration installErrorCallback() {

        GLFWErrorCallback owned = GLFWErrorCallback.createPrint(System.err);
        try {
            GLFWErrorCallback previous = GLFW.glfwSetErrorCallback(owned);
            return new GlfwErrorCallbackRegistration(owned, previous);
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
    public void restoreErrorCallback(GlfwErrorCallbackRegistration state) {

        GLFWErrorCallbackI previous = (GLFWErrorCallbackI) state.previous();
        GLFW.glfwSetErrorCallback(previous);

    }

    @Override
    public void freeOwnedErrorCallback(GlfwErrorCallbackRegistration state) {

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
    public boolean openGlDebugContext() {

        return (GL11.glGetInteger(GL30.GL_CONTEXT_FLAGS) & GL43.GL_CONTEXT_FLAG_DEBUG_BIT) != 0;

    }

    @Override
    public OpenGlDebugCallbackRegistration installOpenGlDebugCallback(OpenGlDebugEventSink sink) {

        GLDebugMessageCallback callback = GLDebugMessageCallback
            .create((source, type, id, severity, length, message, userParam) -> sink.onMessage(source, type, id, severity, GLDebugMessageCallback.getMessage(length, message)));
        try {
            GL11.glEnable(GL43.GL_DEBUG_OUTPUT);
            GL11.glEnable(GL43.GL_DEBUG_OUTPUT_SYNCHRONOUS);
            GL43.glDebugMessageCallback(callback, MemoryUtil.NULL);
            return new OpenGlDebugCallbackRegistration(callback);
        } catch (RuntimeException | Error failure) {
            try {
                callback.free();
            } catch (RuntimeException | Error cleanupFailure) {
                addSuppressedUnlessSame(failure, cleanupFailure);
            }
            throw failure;
        }

    }

    @Override
    public void releaseOpenGlDebugCallback(OpenGlDebugCallbackRegistration state) {

        List<Throwable> failures = new ArrayList<>();
        runCleanup(failures, () -> GL43.glDebugMessageCallback(null, MemoryUtil.NULL));
        runCleanup(failures, () -> GL11.glDisable(GL43.GL_DEBUG_OUTPUT_SYNCHRONOUS));
        runCleanup(failures, () -> GL11.glDisable(GL43.GL_DEBUG_OUTPUT));
        runCleanup(failures, () -> ((GLDebugMessageCallback) state.callback()).free());
        throwCleanupFailure(failures);

    }

    @Override
    public GlfwSizeCallbackRegistration installSizeCallbacks(long handle, GlfwSizeEventSink sink) {

        GLFWWindowSizeCallback logical = GLFWWindowSizeCallback.create((window, callbackWidth, callbackHeight) -> sink.onLogicalSize(callbackWidth, callbackHeight));
        GLFWFramebufferSizeCallback framebuffer = GLFWFramebufferSizeCallback
            .create((window, callbackWidth, callbackHeight) -> sink.onFramebufferSize(callbackWidth, callbackHeight));
        boolean logicalInstalled = false;
        try {
            GLFW.glfwSetWindowSizeCallback(handle, logical);
            logicalInstalled = true;
            GLFW.glfwSetFramebufferSizeCallback(handle, framebuffer);
            return new GlfwSizeCallbackRegistration(logical, framebuffer);
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
    public void releaseSizeCallbacks(long handle, GlfwSizeCallbackRegistration state) {

        List<Throwable> failures = new ArrayList<>();
        runCleanup(failures, () -> GLFW.glfwSetWindowSizeCallback(handle, null));
        runCleanup(failures, () -> GLFW.glfwSetFramebufferSizeCallback(handle, null));
        runCleanup(failures, () -> ((GLFWWindowSizeCallback) state.logical()).free());
        runCleanup(failures, () -> ((GLFWFramebufferSizeCallback) state.framebuffer()).free());
        throwCleanupFailure(failures);

    }

    @Override
    public GlfwInputCallbackRegistration installInputCallbacks(long handle, GlfwInputEventSink sink) {

        GLFWWindowFocusCallback focus = GLFWWindowFocusCallback.create((window, focused) -> sink.onFocus(focused));
        GLFWKeyCallback key = GLFWKeyCallback.create((window, callbackKey, scancode, action, mods) -> sink.onKey(callbackKey, action));
        GLFWMouseButtonCallback mouseButton = GLFWMouseButtonCallback.create((window, button, action, mods) -> sink.onMouseButton(button, action));
        boolean focusInstalled = false;
        boolean keyInstalled = false;
        try {
            GLFW.glfwSetWindowFocusCallback(handle, focus);
            focusInstalled = true;
            GLFW.glfwSetKeyCallback(handle, key);
            keyInstalled = true;
            GLFW.glfwSetMouseButtonCallback(handle, mouseButton);
            return new GlfwInputCallbackRegistration(focus, key, mouseButton);
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
    public void releaseInputCallbacks(long handle, GlfwInputCallbackRegistration state) {

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
    public GlfwCursorPositionCallbackRegistration installCursorPositionCallback(long handle, GlfwCursorPositionEventSink sink) {

        GLFWCursorPosCallback cursorPosition = GLFWCursorPosCallback.create((window, x, y) -> sink.onCursorPosition(x, y));
        try {
            GLFW.glfwSetCursorPosCallback(handle, cursorPosition);
            return new GlfwCursorPositionCallbackRegistration(cursorPosition);
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
    public void releaseCursorPositionCallback(long handle, GlfwCursorPositionCallbackRegistration state) {

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

        GLFW.glfwSetInputMode(handle, GLFW.GLFW_RAW_MOUSE_MOTION, enabled ? GLFW.GLFW_TRUE : GLFW.GLFW_FALSE);

    }

    @Override
    public GlfwDimensions queryLogicalSize(long handle) {

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer sizeWidth = stack.mallocInt(1);
            IntBuffer sizeHeight = stack.mallocInt(1);
            GLFW.glfwGetWindowSize(handle, sizeWidth, sizeHeight);
            return new GlfwDimensions(sizeWidth.get(0), sizeHeight.get(0));
        }

    }

    @Override
    public GlfwDimensions queryFramebufferSize(long handle) {

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer sizeWidth = stack.mallocInt(1);
            IntBuffer sizeHeight = stack.mallocInt(1);
            GLFW.glfwGetFramebufferSize(handle, sizeWidth, sizeHeight);
            return new GlfwDimensions(sizeWidth.get(0), sizeHeight.get(0));
        }

    }

    @Override
    public GlfwPosition queryWindowPosition(long handle) {

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer x = stack.mallocInt(1);
            IntBuffer y = stack.mallocInt(1);
            GLFW.glfwGetWindowPos(handle, x, y);
            return new GlfwPosition(x.get(0), y.get(0));
        }

    }

    @Override
    public long primaryMonitor() {

        return GLFW.glfwGetPrimaryMonitor();

    }

    @Override
    public GlfwVideoMode queryVideoMode(long monitor) {

        GLFWVidMode mode = GLFW.glfwGetVideoMode(monitor);
        if (mode == null) {
            return null;
        }
        return new GlfwVideoMode(mode.width(), mode.height(), mode.refreshRate());

    }

    @Override
    public GlfwPosition queryMonitorPosition(long monitor) {

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer x = stack.mallocInt(1);
            IntBuffer y = stack.mallocInt(1);
            GLFW.glfwGetMonitorPos(monitor, x, y);
            return new GlfwPosition(x.get(0), y.get(0));
        }

    }

    @Override
    public void setDecorated(long handle, boolean decorated) {

        GLFW.glfwSetWindowAttrib(handle, GLFW.GLFW_DECORATED, decorated ? GLFW.GLFW_TRUE : GLFW.GLFW_FALSE);

    }

    @Override
    public void setWindowMonitor(long handle, long monitor, int x, int y, int width, int height, int refreshRate) {

        GLFW.glfwSetWindowMonitor(handle, monitor, x, y, width, height, refreshRate);

    }

    @Override
    public void swapBuffers(long handle) {

        GLFW.glfwSwapBuffers(handle);

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
