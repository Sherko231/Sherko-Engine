package com.samo.engine.core.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.junit.jupiter.api.Test;

final class CameraMatricesTest {
    private static final float EPSILON = 1.0e-5f;

    @Test
    void identityBasisProducesIdentityView() {
        Matrix4f destination = new Matrix4f().zero();
        assertSame(destination, CameraMatrices.view(
                new Vector3f(0, 0, 0),
                new Vector3f(0, 0, -1),
                new Vector3f(0, 1, 0),
                destination));
        assertMatrixIdentity(destination);
    }

    @Test
    void translatedCameraMapsPositionToOriginAndForwardToNegativeZ() {
        Matrix4f view = CameraMatrices.view(
                new Vector3f(10, 2, 3),
                new Vector3f(0, 0, -2),
                new Vector3f(0, 3, 0),
                new Matrix4f());

        assertTransformed(view, new Vector3f(10, 2, 3), 0, 0, 0);
        assertTransformed(view, new Vector3f(10, 2, 1), 0, 0, -2);
        assertTransformed(view, new Vector3f(11, 2, 3), 1, 0, 0);
        assertTransformed(view, new Vector3f(10, 3, 3), 0, 1, 0);
    }

    @Test
    void rotatedCameraFacingPositiveXPreservesRightHandedViewAxes() {
        Matrix4f view = CameraMatrices.view(
                new Vector3f(10, 2, 3),
                new Vector3f(2, 0, 0),
                new Vector3f(0, 5, 0),
                new Matrix4f());

        assertTransformed(view, new Vector3f(12, 2, 3), 0, 0, -2);
        assertTransformed(view, new Vector3f(10, 3, 3), 0, 1, 0);
        assertTransformed(view, new Vector3f(10, 2, 4), 1, 0, 0);
    }

    @Test
    void viewOrthogonalizesNonOrthogonalUpInput() {
        Matrix4f view = CameraMatrices.view(
                new Vector3f(),
                new Vector3f(0, 0, -4),
                new Vector3f(0, 2, -1),
                new Matrix4f());

        assertTransformed(view, new Vector3f(0, 0, -3), 0, 0, -3);
        assertTransformed(view, new Vector3f(0, 2, 0), 0, 2, 0);
        assertTransformed(view, new Vector3f(2, 0, 0), 2, 0, 0);
    }

    @Test
    void invalidViewInputsRejectBeforeDestinationMutation() {
        Matrix4f destination = sentinelMatrix();

        assertThrows(NullPointerException.class,
                () -> CameraMatrices.view(null, new Vector3f(0, 0, -1), new Vector3f(0, 1, 0), destination));
        assertSentinel(destination);
        assertThrows(IllegalArgumentException.class,
                () -> CameraMatrices.view(new Vector3f(), new Vector3f(), new Vector3f(0, 1, 0), destination));
        assertSentinel(destination);
        assertThrows(IllegalArgumentException.class,
                () -> CameraMatrices.view(new Vector3f(), new Vector3f(0, 0, -1), new Vector3f(0, 0, 2), destination));
        assertSentinel(destination);
        assertThrows(IllegalArgumentException.class,
                () -> CameraMatrices.view(new Vector3f(Float.NaN, 0, 0), new Vector3f(0, 0, -1), new Vector3f(0, 1, 0), destination));
        assertSentinel(destination);
    }

    @Test
    void perspectiveMapsNearAndFarToOpenGlDepthRange() {
        Matrix4f projection = CameraMatrices.perspective(
                (float) (Math.PI / 2.0), 2.0f, 1.0f, 11.0f, new Matrix4f());

        assertNdc(projection, 0, 0, -1, 0, 0, -1);
        assertNdc(projection, 0, 0, -11, 0, 0, 1);
    }

    @Test
    void perspectiveMapsAnalyticalVerticalAndHorizontalEdges() {
        Matrix4f projection = CameraMatrices.perspective(
                (float) (Math.PI / 2.0), 2.0f, 1.0f, 11.0f, new Matrix4f());

        assertNdc(projection, 2, 0, -1, 1, 0, -1);
        assertNdc(projection, -2, 0, -1, -1, 0, -1);
        assertNdc(projection, 0, 1, -1, 0, 1, -1);
        assertNdc(projection, 0, -1, -1, 0, -1, -1);
    }

