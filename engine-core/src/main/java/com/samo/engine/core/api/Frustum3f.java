package com.samo.engine.core.api;

import java.util.Objects;
import org.joml.Vector3fc;

/** Immutable world-space frustum defined by six inward-facing normalized planes. */
public final class Frustum3f {
    private final Plane3f left;
    private final Plane3f right;
    private final Plane3f bottom;
    private final Plane3f top;
    private final Plane3f near;
    private final Plane3f far;

    public Frustum3f(Plane3f left, Plane3f right, Plane3f bottom, Plane3f top, Plane3f near, Plane3f far) {
        this.left = Objects.requireNonNull(left, "left");
        this.right = Objects.requireNonNull(right, "right");
        this.bottom = Objects.requireNonNull(bottom, "bottom");
        this.top = Objects.requireNonNull(top, "top");
        this.near = Objects.requireNonNull(near, "near");
        this.far = Objects.requireNonNull(far, "far");
    }

    public boolean containsPoint(Vector3fc point) {
        Objects.requireNonNull(point, "point");
        return left.signedDistance(point) >= 0.0f && right.signedDistance(point) >= 0.0f && bottom.signedDistance(point) >= 0.0f && top.signedDistance(point) >= 0.0f
            && near.signedDistance(point) >= 0.0f && far.signedDistance(point) >= 0.0f;
    }

    public boolean intersects(Sphere3f sphere) {
        Objects.requireNonNull(sphere, "sphere");
        float x = sphere.centerX();
        float y = sphere.centerY();
        float z = sphere.centerZ();
        float radius = sphere.radius();
        return left.signedDistance(x, y, z) >= -radius && right.signedDistance(x, y, z) >= -radius && bottom.signedDistance(x, y, z) >= -radius
            && top.signedDistance(x, y, z) >= -radius && near.signedDistance(x, y, z) >= -radius && far.signedDistance(x, y, z) >= -radius;
    }

    public boolean intersects(Aabb3f aabb) {
        Objects.requireNonNull(aabb, "aabb");
        return notOutside(left, aabb) && notOutside(right, aabb) && notOutside(bottom, aabb) && notOutside(top, aabb) && notOutside(near, aabb) && notOutside(far, aabb);
    }

    private static boolean notOutside(Plane3f plane, Aabb3f aabb) {
        float x = plane.normalX() >= 0.0f ? aabb.maxX() : aabb.minX();
        float y = plane.normalY() >= 0.0f ? aabb.maxY() : aabb.minY();
        float z = plane.normalZ() >= 0.0f ? aabb.maxZ() : aabb.minZ();
        return plane.signedDistance(x, y, z) >= 0.0f;
    }
}
