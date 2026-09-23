package com.samo.engine.assets.internal;

import com.samo.engine.assets.api.MeshAsset;
import java.util.List;

final class RuntimeMeshAdapter {
    private RuntimeMeshAdapter() {

    }

    static MeshAsset toPublic(List<EngineMesh> meshes) {

        return new MeshAsset(meshes.stream().map(RuntimeMeshAdapter::toPublic).toList());

    }

    private static MeshAsset.Primitive toPublic(EngineMesh mesh) {

        return new MeshAsset.Primitive(mesh.meshIndex(), mesh.name(), mesh.positions(), mesh.normals(), mesh.tangents(), mesh.tangentSigns(), mesh.uv0(), mesh.indices());

    }
}
