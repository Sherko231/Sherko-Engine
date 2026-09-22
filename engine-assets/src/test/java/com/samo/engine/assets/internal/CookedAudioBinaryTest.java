package com.samo.engine.assets.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class CookedAudioBinaryTest {
    @Test
    void encodesLittleEndianHeaderPreservesPayloadAndRoundTripsDeterministically() {

        byte[] payload = {'O', 'g', 'g', 'S', 1, 2, 3, 4, 5};
        CookedAudio audio = new CookedAudio(2, 44100, payload);

        byte[] first = CookedAudioBinary.encode(audio);
        byte[] second = CookedAudioBinary.encode(audio);

        assertThat(second).isEqualTo(first);
        ByteBuffer header = ByteBuffer.wrap(first).order(ByteOrder.LITTLE_ENDIAN);
        assertThat(new byte[]{header.get(), header.get(), header.get(), header.get()}).containsExactly((byte) 'S', (byte) 'A', (byte) 'U', (byte) 'D');
        assertThat(header.getInt()).isEqualTo(1);
        assertThat(header.getInt()).isEqualTo(1);
        assertThat(header.getInt()).isEqualTo(2);
        assertThat(header.getInt()).isEqualTo(44100);
        assertThat(header.getInt()).isEqualTo(payload.length);
        header.getInt();
        byte[] storedPayload = new byte[payload.length];
        header.get(storedPayload);
        assertThat(storedPayload).isEqualTo(payload);

        CookedAudio decoded = CookedAudioBinary.decode(first);
        assertThat(decoded.channels()).isEqualTo(2);
        assertThat(decoded.sampleRate()).isEqualTo(44100);
        assertThat(decoded.vorbisPayload()).isEqualTo(payload);

    }

    @Test
    void rejectsInvalidChannelPolicyAtValueBoundary() {

        assertThatThrownBy(() -> new CookedAudio(3, 32000, new byte[]{1}))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("mono or stereo");

    }

    @Test
    void rejectsHeaderMetadataChecksumLengthTruncationAndTrailingCorruption() {

        byte[] valid = CookedAudioBinary.encode(new CookedAudio(1, 22050, new byte[]{1, 2, 3, 4, 5}));

        byte[] badMagic = valid.clone();
        badMagic[0] = 'X';
        assertThatThrownBy(() -> CookedAudioBinary.decode(badMagic)).hasMessageContaining("magic");

        byte[] badVersion = valid.clone();
        ByteBuffer.wrap(badVersion).order(ByteOrder.LITTLE_ENDIAN).putInt(4, 2);
        assertThatThrownBy(() -> CookedAudioBinary.decode(badVersion)).hasMessageContaining("version");

        byte[] badCodec = valid.clone();
        ByteBuffer.wrap(badCodec).order(ByteOrder.LITTLE_ENDIAN).putInt(8, 9);
        assertThatThrownBy(() -> CookedAudioBinary.decode(badCodec)).hasMessageContaining("codec");

        byte[] badChannels = valid.clone();
        ByteBuffer.wrap(badChannels).order(ByteOrder.LITTLE_ENDIAN).putInt(12, 3);
        assertThatThrownBy(() -> CookedAudioBinary.decode(badChannels)).hasMessageContaining("channel");

        byte[] badSampleRate = valid.clone();
        ByteBuffer.wrap(badSampleRate).order(ByteOrder.LITTLE_ENDIAN).putInt(16, 0);
        assertThatThrownBy(() -> CookedAudioBinary.decode(badSampleRate)).hasMessageContaining("sample rate");

        byte[] badLength = valid.clone();
        ByteBuffer.wrap(badLength).order(ByteOrder.LITTLE_ENDIAN).putInt(20, 99);
        assertThatThrownBy(() -> CookedAudioBinary.decode(badLength)).hasMessageContaining("payload length");

        byte[] checksumCorrupt = valid.clone();
        checksumCorrupt[checksumCorrupt.length - 1] ^= 1;
        assertThatThrownBy(() -> CookedAudioBinary.decode(checksumCorrupt)).hasMessageContaining("checksum");

        assertThatThrownBy(() -> CookedAudioBinary.decode(Arrays.copyOf(valid, valid.length - 1))).hasMessageContaining("payload length");

        byte[] trailing = Arrays.copyOf(valid, valid.length + 1);
        assertThatThrownBy(() -> CookedAudioBinary.decode(trailing)).hasMessageContaining("payload length");

    }
}
