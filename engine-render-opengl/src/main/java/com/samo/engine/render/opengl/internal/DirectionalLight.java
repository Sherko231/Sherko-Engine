package com.samo.engine.render.opengl.internal;

import java.util.Objects;
import org.joml.Vector3fc;

record DirectionalLight(
        float directionX,
        float directionY,
        float directionZ,
        float red,
        float green,
        float blue,
        float intensity) {

    DirectionalLight {
        requireFinite("directionX", directionX);
        requireFinite("directionY", directionY);
        requireFinite("directionZ", directionZ);
        float lengthSquared =
                directionX * directionX + directionY * directionY + directionZ * directionZ;
        if (!(lengthSquared > 0.0f) || !Float.isFinite(lengthSquared)) {
            throw new IllegalArgumentException("direction must be finite and non-zero");
        }
        float inverseLength = 1.0f / (float) Math.sqrt(lengthSquared);
        directionX *= inverseLength;
        directionY *= inverseLength;
        directionZ *= inverseLength;

        requireUnit("red", red);
        requireUnit("green", green);
        requireUnit("blue", blue);
        requireUnit("intensity", intensity);
    }

    static DirectionalLight from(Vector3fc direction, Vector3fc linearColor, float intensity) {
        Vector3fc directionValue = Objects.requireNonNull(direction, "direction");
        Vector3fc colorValue = Objects.requireNonNull(linearColor, "linearColor");
        return new DirectionalLight(
                directionValue.x(),
                directionValue.y(),
                directionValue.z(),
                colorValue.x(),
                colorValue.y(),
                colorValue.z(),
                intensity);
    }

    float diffuseFactor(Vector3fc surfaceNormal) {
        Vector3fc normal = Objects.requireNonNull(surfaceNormal, "surfaceNormal");
        float nx = normal.x();
        float ny = normal.y();
        float nz = normal.z();
        requireFinite("surfaceNormal.x", nx);
        requireFinite("surfaceNormal.y", ny);
        requireFinite("surfaceNormal.z", nz);
        float lengthSquared = nx * nx + ny * ny + nz * nz;
        if (!(lengthSquared > 0.0f) || !Float.isFinite(lengthSquared)) {
            throw new IllegalArgumentException("surfaceNormal must be finite and non-zero");
        }
        float inverseLength = 1.0f / (float) Math.sqrt(lengthSquared);
        nx *= inverseLength;
        ny *= inverseLength;
        nz *= inverseLength;

        float facing = nx * -directionX + ny * -directionY + nz * -directionZ;
        return Math.max(facing, 0.0f) * intensity;
    }

    private static void requireFinite(String name, float value) {
        if (!Float.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite");
        }
    }

    private static void requireUnit(String name, float value) {
        if (!Float.isFinite(value) || value < 0.0f || value > 1.0f) {
            throw new IllegalArgumentException(name + " must be finite and within [0,1]");
        }
    }
}
