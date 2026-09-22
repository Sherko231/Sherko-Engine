package com.samo.engine.assets.internal;

import java.util.Arrays;

record CookedAudio(int channels, int sampleRate, byte[] vorbisPayload) {
    CookedAudio {

        if (channels != 1 && channels != 2) {
            throw new IllegalArgumentException("Audio channels must be mono or stereo");
        }
        if (sampleRate <= 0) {
            throw new IllegalArgumentException("Audio sample rate must be positive");
        }
        if (vorbisPayload == null || vorbisPayload.length == 0) {
            throw new IllegalArgumentException("Vorbis payload must be nonempty");
        }
        vorbisPayload = Arrays.copyOf(vorbisPayload, vorbisPayload.length);

    }

    @Override
    public byte[] vorbisPayload() {

        return Arrays.copyOf(vorbisPayload, vorbisPayload.length);

    }
}
