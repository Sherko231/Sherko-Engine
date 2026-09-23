package com.samo.engine.assets.api;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/** Immutable Java-owned runtime mesh decoded from validated cooked SMES content. */
public record MeshAsset(List<Primitive> primitives) {
    public MeshAsset {

        Objects.requireNonNull(primitives, "primitives");
        if (primitives.isEmpty()) {
            throw new IllegalArgumentException("primitives must not be empty");
        }
        primitives = List.copyOf(primitives);

    }

    /** One immutable indexed mesh primitive in canonical engine space. */
    public static final class Primitive {
        private final int meshIndex;
        private final String name;
        private final float[] positions;
        private final float[] normals;
        private final float[] tangents;
        private final float[] tangentSigns;
        private final float[] uv0;
        private final int[] indices;

        public Primitive(int meshIndex, String name, float[] positions, float[] normals, float[] tangents, float[] tangentSigns, float[] uv0, int[] indices) {

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

        public int meshIndex() {

            return meshIndex;

        }

        public String name() {

            return name;

        }

        public float[] positions() {

            return positions.clone();

        }

        public float[] normals() {

            return normals == null ? null : normals.clone();

        }

        public float[] tangents() {

            return tangents == null ? null : tangents.clone();

        }

        public float[] tangentSigns() {

            return tangentSigns == null ? null : tangentSigns.clone();

        }

        public float[] uv0() {

            return uv0 == null ? null : uv0.clone();

        }

        public int[] indices() {

            return indices.clone();

        }

        @Override
        public boolean equals(Object other) {

            if (this == other) {
                return true;
            }
            if (!(other instanceof Primitive primitive)) {
                return false;
            }
            return meshIndex == primitive.meshIndex && name.equals(primitive.name) && Arrays.equals(positions, primitive.positions) && Arrays.equals(normals, primitive.normals)
                && Arrays.equals(tangents, primitive.tangents) && Arrays.equals(tangentSigns, primitive.tangentSigns) && Arrays.equals(uv0, primitive.uv0)
                && Arrays.equals(indices, primitive.indices);

        }

        @Override
        public int hashCode() {

            int result = Objects.hash(meshIndex, name);
            result = 31 * result + Arrays.hashCode(positions);
            result = 31 * result + Arrays.hashCode(normals);
            result = 31 * result + Arrays.hashCode(tangents);
            result = 31 * result + Arrays.hashCode(tangentSigns);
            result = 31 * result + Arrays.hashCode(uv0);
            result = 31 * result + Arrays.hashCode(indices);
            return result;

        }
    }
}
