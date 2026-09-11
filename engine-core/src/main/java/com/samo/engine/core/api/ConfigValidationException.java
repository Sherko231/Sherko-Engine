package com.samo.engine.core.api;

import java.util.List;
import java.util.Objects;

/** Aggregates source-aware user configuration errors from one validation call. */
public final class ConfigValidationException extends IllegalArgumentException {
    private final List<ConfigError> errors;

    ConfigValidationException(List<ConfigError> errors) {
        super(messageFor(errors));
        this.errors = List.copyOf(errors);
        if (this.errors.isEmpty()) {
            throw new IllegalArgumentException("errors must not be empty");
        }
    }

    public List<ConfigError> errors() {
        return errors;
    }

    private static String messageFor(List<ConfigError> errors) {
        Objects.requireNonNull(errors, "errors");
        return "Configuration validation failed with " + errors.size() + " error(s)";
    }
}
