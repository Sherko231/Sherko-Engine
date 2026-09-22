package com.samo.engine.assets.api;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Set;

/** Parses the strict version-1 source asset metadata JSON document. */
final class SourceAssetMetadataJsonParser {
    private static final Set<String> ROOT_FIELDS = Set.of("schemaVersion", "assetId", "assetType");
    private static final ObjectMapper MAPPER = new ObjectMapper(JsonFactory.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build());

    private SourceAssetMetadataJsonParser() {

    }

    static SourceAssetMetadata parse(Path path, Reader reader) throws IOException {

        JsonNode root = MAPPER.readTree(reader);
        try {
            return parseRoot(path, root);
        } catch (SourceAssetMetadataLoadException exception) {
            throw exception;
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw failure(path, exception.getMessage(), exception);
        }

    }

    private static SourceAssetMetadata parseRoot(Path path, JsonNode root) {

        requireObject(path, root, "root");
        requireOnlyFields(path, root, ROOT_FIELDS, "root");

        int schemaVersion = requireSchemaVersion(path, root);
        String assetIdText = requireText(path, root, "assetId");
        String assetTypeText = requireText(path, root, "assetType");

        AssetId assetId;
        try {
            assetId = AssetId.parse(assetIdText);
        } catch (IllegalArgumentException exception) {
            throw failure(path, "assetId must be canonical lowercase AssetId text", exception);
        }

        AssetType assetType;
        try {
            assetType = AssetType.valueOf(assetTypeText);
        } catch (IllegalArgumentException exception) {
            throw failure(path, "unknown assetType: " + assetTypeText, exception);
        }

        return new SourceAssetMetadata(schemaVersion, assetId, assetType);

    }

    private static int requireSchemaVersion(Path path, JsonNode root) {

        JsonNode versionNode = requireField(path, root, "schemaVersion");
        if (!versionNode.isIntegralNumber() || !versionNode.canConvertToInt()) {
            throw failure(path, "schemaVersion must be an integer");
        }

        int schemaVersion = versionNode.intValue();
        if (schemaVersion != SourceAssetMetadata.CURRENT_SCHEMA_VERSION) {
            throw failure(path, "unsupported schemaVersion " + schemaVersion + "; upgrade required");
        }
        return schemaVersion;

    }

    private static String requireText(Path path, JsonNode root, String field) {

        JsonNode value = requireField(path, root, field);
        if (!value.isTextual() || value.textValue().isBlank()) {
            throw failure(path, field + " must be a nonblank string");
        }
        return value.textValue();

    }

    private static JsonNode requireField(Path path, JsonNode root, String field) {

        JsonNode value = root.get(field);
        if (value == null || value.isNull()) {
            throw failure(path, "missing required field root." + field);
        }
        return value;

    }

    private static void requireObject(Path path, JsonNode node, String context) {

        if (node == null || !node.isObject()) {
            throw failure(path, context + " must be a JSON object");
        }

    }

    private static void requireOnlyFields(Path path, JsonNode object, Set<String> allowed, String context) {

        Iterator<String> fields = object.fieldNames();
        while (fields.hasNext()) {
            String field = fields.next();
            if (!allowed.contains(field)) {
                throw failure(path, "unknown field " + context + "." + field);
            }
        }

    }

    private static SourceAssetMetadataLoadException failure(Path path, String message) {

        return new SourceAssetMetadataLoadException(path + ": " + message);

    }

    private static SourceAssetMetadataLoadException failure(Path path, String message, Throwable cause) {

        return new SourceAssetMetadataLoadException(path + ": " + message, cause);

    }
}
