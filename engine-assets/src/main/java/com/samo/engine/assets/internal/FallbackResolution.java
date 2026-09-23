package com.samo.engine.assets.internal;

import com.samo.engine.assets.api.AssetLoadError;
import com.samo.engine.assets.api.ResourceHandle;
import java.util.Objects;

record FallbackResolution<T>(ResourceHandle<T> handle, AssetLoadError error) {
    FallbackResolution {

        Objects.requireNonNull(handle, "handle");
        Objects.requireNonNull(error, "error");

    }
}
