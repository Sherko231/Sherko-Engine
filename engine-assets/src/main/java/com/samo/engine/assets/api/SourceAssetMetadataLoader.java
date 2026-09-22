package com.samo.engine.assets.api;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

final class SourceAssetMetadataLoader {
    private SourceAssetMetadataLoader() {

    }

    static SourceAssetMetadata load(Path path) {

        if (!Files.isRegularFile(path) || !Files.isReadable(path)) {
            throw failure(path, "metadata file is missing or unreadable");
        }

        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return SourceAssetMetadataJsonParser.parse(path, reader);
        } catch (IOException exception) {
            throw failure(path, "failed to read metadata JSON", exception);
        }

    }

    private static SourceAssetMetadataLoadException failure(Path path, String message) {

        return new SourceAssetMetadataLoadException(path + ": " + message);

    }

    private static SourceAssetMetadataLoadException failure(Path path, String message, Throwable cause) {

        return new SourceAssetMetadataLoadException(path + ": " + message, cause);

    }
}
