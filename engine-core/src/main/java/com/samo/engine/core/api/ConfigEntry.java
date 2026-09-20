package com.samo.engine.core.api;

import java.util.Objects;

/** One raw configuration value paired with its diagnostic source. */
public record ConfigEntry(String value, ConfigSource source) {
    public ConfigEntry {

        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(source, "source");

    }
}
