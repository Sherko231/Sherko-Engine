package com.samo.engine.assets.internal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;

final class AssetManifestJson {
    static final int SCHEMA_VERSION = 1;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private AssetManifestJson() {

    }

    static String write(List<AssetCooker.AssetManifestEntry> entries) {

        ObjectNode root = MAPPER.createObjectNode();
        root.put("schemaVersion", SCHEMA_VERSION);
        ArrayNode assets = root.putArray("assets");

        for (AssetCooker.AssetManifestEntry entry : entries) {
            ObjectNode asset = assets.addObject();
            asset.put("assetId", entry.assetId().toString());
            asset.put("assetType", entry.assetType().name());
            asset.put("sourcePath", entry.sourcePath());
            asset.put("cookedPath", entry.cookedPath());
            asset.put("byteSize", entry.byteSize());
        }

        try {
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(root) + "\n";
        } catch (JsonProcessingException exception) {
            throw new AssetCookerException("Failed to serialize asset manifest", exception);
        }

    }
}
