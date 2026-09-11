package com.samo.engine.core.api;

import java.util.Objects;

/** Identifies where one raw configuration value came from for diagnostics. */
public record ConfigSource(String description) {
    public ConfigSource {
        Objects.requireNonNull(description, "description");
        if (description.isBlank()) {
            throw new IllegalArgumentException("description must not be blank");
        }
    }
}
