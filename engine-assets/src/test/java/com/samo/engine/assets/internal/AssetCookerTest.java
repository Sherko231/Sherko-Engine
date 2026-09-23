package com.samo.engine.assets.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.imageio.ImageIO;
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
        createAsset(input.resolve("z/second.bin"), SECOND_ID, "SCENE", new byte[]{9, 8, 7});
        byte[] materialBytes = validMaterialBytes();
        createAsset(input.resolve("a/first.bin"), FIRST_ID, "MATERIAL", materialBytes);

        Path output = tempDir.resolve("output");
        AssetCooker.cook(input, output);

        assertThat(Files.readAllBytes(output.resolve("assets/" + FIRST_ID + ".bin"))).isEqualTo(materialBytes);
        assertThat(Files.readAllBytes(output.resolve("assets/" + SECOND_ID + ".bin"))).containsExactly(9, 8, 7);

        JsonNode manifest = MAPPER.readTree(output.resolve("manifest.json").toFile());
        assertThat(fieldNames(manifest)).containsExactly("schemaVersion", "assets");
        assertThat(manifest.get("schemaVersion").intValue()).isEqualTo(1);
        assertThat(manifest.get("assets").size()).isEqualTo(2);

        JsonNode first = manifest.get("assets").get(0);
        assertThat(fieldNames(first)).containsExactly("assetId", "assetType", "sourcePath", "cookedPath", "byteSize");
        assertThat(first.get("assetId").textValue()).isEqualTo(FIRST_ID);
        assertThat(first.get("assetType").textValue()).isEqualTo("MATERIAL");
        assertThat(first.get("sourcePath").textValue()).isEqualTo("a/first.bin");
        assertThat(first.get("cookedPath").textValue()).isEqualTo("assets/" + FIRST_ID + ".bin");
        assertThat(first.get("byteSize").longValue()).isEqualTo(materialBytes.length);

        JsonNode second = manifest.get("assets").get(1);
        assertThat(second.get("assetId").textValue()).isEqualTo(SECOND_ID);
        assertThat(second.get("assetType").textValue()).isEqualTo("SCENE");
        assertThat(second.get("sourcePath").textValue()).isEqualTo("z/second.bin");
        assertThat(second.get("byteSize").longValue()).isEqualTo(3);

        Path secondOutput = tempDir.resolve("second-output");
        AssetCooker.cook(input, secondOutput);
        assertThat(Files.readAllBytes(secondOutput.resolve("manifest.json"))).isEqualTo(Files.readAllBytes(output.resolve("manifest.json")));

    }

    @Test
    void ignoresUnpairedOrdinarySourceFiles() throws Exception {

        Path input = Files.createDirectory(tempDir.resolve("input"));
        Files.writeString(input.resolve("ignored.txt"), "ignored", StandardCharsets.UTF_8);
        createAsset(input.resolve("kept.bin"), FIRST_ID, "MATERIAL", validMaterialBytes());

        Path output = tempDir.resolve("output");
        AssetCooker.cook(input, output);

        JsonNode manifest = MAPPER.readTree(output.resolve("manifest.json").toFile());
        assertThat(manifest.get("assets").size()).isEqualTo(1);
        assertThat(manifest.get("assets").get(0).get("sourcePath").textValue()).isEqualTo("kept.bin");

    }

    @Test
    void rejectsMissingInputExistingOutputOverlapAndEmptySetsWithoutCreatingOutput() throws Exception {

        Path missing = tempDir.resolve("missing");
        assertThatThrownBy(() -> AssetCooker.cook(missing, tempDir.resolve("missing-output"))).isInstanceOf(AssetCookerException.class).hasMessageContaining("input must be");

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
        Files.write(malformedInput.resolve("bad.bin"), new byte[]{1});
        Files.writeString(malformedInput.resolve("bad.bin.asset.json"), "{", StandardCharsets.UTF_8);
        Path malformedOutput = tempDir.resolve("malformed-output");
        assertThatThrownBy(() -> AssetCooker.cook(malformedInput, malformedOutput)).isInstanceOf(RuntimeException.class);
        assertThat(malformedOutput).doesNotExist();

        Path unsupportedInput = Files.createDirectory(tempDir.resolve("unsupported"));
        Files.write(unsupportedInput.resolve("future.bin"), new byte[]{1});
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
        createAsset(duplicateInput.resolve("one.bin"), FIRST_ID, "MATERIAL", validMaterialBytes());
        createAsset(duplicateInput.resolve("two.bin"), FIRST_ID, "PREFAB", new byte[]{2});
        Path duplicateOutput = tempDir.resolve("duplicate-output");
        assertThatThrownBy(() -> AssetCooker.cook(duplicateInput, duplicateOutput)).isInstanceOf(AssetCookerException.class).hasMessageContaining("duplicate assetId");
        assertThat(duplicateOutput).doesNotExist();

    }

    @Test
    void cooksAudioToSaudWithValidatedMetadataAndExactVorbisPayload() throws Exception {

        Path input = Files.createDirectory(tempDir.resolve("audio-input"));
        Path source = input.resolve("confirm.ogg");
        byte[] sourceBytes = VorbisTestFixtures.read("p6/mono-22050.ogg.b64");
        Files.write(source, sourceBytes);
        writeMetadata(source.resolveSibling(source.getFileName() + AssetCooker.METADATA_SUFFIX), FIRST_ID, "AUDIO");

        Path output = tempDir.resolve("audio-output");
        AssetCooker.cook(input, output);

        byte[] cooked = Files.readAllBytes(output.resolve("assets/" + FIRST_ID + ".bin"));
        assertThat(cooked).startsWith((byte) 'S', (byte) 'A', (byte) 'U', (byte) 'D');
        assertThat(cooked).isNotEqualTo(sourceBytes);

        CookedAudio audio = CookedAudioBinary.decode(cooked);
        assertThat(audio.channels()).isEqualTo(1);
        assertThat(audio.sampleRate()).isEqualTo(22050);
        assertThat(audio.vorbisPayload()).isEqualTo(sourceBytes);

        JsonNode manifest = MAPPER.readTree(output.resolve("manifest.json").toFile());
        assertThat(manifest.get("assets").get(0).get("assetType").textValue()).isEqualTo("AUDIO");
        assertThat(manifest.get("assets").get(0).get("sourcePath").textValue()).isEqualTo("confirm.ogg");
        assertThat(manifest.get("assets").get(0).get("byteSize").longValue()).isEqualTo(cooked.length);

        Path secondOutput = tempDir.resolve("audio-second-output");
        AssetCooker.cook(input, secondOutput);
        assertThat(Files.readAllBytes(secondOutput.resolve("assets/" + FIRST_ID + ".bin"))).isEqualTo(cooked);

    }

    @Test
    void rejectsUnsupportedMalformedAndMultichannelAudioBeforeCreatingOutput() throws Exception {

        Path unsupportedInput = Files.createDirectory(tempDir.resolve("unsupported-audio-input"));
        Path unsupported = unsupportedInput.resolve("audio.wav");
        Files.write(unsupported, new byte[]{1, 2, 3});
        writeMetadata(unsupported.resolveSibling(unsupported.getFileName() + AssetCooker.METADATA_SUFFIX), FIRST_ID, "AUDIO");
        Path unsupportedOutput = tempDir.resolve("unsupported-audio-output");
        assertThatThrownBy(() -> AssetCooker.cook(unsupportedInput, unsupportedOutput)).isInstanceOf(AssetCookerException.class).hasMessageContaining(unsupported.toString())
            .hasMessageContaining(".ogg");
        assertThat(unsupportedOutput).doesNotExist();

        Path malformedInput = Files.createDirectory(tempDir.resolve("malformed-audio-input"));
        Path malformed = malformedInput.resolve("broken.ogg");
        Files.write(malformed, new byte[]{'O', 'g', 'g', 'S', 1, 2, 3});
        writeMetadata(malformed.resolveSibling(malformed.getFileName() + AssetCooker.METADATA_SUFFIX), FIRST_ID, "AUDIO");
        Path malformedOutput = tempDir.resolve("malformed-audio-output");
        assertThatThrownBy(() -> AssetCooker.cook(malformedInput, malformedOutput)).isInstanceOf(AssetCookerException.class).hasMessageContaining(malformed.toString())
            .hasMessageContaining("open failed");
        assertThat(malformedOutput).doesNotExist();

        Path multichannelInput = Files.createDirectory(tempDir.resolve("multichannel-audio-input"));
        Path multichannel = multichannelInput.resolve("surround.ogg");
        Files.write(multichannel, VorbisTestFixtures.read("p6/three-channel-32000.ogg.b64"));
        writeMetadata(multichannel.resolveSibling(multichannel.getFileName() + AssetCooker.METADATA_SUFFIX), FIRST_ID, "AUDIO");
        Path multichannelOutput = tempDir.resolve("multichannel-audio-output");
        assertThatThrownBy(() -> AssetCooker.cook(multichannelInput, multichannelOutput)).isInstanceOf(AssetCookerException.class).hasMessageContaining(multichannel.toString())
            .hasMessageContaining("mono or stereo");
        assertThat(multichannelOutput).doesNotExist();

    }

    @Test
    void cleansNewOutputTreeWhenAudioBinaryWriteFails() throws Exception {

        Path input = Files.createDirectory(tempDir.resolve("audio-write-failure-input"));
        Path source = input.resolve("confirm.ogg");
        Files.write(source, VorbisTestFixtures.read("p6/mono-22050.ogg.b64"));
        writeMetadata(source.resolveSibling(source.getFileName() + AssetCooker.METADATA_SUFFIX), FIRST_ID, "AUDIO");
        Path output = tempDir.resolve("audio-write-failure-output");

        AssetCookerFileSystem failing = new DelegatingFileSystem() {
            @Override
            public void writeBytes(Path path, byte[] bytes) throws IOException {

                throw new IOException("intentional audio binary failure");

            }
        };

        assertThatThrownBy(() -> AssetCooker.cook(input, output, failing)).isInstanceOf(AssetCookerException.class).hasMessageContaining("intentional audio binary failure");
        assertThat(output).doesNotExist();

    }

    @Test
    void cooksTextureToStexMipChainAndRecordsActualByteSize() throws Exception {

        Path input = Files.createDirectory(tempDir.resolve("texture-input"));
        Path source = input.resolve("albedo.png");
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, 0xFF0A141E);
        image.setRGB(1, 0, 0xFF28323C);
        image.setRGB(0, 1, 0xFF46505A);
        image.setRGB(1, 1, 0xFF646E78);
        assertThat(ImageIO.write(image, "png", source.toFile())).isTrue();
        writeMetadata(source.resolveSibling(source.getFileName() + AssetCooker.METADATA_SUFFIX), FIRST_ID, "TEXTURE");

        Path output = tempDir.resolve("texture-output");
        AssetCooker.cook(input, output);

        byte[] cooked = Files.readAllBytes(output.resolve("assets/" + FIRST_ID + ".bin"));
        assertThat(cooked).startsWith((byte) 'S', (byte) 'T', (byte) 'E', (byte) 'X');
        assertThat(cooked).isNotEqualTo(Files.readAllBytes(source));

        CookedTexture texture = CookedTextureBinary.decode(cooked);
        assertThat(texture.width()).isEqualTo(2);
        assertThat(texture.height()).isEqualTo(2);
        assertThat(texture.mipLevels()).hasSize(2);
        assertThat(texture.mipLevels().get(1).rgba8()).containsExactly((byte) 55, (byte) 65, (byte) 75, (byte) 255);

        JsonNode manifest = MAPPER.readTree(output.resolve("manifest.json").toFile());
        assertThat(manifest.get("assets").get(0).get("assetType").textValue()).isEqualTo("TEXTURE");
        assertThat(manifest.get("assets").get(0).get("sourcePath").textValue()).isEqualTo("albedo.png");
        assertThat(manifest.get("assets").get(0).get("byteSize").longValue()).isEqualTo(cooked.length);

    }

    @Test
    void rejectsUnsupportedOrMalformedTextureBeforeCreatingOutput() throws Exception {

        Path unsupportedInput = Files.createDirectory(tempDir.resolve("unsupported-texture-input"));
        Path unsupported = unsupportedInput.resolve("texture.bin");
        Files.write(unsupported, new byte[]{1});
        writeMetadata(unsupported.resolveSibling(unsupported.getFileName() + AssetCooker.METADATA_SUFFIX), FIRST_ID, "TEXTURE");
        Path unsupportedOutput = tempDir.resolve("unsupported-texture-output");
        assertThatThrownBy(() -> AssetCooker.cook(unsupportedInput, unsupportedOutput)).isInstanceOf(AssetCookerException.class).hasMessageContaining(unsupported.toString())
            .hasMessageContaining(".png");
        assertThat(unsupportedOutput).doesNotExist();

        Path malformedInput = Files.createDirectory(tempDir.resolve("malformed-texture-input"));
        Path malformed = malformedInput.resolve("broken.png");
        Files.write(malformed, new byte[]{1, 2, 3});
        writeMetadata(malformed.resolveSibling(malformed.getFileName() + AssetCooker.METADATA_SUFFIX), FIRST_ID, "TEXTURE");
        Path malformedOutput = tempDir.resolve("malformed-texture-output");
        assertThatThrownBy(() -> AssetCooker.cook(malformedInput, malformedOutput)).isInstanceOf(AssetCookerException.class).hasMessageContaining(malformed.toString())
            .hasMessageContaining("decode failed");
        assertThat(malformedOutput).doesNotExist();

    }

    @Test
    void cooksMeshBinaryAndRecordsActualByteSize() throws Exception {

        Path input = Files.createDirectory(tempDir.resolve("mesh-input"));
        Path source = input.resolve("reference.gltf");
        Files.copy(resourcePath("p6/reference-triangle.gltf"), source);
        writeMetadata(source.resolveSibling("reference.gltf" + AssetCooker.METADATA_SUFFIX), FIRST_ID, "MESH");

        Path output = tempDir.resolve("mesh-output");
        AssetCooker.cook(input, output);

        byte[] cooked = Files.readAllBytes(output.resolve("assets/" + FIRST_ID + ".bin"));
        assertThat(cooked).startsWith((byte) 'S', (byte) 'M', (byte) 'E', (byte) 'S');
        assertThat(cooked).isNotEqualTo(Files.readAllBytes(source));

        List<EngineMesh> decoded = CookedMeshBinary.decode(cooked);
        assertThat(decoded).hasSize(1);
        EngineMesh mesh = decoded.getFirst();
        assertThat(mesh.name()).isEqualTo("ReferenceTriangle");
        assertThat(mesh.positions()).containsExactly(-7.0f, -8.0f, -9.0f, -1.0f, 2.0f, -3.0f, 4.0f, 5.0f, -6.0f);
        assertThat(mesh.indices()).containsExactly(0, 1, 2);

        JsonNode manifest = MAPPER.readTree(output.resolve("manifest.json").toFile());
        assertThat(manifest.get("assets").get(0).get("assetType").textValue()).isEqualTo("MESH");
        assertThat(manifest.get("assets").get(0).get("sourcePath").textValue()).isEqualTo("reference.gltf");
        assertThat(manifest.get("assets").get(0).get("byteSize").longValue()).isEqualTo(cooked.length);

    }

    @Test
    void rejectsNormalMappedMeshMissingUvBeforeCreatingOutputWithPathAndMeshName() throws Exception {

        Path input = Files.createDirectory(tempDir.resolve("missing-normal-map-uv-input"));
        Path source = input.resolve("normal-mapped-missing-uv.gltf");
        Files.copy(resourcePath("p6/normal-mapped-missing-uv.gltf"), source);
        writeMetadata(source.resolveSibling(source.getFileName() + AssetCooker.METADATA_SUFFIX), FIRST_ID, "MESH");

        Path output = tempDir.resolve("missing-normal-map-uv-output");
        assertThatThrownBy(() -> AssetCooker.cook(input, output)).isInstanceOf(AssetCookerException.class).hasMessageContaining(source.toString())
            .hasMessageContaining("mesh[0] 'MissingUvTriangle'").hasMessageContaining("requires UV0");
        assertThat(output).doesNotExist();

    }

    @Test
    void rejectsUnsupportedMeshExtensionBeforeCreatingOutput() throws Exception {

        Path input = Files.createDirectory(tempDir.resolve("unsupported-mesh-input"));
        Path source = input.resolve("mesh.obj");
        Files.write(source, new byte[]{1, 2, 3});
        writeMetadata(source.resolveSibling("mesh.obj" + AssetCooker.METADATA_SUFFIX), FIRST_ID, "MESH");

        Path output = tempDir.resolve("unsupported-mesh-output");
        assertThatThrownBy(() -> AssetCooker.cook(input, output)).isInstanceOf(AssetCookerException.class).hasMessageContaining(source.toString())
            .hasMessageContaining(".gltf or .glb");
        assertThat(output).doesNotExist();

    }

    @Test
    void cleansNewOutputTreeWhenMeshBinaryWriteFails() throws Exception {

        Path input = Files.createDirectory(tempDir.resolve("mesh-write-failure-input"));
        Path source = input.resolve("reference.gltf");
        Files.copy(resourcePath("p6/reference-triangle.gltf"), source);
        writeMetadata(source.resolveSibling(source.getFileName() + AssetCooker.METADATA_SUFFIX), FIRST_ID, "MESH");
        Path output = tempDir.resolve("mesh-write-failure-output");

        AssetCookerFileSystem failing = new DelegatingFileSystem() {
            @Override
            public void writeBytes(Path path, byte[] bytes) throws IOException {

                throw new IOException("intentional mesh binary failure");

            }
        };

        assertThatThrownBy(() -> AssetCooker.cook(input, output, failing)).isInstanceOf(AssetCookerException.class).hasMessageContaining("intentional mesh binary failure");
        assertThat(output).doesNotExist();

    }

    @Test
    void cleansNewOutputTreeWhenWriteStageFails() throws Exception {

        Path input = Files.createDirectory(tempDir.resolve("input"));
        createAsset(input.resolve("source.bin"), FIRST_ID, "MATERIAL", validMaterialBytes());
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
    void writesDependencyGraphAndRejectsOrphanSidecars() throws Exception {

        Path input = Files.createDirectory(tempDir.resolve("dependency-input"));
        Path material = input.resolve("material.bin");
        createAsset(material, FIRST_ID, "MATERIAL", validMaterialBytes());
        writeDependencies(material.resolveSibling(material.getFileName() + AssetCooker.DEPENDENCIES_SUFFIX), List.of(), List.of("opaque-baseline"));

        Path prefab = input.resolve("prefab.bin");
        createAsset(prefab, SECOND_ID, "PREFAB", new byte[]{2});
        writeDependencies(prefab.resolveSibling(prefab.getFileName() + AssetCooker.DEPENDENCIES_SUFFIX), List.of(FIRST_ID), List.of());

        Path output = tempDir.resolve("dependency-output");
        AssetCooker.cook(input, output);

        JsonNode dependencies = MAPPER.readTree(output.resolve("dependencies.json").toFile());
        assertThat(fieldNames(dependencies)).containsExactly("schemaVersion", "assets");
        assertThat(dependencies.get("schemaVersion").intValue()).isEqualTo(1);
        assertThat(dependencies.get("assets").size()).isEqualTo(2);
        assertThat(dependencies.get("assets").get(0).get("assetId").textValue()).isEqualTo(FIRST_ID);
        assertThat(dependencies.get("assets").get(0).get("shaderDependencies").get(0).textValue()).isEqualTo("opaque-baseline");
        assertThat(dependencies.get("assets").get(1).get("assetDependencies").get(0).textValue()).isEqualTo(FIRST_ID);

        Path orphanInput = Files.createDirectory(tempDir.resolve("orphan-input"));
        createAsset(orphanInput.resolve("valid.bin"), FIRST_ID, "MATERIAL", validMaterialBytes());
        Files.writeString(orphanInput.resolve("ghost.bin" + AssetCooker.DEPENDENCIES_SUFFIX), """
            {"schemaVersion":1,"assetDependencies":[],"shaderDependencies":[]}
            """);
        Path orphanOutput = tempDir.resolve("orphan-output");
        assertThatThrownBy(() -> AssetCooker.cook(orphanInput, orphanOutput)).isInstanceOf(AssetCookerException.class).hasMessageContaining("orphan dependency sidecar");
        assertThat(orphanOutput).doesNotExist();

    }

    @Test
    void rejectsDependencySidecarForUnsupportedOwnerTypeBeforeOutputCreation() throws Exception {

        Path input = Files.createDirectory(tempDir.resolve("unsupported-dependency-owner"));
        Path audio = input.resolve("audio.bin");
        createAsset(audio, FIRST_ID, "SKELETON", new byte[]{1});
        writeDependencies(audio.resolveSibling(audio.getFileName() + AssetCooker.DEPENDENCIES_SUFFIX), List.of(), List.of());

        Path output = tempDir.resolve("unsupported-dependency-output");
        assertThatThrownBy(() -> AssetCooker.cook(input, output)).isInstanceOf(AssetCookerException.class).hasMessageContaining("only for MATERIAL, PREFAB, or SCENE");
        assertThat(output).doesNotExist();

    }

    @Test
    void cleansNewOutputTreeWhenDependencyGraphWriteFails() throws Exception {

        Path input = Files.createDirectory(tempDir.resolve("dependency-write-failure-input"));
        createAsset(input.resolve("material.bin"), FIRST_ID, "MATERIAL", validMaterialBytes());
        Path output = tempDir.resolve("dependency-write-failure-output");

        AssetCookerFileSystem failing = new DelegatingFileSystem() {
            @Override
            public void writeString(Path path, String content) throws IOException {

                if (path.getFileName().toString().equals("dependencies.json")) {
                    throw new IOException("intentional dependency graph failure");
                }
                super.writeString(path, content);

            }
        };

        assertThatThrownBy(() -> AssetCooker.cook(input, output, failing)).isInstanceOf(AssetCookerException.class).hasMessageContaining("intentional dependency graph failure");
        assertThat(output).doesNotExist();

    }

    @Test
    void cliRequiresExactlyTwoArguments() {

        assertThatThrownBy(() -> AssetCookerMain.main(new String[0])).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("exactly two arguments");
        assertThatThrownBy(() -> AssetCookerMain.main(new String[]{"one"})).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("exactly two arguments");
        assertThatThrownBy(() -> AssetCookerMain.main(new String[]{"one", "two", "three"})).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("exactly two arguments");

    }

    private static Path resourcePath(String name) {

        URL resource = Objects.requireNonNull(AssetCookerTest.class.getClassLoader().getResource(name), "Missing test resource " + name);
        try {
            return Path.of(resource.toURI());
        } catch (URISyntaxException exception) {
            throw new IllegalStateException("Invalid test resource URI for " + name, exception);
        }

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

    private void writeDependencies(Path path, List<String> assetDependencies, List<String> shaderDependencies) throws IOException {

        String assets = assetDependencies.stream().map(value -> "\"" + value + "\"").reduce((left, right) -> left + "," + right).orElse("");
        String shaders = shaderDependencies.stream().map(value -> "\"" + value + "\"").reduce((left, right) -> left + "," + right).orElse("");
        Files.writeString(path, """
            {
              "schemaVersion": 1,
              "assetDependencies": [%s],
              "shaderDependencies": [%s]
            }
            """.formatted(assets, shaders), StandardCharsets.UTF_8);

    }

    private static byte[] validMaterialBytes() {

        return """
            {
              "schemaVersion": 1,
              "shaderKey": "opaque-baseline",
              "redMultiplier": 1.0,
              "greenMultiplier": 1.0,
              "blueMultiplier": 1.0,
              "alphaMultiplier": 1.0
            }
            """.getBytes(StandardCharsets.UTF_8);

    }

    private static List<String> fieldNames(JsonNode node) {

        ArrayList<String> names = new ArrayList<>();
        node.fieldNames().forEachRemaining(names::add);
        return List.copyOf(names);

    }

    private static class DelegatingFileSystem implements AssetCookerFileSystem {
        private final AssetCookerFileSystem delegate = AssetCookerFileSystem.system();

        @Override
        public void createDirectory(Path path) throws IOException {

            delegate.createDirectory(path);

        }

        @Override
        public void createDirectories(Path path) throws IOException {

            delegate.createDirectories(path);

        }

        @Override
        public void copy(Path source, Path target) throws IOException {

            delegate.copy(source, target);

        }

        @Override
        public void writeBytes(Path path, byte[] bytes) throws IOException {

            delegate.writeBytes(path, bytes);

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
