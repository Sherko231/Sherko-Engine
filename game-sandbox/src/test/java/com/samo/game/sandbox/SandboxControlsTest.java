package com.samo.game.sandbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import org.junit.jupiter.api.Test;

final class SandboxControlsTest {
    @Test
    void plainControlsOperateThePersistentPlayground() {
        assertEquals(
                EnumSet.of(SandboxControls.Action.CYCLE_WINDOW_MODE),
                SandboxControls.resolve(input(true, false, false, false, false, false)));
        assertEquals(
                EnumSet.of(SandboxControls.Action.TOGGLE_CURSOR_CAPTURE),
                SandboxControls.resolve(input(false, true, false, false, false, false)));
    }

    @Test
    void rightShiftSelectsResponseSettingsControlsInsteadOfWindowControls() {
        assertEquals(
                EnumSet.of(SandboxControls.Action.CYCLE_MOUSE_SENSITIVITY),
                SandboxControls.resolve(input(true, false, false, true, false, false)));
        assertEquals(
                EnumSet.of(SandboxControls.Action.TOGGLE_MOUSE_Y_INVERSION),
                SandboxControls.resolve(input(false, true, false, true, false, false)));
    }

    @Test
    void controlQIsTheOnlyQCombinationThatRequestsExit() {
        assertTrue(SandboxControls.resolve(input(false, false, true, false, false, false)).isEmpty());
        assertEquals(
                EnumSet.of(SandboxControls.Action.EXIT),
                SandboxControls.resolve(input(false, false, true, false, true, false)));
        assertEquals(
                EnumSet.of(SandboxControls.Action.EXIT),
                SandboxControls.resolve(input(false, false, true, false, false, true)));
    }

    @Test
    void independentControlsCanBeRequestedInTheSameFrame() {
        assertEquals(
                EnumSet.of(
                        SandboxControls.Action.CYCLE_WINDOW_MODE,
                        SandboxControls.Action.TOGGLE_CURSOR_CAPTURE,
                        SandboxControls.Action.EXIT),
                SandboxControls.resolve(input(true, true, true, false, true, false)));
    }

    private static SandboxControls.Input input(
            boolean fPressed,
            boolean rPressed,
            boolean qPressed,
            boolean rightShiftHeld,
            boolean leftControlHeld,
            boolean rightControlHeld) {
        return new SandboxControls.Input(
                fPressed,
                rPressed,
                qPressed,
                rightShiftHeld,
                leftControlHeld,
                rightControlHeld);
    }
}
