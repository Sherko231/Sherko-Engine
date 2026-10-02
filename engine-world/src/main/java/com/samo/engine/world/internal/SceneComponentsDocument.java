package com.samo.engine.world.internal;

import com.samo.engine.world.api.AudioEmitterComponent;
import com.samo.engine.world.api.CameraComponent;
import com.samo.engine.world.api.MeshRendererComponent;
import com.samo.engine.world.api.NameComponent;

record SceneComponentsDocument(SceneTransformData transform, NameComponent name, MeshRendererComponent meshRenderer, CameraComponent camera, AudioEmitterComponent audioEmitter) {
    static final SceneComponentsDocument EMPTY = new SceneComponentsDocument(null, null, null, null, null);
}
