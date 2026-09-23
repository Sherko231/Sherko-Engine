package com.samo.engine.assets.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.samo.engine.assets.api.AssetId;
import com.samo.engine.assets.api.AssetLoader;
import com.samo.engine.assets.api.AssetLoaders;
import com.samo.engine.assets.api.MaterialAsset;
import com.samo.engine.assets.api.MeshAsset;
import com.samo.engine.assets.api.ResourceHandle;
import com.samo.engine.assets.api.ResourceHandleState;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Comparator;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class Phase6CookedRuntimeIntegrationTest {
    private static final AssetId MESH_ID = AssetId.parse("01234567-89ab-cdef-fedc-ba9876543210");
    private static final AssetId MATERIAL_ID = AssetId.parse("11234567-89ab-cdef-fedc-ba9876543210");

    @TempDir
    Path tempDir;

    @Test
    void runtimeLoadsOnlyFromCookedDirectoryAndManifestAfterSourceTreeIsRemoved() throws Exception {

        Path sourceRoot = Files.createDirectory(tempDir.resolve("authoring"));
        Path meshSource = sourceRoot.resolve("room.gltf");
        Files.copy(resourcePath("p6/reference-triangle.gltf"), meshSource);
        writeMetadata(meshSource, MESH_ID, "MESH");

        Path materialSource = sourceRoot.resolve("room.material.json");
        Files.writeString(materialSource, """
            {
              "schemaVersion": 1,
              "shaderKey": "opaque-baseline",
              "redMultiplier": 0.25,
              "greenMultiplier": 0.5,
              "blueMultiplier": 0.75,
              "alphaMultiplier": 1.0
            }
            """, StandardCharsets.UTF_8);
        writeMetadata(materialSource, MATERIAL_ID, "MATERIAL");

        Path cookedRoot = tempDir.resolve("cooked");
        AssetCooker.cook(sourceRoot, cookedRoot);

        assertThat(cookedRoot.resolve("manifest.json")).isRegularFile();
        assertThat(cookedRoot.resolve("assets").resolve(MESH_ID + ".bin")).isRegularFile();
        assertThat(cookedRoot.resolve("assets").resolve(MATERIAL_ID + ".bin")).isRegularFile();

        deleteTree(sourceRoot);
        assertThat(sourceRoot).doesNotExist();

        ResourceHandle<MeshAsset> meshHandle = null;
        ResourceHandle<MaterialAsset> materialHandle = null;
        try (AssetLoader loader = AssetLoaders.open(cookedRoot)) {
            meshHandle = loader.loadMesh(MESH_ID);
            materialHandle = loader.loadMaterial(MATERIAL_ID);

            awaitReady(meshHandle);
            awaitReady(materialHandle);

            assertThat(meshHandle.assetId()).isEqualTo(MESH_ID);
            assertThat(materialHandle.assetId()).isEqualTo(MATERIAL_ID);
            assertThat(meshHandle.state()).isEqualTo(ResourceHandleState.READY);
            assertThat(materialHandle.state()).isEqualTo(ResourceHandleState.READY);

            MeshAsset.Primitive primitive = meshHandle.requireReady().primitives().getFirst();
            assertThat(primitive.name()).isEqualTo("ReferenceTriangle");
            assertThat(primitive.positions()).containsExactly(-7.0f, -8.0f, -9.0f, -1.0f, 2.0f, -3.0f, 4.0f, 5.0f, -6.0f);
            assertThat(primitive.indices()).containsExactly(0, 1, 2);

            MaterialAsset material = materialHandle.requireReady();
            assertThat(material.shaderKey()).isEqualTo("opaque-baseline");
            assertThat(material.redMultiplier()).isEqualTo(0.25f);
            assertThat(material.greenMultiplier()).isEqualTo(0.5f);
            assertThat(material.blueMultiplier()).isEqualTo(0.75f);
            assertThat(material.alphaMultiplier()).isEqualTo(1.0f);

            assertThat(loader.drainErrors()).isEmpty();

            writeEvidence(cookedRoot, meshHandle, materialHandle, 0);
        } finally {
            if (meshHandle != null) {
                meshHandle.close();
            }
            if (materialHandle != null) {
                materialHandle.close();
            }
        }

    }

    private static void awaitReady(ResourceHandle<?> handle) throws InterruptedException {

        long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
        while (handle.state() == ResourceHandleState.LOADING && System.nanoTime() < deadline) {
            Thread.sleep(10L);
        }
        assertThat(handle.state()).isEqualTo(ResourceHandleState.READY);

    }

    private static void writeMetadata(Path source, AssetId assetId, String assetType) throws IOException {

        Files.writeString(source.resolveSibling(source.getFileName() + AssetCooker.METADATA_SUFFIX), """
            {
              "schemaVersion": 1,
              "assetId": "%s",
              "assetType": "%s"
            }
            """.formatted(assetId, assetType), StandardCharsets.UTF_8);

    }

    private static Path resourcePath(String name) {

        URL resource = Objects.requireNonNull(Phase6CookedRuntimeIntegrationTest.class.getClassLoader().getResource(name), "Missing test resource " + name);
        try {
            return Path.of(resource.toURI());
        } catch (URISyntaxException exception) {
            throw new IllegalStateException("Invalid test resource URI for " + name, exception);
        }

    }

    private static void deleteTree(Path root) throws IOException {

        try (var paths = Files.walk(root)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(path);
            }
        }

    }

    private static void writeEvidence(Path cookedRoot, ResourceHandle<MeshAsset> meshHandle, ResourceHandle<MaterialAsset> materialHandle, int runtimeErrorCount) throws IOException {

        Path report = Path.of("build", "reports", "p6", "p6-exit-cooked-runtime.txt");
        Files.createDirectories(report.getParent());
        Files.write(report, java.util.List.of(
            "gate=phase6-cooked-runtime",
            "result=PASS",
            "engine.commit=" + environmentOr("GITHUB_SHA", "local"),
            "java.version=" + System.getProperty("java.version"),
            "os.name=" + System.getProperty("os.name"),
            "source.tree.removed.before.runtime.open=true",
            "manifest.present=" + Files.isRegularFile(cookedRoot.resolve("manifest.json")),
            "runtime.input=cooked-cache-root-only",
            "mesh.asset.id=" + MESH_ID,
            "mesh.handle.state=" + meshHandle.state(),
            "material.asset.id=" + MATERIAL_ID,
            "material.handle.state=" + materialHandle.state(),
            "runtime.error.count=" + runtimeErrorCount),
            StandardCharsets.UTF_8);

    }

    private static String environmentOr(String name, String fallback) {

        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;

    }
}
