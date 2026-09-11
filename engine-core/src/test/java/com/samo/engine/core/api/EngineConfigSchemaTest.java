package com.samo.engine.core.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class EngineConfigSchemaTest {
    private final EngineConfigSchema schema = new EngineConfigSchema();

    @Test
    void emptyInputUsesAllDefaults() {
        Map<ConfigKey<?>, Object> config = schema.validate(Map.of());

        assertEquals(1920, config.get(EngineConfigSchema.FULLSCREEN_WIDTH));
        assertEquals(1080, config.get(EngineConfigSchema.FULLSCREEN_HEIGHT));
        assertEquals(60, config.get(EngineConfigSchema.TICK_RATE));
        assertEquals(3, config.size());
    }

    @Test
    void validExplicitValuesAreTyped() {
        Map<String, ConfigEntry> raw = new LinkedHashMap<>();
        raw.put("fullscreen.width", entry("2560", "game.properties:4"));
        raw.put("fullscreen.height", entry("1440", "game.properties:5"));
        raw.put("simulation.tickRate", entry("60", "game.properties:8"));

        Map<ConfigKey<?>, Object> config = schema.validate(raw);

        assertEquals(2560, config.get(EngineConfigSchema.FULLSCREEN_WIDTH));
        assertEquals(1440, config.get(EngineConfigSchema.FULLSCREEN_HEIGHT));
        assertEquals(60, config.get(EngineConfigSchema.TICK_RATE));
    }

    @Test
    void inclusiveResolutionBoundsAreAccepted() {
        Map<String, ConfigEntry> minimum = new LinkedHashMap<>();
        minimum.put("fullscreen.width", entry("320", "min-width"));
        minimum.put("fullscreen.height", entry("200", "min-height"));
        assertEquals(320, schema.validate(minimum).get(EngineConfigSchema.FULLSCREEN_WIDTH));
        assertEquals(200, schema.validate(minimum).get(EngineConfigSchema.FULLSCREEN_HEIGHT));

        Map<String, ConfigEntry> maximum = new LinkedHashMap<>();
        maximum.put("fullscreen.width", entry("16384", "max-width"));
        maximum.put("fullscreen.height", entry("16384", "max-height"));
        assertEquals(16384, schema.validate(maximum).get(EngineConfigSchema.FULLSCREEN_WIDTH));
        assertEquals(16384, schema.validate(maximum).get(EngineConfigSchema.FULLSCREEN_HEIGHT));
    }

    @Test
    void widthOutsideBoundsIsRejected() {
        assertSingleError("fullscreen.width", "319", "below");
        assertSingleError("fullscreen.width", "16385", "above");
    }

    @Test
    void heightOutsideBoundsIsRejected() {
        assertSingleError("fullscreen.height", "199", "below");
        assertSingleError("fullscreen.height", "16385", "above");
    }

    @Test
    void onlyLockedTickRateIsAccepted() {
        assertEquals(
                60,
                schema.validate(Map.of("simulation.tickRate", entry("60", "locked")))
                        .get(EngineConfigSchema.TICK_RATE));
        assertSingleError("simulation.tickRate", "59", "tick-59");
        assertSingleError("simulation.tickRate", "61", "tick-61");
        assertSingleError("simulation.tickRate", "120", "tick-120");
    }

    @Test
    void integerParsingTrimsWhitespace() {
        Map<String, ConfigEntry> raw = Map.of(
                "fullscreen.width", entry(" 2560 \t", "trimmed"));

        assertEquals(2560, schema.validate(raw).get(EngineConfigSchema.FULLSCREEN_WIDTH));
    }

    @Test
    void malformedIntegerPreservesSource() {
        ConfigSource source = new ConfigSource("game.properties:9");
        ConfigValidationException exception = assertThrows(
                ConfigValidationException.class,
                () -> schema.validate(Map.of(
                        "fullscreen.height", new ConfigEntry("not-a-number", source))));

        assertEquals(1, exception.errors().size());
        assertEquals("fullscreen.height", exception.errors().getFirst().key());
        assertSame(source, exception.errors().getFirst().source());
        assertEquals("must be a valid integer", exception.errors().getFirst().message());
    }

    @Test
    void unknownKeyIsRejectedWithSource() {
        ConfigSource source = new ConfigSource("command line");
        ConfigValidationException exception = assertThrows(
                ConfigValidationException.class,
                () -> schema.validate(Map.of(
                        "mystery.setting", new ConfigEntry("true", source))));

        assertEquals("mystery.setting", exception.errors().getFirst().key());
        assertSame(source, exception.errors().getFirst().source());
        assertEquals("unknown configuration key", exception.errors().getFirst().message());
    }

    @Test
    void multipleErrorsAreAggregatedInInputOrder() {
        ConfigSource widthSource = new ConfigSource("game.properties:4");
        ConfigSource tickSource = new ConfigSource("game.properties:8");
        ConfigSource unknownSource = new ConfigSource("game.properties:12");
        Map<String, ConfigEntry> raw = new LinkedHashMap<>();
        raw.put("fullscreen.width", new ConfigEntry("0", widthSource));
        raw.put("simulation.tickRate", new ConfigEntry("120", tickSource));
        raw.put("mystery.setting", new ConfigEntry("true", unknownSource));

        ConfigValidationException exception =
                assertThrows(ConfigValidationException.class, () -> schema.validate(raw));

        assertEquals("Configuration validation failed with 3 error(s)", exception.getMessage());
        assertEquals(
                List.of("fullscreen.width", "simulation.tickRate", "mystery.setting"),
                exception.errors().stream().map(ConfigError::key).toList());
        assertSame(widthSource, exception.errors().get(0).source());
        assertSame(tickSource, exception.errors().get(1).source());
        assertSame(unknownSource, exception.errors().get(2).source());
    }

    @Test
    void successfulOutputAndFailureErrorsAreImmutable() {
        Map<ConfigKey<?>, Object> config = schema.validate(Map.of());
        assertThrows(
                UnsupportedOperationException.class,
                () -> config.put(EngineConfigSchema.FULLSCREEN_WIDTH, 800));

        ConfigValidationException exception = assertThrows(
                ConfigValidationException.class,
                () -> schema.validate(Map.of("mystery.setting", entry("x", "test"))));
        assertThrows(
                UnsupportedOperationException.class,
                () -> exception.errors().add(
                        new ConfigError("other", new ConfigSource("test"), "bad")));
    }

    @Test
    void programmerContractFailuresHappenBeforeUserValidation() {
        assertThrows(NullPointerException.class, () -> schema.validate(null));
        assertThrows(NullPointerException.class, () -> new ConfigSource(null));
        assertThrows(IllegalArgumentException.class, () -> new ConfigSource("  "));
        assertThrows(NullPointerException.class, () -> new ConfigEntry(null, new ConfigSource("x")));
        assertThrows(NullPointerException.class, () -> new ConfigEntry("1", null));

        Map<String, ConfigEntry> nullKey = new HashMap<>();
        nullKey.put(null, entry("1", "null-key"));
        nullKey.put("mystery.setting", entry("x", "would-be-user-error"));
        assertThrows(NullPointerException.class, () -> schema.validate(nullKey));

        Map<String, ConfigEntry> nullEntry = new HashMap<>();
        nullEntry.put("fullscreen.width", null);
        nullEntry.put("mystery.setting", entry("x", "would-be-user-error"));
        assertThrows(NullPointerException.class, () -> schema.validate(nullEntry));
    }

    @Test
    void invalidConfigPreventsSubsystemInitialization() {
        CountingSubsystem subsystem = new CountingSubsystem();
        Map<String, ConfigEntry> raw = new LinkedHashMap<>();
        raw.put("fullscreen.width", entry("0", "game.properties:4"));
        raw.put("simulation.tickRate", entry("120", "game.properties:8"));

        assertThrows(
                ConfigValidationException.class,
                () -> {
                    schema.validate(raw);
                    subsystem.initialize();
                });

        assertEquals(0, subsystem.initializeCalls);
    }

    private void assertSingleError(String key, String value, String sourceDescription) {
        ConfigSource source = new ConfigSource(sourceDescription);
        ConfigValidationException exception = assertThrows(
                ConfigValidationException.class,
                () -> schema.validate(Map.of(key, new ConfigEntry(value, source))));

        assertEquals(1, exception.errors().size());
        assertEquals(key, exception.errors().getFirst().key());
        assertSame(source, exception.errors().getFirst().source());
    }

    private static ConfigEntry entry(String value, String source) {
        return new ConfigEntry(value, new ConfigSource(source));
    }

    private static final class CountingSubsystem extends EngineSubsystem {
        private int initializeCalls;

        @Override
        protected void onInitialize() {
            initializeCalls++;
        }
    }
}
