package com.samo.engine.assets.internal;

import com.samo.engine.assets.api.AssetId;
import com.samo.engine.assets.api.AssetType;
import com.samo.engine.assets.api.SourceAssetMetadata;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

final class AssetCooker {
    static final String METADATA_SUFFIX = ".asset.json";
    private static final String MANIFEST_FILE = "manifest.json";
    private static final String ASSETS_DIRECTORY = "assets";

    private AssetCooker() {

    }

    static void cook(Path inputDirectory, Path outputCache) {

        cook(inputDirectory, outputCache, AssetCookerFileSystem.system());

    }

    static void cook(Path inputDirectory, Path outputCache, AssetCookerFileSystem fileSystem) {

        Path input = requireInputDirectory(inputDirectory);
        Path output = requireOutputPath(input, outputCache);
        List<SourceAsset> sources = discoverSources(input);

        boolean outputOwned = false;
        try {
            fileSystem.createDirectory(output);
            outputOwned = true;
            Path assetsDirectory = output.resolve(ASSETS_DIRECTORY);
            fileSystem.createDirectories(assetsDirectory);

            ArrayList<AssetManifestEntry> entries = new ArrayList<>(sources.size());
            for (SourceAsset source : sources) {
                String cookedRelative = ASSETS_DIRECTORY + "/" + source.metadata().assetId() + ".bin";
                Path cookedPath = output.resolve(cookedRelative.replace('/', java.io.File.separatorChar));
                if (source.metadata().assetType() == AssetType.MESH) {
                    fileSystem.writeBytes(cookedPath, CookedMeshBinary.encode(source.engineMeshes()));
                } else if (source.metadata().assetType() == AssetType.TEXTURE) {
                    fileSystem.writeBytes(cookedPath, CookedTextureBinary.encode(source.textureMipLevels()));
                } else if (source.metadata().assetType() == AssetType.AUDIO) {
                    fileSystem.writeBytes(cookedPath, CookedAudioBinary.encode(source.cookedAudio()));
                } else {
                    fileSystem.copy(source.sourcePath(), cookedPath);
                }
                long cookedSize = fileSystem.size(cookedPath);
                if (cookedSize <= 0) {
                    throw new AssetCookerException("Cooked payload is empty for asset " + source.metadata().assetId());
                }
                entries.add(new AssetManifestEntry(source.metadata().assetId(), source.metadata().assetType(), source.relativeSourcePath(), cookedRelative, cookedSize));
            }

            fileSystem.writeString(output.resolve(MANIFEST_FILE), AssetManifestJson.write(entries));
        } catch (IOException exception) {
            if (outputOwned) {
                cleanup(output, fileSystem, exception);
            }
            throw new AssetCookerException("Asset cooking failed: " + exception.getMessage(), exception);
        } catch (RuntimeException exception) {
            if (outputOwned) {
                cleanup(output, fileSystem, exception);
            }
            throw exception;
        }

    }

    private static Path requireInputDirectory(Path inputDirectory) {

        if (inputDirectory == null) {
            throw new NullPointerException("inputDirectory");
        }

        Path input = inputDirectory.toAbsolutePath().normalize();
        if (Files.isSymbolicLink(input) || !Files.isDirectory(input, LinkOption.NOFOLLOW_LINKS) || !Files.isReadable(input)) {
            throw new AssetCookerException(input + ": input must be a readable non-symbolic-link directory");
        }
        return input;

    }

    private static Path requireOutputPath(Path input, Path outputCache) {

        if (outputCache == null) {
            throw new NullPointerException("outputCache");
        }

        Path output = outputCache.toAbsolutePath().normalize();
        if (Files.exists(output, LinkOption.NOFOLLOW_LINKS)) {
            throw new AssetCookerException(output + ": output cache already exists");
        }
        if (output.equals(input) || output.startsWith(input) || input.startsWith(output)) {
            throw new AssetCookerException(output + ": output cache must not overlap the input directory");
        }
        return output;

    }

    private static List<SourceAsset> discoverSources(Path input) {

        ArrayList<Path> metadataPaths = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(input)) {
            paths.forEach(path -> inspectDiscoveredPath(input, path, metadataPaths));
        } catch (IOException exception) {
            throw new AssetCookerException(input + ": failed to scan input directory", exception);
        } catch (UncheckedIOException exception) {
            throw new AssetCookerException(input + ": failed to scan input directory", exception.getCause());
        }

