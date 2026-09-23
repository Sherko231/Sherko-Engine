package com.samo.game.sandbox;

import com.samo.engine.assets.api.AssetId;
import com.samo.engine.assets.api.AssetLoadError;
import com.samo.engine.assets.api.AssetLoadErrorCode;
import com.samo.engine.assets.api.AssetLoader;
import com.samo.engine.assets.api.AssetLoaders;
import com.samo.engine.assets.api.MaterialAsset;
import com.samo.engine.assets.api.MeshAsset;
import com.samo.engine.assets.api.ResourceHandle;
import com.samo.engine.assets.api.ResourceHandleState;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

final class SandboxAssetLab implements AutoCloseable {
    static final AssetId MESH_ID = AssetId.parse("01234567-89ab-cdef-fedc-ba9876543210");
    static final AssetId MATERIAL_ID = AssetId.parse("11234567-89ab-cdef-fedc-ba9876543210");
    static final AssetId MISSING_MESH_ID = AssetId.parse("99999999-9999-4999-8999-999999999999");

    private static final String WARM_MATERIAL =
        "{\"schemaVersion\":1,\"shaderKey\":\"sandbox-lit\",\"redMultiplier\":1.0,\"greenMultiplier\":0.35,\"blueMultiplier\":0.15,\"alphaMultiplier\":1.0}";
    private static final String COOL_MATERIAL =
        "{\"schemaVersion\":1,\"shaderKey\":\"sandbox-lit\",\"redMultiplier\":0.15,\"greenMultiplier\":0.45,\"blueMultiplier\":1.0,\"alphaMultiplier\":1.0}";
    private static final String INVALID_MATERIAL = "{\"schemaVersion\":1,\"shaderKey\":\"BROKEN\",\"redMultiplier\":2.0}";

    private final Consumer<String> logSink;
    private final Path cacheRoot;
    private final Path materialPayload;
    private final AssetLoader loader;
    private ResourceHandle<MeshAsset> meshHandle;
    private ResourceHandle<MaterialAsset> materialHandle;
    private ResourceHandleState lastMeshState;
    private ResourceHandleState lastMaterialState;
    private boolean coolPreset;

    private SandboxAssetLab(Consumer<String> logSink, Path cacheRoot, AssetLoader loader) {

        this.logSink = Objects.requireNonNull(logSink, "logSink");
        this.cacheRoot = Objects.requireNonNull(cacheRoot, "cacheRoot");
        materialPayload = cacheRoot.resolve("assets").resolve(MATERIAL_ID + ".bin");
        this.loader = Objects.requireNonNull(loader, "loader");
        startLoads();

    }

    static SandboxAssetLab open(Consumer<String> logSink) {

        Path cacheRoot = materializeFixture();
        try {
            return new SandboxAssetLab(logSink, cacheRoot, AssetLoaders.open(cacheRoot));
        } catch (RuntimeException failure) {
            deleteTreeBestEffort(cacheRoot);
            throw failure;
        }

    }

    void update() {

        ResourceHandleState meshState = meshHandle.state();
        ResourceHandleState materialState = materialHandle.state();
        if (meshState != lastMeshState) {
            logSink.accept("Phase 6 MESH " + MESH_ID + " -> " + meshState);
            lastMeshState = meshState;
        }
        if (materialState != lastMaterialState) {
            logSink.accept("Phase 6 MATERIAL " + MATERIAL_ID + " -> " + materialState);
            lastMaterialState = materialState;
        }
        if (meshState == ResourceHandleState.READY && materialState == ResourceHandleState.READY && (lastMeshState == meshState && lastMaterialState == materialState)) {
            // State-transition logging above is intentionally the only recurring output.
        }
        drainErrors("runtime");
    }

    MaterialAsset currentMaterial() {

        return materialHandle.readyValue().orElse(new MaterialAsset("sandbox-lit", 1.0f, 0.35f, 0.15f, 1.0f));

    }

    void cycleValidMaterial() {

        requireMaterialReady();
        coolPreset = !coolPreset;
        writeMaterial(coolPreset ? COOL_MATERIAL : WARM_MATERIAL);
        loader.pollDevelopmentReloads();
        MaterialAsset material = materialHandle.requireReady();
        logSink.accept("Phase 6 valid MATERIAL hot reload -> " + (coolPreset ? "COOL" : "WARM") + " " + summarize(material));
        drainErrors("hot-reload");

    }

    void demonstrateFailedReload() {

        requireMaterialReady();
        MaterialAsset before = materialHandle.requireReady();
        String restore = coolPreset ? COOL_MATERIAL : WARM_MATERIAL;
        writeMaterial(INVALID_MATERIAL);
        loader.pollDevelopmentReloads();
        List<AssetLoadError> errors = loader.drainErrors();
        boolean sawHotReloadFailure = errors.stream().anyMatch(error -> error.code() == AssetLoadErrorCode.HOT_RELOAD_FAILED);
        writeMaterial(restore);
        MaterialAsset after = materialHandle.requireReady();
        logSink.accept("Phase 6 invalid MATERIAL reload -> " + (sawHotReloadFailure ? "HOT_RELOAD_FAILED" : "unexpected diagnostic")
            + "; previous READY value preserved=" + before.equals(after));
        for (AssetLoadError error : errors) {
            logSink.accept(formatError(error));
        }

    }

