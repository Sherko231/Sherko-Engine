package com.samo.engine.world.internal;

final class PrefabFormatException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    PrefabFormatException(String message) {

        super(message);

    }

    PrefabFormatException(String message, Throwable cause) {

        super(message, cause);

    }
}
