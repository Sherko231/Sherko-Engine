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
        putMatrixColumnMajor(viewMatrix, target);
        putMatrixColumnMajor(projectionMatrix, target);
        output.position(start + SIZE_BYTES);
    }

    private static void putMatrixColumnMajor(Matrix4fc matrix, ByteBuffer target) {
        target.putFloat(matrix.m00());
        target.putFloat(matrix.m01());
        target.putFloat(matrix.m02());
        target.putFloat(matrix.m03());
        target.putFloat(matrix.m10());
        target.putFloat(matrix.m11());
        target.putFloat(matrix.m12());
        target.putFloat(matrix.m13());
        target.putFloat(matrix.m20());
        target.putFloat(matrix.m21());
        target.putFloat(matrix.m22());
        target.putFloat(matrix.m23());
        target.putFloat(matrix.m30());
        target.putFloat(matrix.m31());
        target.putFloat(matrix.m32());
        target.putFloat(matrix.m33());
    }
}
