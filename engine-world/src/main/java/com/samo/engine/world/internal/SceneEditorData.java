package com.samo.engine.world.internal;

import java.util.Objects;

record SceneEditorData(String jsonObject) {
    SceneEditorData {

        Objects.requireNonNull(jsonObject, "jsonObject");
        if (jsonObject.isBlank()) {
            throw new IllegalArgumentException("jsonObject must not be blank");
        }

    }
}
