package com.samo.engine.render.opengl.internal;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;

final class MaterialUniformBlock {
    static final String GLSL_BLOCK_NAME = "MaterialBlock";
    static final int BINDING = 2;
    static final int COLOR_MULTIPLIER_OFFSET_BYTES = 0;
    static final int SIZE_BYTES = 16;

    private MaterialUniformBlock() {
    }

    static void write(MaterialScalars scalars, ByteBuffer destination) {
        MaterialScalars value = Objects.requireNonNull(scalars, "scalars");
        ByteBuffer output = Objects.requireNonNull(destination, "destination");
        if (output.remaining() < SIZE_BYTES) {
            throw new IllegalArgumentException(
                    "destination requires at least " + SIZE_BYTES + " remaining bytes");
        }

        int start = output.position();
        ByteBuffer target = output.duplicate().order(ByteOrder.nativeOrder());
        target.position(start);
        target.putFloat(value.redMultiplier());
        target.putFloat(value.greenMultiplier());
        target.putFloat(value.blueMultiplier());
        target.putFloat(value.alphaMultiplier());
        output.position(start + SIZE_BYTES);
    }
}
