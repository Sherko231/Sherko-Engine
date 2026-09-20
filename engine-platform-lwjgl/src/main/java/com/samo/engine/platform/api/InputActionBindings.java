package com.samo.engine.platform.api;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Immutable complete set of Phase 3 gameplay-action bindings. */
public final class InputActionBindings {
    private final Map<InputAction, List<InputBinding>> bindings;

    public InputActionBindings(Map<InputAction, ? extends List<InputBinding>> bindings) {

        this.bindings = InputActionBindingsValidator.validateAndCopy(bindings);

    }

    public static InputActionBindings load(Path path) {

        return InputActionBindingsLoader.load(Objects.requireNonNull(path, "path"));

    }

    public List<InputBinding> bindingsFor(InputAction action) {

        return bindings.get(Objects.requireNonNull(action, "action"));

    }

    public Map<InputAction, List<InputBinding>> asMap() {

        return bindings;

    }
}
