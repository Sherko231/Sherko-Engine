package com.samo.engine.core.api;

import java.util.Objects;
import org.joml.Vector3fc;

/** Immutable D-041 world-space debug line segment. */
public record DebugLine(float startX, float startY, float startZ, float endX, float endY, float endZ, DebugColor color) implements DebugPrimitive {

    public DebugLine {

        requireFinite("startX", startX);
        requireFinite("startY", startY);
        requireFinite("startZ", startZ);
        requireFinite("endX", endX);
        requireFinite("endY", endY);
        requireFinite("endZ", endZ);
        Objects.requireNonNull(color, "color");

    }

    public DebugLine(Vector3fc start, Vector3fc end, DebugColor color) {

        this(Objects.requireNonNull(start, "start").x(), start.y(), start.z(), Objects.requireNonNull(end, "end").x(), end.y(), end.z(), color);

    }

    private static void requireFinite(String name, float value) {

        if (!Float.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite");
        }

    }
}
