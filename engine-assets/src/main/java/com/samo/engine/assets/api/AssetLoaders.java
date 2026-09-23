package com.samo.engine.assets.api;

import com.samo.engine.assets.internal.AsyncAssetLoader;
import java.nio.file.Path;
import java.util.Objects;

/** Factory for supported runtime asset loaders. */
public final class AssetLoaders {
    private AssetLoaders() {

    }

    /**
     * Opens a cooked-cache runtime loader after validating manifest schema version 1.
     *
     * @param cookedCacheDirectory
     *            cooked cache root containing manifest.json
     * @return asynchronous runtime loader
     */
    public static AssetLoader open(Path cookedCacheDirectory) {

        return AsyncAssetLoader.open(Objects.requireNonNull(cookedCacheDirectory, "cookedCacheDirectory"));

    }
}
