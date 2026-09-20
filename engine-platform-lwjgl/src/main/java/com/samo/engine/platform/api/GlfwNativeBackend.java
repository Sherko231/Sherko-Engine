package com.samo.engine.platform.api;

interface GlfwNativeBackend {
    GlfwErrorCallbackRegistration installErrorCallback();

    void restoreErrorCallback(GlfwErrorCallbackRegistration state);

    void freeOwnedErrorCallback(GlfwErrorCallbackRegistration state);

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

    default boolean openGlDebugContext() {
        return false;
    }

    default OpenGlDebugCallbackRegistration installOpenGlDebugCallback(OpenGlDebugEventSink sink) {
        return null;
    }

    default void releaseOpenGlDebugCallback(OpenGlDebugCallbackRegistration state) {
    }

    GlfwSizeCallbackRegistration installSizeCallbacks(long handle, GlfwSizeEventSink sink);

    void releaseSizeCallbacks(long handle, GlfwSizeCallbackRegistration state);

    default GlfwInputCallbackRegistration installInputCallbacks(long handle, GlfwInputEventSink sink) {
        return null;
    }

    default void releaseInputCallbacks(long handle, GlfwInputCallbackRegistration state) {
    }

    default GlfwCursorPositionCallbackRegistration installCursorPositionCallback(long handle, GlfwCursorPositionEventSink sink) {
        return null;
    }

    default void releaseCursorPositionCallback(long handle, GlfwCursorPositionCallbackRegistration state) {
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

    GlfwDimensions queryLogicalSize(long handle);

    GlfwDimensions queryFramebufferSize(long handle);

    GlfwPosition queryWindowPosition(long handle);

    long primaryMonitor();

    GlfwVideoMode queryVideoMode(long monitor);

    GlfwPosition queryMonitorPosition(long monitor);

    void setDecorated(long handle, boolean decorated);

    void setWindowMonitor(long handle, long monitor, int x, int y, int width, int height, int refreshRate);

    void swapBuffers(long handle);

    void pollEvents();

    void showWindow(long handle);

    void hideWindow(long handle);
}

record GlfwErrorCallbackRegistration(Object owned, Object previous) {
}

record GlfwSizeCallbackRegistration(Object logical, Object framebuffer) {
}

record GlfwInputCallbackRegistration(Object focus, Object key, Object mouseButton) {
}

record GlfwCursorPositionCallbackRegistration(Object cursorPosition) {
}

record OpenGlDebugCallbackRegistration(Object callback) {
}

interface GlfwSizeEventSink {
    void onLogicalSize(int width, int height);

    void onFramebufferSize(int width, int height);
}

interface GlfwInputEventSink {
    void onFocus(boolean focused);

    void onKey(int key, int action);

    void onMouseButton(int button, int action);
}

@FunctionalInterface
interface GlfwCursorPositionEventSink {
    void onCursorPosition(double x, double y);
}

@FunctionalInterface
interface OpenGlDebugEventSink {
    void onMessage(int source, int type, int id, int severity, String message);
}
