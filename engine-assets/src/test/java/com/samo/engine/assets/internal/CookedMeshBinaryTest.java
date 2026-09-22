package com.samo.engine.assets.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.CRC32C;
import org.junit.jupiter.api.Test;

class CookedMeshBinaryTest {
    @Test
    void encodesExactSchemaV1HeaderRecordLayoutAndRoundTrips() {

        EngineMesh mesh = fullMesh(4, "Mésh✓");
        byte[] encoded = CookedMeshBinary.encode(List.of(mesh));

        assertThat(encoded).hasSize(232);
        assertThat(new byte[]{encoded[0], encoded[1], encoded[2], encoded[3]}).containsExactly('S', 'M', 'E', 'S');

        ByteBuffer bytes = ByteBuffer.wrap(encoded).order(ByteOrder.LITTLE_ENDIAN);
        bytes.position(4);
        assertThat(bytes.getInt()).isEqualTo(1);
        assertThat(bytes.getInt()).isEqualTo(1);
        assertThat(bytes.getInt()).isEqualTo(212);
        long storedCrc = Integer.toUnsignedLong(bytes.getInt());

        CRC32C crc = new CRC32C();
        crc.update(encoded, CookedMeshBinary.FILE_HEADER_BYTES, 212);
        assertThat(storedCrc).isEqualTo(crc.getValue());

        assertThat(bytes.getInt()).isEqualTo(4);
        assertThat(bytes.getInt()).isEqualTo("Mésh✓".getBytes(StandardCharsets.UTF_8).length);
        assertThat(bytes.getInt()).isEqualTo(3);
        assertThat(bytes.getInt()).isEqualTo(3);
        assertThat(bytes.getInt()).isEqualTo(CookedMeshBinary.FLAG_NORMALS | CookedMeshBinary.FLAG_TANGENTS | CookedMeshBinary.FLAG_UV0);
        assertThat(bytes.getInt()).isEqualTo(48);
        assertThat(bytes.getFloat()).isEqualTo(-4.0f);
        assertThat(bytes.getFloat()).isEqualTo(-8.0f);
        assertThat(bytes.getFloat()).isEqualTo(-6.0f);
        assertThat(bytes.getFloat()).isEqualTo(7.0f);
        assertThat(bytes.getFloat()).isEqualTo(5.0f);
        assertThat(bytes.getFloat()).isEqualTo(9.0f);

        List<EngineMesh> decoded = CookedMeshBinary.decode(encoded);
        assertThat(decoded).hasSize(1);
        assertMeshEquals(mesh, decoded.getFirst());

    }

    @Test
    void preservesOptionalAttributesListOrderAndDeterministicBytes() {

        EngineMesh first = positionsOnlyMesh(8, "");
        EngineMesh second = fullMesh(2, "Second");

        byte[] firstEncoding = CookedMeshBinary.encode(List.of(first, second));
        byte[] secondEncoding = CookedMeshBinary.encode(List.of(first, second));

        assertThat(secondEncoding).isEqualTo(firstEncoding);

        List<EngineMesh> decoded = CookedMeshBinary.decode(firstEncoding);
        assertThat(decoded).extracting(EngineMesh::meshIndex).containsExactly(8, 2);
        assertMeshEquals(first, decoded.get(0));
        assertMeshEquals(second, decoded.get(1));

    }

    @Test
    void rejectsBodyCorruptionBeforeRecordParsing() {

        byte[] encoded = CookedMeshBinary.encode(List.of(fullMesh(0, "Checksum")));
        encoded[encoded.length - 1] ^= 0x01;

        assertThatThrownBy(() -> CookedMeshBinary.decode(encoded)).isInstanceOf(CookedMeshFormatException.class).hasMessageContaining("CRC32C mismatch");

    }

    @Test
    void rejectsHeaderLengthAndVersionCorruption() {

        byte[] badMagic = CookedMeshBinary.encode(List.of(positionsOnlyMesh(0, "A")));
        badMagic[0] = 'X';
        assertThatThrownBy(() -> CookedMeshBinary.decode(badMagic)).isInstanceOf(CookedMeshFormatException.class).hasMessageContaining("magic");

        byte[] badVersion = CookedMeshBinary.encode(List.of(positionsOnlyMesh(0, "A")));
        ByteBuffer.wrap(badVersion).order(ByteOrder.LITTLE_ENDIAN).putInt(4, 2);
        assertThatThrownBy(() -> CookedMeshBinary.decode(badVersion)).isInstanceOf(CookedMeshFormatException.class).hasMessageContaining("schema version 2");

        byte[] valid = CookedMeshBinary.encode(List.of(positionsOnlyMesh(0, "A")));
        byte[] truncated = java.util.Arrays.copyOf(valid, valid.length - 1);
        assertThatThrownBy(() -> CookedMeshBinary.decode(truncated)).isInstanceOf(CookedMeshFormatException.class).hasMessageContaining("file length");

        byte[] trailing = java.util.Arrays.copyOf(valid, valid.length + 1);
        assertThatThrownBy(() -> CookedMeshBinary.decode(trailing)).isInstanceOf(CookedMeshFormatException.class).hasMessageContaining("file length");

    }

