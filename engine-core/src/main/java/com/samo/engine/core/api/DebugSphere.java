package com.samo.engine.core.api;

import java.util.Objects;

/** Immutable renderer-neutral debug submission for an accepted world-space sphere. */
public record DebugSphere(Sphere3f sphere, DebugColor color) implements DebugPrimitive {
    public DebugSphere {

        Objects.requireNonNull(sphere, "sphere");
        Objects.requireNonNull(color, "color");

    }
}
