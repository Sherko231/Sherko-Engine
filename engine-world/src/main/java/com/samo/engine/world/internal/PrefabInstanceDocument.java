package com.samo.engine.world.internal;

import com.samo.engine.assets.api.AssetId;
import com.samo.engine.world.api.EntityGuid;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

record PrefabInstanceDocument(String instanceKey, AssetId prefabAssetId, EntityGuid parentGuid, List<PrefabPropertyOverride> overrides) {
    private static final Pattern INSTANCE_KEY = Pattern.compile("[a-z][a-z0-9_-]*");

    PrefabInstanceDocument {

        if (instanceKey == null || !INSTANCE_KEY.matcher(instanceKey).matches()) {
            throw new PrefabFormatException("invalid prefab instanceKey: " + instanceKey);
        }
        Objects.requireNonNull(prefabAssetId, "prefabAssetId");
        Objects.requireNonNull(overrides, "overrides");
        overrides = List.copyOf(overrides);

        Set<List<Object>> targets = new HashSet<>();
        for (PrefabPropertyOverride override : overrides) {
            List<Object> target = List.of(override.instancePath(), override.entityGuid(), override.component(), override.property());
            if (!targets.add(target)) {
                throw new PrefabFormatException("duplicate prefab override target: " + instanceKey + " / " + override.instancePath() + " / "
                    + override.entityGuid() + " / " + override.component() + "." + override.property());
            }
        }

    }
}
