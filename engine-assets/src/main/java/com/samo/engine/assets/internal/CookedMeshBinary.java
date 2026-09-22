package com.samo.engine.assets.internal;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.zip.CRC32C;

final class CookedMeshBinary {
    static final int SCHEMA_VERSION = 1;
    static final int FILE_HEADER_BYTES = 20;
    static final int MESH_HEADER_BYTES = 48;
    static final int FLAG_NORMALS = 0x1;
    static final int FLAG_TANGENTS = 0x2;
    static final int FLAG_UV0 = 0x4;
    private static final int KNOWN_FLAGS = FLAG_NORMALS | FLAG_TANGENTS | FLAG_UV0;
    private static final byte[] MAGIC = new byte[]{'S', 'M', 'E', 'S'};

    private CookedMeshBinary() {

    }

    static byte[] encode(List<EngineMesh> meshes) {

        Objects.requireNonNull(meshes, "meshes");
        if (meshes.isEmpty()) {
            throw new CookedMeshFormatException("Cooked mesh payload must contain at least one mesh");
        }

        ArrayList<EncodedMesh> encodedMeshes = new ArrayList<>(meshes.size());
        Set<Integer> meshIndices = new HashSet<>();
        int bodyByteLength = 0;
        for (EngineMesh mesh : meshes) {
            EncodedMesh encoded = validateForEncode(Objects.requireNonNull(mesh, "mesh"));
            if (!meshIndices.add(encoded.meshIndex())) {
                throw new CookedMeshFormatException("Duplicate meshIndex " + encoded.meshIndex());
            }
            encodedMeshes.add(encoded);
            bodyByteLength = addExact(bodyByteLength, encoded.recordByteLength(), "Cooked mesh body is too large");
        }

        int fileByteLength = addExact(FILE_HEADER_BYTES, bodyByteLength, "Cooked mesh file is too large");
        ByteBuffer buffer = ByteBuffer.allocate(fileByteLength).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(MAGIC);
        buffer.putInt(SCHEMA_VERSION);
        buffer.putInt(encodedMeshes.size());
        buffer.putInt(bodyByteLength);
        buffer.putInt(0);

        for (EncodedMesh mesh : encodedMeshes) {
            writeMesh(buffer, mesh);
        }

        CRC32C checksum = new CRC32C();
        checksum.update(buffer.array(), FILE_HEADER_BYTES, bodyByteLength);
        buffer.putInt(16, (int) checksum.getValue());
        return buffer.array();

    }

    static List<EngineMesh> decode(byte[] bytes) {

        Objects.requireNonNull(bytes, "bytes");
        if (bytes.length < FILE_HEADER_BYTES) {
            throw new CookedMeshFormatException("Cooked mesh file is shorter than the 20-byte header");
        }

        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        requireMagic(buffer);

        int schemaVersion = buffer.getInt();
        if (schemaVersion != SCHEMA_VERSION) {
            throw new CookedMeshFormatException("Unsupported cooked mesh schema version " + schemaVersion);
        }

        int meshCount = buffer.getInt();
        int bodyByteLength = buffer.getInt();
        long expectedChecksum = Integer.toUnsignedLong(buffer.getInt());

        if (meshCount <= 0) {
            throw new CookedMeshFormatException("meshCount must be positive");
        }
        if (bodyByteLength <= 0) {
            throw new CookedMeshFormatException("bodyByteLength must be positive");
        }
        if (meshCount > bodyByteLength / MESH_HEADER_BYTES) {
            throw new CookedMeshFormatException("meshCount cannot fit in the declared body");
        }

        int expectedFileLength = addExact(FILE_HEADER_BYTES, bodyByteLength, "Declared cooked mesh body is too large");
        if (bytes.length != expectedFileLength) {
            throw new CookedMeshFormatException("Cooked mesh file length does not match declared bodyByteLength");
        }

        CRC32C checksum = new CRC32C();
        checksum.update(bytes, FILE_HEADER_BYTES, bodyByteLength);
        if (checksum.getValue() != expectedChecksum) {
            throw new CookedMeshFormatException("Cooked mesh body CRC32C mismatch");
        }

        ArrayList<EngineMesh> meshes = new ArrayList<>(meshCount);
        Set<Integer> meshIndices = new HashSet<>();
        for (int meshNumber = 0; meshNumber < meshCount; meshNumber++) {
            EngineMesh mesh = readMesh(buffer, meshNumber);
            if (!meshIndices.add(mesh.meshIndex())) {
                throw new CookedMeshFormatException("Duplicate meshIndex " + mesh.meshIndex());
            }
            meshes.add(mesh);
        }

        if (buffer.hasRemaining()) {
            throw new CookedMeshFormatException("Cooked mesh body contains trailing bytes");
        }
        return List.copyOf(meshes);

    }

