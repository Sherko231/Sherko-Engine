package com.samo.engine.world.internal;

import com.samo.engine.world.api.EntityGuid;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

record PrefabDocument(SceneDocument scene, List<PrefabInstanceDocument> instances) {
    static final int CURRENT_SCHEMA_VERSION = 1;

    PrefabDocument {

        Objects.requireNonNull(scene, "scene");
        Objects.requireNonNull(instances, "instances");
        instances = List.copyOf(instances);
        Set<String> keys = new HashSet<>();
        Set<EntityGuid> localGuids = new HashSet<>();
        for (SceneEntityDocument entity : scene.entities()) {
            localGuids.add(entity.guid());
        }
        for (PrefabInstanceDocument instance : instances) {
            if (!keys.add(instance.instanceKey())) {
                throw new PrefabFormatException("duplicate sibling instanceKey: " + instance.instanceKey());
            }
            if (instance.parentGuid() != null && !localGuids.contains(instance.parentGuid())) {
                throw new PrefabFormatException("missing local attachment parentGuid: " + instance.parentGuid());
            }
        }

    }
}
