package com.samo.engine.render.opengl.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.samo.engine.render.api.RenderPointLight;
import com.samo.engine.render.api.RenderSpotLight;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import org.junit.jupiter.api.Test;

class LocalLightUniformBlockTest {
    private static final float TOLERANCE = 1.0e-6f;

    @Test
    void packsPointAndSpotIntoFixedStd140ArraysAndZeroFillsUnusedEntries() {
        RenderPointLight point =
                new RenderPointLight(1.0f, 2.0f, 3.0f, 0.2f, 0.3f, 0.4f, 0.5f, 10.0f);
        RenderSpotLight spot = new RenderSpotLight(
                -1.0f,
                -2.0f,
                -3.0f,
                0.0f,
                0.0f,
                -2.0f,
                0.6f,
                0.7f,
                0.8f,
                0.9f,
                12.0f,
                0.2f,
                0.5f);

        ByteBuffer buffer = ByteBuffer.allocateDirect(LocalLightUniformBlock.SIZE_BYTES)
                .order(ByteOrder.nativeOrder());
        LocalLightUniformBlock.write(List.of(point, spot), buffer);
        assertEquals(LocalLightUniformBlock.SIZE_BYTES, buffer.position());

        assertVec4(
                buffer,
                LocalLightUniformBlock.POSITION_RANGE_OFFSET_BYTES,
                1.0f,
                2.0f,
                3.0f,
                10.0f);
        assertVec4(
                buffer,
                LocalLightUniformBlock.DIRECTION_TYPE_OFFSET_BYTES,
                0.0f,
                0.0f,
                0.0f,
                0.0f);
        assertVec4(
                buffer,
                LocalLightUniformBlock.COLOR_INTENSITY_OFFSET_BYTES,
                0.2f,
                0.3f,
                0.4f,
                0.5f);

        int spotStride = 4 * Float.BYTES;
        assertVec4(
                buffer,
                LocalLightUniformBlock.POSITION_RANGE_OFFSET_BYTES + spotStride,
                -1.0f,
                -2.0f,
                -3.0f,
                12.0f);
        assertVec4(
                buffer,
                LocalLightUniformBlock.DIRECTION_TYPE_OFFSET_BYTES + spotStride,
                0.0f,
                0.0f,
                -1.0f,
                1.0f);
        assertVec4(
                buffer,
                LocalLightUniformBlock.COLOR_INTENSITY_OFFSET_BYTES + spotStride,
                0.6f,
                0.7f,
                0.8f,
                0.9f);
        assertEquals(
                (float) Math.cos(0.2f),
                buffer.getFloat(LocalLightUniformBlock.CONE_COSINES_OFFSET_BYTES + spotStride),
                TOLERANCE);
        assertEquals(
                (float) Math.cos(0.5f),
                buffer.getFloat(
                        LocalLightUniformBlock.CONE_COSINES_OFFSET_BYTES
                                + spotStride
                                + Float.BYTES),
                TOLERANCE);

        assertEquals(2, buffer.getInt(LocalLightUniformBlock.META_OFFSET_BYTES));

        int unusedIndex = 2;
        assertVec4(
                buffer,
                LocalLightUniformBlock.POSITION_RANGE_OFFSET_BYTES + unusedIndex * spotStride,
                0.0f,
                0.0f,
                0.0f,
                0.0f);
        assertVec4(
                buffer,
                LocalLightUniformBlock.COLOR_INTENSITY_OFFSET_BYTES + unusedIndex * spotStride,
                0.0f,
                0.0f,
                0.0f,
                0.0f);
    }

    @Test
    void rewritesPreviousContentsWhenNextFrameHasNoLights() {
        ByteBuffer buffer = ByteBuffer.allocateDirect(LocalLightUniformBlock.SIZE_BYTES)
                .order(ByteOrder.nativeOrder());
        LocalLightUniformBlock.write(
                List.of(new RenderPointLight(
                        1.0f, 2.0f, 3.0f, 1.0f, 1.0f, 1.0f, 1.0f, 4.0f)),
                buffer);

        buffer.clear();
        LocalLightUniformBlock.write(List.of(), buffer);

        assertEquals(0, buffer.getInt(LocalLightUniformBlock.META_OFFSET_BYTES));
        for (int offset = 0; offset < LocalLightUniformBlock.SIZE_BYTES; offset += Integer.BYTES) {
            assertEquals(0, buffer.getInt(offset), "non-zero stale data at byte offset " + offset);
        }
    }

    private static void assertVec4(
            ByteBuffer buffer,
            int offset,
            float x,
            float y,
            float z,
            float w) {
        assertEquals(x, buffer.getFloat(offset), TOLERANCE);
        assertEquals(y, buffer.getFloat(offset + Float.BYTES), TOLERANCE);
        assertEquals(z, buffer.getFloat(offset + 2 * Float.BYTES), TOLERANCE);
        assertEquals(w, buffer.getFloat(offset + 3 * Float.BYTES), TOLERANCE);
    }
}
