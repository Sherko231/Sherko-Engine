package com.samo.engine.render.opengl.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RendererVisualDemoLoopTest {
    @Test
    void elapsedTimeAdditionSaturatesAtLongMaxValue() {
        assertEquals(42L, RendererVisualDemoLoop.saturatingAdd(40L, 2L));
        assertEquals(
                Long.MAX_VALUE,
                RendererVisualDemoLoop.saturatingAdd(Long.MAX_VALUE - 1L, 2L));
    }
}
