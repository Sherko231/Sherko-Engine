package com.samo.engine.assets.internal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.samo.engine.assets.api.AssetId;
import com.samo.engine.assets.api.AssetType;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

final class RuntimeAssetManifest {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Set<String> ROOT_FIELDS = Set.of("schemaVersion", "assets");
    private static final Set<String> ENTRY_FIELDS = Set.of("assetId", "assetType", "sourcePath", "cookedPath", "byteSize");
    private final Path cacheRoot;
    private final Map<AssetId, Entry> entries;

    private RuntimeAssetManifest(Path cacheRoot, Map<AssetId, Entry> entries) {

        this.cacheRoot = cacheRoot;
        this.entries = Map.copyOf(entries);

    }

    static RuntimeAssetManifest load(Path cookedCacheDirectory) {

        Objects.requireNonNull(cookedCacheDirectory, "cookedCacheDirectory");
        Path root = cookedCacheDirectory.toAbsolutePath().normalize();
        if (Files.isSymbolicLink(root) || !Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS) || !Files.isReadable(root)) {
            throw new IllegalArgumentException(root + ": cooked cache must be a readable non-symbolic-link directory");
        }

        Path manifestPath = root.resolve("manifest.json");
        if (Files.isSymbolicLink(manifestPath) || !Files.isRegularFile(manifestPath, LinkOption.NOFOLLOW_LINKS) || !Files.isReadable(manifestPath)) {
            throw new IllegalArgumentException(manifestPath + ": manifest.json must be a readable non-symbolic-link file");
        }

        JsonNode document;
        try {
            document = MAPPER.readTree(Files.readString(manifestPath, StandardCharsets.UTF_8));
        } catch (IOException exception) {
            throw new IllegalArgumentException(manifestPath + ": failed to read manifest", exception);
        }

        requireObjectWithExactFields(document, ROOT_FIELDS, "manifest root");
        JsonNode version = document.get("schemaVersion");
        if (!version.isIntegralNumber() || version.intValue() != AssetManifestJson.SCHEMA_VERSION) {
            throw new IllegalArgumentException(manifestPath + ": unsupported manifest schemaVersion");
        }
        JsonNode assets = document.get("assets");
        if (!assets.isArray()) {
            throw new IllegalArgumentException(manifestPath + ": assets must be an array");
        }

        Map<AssetId, Entry> entries = new HashMap<>();
        for (int index = 0; index < assets.size(); index++) {
            JsonNode node = assets.get(index);
            requireObjectWithExactFields(node, ENTRY_FIELDS, "asset[" + index + "]");
            AssetId assetId = parseAssetId(node.get("assetId"), index);
            AssetType assetType = parseAssetType(node.get("assetType"), index);
            requireText(node.get("sourcePath"), "asset[" + index + "].sourcePath");
            String cookedPath = requireText(node.get("cookedPath"), "asset[" + index + "].cookedPath");
            String expectedPath = "assets/" + assetId + ".bin";
            if (!cookedPath.equals(expectedPath)) {
                throw new IllegalArgumentException("asset[" + index + "].cookedPath must equal " + expectedPath);
            }
            JsonNode size = node.get("byteSize");
            if (!size.canConvertToLong() || size.longValue() <= 0L) {
                throw new IllegalArgumentException("asset[" + index + "].byteSize must be positive");
            }
            Entry entry = new Entry(assetId, assetType, root.resolve(cookedPath.replace('/', java.io.File.separatorChar)).normalize(), size.longValue());
            if (!entry.cookedPath().startsWith(root)) {
                throw new IllegalArgumentException("asset[" + index + "].cookedPath escapes cooked cache");
            }
            if (entries.putIfAbsent(assetId, entry) != null) {
                throw new IllegalArgumentException("Duplicate assetId " + assetId);
            }
        }
        return new RuntimeAssetManifest(root, entries);

    }

    Path cacheRoot() {

        return cacheRoot;

    }

    Entry find(AssetId assetId) {

        return entries.get(assetId);

    }

    private static void requireObjectWithExactFields(JsonNode node, Set<String> expected, String label) {

        if (node == null || !node.isObject()) {
            throw new IllegalArgumentException(label + " must be an object");
        }
        java.util.HashSet<String> actual = new java.util.HashSet<>();
        Iterator<String> names = node.fieldNames();
        names.forEachRemaining(actual::add);
        if (!actual.equals(expected)) {
            throw new IllegalArgumentException(label + " fields must equal " + expected);
        }

    }

    private static AssetId parseAssetId(JsonNode node, int index) {

        String text = requireText(node, "asset[" + index + "].assetId");
        try {
            return AssetId.parse(text);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("asset[" + index + "].assetId is invalid", exception);
        }

    }

    private static AssetType parseAssetType(JsonNode node, int index) {

        String text = requireText(node, "asset[" + index + "].assetType");
        try {
            return AssetType.valueOf(text);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("asset[" + index + "].assetType is invalid", exception);
        }

    }

    private static String requireText(JsonNode node, String label) {

        if (node == null || !node.isTextual() || node.textValue().isBlank()) {
            throw new IllegalArgumentException(label + " must be non-blank text");
        }
        return node.textValue();

    }

    record Entry(AssetId assetId, AssetType assetType, Path cookedPath, long byteSize) {
    }
}
