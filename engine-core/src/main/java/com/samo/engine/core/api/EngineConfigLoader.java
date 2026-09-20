package com.samo.engine.core.api;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Loads fixed startup configuration layers and validates the merged effective values. */
public final class EngineConfigLoader {
    private final EngineConfigSchema schema = new EngineConfigSchema();

    public EngineConfigLoader() {

    }

    public Map<ConfigKey<?>, Object> load(Path gameConfig, Path userConfig, Map<String, String> commandLineOverrides) throws IOException {

        Objects.requireNonNull(gameConfig, "gameConfig");
        Objects.requireNonNull(userConfig, "userConfig");
        Objects.requireNonNull(commandLineOverrides, "commandLineOverrides");
        validateCommandLineEntries(commandLineOverrides);

        Map<String, ConfigEntry> effective = new LinkedHashMap<>();
        mergeFile(effective, gameConfig);
        mergeFile(effective, userConfig);
        mergeCommandLine(effective, commandLineOverrides);
        return schema.validate(effective);

    }

    private static void validateCommandLineEntries(Map<String, String> commandLineOverrides) {

        for (Map.Entry<String, String> entry : commandLineOverrides.entrySet()) {
            Objects.requireNonNull(entry.getKey(), "commandLineOverrides key");
            Objects.requireNonNull(entry.getValue(), "commandLineOverrides value");
        }

    }

    private static void mergeFile(Map<String, ConfigEntry> effective, Path path) throws IOException {

        Path normalized = path.toAbsolutePath().normalize();
        if (Files.notExists(normalized)) {
            return;
        }

        Set<String> seenKeys = new HashSet<>();
        try (BufferedReader reader = Files.newBufferedReader(normalized, StandardCharsets.UTF_8)) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }

                int separator = line.indexOf('=');
                if (separator < 0) {
                    throw malformed(normalized, lineNumber, "missing '=' separator");
                }

                String key = line.substring(0, separator).trim();
                if (key.isEmpty()) {
                    throw malformed(normalized, lineNumber, "configuration key must not be blank");
                }
                if (!seenKeys.add(key)) {
                    throw malformed(normalized, lineNumber, "duplicate configuration key '" + key + "'");
                }

                String value = line.substring(separator + 1);
                replaceEffective(effective, key, new ConfigEntry(value, new ConfigSource(normalized + ":" + lineNumber)));
            }
        }

    }

    private static void mergeCommandLine(Map<String, ConfigEntry> effective, Map<String, String> commandLineOverrides) {

        for (Map.Entry<String, String> entry : commandLineOverrides.entrySet()) {
            replaceEffective(effective, entry.getKey(), new ConfigEntry(entry.getValue(), new ConfigSource("command line")));
        }

    }

    private static void replaceEffective(Map<String, ConfigEntry> effective, String key, ConfigEntry value) {

        effective.remove(key);
        effective.put(key, value);

    }

    private static IllegalArgumentException malformed(Path path, int lineNumber, String detail) {

        return new IllegalArgumentException("Malformed configuration file " + path + ":" + lineNumber + ": " + detail);

    }
}
