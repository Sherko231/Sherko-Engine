package com.samo.engine.assets.internal;

final class TextureSizes {
    private TextureSizes() {

    }

    static int rgba8ByteCount(int width, int height) {

        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Texture dimensions must be positive");
        }
        try {
            return Math.multiplyExact(Math.multiplyExact(width, height), 4);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("Texture dimensions exceed supported RGBA8 size", exception);
        }

    }
}
