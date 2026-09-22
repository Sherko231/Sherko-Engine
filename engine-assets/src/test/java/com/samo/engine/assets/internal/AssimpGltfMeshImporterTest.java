package com.samo.engine.assets.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
        ImportedMesh mesh = meshes.getFirst();
        assertThat(mesh.meshIndex()).isZero();
        assertThat(mesh.name()).isEqualTo("ReferenceTriangle");
        assertThat(mesh.positions()).containsExactly(1.0f, 2.0f, 3.0f, -4.0f, 5.0f, 6.0f, 7.0f, -8.0f, 9.0f);
        assertThat(mesh.normals()).containsExactly(0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f);
        assertThat(mesh.tangents()).containsExactly(1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f);
        assertThat(mesh.uv0()).containsExactly(0.25f, 0.75f, 0.50f, 0.125f, 1.0f, 0.0f);
        assertThat(mesh.indices()).containsExactly(2, 0, 1);

        assertThat(mesh.positions()).containsExactly(1.0f, 2.0f, 3.0f, -4.0f, 5.0f, 6.0f, 7.0f, -8.0f, 9.0f);

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

    private static Path resourcePath(String name) {

        URL resource = Objects.requireNonNull(AssimpGltfMeshImporterTest.class.getClassLoader().getResource(name), "Missing test resource " + name);
        try {
            return Path.of(resource.toURI());
        } catch (URISyntaxException exception) {
            throw new IllegalStateException("Invalid test resource URI for " + name, exception);
        }

    }
}
