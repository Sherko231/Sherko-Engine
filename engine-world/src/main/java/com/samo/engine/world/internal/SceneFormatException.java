package com.samo.engine.world.internal;

final class SceneFormatException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    SceneFormatException(String message) {

        super(message);

    }

    SceneFormatException(String message, Throwable cause) {

        super(message, cause);

    }
}
