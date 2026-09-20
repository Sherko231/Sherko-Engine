package com.samo.engine.render.opengl.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;

class ViewModelProjectionFactoryTest {
    private static final float TOLERANCE = 1.0e-6f;

    @Test
    void buildsIndependentDocumentedPerspectiveProjection() {
        Matrix4f projection = ViewModelProjectionFactory.build(1600, 900, new Matrix4f());

        float aspect = 1600.0f / 900.0f;
        float focal = (float) (1.0 / Math.tan(Math.toRadians(55.0) * 0.5));
        float near = 0.01f;
        float far = 10.0f;
        float denominator = near - far;

        assertEquals(focal / aspect, projection.m00(), TOLERANCE);
        assertEquals(focal, projection.m11(), TOLERANCE);
        assertEquals((far + near) / denominator, projection.m22(), TOLERANCE);
        assertEquals(-1.0f, projection.m23(), TOLERANCE);
        assertEquals((2.0f * far * near) / denominator, projection.m32(), TOLERANCE);
        assertEquals(0.0f, projection.m33(), TOLERANCE);
    }

    @Test
    void rejectsInvalidFramebufferDimensions() {
        assertThrows(IllegalArgumentException.class, () -> ViewModelProjectionFactory.build(0, 900, new Matrix4f()));
        assertThrows(IllegalArgumentException.class, () -> ViewModelProjectionFactory.build(1600, 0, new Matrix4f()));
    }
}
