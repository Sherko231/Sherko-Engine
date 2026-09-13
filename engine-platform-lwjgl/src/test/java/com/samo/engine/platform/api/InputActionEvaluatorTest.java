package com.samo.engine.platform.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import org.junit.jupiter.api.Test;

class InputActionEvaluatorTest {
    @Test
    void evaluatesDigitalPressHeldAndReleaseAcrossFrames() {
        InputActionEvaluator evaluator = new InputActionEvaluator(bindingsWith(
                InputAction.JUMP,
                List.of(key(InputKey.SPACE, InputActionComponent.VALUE, 1.0d))));

        InputActionState baseline = evaluator.evaluate(snapshot(10L)).state(InputAction.JUMP);
        InputActionState pressed = evaluator.evaluate(snapshot(
                11L,
                EnumSet.of(InputKey.SPACE),
                EnumSet.of(InputKey.SPACE),
                EnumSet.noneOf(InputKey.class))).state(InputAction.JUMP);
        InputActionState held = evaluator.evaluate(snapshot(
                12L,
                EnumSet.of(InputKey.SPACE),
                EnumSet.noneOf(InputKey.class),
                EnumSet.noneOf(InputKey.class))).state(InputAction.JUMP);
        InputActionState released = evaluator.evaluate(snapshot(
                13L,
                EnumSet.noneOf(InputKey.class),
                EnumSet.noneOf(InputKey.class),
                EnumSet.of(InputKey.SPACE))).state(InputAction.JUMP);

        assertState(baseline, false, false, false, 0.0d, 0.0d, 0.0d);
        assertState(pressed, true, true, false, 1.0d, 0.0d, 0.0d);
        assertState(held, false, true, false, 1.0d, 0.0d, 0.0d);
        assertState(released, false, false, true, 0.0d, 0.0d, 0.0d);
    }

    @Test
    void preservesCompleteOneFrameKeyTap() {
        InputActionEvaluator evaluator = new InputActionEvaluator(bindingsWith(
                InputAction.JUMP,
                List.of(key(InputKey.SPACE, InputActionComponent.VALUE, 1.0d))));

        InputActionState tap = evaluator.evaluate(snapshot(
                20L,
                EnumSet.noneOf(InputKey.class),
                EnumSet.of(InputKey.SPACE),
                EnumSet.of(InputKey.SPACE))).state(InputAction.JUMP);
        InputActionState after = evaluator.evaluate(snapshot(21L)).state(InputAction.JUMP);

        assertState(tap, true, false, true, 0.0d, 0.0d, 0.0d);
        assertState(after, false, false, false, 0.0d, 0.0d, 0.0d);
    }

    @Test
    void evaluatesMouseButtonTransitions() {
        InputActionEvaluator evaluator = new InputActionEvaluator(bindingsWith(
                InputAction.GRAB,
                List.of(mouseButton(InputMouseButton.LEFT, 1.0d))));

        InputActionState pressed = evaluator.evaluate(snapshotWithButtons(
                30L,
                EnumSet.of(InputMouseButton.LEFT),
                EnumSet.of(InputMouseButton.LEFT),
                EnumSet.noneOf(InputMouseButton.class))).state(InputAction.GRAB);
        InputActionState released = evaluator.evaluate(snapshotWithButtons(
                31L,
                EnumSet.noneOf(InputMouseButton.class),
                EnumSet.noneOf(InputMouseButton.class),
                EnumSet.of(InputMouseButton.LEFT))).state(InputAction.GRAB);

        assertState(pressed, true, true, false, 1.0d, 0.0d, 0.0d);
        assertState(released, false, false, true, 0.0d, 0.0d, 0.0d);
    }

