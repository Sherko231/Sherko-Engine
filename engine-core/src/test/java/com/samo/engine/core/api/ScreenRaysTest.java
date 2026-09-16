package com.samo.engine.core.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

final class ScreenRaysTest {
    private static final float EPSILON = 1.0e-5f;

    @Test
    void centerPixelProducesNearPlaneForwardRay() {
        Ray3f ray = ScreenRays.worldRay(
                1.5f, 1.5f, 0, 0, 3, 3, new Matrix4f(), perspective90());

        assertRay(ray, 0, 0, -1, 0, 0, -1);
    }

    @Test
    void oddSizedCenterPixelCenterMapsExactlyToForward() {
        Ray3f ray = ScreenRays.worldRay(
                2.5f, 3.5f, 0, 0, 5, 7, new Matrix4f(), perspective90());

        assertRay(ray, 0, 0, -1, 0, 0, -1);
    }

    @Test
    void topAndBottomBoundariesUseTopLeftOriginAndDownwardScreenY() {
        Ray3f top = ScreenRays.worldRay(
                1.5f, 0.0f, 0, 0, 3, 3, new Matrix4f(), perspective90());
        Ray3f bottom = ScreenRays.worldRay(
                1.5f, 3.0f, 0, 0, 3, 3, new Matrix4f(), perspective90());

        float inverseSqrt2 = (float) (1.0 / Math.sqrt(2.0));
        assertRay(top, 0, 1, -1, 0, inverseSqrt2, -inverseSqrt2);
        assertRay(bottom, 0, -1, -1, 0, -inverseSqrt2, -inverseSqrt2);
    }

    @Test
    void leftAndRightBoundariesMapToHorizontalFrustumEdges() {
        Ray3f left = ScreenRays.worldRay(
                0.0f, 1.5f, 0, 0, 3, 3, new Matrix4f(), perspective90());
        Ray3f right = ScreenRays.worldRay(
                3.0f, 1.5f, 0, 0, 3, 3, new Matrix4f(), perspective90());

        float inverseSqrt2 = (float) (1.0 / Math.sqrt(2.0));
        assertRay(left, -1, 0, -1, -inverseSqrt2, 0, -inverseSqrt2);
        assertRay(right, 1, 0, -1, inverseSqrt2, 0, -inverseSqrt2);
    }

    @Test
    void offsetViewportPreservesLocalMapping() {
        Ray3f originViewport = ScreenRays.worldRay(
                3.0f, 0.0f, 0, 0, 3, 3, new Matrix4f(), perspective90());
        Ray3f offsetViewport = ScreenRays.worldRay(
                13.0f, 20.0f, 10, 20, 3, 3, new Matrix4f(), perspective90());

        assertSameRay(originViewport, offsetViewport);
    }

    @Test
    void rotatedCameraFacingPositiveXProducesPositiveXCenterRay() {
        Matrix4f view = new Matrix4f().set(
                0, 0, -1, 0,
                0, 1, 0, 0,
                1, 0, 0, 0,
                0, 0, 0, 1);

        Ray3f ray = ScreenRays.worldRay(
                1.5f, 1.5f, 0, 0, 3, 3, view, perspective90());

        assertRay(ray, 1, 0, 0, 1, 0, 0);
    }

    @Test
    void inputMatricesRemainUnchangedOnSuccessAndFailure() {
        Matrix4f view = new Matrix4f().translation(2, 3, 4);
        Matrix4f projection = perspective90();
        Matrix4f originalView = new Matrix4f(view);
        Matrix4f originalProjection = new Matrix4f(projection);

        ScreenRays.worldRay(1.5f, 1.5f, 0, 0, 3, 3, view, projection);
        assertMatrixEquals(originalView, view);
        assertMatrixEquals(originalProjection, projection);

        assertThrows(IllegalArgumentException.class,
                () -> ScreenRays.worldRay(-1, 1.5f, 0, 0, 3, 3, view, projection));
        assertMatrixEquals(originalView, view);
        assertMatrixEquals(originalProjection, projection);
    }

