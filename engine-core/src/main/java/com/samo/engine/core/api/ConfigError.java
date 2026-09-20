package com.samo.engine.core.api;

import java.util.Objects;

/** One source-aware user configuration validation error. */
public record ConfigError(String key, ConfigSource source, String message) {
    public ConfigError {

        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(message, "message");
        if (message.isBlank()) {
            throw new IllegalArgumentException("message must not be blank");
        }

    }
}