    @Test
    void overlappingBindingsDoNotDuplicatePressOrReleaseWhileActionRemainsActive() {
        InputActionEvaluator evaluator = new InputActionEvaluator(bindingsWith(
                InputAction.INTERACT,
                List.of(
                        key(InputKey.E, InputActionComponent.VALUE, 1.0d),
                        key(InputKey.F, InputActionComponent.VALUE, 1.0d))));

        InputActionState firstPress = evaluator.evaluate(snapshot(
                40L,
                EnumSet.of(InputKey.E),
                EnumSet.of(InputKey.E),
                EnumSet.noneOf(InputKey.class))).state(InputAction.INTERACT);
        InputActionState secondPress = evaluator.evaluate(snapshot(
                41L,
                EnumSet.of(InputKey.E, InputKey.F),
                EnumSet.of(InputKey.F),
                EnumSet.noneOf(InputKey.class))).state(InputAction.INTERACT);
        InputActionState partialRelease = evaluator.evaluate(snapshot(
                42L,
                EnumSet.of(InputKey.F),
                EnumSet.noneOf(InputKey.class),
                EnumSet.of(InputKey.E))).state(InputAction.INTERACT);
        InputActionState finalRelease = evaluator.evaluate(snapshot(
                43L,
                EnumSet.noneOf(InputKey.class),
                EnumSet.noneOf(InputKey.class),
                EnumSet.of(InputKey.F))).state(InputAction.INTERACT);

        assertState(firstPress, true, true, false, 1.0d, 0.0d, 0.0d);
        assertState(secondPress, false, true, false, 2.0d, 0.0d, 0.0d);
        assertState(partialRelease, false, true, false, 1.0d, 0.0d, 0.0d);
        assertState(finalRelease, false, false, true, 0.0d, 0.0d, 0.0d);
    }

    @Test
    void vectorBindingsAddByComponentAndOppositeBindingsCancelExactly() {
        InputActionEvaluator moveEvaluator = new InputActionEvaluator(bindingsWith(
                InputAction.MOVE,
                List.of(
                        key(InputKey.W, InputActionComponent.Y, 1.0d),
                        key(InputKey.S, InputActionComponent.Y, -1.0d),
                        key(InputKey.D, InputActionComponent.X, 1.0d))));

        InputActionState move = moveEvaluator.evaluate(snapshot(
                50L,
                EnumSet.of(InputKey.W, InputKey.D),
                EnumSet.of(InputKey.W, InputKey.D),
                EnumSet.noneOf(InputKey.class))).state(InputAction.MOVE);
        InputActionState cancelled = moveEvaluator.evaluate(snapshot(
                51L,
                EnumSet.of(InputKey.W, InputKey.S),
                EnumSet.of(InputKey.S),
                EnumSet.of(InputKey.D))).state(InputAction.MOVE);

        assertState(move, true, true, false, 0.0d, 1.0d, 1.0d);
        assertState(cancelled, false, false, true, 0.0d, 0.0d, 0.0d);
    }

    @Test
    void mouseDeltaProducesVectorValueAndReleasesOnNextZeroFrame() {
        InputActionEvaluator evaluator = new InputActionEvaluator(bindingsWith(
                InputAction.LOOK,
                List.of(
                        mouseDelta(InputBinding.MouseDeltaAxis.X, InputActionComponent.X, 2.0d),
                        mouseDelta(InputBinding.MouseDeltaAxis.Y, InputActionComponent.Y, -0.5d))));

        InputActionState motion = evaluator.evaluate(snapshotWithMouseDelta(60L, 3.0d, -4.0d))
                .state(InputAction.LOOK);
        InputActionState stopped = evaluator.evaluate(snapshotWithMouseDelta(61L, 0.0d, 0.0d))
                .state(InputAction.LOOK);

        assertState(motion, true, true, false, 0.0d, 6.0d, 2.0d);
        assertState(stopped, false, false, true, 0.0d, 0.0d, 0.0d);
    }

