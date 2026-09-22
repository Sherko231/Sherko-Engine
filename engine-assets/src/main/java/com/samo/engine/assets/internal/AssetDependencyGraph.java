package com.samo.engine.assets.internal;

import com.samo.engine.assets.api.AssetId;
import com.samo.engine.assets.api.AssetType;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class AssetDependencyGraph {
    private static final Comparator<AssetId> ASSET_ID_ORDER = Comparator.comparing(AssetId::toString);

    private final Map<AssetId, Entry> entries;
    private final Map<AssetId, List<AssetId>> reverseAssetDependencies;
    private final Map<String, List<AssetId>> reverseShaderDependencies;
    private final Set<AssetId> knownAssets.keySet();

    private AssetDependencyGraph(Map<AssetId, Entry> entries, Map<AssetId, List<AssetId>> reverseAssetDependencies, Map<String, List<AssetId>> reverseShaderDependencies,
        Set<AssetId> knownAssets.keySet()) {

        this.entries = Map.copyOf(entries);
        this.reverseAssetDependencies = Map.copyOf(reverseAssetDependencies);
        this.reverseShaderDependencies = Map.copyOf(reverseShaderDependencies);
        this.knownAssets.keySet() = Set.copyOf(knownAssets.keySet());

    }

    static AssetDependencyGraph fromSources(List<AssetCooker.SourceAsset> sources) {

        HashMap<AssetId, AssetType> types = new HashMap<>();
        for (AssetCooker.SourceAsset source : sources) {
            types.put(source.metadata().assetId(), source.metadata().assetType());
        }

        LinkedHashMap<AssetId, Entry> entries = new LinkedHashMap<>();
        for (AssetCooker.SourceAsset source : sources) {
            AssetType type = source.metadata().assetType();
            SourceAssetDependencies dependencies = source.dependencies();
            boolean graphOwner = type == AssetType.MATERIAL || type == AssetType.PREFAB || type == AssetType.SCENE;
            if (!graphOwner) {
                if (!dependencies.assetDependencies().isEmpty() || !dependencies.shaderDependencies().isEmpty()) {
                    throw new AssetCookerException(source.sourcePath() + ": dependency sidecars are supported only for MATERIAL, PREFAB, or SCENE assets");
                }
                continue;
            }

            validateOwner(source, dependencies, types);
            ArrayList<AssetId> assetDependencies = new ArrayList<>(dependencies.assetDependencies());
            assetDependencies.sort(ASSET_ID_ORDER);
            ArrayList<String> shaderDependencies = new ArrayList<>(dependencies.shaderDependencies());
            shaderDependencies.sort(String::compareTo);
            entries.put(source.metadata().assetId(), new Entry(source.metadata().assetId(), type, assetDependencies, shaderDependencies));
        }

        rejectCycles(entries);
        return build(entries, types.keySet());

    }

    static AssetDependencyGraph fromPersisted(Collection<Entry> persistedEntries, Map<AssetId, AssetType> knownAssets) {

        LinkedHashMap<AssetId, Entry> entries = new LinkedHashMap<>();
        for (Entry entry : persistedEntries) {
            if (!knownAssets.keySet().contains(entry.assetId())) {
                throw new AssetCookerException("Dependency graph owner is not present in known assets: " + entry.assetId());
            }
            if (entries.putIfAbsent(entry.assetId(), entry) != null) {
                throw new AssetCookerException("Duplicate dependency graph owner " + entry.assetId());
            }
            for (AssetId dependency : entry.assetDependencies()) {
                AssetType dependencyType = knownAssets.get(dependency);
                if (dependencyType == null) {
                    throw new AssetCookerException("Dependency graph references missing asset " + dependency);
                }
                if (dependency.equals(entry.assetId())) {
                    throw new AssetCookerException("Dependency graph owner cannot depend on itself: " + entry.assetId());
                }
                if (entry.assetType() == AssetType.MATERIAL && dependencyType != AssetType.TEXTURE) {
                    throw new AssetCookerException("MATERIAL dependency graph entries must reference TEXTURE assets, got " + dependencyType + " for " + dependency);
                }
            }
            if (entry.assetType() != AssetType.MATERIAL && !entry.shaderDependencies().isEmpty()) {
                throw new AssetCookerException("Only MATERIAL dependency entries may contain shader dependencies: " + entry.assetId());
            }
        }

        rejectCycles(entries);
        return build(entries, knownAssets.keySet());

    }

    List<AssetId> dependentsOfAsset(AssetId changed) {

        if (changed == null) {
            throw new NullPointerException("changed");
        }
        return transitiveDependents(reverseAssetDependencies.getOrDefault(changed, List.of()));

    }

    List<AssetId> dependentsOfShader(String shaderKey) {

        if (shaderKey == null) {
            throw new NullPointerException("shaderKey");
        }
        if (!SourceAssetDependenciesJson.isCanonicalShaderKey(shaderKey)) {
            throw new IllegalArgumentException("shaderKey must be canonical");
        }
        return transitiveDependents(reverseShaderDependencies.getOrDefault(shaderKey, List.of()));

    }

    List<Entry> entries() {

        return entries.values().stream().sorted(Comparator.comparing(entry -> entry.assetId().toString())).toList();

    }

    Set<AssetId> knownAssets.keySet()() {

        return knownAssets.keySet();

    }

    private List<AssetId> transitiveDependents(List<AssetId> roots) {

        HashSet<AssetId> visited = new HashSet<>();
        ArrayDeque<AssetId> pending = new ArrayDeque<>(roots);
        while (!pending.isEmpty()) {
            AssetId current = pending.removeFirst();
            if (!visited.add(current)) {
                continue;
            }
            pending.addAll(reverseAssetDependencies.getOrDefault(current, List.of()));
        }
        return visited.stream().sorted(ASSET_ID_ORDER).toList();

    }

    private static void validateOwner(AssetCooker.SourceAsset source, SourceAssetDependencies dependencies, Map<AssetId, AssetType> types) {

        AssetId owner = source.metadata().assetId();
        AssetType ownerType = source.metadata().assetType();
        for (AssetId dependency : dependencies.assetDependencies()) {
            if (dependency.equals(owner)) {
                throw new AssetCookerException(source.sourcePath() + ": asset cannot depend on itself: " + owner);
            }
            AssetType dependencyType = types.get(dependency);
            if (dependencyType == null) {
                throw new AssetCookerException(source.sourcePath() + ": missing asset dependency " + dependency);
            }
            if (ownerType == AssetType.MATERIAL && dependencyType != AssetType.TEXTURE) {
                throw new AssetCookerException(source.sourcePath() + ": MATERIAL asset dependencies must reference TEXTURE assets, got " + dependencyType + " for " + dependency);
            }
        }
        if (ownerType != AssetType.MATERIAL && !dependencies.shaderDependencies().isEmpty()) {
            throw new AssetCookerException(source.sourcePath() + ": only MATERIAL assets may declare shaderDependencies");
        }

    }

    private static AssetDependencyGraph build(Map<AssetId, Entry> entries, Set<AssetId> knownAssets.keySet()) {

        HashMap<AssetId, ArrayList<AssetId>> reverseAssets = new HashMap<>();
        HashMap<String, ArrayList<AssetId>> reverseShaders = new HashMap<>();
        for (Entry entry : entries.values()) {
            for (AssetId dependency : entry.assetDependencies()) {
                reverseAssets.computeIfAbsent(dependency, ignored -> new ArrayList<>()).add(entry.assetId());
            }
            for (String shaderKey : entry.shaderDependencies()) {
                reverseShaders.computeIfAbsent(shaderKey, ignored -> new ArrayList<>()).add(entry.assetId());
            }
        }
        reverseAssets.values().forEach(values -> values.sort(ASSET_ID_ORDER));
        reverseShaders.values().forEach(values -> values.sort(ASSET_ID_ORDER));

        Map<AssetId, List<AssetId>> immutableReverseAssets = new HashMap<>();
        reverseAssets.forEach((key, value) -> immutableReverseAssets.put(key, List.copyOf(value)));
        Map<String, List<AssetId>> immutableReverseShaders = new HashMap<>();
        reverseShaders.forEach((key, value) -> immutableReverseShaders.put(key, List.copyOf(value)));

        return new AssetDependencyGraph(entries, immutableReverseAssets, immutableReverseShaders, knownAssets.keySet());

    }

    private static void rejectCycles(Map<AssetId, Entry> entries) {

        HashSet<AssetId> visiting = new HashSet<>();
        HashSet<AssetId> visited = new HashSet<>();
        ArrayDeque<AssetId> path = new ArrayDeque<>();
        for (AssetId assetId : entries.keySet()) {
            visit(assetId, entries, visiting, visited, path);
        }

    }

    private static void visit(AssetId assetId, Map<AssetId, Entry> entries, Set<AssetId> visiting, Set<AssetId> visited, ArrayDeque<AssetId> path) {

        if (visited.contains(assetId)) {
            return;
        }
        if (!visiting.add(assetId)) {
            String cycle = path.stream().map(AssetId::toString).reduce((left, right) -> left + " -> " + right).orElse(assetId.toString());
            throw new AssetCookerException("Asset dependency cycle detected: " + cycle + " -> " + assetId);
        }

        path.addLast(assetId);
        Entry entry = entries.get(assetId);
        if (entry != null) {
            for (AssetId dependency : entry.assetDependencies()) {
                if (entries.containsKey(dependency)) {
                    visit(dependency, entries, visiting, visited, path);
                }
            }
        }
        path.removeLast();
        visiting.remove(assetId);
        visited.add(assetId);

    }

    record Entry(AssetId assetId, AssetType assetType, List<AssetId> assetDependencies, List<String> shaderDependencies) {
        Entry {

            if (assetId == null || assetType == null || assetDependencies == null || shaderDependencies == null) {
                throw new NullPointerException("Dependency graph entry fields must be nonnull");
            }
            if (assetType != AssetType.MATERIAL && assetType != AssetType.PREFAB && assetType != AssetType.SCENE) {
                throw new IllegalArgumentException("Dependency graph entry type must be MATERIAL, PREFAB, or SCENE");
            }
            assetDependencies = List.copyOf(assetDependencies);
            shaderDependencies = List.copyOf(shaderDependencies);

        }
    }
}
