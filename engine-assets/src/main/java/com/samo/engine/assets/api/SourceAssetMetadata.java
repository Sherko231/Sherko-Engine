package com.samo.engine.assets.api;

import java.nio.file.Path;
import java.util.Objects;

/** Immutable common source-asset metadata loaded from the strict version-1 JSON schema. */
public record SourceAssetMetadata(int schemaVersion, AssetId assetId, AssetType assetType) {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    public SourceAssetMetadata {

        if (schemaVersion != CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException("schemaVersion must equal " + CURRENT_SCHEMA_VERSION);
        }
        Objects.requireNonNull(assetId, "assetId");
        Objects.requireNonNull(assetType, "assetType");

    }

    /**
     * Loads and validates one strict UTF-8 source metadata document.
     *
     * @param path
     *            metadata document path
     * @return complete validated source metadata
     * @throws NullPointerException
     *             if {@code path} is null
     * @throws SourceAssetMetadataLoadException
     *             if the file cannot be read or its JSON/schema/value content is invalid
     */
    public static SourceAssetMetadata load(Path path) {

        Objects.requireNonNull(path, "path");
        return SourceAssetMetadataLoader.load(path);

    }
}
