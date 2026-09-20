package com.samo.game.sandbox;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

final class SandboxFramebufferSizeTest {
    @Test
    void tracksCurrentFramebufferPixelDimensions() {
        SandboxFramebufferSize size = new SandboxFramebufferSize(1280, 720);

        assertEquals(1280, size.width());
        assertEquals(720, size.height());

        size.update(2560, 1440);

        assertEquals(2560, size.width());
        assertEquals(1440, size.height());
    }
}
