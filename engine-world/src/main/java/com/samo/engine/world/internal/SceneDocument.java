package com.samo.engine.world.internal;

import com.samo.engine.world.api.EntityGuid;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

record SceneDocument(List<SceneEntityDocument> entities, SceneEditorData editorData) {
    static final int CURRENT_SCHEMA_VERSION = 1;

    SceneDocument(List<SceneEntityDocument> entities) {

        this(entities, null);

    }

    SceneDocument {

        Objects.requireNonNull(entities, "entities");
        entities = List.copyOf(entities);
        validateHierarchy(entities);

    }

    private static void validateHierarchy(List<SceneEntityDocument> entities) {

        Map<EntityGuid, EntityGuid> parentsByGuid = new HashMap<>();
        Set<EntityGuid> seenGuids = new HashSet<>();
        for (SceneEntityDocument entity : entities) {
            EntityGuid guid = entity.guid();
            if (!seenGuids.add(guid)) {
                throw new SceneFormatException("duplicate entity guid: " + guid);
            }
            parentsByGuid.put(guid, entity.parentGuid());
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
}
