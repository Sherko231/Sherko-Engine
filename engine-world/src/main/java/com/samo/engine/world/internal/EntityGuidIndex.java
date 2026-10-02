package com.samo.engine.world.internal;

import com.samo.engine.world.api.EntityGuid;
import com.samo.engine.world.api.EntityId;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

final class EntityGuidIndex {
    private final EntityIdAllocator allocator;
    private final Map<EntityGuid, EntityId> entitiesByGuid = new HashMap<>();
    private final Map<EntityId, EntityGuid> guidsByEntity = new HashMap<>();

    EntityGuidIndex(EntityIdAllocator allocator) {

        this.allocator = Objects.requireNonNull(allocator, "allocator");

    }

    void bind(EntityGuid guid, EntityId entityId) {

        Objects.requireNonNull(guid, "guid");
        Objects.requireNonNull(entityId, "entityId");
        if (!allocator.isAlive(entityId)) {
            throw new IllegalArgumentException("entityId must be live");
        }

        purgeDeadBinding(guid);
        if (entitiesByGuid.containsKey(guid)) {
            throw new IllegalStateException("guid is already bound");
        }
        if (guidsByEntity.containsKey(entityId)) {
            throw new IllegalStateException("entityId is already bound");
        }

        entitiesByGuid.put(guid, entityId);
        guidsByEntity.put(entityId, guid);

    }

    EntityId resolve(EntityGuid guid) {

        Objects.requireNonNull(guid, "guid");
        EntityId entityId = entitiesByGuid.get(guid);
        if (entityId == null) {
            return null;
        }
        if (!allocator.isAlive(entityId)) {
            removePair(guid, entityId);
            return null;
        }
        return entityId;

    }

    EntityGuid guidFor(EntityId entityId) {

        Objects.requireNonNull(entityId, "entityId");
        EntityGuid guid = guidsByEntity.get(entityId);
        if (guid == null) {
            return null;
        }
        if (!allocator.isAlive(entityId)) {
            removePair(guid, entityId);
            return null;
        }
        return guid;

    }

    boolean unbind(EntityId entityId) {

        Objects.requireNonNull(entityId, "entityId");
        EntityGuid guid = guidsByEntity.remove(entityId);
        if (guid == null) {
            return false;
        }
        entitiesByGuid.remove(guid, entityId);
        return true;

    }

    private void purgeDeadBinding(EntityGuid guid) {

        EntityId previousEntityId = entitiesByGuid.get(guid);
        if (previousEntityId != null && !allocator.isAlive(previousEntityId)) {
            removePair(guid, previousEntityId);
        }

    }

    private void removePair(EntityGuid guid, EntityId entityId) {

        entitiesByGuid.remove(guid, entityId);
        guidsByEntity.remove(entityId, guid);

    }
}
