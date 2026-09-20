package com.samo.engine.render.opengl.internal;

import com.samo.engine.render.api.RenderLocalLight;
import com.samo.engine.render.api.RenderPointLight;
import com.samo.engine.render.api.RenderSpotLight;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Objects;

final class LocalLightUniformBlock {
    static final String GLSL_BLOCK_NAME = "LocalLightBlock";
    static final int BINDING = 2;
    static final int CAPACITY = LocalLightSelector.SHADER_CAPACITY;

    static final int POSITION_RANGE_OFFSET_BYTES = 0;
    static final int DIRECTION_TYPE_OFFSET_BYTES = POSITION_RANGE_OFFSET_BYTES + CAPACITY * 4 * Float.BYTES;
    static final int COLOR_INTENSITY_OFFSET_BYTES = DIRECTION_TYPE_OFFSET_BYTES + CAPACITY * 4 * Float.BYTES;
    static final int CONE_COSINES_OFFSET_BYTES = COLOR_INTENSITY_OFFSET_BYTES + CAPACITY * 4 * Float.BYTES;
    static final int META_OFFSET_BYTES = CONE_COSINES_OFFSET_BYTES + CAPACITY * 4 * Float.BYTES;
    static final int SIZE_BYTES = META_OFFSET_BYTES + 4 * Integer.BYTES;

    private LocalLightUniformBlock() {
    }

    static void write(List<RenderLocalLight> lights, ByteBuffer destination) {
        List<RenderLocalLight> values = Objects.requireNonNull(lights, "lights");
        ByteBuffer buffer = Objects.requireNonNull(destination, "destination");
        if (values.size() > CAPACITY) {
            throw new IllegalArgumentException("local light count exceeds fixed block capacity");
        }
        if (buffer.remaining() < SIZE_BYTES) {
            throw new IllegalArgumentException("destination must have at least " + SIZE_BYTES + " bytes remaining");
        }

        int base = buffer.position();
        for (int offset = 0; offset < SIZE_BYTES; offset += Integer.BYTES) {
            buffer.putInt(base + offset, 0);
        }

        for (int index = 0; index < values.size(); index++) {
            RenderLocalLight light = Objects.requireNonNull(values.get(index), "light");
            putVec4(
                    buffer,
                    base + POSITION_RANGE_OFFSET_BYTES + index * 4 * Float.BYTES,
                    light.positionX(),
                    light.positionY(),
                    light.positionZ(),
                    light.rangeMeters());
            putVec4(
                    buffer,
                    base + COLOR_INTENSITY_OFFSET_BYTES + index * 4 * Float.BYTES,
                    light.red(),
                    light.green(),
                    light.blue(),
                    light.intensity());

            if (light instanceof RenderSpotLight spot) {
                putVec4(
                        buffer,
                        base + DIRECTION_TYPE_OFFSET_BYTES + index * 4 * Float.BYTES,
                        spot.directionX(),
                        spot.directionY(),
                        spot.directionZ(),
                        1.0f);
                putVec4(
                        buffer,
                        base + CONE_COSINES_OFFSET_BYTES + index * 4 * Float.BYTES,
                        (float) Math.cos(spot.innerConeRadians()),
                        (float) Math.cos(spot.outerConeRadians()),
                        0.0f,
                        0.0f);
            } else if (light instanceof RenderPointLight) {
                putVec4(
                        buffer,
                        base + DIRECTION_TYPE_OFFSET_BYTES + index * 4 * Float.BYTES,
                        0.0f,
                        0.0f,
                        0.0f,
                        0.0f);
            } else {
                throw new IllegalArgumentException(
                        "Unsupported RenderLocalLight implementation: " + light.getClass().getName());
            }
        }

        buffer.putInt(base + META_OFFSET_BYTES, values.size());
        buffer.position(base + SIZE_BYTES);
    }

    private static void putVec4(
            ByteBuffer buffer,
            int offset,
            float x,
            float y,
            float z,
            float w) {
        buffer.putFloat(offset, x);
        buffer.putFloat(offset + Float.BYTES, y);
        buffer.putFloat(offset + 2 * Float.BYTES, z);
        buffer.putFloat(offset + 3 * Float.BYTES, w);
    }
}
