package com.samo.engine.render.opengl.internal;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;

final class PerFrameUniformBlock {
    static final String GLSL_BLOCK_NAME = "PerFrameBlock";
    static final int BINDING = 1;
    static final int FRAMEBUFFER_SIZE_AND_INVERSE_OFFSET_BYTES = 0;
    static final int SIZE_BYTES = 16;

    private PerFrameUniformBlock() {
    }

    static void write(int framebufferWidth, int framebufferHeight, ByteBuffer destination) {
        ByteBuffer output = Objects.requireNonNull(destination, "destination");
        if (framebufferWidth <= 0) {
            throw new IllegalArgumentException("framebufferWidth must be positive");
        }
        if (framebufferHeight <= 0) {
            throw new IllegalArgumentException("framebufferHeight must be positive");
        }
        if (output.remaining() < SIZE_BYTES) {
            throw new IllegalArgumentException(
                    "destination requires at least " + SIZE_BYTES + " remaining bytes");
        }

        int start = output.position();
        ByteBuffer target = output.duplicate().order(ByteOrder.nativeOrder());
        target.position(start);
        target.putFloat((float) framebufferWidth);
        target.putFloat((float) framebufferHeight);
        target.putFloat(1.0f / framebufferWidth);
        target.putFloat(1.0f / framebufferHeight);
        output.position(start + SIZE_BYTES);
    }
}
