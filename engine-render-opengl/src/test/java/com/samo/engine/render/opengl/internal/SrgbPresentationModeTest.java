package com.samo.engine.render.opengl.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL21;

class SrgbPresentationModeTest {
    @Test
    void selectsModeFromActualDefaultFramebufferEncoding() {

        assertEquals(SrgbPresentationMode.HARDWARE_SRGB, SrgbPresentationMode.fromDefaultFramebufferEncoding(GL21.GL_SRGB));
        assertEquals(SrgbPresentationMode.MANUAL_SRGB, SrgbPresentationMode.fromDefaultFramebufferEncoding(GL11.GL_LINEAR));
        assertThrows(IllegalStateException.class, () -> SrgbPresentationMode.fromDefaultFramebufferEncoding(0x7fffffff));

    }

    @Test
    void hardwareModeLeavesShaderAndLinearClearUnchanged() {

        String source = "#version 460 core\nvoid main() {}";

        assertTrue(SrgbPresentationMode.HARDWARE_SRGB.framebufferSrgbEnabled());
        assertEquals(source, SrgbPresentationMode.HARDWARE_SRGB.fragmentSource(source));
        assertEquals(0.10f, SrgbPresentationMode.HARDWARE_SRGB.clearComponent(0.10f));

    }

    @Test
    void manualModeInjectsExactlyOneDefineAndEncodesClear() {

        String source = "#version 460 core\nvoid main() {}";

        String variant = SrgbPresentationMode.MANUAL_SRGB.fragmentSource(source);

        assertFalse(SrgbPresentationMode.MANUAL_SRGB.framebufferSrgbEnabled());
        assertEquals(1, occurrences(variant, "#define SHERKO_MANUAL_SRGB_ENCODE 1"));
        assertTrue(variant.startsWith("#version 460 core" + System.lineSeparator() + "#define SHERKO_MANUAL_SRGB_ENCODE 1"));
        assertEquals(0.34919024f, SrgbPresentationMode.MANUAL_SRGB.clearComponent(0.10f), 1.0e-6f);

    }

    private static int occurrences(String value, String token) {

        return (value.length() - value.replace(token, "").length()) / token.length();

    }
}
