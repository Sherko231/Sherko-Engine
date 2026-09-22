package com.samo.engine.assets.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AssetCookerTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String FIRST_ID = "01234567-89ab-cdef-fedc-ba9876543210";
    private static final String SECOND_ID = "11234567-89ab-cdef-fedc-ba9876543210";

    @TempDir
    Path tempDir;

    @Test
    void cooksDeterministicManifestAndOpaqueNonzeroPayloads() throws Exception {

        Path input = Files.createDirectory(tempDir.resolve("input"));
        createAsset(input.resolve("z/second.bin"), SECOND_ID, "AUDIO", new byte[] {9, 8, 7});
        createAsset(input.resolve("a/first.bin"), FIRST_ID, "MESH", new byte[] {1, 2, 3, 4});

        Path output = tempDir.resolve("output");
        AssetCooker.cook(input, output);

        assertThat(Files.readAllBytes(output.resolve("assets/" + FIRST_ID + ".bin"))).containsExactly(1, 2, 3, 4);
        assertThat(Files.readAllBytes(output.resolve("assets/" + SECOND_ID + ".bin"))).containsExactly(9, 8, 7);

        JsonNode manifest = MAPPER.readTree(output.resolve("manifest.json").toFile());
        assertThat(fieldNames(manifest)).containsExactly("schemaVersion", "assets");
        assertThat(manifest.get("schemaVersion").intValue()).isEqualTo(1);
        assertThat(manifest.get("assets")).hasSize(2);

        JsonNode first = manifest.get("assets").get(0);
        assertThat(fieldNames(first)).containsExactly("assetId", "assetType", "sourcePath", "cookedPath", "byteSize");
        assertThat(first.get("assetId").textValue()).isEqualTo(FIRST_ID);
        assertThat(first.get("assetType").textValue()).isEqualTo("MESH");
        assertThat(first.get("sourcePath").textValue()).isEqualTo("a/first.bin");
        assertThat(first.get("cookedPath").textValue()).isEqualTo("assets/" + FIRST_ID + ".bin");
        assertThat(first.get("byteSize").longValue()).isEqualTo(4);

        JsonNode second = manifest.get("assets").get(1);
        assertThat(second.get("assetId").textValue()).isEqualTo(SECOND_ID);
        assertThat(second.get("assetType").textValue()).isEqualTo("AUDIO");
        assertThat(second.get("sourcePath").textValue()).isEqualTo("z/second.bin");
        assertThat(second.get("byteSize").longValue()).isEqualTo(3);

    }

    @Test
    void ignoresUnpairedOrdinarySourceFiles() throws Exception {

        Path input = Files.createDirectory(tempDir.resolve("input"));
        Files.writeString(input.resolve("ignored.txt"), "ignored", StandardCharsets.UTF_8);
        createAsset(input.resolve("kept.bin"), FIRST_ID, "TEXTURE", new byte[] {5});

        Path output = tempDir.resolve("output");
        AssetCooker.cook(input, output);

        JsonNode manifest = MAPPER.readTree(output.resolve("manifest.json").toFile());
        assertThat(manifest.get("assets")).hasSize(1);
        assertThat(manifest.get("assets").get(0).get("sourcePath").textValue()).isEqualTo("kept.bin");

    }

    @Test
    void rejectsMissingInputExistingOutputOverlapAndEmptySetsWithoutCreatingOutput() throws Exception {

        Path missing = tempDir.resolve("missing");
        assertThatThrownBy(() -> AssetCooker.cook(missing, tempDir.resolve("missing-output"))).isInstanceOf(AssetCookerException.class)
            .hasMessageContaining("input must be");

        Path input = Files.createDirectory(tempDir.resolve("input"));
        Path existingOutput = Files.createDirectory(tempDir.resolve("existing-output"));
        Files.writeString(existingOutput.resolve("keep.txt"), "keep", StandardCharsets.UTF_8);
        assertThatThrownBy(() -> AssetCooker.cook(input, existingOutput)).isInstanceOf(AssetCookerException.class).hasMessageContaining("already exists");
        assertThat(Files.readString(existingOutput.resolve("keep.txt"))).isEqualTo("keep");

        assertThatThrownBy(() -> AssetCooker.cook(input, input.resolve("nested-output"))).isInstanceOf(AssetCookerException.class).hasMessageContaining("overlap");

        Path outsideOutput = tempDir.resolve("empty-output");
        assertThatThrownBy(() -> AssetCooker.cook(input, outsideOutput)).isInstanceOf(AssetCookerException.class).hasMessageContaining("no .asset.json");
        assertThat(outsideOutput).doesNotExist();

    }

    @Test
    void rejectsMissingZeroByteMalformedMetadataAndDuplicateIdentityBeforeCreatingOutput() throws Exception {

        Path missingInput = Files.createDirectory(tempDir.resolve("missing-pair"));
        writeMetadata(missingInput.resolve("ghost.bin.asset.json"), FIRST_ID, "MESH");
        Path missingOutput = tempDir.resolve("missing-pair-output");
        assertThatThrownBy(() -> AssetCooker.cook(missingInput, missingOutput)).isInstanceOf(AssetCookerException.class).hasMessageContaining("paired source file");
        assertThat(missingOutput).doesNotExist();

        Path zeroInput = Files.createDirectory(tempDir.resolve("zero"));
        Files.createFile(zeroInput.resolve("zero.bin"));
        writeMetadata(zeroInput.resolve("zero.bin.asset.json"), FIRST_ID, "AUDIO");
        Path zeroOutput = tempDir.resolve("zero-output");
        assertThatThrownBy(() -> AssetCooker.cook(zeroInput, zeroOutput)).isInstanceOf(AssetCookerException.class).hasMessageContaining("nonzero");
        assertThat(zeroOutput).doesNotExist();

        Path malformedInput = Files.createDirectory(tempDir.resolve("malformed"));
        Files.write(malformedInput.resolve("bad.bin"), new byte[] {1});
        Files.writeString(malformedInput.resolve("bad.bin.asset.json"), "{", StandardCharsets.UTF_8);
        Path malformedOutput = tempDir.resolve("malformed-output");
        assertThatThrownBy(() -> AssetCooker.cook(malformedInput, malformedOutput)).isInstanceOf(RuntimeException.class);
        assertThat(malformedOutput).doesNotExist();

        Path unsupportedInput = Files.createDirectory(tempDir.resolve("unsupported"));
        Files.write(unsupportedInput.resolve("future.bin"), new byte[] {1});
        Files.writeString(unsupportedInput.resolve("future.bin.asset.json"), """
            {
              "schemaVersion": 2,
              "assetId": "%s",
              "assetType": "MESH"
            }
            """.formatted(FIRST_ID), StandardCharsets.UTF_8);
        Path unsupportedOutput = tempDir.resolve("unsupported-output");
        assertThatThrownBy(() -> AssetCooker.cook(unsupportedInput, unsupportedOutput)).isInstanceOf(RuntimeException.class).hasMessageContaining("upgrade required");
        assertThat(unsupportedOutput).doesNotExist();

        Path duplicateInput = Files.createDirectory(tempDir.resolve("duplicate"));
        createAsset(duplicateInput.resolve("one.bin"), FIRST_ID, "MESH", new byte[] {1});
        createAsset(duplicateInput.resolve("two.bin"), FIRST_ID, "TEXTURE", new byte[] {2});
        Path duplicateOutput = tempDir.resolve("duplicate-output");
        assertThatThrownBy(() -> AssetCooker.cook(duplicateInput, duplicateOutput)).isInstanceOf(AssetCookerException.class).hasMessageContaining("duplicate assetId");
        assertThat(duplicateOutput).doesNotExist();

    }

    @Test
    void cleansNewOutputTreeWhenWriteStageFails() throws Exception {

        Path input = Files.createDirectory(tempDir.resolve("input"));
        createAsset(input.resolve("source.bin"), FIRST_ID, "MATERIAL", new byte[] {1, 2});
        Path output = tempDir.resolve("output");

        AssetCookerFileSystem failing = new DelegatingFileSystem() {
            @Override
            public void writeString(Path path, String content) throws IOException {

                throw new IOException("intentional manifest failure");

            }
        };

        assertThatThrownBy(() -> AssetCooker.cook(input, output, failing)).isInstanceOf(AssetCookerException.class).hasMessageContaining("intentional manifest failure");
        assertThat(output).doesNotExist();

    }

    @Test
    void cliRequiresExactlyTwoArguments() {

        assertThatThrownBy(() -> AssetCookerMain.main(new String[0])).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("exactly two arguments");
        assertThatThrownBy(() -> AssetCookerMain.main(new String[] {"one"})).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("exactly two arguments");
        assertThatThrownBy(() -> AssetCookerMain.main(new String[] {"one", "two", "three"})).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("exactly two arguments");

    }

    private void createAsset(Path source, String assetId, String assetType, byte[] bytes) throws IOException {

        Files.createDirectories(source.getParent());
        Files.write(source, bytes);
        writeMetadata(source.resolveSibling(source.getFileName() + AssetCooker.METADATA_SUFFIX), assetId, assetType);

    }

    private void writeMetadata(Path metadata, String assetId, String assetType) throws IOException {

        Files.writeString(metadata, """
            {
              "schemaVersion": 1,
              "assetId": "%s",
              "assetType": "%s"
            }
            """.formatted(assetId, assetType), StandardCharsets.UTF_8);

    }

    private static java.util.List<String> fieldNames(JsonNode node) {

        java.util.ArrayList<String> names = new java.util.ArrayList<>();
        node.fieldNames().forEachRemaining(names::add);
        return java.util.List.copyOf(names);

    }

    private static class DelegatingFileSystem implements AssetCookerFileSystem {
        private final AssetCookerFileSystem delegate = AssetCookerFileSystem.system();

        @Override
        public void createDirectories(Path path) throws IOException {

            delegate.createDirectories(path);

        }

        @Override
        public void copy(Path source, Path target) throws IOException {

            delegate.copy(source, target);

        }

        @Override
        public long size(Path path) throws IOException {

            return delegate.size(path);

        }

        @Override
        public void writeString(Path path, String content) throws IOException {

            delegate.writeString(path, content);

        }

        @Override
        public void deleteTree(Path root) throws IOException {

            delegate.deleteTree(root);

        }
    }
}
