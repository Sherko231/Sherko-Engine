package com.samo.engine.platform.api;

import java.util.Objects;

/** Immutable descriptor connecting one hardware control to one action component. */
public record InputBinding(Control control, InputActionComponent component, double scale) {
    public InputBinding {

        Objects.requireNonNull(control, "control");
        Objects.requireNonNull(component, "component");
        if (!Double.isFinite(scale) || scale == 0.0d) {
            throw new IllegalArgumentException("scale must be finite and non-zero");
        }

    }

    /** Device-neutral control descriptor used by an action binding. */
    public sealed interface Control permits KeyControl, MouseButtonControl, MouseDeltaControl {
    }

    /** Keyboard control using the engine-owned P3-T06 key vocabulary. */
    public record KeyControl(InputKey key) implements Control {
        public KeyControl {

            Objects.requireNonNull(key, "key");

        }
    }

    /** Mouse-button control using the engine-owned P3-T06 button vocabulary. */
    public record MouseButtonControl(InputMouseButton button) implements Control {
        public MouseButtonControl {

            Objects.requireNonNull(button, "button");

        }
    }

    /** Relative mouse-delta control. */
    public record MouseDeltaControl(MouseDeltaAxis axis) implements Control {
        public MouseDeltaControl {

            Objects.requireNonNull(axis, "axis");

        }
    }

    /** Relative mouse axis available to data-driven bindings. */
    public enum MouseDeltaAxis {
        X, Y
    }
}
