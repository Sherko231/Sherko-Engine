package com.samo.engine.assets.internal;

import java.nio.file.Path;

/** Command-line entry point for the Phase 6 asset cooker. */
public final class AssetCookerMain {
    private AssetCookerMain() {

    }

    public static void main(String[] args) {

        if (args.length != 2) {
            throw new IllegalArgumentException("Expected exactly two arguments: <input-directory> <output-cache>");
        }

        AssetCooker.cook(Path.of(args[0]), Path.of(args[1]));

    }
}
