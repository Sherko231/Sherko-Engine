package com.samo.game.sandbox;

import com.samo.engine.assets.api.ResourceHandleState;
import java.util.Objects;

record SandboxAssetLabVisualState(ResourceHandleState meshState, ResourceHandleState materialState, SandboxAssetLabSignal lastSignal) {
    SandboxAssetLabVisualState {

        Objects.requireNonNull(meshState, "meshState");
        Objects.requireNonNull(materialState, "materialState");
        Objects.requireNonNull(lastSignal, "lastSignal");

    }
}

enum SandboxAssetLabSignal {
    NONE,
    VALID_RELOAD,
    FAILED_RELOAD,
    MISSING_FALLBACK,
    HANDLE_RELOAD
}