    @Test
    void invalidPerspectiveInputsRejectBeforeDestinationMutation() {
        Matrix4f destination = sentinelMatrix();

        assertThrows(IllegalArgumentException.class,
                () -> CameraMatrices.perspective(0, 1, 1, 10, destination));
        assertSentinel(destination);
        assertThrows(IllegalArgumentException.class,
                () -> CameraMatrices.perspective((float) Math.PI, 1, 1, 10, destination));
        assertSentinel(destination);
        assertThrows(IllegalArgumentException.class,
                () -> CameraMatrices.perspective(1, 0, 1, 10, destination));
        assertSentinel(destination);
        assertThrows(IllegalArgumentException.class,
                () -> CameraMatrices.perspective(1, 1, 0, 10, destination));
        assertSentinel(destination);
        assertThrows(IllegalArgumentException.class,
                () -> CameraMatrices.perspective(1, 1, 10, 10, destination));
        assertSentinel(destination);
        assertThrows(IllegalArgumentException.class,
                () -> CameraMatrices.perspective(Float.NaN, 1, 1, 10, destination));
        assertSentinel(destination);
    }

    @Test
    void reusedDestinationIsFullyReplaced() {
        Matrix4f destination = sentinelMatrix();
        assertSame(destination, CameraMatrices.perspective(
                (float) (Math.PI / 2.0), 1.0f, 1.0f, 3.0f, destination));
        assertEquals(0.0f, destination.m01(), 0.0f);
        assertEquals(0.0f, destination.m10(), 0.0f);
        assertEquals(0.0f, destination.m33(), 0.0f);

        CameraMatrices.view(
                new Vector3f(),
                new Vector3f(0, 0, -1),
                new Vector3f(0, 1, 0),
                destination);
        assertMatrixIdentity(destination);
    }

    private static void assertTransformed(
            Matrix4f matrix, Vector3f point, float expectedX, float expectedY, float expectedZ) {
        Vector3f transformed = matrix.transformPosition(point, new Vector3f());
        assertEquals(expectedX, transformed.x, EPSILON);
        assertEquals(expectedY, transformed.y, EPSILON);
        assertEquals(expectedZ, transformed.z, EPSILON);
    }

    private static void assertNdc(
            Matrix4f projection,
            float x,
            float y,
            float z,
            float expectedX,
            float expectedY,
            float expectedZ) {
        Vector4f clip = projection.transform(new Vector4f(x, y, z, 1.0f));
        assertEquals(expectedX, clip.x / clip.w, EPSILON);
        assertEquals(expectedY, clip.y / clip.w, EPSILON);
        assertEquals(expectedZ, clip.z / clip.w, EPSILON);
    }

    private static Matrix4f sentinelMatrix() {
        return new Matrix4f().set(
                1, 2, 3, 4,
                5, 6, 7, 8,
                9, 10, 11, 12,
                13, 14, 15, 16);
    }

    private static void assertSentinel(Matrix4f matrix) {
        assertEquals(1.0f, matrix.m00(), 0.0f);
        assertEquals(6.0f, matrix.m11(), 0.0f);
        assertEquals(11.0f, matrix.m22(), 0.0f);
        assertEquals(16.0f, matrix.m33(), 0.0f);
        assertEquals(13.0f, matrix.m30(), 0.0f);
    }

    private static void assertMatrixIdentity(Matrix4f matrix) {
        assertEquals(1.0f, matrix.m00(), EPSILON);
        assertEquals(1.0f, matrix.m11(), EPSILON);
        assertEquals(1.0f, matrix.m22(), EPSILON);
        assertEquals(1.0f, matrix.m33(), EPSILON);
        assertEquals(0.0f, matrix.m01(), EPSILON);
        assertEquals(0.0f, matrix.m10(), EPSILON);
        assertEquals(0.0f, matrix.m20(), EPSILON);
        assertEquals(0.0f, matrix.m30(), EPSILON);
        assertEquals(0.0f, matrix.m31(), EPSILON);
        assertEquals(0.0f, matrix.m32(), EPSILON);
    }
}