    void demonstrateMissingMeshFallback() {

        try (ResourceHandle<MeshAsset> missing = loader.loadMesh(MISSING_MESH_ID)) {
            MeshAsset fallback = missing.requireReady();
            MeshAsset.Primitive primitive = fallback.primitives().getFirst();
            logSink.accept("Phase 6 missing MESH -> " + missing.state() + " fallback primitive=" + primitive.name() + " vertices=" + primitive.positions().length / 3);
            drainErrors("fallback");
        }

    }

    void reloadHandles() {

        ResourceHandle<MeshAsset> oldMesh = meshHandle;
        ResourceHandle<MaterialAsset> oldMaterial = materialHandle;
        oldMesh.close();
        oldMaterial.close();
        logSink.accept("Phase 6 handles released -> mesh=" + oldMesh.state() + ", material=" + oldMaterial.state());
        startLoads();

    }

    String readySummary() {

        awaitReady(meshHandle);
        awaitReady(materialHandle);
        MeshAsset.Primitive primitive = meshHandle.requireReady().primitives().getFirst();
        return "Phase 6 Asset Lab READY | meshId=" + MESH_ID + " primitive=" + primitive.name() + " vertices=" + primitive.positions().length / 3 + " indices="
            + primitive.indices().length + " | materialId=" + MATERIAL_ID + " " + summarize(materialHandle.requireReady());

    }

    Path cacheRoot() {

        return cacheRoot;

    }

    private void startLoads() {

        meshHandle = loader.loadMesh(MESH_ID);
        materialHandle = loader.loadMaterial(MATERIAL_ID);
        lastMeshState = null;
        lastMaterialState = null;
        logSink.accept("Phase 6 Asset Lab started from cooked cache only: " + cacheRoot);

    }

    private void requireMaterialReady() {

        if (materialHandle.state() != ResourceHandleState.READY) {
            logSink.accept("Phase 6 MATERIAL is " + materialHandle.state() + "; hot reload control ignored until READY");
            return;
        }

    }

    private void writeMaterial(String json) {

        try {
            Files.writeString(materialPayload, json, StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalStateException("Failed to edit temporary Phase 6 MATERIAL payload", failure);
        }

    }

    private void drainErrors(String context) {

        for (AssetLoadError error : loader.drainErrors()) {
            logSink.accept("Phase 6 " + context + " " + formatError(error));
        }

    }

    private static String summarize(MaterialAsset material) {

        return "shader=" + material.shaderKey() + " rgba=[%.2f,%.2f,%.2f,%.2f]".formatted(material.redMultiplier(), material.greenMultiplier(), material.blueMultiplier(),
            material.alphaMultiplier());

    }

    private static String formatError(AssetLoadError error) {

        return "assetError[type=" + error.assetType() + ", code=" + error.code() + ", id=" + error.assetId() + ", detail=" + error.detail() + "]";

    }

    private static Path materializeFixture() {

        try {
            Path root = Files.createTempDirectory("sherko-phase6-asset-lab-");
            Path assets = Files.createDirectories(root.resolve("assets"));
            copyTextResource("/phase6/manifest.json", root.resolve("manifest.json"));
            copyTextResource("/phase6/material.json", assets.resolve(MATERIAL_ID + ".bin"));
            try (InputStream input = Objects.requireNonNull(SandboxAssetLab.class.getResourceAsStream("/phase6/mesh.smes.b64"), "Phase 6 mesh fixture")) {
                byte[] encoded = input.readAllBytes();
                Files.write(assets.resolve(MESH_ID + ".bin"), Base64.getMimeDecoder().decode(encoded));
            }
            return root;
        } catch (IOException failure) {
            throw new IllegalStateException("Failed to materialize Phase 6 sandbox fixture", failure);
        }

    }

    private static void copyTextResource(String resource, Path destination) throws IOException {

        try (InputStream input = Objects.requireNonNull(SandboxAssetLab.class.getResourceAsStream(resource), resource)) {
            Files.copy(input, destination, StandardCopyOption.REPLACE_EXISTING);
        }

    }

    private static void awaitReady(ResourceHandle<?> handle) {

        long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
        while (handle.state() == ResourceHandleState.LOADING && System.nanoTime() < deadline) {
            Thread.onSpinWait();
        }
        if (handle.state() != ResourceHandleState.READY) {
            throw new IllegalStateException("Phase 6 sandbox fixture did not reach READY: " + handle.state());
        }

    }

    @Override
    public void close() {

        meshHandle.close();
        materialHandle.close();
        loader.close();
        deleteTreeBestEffort(cacheRoot);

    }

    private static void deleteTreeBestEffort(Path root) {

        try (var paths = Files.walk(root)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        } catch (IOException ignored) {
            root.toFile().deleteOnExit();
        }

    }
}
