package com.samo.engine.core.api;

import java.util.Objects;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/** Immutable world-space axis-aligned bounding box with inclusive boundaries. */
public final class Aabb3f {
    private final float minX;
    private final float minY;
    private final float minZ;
    private final float maxX;
    private final float maxY;
    private final float maxZ;

    public Aabb3f(Vector3fc minimum, Vector3fc maximum) {
        Objects.requireNonNull(minimum, "minimum");
        Objects.requireNonNull(maximum, "maximum");
        requireFinite(minimum.x(), "minimum.x");
        requireFinite(minimum.y(), "minimum.y");
        requireFinite(minimum.z(), "minimum.z");
        requireFinite(maximum.x(), "maximum.x");
        requireFinite(maximum.y(), "maximum.y");
        requireFinite(maximum.z(), "maximum.z");
        if (minimum.x() > maximum.x() || minimum.y() > maximum.y() || minimum.z() > maximum.z()) {
            throw new IllegalArgumentException("minimum must be <= maximum on every axis");
        }
        minX = minimum.x();
        minY = minimum.y();
        minZ = minimum.z();
        maxX = maximum.x();
        maxY = maximum.y();
        maxZ = maximum.z();
    }

    public Vector3f minimum(Vector3f destination) {
        return Objects.requireNonNull(destination, "destination").set(minX, minY, minZ);
    }

    public Vector3f maximum(Vector3f destination) {
        return Objects.requireNonNull(destination, "destination").set(maxX, maxY, maxZ);
    }

    public boolean containsPoint(Vector3fc point) {
        Objects.requireNonNull(point, "point");
        requireFinite(point.x(), "point.x");
        requireFinite(point.y(), "point.y");
        requireFinite(point.z(), "point.z");
        return point.x() >= minX && point.x() <= maxX && point.y() >= minY && point.y() <= maxY && point.z() >= minZ && point.z() <= maxZ;
    }

    public boolean intersects(Aabb3f other) {
        Objects.requireNonNull(other, "other");
        return maxX >= other.minX && minX <= other.maxX && maxY >= other.minY && minY <= other.maxY && maxZ >= other.minZ && minZ <= other.maxZ;
    }

    public boolean intersects(Sphere3f sphere) {
        Objects.requireNonNull(sphere, "sphere");
        double x = clamp(sphere.centerX(), minX, maxX);
        double y = clamp(sphere.centerY(), minY, maxY);
        double z = clamp(sphere.centerZ(), minZ, maxZ);
        double dx = sphere.centerX() - x;
        double dy = sphere.centerY() - y;
        double dz = sphere.centerZ() - z;
        return dx * dx + dy * dy + dz * dz <= (double) sphere.radius() * sphere.radius();
    }

    float minX() {
        return minX;
    }

    float minY() {
        return minY;
    }

    float minZ() {
        return minZ;
    }

    float maxX() {
        return maxX;
    }

    float maxY() {
        return maxY;
    }

    float maxZ() {
        return maxZ;
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static void requireFinite(float value, String name) {
        if (!Float.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite");
        }
    }
}
