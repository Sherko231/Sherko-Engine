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
        return actions;
    }

    enum SandboxAction {
        CYCLE_WINDOW_MODE, TOGGLE_CURSOR_CAPTURE, CYCLE_MOUSE_SENSITIVITY, TOGGLE_MOUSE_Y_INVERSION, EXIT
    }

    record SandboxControlInput(boolean fPressed, boolean rPressed, boolean qPressed, boolean rightShiftHeld, boolean leftControlHeld, boolean rightControlHeld) {
    }
}
