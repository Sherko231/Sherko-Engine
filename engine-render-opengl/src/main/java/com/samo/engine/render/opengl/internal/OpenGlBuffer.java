package com.samo.engine.render.opengl.internal;

import com.samo.engine.core.api.NativeResourceRegistry;
import com.samo.engine.platform.api.OpenGlThreadGuard;

final class OpenGlBuffer implements AutoCloseable {
    private final OwnedOpenGlHandle owned;

    private OpenGlBuffer(OwnedOpenGlHandle owned) {
        this.owned = owned;
    }

    static OpenGlBuffer create(OpenGlThreadGuard guard, NativeResourceRegistry registry, OpenGlResourceBackend backend) {
        guard.assertOwnerThread();
        int handle = backend.createBuffer();
        return new OpenGlBuffer(OwnedOpenGlHandle.register("OpenGL buffer", handle, guard, registry, backend::deleteBuffer));
    }

    int handle() {
        return owned.handle();
    }

    @Override
    public void close() {
        owned.close();
    }
}
