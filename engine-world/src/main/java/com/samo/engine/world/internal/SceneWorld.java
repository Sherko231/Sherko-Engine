package com.samo.engine.world.internal;

import com.samo.engine.world.api.AudioEmitterComponent;
import com.samo.engine.world.api.CameraComponent;
import com.samo.engine.world.api.EntityGuid;
import com.samo.engine.world.api.EntityId;
import com.samo.engine.world.api.MeshRendererComponent;
import com.samo.engine.world.api.NameComponent;
import com.samo.engine.world.api.TransformComponent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * One isolated, in-memory scene snapshot; all entity IDs and component stores belong to this world.
 */
final class SceneWorld {
    private final EntityIdAllocator allocator = new EntityIdAllocator();
    private final Map<List<String>, EntityGuidIndex> guidIndices = new HashMap<>();
    private final Map<EntityId, EntityId> parents = new HashMap<>();
    private final PackedComponentStore<TransformComponent> transforms = new PackedComponentStore<>(allocator);
    private final PackedComponentStore<NameComponent> names = new PackedComponentStore<>(allocator);
    private final PackedComponentStore<MeshRendererComponent> meshRenderers = new PackedComponentStore<>(allocator);
    private final PackedComponentStore<CameraComponent> cameras = new PackedComponentStore<>(allocator);
    private final PackedComponentStore<AudioEmitterComponent> audioEmitters = new PackedComponentStore<>(allocator);
    private int entityCount;

    private SceneWorld() {

    }

    static SceneWorld empty() {

        return new SceneWorld();

    }

    static SceneWorld fromScene(SceneDocument scene) {

        Objects.requireNonNull(scene, "scene");
        return stage(List.of(new Group(List.of(), scene, List.of(), null)));

    }

    static SceneWorld fromPrefabs(List<PrefabResolvedGroup> resolvedGroups) {

        Objects.requireNonNull(resolvedGroups, "resolvedGroups");
        List<Group> groups = new ArrayList<>();
        for (PrefabResolvedGroup resolved : resolvedGroups) {
            Objects.requireNonNull(resolved, "resolved group");
            groups.add(new Group(resolved.instancePath(), resolved.scene(), resolved.parentInstancePath(), resolved.parentGuid()));
        }
        return stage(groups);

    }

    private static SceneWorld stage(List<Group> groups) {

        if (groups.isEmpty() || !groups.getFirst().path().isEmpty()) {
            throw new SceneFormatException("scene groups must begin with one root instance path");
        }

        SceneWorld candidate = new SceneWorld();
        Set<List<String>> paths = new HashSet<>();
        for (Group group : groups) {
            if (!paths.add(group.path())) {
                throw new SceneFormatException("duplicate resolved scene instance path: " + group.path());
            }
            for (SceneEntityDocument entity : group.scene().entities()) {
                candidate.addEntity(group.path(), entity);
            }
        }

        // Resolve all parents after all GUID namespaces have been populated, including forward references.
        for (Group group : groups) {
            for (SceneEntityDocument entity : group.scene().entities()) {
                EntityId child = candidate.entity(group.path(), entity.guid());
                EntityId parent = null;
                if (entity.parentGuid() != null) {
                    parent = candidate.entity(group.path(), entity.parentGuid());
                    if (parent == null) {
                        throw new SceneFormatException("missing local scene parentGuid: " + entity.parentGuid() + " at " + group.path());
                    }
                } else if (group.parentGuid() != null) {
                    parent = candidate.entity(group.parentPath(), group.parentGuid());
                    if (parent == null) {
                        throw new SceneFormatException("missing prefab attachment parentGuid: " + group.parentGuid() + " at " + group.path());
                    }
                }
                if (parent != null) {
                    candidate.bindParent(child, parent);
                }
            }
        }
        return candidate;

    }

    private void addEntity(List<String> path, SceneEntityDocument document) {

        EntityId entityId = allocator.create();
        guidIndices.computeIfAbsent(path, ignored -> new EntityGuidIndex(allocator)).bind(document.guid(), entityId);
        SceneComponentsDocument components = document.components();
        if (components.transform() != null) {
            requireAdded(transforms.add(entityId, components.transform().toComponent()));
        }
        if (components.name() != null) {
            requireAdded(names.add(entityId, components.name()));
        }
        if (components.meshRenderer() != null) {
            requireAdded(meshRenderers.add(entityId, components.meshRenderer()));
        }
        if (components.camera() != null) {
            requireAdded(cameras.add(entityId, components.camera()));
        }
        if (components.audioEmitter() != null) {
            requireAdded(audioEmitters.add(entityId, components.audioEmitter()));
        }
        entityCount++;

    }

    private void bindParent(EntityId child, EntityId parent) {

        if (!allocator.isAlive(child) || !allocator.isAlive(parent) || child.equals(parent) || parents.putIfAbsent(child, parent) != null) {
            throw new SceneFormatException("invalid runtime scene parent binding");
        }
        TransformComponent childTransform = transforms.get(child);
        TransformComponent parentTransform = transforms.get(parent);
        if (childTransform != null && parentTransform != null) {
            childTransform.transform().setParent(parentTransform.transform());
        }

    }

    private static void requireAdded(boolean added) {

        if (!added) {
            throw new SceneFormatException("failed to add a scene component to a newly allocated entity");
        }

    }

    EntityId entity(List<String> path, EntityGuid guid) {

        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(guid, "guid");
        EntityGuidIndex index = guidIndices.get(path);
        return index == null ? null : index.resolve(guid);

    }

    EntityId parent(EntityId entityId) {

        return parents.get(Objects.requireNonNull(entityId, "entityId"));

    }

    TransformComponent transform(EntityId entityId) {

        return transforms.get(entityId);

    }

    NameComponent name(EntityId entityId) {

        return names.get(entityId);

    }

    MeshRendererComponent meshRenderer(EntityId entityId) {

        return meshRenderers.get(entityId);

    }

    CameraComponent camera(EntityId entityId) {

        return cameras.get(entityId);

    }

    AudioEmitterComponent audioEmitter(EntityId entityId) {

        return audioEmitters.get(entityId);

    }

    boolean isAlive(EntityId entityId) {

        return allocator.isAlive(entityId);

    }

    int entityCount() {

        return entityCount;

    }

    private record Group(List<String> path, SceneDocument scene, List<String> parentPath, EntityGuid parentGuid) {
        private Group {

            path = List.copyOf(path);
            parentPath = List.copyOf(parentPath);
            Objects.requireNonNull(scene, "scene");

        }
    }
}
