package com.samo.engine.assets.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class MeshAssetTest {
    @Test
    void snapshotsPrimitiveListAndArrayValuesDefensively() {

        float[] positions = {-1.0f, 0.0f, -2.0f, 1.0f, 0.0f, -2.0f, 0.0f, 1.0f, -2.0f};
        int[] indices = {0, 1, 2};
        MeshAsset.Primitive primitive = new MeshAsset.Primitive(0, "Triangle", positions, null, null, null, null, indices);
        ArrayList<MeshAsset.Primitive> source = new ArrayList<>(List.of(primitive));
        MeshAsset asset = new MeshAsset(source);

        positions[0] = 99.0f;
        indices[0] = 2;
        source.clear();

        assertThat(asset.primitives()).containsExactly(primitive);
        assertThat(primitive.positions()).containsExactly(-1.0f, 0.0f, -2.0f, 1.0f, 0.0f, -2.0f, 0.0f, 1.0f, -2.0f);
        assertThat(primitive.indices()).containsExactly(0, 1, 2);

        float[] returnedPositions = primitive.positions();
        int[] returnedIndices = primitive.indices();
        returnedPositions[0] = 42.0f;
        returnedIndices[0] = 2;

        assertThat(primitive.positions()[0]).isEqualTo(-1.0f);
        assertThat(primitive.indices()[0]).isZero();

    }

    @Test
    void publicRuntimeMeshSurfaceContainsNoNativeBackendTypes() {

        assertThat(MeshAsset.class.getRecordComponents())
            .allSatisfy(component -> assertThat(component.getType().getName()).doesNotStartWith("org.lwjgl").doesNotContain("opengl").doesNotContain("openal"));
        assertThat(MeshAsset.Primitive.class.getDeclaredMethods()).allSatisfy(method -> {
            assertThat(method.getReturnType()).isNotEqualTo(ByteBuffer.class);
            assertThat(method.getReturnType().getName()).doesNotStartWith("org.lwjgl").doesNotContain("opengl").doesNotContain("openal");
        });

    }
}
