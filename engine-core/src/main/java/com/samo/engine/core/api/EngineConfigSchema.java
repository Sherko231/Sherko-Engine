package com.samo.engine.core.api;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Validates one already-resolved raw configuration map into typed engine startup values. */
public final class EngineConfigSchema {
    public static final ConfigKey<Integer> FULLSCREEN_WIDTH =
            new ConfigKey<>("fullscreen.width", 1920);
    public static final ConfigKey<Integer> FULLSCREEN_HEIGHT =
            new ConfigKey<>("fullscreen.height", 1080);
    public static final ConfigKey<Integer> TICK_RATE =
            new ConfigKey<>("simulation.tickRate", 60);

    private static final int MIN_WIDTH = 320;
    private static final int MAX_WIDTH = 16_384;
    private static final int MIN_HEIGHT = 200;
    private static final int MAX_HEIGHT = 16_384;

    public EngineConfigSchema() {
    }

    public Map<ConfigKey<?>, Object> validate(Map<String, ConfigEntry> entries) {
        Objects.requireNonNull(entries, "entries");

        for (Map.Entry<String, ConfigEntry> entry : entries.entrySet()) {
            Objects.requireNonNull(entry.getKey(), "configuration key");
            Objects.requireNonNull(entry.getValue(), "configuration entry");
        }

        Map<ConfigKey<?>, Object> validated = new LinkedHashMap<>();
        validated.put(FULLSCREEN_WIDTH, FULLSCREEN_WIDTH.defaultValue());
        validated.put(FULLSCREEN_HEIGHT, FULLSCREEN_HEIGHT.defaultValue());
        validated.put(TICK_RATE, TICK_RATE.defaultValue());

        List<ConfigError> errors = new ArrayList<>();
        for (Map.Entry<String, ConfigEntry> raw : entries.entrySet()) {
            String name = raw.getKey();
            ConfigEntry entry = raw.getValue();

            if (name.equals(FULLSCREEN_WIDTH.name())) {
                parseBoundedInteger(FULLSCREEN_WIDTH, entry, MIN_WIDTH, MAX_WIDTH, validated, errors);
            } else if (name.equals(FULLSCREEN_HEIGHT.name())) {
                parseBoundedInteger(FULLSCREEN_HEIGHT, entry, MIN_HEIGHT, MAX_HEIGHT, validated, errors);
            } else if (name.equals(TICK_RATE.name())) {
                parseLockedTickRate(entry, validated, errors);
            } else {
                errors.add(new ConfigError(name, entry.source(), "unknown configuration key"));
            }
        }

        if (!errors.isEmpty()) {
            throw new ConfigValidationException(errors);
        }
        return Map.copyOf(validated);
    }

    private static void parseBoundedInteger(
            ConfigKey<Integer> key,
            ConfigEntry entry,
            int minimum,
            int maximum,
            Map<ConfigKey<?>, Object> validated,
            List<ConfigError> errors) {
        Integer parsed = parseInteger(key, entry, errors);
        if (parsed == null) {
            return;
        }
        if (parsed < minimum || parsed > maximum) {
            errors.add(new ConfigError(
                    key.name(),
                    entry.source(),
                    "must be between " + minimum + " and " + maximum + " inclusive"));
            return;
        }
        validated.put(key, parsed);
    }

    private static void parseLockedTickRate(
            ConfigEntry entry,
            Map<ConfigKey<?>, Object> validated,
            List<ConfigError> errors) {
        Integer parsed = parseInteger(TICK_RATE, entry, errors);
        if (parsed == null) {
            return;
        }
        if (parsed != FixedStepAccumulator.TICKS_PER_SECOND) {
            errors.add(new ConfigError(
                    TICK_RATE.name(),
                    entry.source(),
                    "must equal the fixed simulation rate of " + FixedStepAccumulator.TICKS_PER_SECOND));
            return;
        }
        validated.put(TICK_RATE, parsed);
    }

    private static Integer parseInteger(
            ConfigKey<Integer> key,
            ConfigEntry entry,
            List<ConfigError> errors) {
        try {
            return Integer.valueOf(entry.value().trim());
        } catch (NumberFormatException exception) {
            errors.add(new ConfigError(key.name(), entry.source(), "must be a valid integer"));
            return null;
        }
    }
}
