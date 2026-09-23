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
        for (Primitive primitive : primitives) {
            Objects.requireNonNull(primitive, "primitive");
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
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(positions, "positions");
            Objects.requireNonNull(indices, "indices");
            if (positions.length == 0 || positions.length % 3 != 0) {
                throw new IllegalArgumentException("positions length must be a positive multiple of three");
            }
            int vertexCount = positions.length / 3;
            requireFinite(positions, "positions");
            if (normals != null) {
                requireLength(normals, positions.length, "normals");
                requireFinite(normals, "normals");
            }
            if (tangents != null) {
                requireLength(tangents, positions.length, "tangents");
                requireFinite(tangents, "tangents");
                requireLength(tangentSigns, vertexCount, "tangentSigns");
                for (float sign : tangentSigns) {
                    if (sign != 1.0f && sign != -1.0f) {
                        throw new IllegalArgumentException("tangentSigns values must be exactly +1 or -1");
                    }
                }
            }
            if (uv0 != null) {
                requireLength(uv0, Math.multiplyExact(vertexCount, 2), "uv0");
                requireFinite(uv0, "uv0");
            }
            if (indices.length == 0 || indices.length % 3 != 0) {
                throw new IllegalArgumentException("indices length must be positive and divisible by three");
            }
            for (int index : indices) {
                if (index < 0 || index >= vertexCount) {
                    throw new IllegalArgumentException("index is outside vertex range");
                }
            }

            this.meshIndex = meshIndex;
            this.name = name;
            this.positions = positions.clone();
            this.normals = normals == null ? null : normals.clone();
            this.tangents = tangents == null ? null : tangents.clone();
            this.tangentSigns = tangentSigns == null ? null : tangentSigns.clone();
            this.uv0 = uv0 == null ? null : uv0.clone();
            this.indices = indices.clone();

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

        private static void requireLength(float[] values, int expected, String label) {

            if (values.length != expected) {
                throw new IllegalArgumentException(label + " length must equal " + expected);
            }

        }

        private static void requireFinite(float[] values, String label) {

            for (int index = 0; index < values.length; index++) {
                if (!Float.isFinite(values[index])) {
                    throw new IllegalArgumentException(label + "[" + index + "] must be finite");
                }
            }

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
