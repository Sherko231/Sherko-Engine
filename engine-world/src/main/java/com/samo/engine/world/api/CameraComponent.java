package com.samo.engine.world.api;

/** Immutable perspective-camera configuration independent of runtime framebuffer aspect. */
public record CameraComponent(float verticalFovRadians, float nearPlaneMeters, float farPlaneMeters) {
    public CameraComponent {

        requireFinite(verticalFovRadians, "verticalFovRadians");
        requireFinite(nearPlaneMeters, "nearPlaneMeters");
        requireFinite(farPlaneMeters, "farPlaneMeters");

        if (!(verticalFovRadians > 0.0f && verticalFovRadians < Math.PI)) {
            throw new IllegalArgumentException("verticalFovRadians must be > 0 and < PI");
        }
        if (!(nearPlaneMeters > 0.0f)) {
            throw new IllegalArgumentException("nearPlaneMeters must be > 0");
        }
        if (!(farPlaneMeters > nearPlaneMeters)) {
            throw new IllegalArgumentException("farPlaneMeters must be > nearPlaneMeters");
        }

    }

    private static void requireFinite(float value, String name) {

        if (!Float.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite");
        }

    }
}
