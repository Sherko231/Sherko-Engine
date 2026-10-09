package com.samo.engine.world.internal;

import com.samo.engine.assets.api.AssetId;
import java.util.Map;
import java.util.Objects;

/**
 * Single-caller scene activation boundary: failed staging never touches the active world.
 */
final class SceneWorldActivation {
    private SceneWorld activeWorld = SceneWorld.empty();

    SceneWorld activeWorld() {

        return activeWorld;

    }

    void loadAndActivateScene(String sceneJson) {

        SceneDocument document = SceneJsonCodec.decode(Objects.requireNonNull(sceneJson, "sceneJson"));
        SceneWorld candidate = SceneWorld.fromScene(document);
        activeWorld = candidate;

    }

    void loadAndActivatePrefab(AssetId rootPrefabAssetId, Map<AssetId, PrefabDocument> sources) {

        SceneWorld candidate = SceneWorld.fromPrefabs(PrefabResolver.resolve(rootPrefabAssetId, sources));
        activeWorld = candidate;

    }
}
