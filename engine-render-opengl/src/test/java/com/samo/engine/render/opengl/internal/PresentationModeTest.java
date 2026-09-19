package com.samo.engine.render.opengl.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL21;

class PresentationModeTest {
    @Test
    void selectsModeFromActualDefaultFramebufferEncoding() {
        assertEquals(
                PresentationMode.HARDWARE_SRGB,
                PresentationMode.fromDefaultFramebufferEncoding(GL21.GL_SRGB));
        assertEquals(
                PresentationMode.MANUAL_SRGB,
                PresentationMode.fromDefaultFramebufferEncoding(GL11.GL_LINEAR));
        assertThrows(
                IllegalStateException.class,
                () -> PresentationMode.fromDefaultFramebufferEncoding(0x7fffffff));
    }

    @Test
    void hardwareModeLeavesShaderAndLinearClearUnchanged() {
        String source = "#version 460 core\nvoid main() {}";

        assertTrue(PresentationMode.HARDWARE_SRGB.framebufferSrgbEnabled());
        assertEquals(source, PresentationMode.HARDWARE_SRGB.fragmentSource(source));
        assertEquals(0.10f, PresentationMode.HARDWARE_SRGB.clearComponent(0.10f));
    }

    @Test
    void manualModeInjectsExactlyOneDefineAndEncodesClear() {
        String source = "#version 460 core\nvoid main() {}";

        String variant = PresentationMode.MANUAL_SRGB.fragmentSource(source);

        assertFalse(PresentationMode.MANUAL_SRGB.framebufferSrgbEnabled());
        assertEquals(1, occurrences(variant, "#define SHERKO_MANUAL_SRGB_ENCODE 1"));
        assertTrue(variant.startsWith(
                "#version 460 core"
                        + System.lineSeparator()
                        + "#define SHERKO_MANUAL_SRGB_ENCODE 1"));
        assertEquals(0.34919024f, PresentationMode.MANUAL_SRGB.clearComponent(0.10f), 1.0e-6f);
    }

    private static int occurrences(String value, String token) {
        return (value.length() - value.replace(token, "").length()) / token.length();
    }
}
