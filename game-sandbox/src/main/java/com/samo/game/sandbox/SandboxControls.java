package com.samo.game.sandbox;

import java.util.EnumSet;

/** Pure owner-control mapping for the persistent sandbox playground. */
final class SandboxControls {
    private SandboxControls() {
    }

    static EnumSet<Action> resolve(Input input) {
        EnumSet<Action> actions = EnumSet.noneOf(Action.class);
        if (input.qPressed() && (input.leftControlHeld() || input.rightControlHeld())) {
            actions.add(Action.EXIT);
        }
        if (input.fPressed()) {
            actions.add(input.rightShiftHeld()
                    ? Action.CYCLE_MOUSE_SENSITIVITY
                    : Action.CYCLE_WINDOW_MODE);
        }
        if (input.rPressed()) {
            actions.add(input.rightShiftHeld()
                    ? Action.TOGGLE_MOUSE_Y_INVERSION
                    : Action.TOGGLE_CURSOR_CAPTURE);
        }
        return actions;
    }

    enum Action {
        CYCLE_WINDOW_MODE,
        TOGGLE_CURSOR_CAPTURE,
        CYCLE_MOUSE_SENSITIVITY,
        TOGGLE_MOUSE_Y_INVERSION,
        EXIT
    }

    record Input(
            boolean fPressed,
            boolean rPressed,
            boolean qPressed,
            boolean rightShiftHeld,
            boolean leftControlHeld,
            boolean rightControlHeld) {
    }
}