    private static EncodedMesh validateForEncode(EngineMesh mesh) {

        int meshIndex = mesh.meshIndex();
        if (meshIndex < 0) {
            throw new CookedMeshFormatException("meshIndex must be non-negative");
        }

        byte[] nameBytes = encodeUtf8(mesh.name());
        float[] positions = mesh.positions();
        if (positions.length == 0 || positions.length % 3 != 0) {
            throw failure(meshIndex, "positions length must be a positive multiple of 3");
        }
        int vertexCount = positions.length / 3;
        requireFinite(positions, meshIndex, "positions");

        float[] normals = mesh.normals();
        if (normals != null) {
            requireLength(normals, positions.length, meshIndex, "normals");
            requireFinite(normals, meshIndex, "normals");
        }

        float[] tangents = mesh.tangents();
        float[] tangentSigns = mesh.tangentSigns();
        if ((tangents == null) != (tangentSigns == null)) {
            throw failure(meshIndex, "tangent xyz and tangent signs must both be present or absent");
        }
        if (tangents != null) {
            requireLength(tangents, positions.length, meshIndex, "tangents");
            requireFinite(tangents, meshIndex, "tangents");
            if (tangentSigns.length != vertexCount) {
                throw failure(meshIndex, "tangentSigns length must equal vertexCount");
            }
            requireSigns(tangentSigns, meshIndex);
        }

        float[] uv0 = mesh.uv0();
        if (uv0 != null) {
            requireLength(uv0, multiplyExact(vertexCount, 2, "UV0 length overflow"), meshIndex, "UV0");
            requireFinite(uv0, meshIndex, "UV0");
        }

        int[] indices = mesh.indices();
        validateIndices(indices, vertexCount, meshIndex);

        int flags = flags(normals, tangents, uv0);
        int strideBytes = strideBytes(flags);
        float[] bounds = bounds(positions);
        int vertexBytes = multiplyExact(vertexCount, strideBytes, "Vertex stream is too large");
        int indexBytes = multiplyExact(indices.length, Integer.BYTES, "Index stream is too large");
        int recordByteLength = addExact(MESH_HEADER_BYTES, nameBytes.length, "Mesh record is too large");
        recordByteLength = addExact(recordByteLength, vertexBytes, "Mesh record is too large");
        recordByteLength = addExact(recordByteLength, indexBytes, "Mesh record is too large");

        return new EncodedMesh(
            meshIndex,
            nameBytes,
            vertexCount,
            indices.length,
            flags,
            strideBytes,
            bounds,
            positions,
            normals,
            tangents,
            tangentSigns,
            uv0,
            indices,
            recordByteLength);

    }

    private static void writeMesh(ByteBuffer buffer, EncodedMesh mesh) {

        buffer.putInt(mesh.meshIndex());
        buffer.putInt(mesh.nameBytes().length);
        buffer.putInt(mesh.vertexCount());
        buffer.putInt(mesh.indexCount());
        buffer.putInt(mesh.flags());
        buffer.putInt(mesh.strideBytes());
        for (float bound : mesh.bounds()) {
            buffer.putFloat(bound);
        }
        buffer.put(mesh.nameBytes());

        for (int vertex = 0; vertex < mesh.vertexCount(); vertex++) {
            putVec3(buffer, mesh.positions(), vertex);
            if (mesh.normals() != null) {
                putVec3(buffer, mesh.normals(), vertex);
            }
            if (mesh.tangents() != null) {
                putVec3(buffer, mesh.tangents(), vertex);
                buffer.putFloat(mesh.tangentSigns()[vertex]);
            }
            if (mesh.uv0() != null) {
                int uvBase = vertex * 2;
                buffer.putFloat(mesh.uv0()[uvBase]);
                buffer.putFloat(mesh.uv0()[uvBase + 1]);
            }
        }

        for (int index : mesh.indices()) {
            buffer.putInt(index);
        }

    }

