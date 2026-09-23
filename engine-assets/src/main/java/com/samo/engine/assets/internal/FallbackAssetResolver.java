package com.samo.engine.assets.internal;

import com.samo.engine.assets.api.AssetId;
import com.samo.engine.assets.api.AssetLoadError;
import com.samo.engine.assets.api.AssetLoadErrorCode;
import com.samo.engine.assets.api.AssetType;
import com.samo.engine.assets.api.MaterialAsset;
import com.samo.engine.assets.api.MeshAsset;
import java.util.List;
import java.util.Objects;

final class FallbackAssetResolver {
    private static final byte OPAQUE = (byte) 255;
    private static final byte MAGENTA = (byte) 255;
    private static final byte BLACK = 0;
    private static final int SOUND_SAMPLE_RATE = 22050;
    private static final int SOUND_SAMPLE_COUNT = 220;
    private static final short SOUND_AMPLITUDE = 12000;

    private FallbackAssetResolver() {

    }

    static FallbackResolution<List<EngineMesh>> missingMesh(AssetId assetId) {

        AssetId requested = Objects.requireNonNull(assetId, "assetId");
        return resolution(requested, AssetType.MESH, List.of(fallbackMesh()));

    }

    static FallbackResolution<MeshAsset> missingMeshAsset(AssetId assetId) {

        AssetId requested = Objects.requireNonNull(assetId, "assetId");
        return resolution(requested, AssetType.MESH, fallbackMeshAsset());

    }

    static MeshAsset fallbackMeshAsset() {

        return RuntimeMeshAdapter.toPublic(List.of(fallbackMesh()));

    }

    static AssetLoadError missingError(AssetId assetId, AssetType assetType) {

        AssetId requested = Objects.requireNonNull(assetId, "assetId");
        AssetType type = Objects.requireNonNull(assetType, "assetType");
        return new AssetLoadError(requested, type, AssetLoadErrorCode.MISSING_CONTENT, "Missing " + type + " content for asset " + requested + "; using built-in fallback");

    }

    static FallbackResolution<CookedTexture> missingTexture(AssetId assetId) {

        AssetId requested = Objects.requireNonNull(assetId, "assetId");
        return resolution(requested, AssetType.TEXTURE, fallbackTexture());

    }

    static FallbackResolution<FallbackMaterial> missingMaterial(AssetId assetId) {

        AssetId requested = Objects.requireNonNull(assetId, "assetId");
        return resolution(requested, AssetType.MATERIAL, new FallbackMaterial(1.0f, 0.0f, 1.0f, 1.0f));

    }

    static FallbackResolution<MaterialAsset> missingMaterialAsset(AssetId assetId) {

        AssetId requested = Objects.requireNonNull(assetId, "assetId");
        return resolution(requested, AssetType.MATERIAL, fallbackMaterialAsset());

    }

    static MaterialAsset fallbackMaterialAsset() {

        return new MaterialAsset("opaque-baseline", 1.0f, 0.0f, 1.0f, 1.0f);

    }

    static FallbackResolution<FallbackSound> missingSound(AssetId assetId) {

        AssetId requested = Objects.requireNonNull(assetId, "assetId");
        return resolution(requested, AssetType.AUDIO, fallbackSound());

    }

    private static EngineMesh fallbackMesh() {

        float[] positions = {-0.5f, -0.5f, -0.5f, 0.5f, -0.5f, -0.5f, 0.5f, 0.5f, -0.5f, -0.5f, 0.5f, -0.5f, -0.5f, -0.5f, 0.5f, 0.5f, -0.5f, 0.5f, 0.5f, 0.5f, 0.5f, -0.5f, 0.5f,
            0.5f};
        int[] indices = {0, 2, 1, 0, 3, 2, 4, 5, 6, 4, 6, 7, 0, 1, 5, 0, 5, 4, 3, 7, 6, 3, 6, 2, 1, 2, 6, 1, 6, 5, 0, 4, 7, 0, 7, 3};
        return new EngineMesh(0, "FallbackCube", positions, null, null, null, null, indices);

    }

    private static CookedTexture fallbackTexture() {

        byte[] base = {MAGENTA, BLACK, MAGENTA, OPAQUE, BLACK, BLACK, BLACK, OPAQUE, BLACK, BLACK, BLACK, OPAQUE, MAGENTA, BLACK, MAGENTA, OPAQUE};
        byte[] mip = {(byte) 127, BLACK, (byte) 127, OPAQUE};
        return new CookedTexture(List.of(new TextureMipLevel(2, 2, base), new TextureMipLevel(1, 1, mip)));

    }

    private static FallbackSound fallbackSound() {

        short[] samples = new short[SOUND_SAMPLE_COUNT];
        short[] pattern = {0, SOUND_AMPLITUDE, SOUND_AMPLITUDE, 0, (short) -SOUND_AMPLITUDE, (short) -SOUND_AMPLITUDE, 0, 0};
        for (int index = 0; index < samples.length; index++) {
            samples[index] = pattern[index % pattern.length];
        }
        return new FallbackSound(1, SOUND_SAMPLE_RATE, samples);

    }

    private static <T> FallbackResolution<T> resolution(AssetId assetId, AssetType assetType, T value) {

        ResourceHandleCell<T> handle = new ResourceHandleCell<>(assetId);
        handle.completeReady(value);
        AssetLoadError error = missingError(assetId, assetType);
        return new FallbackResolution<>(handle, error);

    }
}
