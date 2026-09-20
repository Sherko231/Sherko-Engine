package com.samo.engine.platform.api;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/** Immutable evaluated gameplay-action state for one source renderer frame. */
public final class InputActionSnapshot {
    private final long frameId;
    private final Map<InputAction, InputActionState> states;

    InputActionSnapshot(long frameId, Map<InputAction, InputActionState> states) {

        if (frameId < 0L) {
            throw new IllegalArgumentException("frameId must be non-negative");
        }
        Objects.requireNonNull(states, "states");

        EnumMap<InputAction, InputActionState> copy = new EnumMap<>(InputAction.class);
        for (InputAction action : InputAction.values()) {
            InputActionState state = Objects.requireNonNull(states.get(action), "state for " + action);
            if (state.action() != action) {
                throw new IllegalArgumentException("state action mismatch for " + action);
            }
            copy.put(action, state);
        }
        if (states.size() != InputAction.values().length) {
            throw new IllegalArgumentException("action state map contains unsupported entries");
        }

        this.frameId = frameId;
        this.states = Map.copyOf(copy);

    }

    public long frameId() {

        return frameId;

    }

    public InputActionState state(InputAction action) {

        return states.get(Objects.requireNonNull(action, "action"));

    }

    public Map<InputAction, InputActionState> asMap() {

        return states;

    }
}
