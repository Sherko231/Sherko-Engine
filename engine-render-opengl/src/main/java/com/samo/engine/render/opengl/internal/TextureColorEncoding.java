package com.samo.engine.render.opengl.internal;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL21;

enum TextureColorEncoding {
    SRGB_COLOR(GL21.GL_SRGB8_ALPHA8), LINEAR_DATA(GL11.GL_RGBA8);

    private final int internalFormat;

    TextureColorEncoding(int internalFormat) {
        this.internalFormat = internalFormat;
    }

    int internalFormat() {
        return internalFormat;
    }
}
