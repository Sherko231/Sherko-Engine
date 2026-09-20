package com.samo.spike.audio;

import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALCCapabilities;
import org.lwjgl.openal.ALCapabilities;

import java.nio.ShortBuffer;

import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.openal.ALC10.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public final class OpenAL3DAudioSpike {
    private static final int SAMPLE_RATE = 48_000;
    private static final float TONE_HZ = 440.0f;
    private static final int DEFAULT_DURATION_SECONDS = 10;
    private static final float LEFT_X = -4.0f;
    private static final float RIGHT_X = 4.0f;

    private OpenAL3DAudioSpike() {

    }

    public static void main(String[] args) throws InterruptedException {

        int durationSeconds = Integer.getInteger("spike.durationSeconds", DEFAULT_DURATION_SECONDS);
        if (durationSeconds <= 0) {
            throw new IllegalArgumentException("spike.durationSeconds must be greater than zero");
        }

        long device = NULL;
        long context = NULL;
        int buffer = 0;
        int source = 0;
        int liveBuffers = 0;
        int liveSources = 0;

        try {
            device = alcOpenDevice((String) null);
            if (device == NULL) {
                throw new IllegalStateException("Failed to open default OpenAL device");
            }

            ALCCapabilities alcCapabilities = ALC.createCapabilities(device);
            context = alcCreateContext(device, (int[]) null);
            if (context == NULL) {
                throw new IllegalStateException("Failed to create OpenAL context");
            }
            if (!alcMakeContextCurrent(context)) {
                throw new IllegalStateException("Failed to make OpenAL context current");
            }

            ALCapabilities alCapabilities = AL.createCapabilities(alcCapabilities);
            if (!alCapabilities.OpenAL10) {
                throw new IllegalStateException("OpenAL 1.0 capability unavailable");
            }

            String vendor = alGetString(AL_VENDOR);
            String renderer = alGetString(AL_RENDERER);
            String version = alGetString(AL_VERSION);
            System.out.println("OpenAL vendor   : " + vendor);
            System.out.println("OpenAL renderer : " + renderer);
            System.out.println("OpenAL version  : " + version);
            System.out.printf("Requested run   : %d seconds%n", durationSeconds);

            alDistanceModel(AL_NONE);
            alListener3f(AL_POSITION, 0.0f, 0.0f, 0.0f);
            alListener3f(AL_VELOCITY, 0.0f, 0.0f, 0.0f);
            alListenerfv(AL_ORIENTATION, new float[]{0.0f, 0.0f, -1.0f, 0.0f, 1.0f, 0.0f});
            checkAl("listener setup");

            ShortBuffer pcm = generateMonoTone(SAMPLE_RATE, TONE_HZ);

            buffer = alGenBuffers();
            liveBuffers++;
            checkAl("alGenBuffers");
            alBufferData(buffer, AL_FORMAT_MONO16, pcm, SAMPLE_RATE);
            checkAl("alBufferData");

            source = alGenSources();
            liveSources++;
            checkAl("alGenSources");
            alSourcei(source, AL_BUFFER, buffer);
            alSourcei(source, AL_LOOPING, AL_TRUE);
            alSourcef(source, AL_GAIN, 0.20f);
            alSource3f(source, AL_POSITION, LEFT_X, 0.0f, -1.0f);
            checkAl("source setup");

            alSourcePlay(source);
            checkAl("alSourcePlay");

            System.out.println("Listen for the tone moving continuously left <-> right.");

            long startNanos = System.nanoTime();
            long durationNanos = durationSeconds * 1_000_000_000L;

            while (true) {
                long elapsedNanos = System.nanoTime() - startNanos;
                if (elapsedNanos >= durationNanos) {
                    break;
                }

                double elapsedSeconds = elapsedNanos / 1_000_000_000.0;
                double normalized = elapsedSeconds / durationSeconds;
                float x = (float) (Math.sin(normalized * Math.PI * 4.0) * RIGHT_X);
                alSource3f(source, AL_POSITION, x, 0.0f, -1.0f);
                checkAl("source movement");

                Thread.sleep(20L);
            }

            alSourceStop(source);
            checkAl("alSourceStop");
        } finally {
            if (source != 0) {
                alDeleteSources(source);
                liveSources--;
                source = 0;
            }

            if (buffer != 0) {
                alDeleteBuffers(buffer);
                liveBuffers--;
                buffer = 0;
            }

            if (context != NULL) {
                alcMakeContextCurrent(NULL);
                alcDestroyContext(context);
                context = NULL;
            }

            if (device != NULL) {
                if (!alcCloseDevice(device)) {
                    throw new IllegalStateException("Failed to close OpenAL device");
                }
                device = NULL;
            }

            if (liveSources != 0 || liveBuffers != 0) {
                throw new IllegalStateException("OpenAL cleanup counters are not zero: sources=" + liveSources + ", buffers=" + liveBuffers);
            }
        }

        System.out.println("P0-T05 passed: moving mono source completed and source/buffer counts returned to zero.");

    }

    private static ShortBuffer generateMonoTone(int sampleRate, float frequency) {

        int sampleCount = sampleRate;
        ShortBuffer samples = BufferUtils.createShortBuffer(sampleCount);

        double angularStep = 2.0 * Math.PI * frequency / sampleRate;
        for (int i = 0; i < sampleCount; i++) {
            double sample = Math.sin(i * angularStep);
            samples.put((short) (sample * Short.MAX_VALUE * 0.25));
        }
        samples.flip();
        return samples;

    }

    private static void checkAl(String operation) {

        int error = alGetError();
        if (error != AL_NO_ERROR) {
            throw new IllegalStateException(operation + " failed with OpenAL error 0x" + Integer.toHexString(error));
        }

    }
}
