package com.samo.engine.assets.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SourceAssetMetadataTest {
    private static final String ASSET_ID = "01234567-89ab-cdef-fedc-ba9876543210";

    @TempDir
    Path tempDir;

    @Test
    void loadsEverySchemaV1AssetTypeWithPersistedIdentity() throws IOException {

        EnumSet<AssetType> loadedTypes = EnumSet.noneOf(AssetType.class);
        for (AssetType assetType : AssetType.values()) {
            SourceAssetMetadata metadata = SourceAssetMetadata.load(writeMetadata("""
                {
                  "schemaVersion": 1,
                  "assetId": "%s",
                  "assetType": "%s"
                }
                """.formatted(ASSET_ID, assetType)));

            assertThat(metadata.schemaVersion()).isEqualTo(SourceAssetMetadata.CURRENT_SCHEMA_VERSION);
            assertThat(metadata.assetId()).isEqualTo(AssetId.parse(ASSET_ID));
            loadedTypes.add(metadata.assetType());
        }

        assertThat(loadedTypes).containsExactlyInAnyOrderElementsOf(EnumSet.allOf(AssetType.class));

    }

    @Test
    void acceptsFieldsInDifferentOrder() throws IOException {

        SourceAssetMetadata metadata = SourceAssetMetadata.load(writeMetadata("""
            {
              "assetType": "TEXTURE",
              "assetId": "%s",
              "schemaVersion": 1
            }
            """.formatted(ASSET_ID)));

        assertThat(metadata).isEqualTo(new SourceAssetMetadata(1, AssetId.parse(ASSET_ID), AssetType.TEXTURE));

    }

    @Test
    void rejectsUnsupportedVersionsWithUpgradeRequiredMessage() throws IOException {

        Path metadataPath = writeMetadata("""
            {
              "schemaVersion": 2,
              "assetId": "%s",
              "assetType": "MESH"
            }
            """.formatted(ASSET_ID));

        assertThatThrownBy(() -> SourceAssetMetadata.load(metadataPath)).isInstanceOf(SourceAssetMetadataLoadException.class).hasMessageContaining(metadataPath.toString())
            .hasMessageContaining("upgrade required");

    }

    @Test
    void rejectsMalformedMissingUnknownDuplicateAndWrongTypeFields() throws IOException {

        assertLoadFails("{", "failed to read metadata JSON");
        assertLoadFails("""
            {"schemaVersion":1,"assetId":"%s"}
            """.formatted(ASSET_ID), "missing required field root.assetType");
        assertLoadFails("""
            {"schemaVersion":1,"assetId":"%s","assetType":"MESH","extra":true}
            """.formatted(ASSET_ID), "unknown field root.extra");
        assertLoadFails("""
            {"schemaVersion":1,"assetId":"%s","assetType":"MESH","assetType":"TEXTURE"}
            """.formatted(ASSET_ID), "Duplicate field");
        assertLoadFails("""
            {"schemaVersion":"1","assetId":"%s","assetType":"MESH"}
            """.formatted(ASSET_ID), "schemaVersion must be an integer");
        assertLoadFails("""
            {"schemaVersion":1,"assetId":42,"assetType":"MESH"}
            """, "assetId must be a nonblank string");
        assertLoadFails("""
            {"schemaVersion":1,"assetId":"%s","assetType":42}
            """.formatted(ASSET_ID), "assetType must be a nonblank string");

    }

    @Test
    void rejectsNoncanonicalIdsAndUnknownAssetTypes() throws IOException {

        assertLoadFails("""
            {"schemaVersion":1,"assetId":"01234567-89AB-CDEF-FEDC-BA9876543210","assetType":"MESH"}
            """, "assetId must be canonical lowercase AssetId text");
        assertLoadFails("""
            {"schemaVersion":1,"assetId":"%s","assetType":"MODEL"}
            """.formatted(ASSET_ID), "unknown assetType: MODEL");

    }

    @Test
    void separatesProgrammerContractFailuresFromMetadataDataFailures() {

        assertThatThrownBy(() -> SourceAssetMetadata.load(null)).isInstanceOf(NullPointerException.class).hasMessageContaining("path");
        assertThatThrownBy(() -> SourceAssetMetadata.load(tempDir.resolve("missing.json"))).isInstanceOf(SourceAssetMetadataLoadException.class)
            .hasMessageContaining("missing.json").hasMessageContaining("missing or unreadable");

        assertThatThrownBy(() -> new SourceAssetMetadata(2, AssetId.parse(ASSET_ID), AssetType.MESH)).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("schemaVersion");
        assertThatThrownBy(() -> new SourceAssetMetadata(1, null, AssetType.MESH)).isInstanceOf(NullPointerException.class).hasMessageContaining("assetId");
        assertThatThrownBy(() -> new SourceAssetMetadata(1, AssetId.parse(ASSET_ID), null)).isInstanceOf(NullPointerException.class).hasMessageContaining("assetType");

    }

    private void assertLoadFails(String json, String messagePart) throws IOException {

        Path metadataPath = writeMetadata(json);

        assertThatThrownBy(() -> SourceAssetMetadata.load(metadataPath)).isInstanceOf(SourceAssetMetadataLoadException.class).hasMessageContaining(metadataPath.toString())
            .hasMessageContaining(messagePart);

    }

    private Path writeMetadata(String json) throws IOException {

        Path path = tempDir.resolve("fixture.json");
        Files.writeString(path, json, StandardCharsets.UTF_8);
        return path;

    }
}