        if (metadataPaths.isEmpty()) {
            throw new AssetCookerException(input + ": no " + METADATA_SUFFIX + " metadata sidecars found");
        }

        metadataPaths.sort(Comparator.comparing(path -> normalizedRelativePath(input, path)));

        ArrayList<SourceAsset> sources = new ArrayList<>(metadataPaths.size());
        Set<AssetId> ids = new HashSet<>();
        for (Path metadataPath : metadataPaths) {
            SourceAsset source = loadSource(input, metadataPath);
            if (!ids.add(source.metadata().assetId())) {
                throw new AssetCookerException(metadataPath + ": duplicate assetId " + source.metadata().assetId());
            }
            sources.add(source);
        }

        sources.sort(Comparator.comparing(source -> source.metadata().assetId().toString()));
        return List.copyOf(sources);

    }

    private static void inspectDiscoveredPath(Path input, Path path, List<Path> metadataPaths) {

        if (path.equals(input)) {
            return;
        }

        String fileName = path.getFileName().toString();
        if (Files.isSymbolicLink(path)) {
            if (fileName.endsWith(METADATA_SUFFIX)) {
                throw new AssetCookerException(path + ": metadata symbolic links are not supported");
            }
            return;
        }

        if (fileName.endsWith(METADATA_SUFFIX) && Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
            metadataPaths.add(path);
        }

    }

    private static SourceAsset loadSource(Path input, Path metadataPath) {

        String metadataName = metadataPath.getFileName().toString();
        String sourceName = metadataName.substring(0, metadataName.length() - METADATA_SUFFIX.length());
        if (sourceName.isEmpty()) {
            throw new AssetCookerException(metadataPath + ": metadata sidecar must name a source file before " + METADATA_SUFFIX);
        }

        Path sourcePath = metadataPath.resolveSibling(sourceName);
        if (Files.isSymbolicLink(sourcePath)) {
            throw new AssetCookerException(sourcePath + ": source symbolic links are not supported");
        }
        if (!Files.isRegularFile(sourcePath, LinkOption.NOFOLLOW_LINKS) || !Files.isReadable(sourcePath)) {
            throw new AssetCookerException(metadataPath + ": paired source file is missing or unreadable: " + sourcePath);
        }

        long sourceSize;
        try {
            sourceSize = Files.size(sourcePath);
        } catch (IOException exception) {
            throw new AssetCookerException(sourcePath + ": failed to inspect source size", exception);
        }
        if (sourceSize <= 0) {
            throw new AssetCookerException(sourcePath + ": source file must be nonzero");
        }

        SourceAssetMetadata metadata = SourceAssetMetadata.load(metadataPath);
        List<EngineMesh> engineMeshes = metadata.assetType() == AssetType.MESH
            ? AssimpGltfMeshImporter.importFile(sourcePath).stream().map(MeshCoordinateConverter::toEngineSpace).toList()
            : List.of();
        List<TextureMipLevel> textureMipLevels = metadata.assetType() == AssetType.TEXTURE ? TextureMipChain.generate(StbTextureImporter.importFile(sourcePath)) : List.of();
        CookedAudio cookedAudio = metadata.assetType() == AssetType.AUDIO ? StbVorbisAudioImporter.importFile(sourcePath) : null;
        return new SourceAsset(sourcePath, metadata, normalizedRelativePath(input, sourcePath), engineMeshes, textureMipLevels, cookedAudio);

    }

    private static String normalizedRelativePath(Path root, Path path) {

        return root.relativize(path).toString().replace('\\', '/');

    }

    private static void cleanup(Path output, AssetCookerFileSystem fileSystem, Throwable originalFailure) {

        try {
            fileSystem.deleteTree(output);
        } catch (IOException cleanupFailure) {
            originalFailure.addSuppressed(cleanupFailure);
        }

    }

    record SourceAsset(Path sourcePath, SourceAssetMetadata metadata, String relativeSourcePath, List<EngineMesh> engineMeshes, List<TextureMipLevel> textureMipLevels,
        CookedAudio cookedAudio) {
        SourceAsset {

            engineMeshes = List.copyOf(engineMeshes);
            textureMipLevels = List.copyOf(textureMipLevels);

        }
    }

    record AssetManifestEntry(AssetId assetId, AssetType assetType, String sourcePath, String cookedPath, long byteSize) {

    }
}
