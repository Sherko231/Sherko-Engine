package com.samo.engine.assets.internal;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.samo.engine.assets.api.AssetId;
import com.samo.engine.assets.api.AssetType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

final class CookedDependencyGraphJson {
    static final int SCHEMA_VERSION = 1;
    private static final Set<String> ROOT_FIELDS = Set.of("schemaVersion", "assets");
    private static final Set<String> ENTRY_FIELDS = Set.of("assetId", "assetType", "assetDependencies", "shaderDependencies");
    private static final ObjectMapper MAPPER = new ObjectMapper(JsonFactory.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build());

    private CookedDependencyGraphJson() {

    }

    static String write(AssetDependencyGraph graph) {

        ObjectNode root = MAPPER.createObjectNode();
        root.put("schemaVersion", SCHEMA_VERSION);
        ArrayNode assets = root.putArray("assets");
        for (AssetDependencyGraph.Entry entry : graph.entries()) {
            ObjectNode asset = assets.addObject();
            asset.put("assetId", entry.assetId().toString());
            asset.put("assetType", entry.assetType().name());
            ArrayNode dependencies = asset.putArray("assetDependencies");
            entry.assetDependencies().forEach(assetId -> dependencies.add(assetId.toString()));
            ArrayNode shaders = asset.putArray("shaderDependencies");
            entry.shaderDependencies().forEach(shaders::add);
        }

        try {
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(root) + "\n";
        } catch (JsonProcessingException exception) {
            throw new AssetCookerException("Failed to serialize dependency graph", exception);
        }

    }

    static AssetDependencyGraph decode(String json, Set<AssetId> knownAssetIds) {

        if (json == null) {
            throw new NullPointerException("json");
        }
        if (knownAssetIds == null) {
            throw new NullPointerException("knownAssetIds");
        }

        try {
            JsonNode root = MAPPER.readTree(json);
            requireObject(root, "root");
            requireOnlyFields(root, ROOT_FIELDS, "root");
            JsonNode version = requireField(root, "schemaVersion", "root");
            if (!version.isIntegralNumber() || !version.canConvertToInt() || version.intValue() != SCHEMA_VERSION) {
                throw new AssetCookerException("Invalid dependency graph: unsupported schemaVersion " + version.asText() + "; upgrade required");
            }

            JsonNode assets = requireField(root, "assets", "root");
            if (!assets.isArray()) {
                throw new AssetCookerException("Invalid dependency graph: root.assets must be an array");
            }

            ArrayList<AssetDependencyGraph.Entry> entries = new ArrayList<>();
            HashSet<AssetId> owners = new HashSet<>();
            for (JsonNode node : assets) {
                requireObject(node, "asset entry");
                requireOnlyFields(node, ENTRY_FIELDS, "asset entry");
                AssetId owner = parseAssetId(requireText(node, "assetId"), "assetId");
                if (!owners.add(owner)) {
                    throw new AssetCookerException("Invalid dependency graph: duplicate owner " + owner);
                }
                AssetType type = parseOwnerType(requireText(node, "assetType"));
                List<AssetId> dependencies = parseAssetIds(requireField(node, "assetDependencies", "asset entry"));
                List<String> shaders = parseShaderKeys(requireField(node, "shaderDependencies", "asset entry"));
                entries.add(new AssetDependencyGraph.Entry(owner, type, dependencies, shaders));
            }
            return AssetDependencyGraph.fromPersisted(entries, knownAssetIds);
        } catch (AssetCookerException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new AssetCookerException("Invalid dependency graph JSON", exception);
        }

    }

    private static List<AssetId> parseAssetIds(JsonNode node) {

        if (!node.isArray()) {
            throw new AssetCookerException("Invalid dependency graph: assetDependencies must be an array");
        }
        ArrayList<AssetId> result = new ArrayList<>();
        HashSet<AssetId> unique = new HashSet<>();
        for (JsonNode item : node) {
            if (!item.isTextual()) {
                throw new AssetCookerException("Invalid dependency graph: assetDependencies entries must be strings");
            }
            AssetId assetId = parseAssetId(item.textValue(), "assetDependencies");
            if (!unique.add(assetId)) {
                throw new AssetCookerException("Invalid dependency graph: duplicate asset dependency " + assetId);
            }
            result.add(assetId);
        }
        return result.stream().sorted((left, right) -> left.toString().compareTo(right.toString())).toList();

    }

    private static List<String> parseShaderKeys(JsonNode node) {

        if (!node.isArray()) {
            throw new AssetCookerException("Invalid dependency graph: shaderDependencies must be an array");
        }
        ArrayList<String> result = new ArrayList<>();
        HashSet<String> unique = new HashSet<>();
        for (JsonNode item : node) {
            if (!item.isTextual() || !SourceAssetDependenciesJson.isCanonicalShaderKey(item.textValue())) {
                throw new AssetCookerException("Invalid dependency graph: invalid shader dependency");
            }
            if (!unique.add(item.textValue())) {
                throw new AssetCookerException("Invalid dependency graph: duplicate shader dependency " + item.textValue());
            }
            result.add(item.textValue());
        }
        result.sort(String::compareTo);
        return List.copyOf(result);

    }

    private static AssetId parseAssetId(String text, String field) {

        try {
            return AssetId.parse(text);
        } catch (IllegalArgumentException exception) {
            throw new AssetCookerException("Invalid dependency graph: " + field + " must be canonical AssetId text", exception);
        }

    }

    private static AssetType parseOwnerType(String text) {

        try {
            AssetType type = AssetType.valueOf(text);
            if (type != AssetType.MATERIAL && type != AssetType.PREFAB && type != AssetType.SCENE) {
                throw new AssetCookerException("Invalid dependency graph: unsupported owner assetType " + type);
            }
            return type;
        } catch (IllegalArgumentException exception) {
            throw new AssetCookerException("Invalid dependency graph: unknown assetType " + text, exception);
        }

    }

    private static String requireText(JsonNode object, String field) {

        JsonNode node = requireField(object, field, "asset entry");
        if (!node.isTextual() || node.textValue().isBlank()) {
            throw new AssetCookerException("Invalid dependency graph: " + field + " must be a nonblank string");
        }
        return node.textValue();

    }

    private static JsonNode requireField(JsonNode object, String field, String context) {

        JsonNode value = object.get(field);
        if (value == null || value.isNull()) {
            throw new AssetCookerException("Invalid dependency graph: missing required field " + context + "." + field);
        }
        return value;

    }

    private static void requireObject(JsonNode node, String context) {

        if (node == null || !node.isObject()) {
            throw new AssetCookerException("Invalid dependency graph: " + context + " must be an object");
        }

    }

    private static void requireOnlyFields(JsonNode object, Set<String> allowed, String context) {

        Iterator<String> fields = object.fieldNames();
        while (fields.hasNext()) {
            String field = fields.next();
            if (!allowed.contains(field)) {
                throw new AssetCookerException("Invalid dependency graph: unknown field " + context + "." + field);
            }
        }
        for (String field : allowed) {
            if (!object.has(field) || object.get(field).isNull()) {
                throw new AssetCookerException("Invalid dependency graph: missing required field " + context + "." + field);
            }
        }

    }
}
