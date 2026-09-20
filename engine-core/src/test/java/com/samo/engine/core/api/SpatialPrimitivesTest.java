package com.samo.engine.core.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

final class SpatialPrimitivesTest {
    private static final float EPSILON = 1.0e-5f;

    @Test
    void rayNormalizesDirectionAndEvaluatesPoints() {

        Ray3f ray = new Ray3f(new Vector3f(1, 2, 3), new Vector3f(0, 0, -4));
        assertVector(ray.direction(new Vector3f()), 0, 0, -1);
        assertVector(ray.pointAt(2.5f, new Vector3f()), 1, 2, 0.5f);
        assertThrows(IllegalArgumentException.class, () -> new Ray3f(new Vector3f(), new Vector3f()));
        assertThrows(IllegalArgumentException.class, () -> ray.pointAt(-1, new Vector3f()));

    }

    @Test
    void rayPlaneCoversHitContactAndParallelMiss() {

        Plane3f plane = Plane3f.fromPointNormal(new Vector3f(0, 0, -5), new Vector3f(0, 0, 2));
        assertEquals(5.0f, new Ray3f(new Vector3f(), new Vector3f(0, 0, -1)).intersectPlane(plane), EPSILON);
        assertEquals(0.0f, new Ray3f(new Vector3f(0, 0, -5), new Vector3f(1, 0, 0)).intersectPlane(plane));
        assertTrue(Float.isNaN(new Ray3f(new Vector3f(), new Vector3f(1, 0, 0)).intersectPlane(plane)));

    }

    @Test
    void raySphereCoversOutsideTangentInsideAndMiss() {

        Sphere3f sphere = new Sphere3f(new Vector3f(0, 0, -5), 1);
        assertEquals(4.0f, new Ray3f(new Vector3f(), new Vector3f(0, 0, -1)).intersectSphere(sphere), EPSILON);
        assertEquals(5.0f, new Ray3f(new Vector3f(0, 1, 0), new Vector3f(0, 0, -1)).intersectSphere(sphere), EPSILON);
        assertEquals(0.0f, new Ray3f(new Vector3f(0, 0, -5), new Vector3f(1, 0, 0)).intersectSphere(sphere));
        assertTrue(Float.isNaN(new Ray3f(new Vector3f(0, 2, 0), new Vector3f(0, 0, -1)).intersectSphere(sphere)));

    }

    @Test
    void rayAabbCoversHitContactInsideAndMiss() {

        Aabb3f box = new Aabb3f(new Vector3f(-1, -1, -6), new Vector3f(1, 1, -4));
        assertEquals(4.0f, new Ray3f(new Vector3f(), new Vector3f(0, 0, -1)).intersectAabb(box), EPSILON);
        assertEquals(0.0f, new Ray3f(new Vector3f(1, 0, -5), new Vector3f(1, 0, 0)).intersectAabb(box));
        assertEquals(0.0f, new Ray3f(new Vector3f(0, 0, -5), new Vector3f(1, 0, 0)).intersectAabb(box));
        assertTrue(Float.isNaN(new Ray3f(new Vector3f(2, 0, 0), new Vector3f(0, 0, -1)).intersectAabb(box)));

    }

    @Test
    void planeNormalizesAndPreservesSignedSideSemantics() {

        Plane3f plane = new Plane3f(new Vector3f(0, 2, 0), -4);
        assertVector(plane.normal(new Vector3f()), 0, 1, 0);
        assertEquals(-2.0f, plane.offset(), EPSILON);
        assertEquals(1.0f, plane.signedDistance(new Vector3f(0, 3, 0)), EPSILON);
        assertEquals(0.0f, plane.signedDistance(new Vector3f(0, 2, 0)), EPSILON);
        assertEquals(-1.0f, plane.signedDistance(new Vector3f(0, 1, 0)), EPSILON);

        Plane3f fromPoint = Plane3f.fromPointNormal(new Vector3f(0, 2, 0), new Vector3f(0, 5, 0));
        assertEquals(0.0f, fromPoint.signedDistance(new Vector3f(4, 2, -7)), EPSILON);
        assertThrows(IllegalArgumentException.class, () -> new Plane3f(new Vector3f(), 0));

    }

