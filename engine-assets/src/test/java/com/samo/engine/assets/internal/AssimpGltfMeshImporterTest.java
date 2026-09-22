package com.samo.engine.assets.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AssimpGltfMeshImporterTest {
    @TempDir
    Path tempDir;

    @Test
    void importsReferenceTriangleWithoutSpatialOrUvConversion() {

        List<ImportedMesh> meshes = AssimpGltfMeshImporter.importFile(resourcePath("p6/reference-triangle.gltf"));

        assertThat(meshes).hasSize(1);
        assertReferenceTriangle(meshes.getFirst());

        assertThat(meshes.getFirst().positions()).containsExactly(1.0f, 2.0f, 3.0f, -4.0f, 5.0f, 6.0f, 7.0f, -8.0f, 9.0f);

    }


    @Test
    void importsBinaryGlbReferenceWithoutConversion() throws Exception {

        Path source = tempDir.resolve("reference.glb");
        writeReferenceGlb(source);

        List<ImportedMesh> meshes = AssimpGltfMeshImporter.importFile(source);

        assertThat(meshes).hasSize(1);
        assertReferenceTriangle(meshes.getFirst());

    }

    @Test
    void leavesMissingOptionalAttributesAbsent() {

        ImportedMesh mesh = AssimpGltfMeshImporter.importFile(resourcePath("p6/positions-only-triangle.gltf")).getFirst();

        assertThat(mesh.name()).isEqualTo("PositionsOnly");
        assertThat(mesh.positions()).containsExactly(1.0f, 2.0f, 3.0f, -4.0f, 5.0f, 6.0f, 7.0f, -8.0f, 9.0f);
        assertThat(mesh.normals()).isNull();
        assertThat(mesh.tangents()).isNull();
        assertThat(mesh.uv0()).isNull();
        assertThat(mesh.indices()).containsExactly(2, 0, 1);

    }

    @Test
    void rejectsUnsupportedMeshSourceExtensionBeforeNativeImport() {

        Path source = tempDir.resolve("mesh.obj");

        assertThatThrownBy(() -> AssimpGltfMeshImporter.importFile(source)).isInstanceOf(AssetCookerException.class).hasMessageContaining(source.toString())
            .hasMessageContaining(".gltf or .glb");

    }

    @Test
    void reportsMalformedGltfWithSourcePath() throws Exception {

        Path source = tempDir.resolve("malformed.gltf");
        Files.writeString(source, "{", StandardCharsets.UTF_8);

        assertThatThrownBy(() -> AssimpGltfMeshImporter.importFile(source)).isInstanceOf(AssetCookerException.class).hasMessageContaining(source.toString())
            .hasMessageContaining("Assimp failed");

    }

    @Test
    void rejectsGltfWithoutMeshes() throws Exception {

        Path source = tempDir.resolve("empty.gltf");
        Files.writeString(source, """
            {
              "asset": {"version": "2.0"},
              "scenes": [{"nodes": []}],
              "scene": 0
            }
            """, StandardCharsets.UTF_8);

        assertThatThrownBy(() -> AssimpGltfMeshImporter.importFile(source)).isInstanceOf(AssetCookerException.class).hasMessageContaining(source.toString());

    }

    @Test
    void rejectsNonTriangleTopologyWithoutTriangulation() {

        Path source = resourcePath("p6/non-triangle-line.gltf");

        assertThatThrownBy(() -> AssimpGltfMeshImporter.importFile(source)).isInstanceOf(AssetCookerException.class).hasMessageContaining(source.toString())
            .hasMessageContaining("exactly three indices");

    }


    private static void assertReferenceTriangle(ImportedMesh mesh) {

        assertThat(mesh.meshIndex()).isZero();
        assertThat(mesh.name()).isEqualTo("ReferenceTriangle");
        assertThat(mesh.positions()).containsExactly(1.0f, 2.0f, 3.0f, -4.0f, 5.0f, 6.0f, 7.0f, -8.0f, 9.0f);
        assertThat(mesh.normals()).containsExactly(0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f);
        assertThat(mesh.tangents()).containsExactly(1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f);
        assertThat(mesh.uv0()).containsExactly(0.25f, 0.75f, 0.50f, 0.125f, 1.0f, 0.0f);
        assertThat(mesh.indices()).containsExactly(2, 0, 1);

    }

    private static void writeReferenceGlb(Path path) throws Exception {

        String binaryBase64 = "AACAPwAAAEAAAEBAAACAwAAAoEAAAMBAAADgQAAAAMEAABBBAAAAAAAAAAAAAIA/"
                + "AAAAAAAAAAAAAIA/AAAAAAAAAAAAAIA/AACAPwAAAAAAAAAAAACAPwAAgD8AAAAAAAAA"
                + "AAAAgD8AAIA/AAAAAAAAAAAAAIA/AACAPgAAQD8AAAA/AAAAPgAAgD8AAAAAAgAAAAEA"
                + "AAA=";
        byte[] binary = Base64.getDecoder().decode(binaryBase64);
        byte[] json = """
            {
              "asset": {"version": "2.0"},
              "buffers": [{"byteLength": 152}],
              "bufferViews": [
                {"buffer": 0, "byteOffset": 0, "byteLength": 36},
                {"buffer": 0, "byteOffset": 36, "byteLength": 36},
                {"buffer": 0, "byteOffset": 72, "byteLength": 48},
                {"buffer": 0, "byteOffset": 120, "byteLength": 24},
                {"buffer": 0, "byteOffset": 144, "byteLength": 6}
              ],
              "accessors": [
                {"bufferView": 0, "componentType": 5126, "count": 3, "type": "VEC3", "min": [-4, -8, 3], "max": [7, 5, 9]},
                {"bufferView": 1, "componentType": 5126, "count": 3, "type": "VEC3"},
                {"bufferView": 2, "componentType": 5126, "count": 3, "type": "VEC4"},
                {"bufferView": 3, "componentType": 5126, "count": 3, "type": "VEC2"},
                {"bufferView": 4, "componentType": 5123, "count": 3, "type": "SCALAR"}
              ],
              "meshes": [{
                "name": "ReferenceTriangle",
                "primitives": [{
                  "attributes": {"POSITION": 0, "NORMAL": 1, "TANGENT": 2, "TEXCOORD_0": 3},
                  "indices": 4,
                  "mode": 4
                }]
              }],
              "nodes": [{"mesh": 0}],
              "scenes": [{"nodes": [0]}],
              "scene": 0
            }
            """.strip().getBytes(StandardCharsets.UTF_8);

        int paddedJsonLength = (json.length + 3) & ~3;
        int paddedBinaryLength = (binary.length + 3) & ~3;
        int totalLength = 12 + 8 + paddedJsonLength + 8 + paddedBinaryLength;
        ByteBuffer glb = ByteBuffer.allocate(totalLength).order(ByteOrder.LITTLE_ENDIAN);
        glb.putInt(0x46546C67);
        glb.putInt(2);
        glb.putInt(totalLength);
        glb.putInt(paddedJsonLength);
        glb.putInt(0x4E4F534A);
        glb.put(json);
        while (glb.position() < 20 + paddedJsonLength) {
            glb.put((byte) 0x20);
        }
        glb.putInt(paddedBinaryLength);
        glb.putInt(0x004E4942);
        glb.put(binary);
        while (glb.position() < totalLength) {
            glb.put((byte) 0);
        }
        Files.write(path, glb.array());

    }

    private static Path resourcePath(String name) {

        URL resource = Objects.requireNonNull(AssimpGltfMeshImporterTest.class.getClassLoader().getResource(name), "Missing test resource " + name);
        try {
            return Path.of(resource.toURI());
        } catch (URISyntaxException exception) {
            throw new IllegalStateException("Invalid test resource URI for " + name, exception);
        }

    }
}
