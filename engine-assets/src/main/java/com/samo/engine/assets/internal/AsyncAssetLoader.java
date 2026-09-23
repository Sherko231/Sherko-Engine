package com.samo.engine.assets.internal;

import com.samo.engine.assets.api.AssetId;
import com.samo.engine.assets.api.AssetLoadError;
import com.samo.engine.assets.api.AssetLoadErrorCode;
import com.samo.engine.assets.api.AssetLoader;
import com.samo.engine.assets.api.AssetType;
import com.samo.engine.assets.api.MeshAsset;
import com.samo.engine.assets.api.ResourceHandle;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class AsyncAssetLoader implements AssetLoader {
    private final RuntimeAssetManifest manifest;
    private final RuntimeAssetFileSystem fileSystem;
    private final ExecutorService executor;
    private final ConcurrentLinkedQueue<AssetLoadError> errors = new ConcurrentLinkedQueue<>();
    private boolean closed;

    private AsyncAssetLoader(RuntimeAssetManifest manifest, RuntimeAssetFileSystem fileSystem, ExecutorService executor) {

        this.manifest = Objects.requireNonNull(manifest, "manifest");
        this.fileSystem = Objects.requireNonNull(fileSystem, "fileSystem");
        this.executor = Objects.requireNonNull(executor, "executor");

    }

    public static AssetLoader open(Path cookedCacheDirectory) {

        RuntimeAssetManifest manifest = RuntimeAssetManifest.load(cookedCacheDirectory);
        return new AsyncAssetLoader(manifest, RuntimeAssetFileSystem.system(), Executors.newVirtualThreadPerTaskExecutor());

    }

    static AsyncAssetLoader createForTest(RuntimeAssetManifest manifest, RuntimeAssetFileSystem fileSystem, ExecutorService executor) {

        return new AsyncAssetLoader(manifest, fileSystem, executor);

    }

    @Override
    public synchronized ResourceHandle<MeshAsset> loadMesh(AssetId assetId) {

        AssetId requested = Objects.requireNonNull(assetId, "assetId");
        if (closed) {
            throw new IllegalStateException("AssetLoader is closed");
        }

        RuntimeAssetManifest.Entry entry = manifest.find(requested);
        if (entry == null) {
            FallbackResolution<MeshAsset> fallback = FallbackAssetResolver.missingMeshAsset(requested);
            errors.add(fallback.error());
            return fallback.handle();
        }
        if (entry.assetType() != AssetType.MESH) {
            throw new IllegalArgumentException("Asset " + requested + " is " + entry.assetType() + ", not MESH");
        }

        ResourceHandleCell<MeshAsset> handle = new ResourceHandleCell<>(requested);
        executor.submit(() -> loadMeshWorker(entry, handle));
        return handle;

    }

    @Override
    public List<AssetLoadError> drainErrors() {

        ArrayList<AssetLoadError> drained = new ArrayList<>();
        AssetLoadError error;
        while ((error = errors.poll()) != null) {
            drained.add(error);
        }
        return List.copyOf(drained);

    }

    @Override
    public synchronized void close() {

        if (closed) {
            return;
        }
        closed = true;
        executor.shutdown();

    }

    private void loadMeshWorker(RuntimeAssetManifest.Entry entry, ResourceHandleCell<MeshAsset> handle) {

        byte[] bytes;
        try {
            bytes = fileSystem.readAllBytes(entry.cookedPath());
        } catch (NoSuchFileException exception) {
            completeMissing(entry.assetId(), handle);
            return;
        } catch (IOException exception) {
            if (handle.tryCompleteFailed()) {
                errors.add(new AssetLoadError(entry.assetId(), AssetType.MESH, AssetLoadErrorCode.READ_FAILED, "Failed to read cooked MESH content for asset " + entry.assetId()));
            }
            return;
        }

        if (bytes.length != entry.byteSize()) {
            completeInvalid(entry.assetId(), handle, "Cooked MESH byte size does not match manifest");
            return;
        }

        MeshAsset mesh;
        try {
            mesh = RuntimeMeshAdapter.toPublic(CookedMeshBinary.decode(bytes));
        } catch (RuntimeException exception) {
            completeInvalid(entry.assetId(), handle, "Cooked MESH content is invalid");
            return;
        }
        handle.tryCompleteReady(mesh);

    }

    private void completeMissing(AssetId assetId, ResourceHandleCell<MeshAsset> handle) {

        MeshAsset fallback = FallbackAssetResolver.fallbackMeshAsset();
        if (handle.tryCompleteReady(fallback)) {
            errors.add(FallbackAssetResolver.missingError(assetId, AssetType.MESH));
        }

    }

    private void completeInvalid(AssetId assetId, ResourceHandleCell<MeshAsset> handle, String detail) {

        if (handle.tryCompleteFailed()) {
            errors.add(new AssetLoadError(assetId, AssetType.MESH, AssetLoadErrorCode.INVALID_CONTENT, detail + " for asset " + assetId));
        }

    }
}
