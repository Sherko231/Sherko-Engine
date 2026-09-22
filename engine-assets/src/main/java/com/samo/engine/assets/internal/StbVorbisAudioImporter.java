package com.samo.engine.assets.internal;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import org.lwjgl.stb.STBVorbis;
import org.lwjgl.stb.STBVorbisInfo;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

final class StbVorbisAudioImporter {
    private static final int DECODE_BUFFER_FRAMES = 4096;

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
        long decoder = 0L;
        try {
            nativeBytes.put(sourceBytes).flip();
            try (MemoryStack stack = MemoryStack.stackPush()) {
                int[] openError = new int[1];
                decoder = STBVorbis.stb_vorbis_open_memory(nativeBytes, openError, null);
                if (decoder == 0L) {
                    throw new AssetCookerException(sourcePath + ": Ogg Vorbis open failed with stb_vorbis error " + openError[0]);
                }

                STBVorbisInfo info = STBVorbisInfo.malloc(stack);
                STBVorbis.stb_vorbis_get_info(decoder, info);
                int channels = info.channels();
                int sampleRate = info.sample_rate();
                if (channels != 1 && channels != 2) {
                    throw new AssetCookerException(sourcePath + ": Ogg Vorbis channel count must be mono or stereo, got " + channels);
                }
                if (sampleRate <= 0) {
                    throw new AssetCookerException(sourcePath + ": Ogg Vorbis sample rate must be positive, got " + sampleRate);
                }

                validateCompleteDecode(sourcePath, decoder, channels, stack);
                return new CookedAudio(channels, sampleRate, sourceBytes);
            }
        } finally {
            if (decoder != 0L) {
                STBVorbis.stb_vorbis_close(decoder);
            }
            MemoryUtil.memFree(nativeBytes);
        }

    }

    private static void validateCompleteDecode(Path sourcePath, long decoder, int channels, MemoryStack stack) {

        ShortBuffer decodeBuffer = stack.mallocShort(DECODE_BUFFER_FRAMES * channels);
        while (true) {
            decodeBuffer.clear();
            int decodedFrames = STBVorbis.stb_vorbis_get_samples_short_interleaved(decoder, channels, decodeBuffer);
            if (decodedFrames == 0) {
                break;
            }
        }

        int decodeError = STBVorbis.stb_vorbis_get_error(decoder);
        if (decodeError != STBVorbis.VORBIS__no_error) {
            throw new AssetCookerException(sourcePath + ": Ogg Vorbis decode failed with stb_vorbis error " + decodeError);
        }

    }
}
