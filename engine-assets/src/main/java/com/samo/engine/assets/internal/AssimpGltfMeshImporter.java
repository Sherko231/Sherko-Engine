package com.samo.engine.assets.internal;

import java.nio.IntBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.AIFace;
import org.lwjgl.assimp.AIMaterial;
import org.lwjgl.assimp.AIMesh;
import org.lwjgl.assimp.AIScene;
import org.lwjgl.assimp.AIString;
import org.lwjgl.assimp.AIVector3D;
import org.lwjgl.assimp.Assimp;

final class AssimpGltfMeshImporter {
    private AssimpGltfMeshImporter() {

    }

    static List<ImportedMesh> importFile(Path sourcePath) {

        Objects.requireNonNull(sourcePath, "sourcePath");
        requireSupportedExtension(sourcePath);

        AIScene scene = Assimp.aiImportFile(sourcePath.toString(), Assimp.aiProcess_CalcTangentSpace);
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
                meshes.add(copyMesh(sourcePath, meshIndex, mesh, scene));
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

    private static ImportedMesh copyMesh(Path sourcePath, int meshIndex, AIMesh mesh, AIScene scene) {

        int vertexCount = mesh.mNumVertices();
        if (vertexCount <= 0) {
            throw meshFailure(sourcePath, meshIndex, mesh, "mesh contains no vertices");
        }

        AIVector3D.Buffer vertices = mesh.mVertices();
        if (vertices == null) {
            throw meshFailure(sourcePath, meshIndex, mesh, "mesh has no position buffer");
        }

        boolean tangentSpaceRequired = tangentSpaceRequired(sourcePath, meshIndex, mesh, scene);
        AIVector3D.Buffer normalBuffer = mesh.mNormals();
        AIVector3D.Buffer tangentBuffer = mesh.mTangents();
        AIVector3D.Buffer bitangentBuffer = mesh.mBitangents();
        AIVector3D.Buffer uv0Buffer = mesh.mTextureCoords(0);

        if (tangentSpaceRequired) {
            if (normalBuffer == null) {
                throw meshFailure(sourcePath, meshIndex, mesh, "tangent-space normal map requires authored normals");
            }
            if (uv0Buffer == null) {
                throw meshFailure(sourcePath, meshIndex, mesh, "tangent-space normal map requires UV0");
            }
            if (tangentBuffer == null || bitangentBuffer == null) {
                throw meshFailure(sourcePath, meshIndex, mesh, "tangent-space normal map requires tangent generation result");
            }
        }

        float[] positions = copyVec3(vertices, vertexCount);
        float[] normals = copyOptionalVec3(normalBuffer, vertexCount, sourcePath, meshIndex, mesh, "normal");
        TangentData tangentData = copyTangents(tangentBuffer, bitangentBuffer, normalBuffer, vertexCount, sourcePath, meshIndex, mesh);
        float[] uv0 = copyOptionalUv0(uv0Buffer, vertexCount, sourcePath, meshIndex, mesh);
        int[] indices = copyTriangleIndices(sourcePath, meshIndex, mesh, vertexCount);

        return new ImportedMesh(meshIndex, mesh.mName().dataString(), positions, normals, tangentData.xyz(), tangentData.signs(), uv0, indices);

    }

    private static boolean tangentSpaceRequired(Path sourcePath, int meshIndex, AIMesh mesh, AIScene scene) {

        int materialIndex = mesh.mMaterialIndex();
        int materialCount = scene.mNumMaterials();
        PointerBuffer materialPointers = scene.mMaterials();
        if (materialIndex < 0 || materialIndex >= materialCount || materialPointers == null) {
            throw meshFailure(sourcePath, meshIndex, mesh, "material index " + materialIndex + " is outside material range 0.." + Math.max(materialCount - 1, 0));
        }

        AIMaterial material = AIMaterial.create(materialPointers.get(materialIndex));
        int normalTextureCount = Assimp.aiGetMaterialTextureCount(material, Assimp.aiTextureType_NORMALS);
        if (normalTextureCount <= 0) {
            return false;
        }

        for (int textureIndex = 0; textureIndex < normalTextureCount; textureIndex++) {
            int uvIndex = normalTextureUvIndex(sourcePath, meshIndex, mesh, material, textureIndex);
            if (uvIndex != 0) {
                throw meshFailure(sourcePath, meshIndex, mesh, "tangent-space normal map uses unsupported UV channel " + uvIndex + "; only UV0 is supported");
            }
        }
        return true;

    }

    private static int normalTextureUvIndex(Path sourcePath, int meshIndex, AIMesh mesh, AIMaterial material, int textureIndex) {

        int[] uvIndex = new int[]{0};
        try (AIString texturePath = AIString.calloc()) {
            int result = Assimp.aiGetMaterialTexture(material, Assimp.aiTextureType_NORMALS, textureIndex, texturePath, null, uvIndex, null, null, null, null);
            if (result != Assimp.aiReturn_SUCCESS) {
                throw meshFailure(sourcePath, meshIndex, mesh, "failed to inspect tangent-space normal map UV channel");
            }
        }
        return uvIndex[0];

    }

    private static TangentData copyTangents(AIVector3D.Buffer tangents, AIVector3D.Buffer bitangents, AIVector3D.Buffer normals, int vertexCount, Path sourcePath, int meshIndex,
        AIMesh mesh) {

        if (tangents == null && bitangents == null) {
            return TangentData.absent();
        }
        if (tangents == null || bitangents == null) {
            throw meshFailure(sourcePath, meshIndex, mesh, "tangent and bitangent buffers must both be present or absent");
        }
        if (normals == null) {
            throw meshFailure(sourcePath, meshIndex, mesh, "tangent data requires normals");
        }
        if (tangents.remaining() < vertexCount || bitangents.remaining() < vertexCount || normals.remaining() < vertexCount) {
            throw meshFailure(sourcePath, meshIndex, mesh, "tangent-space buffer is shorter than vertex count");
        }

        float[] xyz = new float[Math.multiplyExact(vertexCount, 3)];
        float[] signs = new float[vertexCount];
        for (int index = 0; index < vertexCount; index++) {
            AIVector3D normal = normals.get(index);
            AIVector3D tangent = tangents.get(index);
            AIVector3D bitangent = bitangents.get(index);
            requireFiniteTangentSpace(sourcePath, meshIndex, mesh, index, normal, tangent, bitangent);

            int base = index * 3;
            xyz[base] = tangent.x();
            xyz[base + 1] = tangent.y();
            xyz[base + 2] = tangent.z();

            float crossX = normal.y() * tangent.z() - normal.z() * tangent.y();
            float crossY = normal.z() * tangent.x() - normal.x() * tangent.z();
            float crossZ = normal.x() * tangent.y() - normal.y() * tangent.x();
            float handedness = crossX * bitangent.x() + crossY * bitangent.y() + crossZ * bitangent.z();
            if (!Float.isFinite(handedness) || handedness == 0.0f) {
                throw meshFailure(sourcePath, meshIndex, mesh, "tangent-space handedness is degenerate at vertex " + index);
            }
            signs[index] = handedness < 0.0f ? -1.0f : 1.0f;
        }
        return new TangentData(xyz, signs);

    }

    private static void requireFiniteTangentSpace(Path sourcePath, int meshIndex, AIMesh mesh, int vertexIndex, AIVector3D normal, AIVector3D tangent, AIVector3D bitangent) {

        if (!finite(normal) || !finite(tangent) || !finite(bitangent)) {
            throw meshFailure(sourcePath, meshIndex, mesh, "tangent-space data contains non-finite values at vertex " + vertexIndex);
        }

    }

    private static boolean finite(AIVector3D vector) {

        return Float.isFinite(vector.x()) && Float.isFinite(vector.y()) && Float.isFinite(vector.z());

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

    private record TangentData(float[] xyz, float[] signs) {
        private static TangentData absent() {

            return new TangentData(null, null);

        }
    }
}
