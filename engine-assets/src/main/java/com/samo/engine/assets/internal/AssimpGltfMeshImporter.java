package com.samo.engine.assets.internal;

import java.nio.IntBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.AIFace;
import org.lwjgl.assimp.AIMesh;
import org.lwjgl.assimp.AIScene;
import org.lwjgl.assimp.AIVector3D;
import org.lwjgl.assimp.Assimp;

final class AssimpGltfMeshImporter {
    private AssimpGltfMeshImporter() {

    }

    static List<ImportedMesh> importFile(Path sourcePath) {

        Objects.requireNonNull(sourcePath, "sourcePath");
        requireSupportedExtension(sourcePath);

        AIScene scene = Assimp.aiImportFile(sourcePath.toString(), 0);
        if (scene == null) {
            String diagnostic = Assimp.aiGetErrorString();
            throw new AssetCookerException(sourcePath + ": Assimp failed to import glTF" + diagnosticSuffix(diagnostic));
        }

        try {
            int meshCount = scene.mNumMeshes();
            PointerBuffer meshPointers = scene.mMeshes();
            if (meshCount <= 0 || meshPointers == null) {
                throw new AssetCookerException(sourcePath + ": Assimp imported no meshes");
            }

            ArrayList<ImportedMesh> meshes = new ArrayList<>(meshCount);
            for (int meshIndex = 0; meshIndex < meshCount; meshIndex++) {
                AIMesh mesh = AIMesh.create(meshPointers.get(meshIndex));
                meshes.add(copyMesh(sourcePath, meshIndex, mesh));
            }
            return List.copyOf(meshes);
        } finally {
            Assimp.aiReleaseImport(scene);
        }

    }

    private static void requireSupportedExtension(Path sourcePath) {

        String fileName = sourcePath.getFileName().toString().toLowerCase(Locale.ROOT);
        if (!fileName.endsWith(".gltf") && !fileName.endsWith(".glb")) {
            throw new AssetCookerException(sourcePath + ": MESH source must use .gltf or .glb");
        }

    }

    private static ImportedMesh copyMesh(Path sourcePath, int meshIndex, AIMesh mesh) {

        int vertexCount = mesh.mNumVertices();
        if (vertexCount <= 0) {
            throw meshFailure(sourcePath, meshIndex, mesh, "mesh contains no vertices");
        }

        AIVector3D.Buffer vertices = mesh.mVertices();
        if (vertices == null) {
            throw meshFailure(sourcePath, meshIndex, mesh, "mesh has no position buffer");
        }

        float[] positions = copyVec3(vertices, vertexCount);
        float[] normals = copyOptionalVec3(mesh.mNormals(), vertexCount, sourcePath, meshIndex, mesh, "normal");
        float[] tangents = copyOptionalVec3(mesh.mTangents(), vertexCount, sourcePath, meshIndex, mesh, "tangent");
        float[] uv0 = copyOptionalUv0(mesh.mTextureCoords(0), vertexCount, sourcePath, meshIndex, mesh);
        int[] indices = copyTriangleIndices(sourcePath, meshIndex, mesh, vertexCount);

        return new ImportedMesh(meshIndex, mesh.mName().dataString(), positions, normals, tangents, uv0, indices);

    }

    private static float[] copyVec3(AIVector3D.Buffer vectors, int count) {

        float[] values = new float[Math.multiplyExact(count, 3)];
        for (int index = 0; index < count; index++) {
            AIVector3D vector = vectors.get(index);
            int base = index * 3;
            values[base] = vector.x();
            values[base + 1] = vector.y();
            values[base + 2] = vector.z();
        }
        return values;

    }

    private static float[] copyOptionalVec3(AIVector3D.Buffer vectors, int vertexCount, Path sourcePath, int meshIndex, AIMesh mesh, String label) {

        if (vectors == null) {
            return null;
        }
        if (vectors.remaining() < vertexCount) {
            throw meshFailure(sourcePath, meshIndex, mesh, label + " buffer is shorter than vertex count");
        }
        return copyVec3(vectors, vertexCount);

    }

    private static float[] copyOptionalUv0(AIVector3D.Buffer vectors, int vertexCount, Path sourcePath, int meshIndex, AIMesh mesh) {

        if (vectors == null) {
            return null;
        }
        if (vectors.remaining() < vertexCount) {
            throw meshFailure(sourcePath, meshIndex, mesh, "UV0 buffer is shorter than vertex count");
        }

        float[] values = new float[Math.multiplyExact(vertexCount, 2)];
        for (int index = 0; index < vertexCount; index++) {
            AIVector3D vector = vectors.get(index);
            int base = index * 2;
            values[base] = vector.x();
            values[base + 1] = vector.y();
        }
        return values;

    }

    private static int[] copyTriangleIndices(Path sourcePath, int meshIndex, AIMesh mesh, int vertexCount) {

        int faceCount = mesh.mNumFaces();
        AIFace.Buffer faces = mesh.mFaces();
        if (faceCount <= 0 || faces == null) {
            throw meshFailure(sourcePath, meshIndex, mesh, "mesh contains no faces");
        }

        int[] indices = new int[Math.multiplyExact(faceCount, 3)];
        for (int faceIndex = 0; faceIndex < faceCount; faceIndex++) {
            AIFace face = faces.get(faceIndex);
            if (face.mNumIndices() != 3) {
                throw meshFailure(sourcePath, meshIndex, mesh, "face " + faceIndex + " must contain exactly three indices");
            }

            IntBuffer faceIndices = face.mIndices();
            for (int corner = 0; corner < 3; corner++) {
                int vertexIndex = faceIndices.get(corner);
                if (vertexIndex < 0 || vertexIndex >= vertexCount) {
                    throw meshFailure(sourcePath, meshIndex, mesh, "face " + faceIndex + " index " + vertexIndex + " is outside vertex range 0.." + (vertexCount - 1));
                }
                indices[faceIndex * 3 + corner] = vertexIndex;
            }
        }
        return indices;

    }

    private static AssetCookerException meshFailure(Path sourcePath, int meshIndex, AIMesh mesh, String message) {

        String name = mesh.mName().dataString();
        String descriptor = name.isEmpty() ? "mesh[" + meshIndex + "]" : "mesh[" + meshIndex + "] '" + name + "'";
        return new AssetCookerException(sourcePath + ": " + descriptor + ": " + message);

    }

    private static String diagnosticSuffix(String diagnostic) {

        return diagnostic == null || diagnostic.isBlank() ? "" : ": " + diagnostic;

    }
}
