package com.samo.engine.platform.api;

/** Receives logical window and framebuffer pixel dimensions as separate platform signals. */
public interface WindowSizeListener {
    /** Called with the latest logical window dimensions in screen coordinates. */
    void onLogicalWindowSizeChanged(int width, int height);

    /** Called with the latest framebuffer dimensions in pixels. Zero axes are valid while minimized. */
    void onFramebufferSizeChanged(int width, int height);
}
