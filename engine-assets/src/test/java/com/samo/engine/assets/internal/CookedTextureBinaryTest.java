package com.samo.engine.assets.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.List;
import java.util.zip.CRC32C;
import org.junit.jupiter.api.Test;

class CookedTextureBinaryTest {
    @Test
    void encodesLittleEndianHeaderAndRoundTripsCompleteMipChainDeterministically() {

        List<TextureMipLevel> levels = List.of(
            new TextureMipLevel(2, 2, new byte[]{
                1, 2, 3, 4, 5, 6, 7, 8,
                9, 10, 11, 12, 13, 14, 15, 16
            }),
            new TextureMipLevel(1, 1, new byte[]{7, 8, 9, 10}));

        byte[] first = CookedTextureBinary.encode(levels);
        byte[] second = CookedTextureBinary.encode(levels);
        assertThat(second).isEqualTo(first);

        ByteBuffer header = ByteBuffer.wrap(first).order(ByteOrder.LITTLE_ENDIAN);
        assertThat(new byte[]{header.get(), header.get(), header.get(), header.get()}).containsExactly((byte) 'S', (byte) 'T', (byte) 'E', (byte) 'X');
        assertThat(header.getInt()).isEqualTo(1);
        assertThat(header.getInt()).isEqualTo(1);
        assertThat(header.getInt()).isEqualTo(2);
        assertThat(header.getInt()).isEqualTo(2);
        assertThat(header.getInt()).isEqualTo(2);
        assertThat(header.getInt()).isEqualTo(first.length - 32);

        CookedTexture decoded = CookedTextureBinary.decode(first);
        assertThat(decoded.width()).isEqualTo(2);
        assertThat(decoded.height()).isEqualTo(2);
        assertThat(decoded.mipLevels()).hasSize(2);
        assertThat(decoded.mipLevels().get(0).rgba8()).containsExactly(levels.get(0).rgba8());
        assertThat(decoded.mipLevels().get(1).rgba8()).containsExactly(levels.get(1).rgba8());

    }

    @Test
    void rejectsChecksumAndStructuralCorruptionBeforeReturningTexture() {

        byte[] valid = CookedTextureBinary.encode(List.of(
            new TextureMipLevel(2, 2, new byte[16]),
            new TextureMipLevel(1, 1, new byte[4])));

        byte[] checksumCorrupt = valid.clone();
        checksumCorrupt[checksumCorrupt.length - 1] ^= 1;
        assertThatThrownBy(() -> CookedTextureBinary.decode(checksumCorrupt))
            .isInstanceOf(AssetCookerException.class)
            .hasMessageContaining("checksum");

        byte[] badMagic = valid.clone();
        badMagic[0] = 'X';
        assertThatThrownBy(() -> CookedTextureBinary.decode(badMagic)).hasMessageContaining("magic");

        byte[] badVersion = valid.clone();
        ByteBuffer.wrap(badVersion).order(ByteOrder.LITTLE_ENDIAN).putInt(4, 2);
        assertThatThrownBy(() -> CookedTextureBinary.decode(badVersion)).hasMessageContaining("version");

        byte[] badFormat = valid.clone();
        ByteBuffer.wrap(badFormat).order(ByteOrder.LITTLE_ENDIAN).putInt(8, 9);
        assertThatThrownBy(() -> CookedTextureBinary.decode(badFormat)).hasMessageContaining("format");

        byte[] semanticCorrupt = valid.clone();
        ByteBuffer semantic = ByteBuffer.wrap(semanticCorrupt).order(ByteOrder.LITTLE_ENDIAN);
        semantic.putInt(32, 3);
        rewriteChecksum(semanticCorrupt);
        assertThatThrownBy(() -> CookedTextureBinary.decode(semanticCorrupt)).hasMessageContaining("dimensions");

        assertThatThrownBy(() -> CookedTextureBinary.decode(Arrays.copyOf(valid, valid.length - 1)))
            .hasMessageContaining("body length");

        byte[] trailing = Arrays.copyOf(valid, valid.length + 1);
        assertThatThrownBy(() -> CookedTextureBinary.decode(trailing)).hasMessageContaining("body length");

    }

    private static void rewriteChecksum(byte[] bytes) {

        ByteBuffer file = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        int bodyLength = file.getInt(24);
        CRC32C crc = new CRC32C();
        crc.update(bytes, 32, bodyLength);
        file.putInt(28, (int) crc.getValue());

    }
}
