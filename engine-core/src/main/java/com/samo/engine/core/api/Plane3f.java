package com.samo.engine.core.api;

import java.util.Objects;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/** Immutable normalized world-space plane using {@code normal dot point + offset = 0}. */
public final class Plane3f {
    private final float normalX;
    private final float normalY;
    private final float normalZ;
    private final float offset;

    public Plane3f(Vector3fc normal, float offset) {
        Objects.requireNonNull(normal, "normal");
        requireFinite(normal.x(), "normal.x");
        requireFinite(normal.y(), "normal.y");
        requireFinite(normal.z(), "normal.z");
        requireFinite(offset, "offset");

        double lengthSquared = (double) normal.x() * normal.x() + (double) normal.y() * normal.y() + (double) normal.z() * normal.z();
        if (!(lengthSquared > 0.0) || !Double.isFinite(lengthSquared)) {
            throw new IllegalArgumentException("plane normal must have finite non-zero length");
        }

        double inverseLength = 1.0 / Math.sqrt(lengthSquared);
        normalX = (float) (normal.x() * inverseLength);
        normalY = (float) (normal.y() * inverseLength);
        normalZ = (float) (normal.z() * inverseLength);
        this.offset = (float) (offset * inverseLength);
    }

    public static Plane3f fromPointNormal(Vector3fc point, Vector3fc normal) {
        Objects.requireNonNull(point, "point");
        Objects.requireNonNull(normal, "normal");
        requireFinite(point.x(), "point.x");
        requireFinite(point.y(), "point.y");
        requireFinite(point.z(), "point.z");
        requireFinite(normal.x(), "normal.x");
        requireFinite(normal.y(), "normal.y");
        requireFinite(normal.z(), "normal.z");

        double rawOffset = -((double) normal.x() * point.x() + (double) normal.y() * point.y() + (double) normal.z() * point.z());
        if (!Double.isFinite(rawOffset)) {
            throw new IllegalArgumentException("plane offset must be finite");
        }
        return new Plane3f(normal, (float) rawOffset);
    }

    public Vector3f normal(Vector3f destination) {
        return Objects.requireNonNull(destination, "destination").set(normalX, normalY, normalZ);
    }

    public float offset() {
        return offset;
    }

    public float signedDistance(Vector3fc point) {
        Objects.requireNonNull(point, "point");
        requireFinite(point.x(), "point.x");
        requireFinite(point.y(), "point.y");
        requireFinite(point.z(), "point.z");
        return normalX * point.x() + normalY * point.y() + normalZ * point.z() + offset;
    }

    float normalX() {
        return normalX;
    }

    float normalY() {
        return normalY;
    }

    float normalZ() {
        return normalZ;
    }

    float signedDistance(float x, float y, float z) {
        return normalX * x + normalY * y + normalZ * z + offset;
    }

    private static void requireFinite(float value, String name) {
        if (!Float.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite");
        }
    }
}
