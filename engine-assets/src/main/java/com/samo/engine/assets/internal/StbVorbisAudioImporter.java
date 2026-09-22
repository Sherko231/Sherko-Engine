package com.samo.engine.assets.internal;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import org.lwjgl.stb.STBVorbis;
import org.lwjgl.stb.STBVorbisInfo;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

final class StbVorbisAudioImporter {
    private static final int VALIDATION_SAMPLES_PER_CHANNEL = 4096;

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
        long decoder = MemoryUtil.NULL;
        ShortBuffer validationPcm = null;
        try {
            nativeBytes.put(sourceBytes).flip();
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer openError = stack.mallocInt(1);
                decoder = STBVorbis.stb_vorbis_open_memory(nativeBytes, openError, null);
                if (decoder == MemoryUtil.NULL) {
                    throw new AssetCookerException(sourcePath + ": Ogg Vorbis open failed with stb error " + openError.get(0));
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

                int validationShorts = Math.multiplyExact(VALIDATION_SAMPLES_PER_CHANNEL, channels);
                validationPcm = MemoryUtil.memAllocShort(validationShorts);
                while (true) {
                    validationPcm.clear();
                    int samples = STBVorbis.stb_vorbis_get_samples_short_interleaved(decoder, channels, validationPcm);
                    if (samples == 0) {
                        break;
                    }
                }

                int decodeError = STBVorbis.stb_vorbis_get_error(decoder);
                if (decodeError != STBVorbis.VORBIS__no_error) {
                    throw new AssetCookerException(sourcePath + ": Ogg Vorbis decode failed with stb error " + decodeError);
                }
                return new CookedAudio(channels, sampleRate, sourceBytes);
            }
        } catch (ArithmeticException exception) {
            throw new AssetCookerException(sourcePath + ": Ogg Vorbis validation buffer size overflow", exception);
        } finally {
            if (validationPcm != null) {
                MemoryUtil.memFree(validationPcm);
            }
            if (decoder != MemoryUtil.NULL) {
                STBVorbis.stb_vorbis_close(decoder);
            }
            MemoryUtil.memFree(nativeBytes);
        }

    }
}
