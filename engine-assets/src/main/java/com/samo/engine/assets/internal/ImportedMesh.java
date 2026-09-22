package com.samo.engine.assets.internal;

import java.util.Objects;

final class ImportedMesh {
    private final int meshIndex;
    private final String name;
    private final float[] positions;
    private final float[] normals;
    private final float[] tangents;
    private final float[] tangentSigns;
    private final float[] uv0;
    private final int[] indices;

    ImportedMesh(int meshIndex, String name, float[] positions, float[] normals, float[] tangents, float[] tangentSigns, float[] uv0, int[] indices) {

        if (meshIndex < 0) {
            throw new IllegalArgumentException("meshIndex must be non-negative");
        }
        if ((tangents == null) != (tangentSigns == null)) {
            throw new IllegalArgumentException("tangents and tangentSigns must both be present or absent");
        }
        this.meshIndex = meshIndex;
        this.name = Objects.requireNonNull(name, "name");
        this.positions = Objects.requireNonNull(positions, "positions").clone();
        this.normals = normals == null ? null : normals.clone();
        this.tangents = tangents == null ? null : tangents.clone();
        this.tangentSigns = tangentSigns == null ? null : tangentSigns.clone();
        this.uv0 = uv0 == null ? null : uv0.clone();
        this.indices = Objects.requireNonNull(indices, "indices").clone();

    }

    int meshIndex() {

        return meshIndex;

    }

    String name() {

        return name;

    }

    float[] positions() {

        return positions.clone();

    }

    float[] normals() {

        return normals == null ? null : normals.clone();

    }

    float[] tangents() {

        return tangents == null ? null : tangents.clone();

    }

    float[] tangentSigns() {

        return tangentSigns == null ? null : tangentSigns.clone();

    }

    float[] uv0() {

        return uv0 == null ? null : uv0.clone();

    }

    int[] indices() {

        return indices.clone();

    }

}
