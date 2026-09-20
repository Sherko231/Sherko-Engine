package com.samo.engine.platform.api;

import java.util.Objects;

/** Immutable evaluated state for one gameplay action in one renderer-frame action snapshot. */
public final class InputActionState {
    private final InputAction action;
    private final boolean pressed;
    private final boolean held;
    private final boolean released;
    private final double value;
    private final double x;
    private final double y;

    InputActionState(InputAction action, boolean pressed, boolean held, boolean released, double value, double x, double y) {
        this.action = Objects.requireNonNull(action, "action");
        if (!Double.isFinite(value) || !Double.isFinite(x) || !Double.isFinite(y)) {
            throw new IllegalArgumentException("action values must be finite");
        }
        if (action.valueType() == InputActionValueType.DIGITAL && (x != 0.0d || y != 0.0d)) {
            throw new IllegalArgumentException("digital action state requires zero vector components");
        }
        if (action.valueType() == InputActionValueType.VECTOR2 && value != 0.0d) {
            throw new IllegalArgumentException("vector action state requires zero scalar value");
        }
        this.pressed = pressed;
        this.held = held;
        this.released = released;
        this.value = value;
        this.x = x;
        this.y = y;
    }

    public InputAction action() {
        return action;
    }

    public boolean pressed() {
        return pressed;
    }

    public boolean held() {
        return held;
    }

    public boolean released() {
        return released;
    }

    public double value() {
        return value;
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }
}
