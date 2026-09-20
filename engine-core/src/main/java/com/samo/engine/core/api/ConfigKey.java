package com.samo.engine.core.api;

import java.util.Objects;

/** Canonical typed key in the engine startup configuration schema. */
public final class ConfigKey<T> {
    private final String name;
    private final T defaultValue;

    ConfigKey(String name, T defaultValue) {

        this.name = Objects.requireNonNull(name, "name");
        this.defaultValue = Objects.requireNonNull(defaultValue, "defaultValue");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }

    }

    public String name() {

        return name;

    }

    public T defaultValue() {

        return defaultValue;

    }
}
