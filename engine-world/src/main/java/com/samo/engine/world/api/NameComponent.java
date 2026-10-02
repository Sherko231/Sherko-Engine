package com.samo.engine.world.api;

import java.util.Objects;

/** Immutable human-readable world entity name. */
public record NameComponent(String name) {
    public NameComponent {

        Objects.requireNonNull(name, "name");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }

    }
}
