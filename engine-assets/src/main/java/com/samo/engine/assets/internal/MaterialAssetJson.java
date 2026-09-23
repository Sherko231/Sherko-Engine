package com.samo.engine.assets.internal;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.samo.engine.assets.api.MaterialAsset;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

final class MaterialAssetJson {
    static final int SCHEMA_VERSION = 1;
    private static final Set<String> FIELDS = Set.of("schemaVersion", "shaderKey", "redMultiplier", "greenMultiplier", "blueMultiplier", "alphaMultiplier");
    private static final ObjectMapper MAPPER = new ObjectMapper(JsonFactory.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build());

    private MaterialAssetJson() {

    }

    static MaterialAsset decode(byte[] bytes) {

        if (bytes == null) {
            throw new NullPointerException("bytes");
        }

        try {
            JsonNode root = MAPPER.readTree(bytes);
            requireExactFields(root);
            JsonNode version = root.get("schemaVersion");
            if (!version.isIntegralNumber() || !version.canConvertToInt() || version.intValue() != SCHEMA_VERSION) {
                throw new IllegalArgumentException("Unsupported MATERIAL schemaVersion");
            }
            JsonNode shaderKey = root.get("shaderKey");
            if (!shaderKey.isTextual()) {
                throw new IllegalArgumentException("MATERIAL shaderKey must be text");
            }
            return new MaterialAsset(shaderKey.textValue(), requireFloat(root, "redMultiplier"), requireFloat(root, "greenMultiplier"), requireFloat(root, "blueMultiplier"),
                requireFloat(root, "alphaMultiplier"));
        } catch (IOException exception) {
            throw new IllegalArgumentException("Invalid MATERIAL JSON", exception);
        }

    }

    private static float requireFloat(JsonNode root, String field) {

        JsonNode node = root.get(field);
        if (!node.isNumber()) {
            throw new IllegalArgumentException("MATERIAL " + field + " must be numeric");
        }
        double value = node.doubleValue();
        if (!Double.isFinite(value) || value < 0.0 || value > 1.0) {
            throw new IllegalArgumentException("MATERIAL " + field + " must be finite and within [0,1]");
        }
        return (float) value;

    }

    private static void requireExactFields(JsonNode root) {

        if (root == null || !root.isObject()) {
            throw new IllegalArgumentException("MATERIAL root must be an object");
        }
        HashSet<String> actual = new HashSet<>();
        Iterator<String> names = root.fieldNames();
        names.forEachRemaining(actual::add);
        if (!actual.equals(FIELDS)) {
            throw new IllegalArgumentException("MATERIAL fields must equal " + FIELDS);
        }

    }
}
