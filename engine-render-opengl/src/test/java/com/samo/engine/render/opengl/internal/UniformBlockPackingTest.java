package com.samo.engine.render.opengl.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;

class UniformBlockPackingTest {
    @Test
    void cameraBlockWritesHandwrittenStd140Offsets() {
        Matrix4f view = new Matrix4f().set(
                1, 2, 3, 4,
                5, 6, 7, 8,
                9, 10, 11, 12,
                13, 14, 15, 16);
        Matrix4f projection = new Matrix4f().set(
                21, 22, 23, 24,
                25, 26, 27, 28,
                29, 30, 31, 32,
                33, 34, 35, 36);
        ByteBuffer buffer = ByteBuffer.allocateDirect(CameraUniformBlock.SIZE_BYTES + 8)
                .order(ByteOrder.nativeOrder());
        buffer.position(4);

        CameraUniformBlock.write(view, projection, buffer);

        assertEquals(4 + CameraUniformBlock.SIZE_BYTES, buffer.position());
        assertEquals(1.0f, buffer.getFloat(4 + CameraUniformBlock.VIEW_OFFSET_BYTES));
        assertEquals(16.0f, buffer.getFloat(4 + CameraUniformBlock.VIEW_OFFSET_BYTES + 60));
        assertEquals(21.0f, buffer.getFloat(4 + CameraUniformBlock.PROJECTION_OFFSET_BYTES));
        assertEquals(36.0f, buffer.getFloat(4 + CameraUniformBlock.PROJECTION_OFFSET_BYTES + 60));
    }

    @Test
    void perFrameBlockWritesFramebufferAndInverseAtOffsetZero() {
        ByteBuffer buffer = ByteBuffer.allocateDirect(PerFrameUniformBlock.SIZE_BYTES)
                .order(ByteOrder.nativeOrder());

        PerFrameUniformBlock.write(1920, 1080, buffer);

        assertEquals(PerFrameUniformBlock.SIZE_BYTES, buffer.position());
        assertEquals(1920.0f, buffer.getFloat(0));
        assertEquals(1080.0f, buffer.getFloat(4));
        assertEquals(1.0f / 1920.0f, buffer.getFloat(8));
        assertEquals(1.0f / 1080.0f, buffer.getFloat(12));
    }

    @Test
    void invalidInputsFailBeforeDestinationPositionChanges() {
        ByteBuffer camera = ByteBuffer.allocateDirect(CameraUniformBlock.SIZE_BYTES - 1);
        int cameraStart = camera.position();
        assertThrows(
                IllegalArgumentException.class,
                () -> CameraUniformBlock.write(new Matrix4f(), new Matrix4f(), camera));
        assertEquals(cameraStart, camera.position());

        ByteBuffer perFrame = ByteBuffer.allocateDirect(PerFrameUniformBlock.SIZE_BYTES);
        int frameStart = perFrame.position();
        assertThrows(
                IllegalArgumentException.class,
                () -> PerFrameUniformBlock.write(0, 1080, perFrame));
        assertEquals(frameStart, perFrame.position());
    }

    @Test
    void blockConstantsMatchExpectedAbi() {
        assertEquals(0, CameraUniformBlock.BINDING);
        assertEquals(0, CameraUniformBlock.VIEW_OFFSET_BYTES);
        assertEquals(64, CameraUniformBlock.PROJECTION_OFFSET_BYTES);
        assertEquals(128, CameraUniformBlock.SIZE_BYTES);

        assertEquals(1, PerFrameUniformBlock.BINDING);
        assertEquals(0, PerFrameUniformBlock.FRAMEBUFFER_SIZE_AND_INVERSE_OFFSET_BYTES);
        assertEquals(16, PerFrameUniformBlock.SIZE_BYTES);
    }
}
