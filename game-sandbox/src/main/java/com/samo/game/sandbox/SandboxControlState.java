package com.samo.game.sandbox;

import com.samo.engine.core.api.InputResponseSettings;
import com.samo.engine.platform.api.InputActionEvaluator;
import com.samo.engine.platform.api.WindowMode;
import java.util.EnumSet;
import java.util.Objects;
import java.util.function.Consumer;

final class SandboxControlState {
    private static final double[] MOUSE_SENSITIVITIES = {0.5d, 1.0d, 2.0d};

    private WindowMode currentWindowMode = WindowMode.WINDOWED;
    private InputResponseSettings responseSettings;
    private int sensitivityIndex;

    SandboxControlState(InputResponseSettings initialResponseSettings) {
        responseSettings = Objects.requireNonNull(initialResponseSettings, "initialResponseSettings");
        sensitivityIndex = indexOfSensitivity(responseSettings.mouseSensitivity());
    }

    boolean apply(EnumSet<SandboxControls.SandboxAction> actions, boolean cursorCaptured, InputActionEvaluator actionEvaluator, Consumer<WindowMode> windowModeSetter,
        Consumer<Boolean> cursorCaptureSetter, Consumer<String> logSink) {
        Objects.requireNonNull(actions, "actions");
        Objects.requireNonNull(windowModeSetter, "windowModeSetter");
        Objects.requireNonNull(cursorCaptureSetter, "cursorCaptureSetter");
        Objects.requireNonNull(logSink, "logSink");

        if (actions.contains(SandboxControls.SandboxAction.CYCLE_WINDOW_MODE)) {
            currentWindowMode = nextWindowMode(currentWindowMode);
            windowModeSetter.accept(currentWindowMode);
            logSink.accept("Sandbox window mode -> " + currentWindowMode);
        }
        if (actions.contains(SandboxControls.SandboxAction.TOGGLE_CURSOR_CAPTURE)) {
            boolean requestedCapture = !cursorCaptured;
            cursorCaptureSetter.accept(requestedCapture);
            logSink.accept("Sandbox cursor capture requested -> " + requestedCapture);
        }
        if (actions.contains(SandboxControls.SandboxAction.CYCLE_MOUSE_SENSITIVITY)) {
            InputActionEvaluator evaluator = Objects.requireNonNull(actionEvaluator, "actionEvaluator");
            sensitivityIndex = (sensitivityIndex + 1) % MOUSE_SENSITIVITIES.length;
            responseSettings = new InputResponseSettings(MOUSE_SENSITIVITIES[sensitivityIndex], responseSettings.invertMouseY(), responseSettings.controllerDeadZone(),
                responseSettings.controllerCurveExponent());
            evaluator.setResponseSettings(responseSettings);
            logSink.accept("Mouse sensitivity -> %.2f".formatted(responseSettings.mouseSensitivity()));
        }
        if (actions.contains(SandboxControls.SandboxAction.TOGGLE_MOUSE_Y_INVERSION)) {
            InputActionEvaluator evaluator = Objects.requireNonNull(actionEvaluator, "actionEvaluator");
            responseSettings = new InputResponseSettings(responseSettings.mouseSensitivity(), !responseSettings.invertMouseY(), responseSettings.controllerDeadZone(),
                responseSettings.controllerCurveExponent());
            evaluator.setResponseSettings(responseSettings);
            logSink.accept("Mouse Y inversion -> " + responseSettings.invertMouseY());
        }

        return actions.contains(SandboxControls.SandboxAction.EXIT);
    }

    WindowMode currentWindowMode() {
        return currentWindowMode;
    }

    InputResponseSettings responseSettings() {
        return responseSettings;
    }

    static WindowMode nextWindowMode(WindowMode current) {
        return switch (Objects.requireNonNull(current, "current")) {
            case WINDOWED -> WindowMode.BORDERLESS_FULLSCREEN;
            case BORDERLESS_FULLSCREEN -> WindowMode.EXCLUSIVE_FULLSCREEN;
            case EXCLUSIVE_FULLSCREEN -> WindowMode.WINDOWED;
        };
    }

    static int indexOfSensitivity(double sensitivity) {
        for (int index = 0; index < MOUSE_SENSITIVITIES.length; index++) {
            if (Double.compare(MOUSE_SENSITIVITIES[index], sensitivity) == 0) {
                return index;
            }
        }
        return 0;
    }
}
