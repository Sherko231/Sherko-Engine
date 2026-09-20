package com.samo.engine.platform.api;

import java.util.Objects;

/** Owns coalesced logical/framebuffer size staging and post-poll delivery. */
final class GlfwDeferredSizeDelivery {
    private final WindowSizeListener listener;

    private boolean logicalPending;
    private int logicalWidth;
    private int logicalHeight;
    private boolean framebufferPending;
    private int framebufferWidth;
    private int framebufferHeight;

    GlfwDeferredSizeDelivery(WindowSizeListener listener) {
        this.listener = Objects.requireNonNull(listener, "listener");
    }

    void stageLogical(int width, int height) {
        logicalWidth = width;
        logicalHeight = height;
        logicalPending = true;
    }

    void stageFramebuffer(int width, int height) {
        framebufferWidth = width;
        framebufferHeight = height;
        framebufferPending = true;
    }

    void dispatchPending() {
        if (logicalPending) {
            int width = logicalWidth;
            int height = logicalHeight;
            logicalPending = false;
            validateDimensions("logical window", new GlfwDimensions(width, height));
            listener.onLogicalWindowSizeChanged(width, height);
        }
        if (framebufferPending) {
            int width = framebufferWidth;
            int height = framebufferHeight;
            framebufferPending = false;
            validateDimensions("framebuffer", new GlfwDimensions(width, height));
            listener.onFramebufferSizeChanged(width, height);
        }
    }

    void clear() {
        logicalPending = false;
        framebufferPending = false;
    }

    static void validateDimensions(String kind, GlfwDimensions dimensions) {
        if (dimensions.width() < 0 || dimensions.height() < 0) {
            throw new IllegalStateException("GLFW reported negative " + kind + " dimensions: " + dimensions.width() + "x" + dimensions.height());
        }
    }
}
