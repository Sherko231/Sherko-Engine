package com.samo.engine.platform.api;

/** Named gameplay actions whose bindings are loaded by the Phase 3 input configuration boundary. */
public enum InputAction {
    MOVE(InputActionValueType.VECTOR2), LOOK(InputActionValueType.VECTOR2), JUMP(InputActionValueType.DIGITAL), CROUCH(InputActionValueType.DIGITAL), SPRINT(
        InputActionValueType.DIGITAL), INTERACT(InputActionValueType.DIGITAL), GRAB(InputActionValueType.DIGITAL), THROW(
            InputActionValueType.DIGITAL), PRIMARY_USE(InputActionValueType.DIGITAL), PAUSE(InputActionValueType.DIGITAL), PUSH_TO_TALK(InputActionValueType.DIGITAL);

    private final InputActionValueType valueType;

    InputAction(InputActionValueType valueType) {
        this.valueType = valueType;
    }

    public InputActionValueType valueType() {
        return valueType;
    }
}
