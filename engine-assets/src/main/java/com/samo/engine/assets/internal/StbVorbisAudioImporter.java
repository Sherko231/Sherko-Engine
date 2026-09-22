package com.samo.engine.assets.internal;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import org.lwjgl.stb.STBVorbis;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

final class StbVorbisAudioImporter {
    private StbVorbisAudioImporter() {

    }

    static CookedAudio importFile(Path sourcePath) {

        if (sourcePath == null) {
            throw new NullPointerException("sourcePath");
        }

        String fileName = sourcePath.getFileName().toString().toLowerCase(Locale.ROOT);
        if (!fileName.endsWith(".ogg")) {
            throw new AssetCookerException(sourcePath + ": AUDIO source must use .ogg Ogg Vorbis");
        }

        byte[] sourceBytes;
        try {
            sourceBytes = Files.readAllBytes(sourcePath);
        } catch (IOException exception) {
            throw new AssetCookerException(sourcePath + ": failed to read Ogg Vorbis source", exception);
        }

        ByteBuffer nativeBytes = MemoryUtil.memAlloc(sourceBytes.length);
        ShortBuffer decodedPcm = null;
        try {
            nativeBytes.put(sourceBytes).flip();
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer channelsValue = stack.mallocInt(1);
                IntBuffer sampleRateValue = stack.mallocInt(1);
                decodedPcm = STBVorbis.stb_vorbis_decode_memory(nativeBytes, channelsValue, sampleRateValue);
                if (decodedPcm == null) {
                    throw new AssetCookerException(sourcePath + ": Ogg Vorbis full decode failed");
                }

                int channels = channelsValue.get(0);
                int sampleRate = sampleRateValue.get(0);
                if (channels != 1 && channels != 2) {
                    throw new AssetCookerException(sourcePath + ": Ogg Vorbis channel count must be mono or stereo, got " + channels);
                }
                if (sampleRate <= 0) {
                    throw new AssetCookerException(sourcePath + ": Ogg Vorbis sample rate must be positive, got " + sampleRate);
                }
                return new CookedAudio(channels, sampleRate, sourceBytes);
            }
        } finally {
            if (decodedPcm != null) {
                MemoryUtil.memFree(decodedPcm);
            }
            MemoryUtil.memFree(nativeBytes);
        }

    }
}