    private static EngineMesh readMesh(ByteBuffer buffer, int meshNumber) {

        requireRemaining(buffer, MESH_HEADER_BYTES, "mesh[" + meshNumber + "] header is truncated");

        int meshIndex = buffer.getInt();
        int nameByteLength = buffer.getInt();
        int vertexCount = buffer.getInt();
        int indexCount = buffer.getInt();
        int flags = buffer.getInt();
        int strideBytes = buffer.getInt();
        float[] storedBounds = new float[]{buffer.getFloat(), buffer.getFloat(), buffer.getFloat(), buffer.getFloat(), buffer.getFloat(), buffer.getFloat()};

        if (meshIndex < 0) {
            throw failure(meshIndex, "meshIndex must be non-negative");
        }
        if (nameByteLength < 0) {
            throw failure(meshIndex, "nameByteLength must be non-negative");
        }
        if (vertexCount <= 0) {
            throw failure(meshIndex, "vertexCount must be positive");
        }
        if (indexCount <= 0 || indexCount % 3 != 0) {
            throw failure(meshIndex, "indexCount must be positive and divisible by 3");
        }
        if ((flags & ~KNOWN_FLAGS) != 0) {
            throw failure(meshIndex, "attributeFlags contain unknown bits");
        }

        int expectedStride = strideBytes(flags);
        if (strideBytes != expectedStride) {
            throw failure(meshIndex, "vertexStrideBytes does not match attributeFlags");
        }
        validateBoundsHeader(storedBounds, meshIndex);

        int vertexBytes = multiplyExact(vertexCount, strideBytes, "Vertex stream is too large");
        int indexBytes = multiplyExact(indexCount, Integer.BYTES, "Index stream is too large");
        int requiredBytes = addExact(nameByteLength, vertexBytes, "Mesh record is too large");
        requiredBytes = addExact(requiredBytes, indexBytes, "Mesh record is too large");
        requireRemaining(buffer, requiredBytes, "mesh[" + meshIndex + "] record is truncated");

        byte[] nameBytes = new byte[nameByteLength];
        buffer.get(nameBytes);
        String name = decodeUtf8(nameBytes, meshIndex);

        float[] positions = new float[multiplyExact(vertexCount, 3, "Position stream is too large")];
        float[] normals = (flags & FLAG_NORMALS) != 0 ? new float[positions.length] : null;
        float[] tangents = (flags & FLAG_TANGENTS) != 0 ? new float[positions.length] : null;
        float[] tangentSigns = tangents != null ? new float[vertexCount] : null;
        float[] uv0 = (flags & FLAG_UV0) != 0 ? new float[multiplyExact(vertexCount, 2, "UV0 stream is too large")] : null;

        for (int vertex = 0; vertex < vertexCount; vertex++) {
            readVec3(buffer, positions, vertex);
            if (normals != null) {
                readVec3(buffer, normals, vertex);
            }
            if (tangents != null) {
                readVec3(buffer, tangents, vertex);
                tangentSigns[vertex] = buffer.getFloat();
            }
            if (uv0 != null) {
                int uvBase = vertex * 2;
                uv0[uvBase] = buffer.getFloat();
                uv0[uvBase + 1] = buffer.getFloat();
            }
        }

        requireFinite(positions, meshIndex, "positions");
        if (normals != null) {
            requireFinite(normals, meshIndex, "normals");
        }
        if (tangents != null) {
            requireFinite(tangents, meshIndex, "tangents");
            requireSigns(tangentSigns, meshIndex);
        }
        if (uv0 != null) {
            requireFinite(uv0, meshIndex, "UV0");
        }

        int[] indices = new int[indexCount];
        for (int index = 0; index < indexCount; index++) {
            indices[index] = buffer.getInt();
        }
        validateIndices(indices, vertexCount, meshIndex);

        float[] decodedBounds = bounds(positions);
        for (int index = 0; index < storedBounds.length; index++) {
            if (Float.floatToIntBits(storedBounds[index]) != Float.floatToIntBits(decodedBounds[index])) {
                throw failure(meshIndex, "stored bounds do not match decoded positions");
            }
        }

        return new EngineMesh(meshIndex, name, positions, normals, tangents, tangentSigns, uv0, indices);

    }

    private static void requireMagic(ByteBuffer buffer) {

        for (byte expected : MAGIC) {
            if (buffer.get() != expected) {
                throw new CookedMeshFormatException("Cooked mesh magic must be SMES");
            }
        }

    }

