package com.samo.engine.assets.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.samo.engine.assets.api.AssetId;
import com.samo.engine.assets.api.AssetLoadError;
import com.samo.engine.assets.api.AssetLoadErrorCode;
import com.samo.engine.assets.api.MeshAsset;
import com.samo.engine.assets.api.ResourceHandle;
import com.samo.engine.assets.api.ResourceHandleState;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AsyncAssetLoaderTest {
    private static final AssetId MESH_ID = AssetId.parse("01234567-89ab-cdef-fedc-ba9876543210");
    private static final AssetId OTHER_ID = AssetId.parse("11234567-89ab-cdef-fedc-ba9876543210");

    @TempDir
    Path tempDir;

    @Test
    void manifestBackedMeshStartsLoadingAndWorkerDecodesExactValuesOffCallerThread() throws Exception {

        byte[] cooked = CookedMeshBinary.encode(List.of(referenceMesh()));
        Path cookedPath = createCache(MESH_ID, "MESH", cooked);
        RuntimeAssetManifest manifest = RuntimeAssetManifest.load(tempDir);
        ManualExecutor executor = new ManualExecutor();
        AtomicReference<Thread> readThread = new AtomicReference<>();
        RuntimeAssetFileSystem fileSystem = path -> {
            readThread.set(Thread.currentThread());
            return Files.readAllBytes(path);
        };
        try (AsyncAssetLoader loader = AsyncAssetLoader.createForTest(manifest, fileSystem, executor)) {
            Thread caller = Thread.currentThread();
            ResourceHandle<MeshAsset> handle = loader.loadMesh(MESH_ID);

            assertThat(handle.state()).isEqualTo(ResourceHandleState.LOADING);
            assertThat(readThread.get()).isNull();
            executor.runNextOnNewThread();

            assertThat(readThread.get()).isNotNull().isNotSameAs(caller);
            assertThat(handle.state()).isEqualTo(ResourceHandleState.READY);
            MeshAsset.Primitive primitive = handle.requireReady().primitives().getFirst();
            assertThat(primitive.meshIndex()).isZero();
            assertThat(primitive.name()).isEqualTo("RuntimeTriangle");
            assertThat(primitive.positions()).containsExactly(-1.0f, 0.0f, -2.0f, 1.0f, 0.0f, -2.0f, 0.0f, 1.0f, -2.0f);
            assertThat(primitive.normals()).containsExactly(0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f);
            assertThat(primitive.uv0()).containsExactly(0.0f, 0.0f, 1.0f, 0.0f, 0.5f, 1.0f);
            assertThat(primitive.indices()).containsExactly(0, 1, 2);
            assertThat(loader.drainErrors()).isEmpty();
            assertThat(cookedPath).exists();
        }

    }

    @Test
    void missingIdentityAndMissingFileYieldReadyFallbackAndOrderedDrain() throws Exception {

        byte[] cooked = CookedMeshBinary.encode(List.of(referenceMesh()));
        Path cookedPath = createCache(MESH_ID, "MESH", cooked);
        RuntimeAssetManifest manifest = RuntimeAssetManifest.load(tempDir);
        ManualExecutor executor = new ManualExecutor();
        try (AsyncAssetLoader loader = AsyncAssetLoader.createForTest(manifest, RuntimeAssetFileSystem.system(), executor)) {
            ResourceHandle<MeshAsset> unknown = loader.loadMesh(OTHER_ID);
            assertThat(unknown.state()).isEqualTo(ResourceHandleState.READY);
            assertThat(unknown.requireReady().primitives().getFirst().name()).isEqualTo("FallbackCube");

            Files.delete(cookedPath);
            ResourceHandle<MeshAsset> missingFile = loader.loadMesh(MESH_ID);
            assertThat(missingFile.state()).isEqualTo(ResourceHandleState.LOADING);
            executor.runNext();

            assertThat(missingFile.state()).isEqualTo(ResourceHandleState.READY);
            assertThat(missingFile.requireReady().primitives().getFirst().name()).isEqualTo("FallbackCube");
            List<AssetLoadError> errors = loader.drainErrors();
            assertThat(errors).extracting(AssetLoadError::assetId).containsExactly(OTHER_ID, MESH_ID);
            assertThat(errors).extracting(AssetLoadError::code).containsExactly(AssetLoadErrorCode.MISSING_CONTENT, AssetLoadErrorCode.MISSING_CONTENT);
            assertThat(loader.drainErrors()).isEmpty();
        }

    }

    @Test
    void readFailureSizeMismatchAndCorruptionFailWithStructuredErrors() throws Exception {

        byte[] valid = CookedMeshBinary.encode(List.of(referenceMesh()));
        createCache(MESH_ID, "MESH", valid);
        RuntimeAssetManifest manifest = RuntimeAssetManifest.load(tempDir);

        ManualExecutor deniedExecutor = new ManualExecutor();
        RuntimeAssetFileSystem denied = path -> {
            throw new AccessDeniedException(path.toString());
        };
        try (AsyncAssetLoader loader = AsyncAssetLoader.createForTest(manifest, denied, deniedExecutor)) {
            ResourceHandle<MeshAsset> handle = loader.loadMesh(MESH_ID);
            deniedExecutor.runNext();
            assertThat(handle.state()).isEqualTo(ResourceHandleState.FAILED);
            assertThat(loader.drainErrors()).extracting(AssetLoadError::code).containsExactly(AssetLoadErrorCode.READ_FAILED);
        }

        ManualExecutor shortExecutor = new ManualExecutor();
        RuntimeAssetFileSystem shortRead = path -> new byte[valid.length - 1];
        try (AsyncAssetLoader loader = AsyncAssetLoader.createForTest(manifest, shortRead, shortExecutor)) {
            ResourceHandle<MeshAsset> handle = loader.loadMesh(MESH_ID);
            shortExecutor.runNext();
            assertThat(handle.state()).isEqualTo(ResourceHandleState.FAILED);
            assertThat(loader.drainErrors()).extracting(AssetLoadError::code).containsExactly(AssetLoadErrorCode.INVALID_CONTENT);
        }

        byte[] corrupt = valid.clone();
        corrupt[corrupt.length - 1] ^= 1;
        ManualExecutor corruptExecutor = new ManualExecutor();
        RuntimeAssetFileSystem corruptRead = path -> corrupt;
        try (AsyncAssetLoader loader = AsyncAssetLoader.createForTest(manifest, corruptRead, corruptExecutor)) {
            ResourceHandle<MeshAsset> handle = loader.loadMesh(MESH_ID);
            corruptExecutor.runNext();
            assertThat(handle.state()).isEqualTo(ResourceHandleState.FAILED);
            assertThat(loader.drainErrors()).extracting(AssetLoadError::code).containsExactly(AssetLoadErrorCode.INVALID_CONTENT);
        }

    }

    @Test
    void wrongTypeFailsSynchronouslyBeforeSubmission() throws Exception {

        byte[] bytes = {1, 2, 3};
        createCache(MESH_ID, "TEXTURE", bytes);
        RuntimeAssetManifest manifest = RuntimeAssetManifest.load(tempDir);
        ManualExecutor executor = new ManualExecutor();
        try (AsyncAssetLoader loader = AsyncAssetLoader.createForTest(manifest, RuntimeAssetFileSystem.system(), executor)) {
            assertThatThrownBy(() -> loader.loadMesh(MESH_ID)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("not MESH");
            assertThat(executor.queued()).isZero();
        }

    }

    @Test
    void releasedHandleIsNeverResurrectedByLateWorkerCompletion() throws Exception {

        byte[] cooked = CookedMeshBinary.encode(List.of(referenceMesh()));
        createCache(MESH_ID, "MESH", cooked);
        RuntimeAssetManifest manifest = RuntimeAssetManifest.load(tempDir);
        ManualExecutor executor = new ManualExecutor();
        try (AsyncAssetLoader loader = AsyncAssetLoader.createForTest(manifest, RuntimeAssetFileSystem.system(), executor)) {
            ResourceHandle<MeshAsset> handle = loader.loadMesh(MESH_ID);
            handle.close();
            assertThat(handle.state()).isEqualTo(ResourceHandleState.RELEASED);

            executor.runNext();

            assertThat(handle.state()).isEqualTo(ResourceHandleState.RELEASED);
            assertThat(handle.readyValue()).isEmpty();
            assertThat(loader.drainErrors()).isEmpty();
        }

    }

    @Test
    void closingLoaderRejectsNewLoadsWithoutChangingCompletedHandle() throws Exception {

        byte[] cooked = CookedMeshBinary.encode(List.of(referenceMesh()));
        createCache(MESH_ID, "MESH", cooked);
        RuntimeAssetManifest manifest = RuntimeAssetManifest.load(tempDir);
        ManualExecutor executor = new ManualExecutor();
        AsyncAssetLoader loader = AsyncAssetLoader.createForTest(manifest, RuntimeAssetFileSystem.system(), executor);

        ResourceHandle<MeshAsset> handle = loader.loadMesh(MESH_ID);
        executor.runNext();
        assertThat(handle.state()).isEqualTo(ResourceHandleState.READY);

        loader.close();
        assertThatThrownBy(() -> loader.loadMesh(MESH_ID)).isInstanceOf(IllegalStateException.class).hasMessageContaining("closed");
        assertThat(handle.state()).isEqualTo(ResourceHandleState.READY);
        loader.close();

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

    private static EngineMesh referenceMesh() {

        return new EngineMesh(0, "RuntimeTriangle", new float[]{-1.0f, 0.0f, -2.0f, 1.0f, 0.0f, -2.0f, 0.0f, 1.0f, -2.0f},
            new float[]{0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f}, null, null, new float[]{0.0f, 0.0f, 1.0f, 0.0f, 0.5f, 1.0f}, new int[]{0, 1, 2});

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

            if (shutdown) {
                throw new java.util.concurrent.RejectedExecutionException();
            }
            tasks.add(command);

        }

        int queued() {

            return tasks.size();

        }

        void runNext() {

            Runnable task = tasks.remove();
            task.run();

        }

        void runNextOnNewThread() throws InterruptedException {

            Runnable task = tasks.remove();
            Thread thread = Thread.ofPlatform().start(task);
            thread.join(5_000L);
            assertThat(thread.isAlive()).isFalse();

        }
    }
}
