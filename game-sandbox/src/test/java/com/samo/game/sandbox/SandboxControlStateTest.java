package com.samo.game.sandbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.samo.engine.core.api.InputResponseSettings;
import com.samo.engine.platform.api.InputAction;
import com.samo.engine.platform.api.InputActionBindings;
import com.samo.engine.platform.api.InputActionComponent;
import com.samo.engine.platform.api.InputActionEvaluator;
import com.samo.engine.platform.api.InputActionValueType;
import com.samo.engine.platform.api.InputBinding;
import com.samo.engine.platform.api.InputKey;
import com.samo.engine.platform.api.WindowMode;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import org.junit.jupiter.api.Test;

final class SandboxControlStateTest {
    @Test
    void appliesWindowAndCursorEffectsInCurrentOrder() {
        SandboxControlState state = new SandboxControlState(InputResponseSettings.defaults());
        List<String> trace = new ArrayList<>();

        boolean exit = state.apply(
                EnumSet.of(
                        SandboxControls.Action.CYCLE_WINDOW_MODE,
                        SandboxControls.Action.TOGGLE_CURSOR_CAPTURE,
                        SandboxControls.Action.EXIT),
                false,
                null,
                mode -> trace.add("mode:" + mode),
                captured -> trace.add("capture:" + captured),
                message -> trace.add("log:" + message));

        assertTrue(exit);
        assertEquals(WindowMode.BORDERLESS_FULLSCREEN, state.currentWindowMode());
        assertEquals(
                List.of(
                        "mode:BORDERLESS_FULLSCREEN",
                        "log:Sandbox window mode -> BORDERLESS_FULLSCREEN",
                        "capture:true",
                        "log:Sandbox cursor capture requested -> true"),
                trace);
    }

    @Test
    void sensitivityAndInversionUpdateEvaluatorAndStateWithoutWindowEffects() {
        InputActionEvaluator evaluator = new InputActionEvaluator(emptyBindings(), InputResponseSettings.defaults());
        SandboxControlState state = new SandboxControlState(InputResponseSettings.defaults());
        List<String> logs = new ArrayList<>();

        boolean exit = state.apply(
                EnumSet.of(
                        SandboxControls.Action.CYCLE_MOUSE_SENSITIVITY,
                        SandboxControls.Action.TOGGLE_MOUSE_Y_INVERSION),
                false,
                evaluator,
                mode -> {
                    throw new AssertionError("unexpected window mode effect");
                },
                captured -> {
                    throw new AssertionError("unexpected cursor effect");
                },
                logs::add);

        assertFalse(exit);
        assertEquals(2.0d, state.responseSettings().mouseSensitivity());
        assertTrue(state.responseSettings().invertMouseY());
        assertEquals(
                List.of("Mouse sensitivity -> 2.00", "Mouse Y inversion -> true"),
                logs);
    }

    @Test
    void windowModeCycleRemainsThreeStateLoop() {
        assertEquals(
                WindowMode.BORDERLESS_FULLSCREEN,
                SandboxControlState.nextWindowMode(WindowMode.WINDOWED));
        assertEquals(
                WindowMode.EXCLUSIVE_FULLSCREEN,
                SandboxControlState.nextWindowMode(WindowMode.BORDERLESS_FULLSCREEN));
        assertEquals(
                WindowMode.WINDOWED,
                SandboxControlState.nextWindowMode(WindowMode.EXCLUSIVE_FULLSCREEN));
        assertEquals(0, SandboxControlState.indexOfSensitivity(123.0d));
    }

    private static InputActionBindings emptyBindings() {
        EnumMap<InputAction, List<InputBinding>> bindings = new EnumMap<>(InputAction.class);
        for (InputAction action : InputAction.values()) {
            InputActionComponent component = action.valueType() == InputActionValueType.DIGITAL
                    ? InputActionComponent.VALUE
                    : InputActionComponent.X;
            bindings.put(
                    action,
                    List.of(new InputBinding(
                            new InputBinding.KeyControl(InputKey.W),
                            component,
                            1.0d)));
        }
        return new InputActionBindings(bindings);
    }
}
