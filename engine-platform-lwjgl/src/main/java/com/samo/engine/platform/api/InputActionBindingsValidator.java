package com.samo.engine.platform.api;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Validates and defensively copies one complete input-action binding set. */
final class InputActionBindingsValidator {
    private InputActionBindingsValidator() {

    }

    static Map<InputAction, List<InputBinding>> validateAndCopy(Map<InputAction, ? extends List<InputBinding>> bindings) {

        Objects.requireNonNull(bindings, "bindings");

        EnumMap<InputAction, List<InputBinding>> copy = new EnumMap<>(InputAction.class);
        for (InputAction action : InputAction.values()) {
            List<InputBinding> actionBindings = bindings.get(action);
            if (actionBindings == null) {
                throw new IllegalArgumentException("missing bindings for action " + action);
            }
            if (actionBindings.isEmpty()) {
                throw new IllegalArgumentException("action " + action + " must have at least one binding");
            }

            ArrayList<InputBinding> bindingCopy = new ArrayList<>(actionBindings.size());
            for (InputBinding binding : actionBindings) {
                InputBinding nonNullBinding = Objects.requireNonNull(binding, "binding for " + action);
                validateActionComponent(action, nonNullBinding);
                if (bindingCopy.contains(nonNullBinding)) {
                    throw new IllegalArgumentException("duplicate binding for action " + action + ": " + nonNullBinding);
                }
                bindingCopy.add(nonNullBinding);
            }
            copy.put(action, List.copyOf(bindingCopy));
        }

        if (bindings.size() != InputAction.values().length) {
            throw new IllegalArgumentException("binding map contains unsupported action entries");
        }
        return Map.copyOf(copy);

    }

    static void validateActionComponent(InputAction action, InputBinding binding) {

        InputActionComponent component = binding.component();
        if (action.valueType() == InputActionValueType.DIGITAL && component != InputActionComponent.VALUE) {
            throw new IllegalArgumentException("digital action " + action + " requires VALUE bindings");
        }
        if (action.valueType() == InputActionValueType.VECTOR2 && component == InputActionComponent.VALUE) {
            throw new IllegalArgumentException("vector action " + action + " requires X or Y bindings");
        }

    }
}
