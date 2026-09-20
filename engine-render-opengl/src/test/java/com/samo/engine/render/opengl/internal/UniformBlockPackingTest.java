package com.samo.engine.render.opengl.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;

class UniformBlockPackingTest {
    @Test
    void cameraMatricesPackerWritesHandwrittenStd140Offsets() {
        Matrix4f view = new Matrix4f().set(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16);
        Matrix4f projection = new Matrix4f().set(21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36);
        ByteBuffer buffer = ByteBuffer.allocateDirect(CameraMatricesUniformBlock.SIZE_BYTES + 8).order(ByteOrder.nativeOrder());
        buffer.position(4);

        CameraMatricesUniformBlock.write(view, projection, buffer);

        assertEquals(4 + CameraMatricesUniformBlock.SIZE_BYTES, buffer.position());
        assertEquals(1.0f, buffer.getFloat(4 + CameraMatricesUniformBlock.VIEW_OFFSET_BYTES));
        assertEquals(16.0f, buffer.getFloat(4 + CameraMatricesUniformBlock.VIEW_OFFSET_BYTES + 60));
        assertEquals(21.0f, buffer.getFloat(4 + CameraMatricesUniformBlock.PROJECTION_OFFSET_BYTES));
        assertEquals(36.0f, buffer.getFloat(4 + CameraMatricesUniformBlock.PROJECTION_OFFSET_BYTES + 60));
    }

    @Test
    void framebufferMetricsPackerWritesFramebufferAndInverseAtOffsetZero() {
        ByteBuffer buffer = ByteBuffer.allocateDirect(FramebufferMetricsUniformBlock.SIZE_BYTES).order(ByteOrder.nativeOrder());

        FramebufferMetricsUniformBlock.write(1920, 1080, buffer);

        assertEquals(FramebufferMetricsUniformBlock.SIZE_BYTES, buffer.position());
        assertEquals(1920.0f, buffer.getFloat(0));
        assertEquals(1080.0f, buffer.getFloat(4));
        assertEquals(1.0f / 1920.0f, buffer.getFloat(8));
        assertEquals(1.0f / 1080.0f, buffer.getFloat(12));
    }

    @Test
    void invalidInputsFailBeforeDestinationPositionChanges() {
        ByteBuffer camera = ByteBuffer.allocateDirect(CameraMatricesUniformBlock.SIZE_BYTES - 1);
        int cameraStart = camera.position();
        assertThrows(IllegalArgumentException.class, () -> CameraMatricesUniformBlock.write(new Matrix4f(), new Matrix4f(), camera));
        assertEquals(cameraStart, camera.position());

        ByteBuffer framebufferMetrics = ByteBuffer.allocateDirect(FramebufferMetricsUniformBlock.SIZE_BYTES);
        int frameStart = framebufferMetrics.position();
        assertThrows(IllegalArgumentException.class, () -> FramebufferMetricsUniformBlock.write(0, 1080, framebufferMetrics));
        assertEquals(frameStart, framebufferMetrics.position());
    }

    @Test
    void blockConstantsMatchExpectedAbi() {
        assertEquals(0, CameraMatricesUniformBlock.BINDING);
        assertEquals(0, CameraMatricesUniformBlock.VIEW_OFFSET_BYTES);
        assertEquals(64, CameraMatricesUniformBlock.PROJECTION_OFFSET_BYTES);
        assertEquals(128, CameraMatricesUniformBlock.SIZE_BYTES);

        assertEquals(1, FramebufferMetricsUniformBlock.BINDING);
        assertEquals(0, FramebufferMetricsUniformBlock.FRAMEBUFFER_SIZE_AND_INVERSE_OFFSET_BYTES);
        assertEquals(16, FramebufferMetricsUniformBlock.SIZE_BYTES);
    }
}
