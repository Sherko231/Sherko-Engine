package com.samo.game.sandbox;

import com.samo.engine.core.api.InputResponseSettings;
import com.samo.engine.platform.api.InputActionEvaluator;
import com.samo.engine.platform.api.InputSnapshot;
import com.samo.engine.platform.api.WindowMode;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

final class SandboxControlState {
    private static final double[] MOUSE_SENSITIVITIES = {0.5d, 1.0d, 2.0d};

    private WindowMode currentWindowMode = WindowMode.WINDOWED;
    private InputResponseSettings responseSettings;
    private int sensitivityIndex;

    SandboxControlState(InputResponseSettings initialResponseSettings) {
        responseSettings = Objects.requireNonNull(initialResponseSettings, "initialResponseSettings");
        sensitivityIndex = indexOfSensitivity(responseSettings.mouseSensitivity());
    }

    ControlUpdate apply(
            EnumSet<SandboxControls.Action> actions,
            InputSnapshot latestInput,
            InputActionEvaluator actionEvaluator) {
        Objects.requireNonNull(actions, "actions");
        InputSnapshot input = Objects.requireNonNull(latestInput, "latestInput");
        InputActionEvaluator evaluator = Objects.requireNonNull(actionEvaluator, "actionEvaluator");

        WindowMode requestedWindowMode = null;
        Boolean requestedCursorCapture = null;
        List<String> logMessages = new ArrayList<>();

        if (actions.contains(SandboxControls.Action.CYCLE_WINDOW_MODE)) {
            currentWindowMode = nextWindowMode(currentWindowMode);
            requestedWindowMode = currentWindowMode;
            logMessages.add("Sandbox window mode -> " + currentWindowMode);
        }
        if (actions.contains(SandboxControls.Action.TOGGLE_CURSOR_CAPTURE)) {
            requestedCursorCapture = !input.cursorCaptured();
            logMessages.add("Sandbox cursor capture requested -> " + requestedCursorCapture);
        }
        if (actions.contains(SandboxControls.Action.CYCLE_MOUSE_SENSITIVITY)) {
            sensitivityIndex = (sensitivityIndex + 1) % MOUSE_SENSITIVITIES.length;
            responseSettings = new InputResponseSettings(
                    MOUSE_SENSITIVITIES[sensitivityIndex],
                    responseSettings.invertMouseY(),
                    responseSettings.controllerDeadZone(),
                    responseSettings.controllerCurveExponent());
            evaluator.setResponseSettings(responseSettings);
            logMessages.add("Mouse sensitivity -> %.2f".formatted(responseSettings.mouseSensitivity()));
        }
        if (actions.contains(SandboxControls.Action.TOGGLE_MOUSE_Y_INVERSION)) {
            responseSettings = new InputResponseSettings(
                    responseSettings.mouseSensitivity(),
                    !responseSettings.invertMouseY(),
                    responseSettings.controllerDeadZone(),
                    responseSettings.controllerCurveExponent());
            evaluator.setResponseSettings(responseSettings);
            logMessages.add("Mouse Y inversion -> " + responseSettings.invertMouseY());
        }

        return new ControlUpdate(
                requestedWindowMode,
                requestedCursorCapture,
                List.copyOf(logMessages),
                actions.contains(SandboxControls.Action.EXIT));
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

    record ControlUpdate(
            WindowMode requestedWindowMode,
            Boolean requestedCursorCapture,
            List<String> logMessages,
            boolean exitRequested) {
        ControlUpdate {
            logMessages = List.copyOf(Objects.requireNonNull(logMessages, "logMessages"));
        }
    }
}
