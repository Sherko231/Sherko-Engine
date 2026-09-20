package com.samo.engine.platform.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.samo.engine.core.api.InputResponseSettings;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import org.junit.jupiter.api.Test;

class InputActionEvaluatorResponseSettingsTest {
    @Test
    void neutralConstructorPreservesExistingMouseLookBehavior() {
        InputActionEvaluator evaluator = new InputActionEvaluator(lookBindings(2.0d, -0.5d));

        InputActionState look = evaluator.evaluate(snapshotWithMouseDelta(1L, 3.0d, -4.0d)).state(InputAction.LOOK);

        assertThat(look.x()).isEqualTo(6.0d);
        assertThat(look.y()).isEqualTo(2.0d);
    }

    @Test
    void appliesMouseResponseBeforeBindingScaleAndAggregation() {
        InputResponseSettings settings = new InputResponseSettings(2.0d, true, 0.0d, 1.0d);
        InputActionEvaluator evaluator = new InputActionEvaluator(lookBindings(1.5d, 0.5d), settings);

        InputActionState look = evaluator.evaluate(snapshotWithMouseDelta(10L, 3.0d, -4.0d)).state(InputAction.LOOK);

        assertThat(look.x()).isEqualTo(9.0d);
        assertThat(look.y()).isEqualTo(4.0d);
    }

    @Test
    void replacingSettingsAffectsOnlyFutureFramesAndPreservesFrameHistory() {
        InputActionEvaluator evaluator = new InputActionEvaluator(lookBindings(1.0d, 1.0d));

        InputActionSnapshot first = evaluator.evaluate(snapshotWithMouseDelta(20L, 1.0d, 2.0d));
        evaluator.setResponseSettings(new InputResponseSettings(3.0d, true, 0.0d, 1.0d));
        InputActionSnapshot second = evaluator.evaluate(snapshotWithMouseDelta(21L, 1.0d, 2.0d));

        assertThat(first.state(InputAction.LOOK).x()).isEqualTo(1.0d);
        assertThat(first.state(InputAction.LOOK).y()).isEqualTo(2.0d);
        assertThat(second.state(InputAction.LOOK).x()).isEqualTo(3.0d);
        assertThat(second.state(InputAction.LOOK).y()).isEqualTo(-6.0d);

        assertThatThrownBy(() -> evaluator.evaluate(snapshotWithMouseDelta(21L, 0.0d, 0.0d))).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("strictly increasing");
    }

    @Test
    void rejectsNullSettings() {
        InputActionBindings bindings = lookBindings(1.0d, 1.0d);

        assertThatThrownBy(() -> new InputActionEvaluator(bindings, null)).isInstanceOf(NullPointerException.class).hasMessageContaining("responseSettings");

        InputActionEvaluator evaluator = new InputActionEvaluator(bindings);
        assertThatThrownBy(() -> evaluator.setResponseSettings(null)).isInstanceOf(NullPointerException.class).hasMessageContaining("responseSettings");
    }

    @Test
    void responseFailureDoesNotAdvanceEvaluatorFrameState() {
        InputActionEvaluator evaluator = new InputActionEvaluator(lookBindings(1.0d, 1.0d), new InputResponseSettings(Double.MAX_VALUE, false, 0.0d, 1.0d));

        assertThatThrownBy(() -> evaluator.evaluate(snapshotWithMouseDelta(30L, 2.0d, 0.0d))).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("finite");

        evaluator.setResponseSettings(InputResponseSettings.defaults());
        assertThat(evaluator.evaluate(snapshotWithMouseDelta(30L, 1.0d, 0.0d)).frameId()).isEqualTo(30L);
    }

    private static InputActionBindings lookBindings(double xScale, double yScale) {
        EnumMap<InputAction, List<InputBinding>> values = new EnumMap<>(InputAction.class);
        for (InputAction action : InputAction.values()) {
            InputActionComponent component = action.valueType() == InputActionValueType.DIGITAL ? InputActionComponent.VALUE : InputActionComponent.X;
            values.put(action, List.of(new InputBinding(new InputBinding.KeyControl(InputKey.W), component, 1.0d)));
        }
        values.put(InputAction.LOOK, List.of(new InputBinding(new InputBinding.MouseDeltaControl(InputBinding.MouseDeltaAxis.X), InputActionComponent.X, xScale),
            new InputBinding(new InputBinding.MouseDeltaControl(InputBinding.MouseDeltaAxis.Y), InputActionComponent.Y, yScale)));
        return new InputActionBindings(values);
    }

    private static InputSnapshot snapshotWithMouseDelta(long frameId, double deltaX, double deltaY) {
        return new InputSnapshot(frameId, true, true, EnumSet.noneOf(InputKey.class), EnumSet.noneOf(InputKey.class), EnumSet.noneOf(InputKey.class),
            EnumSet.noneOf(InputMouseButton.class), EnumSet.noneOf(InputMouseButton.class), EnumSet.noneOf(InputMouseButton.class), deltaX, deltaY);
    }
}
