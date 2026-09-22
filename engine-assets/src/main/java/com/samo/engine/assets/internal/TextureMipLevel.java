package com.samo.engine.assets.internal;

import java.util.Arrays;

record TextureMipLevel(int width, int height, byte[] rgba8) {
    TextureMipLevel {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Mip dimensions must be positive");
        }
        int expectedLength = TextureSizes.rgba8ByteCount(width, height);
        if (rgba8 == null || rgba8.length != expectedLength) {
            throw new IllegalArgumentException("RGBA8 byte length does not match mip dimensions");
        }
        rgba8 = Arrays.copyOf(rgba8, rgba8.length);
    }

    @Override
    public byte[] rgba8() {
        return Arrays.copyOf(rgba8, rgba8.length);
    }
}
