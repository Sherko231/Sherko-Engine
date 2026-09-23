package com.samo.engine.assets.api;

import java.util.Objects;

/**
 * Structured diagnostic accompanying a runtime asset fallback.
 *
 * @param assetId
 *            requested asset identity
 * @param assetType
 *            requested asset category
 * @param code
 *            stable structured error code
 * @param detail
 *            non-blank human-readable detail
 */
public record AssetLoadError(AssetId assetId, AssetType assetType, AssetLoadErrorCode code, String detail) {
    public AssetLoadError {

        Objects.requireNonNull(assetId, "assetId");
        Objects.requireNonNull(assetType, "assetType");
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(detail, "detail");
        if (detail.isBlank()) {
            throw new IllegalArgumentException("detail must not be blank");
        }

    }
}
