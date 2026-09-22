package com.samo.engine.assets.internal;

import java.util.Objects;

final class MeshCoordinateConverter {
    private MeshCoordinateConverter() {

    }

    static EngineMesh toEngineSpace(ImportedMesh importedMesh) {

        Objects.requireNonNull(importedMesh, "importedMesh");

        float[] positions = importedMesh.positions();
        validateVec3Array(importedMesh, "positions", positions, true);
        int vertexCount = positions.length / 3;

        float[] normals = importedMesh.normals();
        validateOptionalVec3Array(importedMesh, "normals", normals, positions.length);

        float[] tangents = importedMesh.tangents();
        validateOptionalVec3Array(importedMesh, "tangents", tangents, positions.length);

        float[] uv0 = importedMesh.uv0();
        if (uv0 != null && uv0.length != Math.multiplyExact(vertexCount, 2)) {
            throw failure(importedMesh, "UV0 length must equal vertexCount * 2");
        }

        float[] enginePositions = convertVec3(importedMesh, "positions", positions);
        float[] engineNormals = normals == null ? null : convertVec3(importedMesh, "normals", normals);
        float[] engineTangents = tangents == null ? null : convertVec3(importedMesh, "tangents", tangents);

        return new EngineMesh(importedMesh.meshIndex(), importedMesh.name(), enginePositions, engineNormals, engineTangents, uv0, importedMesh.indices());

    }

    private static void validateVec3Array(ImportedMesh mesh, String label, float[] values, boolean requireNonEmpty) {

        if (requireNonEmpty && values.length == 0) {
            throw failure(mesh, label + " must not be empty");
        }
        if (values.length % 3 != 0) {
            throw failure(mesh, label + " length must be divisible by 3");
        }

    }

    private static void validateOptionalVec3Array(ImportedMesh mesh, String label, float[] values, int positionLength) {

        if (values == null) {
            return;
        }
        if (values.length != positionLength) {
            throw failure(mesh, label + " length must equal positions length");
        }
        validateVec3Array(mesh, label, values, false);

    }

    private static float[] convertVec3(ImportedMesh mesh, String label, float[] values) {

        float[] converted = new float[values.length];
        for (int index = 0; index < values.length; index += 3) {
            float x = values[index];
            float y = values[index + 1];
            float z = values[index + 2];
            requireFinite(mesh, label, index / 3, x, y, z);
            converted[index] = -x;
            converted[index + 1] = y;
            converted[index + 2] = -z;
        }
        return converted;

    }

    private static void requireFinite(ImportedMesh mesh, String label, int vertexIndex, float x, float y, float z) {

        if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z)) {
            throw failure(mesh, label + " vertex " + vertexIndex + " contains non-finite spatial data");
        }

    }

    private static IllegalArgumentException failure(ImportedMesh mesh, String message) {

        String name = mesh.name().isEmpty() ? "" : " '" + mesh.name() + "'";
        return new IllegalArgumentException("mesh[" + mesh.meshIndex() + "]" + name + ": " + message);

    }
}
