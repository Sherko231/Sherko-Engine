package com.samo.engine.core.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EngineConfigLoaderTest {
    @TempDir
    Path tempDir;

    private final EngineConfigLoader loader = new EngineConfigLoader();

    @Test
    void exactPrecedenceFallsBackOneLayerAtATime() throws IOException {
        Path game = write("game.cfg", "fullscreen.width=1280\n");
        Path user = write("user.cfg", "fullscreen.width=1600\n");

        assertWidth(2560, loader.load(game, user, Map.of("fullscreen.width", "2560")));
        assertWidth(1600, loader.load(game, user, Map.of()));
        assertWidth(1280, loader.load(game, missing("missing-user.cfg"), Map.of()));
        assertWidth(1920, loader.load(missing("missing-game.cfg"), missing("missing-user.cfg"), Map.of()));
    }

    @Test
    void higherLayersDoNotEraseIndependentLowerLayerKeys() throws IOException {
        Path game = write("game.cfg", "fullscreen.width=1280\nfullscreen.height=720\nsimulation.tickRate=60\n");
        Path user = write("user.cfg", "fullscreen.width=1600\n");

        Map<ConfigKey<?>, Object> config = loader.load(game, user, Map.of("simulation.tickRate", "60"));

        assertEquals(1600, config.get(EngineConfigSchema.FULLSCREEN_WIDTH));
        assertEquals(720, config.get(EngineConfigSchema.FULLSCREEN_HEIGHT));
        assertEquals(60, config.get(EngineConfigSchema.TICK_RATE));
    }

    @Test
    void missingFilesAreAbsentLayers() throws IOException {
        Map<ConfigKey<?>, Object> config = loader.load(missing("missing-game.cfg"), missing("missing-user.cfg"), Map.of());

        assertEquals(1920, config.get(EngineConfigSchema.FULLSCREEN_WIDTH));
        assertEquals(1080, config.get(EngineConfigSchema.FULLSCREEN_HEIGHT));
        assertEquals(60, config.get(EngineConfigSchema.TICK_RATE));
    }

    @Test
    void winningUserValuePreservesFileAndLineSource() throws IOException {
        Path game = write("game.cfg", "fullscreen.width=1280\n");
        Path user = write("user.cfg", "# comment\nfullscreen.width=0\n");

        ConfigValidationException exception = assertThrows(ConfigValidationException.class, () -> loader.load(game, user, Map.of()));

        ConfigError error = exception.errors().getFirst();
        assertEquals("fullscreen.width", error.key());
        assertEquals(user.toAbsolutePath().normalize() + ":2", error.source().description());
    }

    @Test
    void winningCommandLineValuePreservesCommandLineSource() throws IOException {
        Path missingGame = missing("game.cfg");
        Path missingUser = missing("user.cfg");

        ConfigValidationException exception = assertThrows(ConfigValidationException.class, () -> loader.load(missingGame, missingUser, Map.of("simulation.tickRate", "120")));

        assertEquals("simulation.tickRate", exception.errors().getFirst().key());
        assertEquals("command line", exception.errors().getFirst().source().description());
    }

    @Test
    void invalidLowerValueOverriddenByValidHigherValueDoesNotFail() throws IOException {
        Path game = write("game.cfg", "fullscreen.width=0\n");

        Map<ConfigKey<?>, Object> config = loader.load(game, missing("user.cfg"), Map.of("fullscreen.width", "2560"));

        assertWidth(2560, config);
    }

    @Test
    void malformedLineWithoutSeparatorFailsWithLocation() throws IOException {
        Path game = write("game.cfg", "fullscreen.width 1920\n");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> loader.load(game, missing("user.cfg"), Map.of()));

        assertTrue(exception.getMessage().contains(game.toAbsolutePath().normalize() + ":1"));
        assertTrue(exception.getMessage().contains("missing '=' separator"));
    }

    @Test
    void blankKeyFailsWithLocation() throws IOException {
        Path game = write("game.cfg", "   =1920\n");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> loader.load(game, missing("user.cfg"), Map.of()));

        assertTrue(exception.getMessage().contains(game.toAbsolutePath().normalize() + ":1"));
        assertTrue(exception.getMessage().contains("must not be blank"));
    }

    @Test
    void duplicateKeyInsideOneFileFailsAtSecondOccurrence() throws IOException {
        Path game = write("game.cfg", "fullscreen.width=1280\nfullscreen.width=1600\n");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> loader.load(game, missing("user.cfg"), Map.of()));

        assertTrue(exception.getMessage().contains(game.toAbsolutePath().normalize() + ":2"));
        assertTrue(exception.getMessage().contains("duplicate configuration key 'fullscreen.width'"));
    }

    @Test
    void commentsBlankLinesAndWhitespaceAroundKeysAreHandled() throws IOException {
        Path game = write("game.cfg", "\n   # comment\n fullscreen.width = 1280 \n\nfullscreen.height=720\n");

        Map<ConfigKey<?>, Object> config = loader.load(game, missing("user.cfg"), Map.of());

        assertEquals(1280, config.get(EngineConfigSchema.FULLSCREEN_WIDTH));
        assertEquals(720, config.get(EngineConfigSchema.FULLSCREEN_HEIGHT));
    }

    @Test
    void firstSeparatorOnlyLeavesAdditionalSeparatorsInRawValue() throws IOException {
        Path game = write("game.cfg", "fullscreen.width=1280=extra\n");

        ConfigValidationException exception = assertThrows(ConfigValidationException.class, () -> loader.load(game, missing("user.cfg"), Map.of()));

        assertEquals("fullscreen.width", exception.errors().getFirst().key());
        assertEquals("must be a valid integer", exception.errors().getFirst().message());
        assertEquals(game.toAbsolutePath().normalize() + ":1", exception.errors().getFirst().source().description());
    }

    @Test
    void programmerContractFailuresHappenBeforeFileWork() {
        Path unreadableIfReached = tempDir;

        assertThrows(NullPointerException.class, () -> loader.load(null, tempDir, Map.of()));
        assertThrows(NullPointerException.class, () -> loader.load(tempDir, null, Map.of()));
        assertThrows(NullPointerException.class, () -> loader.load(tempDir, tempDir, null));

        Map<String, String> nullKey = new HashMap<>();
        nullKey.put(null, "1");
        assertThrows(NullPointerException.class, () -> loader.load(unreadableIfReached, unreadableIfReached, nullKey));

        Map<String, String> nullValue = new HashMap<>();
        nullValue.put("fullscreen.width", null);
        assertThrows(NullPointerException.class, () -> loader.load(unreadableIfReached, unreadableIfReached, nullValue));
    }

    @Test
    void existingNonFileLayerPropagatesIOException() {
        assertThrows(IOException.class, () -> loader.load(tempDir, missing("user.cfg"), Map.of()));
    }

    @Test
    void successfulOutputRemainsImmutable() throws IOException {
        Map<ConfigKey<?>, Object> config = loader.load(missing("game.cfg"), missing("user.cfg"), Map.of());

        assertThrows(UnsupportedOperationException.class, () -> config.put(EngineConfigSchema.FULLSCREEN_WIDTH, 800));
    }

    @Test
    void loadingFailurePreventsSubsystemInitialization() throws IOException {
        Path user = write("user.cfg", "fullscreen.width=0\n");
        CountingSubsystem subsystem = new CountingSubsystem();

        assertThrows(ConfigValidationException.class, () -> {
            loader.load(missing("game.cfg"), user, Map.of());
            subsystem.initialize();
        });

        assertEquals(0, subsystem.initializeCalls);
    }

    private Path write(String name, String content) throws IOException {
        Path path = tempDir.resolve(name);
        Files.writeString(path, content, StandardCharsets.UTF_8);
        return path;
    }

    private Path missing(String name) {
        return tempDir.resolve(name);
    }

    private static void assertWidth(int expected, Map<ConfigKey<?>, Object> config) {
        assertEquals(expected, config.get(EngineConfigSchema.FULLSCREEN_WIDTH));
    }

    private static final class CountingSubsystem extends EngineSubsystem {
        private int initializeCalls;

        @Override
        protected void onInitialize() {
            initializeCalls++;
        }

        @Override
        protected void onStart() {
        }

        @Override
        protected void onStop() {
        }

        @Override
        protected void onClose() {
        }
    }
}
