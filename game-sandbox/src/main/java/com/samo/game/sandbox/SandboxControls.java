package com.samo.game.sandbox;

import java.util.EnumSet;

/** Pure owner-control mapping for the persistent sandbox playground. */
final class SandboxControls {
    private SandboxControls() {

    }

    static EnumSet<SandboxAction> resolve(SandboxControlInput input) {

        EnumSet<SandboxAction> actions = EnumSet.noneOf(SandboxAction.class);
        if (input.qPressed() && (input.leftControlHeld() || input.rightControlHeld())) {
            actions.add(SandboxAction.EXIT);
        }
        if (input.fPressed()) {
            actions.add(input.rightShiftHeld() ? SandboxAction.CYCLE_MOUSE_SENSITIVITY : SandboxAction.CYCLE_WINDOW_MODE);
        }
        if (input.rPressed()) {
            actions.add(input.rightShiftHeld() ? SandboxAction.TOGGLE_MOUSE_Y_INVERSION : SandboxAction.TOGGLE_CURSOR_CAPTURE);
        }
        if (input.gPressed()) {
            actions.add(input.rightShiftHeld() ? SandboxAction.DEMONSTRATE_FAILED_MATERIAL_RELOAD : SandboxAction.CYCLE_VALID_MATERIAL);
        }
        if (input.mPressed()) {
            actions.add(SandboxAction.DEMONSTRATE_MISSING_MESH);
        }
        if (input.hPressed()) {
            actions.add(SandboxAction.RELOAD_ASSET_HANDLES);
        }
        return actions;

    }

    enum SandboxAction {
        CYCLE_WINDOW_MODE,
        TOGGLE_CURSOR_CAPTURE,
        CYCLE_MOUSE_SENSITIVITY,
        TOGGLE_MOUSE_Y_INVERSION,
        CYCLE_VALID_MATERIAL,
        DEMONSTRATE_FAILED_MATERIAL_RELOAD,
        DEMONSTRATE_MISSING_MESH,
        RELOAD_ASSET_HANDLES,
        EXIT
    }

    record SandboxControlInput(boolean fPressed, boolean rPressed, boolean qPressed, boolean gPressed, boolean mPressed, boolean hPressed, boolean rightShiftHeld,
        boolean leftControlHeld, boolean rightControlHeld) {
    }
}
