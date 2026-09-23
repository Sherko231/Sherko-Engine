package com.samo.engine.assets.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.samo.engine.assets.api.AssetId;
import com.samo.engine.assets.api.AssetLoadError;
import com.samo.engine.assets.api.AssetLoadErrorCode;
import com.samo.engine.assets.api.MaterialAsset;
import com.samo.engine.assets.api.ResourceHandle;
import com.samo.engine.assets.api.ResourceHandleState;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AsyncAssetLoaderMaterialTest {
    private static final AssetId MATERIAL_ID = AssetId.parse("01234567-89ab-cdef-fedc-ba9876543210");
    private static final AssetId OTHER_ID = AssetId.parse("11234567-89ab-cdef-fedc-ba9876543210");

    @TempDir
    Path tempDir;

    @Test
    void loadsMaterialAsynchronouslyAndReloadsSameReadyHandle() throws Exception {

        byte[] initial = materialBytes("opaque-baseline", 1.0f, 0.5f, 0.25f, 1.0f);
        Path cooked = createCache(MATERIAL_ID, "MATERIAL", initial);
        RuntimeAssetManifest manifest = RuntimeAssetManifest.load(tempDir);
        ManualExecutor executor = new ManualExecutor();

        try (AsyncAssetLoader loader = AsyncAssetLoader.createForTest(manifest, RuntimeAssetFileSystem.system(), executor)) {
            ResourceHandle<MaterialAsset> handle = loader.loadMaterial(MATERIAL_ID);
            assertThat(handle.state()).isEqualTo(ResourceHandleState.LOADING);

            executor.runNext();
            assertThat(handle.state()).isEqualTo(ResourceHandleState.READY);
            MaterialAsset first = handle.requireReady();
            assertThat(first.shaderKey()).isEqualTo("opaque-baseline");
            assertThat(first.redMultiplier()).isEqualTo(1.0f);

            byte[] changed = materialBytes("opaque-baseline", 0.2f, 0.4f, 0.6f, 1.0f);
            Files.write(cooked, changed);
            loader.pollDevelopmentReloads();

            assertThat(handle.state()).isEqualTo(ResourceHandleState.READY);
            assertThat(handle.assetId()).isEqualTo(MATERIAL_ID);
            assertThat(handle.requireReady().redMultiplier()).isEqualTo(0.2f);
            assertThat(handle.requireReady().greenMultiplier()).isEqualTo(0.4f);
            assertThat(loader.drainErrors()).isEmpty();
        }

    }

    @Test
    void failedReloadPreservesPreviousValueAndDeduplicatesSameCandidate() throws Exception {

        byte[] initial = materialBytes("opaque-baseline", 0.3f, 0.4f, 0.5f, 1.0f);
        Path cooked = createCache(MATERIAL_ID, "MATERIAL", initial);
        RuntimeAssetManifest manifest = RuntimeAssetManifest.load(tempDir);
        ManualExecutor executor = new ManualExecutor();

        try (AsyncAssetLoader loader = AsyncAssetLoader.createForTest(manifest, RuntimeAssetFileSystem.system(), executor)) {
            ResourceHandle<MaterialAsset> handle = loader.loadMaterial(MATERIAL_ID);
            executor.runNext();
            MaterialAsset accepted = handle.requireReady();

            Files.writeString(cooked, "{broken", StandardCharsets.UTF_8);
            loader.pollDevelopmentReloads();
            loader.pollDevelopmentReloads();

            assertThat(handle.state()).isEqualTo(ResourceHandleState.READY);
            assertThat(handle.requireReady()).isEqualTo(accepted);
            List<AssetLoadError> errors = loader.drainErrors();
            assertThat(errors).hasSize(1);
            assertThat(errors.getFirst().code()).isEqualTo(AssetLoadErrorCode.HOT_RELOAD_FAILED);

            Files.write(cooked, materialBytes("opaque-baseline", 0.9f, 0.4f, 0.5f, 1.0f));
            loader.pollDevelopmentReloads();
            assertThat(handle.requireReady().redMultiplier()).isEqualTo(0.9f);
        }

    }

    @Test
    void missingAndWrongTypeBehaviorMatchesRuntimeContract() throws Exception {

        byte[] meshLike = {1, 2, 3};
        createCache(MATERIAL_ID, "MESH", meshLike);
        RuntimeAssetManifest manifest = RuntimeAssetManifest.load(tempDir);
        ManualExecutor executor = new ManualExecutor();

        try (AsyncAssetLoader loader = AsyncAssetLoader.createForTest(manifest, RuntimeAssetFileSystem.system(), executor)) {
            ResourceHandle<MaterialAsset> missing = loader.loadMaterial(OTHER_ID);
            assertThat(missing.state()).isEqualTo(ResourceHandleState.READY);
            assertThat(missing.requireReady().shaderKey()).isEqualTo("opaque-baseline");
            assertThat(missing.requireReady().redMultiplier()).isEqualTo(1.0f);
            assertThat(missing.requireReady().greenMultiplier()).isZero();
            assertThat(loader.drainErrors()).extracting(AssetLoadError::code).containsExactly(AssetLoadErrorCode.MISSING_CONTENT);

            assertThatThrownBy(() -> loader.loadMaterial(MATERIAL_ID)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("not MATERIAL");
            assertThat(executor.queued()).isZero();
        }

    }

    @Test
    void releasedMaterialIsNotReloadedAndClosedLoaderRejectsPolling() throws Exception {

        byte[] initial = materialBytes("opaque-baseline", 0.3f, 0.4f, 0.5f, 1.0f);
        Path cooked = createCache(MATERIAL_ID, "MATERIAL", initial);
        RuntimeAssetManifest manifest = RuntimeAssetManifest.load(tempDir);
        ManualExecutor executor = new ManualExecutor();
        AsyncAssetLoader loader = AsyncAssetLoader.createForTest(manifest, RuntimeAssetFileSystem.system(), executor);

        ResourceHandle<MaterialAsset> handle = loader.loadMaterial(MATERIAL_ID);
        executor.runNext();
        handle.close();
        Files.write(cooked, materialBytes("opaque-baseline", 0.9f, 0.9f, 0.9f, 1.0f));
        loader.pollDevelopmentReloads();

        assertThat(handle.state()).isEqualTo(ResourceHandleState.RELEASED);
        assertThat(handle.readyValue()).isEmpty();

        loader.close();
        assertThatThrownBy(loader::pollDevelopmentReloads).isInstanceOf(IllegalStateException.class).hasMessageContaining("closed");
        assertThatThrownBy(() -> loader.loadMaterial(MATERIAL_ID)).isInstanceOf(IllegalStateException.class).hasMessageContaining("closed");

    }

    @Test
    void initialInvalidMaterialFailsWithStructuredError() throws Exception {

        byte[] invalid = "{broken".getBytes(StandardCharsets.UTF_8);
        createCache(MATERIAL_ID, "MATERIAL", invalid);
        RuntimeAssetManifest manifest = RuntimeAssetManifest.load(tempDir);
        ManualExecutor executor = new ManualExecutor();

        try (AsyncAssetLoader loader = AsyncAssetLoader.createForTest(manifest, RuntimeAssetFileSystem.system(), executor)) {
            ResourceHandle<MaterialAsset> handle = loader.loadMaterial(MATERIAL_ID);
            executor.runNext();

            assertThat(handle.state()).isEqualTo(ResourceHandleState.FAILED);
            assertThat(loader.drainErrors()).extracting(AssetLoadError::code).containsExactly(AssetLoadErrorCode.INVALID_CONTENT);
        }

    }

    private Path createCache(AssetId id, String type, byte[] bytes) throws Exception {

        Path assets = Files.createDirectories(tempDir.resolve("assets"));
        Path cooked = assets.resolve(id + ".bin");
        Files.write(cooked, bytes);
        Files.writeString(tempDir.resolve("manifest.json"), """
            {
              "schemaVersion":1,
              "assets":[
                {
                  "assetId":"%s",
                  "assetType":"%s",
                  "sourcePath":"source.asset",
                  "cookedPath":"assets/%s.bin",
                  "byteSize":%d
                }
              ]
            }
            """.formatted(id, type, id, bytes.length));
        return cooked;

    }

    private static byte[] materialBytes(String shaderKey, float red, float green, float blue, float alpha) {

        return """
            {
              "schemaVersion": 1,
              "shaderKey": "%s",
              "redMultiplier": %s,
              "greenMultiplier": %s,
              "blueMultiplier": %s,
              "alphaMultiplier": %s
            }
            """.formatted(shaderKey, red, green, blue, alpha).getBytes(StandardCharsets.UTF_8);

    }

    private static final class ManualExecutor extends AbstractExecutorService {
        private final Queue<Runnable> tasks = new ArrayDeque<>();
        private boolean shutdown;

        @Override
        public void shutdown() {

            shutdown = true;

        }

        @Override
        public List<Runnable> shutdownNow() {

            shutdown = true;
            ArrayList<Runnable> remaining = new ArrayList<>(tasks);
            tasks.clear();
            return remaining;

        }

        @Override
        public boolean isShutdown() {

            return shutdown;

        }

        @Override
        public boolean isTerminated() {

            return shutdown && tasks.isEmpty();

        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) {

            return isTerminated();

        }

        @Override
        public void execute(Runnable command) {

            tasks.add(command);

        }

        int queued() {

            return tasks.size();

        }

        void runNext() {

            tasks.remove().run();

        }
    }
}
