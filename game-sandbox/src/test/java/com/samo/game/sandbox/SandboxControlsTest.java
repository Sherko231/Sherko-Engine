package com.samo.game.sandbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import org.junit.jupiter.api.Test;

final class SandboxControlsTest {
    @Test
    void plainControlsOperateThePersistentPlayground() {

        assertAction(SandboxControls.SandboxAction.CYCLE_WINDOW_MODE, input(true, false, false, false, false, false, false, false, false));
        assertAction(SandboxControls.SandboxAction.TOGGLE_CURSOR_CAPTURE, input(false, true, false, false, false, false, false, false, false));

    }

    @Test
    void rightShiftSelectsResponseSettingsControlsInsteadOfWindowControls() {

        assertAction(SandboxControls.SandboxAction.CYCLE_MOUSE_SENSITIVITY, input(true, false, false, false, true, false, false, false, false));
        assertAction(SandboxControls.SandboxAction.TOGGLE_MOUSE_Y_INVERSION, input(false, true, false, false, true, false, false, false, false));

    }

    @Test
    void phase6AssetLabUsesExistingEKeyWithModifiers() {

        assertAction(SandboxControls.SandboxAction.CYCLE_VALID_MATERIAL, input(false, false, false, true, false, false, false, false, false));
        assertAction(SandboxControls.SandboxAction.DEMONSTRATE_FAILED_MATERIAL_RELOAD, input(false, false, false, true, true, false, false, false, false));
        assertAction(SandboxControls.SandboxAction.DEMONSTRATE_MISSING_MESH, input(false, false, false, true, false, true, false, false, false));
        assertAction(SandboxControls.SandboxAction.RELOAD_ASSET_HANDLES, input(false, false, false, true, false, false, false, true, false));

    }

    @Test
    void assetControlModifierPrecedenceIsControlThenAltThenShift() {

        assertAction(SandboxControls.SandboxAction.RELOAD_ASSET_HANDLES, input(false, false, false, true, true, true, false, true, false));
        assertAction(SandboxControls.SandboxAction.DEMONSTRATE_MISSING_MESH, input(false, false, false, true, true, true, false, false, false));

    }

    @Test
    void controlQIsTheOnlyQCombinationThatRequestsExit() {

        assertTrue(SandboxControls.resolve(input(false, false, true, false, false, false, false, false, false)).isEmpty());
        assertAction(SandboxControls.SandboxAction.EXIT, input(false, false, true, false, false, false, false, true, false));
        assertAction(SandboxControls.SandboxAction.EXIT, input(false, false, true, false, false, false, false, false, true));

    }

    @Test
    void independentControlsCanBeRequestedInTheSameFrame() {

        EnumSet<SandboxControls.SandboxAction> expected = EnumSet.of(SandboxControls.SandboxAction.CYCLE_WINDOW_MODE, SandboxControls.SandboxAction.TOGGLE_CURSOR_CAPTURE,
            SandboxControls.SandboxAction.EXIT, SandboxControls.SandboxAction.RELOAD_ASSET_HANDLES);
        assertEquals(expected, SandboxControls.resolve(input(true, true, true, true, false, false, false, true, false)));

    }

    private static void assertAction(SandboxControls.SandboxAction action, SandboxControls.SandboxControlInput input) {

        assertEquals(EnumSet.of(action), SandboxControls.resolve(input));

    }

    private static SandboxControls.SandboxControlInput input(boolean fPressed, boolean rPressed, boolean qPressed, boolean ePressed, boolean rightShiftHeld, boolean leftAltHeld,
        boolean rightAltHeld, boolean leftControlHeld, boolean rightControlHeld) {

        return new SandboxControls.SandboxControlInput(fPressed, rPressed, qPressed, ePressed, rightShiftHeld, leftAltHeld, rightAltHeld, leftControlHeld, rightControlHeld);

    }
}
