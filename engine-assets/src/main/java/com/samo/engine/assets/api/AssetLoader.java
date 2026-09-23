package com.samo.engine.assets.api;

import java.util.List;

/** Asynchronous runtime loader for supported cooked asset types. */
public interface AssetLoader extends AutoCloseable {
    /**
     * Starts asynchronous loading of one cooked mesh.
     *
     * @param assetId
     *            requested identity
     * @return typed handle, initially loading for manifest-backed content
     */
    ResourceHandle<MeshAsset> loadMesh(AssetId assetId);

    /**
     * Starts asynchronous loading of one cooked material.
     *
     * @param assetId
     *            requested identity
     * @return typed handle, initially loading for manifest-backed content
     */
    ResourceHandle<MaterialAsset> loadMaterial(AssetId assetId);

    /**
     * Polls opt-in development material hot reloads.
     */
    void pollDevelopmentReloads();

    /**
     * Returns and removes structured asynchronous loading errors in occurrence order.
     *
     * @return drained errors
     */
    List<AssetLoadError> drainErrors();

    /** Stops accepting new load submissions. */
    @Override
    void close();
}
