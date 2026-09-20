package com.samo.engine.core.api;

import java.util.Objects;

/** Immutable renderer-neutral finite visualization of an accepted normalized world-space ray. */
public record DebugRay(Ray3f ray, float lengthMeters, DebugColor color) implements DebugPrimitive {
    public DebugRay {

        Objects.requireNonNull(ray, "ray");
        if (!Float.isFinite(lengthMeters) || !(lengthMeters > 0.0f)) {
            throw new IllegalArgumentException("lengthMeters must be finite and positive");
        }
        Objects.requireNonNull(color, "color");

    }
}
