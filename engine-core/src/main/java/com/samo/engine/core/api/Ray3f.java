package com.samo.engine.core.api;

import java.util.Objects;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/** Immutable normalized world-space ray. */
public final class Ray3f {
    private final float originX;
    private final float originY;
    private final float originZ;
    private final float directionX;
    private final float directionY;
    private final float directionZ;

    public Ray3f(Vector3fc origin, Vector3fc direction) {

        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(direction, "direction");
        requireFinite(origin.x(), "origin.x");
        requireFinite(origin.y(), "origin.y");
        requireFinite(origin.z(), "origin.z");
        requireFinite(direction.x(), "direction.x");
        requireFinite(direction.y(), "direction.y");
        requireFinite(direction.z(), "direction.z");
        double lengthSquared = (double) direction.x() * direction.x() + (double) direction.y() * direction.y() + (double) direction.z() * direction.z();
        if (!(lengthSquared > 0.0) || !Double.isFinite(lengthSquared)) {
            throw new IllegalArgumentException("ray direction must have finite non-zero length");
        }
        double inverseLength = 1.0 / Math.sqrt(lengthSquared);
        originX = origin.x();
        originY = origin.y();
        originZ = origin.z();
        directionX = (float) (direction.x() * inverseLength);
        directionY = (float) (direction.y() * inverseLength);
        directionZ = (float) (direction.z() * inverseLength);

    }

    Ray3f(float originX, float originY, float originZ, float directionX, float directionY, float directionZ) {

        requireFinite(originX, "origin.x");
        requireFinite(originY, "origin.y");
        requireFinite(originZ, "origin.z");
        requireFinite(directionX, "direction.x");
        requireFinite(directionY, "direction.y");
        requireFinite(directionZ, "direction.z");
        double lengthSquared = (double) directionX * directionX + (double) directionY * directionY + (double) directionZ * directionZ;
        if (!(lengthSquared > 0.0) || !Double.isFinite(lengthSquared)) {
            throw new IllegalArgumentException("ray direction must have finite non-zero length");
        }
        double inverseLength = 1.0 / Math.sqrt(lengthSquared);
        this.originX = originX;
        this.originY = originY;
        this.originZ = originZ;
        this.directionX = (float) (directionX * inverseLength);
        this.directionY = (float) (directionY * inverseLength);
        this.directionZ = (float) (directionZ * inverseLength);

    }

    public Vector3f origin(Vector3f destination) {

        return Objects.requireNonNull(destination, "destination").set(originX, originY, originZ);

    }

    public Vector3f direction(Vector3f destination) {

        return Objects.requireNonNull(destination, "destination").set(directionX, directionY, directionZ);

    }

    public Vector3f pointAt(float distance, Vector3f destination) {

        Objects.requireNonNull(destination, "destination");
        requireFinite(distance, "distance");
        if (distance < 0.0f) {
            throw new IllegalArgumentException("distance must be >= 0");
        }
        return destination.set(originX + directionX * distance, originY + directionY * distance, originZ + directionZ * distance);

    }

    public float intersectPlane(Plane3f plane) {

        Objects.requireNonNull(plane, "plane");
        float signedDistance = plane.signedDistance(originX, originY, originZ);
        if (signedDistance == 0.0f) {
            return 0.0f;
        }
        double denominator = (double) plane.normalX() * directionX + (double) plane.normalY() * directionY + (double) plane.normalZ() * directionZ;
        if (denominator == 0.0) {
            return Float.NaN;
        }
        double t = -signedDistance / denominator;
        return t >= 0.0 && t <= Float.MAX_VALUE ? (float) t : Float.NaN;

    }

    public float intersectSphere(Sphere3f sphere) {

        Objects.requireNonNull(sphere, "sphere");
        double mx = (double) originX - sphere.centerX();
        double my = (double) originY - sphere.centerY();
        double mz = (double) originZ - sphere.centerZ();
        double radius = sphere.radius();
        double c = mx * mx + my * my + mz * mz - radius * radius;
        if (c <= 0.0) {
            return 0.0f;
        }
        double b = mx * directionX + my * directionY + mz * directionZ;
        if (b > 0.0) {
            return Float.NaN;
        }
        double discriminant = b * b - c;
        if (discriminant < 0.0) {
            return Float.NaN;
        }
        double t = -b - Math.sqrt(discriminant);
        return t >= 0.0 && t <= Float.MAX_VALUE ? (float) t : Float.NaN;

    }

    public float intersectAabb(Aabb3f aabb) {

        Objects.requireNonNull(aabb, "aabb");
        if (originX >= aabb.minX() && originX <= aabb.maxX() && originY >= aabb.minY() && originY <= aabb.maxY() && originZ >= aabb.minZ() && originZ <= aabb.maxZ()) {
            return 0.0f;
        }

        double tMin = 0.0;
        double tMax = Double.POSITIVE_INFINITY;

        if (directionX == 0.0f) {
            if (originX < aabb.minX() || originX > aabb.maxX())
                return Float.NaN;
        } else {
            double first = (aabb.minX() - originX) / directionX;
            double second = (aabb.maxX() - originX) / directionX;
            if (first > second) {
                double temporary = first;
                first = second;
                second = temporary;
            }
            tMin = Math.max(tMin, first);
            tMax = Math.min(tMax, second);
            if (tMin > tMax)
                return Float.NaN;
        }

        if (directionY == 0.0f) {
            if (originY < aabb.minY() || originY > aabb.maxY())
                return Float.NaN;
        } else {
            double first = (aabb.minY() - originY) / directionY;
            double second = (aabb.maxY() - originY) / directionY;
            if (first > second) {
                double temporary = first;
                first = second;
                second = temporary;
            }
            tMin = Math.max(tMin, first);
            tMax = Math.min(tMax, second);
            if (tMin > tMax)
                return Float.NaN;
        }

        if (directionZ == 0.0f) {
            if (originZ < aabb.minZ() || originZ > aabb.maxZ())
                return Float.NaN;
        } else {
            double first = (aabb.minZ() - originZ) / directionZ;
            double second = (aabb.maxZ() - originZ) / directionZ;
            if (first > second) {
                double temporary = first;
                first = second;
                second = temporary;
            }
            tMin = Math.max(tMin, first);
            tMax = Math.min(tMax, second);
            if (tMin > tMax)
                return Float.NaN;
        }

        return tMin <= Float.MAX_VALUE ? (float) tMin : Float.NaN;

    }

    private static void requireFinite(float value, String name) {

        if (!Float.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite");
        }

    }
}
