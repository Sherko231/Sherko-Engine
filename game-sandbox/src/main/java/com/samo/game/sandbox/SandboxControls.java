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
        if (input.ePressed()) {
            if (input.leftControlHeld() || input.rightControlHeld()) {
                actions.add(SandboxAction.RELOAD_ASSET_HANDLES);
            } else if (input.leftAltHeld() || input.rightAltHeld()) {
                actions.add(SandboxAction.DEMONSTRATE_MISSING_MESH);
            } else if (input.rightShiftHeld()) {
                actions.add(SandboxAction.DEMONSTRATE_FAILED_MATERIAL_RELOAD);
            } else {
                actions.add(SandboxAction.CYCLE_VALID_MATERIAL);
            }
        }
        return actions;

    }

    enum SandboxAction {
        CYCLE_WINDOW_MODE, TOGGLE_CURSOR_CAPTURE, CYCLE_MOUSE_SENSITIVITY, TOGGLE_MOUSE_Y_INVERSION, CYCLE_VALID_MATERIAL, DEMONSTRATE_FAILED_MATERIAL_RELOAD,
        DEMONSTRATE_MISSING_MESH, RELOAD_ASSET_HANDLES, EXIT
    }

    record SandboxControlInput(boolean fPressed, boolean rPressed, boolean qPressed, boolean ePressed, boolean rightShiftHeld, boolean leftAltHeld, boolean rightAltHeld,
        boolean leftControlHeld, boolean rightControlHeld) {
    }
}
