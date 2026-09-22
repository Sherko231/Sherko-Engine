package com.samo.engine.assets.internal;

import java.util.Arrays;
import java.util.Objects;

final class ImportedMesh {
    private final int meshIndex;
    private final String name;
    private final float[] positions;
    private final float[] normals;
    private final float[] tangents;
    private final float[] uv0;
    private final int[] indices;

    ImportedMesh(int meshIndex, String name, float[] positions, float[] normals, float[] tangents, float[] uv0, int[] indices) {

        if (meshIndex < 0) {
            throw new IllegalArgumentException("meshIndex must be non-negative");
        }
        this.meshIndex = meshIndex;
        this.name = Objects.requireNonNull(name, "name");
        this.positions = Objects.requireNonNull(positions, "positions").clone();
        this.normals = normals == null ? null : normals.clone();
        this.tangents = tangents == null ? null : tangents.clone();
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

    float[] uv0() {

        return uv0 == null ? null : uv0.clone();

    }

    int[] indices() {

        return indices.clone();

    }

    @Override
    public boolean equals(Object other) {

        if (this == other) {
            return true;
        }
        if (!(other instanceof ImportedMesh that)) {
            return false;
        }
        return meshIndex == that.meshIndex
                && name.equals(that.name)
                && Arrays.equals(positions, that.positions)
                && Arrays.equals(normals, that.normals)
                && Arrays.equals(tangents, that.tangents)
                && Arrays.equals(uv0, that.uv0)
                && Arrays.equals(indices, that.indices);

    }

    @Override
    public int hashCode() {

        int result = Objects.hash(meshIndex, name);
        result = 31 * result + Arrays.hashCode(positions);
        result = 31 * result + Arrays.hashCode(normals);
        result = 31 * result + Arrays.hashCode(tangents);
        result = 31 * result + Arrays.hashCode(uv0);
        result = 31 * result + Arrays.hashCode(indices);
        return result;

    }
}
