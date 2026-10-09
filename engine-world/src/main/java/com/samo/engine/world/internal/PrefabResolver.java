package com.samo.engine.world.internal;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.samo.engine.assets.api.AssetId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

final class PrefabResolver {
    private static final ObjectMapper MAPPER = new ObjectMapper(JsonFactory.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build());

    private PrefabResolver() {

    }

    static List<PrefabResolvedGroup> resolve(AssetId rootPrefabAssetId, Map<AssetId, PrefabDocument> sources) {

        Objects.requireNonNull(rootPrefabAssetId, "rootPrefabAssetId");
        Objects.requireNonNull(sources, "sources");
        Map<AssetId, PrefabDocument> sourceSnapshot = Map.copyOf(sources);
        List<PrefabResolvedGroup> groups = new ArrayList<>();
        resolveInto(rootPrefabAssetId, List.of(), List.of(), null, sourceSnapshot, new HashSet<>(), groups);
        return List.copyOf(groups);

    }

    private static void resolveInto(AssetId prefabId, List<String> path, List<String> parentPath, com.samo.engine.world.api.EntityGuid parentGuid,
        Map<AssetId, PrefabDocument> sources, Set<AssetId> active, List<PrefabResolvedGroup> groups) {

        PrefabDocument source = sources.get(prefabId);
        if (source == null) {
            throw new PrefabFormatException("missing referenced prefab AssetId: " + prefabId);
        }
        if (!active.add(prefabId)) {
            throw new PrefabFormatException("cyclic prefab reference (complete chain validation belongs to P7-T10)");
        }

        try {
            groups.add(new PrefabResolvedGroup(path, prefabId, source.scene(), parentPath, parentGuid));
            for (PrefabInstanceDocument instance : source.instances()) {
                List<String> childPath = append(path, instance.instanceKey());
                resolveInto(instance.prefabAssetId(), childPath, path, instance.parentGuid(), sources, active, groups);
                for (PrefabPropertyOverride override : instance.overrides()) {
                    List<String> targetPath = appendAll(childPath, override.instancePath());
                    int index = findGroup(groups, targetPath);
                    if (index < 0) {
                        throw new PrefabFormatException("unknown prefab override instancePath: " + targetPath);
                    }
                    PrefabResolvedGroup group = groups.get(index);
                    SceneDocument updated = applyOverride(group.scene(), override, targetPath);
                    groups.set(index,
                        new PrefabResolvedGroup(group.instancePath(), group.sourcePrefabAssetId(), updated, group.parentInstancePath(), group.parentGuid()));
                }
            }
        } finally {
            active.remove(prefabId);
        }

    }

    private static int findGroup(List<PrefabResolvedGroup> groups, List<String> path) {

        for (int i = 0; i < groups.size(); i++) {
            if (groups.get(i).instancePath().equals(path)) {
                return i;
            }
        }
        return -1;

    }

    private static SceneDocument applyOverride(SceneDocument scene, PrefabPropertyOverride override, List<String> path) {

        try {
            ObjectNode root = (ObjectNode) MAPPER.readTree(SceneJsonCodec.encode(scene));
            ArrayNode entities = (ArrayNode) root.get("entities");
            for (JsonNode entityNode : entities) {
                if (!entityNode.get("guid").textValue().equals(override.entityGuid().toString())) {
                    continue;
                }
                JsonNode component = entityNode.get("components").get(override.component());
                if (!(component instanceof ObjectNode componentObject) || !componentObject.has(override.property())) {
                    throw new PrefabFormatException("override targets missing component property at " + path + ": " + override.component() + "."
                        + override.property());
                }
                JsonNode value = MAPPER.readTree(override.valueJson());
                if (value == null) {
                    throw new PrefabFormatException("override has invalid JSON value at " + path);
                }
                componentObject.set(override.property(), value);
                return SceneJsonCodec.decode(MAPPER.writeValueAsString(root));
            }
            throw new PrefabFormatException("override targets missing entityGuid at " + path + ": " + override.entityGuid());
        } catch (JsonProcessingException exception) {
            throw new PrefabFormatException("invalid override JSON at " + path, exception);
        } catch (SceneFormatException exception) {
            throw new PrefabFormatException("invalid override value at " + path + ": " + exception.getMessage(), exception);
        }

    }

    private static List<String> append(List<String> path, String segment) {

        List<String> result = new ArrayList<>(path);
        result.add(segment);
        return List.copyOf(result);

    }

    private static List<String> appendAll(List<String> prefix, List<String> suffix) {

        List<String> result = new ArrayList<>(prefix);
        result.addAll(suffix);
        return List.copyOf(result);

    }
}
