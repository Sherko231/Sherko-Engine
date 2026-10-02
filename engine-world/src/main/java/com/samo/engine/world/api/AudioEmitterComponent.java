package com.samo.engine.world.api;

import com.samo.engine.assets.api.AssetId;
import java.util.Objects;

/** Immutable stable audio-asset reference for a world emitter. */
public record AudioEmitterComponent(AssetId audioAssetId) {
    public AudioEmitterComponent {

        Objects.requireNonNull(audioAssetId, "audioAssetId");

    }
}
