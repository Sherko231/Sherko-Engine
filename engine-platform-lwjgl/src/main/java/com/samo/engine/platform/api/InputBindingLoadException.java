package com.samo.engine.platform.api;

/** Reports a complete action-binding file that could not be loaded or validated. */
public final class InputBindingLoadException extends RuntimeException {
    InputBindingLoadException(String message) {
        super(message);
    }

    InputBindingLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}