    @Test
    void focusLossStyleSynthesizedReleaseReleasesActiveAction() {
        InputActionEvaluator evaluator = new InputActionEvaluator(bindingsWith(
                InputAction.SPRINT,
                List.of(key(InputKey.LEFT_SHIFT, InputActionComponent.VALUE, 1.0d))));

        evaluator.evaluate(snapshot(
                70L,
                EnumSet.of(InputKey.LEFT_SHIFT),
                EnumSet.of(InputKey.LEFT_SHIFT),
                EnumSet.noneOf(InputKey.class)));
        InputActionState focusLoss = evaluator.evaluate(snapshot(
                71L,
                EnumSet.noneOf(InputKey.class),
                EnumSet.noneOf(InputKey.class),
                EnumSet.of(InputKey.LEFT_SHIFT),
                false)).state(InputAction.SPRINT);

        assertState(focusLoss, false, false, true, 0.0d, 0.0d, 0.0d);
    }

    @Test
    void rejectsBadFrameOrderWithoutAdvancingState() {
        InputActionEvaluator evaluator = new InputActionEvaluator(bindingsWith(
                InputAction.JUMP,
                List.of(key(InputKey.SPACE, InputActionComponent.VALUE, 1.0d))));

        evaluator.evaluate(snapshot(
                80L,
                EnumSet.of(InputKey.SPACE),
                EnumSet.of(InputKey.SPACE),
                EnumSet.noneOf(InputKey.class)));

        assertThatThrownBy(() -> evaluator.evaluate(snapshot(80L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("strictly increasing");
        assertThatThrownBy(() -> evaluator.evaluate(snapshot(79L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("strictly increasing");

        InputActionState stillHeld = evaluator.evaluate(snapshot(
                81L,
                EnumSet.of(InputKey.SPACE),
                EnumSet.noneOf(InputKey.class),
                EnumSet.noneOf(InputKey.class))).state(InputAction.JUMP);
        assertState(stillHeld, false, true, false, 1.0d, 0.0d, 0.0d);
    }

    @Test
    void rejectsNonFiniteInputOrAggregateWithoutAdvancingState() {
        InputActionEvaluator finiteEvaluator = new InputActionEvaluator(defaultBindings());
        assertThatThrownBy(() -> finiteEvaluator.evaluate(snapshotWithMouseDelta(90L, Double.NaN, 0.0d)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("finite");
        assertThat(finiteEvaluator.evaluate(snapshot(90L)).frameId()).isEqualTo(90L);

        InputActionEvaluator overflowEvaluator = new InputActionEvaluator(bindingsWith(
                InputAction.LOOK,
                List.of(mouseDelta(
                        InputBinding.MouseDeltaAxis.X,
                        InputActionComponent.X,
                        Double.MAX_VALUE))));
        assertThatThrownBy(() -> overflowEvaluator.evaluate(snapshotWithMouseDelta(100L, 2.0d, 0.0d)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("finite");
        assertThat(overflowEvaluator.evaluate(snapshot(100L)).frameId()).isEqualTo(100L);
    }

    @Test
    void returnedSnapshotsAreImmutableStableAndNullsAreRejected() {
        assertThatThrownBy(() -> new InputActionEvaluator(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("bindings");

        InputActionEvaluator evaluator = new InputActionEvaluator(defaultBindings());
        assertThatThrownBy(() -> evaluator.evaluate(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("snapshot");

        InputActionSnapshot first = evaluator.evaluate(snapshot(110L));
        InputActionState firstJump = first.state(InputAction.JUMP);
        evaluator.evaluate(snapshot(
                111L,
                EnumSet.of(InputKey.W),
                EnumSet.of(InputKey.W),
                EnumSet.noneOf(InputKey.class)));

        assertThat(first.frameId()).isEqualTo(110L);
        assertState(firstJump, false, false, false, 0.0d, 0.0d, 0.0d);
        assertThatThrownBy(() -> first.asMap().put(InputAction.JUMP, firstJump))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> first.state(null)).isInstanceOf(NullPointerException.class);
    }

    private static InputActionBindings defaultBindings() {
        return bindingsWith(InputAction.JUMP, List.of(key(InputKey.SPACE, InputActionComponent.VALUE, 1.0d)));
    }

    private static InputActionBindings bindingsWith(InputAction action, List<InputBinding> replacements) {
        EnumMap<InputAction, List<InputBinding>> values = new EnumMap<>(InputAction.class);
        for (InputAction current : InputAction.values()) {
            InputActionComponent component = current.valueType() == InputActionValueType.DIGITAL
                    ? InputActionComponent.VALUE
                    : InputActionComponent.X;
            values.put(current, List.of(key(InputKey.W, component, 1.0d)));
        }
        values.put(action, replacements);
        return new InputActionBindings(values);
    }

    private static InputBinding key(InputKey key, InputActionComponent component, double scale) {
        return new InputBinding(new InputBinding.KeyControl(key), component, scale);
    }

    private static InputBinding mouseButton(InputMouseButton button, double scale) {
        return new InputBinding(
                new InputBinding.MouseButtonControl(button),
                InputActionComponent.VALUE,
                scale);
    }

    private static InputBinding mouseDelta(
            InputBinding.MouseDeltaAxis axis,
            InputActionComponent component,
            double scale) {
        return new InputBinding(new InputBinding.MouseDeltaControl(axis), component, scale);
    }

    private static InputSnapshot snapshot(long frameId) {
        return snapshot(
                frameId,
                EnumSet.noneOf(InputKey.class),
                EnumSet.noneOf(InputKey.class),
                EnumSet.noneOf(InputKey.class));
    }

    private static InputSnapshot snapshot(
            long frameId,
            EnumSet<InputKey> heldKeys,
            EnumSet<InputKey> pressedKeys,
            EnumSet<InputKey> releasedKeys) {
        return snapshot(frameId, heldKeys, pressedKeys, releasedKeys, true);
    }

    private static InputSnapshot snapshot(
            long frameId,
            EnumSet<InputKey> heldKeys,
            EnumSet<InputKey> pressedKeys,
            EnumSet<InputKey> releasedKeys,
            boolean focused) {
        return new InputSnapshot(
                frameId,
                focused,
                false,
                heldKeys,
                pressedKeys,
                releasedKeys,
                EnumSet.noneOf(InputMouseButton.class),
                EnumSet.noneOf(InputMouseButton.class),
                EnumSet.noneOf(InputMouseButton.class),
                0.0d,
                0.0d);
    }

    private static InputSnapshot snapshotWithButtons(
            long frameId,
            EnumSet<InputMouseButton> heldButtons,
            EnumSet<InputMouseButton> pressedButtons,
            EnumSet<InputMouseButton> releasedButtons) {
        return new InputSnapshot(
                frameId,
                true,
                false,
                EnumSet.noneOf(InputKey.class),
                EnumSet.noneOf(InputKey.class),
                EnumSet.noneOf(InputKey.class),
                heldButtons,
                pressedButtons,
                releasedButtons,
                0.0d,
                0.0d);
    }

    private static InputSnapshot snapshotWithMouseDelta(long frameId, double deltaX, double deltaY) {
        return new InputSnapshot(
                frameId,
                true,
                true,
                EnumSet.noneOf(InputKey.class),
                EnumSet.noneOf(InputKey.class),
                EnumSet.noneOf(InputKey.class),
                EnumSet.noneOf(InputMouseButton.class),
                EnumSet.noneOf(InputMouseButton.class),
                EnumSet.noneOf(InputMouseButton.class),
                deltaX,
                deltaY);
    }

    private static void assertState(
            InputActionState state,
            boolean pressed,
            boolean held,
            boolean released,
            double value,
            double x,
            double y) {
        assertThat(state.pressed()).isEqualTo(pressed);
        assertThat(state.held()).isEqualTo(held);
        assertThat(state.released()).isEqualTo(released);
        assertThat(state.value()).isEqualTo(value);
        assertThat(state.x()).isEqualTo(x);
        assertThat(state.y()).isEqualTo(y);
    }
}
