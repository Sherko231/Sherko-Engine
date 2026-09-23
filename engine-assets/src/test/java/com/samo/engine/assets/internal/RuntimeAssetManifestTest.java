package com.samo.engine.assets.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.samo.engine.assets.api.AssetId;
import com.samo.engine.assets.api.AssetType;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RuntimeAssetManifestTest {
    private static final AssetId FIRST = AssetId.parse("01234567-89ab-cdef-fedc-ba9876543210");

    @TempDir
    Path tempDir;

    @Test
    void loadsStrictManifestAndResolvesCanonicalCookedPath() throws Exception {

        Files.createDirectories(tempDir.resolve("assets"));
        writeManifest("""
            {
              "schemaVersion": 1,
              "assets": [
                {
                  "assetId": "%s",
                  "assetType": "MESH",
                  "sourcePath": "models/hero.gltf",
                  "cookedPath": "assets/%s.bin",
                  "byteSize": 123
                }
              ]
            }
            """.formatted(FIRST, FIRST));

        RuntimeAssetManifest manifest = RuntimeAssetManifest.load(tempDir);
        RuntimeAssetManifest.Entry entry = manifest.find(FIRST);

        assertThat(entry.assetId()).isEqualTo(FIRST);
        assertThat(entry.assetType()).isEqualTo(AssetType.MESH);
        assertThat(entry.byteSize()).isEqualTo(123L);
        assertThat(entry.cookedPath()).isEqualTo(tempDir.resolve("assets/" + FIRST + ".bin").toAbsolutePath().normalize());

    }

    @Test
    void rejectsMalformedUnknownVersionDuplicateTraversalAndZeroSize() throws Exception {

        writeManifest("{");
        assertThatThrownBy(() -> RuntimeAssetManifest.load(tempDir)).isInstanceOf(IllegalArgumentException.class);

        writeManifest("""
            {"schemaVersion":2,"assets":[]}
            """);
        assertThatThrownBy(() -> RuntimeAssetManifest.load(tempDir)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("schemaVersion");

        writeManifest("""
            {
              "schemaVersion":1,
              "assets":[
                {"assetId":"%s","assetType":"MESH","sourcePath":"a","cookedPath":"assets/%s.bin","byteSize":1},
                {"assetId":"%s","assetType":"MESH","sourcePath":"b","cookedPath":"assets/%s.bin","byteSize":1}
              ]
            }
            """.formatted(FIRST, FIRST, FIRST, FIRST));
        assertThatThrownBy(() -> RuntimeAssetManifest.load(tempDir)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Duplicate");

        writeManifest("""
            {
              "schemaVersion":1,
              "assets":[
                {"assetId":"%s","assetType":"MESH","sourcePath":"a","cookedPath":"../escape.bin","byteSize":1}
              ]
            }
            """.formatted(FIRST));
        assertThatThrownBy(() -> RuntimeAssetManifest.load(tempDir)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("cookedPath");

        writeManifest("""
            {
              "schemaVersion":1,
              "assets":[
                {"assetId":"%s","assetType":"MESH","sourcePath":"a","cookedPath":"assets/%s.bin","byteSize":0}
              ]
            }
            """.formatted(FIRST, FIRST));
        assertThatThrownBy(() -> RuntimeAssetManifest.load(tempDir)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("byteSize");

    }

    @Test
    void rejectsUnknownAndMissingFields() throws Exception {

        writeManifest("""
            {"schemaVersion":1,"assets":[],"extra":true}
            """);
        assertThatThrownBy(() -> RuntimeAssetManifest.load(tempDir)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("fields");

        writeManifest("""
            {
              "schemaVersion":1,
              "assets":[
                {"assetId":"%s","assetType":"MESH","sourcePath":"a","cookedPath":"assets/%s.bin"}
              ]
            }
            """.formatted(FIRST, FIRST));
        assertThatThrownBy(() -> RuntimeAssetManifest.load(tempDir)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("fields");

    }

    private void writeManifest(String json) throws Exception {

        Files.writeString(tempDir.resolve("manifest.json"), json);

    }
}