    @Test
    void invalidInputsRejectPredictably() {
        Matrix4f identity = new Matrix4f();
        Matrix4f projection = perspective90();

        assertThrows(NullPointerException.class,
                () -> ScreenRays.worldRay(1, 1, 0, 0, 3, 3, null, projection));
        assertThrows(NullPointerException.class,
                () -> ScreenRays.worldRay(1, 1, 0, 0, 3, 3, identity, null));
        assertThrows(IllegalArgumentException.class,
                () -> ScreenRays.worldRay(Float.NaN, 1, 0, 0, 3, 3, identity, projection));
        assertThrows(IllegalArgumentException.class,
                () -> ScreenRays.worldRay(1, Float.POSITIVE_INFINITY, 0, 0, 3, 3, identity, projection));
        assertThrows(IllegalArgumentException.class,
                () -> ScreenRays.worldRay(1, 1, 0, 0, 0, 3, identity, projection));
        assertThrows(IllegalArgumentException.class,
                () -> ScreenRays.worldRay(1, 1, 0, 0, 3, -1, identity, projection));
        assertThrows(IllegalArgumentException.class,
                () -> ScreenRays.worldRay(-0.01f, 1, 0, 0, 3, 3, identity, projection));
        assertThrows(IllegalArgumentException.class,
                () -> ScreenRays.worldRay(3.01f, 1, 0, 0, 3, 3, identity, projection));

        Matrix4f nonFinite = new Matrix4f().m00(Float.NaN);
        assertThrows(IllegalArgumentException.class,
                () -> ScreenRays.worldRay(1, 1, 0, 0, 3, 3, nonFinite, projection));
        assertThrows(IllegalArgumentException.class,
                () -> ScreenRays.worldRay(1, 1, 0, 0, 3, 3, identity, nonFinite));

        Matrix4f singular = new Matrix4f().zero();
        assertThrows(IllegalArgumentException.class,
                () -> ScreenRays.worldRay(1, 1, 0, 0, 3, 3, identity, singular));
    }

    @Test
    void invalidHomogeneousDivisionIsRejected() {
        Matrix4f inverseViewProjection = new Matrix4f().identity().m23(1.0f).m33(1.0f);
        Matrix4f projection = new Matrix4f(inverseViewProjection).invert();

        assertThrows(IllegalArgumentException.class,
                () -> ScreenRays.worldRay(1.5f, 1.5f, 0, 0, 3, 3, new Matrix4f(), projection));
    }

    @Test
    void constructedRayWorksWithExistingGeometryQueries() {
        Ray3f ray = ScreenRays.worldRay(
                1.5f, 1.5f, 0, 0, 3, 3, new Matrix4f(), perspective90());
        Sphere3f sphere = new Sphere3f(new Vector3f(0, 0, -5), 1.0f);

        assertEquals(3.0f, ray.intersectSphere(sphere), EPSILON);
    }

    private static Matrix4f perspective90() {
        return new Matrix4f().set(
                1, 0, 0, 0,
                0, 1, 0, 0,
                0, 0, -1.2f, -1,
                0, 0, -2.2f, 0);
    }

    private static void assertRay(
            Ray3f ray,
            float originX,
            float originY,
            float originZ,
            float directionX,
            float directionY,
            float directionZ) {
        Vector3f origin = ray.origin(new Vector3f());
        Vector3f direction = ray.direction(new Vector3f());
        assertEquals(originX, origin.x, EPSILON);
        assertEquals(originY, origin.y, EPSILON);
        assertEquals(originZ, origin.z, EPSILON);
        assertEquals(directionX, direction.x, EPSILON);
        assertEquals(directionY, direction.y, EPSILON);
        assertEquals(directionZ, direction.z, EPSILON);
    }

    private static void assertSameRay(Ray3f expected, Ray3f actual) {
        Vector3f expectedOrigin = expected.origin(new Vector3f());
        Vector3f actualOrigin = actual.origin(new Vector3f());
        Vector3f expectedDirection = expected.direction(new Vector3f());
        Vector3f actualDirection = actual.direction(new Vector3f());
        assertEquals(expectedOrigin.x, actualOrigin.x, EPSILON);
        assertEquals(expectedOrigin.y, actualOrigin.y, EPSILON);
        assertEquals(expectedOrigin.z, actualOrigin.z, EPSILON);
        assertEquals(expectedDirection.x, actualDirection.x, EPSILON);
        assertEquals(expectedDirection.y, actualDirection.y, EPSILON);
        assertEquals(expectedDirection.z, actualDirection.z, EPSILON);
    }

    private static void assertMatrixEquals(Matrix4f expected, Matrix4f actual) {
        assertEquals(expected.m00(), actual.m00(), 0.0f);
        assertEquals(expected.m01(), actual.m01(), 0.0f);
        assertEquals(expected.m02(), actual.m02(), 0.0f);
        assertEquals(expected.m03(), actual.m03(), 0.0f);
        assertEquals(expected.m10(), actual.m10(), 0.0f);
        assertEquals(expected.m11(), actual.m11(), 0.0f);
        assertEquals(expected.m12(), actual.m12(), 0.0f);
        assertEquals(expected.m13(), actual.m13(), 0.0f);
        assertEquals(expected.m20(), actual.m20(), 0.0f);
        assertEquals(expected.m21(), actual.m21(), 0.0f);
        assertEquals(expected.m22(), actual.m22(), 0.0f);
        assertEquals(expected.m23(), actual.m23(), 0.0f);
        assertEquals(expected.m30(), actual.m30(), 0.0f);
        assertEquals(expected.m31(), actual.m31(), 0.0f);
        assertEquals(expected.m32(), actual.m32(), 0.0f);
        assertEquals(expected.m33(), actual.m33(), 0.0f);
    }
}
