package com.samo.engine.world.internal;

import com.samo.engine.world.api.EntityGuid;
import java.util.Objects;

record SceneEntityDocument(EntityGuid guid, EntityGuid parentGuid, SceneComponentsDocument components) {
    SceneEntityDocument {

        Objects.requireNonNull(guid, "guid");
        Objects.requireNonNull(components, "components");

    }
}