    @Test
    void sphereUsesInclusivePointSphereAndAabbBoundaries() {

        Sphere3f sphere = new Sphere3f(new Vector3f(), 2);
        assertTrue(sphere.containsPoint(new Vector3f(0, 0, 0)));
        assertTrue(sphere.containsPoint(new Vector3f(2, 0, 0)));
        assertFalse(sphere.containsPoint(new Vector3f(2.01f, 0, 0)));
        assertTrue(sphere.intersects(new Sphere3f(new Vector3f(4, 0, 0), 2)));
        assertFalse(sphere.intersects(new Sphere3f(new Vector3f(4.01f, 0, 0), 2)));
        assertTrue(sphere.intersects(new Aabb3f(new Vector3f(2, -1, -1), new Vector3f(3, 1, 1))));
        assertFalse(sphere.intersects(new Aabb3f(new Vector3f(2.01f, -1, -1), new Vector3f(3, 1, 1))));
        assertTrue(new Sphere3f(new Vector3f(1, 2, 3), 0).containsPoint(new Vector3f(1, 2, 3)));
        assertThrows(IllegalArgumentException.class, () -> new Sphere3f(new Vector3f(), -0.1f));

    }

    @Test
    void aabbUsesInclusivePointBoxAndSphereBoundaries() {

        Aabb3f box = new Aabb3f(new Vector3f(-1, -1, -1), new Vector3f(1, 1, 1));
        assertTrue(box.containsPoint(new Vector3f()));
        assertTrue(box.containsPoint(new Vector3f(1, 0, 0)));
        assertFalse(box.containsPoint(new Vector3f(1.01f, 0, 0)));
        assertTrue(box.intersects(new Aabb3f(new Vector3f(1, -1, -1), new Vector3f(2, 1, 1))));
        assertFalse(box.intersects(new Aabb3f(new Vector3f(1.01f, -1, -1), new Vector3f(2, 1, 1))));
        assertTrue(box.intersects(new Sphere3f(new Vector3f(2, 0, 0), 1)));
        assertFalse(box.intersects(new Sphere3f(new Vector3f(2.01f, 0, 0), 1)));
        assertTrue(new Aabb3f(new Vector3f(1, 1, 1), new Vector3f(1, 1, 1)).containsPoint(new Vector3f(1, 1, 1)));
        assertThrows(IllegalArgumentException.class, () -> new Aabb3f(new Vector3f(2, 0, 0), new Vector3f(1, 1, 1)));

    }

    @Test
    void frustumClassifiesInsideContactAndOutsideAnalytically() {

        Frustum3f frustum = unitBoxFrustum();
        assertTrue(frustum.containsPoint(new Vector3f()));
        assertTrue(frustum.containsPoint(new Vector3f(1, 0, 0)));
        assertFalse(frustum.containsPoint(new Vector3f(1.01f, 0, 0)));

        assertTrue(frustum.intersects(new Sphere3f(new Vector3f(2, 0, 0), 1)));
        assertFalse(frustum.intersects(new Sphere3f(new Vector3f(2.01f, 0, 0), 1)));

        assertTrue(frustum.intersects(new Aabb3f(new Vector3f(-0.5f, -0.5f, -1.5f), new Vector3f(0.5f, 0.5f, -1))));
        assertFalse(frustum.intersects(new Aabb3f(new Vector3f(1.01f, -0.5f, -0.5f), new Vector3f(2, 0.5f, 0.5f))));

    }

    @Test
    void inputsAreCopiedAndDestinationsAreCallerOwned() {

        Vector3f center = new Vector3f(1, 2, 3);
        Sphere3f sphere = new Sphere3f(center, 4);
        center.set(99, 99, 99);
        assertVector(sphere.center(new Vector3f()), 1, 2, 3);
        Vector3f destination = sphere.center(new Vector3f());
        destination.zero();
        assertVector(sphere.center(new Vector3f()), 1, 2, 3);

    }

    @Test
    void nonFiniteInputsAreRejected() {

        assertThrows(IllegalArgumentException.class, () -> new Ray3f(new Vector3f(Float.NaN, 0, 0), new Vector3f(1, 0, 0)));
        assertThrows(IllegalArgumentException.class, () -> new Plane3f(new Vector3f(1, 0, 0), Float.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class, () -> new Sphere3f(new Vector3f(), Float.NaN));
        assertThrows(IllegalArgumentException.class, () -> new Aabb3f(new Vector3f(), new Vector3f(Float.POSITIVE_INFINITY, 1, 1)));

    }

    private static Frustum3f unitBoxFrustum() {

        return new Frustum3f(new Plane3f(new Vector3f(1, 0, 0), 1), new Plane3f(new Vector3f(-1, 0, 0), 1), new Plane3f(new Vector3f(0, 1, 0), 1),
            new Plane3f(new Vector3f(0, -1, 0), 1), new Plane3f(new Vector3f(0, 0, 1), 1), new Plane3f(new Vector3f(0, 0, -1), 1));

    }

    private static void assertVector(Vector3f actual, float x, float y, float z) {

        assertEquals(x, actual.x, EPSILON);
        assertEquals(y, actual.y, EPSILON);
        assertEquals(z, actual.z, EPSILON);

    }
}
