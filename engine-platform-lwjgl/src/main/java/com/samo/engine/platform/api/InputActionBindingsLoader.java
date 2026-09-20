package com.samo.engine.platform.api;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

final class InputActionBindingsLoader {
    private InputActionBindingsLoader() {
    }

    static InputActionBindings load(Path path) {
        if (!Files.isRegularFile(path) || !Files.isReadable(path)) {
            throw failure(path, "binding file is missing or unreadable");
        }

        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return InputActionBindingsJsonParser.parse(path, reader);
        } catch (IOException exception) {
            throw failure(path, "failed to read binding JSON", exception);
        }
    }

    private static InputBindingLoadException failure(Path path, String message) {
        return new InputBindingLoadException(path + ": " + message);
    }

    private static InputBindingLoadException failure(Path path, String message, Throwable cause) {
        return new InputBindingLoadException(path + ": " + message, cause);
    }
}
