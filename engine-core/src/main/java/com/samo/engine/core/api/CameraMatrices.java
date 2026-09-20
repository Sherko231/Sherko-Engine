package com.samo.engine.core.api;

import java.util.Objects;
import org.joml.Matrix4f;
import org.joml.Vector3fc;

/**
 * Convention-consistent construction of world-to-view and perspective projection matrices.
 *
 * <p>
 * View space is right-handed with camera forward on {@code -Z}. Perspective projection uses
 * vertical field of view in radians and OpenGL normalized-device-coordinate depth {@code [-1,+1]}.
 */
public final class CameraMatrices {
    private CameraMatrices() {
    }

    public static Matrix4f view(Vector3fc position, Vector3fc forward, Vector3fc up, Matrix4f destination) {
        Objects.requireNonNull(position, "position");
        Objects.requireNonNull(forward, "forward");
        Objects.requireNonNull(up, "up");
        Objects.requireNonNull(destination, "destination");

        float px = position.x();
        float py = position.y();
        float pz = position.z();
        requireFinite(px, "position.x");
        requireFinite(py, "position.y");
        requireFinite(pz, "position.z");

        float fx = forward.x();
        float fy = forward.y();
        float fz = forward.z();
        requireFinite(fx, "forward.x");
        requireFinite(fy, "forward.y");
        requireFinite(fz, "forward.z");

        float ux = up.x();
        float uy = up.y();
        float uz = up.z();
        requireFinite(ux, "up.x");
        requireFinite(uy, "up.y");
        requireFinite(uz, "up.z");

        double forwardLengthSquared = (double) fx * fx + (double) fy * fy + (double) fz * fz;
        if (!(forwardLengthSquared > 0.0) || !Double.isFinite(forwardLengthSquared)) {
            throw new IllegalArgumentException("forward must have finite non-zero length");
        }
        double upLengthSquared = (double) ux * ux + (double) uy * uy + (double) uz * uz;
        if (!(upLengthSquared > 0.0) || !Double.isFinite(upLengthSquared)) {
            throw new IllegalArgumentException("up must have finite non-zero length");
        }

        double inverseForwardLength = 1.0 / Math.sqrt(forwardLengthSquared);
        fx = (float) (fx * inverseForwardLength);
        fy = (float) (fy * inverseForwardLength);
        fz = (float) (fz * inverseForwardLength);

        float rx = fy * uz - fz * uy;
        float ry = fz * ux - fx * uz;
        float rz = fx * uy - fy * ux;
        double rightLengthSquared = (double) rx * rx + (double) ry * ry + (double) rz * rz;
        if (!(rightLengthSquared > 0.0) || !Double.isFinite(rightLengthSquared)) {
            throw new IllegalArgumentException("forward and up must not be parallel");
        }

        double inverseRightLength = 1.0 / Math.sqrt(rightLengthSquared);
        rx = (float) (rx * inverseRightLength);
        ry = (float) (ry * inverseRightLength);
        rz = (float) (rz * inverseRightLength);

        float correctedUpX = ry * fz - rz * fy;
        float correctedUpY = rz * fx - rx * fz;
        float correctedUpZ = rx * fy - ry * fx;

        float translationX = -(rx * px + ry * py + rz * pz);
        float translationY = -(correctedUpX * px + correctedUpY * py + correctedUpZ * pz);
        float translationZ = fx * px + fy * py + fz * pz;

        return destination.set(rx, correctedUpX, -fx, 0.0f, ry, correctedUpY, -fy, 0.0f, rz, correctedUpZ, -fz, 0.0f, translationX, translationY, translationZ, 1.0f);
    }

    public static Matrix4f perspective(float verticalFovRadians, float aspectRatio, float nearPlane, float farPlane, Matrix4f destination) {
        Objects.requireNonNull(destination, "destination");
        requireFinite(verticalFovRadians, "verticalFovRadians");
        requireFinite(aspectRatio, "aspectRatio");
        requireFinite(nearPlane, "nearPlane");
        requireFinite(farPlane, "farPlane");

        if (!(verticalFovRadians > 0.0f && verticalFovRadians < Math.PI)) {
            throw new IllegalArgumentException("verticalFovRadians must be > 0 and < PI");
        }
        if (!(aspectRatio > 0.0f)) {
            throw new IllegalArgumentException("aspectRatio must be > 0");
        }
        if (!(nearPlane > 0.0f)) {
            throw new IllegalArgumentException("nearPlane must be > 0");
        }
        if (!(farPlane > nearPlane)) {
            throw new IllegalArgumentException("farPlane must be > nearPlane");
        }

        float focalLength = (float) (1.0 / Math.tan(verticalFovRadians * 0.5));
        float depthDenominator = nearPlane - farPlane;

        return destination.set(focalLength / aspectRatio, 0.0f, 0.0f, 0.0f, 0.0f, focalLength, 0.0f, 0.0f, 0.0f, 0.0f, (farPlane + nearPlane) / depthDenominator, -1.0f, 0.0f, 0.0f,
            (2.0f * farPlane * nearPlane) / depthDenominator, 0.0f);
    }

    private static void requireFinite(float value, String name) {
        if (!Float.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite");
        }
    }
}
