package com.samo.engine.assets.internal;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

interface RuntimeAssetFileSystem {
    byte[] readAllBytes(Path path) throws IOException;

    static RuntimeAssetFileSystem system() {

        return Files::readAllBytes;

    }
}
