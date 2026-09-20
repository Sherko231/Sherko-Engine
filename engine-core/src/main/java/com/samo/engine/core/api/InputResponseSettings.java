package com.samo.engine.core.api;

/** Immutable deterministic input-response settings shared across platform adapters. */
public record InputResponseSettings(double mouseSensitivity, boolean invertMouseY, double controllerDeadZone, double controllerCurveExponent) {

    public InputResponseSettings {
        if (!Double.isFinite(mouseSensitivity) || mouseSensitivity < 0.0d) {
            throw new IllegalArgumentException("mouseSensitivity must be finite and >= 0");
        }
        if (!Double.isFinite(controllerDeadZone) || controllerDeadZone < 0.0d || controllerDeadZone >= 1.0d) {
            throw new IllegalArgumentException("controllerDeadZone must be finite and in [0, 1)");
        }
        if (!Double.isFinite(controllerCurveExponent) || controllerCurveExponent <= 0.0d) {
            throw new IllegalArgumentException("controllerCurveExponent must be finite and > 0");
        }
    }

    public static InputResponseSettings defaults() {
        return new InputResponseSettings(1.0d, false, 0.0d, 1.0d);
    }

    public double applyMouseX(double rawDelta) {
        return applyMouse(rawDelta, false);
    }

    public double applyMouseY(double rawDelta) {
        return applyMouse(rawDelta, invertMouseY);
    }

    public double applyControllerAxis(double rawAxis) {
        if (!Double.isFinite(rawAxis)) {
            throw new IllegalArgumentException("rawAxis must be finite");
        }
        if (rawAxis < -1.0d || rawAxis > 1.0d) {
            throw new IllegalArgumentException("rawAxis must be in [-1, 1]");
        }

        double magnitude = Math.abs(rawAxis);
        if (magnitude <= controllerDeadZone) {
            return 0.0d;
        }

        double normalized = (magnitude - controllerDeadZone) / (1.0d - controllerDeadZone);
        double curved = Math.pow(normalized, controllerCurveExponent);
        if (!Double.isFinite(curved)) {
            throw new IllegalArgumentException("controller response result must be finite");
        }
        return Math.copySign(curved, rawAxis);
    }

    private double applyMouse(double rawDelta, boolean invert) {
        if (!Double.isFinite(rawDelta)) {
            throw new IllegalArgumentException("mouse delta must be finite");
        }
        double scaled = rawDelta * mouseSensitivity;
        if (!Double.isFinite(scaled)) {
            throw new IllegalArgumentException("mouse response result must be finite");
        }
        return invert ? -scaled : scaled;
    }
}