    @Test
    void rejectsSemanticCorruptionEvenWithValidChecksum() {

        byte[] unknownFlags = CookedMeshBinary.encode(List.of(fullMesh(0, "Flags")));
        ByteBuffer.wrap(unknownFlags).order(ByteOrder.LITTLE_ENDIAN).putInt(CookedMeshBinary.FILE_HEADER_BYTES + 16, 0x8);
        rewriteChecksum(unknownFlags);
        assertThatThrownBy(() -> CookedMeshBinary.decode(unknownFlags)).isInstanceOf(CookedMeshFormatException.class).hasMessageContaining("unknown bits");

        byte[] badBounds = CookedMeshBinary.encode(List.of(positionsOnlyMesh(0, "Bounds")));
        ByteBuffer.wrap(badBounds).order(ByteOrder.LITTLE_ENDIAN).putFloat(CookedMeshBinary.FILE_HEADER_BYTES + 24, -99.0f);
        rewriteChecksum(badBounds);
        assertThatThrownBy(() -> CookedMeshBinary.decode(badBounds)).isInstanceOf(CookedMeshFormatException.class).hasMessageContaining("bounds do not match");

        byte[] malformedName = CookedMeshBinary.encode(List.of(positionsOnlyMesh(0, "A")));
        malformedName[CookedMeshBinary.FILE_HEADER_BYTES + CookedMeshBinary.MESH_HEADER_BYTES] = (byte) 0xC3;
        rewriteChecksum(malformedName);
        assertThatThrownBy(() -> CookedMeshBinary.decode(malformedName)).isInstanceOf(CookedMeshFormatException.class).hasMessageContaining("valid UTF-8");

    }

    @Test
    void rejectsInvalidEncoderValuesAndDuplicateMeshIndices() {

        EngineMesh malformedNormals = new EngineMesh(0, "BadNormals", new float[]{0.0f, 0.0f, 0.0f}, new float[]{0.0f, 1.0f}, null, null, null, new int[]{0, 0, 0});
        assertThatThrownBy(() -> CookedMeshBinary.encode(List.of(malformedNormals))).isInstanceOf(CookedMeshFormatException.class).hasMessageContaining("normals length");

        EngineMesh invalidIndex = new EngineMesh(1, "BadIndex", new float[]{0.0f, 0.0f, 0.0f}, null, null, null, null, new int[]{0, 0, 2});
        assertThatThrownBy(() -> CookedMeshBinary.encode(List.of(invalidIndex))).isInstanceOf(CookedMeshFormatException.class).hasMessageContaining("outside vertex range");

        EngineMesh first = positionsOnlyMesh(5, "One");
        EngineMesh duplicate = positionsOnlyMesh(5, "Two");
        assertThatThrownBy(() -> CookedMeshBinary.encode(List.of(first, duplicate))).isInstanceOf(CookedMeshFormatException.class).hasMessageContaining("Duplicate meshIndex 5");

    }

    private static EngineMesh fullMesh(int meshIndex, String name) {

        return new EngineMesh(
            meshIndex,
            name,
            new float[]{1.0f, 2.0f, 3.0f, -4.0f, 5.0f, -6.0f, 7.0f, -8.0f, 9.0f},
            new float[]{0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f},
            new float[]{1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f},
            new float[]{1.0f, -1.0f, 1.0f},
            new float[]{0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f},
            new int[]{0, 1, 2});

    }

    private static EngineMesh positionsOnlyMesh(int meshIndex, String name) {

        return new EngineMesh(meshIndex, name, new float[]{0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f}, null, null, null, null, new int[]{0, 1, 2});

    }

    private static void rewriteChecksum(byte[] encoded) {

        ByteBuffer header = ByteBuffer.wrap(encoded).order(ByteOrder.LITTLE_ENDIAN);
        int bodyByteLength = header.getInt(12);
        CRC32C crc = new CRC32C();
        crc.update(encoded, CookedMeshBinary.FILE_HEADER_BYTES, bodyByteLength);
        header.putInt(16, (int) crc.getValue());

    }

    private static void assertMeshEquals(EngineMesh expected, EngineMesh actual) {

        assertThat(actual.meshIndex()).isEqualTo(expected.meshIndex());
        assertThat(actual.name()).isEqualTo(expected.name());
        assertThat(actual.positions()).containsExactly(expected.positions());
        assertThat(actual.normals()).containsExactly(expected.normals());
        assertThat(actual.tangents()).containsExactly(expected.tangents());
        assertThat(actual.tangentSigns()).containsExactly(expected.tangentSigns());
        assertThat(actual.uv0()).containsExactly(expected.uv0());
        assertThat(actual.indices()).containsExactly(expected.indices());

    }
}
