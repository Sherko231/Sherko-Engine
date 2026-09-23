package com.samo.engine.assets.internal;

import com.samo.engine.assets.api.AssetId;
import com.samo.engine.assets.api.AssetLoadError;
import com.samo.engine.assets.api.AssetLoadErrorCode;
import com.samo.engine.assets.api.AssetLoader;
import com.samo.engine.assets.api.AssetType;
import com.samo.engine.assets.api.MaterialAsset;
import com.samo.engine.assets.api.MeshAsset;
import com.samo.engine.assets.api.ResourceHandle;
import com.samo.engine.assets.api.ResourceHandleState;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
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
    private final List<TrackedMaterial> trackedMaterials = new ArrayList<>();
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

        AssetId requested = requireOpenAssetId(assetId);
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
    public synchronized ResourceHandle<MaterialAsset> loadMaterial(AssetId assetId) {

        AssetId requested = requireOpenAssetId(assetId);
        RuntimeAssetManifest.Entry entry = manifest.find(requested);
        if (entry == null) {
            FallbackResolution<MaterialAsset> fallback = FallbackAssetResolver.missingMaterialAsset(requested);
            errors.add(fallback.error());
            return fallback.handle();
        }
        if (entry.assetType() != AssetType.MATERIAL) {
            throw new IllegalArgumentException("Asset " + requested + " is " + entry.assetType() + ", not MATERIAL");
        }

        ResourceHandleCell<MaterialAsset> handle = new ResourceHandleCell<>(requested);
        executor.submit(() -> loadMaterialWorker(entry, handle));
        return handle;

    }

    @Override
    public synchronized void pollDevelopmentReloads() {

        if (closed) {
            throw new IllegalStateException("AssetLoader is closed");
        }

        Iterator<TrackedMaterial> iterator = trackedMaterials.iterator();
        while (iterator.hasNext()) {
            TrackedMaterial tracked = iterator.next();
            if (tracked.handle.state() == ResourceHandleState.RELEASED) {
                iterator.remove();
                continue;
            }
            if (tracked.handle.state() != ResourceHandleState.READY) {
                continue;
            }
            pollMaterial(tracked);
        }

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
        trackedMaterials.clear();
        executor.shutdown();

    }

    private AssetId requireOpenAssetId(AssetId assetId) {

        AssetId requested = Objects.requireNonNull(assetId, "assetId");
        if (closed) {
            throw new IllegalStateException("AssetLoader is closed");
        }
        return requested;

    }

    private void loadMeshWorker(RuntimeAssetManifest.Entry entry, ResourceHandleCell<MeshAsset> handle) {

        byte[] bytes;
        try {
            bytes = fileSystem.readAllBytes(entry.cookedPath());
        } catch (NoSuchFileException exception) {
            completeMissingMesh(entry.assetId(), handle);
            return;
        } catch (IOException exception) {
            if (handle.tryCompleteFailed()) {
                errors.add(new AssetLoadError(entry.assetId(), AssetType.MESH, AssetLoadErrorCode.READ_FAILED, "Failed to read cooked MESH content for asset " + entry.assetId()));
            }
            return;
        }

        if (bytes.length != entry.byteSize()) {
            completeInvalidMesh(entry.assetId(), handle, "Cooked MESH byte size does not match manifest");
            return;
        }

        MeshAsset mesh;
        try {
            mesh = RuntimeMeshAdapter.toPublic(CookedMeshBinary.decode(bytes));
        } catch (RuntimeException exception) {
            completeInvalidMesh(entry.assetId(), handle, "Cooked MESH content is invalid");
            return;
        }
        handle.tryCompleteReady(mesh);

    }

    private void loadMaterialWorker(RuntimeAssetManifest.Entry entry, ResourceHandleCell<MaterialAsset> handle) {

        byte[] bytes;
        try {
            bytes = fileSystem.readAllBytes(entry.cookedPath());
        } catch (NoSuchFileException exception) {
            MaterialAsset fallback = FallbackAssetResolver.fallbackMaterialAsset();
            if (handle.tryCompleteReady(fallback)) {
                errors.add(FallbackAssetResolver.missingError(entry.assetId(), AssetType.MATERIAL));
                trackMaterial(entry, handle, null);
            }
            return;
        } catch (IOException exception) {
            if (handle.tryCompleteFailed()) {
                errors.add(
                    new AssetLoadError(entry.assetId(), AssetType.MATERIAL, AssetLoadErrorCode.READ_FAILED, "Failed to read cooked MATERIAL content for asset " + entry.assetId()));
            }
            return;
        }

        if (bytes.length != entry.byteSize()) {
            completeInvalidMaterial(entry.assetId(), handle, "Cooked MATERIAL byte size does not match manifest");
            return;
        }

        MaterialAsset material;
        try {
            material = MaterialAssetJson.decode(bytes);
        } catch (RuntimeException exception) {
            completeInvalidMaterial(entry.assetId(), handle, "Cooked MATERIAL content is invalid");
            return;
        }

        if (handle.tryCompleteReady(material)) {
            trackMaterial(entry, handle, bytes);
        }

    }

    private synchronized void trackMaterial(RuntimeAssetManifest.Entry entry, ResourceHandleCell<MaterialAsset> handle, byte[] acceptedBytes) {

        if (!closed && handle.state() == ResourceHandleState.READY) {
            trackedMaterials.add(new TrackedMaterial(entry, handle, copy(acceptedBytes)));
        }

    }

    private void pollMaterial(TrackedMaterial tracked) {

        byte[] candidate;
        try {
            candidate = fileSystem.readAllBytes(tracked.entry.cookedPath());
        } catch (NoSuchFileException exception) {
            reportReloadFailureOnce(tracked, "missing", "Development MATERIAL reload file is missing");
            return;
        } catch (IOException exception) {
            reportReloadFailureOnce(tracked, "read:" + exception.getClass().getName(), "Development MATERIAL reload read failed");
            return;
        }

        if (tracked.acceptedBytes != null && Arrays.equals(candidate, tracked.acceptedBytes)) {
            tracked.lastFailureKey = null;
            return;
        }

        MaterialAsset material;
        try {
            material = MaterialAssetJson.decode(candidate);
        } catch (RuntimeException exception) {
            reportReloadFailureOnce(tracked, "invalid:" + Arrays.hashCode(candidate), "Development MATERIAL reload content is invalid");
            return;
        }

        if (tracked.handle.tryReplaceReady(material)) {
            tracked.acceptedBytes = candidate.clone();
            tracked.lastFailureKey = null;
        }

    }

    private void reportReloadFailureOnce(TrackedMaterial tracked, String failureKey, String detail) {

        if (failureKey.equals(tracked.lastFailureKey)) {
            return;
        }
        tracked.lastFailureKey = failureKey;
        errors.add(new AssetLoadError(tracked.entry.assetId(), AssetType.MATERIAL, AssetLoadErrorCode.HOT_RELOAD_FAILED,
            detail + " for asset " + tracked.entry.assetId() + "; preserving previous valid resource"));

    }

    private void completeMissingMesh(AssetId assetId, ResourceHandleCell<MeshAsset> handle) {

        MeshAsset fallback = FallbackAssetResolver.fallbackMeshAsset();
        if (handle.tryCompleteReady(fallback)) {
            errors.add(FallbackAssetResolver.missingError(assetId, AssetType.MESH));
        }

    }

    private void completeInvalidMesh(AssetId assetId, ResourceHandleCell<MeshAsset> handle, String detail) {

        if (handle.tryCompleteFailed()) {
            errors.add(new AssetLoadError(assetId, AssetType.MESH, AssetLoadErrorCode.INVALID_CONTENT, detail + " for asset " + assetId));
        }

    }

    private void completeInvalidMaterial(AssetId assetId, ResourceHandleCell<MaterialAsset> handle, String detail) {

        if (handle.tryCompleteFailed()) {
            errors.add(new AssetLoadError(assetId, AssetType.MATERIAL, AssetLoadErrorCode.INVALID_CONTENT, detail + " for asset " + assetId));
        }

    }

    private static byte[] copy(byte[] bytes) {

        return bytes == null ? null : bytes.clone();

    }

    private static final class TrackedMaterial {
        private final RuntimeAssetManifest.Entry entry;
        private final ResourceHandleCell<MaterialAsset> handle;
        private byte[] acceptedBytes;
        private String lastFailureKey;

        private TrackedMaterial(RuntimeAssetManifest.Entry entry, ResourceHandleCell<MaterialAsset> handle, byte[] acceptedBytes) {

            this.entry = entry;
            this.handle = handle;
            this.acceptedBytes = acceptedBytes;

        }
    }
}
