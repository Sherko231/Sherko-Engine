package com.samo.engine.world.api;

import com.samo.engine.assets.api.AssetId;
import java.util.Objects;

/** Immutable stable asset references for a renderable world entity. */
public record MeshRendererComponent(AssetId meshAssetId, AssetId materialAssetId) {
    public MeshRendererComponent {

        Objects.requireNonNull(meshAssetId, "meshAssetId");
        Objects.requireNonNull(materialAssetId, "materialAssetId");

    }
}