    private static byte[] encodeUtf8(String value) {

        Objects.requireNonNull(value, "name");
        try {
            CharsetEncoder encoder = StandardCharsets.UTF_8.newEncoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT);
            ByteBuffer encoded = encoder.encode(CharBuffer.wrap(value));
            byte[] bytes = new byte[encoded.remaining()];
            encoded.get(bytes);
            return bytes;
        } catch (CharacterCodingException exception) {
            throw new CookedMeshFormatException("Mesh name cannot be encoded as strict UTF-8", exception);
        }

    }

    private static String decodeUtf8(byte[] bytes, int meshIndex) {

        try {
            CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT);
            return decoder.decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException exception) {
            throw failure(meshIndex, "mesh name is not valid UTF-8", exception);
        }

    }

    private static void putVec3(ByteBuffer buffer, float[] values, int vertex) {

        int base = vertex * 3;
        buffer.putFloat(values[base]);
        buffer.putFloat(values[base + 1]);
        buffer.putFloat(values[base + 2]);

    }

    private static void readVec3(ByteBuffer buffer, float[] values, int vertex) {

        int base = vertex * 3;
        values[base] = buffer.getFloat();
        values[base + 1] = buffer.getFloat();
        values[base + 2] = buffer.getFloat();

    }

    private static int flags(float[] normals, float[] tangents, float[] uv0) {

        int flags = 0;
        if (normals != null) {
            flags |= FLAG_NORMALS;
        }
        if (tangents != null) {
            flags |= FLAG_TANGENTS;
        }
        if (uv0 != null) {
            flags |= FLAG_UV0;
        }
        return flags;

    }

    private static int strideBytes(int flags) {

        int floats = 3;
        if ((flags & FLAG_NORMALS) != 0) {
            floats += 3;
        }
        if ((flags & FLAG_TANGENTS) != 0) {
            floats += 4;
        }
        if ((flags & FLAG_UV0) != 0) {
            floats += 2;
        }
        return multiplyExact(floats, Float.BYTES, "vertexStrideBytes overflow");

    }

    private static float[] bounds(float[] positions) {

        float minX = positions[0];
        float minY = positions[1];
        float minZ = positions[2];
        float maxX = minX;
        float maxY = minY;
        float maxZ = minZ;
        for (int index = 3; index < positions.length; index += 3) {
            minX = Math.min(minX, positions[index]);
            minY = Math.min(minY, positions[index + 1]);
            minZ = Math.min(minZ, positions[index + 2]);
            maxX = Math.max(maxX, positions[index]);
            maxY = Math.max(maxY, positions[index + 1]);
            maxZ = Math.max(maxZ, positions[index + 2]);
        }
        return new float[]{minX, minY, minZ, maxX, maxY, maxZ};

    }

    private static void validateBoundsHeader(float[] bounds, int meshIndex) {

        requireFinite(bounds, meshIndex, "bounds");
        if (bounds[0] > bounds[3] || bounds[1] > bounds[4] || bounds[2] > bounds[5]) {
            throw failure(meshIndex, "stored bounds min must not exceed max");
        }

    }

    private static void validateIndices(int[] indices, int vertexCount, int meshIndex) {

        if (indices.length == 0 || indices.length % 3 != 0) {
            throw failure(meshIndex, "indices length must be positive and divisible by 3");
        }
        for (int index = 0; index < indices.length; index++) {
            int value = indices[index];
            if (value < 0 || value >= vertexCount) {
                throw failure(meshIndex, "index[" + index + "] " + value + " is outside vertex range 0.." + (vertexCount - 1));
            }
        }

    }

    private static void requireLength(float[] values, int expected, int meshIndex, String label) {

        if (values.length != expected) {
            throw failure(meshIndex, label + " length must equal " + expected);
        }

    }

    private static void requireFinite(float[] values, int meshIndex, String label) {

        for (int index = 0; index < values.length; index++) {
            if (!Float.isFinite(values[index])) {
                throw failure(meshIndex, label + "[" + index + "] must be finite");
            }
        }

    }

    private static void requireSigns(float[] signs, int meshIndex) {

        for (int index = 0; index < signs.length; index++) {
            if (signs[index] != 1.0f && signs[index] != -1.0f) {
                throw failure(meshIndex, "tangentSigns[" + index + "] must be exactly +1 or -1");
            }
        }

    }

    private static void requireRemaining(ByteBuffer buffer, int required, String message) {

        if (required < 0 || buffer.remaining() < required) {
            throw new CookedMeshFormatException(message);
        }

    }

    private static int addExact(int left, int right, String message) {

        try {
            return Math.addExact(left, right);
        } catch (ArithmeticException exception) {
            throw new CookedMeshFormatException(message, exception);
        }

    }

    private static int multiplyExact(int left, int right, String message) {

        try {
            return Math.multiplyExact(left, right);
        } catch (ArithmeticException exception) {
            throw new CookedMeshFormatException(message, exception);
        }

    }

    private static CookedMeshFormatException failure(int meshIndex, String message) {

        return new CookedMeshFormatException("mesh[" + meshIndex + "]: " + message);

    }

    private static CookedMeshFormatException failure(int meshIndex, String message, Throwable cause) {

        return new CookedMeshFormatException("mesh[" + meshIndex + "]: " + message, cause);

    }

    private record EncodedMesh(
        int meshIndex,
        byte[] nameBytes,
        int vertexCount,
        int indexCount,
        int flags,
        int strideBytes,
        float[] bounds,
        float[] positions,
        float[] normals,
        float[] tangents,
        float[] tangentSigns,
        float[] uv0,
        int[] indices,
        int recordByteLength) {
    }
}
