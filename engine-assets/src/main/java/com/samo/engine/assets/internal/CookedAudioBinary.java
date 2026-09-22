package com.samo.engine.assets.internal;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.zip.CRC32C;

final class CookedAudioBinary {
    private static final byte[] MAGIC = {'S', 'A', 'U', 'D'};
    private static final int SCHEMA_VERSION = 1;
    private static final int CODEC_OGG_VORBIS = 1;
    private static final int HEADER_BYTES = 28;

    private CookedAudioBinary() {

    }

    static byte[] encode(CookedAudio audio) {

        if (audio == null) {
            throw new NullPointerException("audio");
        }

        byte[] payload = audio.vorbisPayload();
        CRC32C checksum = new CRC32C();
        checksum.update(payload, 0, payload.length);

        ByteBuffer file = ByteBuffer.allocate(Math.addExact(HEADER_BYTES, payload.length)).order(ByteOrder.LITTLE_ENDIAN);
        file.put(MAGIC);
        file.putInt(SCHEMA_VERSION);
        file.putInt(CODEC_OGG_VORBIS);
        file.putInt(audio.channels());
        file.putInt(audio.sampleRate());
        file.putInt(payload.length);
        file.putInt((int) checksum.getValue());
        file.put(payload);
        return file.array();

    }

    static CookedAudio decode(byte[] bytes) {

        if (bytes == null) {
            throw new NullPointerException("bytes");
        }
        if (bytes.length < HEADER_BYTES) {
            throw invalid("truncated header");
        }

        ByteBuffer file = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        for (byte expected : MAGIC) {
            if (file.get() != expected) {
                throw invalid("bad magic");
            }
        }

        int version = file.getInt();
        if (version != SCHEMA_VERSION) {
            throw invalid("unsupported schema version " + version);
        }
        int codec = file.getInt();
        if (codec != CODEC_OGG_VORBIS) {
            throw invalid("unsupported codec " + codec);
        }

        int channels = file.getInt();
        if (channels != 1 && channels != 2) {
            throw invalid("channel count must be mono or stereo");
        }
        int sampleRate = file.getInt();
        if (sampleRate <= 0) {
            throw invalid("sample rate must be positive");
        }
        int payloadLength = file.getInt();
        int declaredChecksum = file.getInt();
        if (payloadLength <= 0 || payloadLength != bytes.length - HEADER_BYTES) {
            throw invalid("payload length mismatch or trailing data");
        }

        byte[] payload = new byte[payloadLength];
        file.get(payload);
        CRC32C checksum = new CRC32C();
        checksum.update(payload, 0, payload.length);
        if ((int) checksum.getValue() != declaredChecksum) {
            throw invalid("checksum mismatch");
        }

        return new CookedAudio(channels, sampleRate, payload);

    }

    private static AssetCookerException invalid(String message) {

        return new AssetCookerException("Invalid SAUD payload: " + message);

    }
}
