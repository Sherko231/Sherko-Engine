package com.samo.engine.world.internal;

import com.samo.engine.assets.api.AssetId;
import com.samo.engine.world.api.EntityGuid;
import java.util.List;
import java.util.Objects;

record PrefabResolvedGroup(List<String> instancePath, AssetId sourcePrefabAssetId, SceneDocument scene, List<String> parentInstancePath,
    EntityGuid parentGuid) {

    PrefabResolvedGroup {

        instancePath = List.copyOf(instancePath);
        parentInstancePath = List.copyOf(parentInstancePath);
        Objects.requireNonNull(sourcePrefabAssetId, "sourcePrefabAssetId");
        Objects.requireNonNull(scene, "scene");

    }

}
