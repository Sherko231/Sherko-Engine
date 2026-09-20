package com.samo.engine.platform.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import org.junit.jupiter.api.Test;

class InputActionEvaluatorEdgeTest {
    @Test
    void pressAndReleaseFromDifferentBindingsDoNotSynthesizeOneFrameTap() {

        EnumMap<InputAction, List<InputBinding>> bindings = new EnumMap<>(InputAction.class);
        for (InputAction action : InputAction.values()) {
            InputActionComponent component = action.valueType() == InputActionValueType.DIGITAL ? InputActionComponent.VALUE : InputActionComponent.X;
            bindings.put(action, List.of(key(InputKey.W, component, 1.0d)));
        }
        bindings.put(InputAction.INTERACT, List.of(key(InputKey.E, InputActionComponent.VALUE, 1.0d), key(InputKey.F, InputActionComponent.VALUE, -1.0d)));

        InputActionEvaluator evaluator = new InputActionEvaluator(new InputActionBindings(bindings));
        InputSnapshot snapshot = new InputSnapshot(7L, true, false, EnumSet.noneOf(InputKey.class), EnumSet.of(InputKey.E), EnumSet.of(InputKey.F),
            EnumSet.noneOf(InputMouseButton.class), EnumSet.noneOf(InputMouseButton.class), EnumSet.noneOf(InputMouseButton.class), 0.0d, 0.0d);

        InputActionState interact = evaluator.evaluate(snapshot).state(InputAction.INTERACT);

        assertThat(interact.pressed()).isFalse();
        assertThat(interact.held()).isFalse();
        assertThat(interact.released()).isFalse();
        assertThat(interact.value()).isZero();

    }

    private static InputBinding key(InputKey key, InputActionComponent component, double scale) {

        return new InputBinding(new InputBinding.KeyControl(key), component, scale);

    }
}
