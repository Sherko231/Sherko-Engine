package com.samo.engine.render.opengl.internal;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;
import org.joml.Matrix4fc;

final class CameraUniformBlock {
    static final String GLSL_BLOCK_NAME = "CameraBlock";
    static final int BINDING = 0;
    static final int VIEW_OFFSET_BYTES = 0;
    static final int PROJECTION_OFFSET_BYTES = 64;
    static final int SIZE_BYTES = 128;

    private CameraUniformBlock() {
    }

    static void write(Matrix4fc view, Matrix4fc projection, ByteBuffer destination) {
        Matrix4fc viewMatrix = Objects.requireNonNull(view, "view");
        Matrix4fc projectionMatrix = Objects.requireNonNull(projection, "projection");
        ByteBuffer output = Objects.requireNonNull(destination, "destination");

        if (output.remaining() < SIZE_BYTES) {
            throw new IllegalArgumentException(
                    "destination requires at least " + SIZE_BYTES + " remaining bytes");
        }

        int start = output.position();
        ByteBuffer target = output.duplicate().order(ByteOrder.nativeOrder());
        target.position(start);
        viewMatrix.get(target);
        projectionMatrix.get(target);
        output.position(start + SIZE_BYTES);
    }
}
