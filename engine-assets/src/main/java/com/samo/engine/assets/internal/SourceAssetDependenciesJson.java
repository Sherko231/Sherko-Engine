package com.samo.engine.assets.internal;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.samo.engine.assets.api.AssetId;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

final class SourceAssetDependenciesJson {
    static final int SCHEMA_VERSION = 1;
    private static final Set<String> ROOT_FIELDS = Set.of("schemaVersion", "assetDependencies", "shaderDependencies");
    private static final Pattern SHADER_KEY = Pattern.compile("[a-z][a-z0-9._-]{0,127}");
    private static final ObjectMapper MAPPER = new ObjectMapper(JsonFactory.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build());

    private SourceAssetDependenciesJson() {

    }

    static SourceAssetDependencies load(Path path) {

        try {
            JsonNode root = MAPPER.readTree(Files.readString(path, StandardCharsets.UTF_8));
            return parse(path, root);
        } catch (AssetCookerException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new AssetCookerException(path + ": failed to read dependency sidecar", exception);
        }

    }

    static boolean isCanonicalShaderKey(String key) {

        return key != null && SHADER_KEY.matcher(key).matches();

    }

    private static SourceAssetDependencies parse(Path path, JsonNode root) {

        requireObject(path, root);
        requireOnlyFields(path, root);
        requireVersion(path, root);

        List<AssetId> assetDependencies = parseAssetDependencies(path, requireArray(path, root, "assetDependencies"));
        List<String> shaderDependencies = parseShaderDependencies(path, requireArray(path, root, "shaderDependencies"));
        return new SourceAssetDependencies(assetDependencies, shaderDependencies);

    }

    private static List<AssetId> parseAssetDependencies(Path path, JsonNode array) {

        ArrayList<AssetId> values = new ArrayList<>();
        Set<AssetId> unique = new HashSet<>();
        for (JsonNode node : array) {
            if (!node.isTextual()) {
                throw failure(path, "assetDependencies entries must be canonical AssetId strings");
            }
            AssetId assetId;
            try {
                assetId = AssetId.parse(node.textValue());
            } catch (IllegalArgumentException exception) {
                throw new AssetCookerException(path + ": assetDependencies entry must be canonical lowercase AssetId text", exception);
            }
            if (!unique.add(assetId)) {
                throw failure(path, "duplicate asset dependency " + assetId);
            }
            values.add(assetId);
        }
        return List.copyOf(values);

    }

    private static List<String> parseShaderDependencies(Path path, JsonNode array) {

        ArrayList<String> values = new ArrayList<>();
        Set<String> unique = new HashSet<>();
        for (JsonNode node : array) {
            if (!node.isTextual() || !isCanonicalShaderKey(node.textValue())) {
                throw failure(path, "shaderDependencies entries must match [a-z][a-z0-9._-]{0,127}");
            }
            String key = node.textValue();
            if (!unique.add(key)) {
                throw failure(path, "duplicate shader dependency " + key);
            }
            values.add(key);
        }
        return List.copyOf(values);

    }

    private static void requireObject(Path path, JsonNode root) {

        if (root == null || !root.isObject()) {
            throw failure(path, "root must be a JSON object");
        }

    }

    private static void requireOnlyFields(Path path, JsonNode root) {

        Iterator<String> names = root.fieldNames();
        while (names.hasNext()) {
            String name = names.next();
            if (!ROOT_FIELDS.contains(name)) {
                throw failure(path, "unknown field root." + name);
            }
        }
        for (String field : ROOT_FIELDS) {
            if (!root.has(field) || root.get(field).isNull()) {
                throw failure(path, "missing required field root." + field);
            }
        }

    }

    private static void requireVersion(Path path, JsonNode root) {

        JsonNode version = root.get("schemaVersion");
        if (!version.isIntegralNumber()) {
            throw failure(path, "schemaVersion must be an integer");
        }
        if (!version.canConvertToInt() || version.intValue() != SCHEMA_VERSION) {
            throw failure(path, "unsupported schemaVersion " + version.asText() + "; upgrade required");
        }

    }

    private static JsonNode requireArray(Path path, JsonNode root, String field) {

        JsonNode value = root.get(field);
        if (!value.isArray()) {
            throw failure(path, field + " must be an array");
        }
        return value;

    }

    private static AssetCookerException failure(Path path, String message) {

        return new AssetCookerException(path + ": " + message);

    }
}
