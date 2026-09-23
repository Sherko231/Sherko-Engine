package com.samo.engine.assets.internal;

import java.util.Arrays;

record FallbackSound(int channels, int sampleRate, short[] pcm16) {
    FallbackSound {

        if (channels != 1) {
            throw new IllegalArgumentException("Fallback sound must be mono");
        }
        if (sampleRate <= 0) {
            throw new IllegalArgumentException("Fallback sound sample rate must be positive");
        }
        if (pcm16 == null || pcm16.length == 0) {
            throw new IllegalArgumentException("Fallback sound samples must be nonempty");
        }
        pcm16 = Arrays.copyOf(pcm16, pcm16.length);

    }

    @Override
    public short[] pcm16() {

        return Arrays.copyOf(pcm16, pcm16.length);

    }
}
