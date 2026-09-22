package com.samo.engine.assets.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.samo.engine.assets.api.AssetId;
import com.samo.engine.assets.api.AssetType;
import com.samo.engine.assets.api.SourceAssetMetadata;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class AssetDependencyGraphTest {
    private static final AssetId TEXTURE = id(1);
    private static final AssetId MATERIAL = id(2);
    private static final AssetId PREFAB = id(3);
    private static final AssetId SCENE = id(4);
    private static final AssetId OTHER_TEXTURE = id(5);
    private static final AssetId OTHER_MATERIAL = id(6);

    @Test
    void returnsOnlyDeterministicTransitiveDependentsForAssetAndShader() {

        AssetDependencyGraph graph = AssetDependencyGraph.fromSources(List.of(source(TEXTURE, AssetType.TEXTURE, SourceAssetDependencies.EMPTY),
            source(MATERIAL, AssetType.MATERIAL, new SourceAssetDependencies(List.of(TEXTURE), List.of("opaque-baseline"))),
            source(PREFAB, AssetType.PREFAB, new SourceAssetDependencies(List.of(MATERIAL), List.of())),
            source(SCENE, AssetType.SCENE, new SourceAssetDependencies(List.of(PREFAB), List.of())), source(OTHER_TEXTURE, AssetType.TEXTURE, SourceAssetDependencies.EMPTY),
            source(OTHER_MATERIAL, AssetType.MATERIAL, new SourceAssetDependencies(List.of(OTHER_TEXTURE), List.of("other")))));

        assertThat(graph.dependentsOfAsset(TEXTURE)).containsExactly(MATERIAL, PREFAB, SCENE);
        assertThat(graph.dependentsOfShader("opaque-baseline")).containsExactly(MATERIAL, PREFAB, SCENE);
        assertThat(graph.dependentsOfAsset(OTHER_TEXTURE)).containsExactly(OTHER_MATERIAL);
        assertThat(graph.dependentsOfAsset(id(99))).isEmpty();

    }

    @Test
    void deduplicatesDiamondDependents() {

        AssetId prefabA = id(10);
        AssetId prefabB = id(11);
        AssetId scene = id(12);
        AssetDependencyGraph graph = AssetDependencyGraph.fromSources(List.of(source(TEXTURE, AssetType.TEXTURE, SourceAssetDependencies.EMPTY),
            source(MATERIAL, AssetType.MATERIAL, new SourceAssetDependencies(List.of(TEXTURE), List.of())),
            source(prefabA, AssetType.PREFAB, new SourceAssetDependencies(List.of(MATERIAL), List.of())),
            source(prefabB, AssetType.PREFAB, new SourceAssetDependencies(List.of(MATERIAL), List.of())),
            source(scene, AssetType.SCENE, new SourceAssetDependencies(List.of(prefabA, prefabB), List.of()))));

        assertThat(graph.dependentsOfAsset(TEXTURE)).containsExactly(MATERIAL, prefabA, prefabB, scene);

    }

    @Test
    void rejectsMissingWrongTypeSelfShaderAndCycleDependencies() {

        assertThatThrownBy(() -> AssetDependencyGraph.fromSources(List.of(source(MATERIAL, AssetType.MATERIAL, new SourceAssetDependencies(List.of(TEXTURE), List.of())))))
            .isInstanceOf(AssetCookerException.class).hasMessageContaining("missing asset dependency");

        assertThatThrownBy(() -> AssetDependencyGraph.fromSources(List.of(source(PREFAB, AssetType.PREFAB, SourceAssetDependencies.EMPTY),
            source(MATERIAL, AssetType.MATERIAL, new SourceAssetDependencies(List.of(PREFAB), List.of()))))).isInstanceOf(AssetCookerException.class)
            .hasMessageContaining("MATERIAL").hasMessageContaining("TEXTURE");

        assertThatThrownBy(() -> AssetDependencyGraph.fromSources(List.of(source(PREFAB, AssetType.PREFAB, new SourceAssetDependencies(List.of(PREFAB), List.of())))))
            .isInstanceOf(AssetCookerException.class).hasMessageContaining("itself");

        assertThatThrownBy(() -> AssetDependencyGraph.fromSources(List.of(source(PREFAB, AssetType.PREFAB, new SourceAssetDependencies(List.of(), List.of("shader"))))))
            .isInstanceOf(AssetCookerException.class).hasMessageContaining("only MATERIAL");

        assertThatThrownBy(() -> AssetDependencyGraph.fromSources(List.of(source(PREFAB, AssetType.PREFAB, new SourceAssetDependencies(List.of(SCENE), List.of())),
            source(SCENE, AssetType.SCENE, new SourceAssetDependencies(List.of(PREFAB), List.of()))))).isInstanceOf(AssetCookerException.class).hasMessageContaining("cycle")
            .hasMessageContaining(PREFAB.toString()).hasMessageContaining(SCENE.toString());

    }

    private static AssetCooker.SourceAsset source(AssetId assetId, AssetType type, SourceAssetDependencies dependencies) {

        return new AssetCooker.SourceAsset(Path.of(assetId + ".bin"), new SourceAssetMetadata(1, assetId, type), assetId + ".bin", List.of(), List.of(), null, dependencies);

    }

    private static AssetId id(long low) {

        return new AssetId(0, low);

    }
}
