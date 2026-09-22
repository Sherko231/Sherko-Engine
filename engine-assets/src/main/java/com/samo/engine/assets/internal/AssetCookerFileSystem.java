package com.samo.engine.assets.internal;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

interface AssetCookerFileSystem {
    void createDirectories(Path path) throws IOException;

    void copy(Path source, Path target) throws IOException;

    long size(Path path) throws IOException;

    void writeString(Path path, String content) throws IOException;

    void deleteTree(Path root) throws IOException;

    static AssetCookerFileSystem system() {

        return new AssetCookerFileSystem() {
            @Override
            public void createDirectories(Path path) throws IOException {

                Files.createDirectories(path);

            }

            @Override
            public void copy(Path source, Path target) throws IOException {

                Files.copy(source, target);

            }

            @Override
            public long size(Path path) throws IOException {

                return Files.size(path);

            }

            @Override
            public void writeString(Path path, String content) throws IOException {

                Files.writeString(path, content, StandardCharsets.UTF_8);

            }

            @Override
            public void deleteTree(Path root) throws IOException {

                if (!Files.exists(root)) {
                    return;
                }
                try (Stream<Path> paths = Files.walk(root)) {
                    for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                        Files.deleteIfExists(path);
                    }
                }

            }
        };

    }
}
