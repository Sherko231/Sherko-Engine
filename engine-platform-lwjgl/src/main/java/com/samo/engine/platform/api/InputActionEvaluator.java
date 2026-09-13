package com.samo.engine.platform.api;

import java.util.EnumMap;
import java.util.List;
import java.util.Objects;

/** Stateful renderer-frame evaluator from hardware snapshots to gameplay action state. */
public final class InputActionEvaluator {
    private final InputActionBindings bindings;
    private final EnumMap<InputAction, Boolean> previouslyActive = new EnumMap<>(InputAction.class);
    private boolean hasPreviousFrame;
    private long lastFrameId;

    public InputActionEvaluator(InputActionBindings bindings) {
        this.bindings = Objects.requireNonNull(bindings, "bindings");
        for (InputAction action : InputAction.values()) {
            previouslyActive.put(action, false);
        }
    }

    public InputActionSnapshot evaluate(InputSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        long frameId = snapshot.frameId();
        if (hasPreviousFrame && frameId <= lastFrameId) {
            throw new IllegalArgumentException("snapshot frameId must be strictly increasing");
        }
        if (!Double.isFinite(snapshot.mouseDeltaX()) || !Double.isFinite(snapshot.mouseDeltaY())) {
            throw new IllegalArgumentException("snapshot mouse delta must be finite");
        }

        EnumMap<InputAction, InputActionState> evaluatedStates = new EnumMap<>(InputAction.class);
        EnumMap<InputAction, Boolean> nextActive = new EnumMap<>(InputAction.class);

        for (InputAction action : InputAction.values()) {
            Evaluation evaluation = evaluateAction(action, bindings.bindingsFor(action), snapshot);
            boolean previous = previouslyActive.get(action);
            boolean pressed = !previous && evaluation.active;
            boolean released = previous && !evaluation.active;

            if (!previous && !evaluation.active && evaluation.sawCompleteTap) {
                pressed = true;
                released = true;
            }

            InputActionState state = action.valueType() == InputActionValueType.DIGITAL
                    ? new InputActionState(action, pressed, evaluation.active, released, evaluation.value, 0.0d, 0.0d)
                    : new InputActionState(action, pressed, evaluation.active, released, 0.0d, evaluation.x, evaluation.y);
            evaluatedStates.put(action, state);
            nextActive.put(action, evaluation.active);
        }

        previouslyActive.clear();
        previouslyActive.putAll(nextActive);
        hasPreviousFrame = true;
        lastFrameId = frameId;
        return new InputActionSnapshot(frameId, evaluatedStates);
    }

    private static Evaluation evaluateAction(
            InputAction action,
            List<InputBinding> actionBindings,
            InputSnapshot snapshot) {
        double value = 0.0d;
        double x = 0.0d;
        double y = 0.0d;
        boolean sawCompleteTap = false;

        for (InputBinding binding : actionBindings) {
            BindingSample sample = sample(binding, snapshot);
            sawCompleteTap |= sample.pressed && sample.released;

            double contribution = multiplyFinite(sample.amount, binding.scale());
            switch (binding.component()) {
                case VALUE -> value = addFinite(value, contribution);
                case X -> x = addFinite(x, contribution);
                case Y -> y = addFinite(y, contribution);
            }
        }

        boolean active = action.valueType() == InputActionValueType.DIGITAL
                ? value != 0.0d
                : x != 0.0d || y != 0.0d;
        return new Evaluation(value, x, y, active, sawCompleteTap);
    }

    private static BindingSample sample(InputBinding binding, InputSnapshot snapshot) {
        InputBinding.Control control = binding.control();
        if (control instanceof InputBinding.KeyControl keyControl) {
            InputKey key = keyControl.key();
            return new BindingSample(
                    snapshot.keyHeld(key) ? 1.0d : 0.0d,
                    snapshot.keyPressed(key),
                    snapshot.keyReleased(key));
        }
        if (control instanceof InputBinding.MouseButtonControl buttonControl) {
            InputMouseButton button = buttonControl.button();
            return new BindingSample(
                    snapshot.mouseButtonHeld(button) ? 1.0d : 0.0d,
                    snapshot.mouseButtonPressed(button),
                    snapshot.mouseButtonReleased(button));
        }
        InputBinding.MouseDeltaControl mouseDeltaControl = (InputBinding.MouseDeltaControl) control;
        double amount = mouseDeltaControl.axis() == InputBinding.MouseDeltaAxis.X
                ? snapshot.mouseDeltaX()
                : snapshot.mouseDeltaY();
        return new BindingSample(amount, false, false);
    }

    private static double multiplyFinite(double left, double right) {
        double result = left * right;
        if (!Double.isFinite(result)) {
            throw new IllegalArgumentException("action binding contribution must be finite");
        }
        return result;
    }

    private static double addFinite(double left, double right) {
        double result = left + right;
        if (!Double.isFinite(result)) {
            throw new IllegalArgumentException("aggregated action value must be finite");
        }
        return result;
    }

    private record BindingSample(double amount, boolean pressed, boolean released) {
    }

    private record Evaluation(
            double value,
            double x,
            double y,
            boolean active,
            boolean sawCompleteTap) {
    }
}
