package com.samo.engine.core.api;

import java.util.Objects;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/** Immutable world-space sphere. Boundary contact is included in containment/intersection. */
public final class Sphere3f {
    private final float centerX;
    private final float centerY;
    private final float centerZ;
    private final float radius;

    public Sphere3f(Vector3fc center, float radius) {
        Objects.requireNonNull(center, "center");
        requireFinite(center.x(), "center.x");
        requireFinite(center.y(), "center.y");
        requireFinite(center.z(), "center.z");
        requireFinite(radius, "radius");
        if (radius < 0.0f) {
            throw new IllegalArgumentException("radius must be >= 0");
        }
        centerX = center.x();
        centerY = center.y();
        centerZ = center.z();
        this.radius = radius;
    }

    public Vector3f center(Vector3f destination) {
        return Objects.requireNonNull(destination, "destination").set(centerX, centerY, centerZ);
    }

    public float radius() {
        return radius;
    }

    public boolean containsPoint(Vector3fc point) {
        Objects.requireNonNull(point, "point");
        requireFinite(point.x(), "point.x");
        requireFinite(point.y(), "point.y");
        requireFinite(point.z(), "point.z");
        double dx = (double) point.x() - centerX;
        double dy = (double) point.y() - centerY;
        double dz = (double) point.z() - centerZ;
        return dx * dx + dy * dy + dz * dz <= (double) radius * radius;
    }

    public boolean intersects(Sphere3f other) {
        Objects.requireNonNull(other, "other");
        double dx = (double) other.centerX - centerX;
        double dy = (double) other.centerY - centerY;
        double dz = (double) other.centerZ - centerZ;
        double sum = (double) radius + other.radius;
        return dx * dx + dy * dy + dz * dz <= sum * sum;
    }

    public boolean intersects(Aabb3f aabb) {
        return Objects.requireNonNull(aabb, "aabb").intersects(this);
    }

    float centerX() {
        return centerX;
    }

    float centerY() {
        return centerY;
    }

    float centerZ() {
        return centerZ;
    }

    private static void requireFinite(float value, String name) {
        if (!Float.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite");
        }
    }
}
