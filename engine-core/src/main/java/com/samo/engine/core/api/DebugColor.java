package com.samo.engine.core.api;

/** Immutable linear RGB debug color. */
public record DebugColor(float red, float green, float blue) {
    public DebugColor {
        requireUnit("red", red);
        requireUnit("green", green);
        requireUnit("blue", blue);
    }

    private static void requireUnit(String name, float value) {
        if (!Float.isFinite(value) || value < 0.0f || value > 1.0f) {
            throw new IllegalArgumentException(name + " must be finite and within [0,1]");
        }
    }
}
