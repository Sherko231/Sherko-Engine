package com.samo.engine.assets.internal;

import java.util.Arrays;

record TextureImage(int width, int height, byte[] rgba8) {
    TextureImage {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Texture dimensions must be positive");
        }
        int expectedLength = TextureSizes.rgba8ByteCount(width, height);
        if (rgba8 == null || rgba8.length != expectedLength) {
            throw new IllegalArgumentException("RGBA8 byte length does not match texture dimensions");
        }
        rgba8 = Arrays.copyOf(rgba8, rgba8.length);
    }

    @Override
    public byte[] rgba8() {
        return Arrays.copyOf(rgba8, rgba8.length);
    }
}
