package com.samo.engine.platform.api;

/** Tracks relative cursor motion from GLFW absolute cursor-position callbacks. */
final class GlfwMouseMotionTracker {
    private boolean baselineValid;
    private double previousX;
    private double previousY;
    private double accumulatedDeltaX;
    private double accumulatedDeltaY;

    void onCursorPosition(boolean focused, boolean cursorCaptured, double x, double y) {

        if (!focused || !cursorCaptured) {
            return;
        }
        if (!baselineValid) {
            previousX = x;
            previousY = y;
            baselineValid = true;
            return;
        }
        accumulatedDeltaX += x - previousX;
        accumulatedDeltaY += y - previousY;
        previousX = x;
        previousY = y;

    }

    double accumulatedDeltaX() {

        return accumulatedDeltaX;

    }

    double accumulatedDeltaY() {

        return accumulatedDeltaY;

    }

    void clearAccumulatedDelta() {

        accumulatedDeltaX = 0.0;
        accumulatedDeltaY = 0.0;

    }

    void reset() {

        baselineValid = false;
        previousX = 0.0;
        previousY = 0.0;
        clearAccumulatedDelta();

    }

    MouseDelta drainForTest() {

        MouseDelta motion = new MouseDelta(accumulatedDeltaX, accumulatedDeltaY);
        clearAccumulatedDelta();
        return motion;

    }

    record MouseDelta(double x, double y) {
    }
}
