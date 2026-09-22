package com.samo.engine.assets.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Test;

class MeshCoordinateConverterTest {
    @Test
    void convertsReferenceTriangleIntoCanonicalEngineBasisExactlyOnce() {

        ImportedMesh imported = AssimpGltfMeshImporter.importFile(resourcePath("p6/reference-triangle.gltf")).getFirst();

        EngineMesh engine = MeshCoordinateConverter.toEngineSpace(imported);

        assertThat(engine.meshIndex()).isZero();
        assertThat(engine.name()).isEqualTo("ReferenceTriangle");
        assertThat(engine.positions()).containsExactly(-7.0f, -8.0f, -9.0f, -1.0f, 2.0f, -3.0f, 4.0f, 5.0f, -6.0f);
        assertThat(engine.normals()).containsExactly(0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f);
        assertThat(engine.tangents()).containsExactly(-1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f);
        assertThat(engine.tangentSigns()).containsExactly(1.0f, 1.0f, 1.0f);
        assertThat(engine.uv0()).containsExactly(1.0f, 1.0f, 0.25f, 0.25f, 0.50f, 0.875f);
        assertThat(engine.indices()).containsExactly(0, 1, 2);

    }

    @Test
    void preservesOneMeterCubeDimensionsInEngineSpace() {

        List<ImportedMesh> imported = AssimpGltfMeshImporter.importFile(resourcePath("p6/one-meter-cube.gltf"));

        assertThat(imported).hasSize(1);
        EngineMesh engine = MeshCoordinateConverter.toEngineSpace(imported.getFirst());
        float[] positions = engine.positions();

        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float minZ = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        float maxZ = Float.NEGATIVE_INFINITY;
        for (int index = 0; index < positions.length; index += 3) {
            minX = Math.min(minX, positions[index]);
            minY = Math.min(minY, positions[index + 1]);
            minZ = Math.min(minZ, positions[index + 2]);
            maxX = Math.max(maxX, positions[index]);
            maxY = Math.max(maxY, positions[index + 1]);
            maxZ = Math.max(maxZ, positions[index + 2]);
        }

        assertThat(maxX - minX).isEqualTo(1.0f);
        assertThat(maxY - minY).isEqualTo(1.0f);
        assertThat(maxZ - minZ).isEqualTo(1.0f);

    }

    @Test
    void keepsMissingOptionalAttributesAbsent() {

        ImportedMesh imported = AssimpGltfMeshImporter.importFile(resourcePath("p6/positions-only-triangle.gltf")).getFirst();

        EngineMesh engine = MeshCoordinateConverter.toEngineSpace(imported);

        assertThat(engine.normals()).isNull();
        assertThat(engine.tangents()).isNull();
        assertThat(engine.tangentSigns()).isNull();
        assertThat(engine.uv0()).isNull();
        assertThat(engine.indices()).containsExactly(imported.indices());

    }

    @Test
    void rejectsMalformedInternalAttributeLengths() {

        ImportedMesh badPositions = new ImportedMesh(4, "BrokenPositions", new float[]{1.0f, 2.0f}, null, null, null, null, new int[]{0});
        assertThatThrownBy(() -> MeshCoordinateConverter.toEngineSpace(badPositions)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("mesh[4] 'BrokenPositions'")
            .hasMessageContaining("positions length");

        ImportedMesh badNormals = new ImportedMesh(5, "BrokenNormals", new float[]{1.0f, 2.0f, 3.0f}, new float[]{0.0f, 1.0f}, null, null, null, new int[]{0});
        assertThatThrownBy(() -> MeshCoordinateConverter.toEngineSpace(badNormals)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("normals length");

        ImportedMesh badTangents = new ImportedMesh(6, "BrokenTangents", new float[]{1.0f, 2.0f, 3.0f}, null, new float[]{1.0f, 0.0f}, new float[]{1.0f}, null, new int[]{0});
        assertThatThrownBy(() -> MeshCoordinateConverter.toEngineSpace(badTangents)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("tangents length");

        ImportedMesh badUv = new ImportedMesh(7, "BrokenUv", new float[]{1.0f, 2.0f, 3.0f}, null, null, null, new float[]{0.5f}, new int[]{0});
        assertThatThrownBy(() -> MeshCoordinateConverter.toEngineSpace(badUv)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("UV0 length");

    }

    @Test
    void rejectsNonFiniteSpatialDataWithMeshContext() {

        ImportedMesh nonFinitePosition = new ImportedMesh(8, "NonFinite", new float[]{Float.NaN, 0.0f, 0.0f}, null, null, null, null, new int[]{0});
        assertThatThrownBy(() -> MeshCoordinateConverter.toEngineSpace(nonFinitePosition)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("mesh[8] 'NonFinite'")
            .hasMessageContaining("non-finite");

        ImportedMesh nonFiniteNormal = new ImportedMesh(9, "BadNormal", new float[]{0.0f, 0.0f, 0.0f}, new float[]{Float.POSITIVE_INFINITY, 0.0f, 0.0f}, null, null, null, new int[]{0});
        assertThatThrownBy(() -> MeshCoordinateConverter.toEngineSpace(nonFiniteNormal)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("mesh[9] 'BadNormal'")
            .hasMessageContaining("non-finite");

        float[] nonFiniteTangentValues = new float[]{0.0f, Float.NEGATIVE_INFINITY, 0.0f};
        ImportedMesh nonFiniteTangent = new ImportedMesh(10, "BadTangent", new float[]{0.0f, 0.0f, 0.0f}, null, nonFiniteTangentValues, new float[]{1.0f}, null, new int[]{0});
        assertThatThrownBy(() -> MeshCoordinateConverter.toEngineSpace(nonFiniteTangent)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("mesh[10] 'BadTangent'")
            .hasMessageContaining("non-finite");

    }

    @Test
    void rejectsInvalidTangentSignStream() {

        ImportedMesh wrongLength = new ImportedMesh(11, "WrongSigns", new float[]{0.0f, 0.0f, 0.0f}, null, new float[]{1.0f, 0.0f, 0.0f}, new float[]{1.0f, -1.0f}, null, new int[]{0});
        assertThatThrownBy(() -> MeshCoordinateConverter.toEngineSpace(wrongLength)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("tangentSigns length");

        ImportedMesh invalidSign = new ImportedMesh(12, "InvalidSign", new float[]{0.0f, 0.0f, 0.0f}, null, new float[]{1.0f, 0.0f, 0.0f}, new float[]{0.0f}, null, new int[]{0});
        assertThatThrownBy(() -> MeshCoordinateConverter.toEngineSpace(invalidSign)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("exactly +1 or -1");

    }

    @Test
    void rejectsNullImportedMeshAsProgrammerError() {

        assertThatThrownBy(() -> MeshCoordinateConverter.toEngineSpace(null)).isInstanceOf(NullPointerException.class).hasMessageContaining("importedMesh");

    }

    private static Path resourcePath(String name) {

        URL resource = Objects.requireNonNull(MeshCoordinateConverterTest.class.getClassLoader().getResource(name), "Missing test resource " + name);
        try {
            return Path.of(resource.toURI());
        } catch (URISyntaxException exception) {
            throw new IllegalStateException("Invalid test resource URI for " + name, exception);
        }

    }
}
