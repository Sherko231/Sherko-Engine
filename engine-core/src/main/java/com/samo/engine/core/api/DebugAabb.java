package com.samo.engine.core.api;

import java.util.Objects;

/** Immutable renderer-neutral debug submission for an accepted world-space AABB. */
public record DebugAabb(Aabb3f bounds, DebugColor color) implements DebugPrimitive {
    public DebugAabb {
        Objects.requireNonNull(bounds, "bounds");
        Objects.requireNonNull(color, "color");
    }
}
