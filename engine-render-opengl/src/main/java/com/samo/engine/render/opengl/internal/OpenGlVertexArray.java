package com.samo.engine.render.opengl.internal;

import com.samo.engine.core.api.NativeResourceRegistry;
import com.samo.engine.platform.api.OpenGlThreadGuard;

final class OpenGlVertexArray implements AutoCloseable {
    private final OwnedOpenGlHandle owned;

    private OpenGlVertexArray(OwnedOpenGlHandle owned) {
        this.owned = owned;
    }

    static OpenGlVertexArray create(OpenGlThreadGuard guard, NativeResourceRegistry registry, OpenGlResourceBackend backend) {
        guard.assertOwnerThread();
        int handle = backend.createVertexArray();
        return new OpenGlVertexArray(OwnedOpenGlHandle.register("OpenGL vertex array", handle, guard, registry, backend::deleteVertexArray));
    }

    int handle() {
        return owned.handle();
    }

    @Override
    public void close() {
        owned.close();
    }
}
