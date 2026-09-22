package com.samo.engine.assets.api;

/** Reports a source asset metadata file that could not be read or validated. */
public final class SourceAssetMetadataLoadException extends RuntimeException {
    SourceAssetMetadataLoadException(String message) {

        super(message);

    }

    SourceAssetMetadataLoadException(String message, Throwable cause) {

        super(message, cause);

    }
}
