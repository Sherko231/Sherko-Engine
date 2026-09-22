package com.samo.engine.assets.internal;

import com.samo.engine.assets.api.AssetId;
import java.util.List;

record SourceAssetDependencies(List<AssetId> assetDependencies, List<String> shaderDependencies) {
    static final SourceAssetDependencies EMPTY = new SourceAssetDependencies(List.of(), List.of());

    SourceAssetDependencies {

        assetDependencies = List.copyOf(assetDependencies);
        shaderDependencies = List.copyOf(shaderDependencies);

    }
}
