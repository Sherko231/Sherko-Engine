package com.samo.engine.world.internal;

import com.samo.engine.world.api.EntityGuid;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

record SceneDocument(List<SceneEntityDocument> entities) {
    static final int CURRENT_SCHEMA_VERSION = 1;

    SceneDocument {

        Objects.requireNonNull(entities, "entities");
        entities = List.copyOf(entities);
        validateHierarchy(entities);

    }

    private static void validateHierarchy(List<SceneEntityDocument> entities) {

        Map<EntityGuid, EntityGuid> parentsByGuid = new HashMap<>();
        for (SceneEntityDocument entity : entities) {
            EntityGuid guid = entity.guid();
            if (parentsByGuid.putIfAbsent(guid, entity.parentGuid()) != null || parentsByGuid.containsKey(guid) && countGuid(entities, guid) > 1) {
                throw new SceneFormatException("duplicate entity guid: " + guid);
            }
        }

        for (SceneEntityDocument entity : entities) {
            EntityGuid parentGuid = entity.parentGuid();
            if (parentGuid == null) {
                continue;
            }
            if (entity.guid().equals(parentGuid)) {
                throw new SceneFormatException("entity cannot parent itself: " + entity.guid());
            }
            if (!parentsByGuid.containsKey(parentGuid)) {
                throw new SceneFormatException("missing parent entity guid: " + parentGuid);
            }
        }

        Set<EntityGuid> resolved = new HashSet<>();
        for (SceneEntityDocument entity : entities) {
            EntityGuid current = entity.guid();
            Set<EntityGuid> path = new HashSet<>();
            while (current != null && !resolved.contains(current)) {
                if (!path.add(current)) {
                    throw new SceneFormatException("parent cycle contains entity guid: " + current);
                }
                current = parentsByGuid.get(current);
            }
            resolved.addAll(path);
        }

    }

    private static int countGuid(List<SceneEntityDocument> entities, EntityGuid guid) {

        int count = 0;
        for (SceneEntityDocument entity : entities) {
            if (entity.guid().equals(guid)) {
                count++;
            }
        }
        return count;

    }
}
