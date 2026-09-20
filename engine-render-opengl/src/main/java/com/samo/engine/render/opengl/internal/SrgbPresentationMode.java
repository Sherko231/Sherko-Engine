package com.samo.engine.render.opengl.internal;

import java.util.Objects;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL21;

enum SrgbPresentationMode {
    HARDWARE_SRGB(true, false), MANUAL_SRGB(false, true);

    private final boolean framebufferSrgbEnabled;
    private final boolean manualShaderEncode;

    SrgbPresentationMode(boolean framebufferSrgbEnabled, boolean manualShaderEncode) {

        this.framebufferSrgbEnabled = framebufferSrgbEnabled;
        this.manualShaderEncode = manualShaderEncode;

    }

    static SrgbPresentationMode fromDefaultFramebufferEncoding(int encoding) {

        if (encoding == GL21.GL_SRGB) {
            return HARDWARE_SRGB;
        }
        if (encoding == GL11.GL_LINEAR) {
            return MANUAL_SRGB;
        }
        throw new IllegalStateException("Unsupported default framebuffer color encoding: " + encoding);

    }

    boolean framebufferSrgbEnabled() {

        return framebufferSrgbEnabled;

    }

    String fragmentSource(String fragmentSource) {

        String source = Objects.requireNonNull(fragmentSource, "fragmentSource");
        if (!manualShaderEncode) {
            return source;
        }
        String version = "#version 460 core";
        if (!source.startsWith(version)) {
            throw new IllegalArgumentException("Fragment shader must start with '" + version + "' for presentation variant injection");
        }
        return version + System.lineSeparator() + "#define SHERKO_MANUAL_SRGB_ENCODE 1" + source.substring(version.length());

    }

    float clearComponent(float linear) {

        return manualShaderEncode ? SrgbTransfer.encodeLinear(linear) : linear;

    }
}
