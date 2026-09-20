package com.samo.engine.render.opengl.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RendererVisualDemoFramebufferSizeTest {
    @Test
    void tracksOnlyFramebufferSizeCallbacks() {
        RendererVisualDemoFramebufferSize size =
                new RendererVisualDemoFramebufferSize(1280, 720);

        size.onLogicalWindowSizeChanged(640, 360);
        assertEquals(1280, size.width());
        assertEquals(720, size.height());

        size.onFramebufferSizeChanged(1920, 1080);
        assertEquals(1920, size.width());
        assertEquals(1080, size.height());
    }
}
