package com.samo.engine.render.opengl.internal;

import com.samo.engine.platform.api.WindowSizeListener;

final class RendererVisualDemoFramebufferSize implements WindowSizeListener {
    private int width;
    private int height;

    RendererVisualDemoFramebufferSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public void onLogicalWindowSizeChanged(int width, int height) {
    }

    @Override
    public void onFramebufferSizeChanged(int width, int height) {
        this.width = width;
        this.height = height;
    }

    int width() {
        return width;
    }

    int height() {
        return height;
    }
}
