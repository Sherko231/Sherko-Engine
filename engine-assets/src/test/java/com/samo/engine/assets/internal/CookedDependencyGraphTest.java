package com.samo.engine.assets.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.samo.engine.assets.api.AssetId;
import com.samo.engine.assets.api.AssetType;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CookedDependencyGraphTest {
    private static final AssetId TEXTURE = new AssetId(0, 1);
    private static final AssetId MATERIAL = new AssetId(0, 2);
    private static final AssetId PREFAB = new AssetId(0, 3);
    private static final AssetId SCENE = new AssetId(0, 4);

    @Test
    void writesDeterministicMinimalSchemaAndDecodesAgainstKnownAssets() {

        Map<AssetId, AssetType> known = Map.of(TEXTURE, AssetType.TEXTURE, MATERIAL, AssetType.MATERIAL, PREFAB, AssetType.PREFAB);
        AssetDependencyGraph graph = AssetDependencyGraph.fromPersisted(List.of(new AssetDependencyGraph.Entry(PREFAB, AssetType.PREFAB, List.of(MATERIAL), List.of()),
            new AssetDependencyGraph.Entry(MATERIAL, AssetType.MATERIAL, List.of(TEXTURE), List.of("opaque-baseline"))), known);

        String first = CookedDependencyGraphJson.write(graph);
        String second = CookedDependencyGraphJson.write(graph);

        assertThat(second).isEqualTo(first);
        assertThat(first).contains("\"schemaVersion\" : 1").contains("\"assetId\"").contains("\"assetDependencies\"").contains("\"shaderDependencies\"")
            .doesNotContain("\"assetType\"");
        assertThat(first.indexOf(MATERIAL.toString())).isLessThan(first.indexOf(PREFAB.toString()));

        AssetDependencyGraph decoded = CookedDependencyGraphJson.decode(first, known);
        assertThat(decoded.dependentsOfAsset(TEXTURE)).containsExactly(MATERIAL, PREFAB);
        assertThat(decoded.dependentsOfShader("opaque-baseline")).containsExactly(MATERIAL, PREFAB);

    }

    @Test
    void rejectsMalformedVersionDuplicateOwnerMissingReferenceAndCycle() {

        Map<AssetId, AssetType> known = Map.of(TEXTURE, AssetType.TEXTURE, MATERIAL, AssetType.MATERIAL, PREFAB, AssetType.PREFAB, SCENE, AssetType.SCENE);

        assertRejected("{\"schemaVersion\":2,\"assets\":[]}", known, "upgrade required");
        assertRejected("""
            {"schemaVersion":1,"assets":[
              {"assetId":"%s","assetDependencies":[],"shaderDependencies":[]},
              {"assetId":"%s","assetDependencies":[],"shaderDependencies":[]}
            ]}
            """.formatted(MATERIAL, MATERIAL), known, "duplicate owner");
        assertRejected("""
            {"schemaVersion":1,"assets":[
              {"assetId":"%s","assetDependencies":["00000000-0000-0000-0000-000000000099"],"shaderDependencies":[]}
            ]}
            """.formatted(MATERIAL), known, "missing asset");
        assertRejected("""
            {"schemaVersion":1,"assets":[
              {"assetId":"%s","assetDependencies":["%s"],"shaderDependencies":[]},
              {"assetId":"%s","assetDependencies":["%s"],"shaderDependencies":[]}
            ]}
            """.formatted(PREFAB, SCENE, SCENE, PREFAB), known, "cycle");
        assertRejected("""
            {"schemaVersion":1,"assets":[
              {"assetId":"%s","assetDependencies":[],"shaderDependencies":["not/valid"]}
            ]}
            """.formatted(MATERIAL), known, "invalid shader");

    }

    private static void assertRejected(String json, Map<AssetId, AssetType> known, String message) {

        assertThatThrownBy(() -> CookedDependencyGraphJson.decode(json, known)).isInstanceOf(AssetCookerException.class).hasMessageContaining(message);

    }
}
