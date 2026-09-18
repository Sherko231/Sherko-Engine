package com.samo.engine.render.opengl.internal;

import com.samo.engine.core.api.NativeResourceRegistry;
import com.samo.engine.platform.api.OpenGlThreadGuard;

final class OpenGlFramebuffer implements AutoCloseable {
    private final OwnedOpenGlHandle owned;

    private OpenGlFramebuffer(OwnedOpenGlHandle owned) {
        this.owned = owned;
    }

    static OpenGlFramebuffer create(
            OpenGlThreadGuard guard,
            NativeResourceRegistry registry,
            OpenGlResourceBackend backend) {
        guard.assertOwnerThread();
        int handle = backend.createFramebuffer();
        return new OpenGlFramebuffer(OwnedOpenGlHandle.register(
                "OpenGL framebuffer", handle, guard, registry, backend::deleteFramebuffer));
    }

    @Override
    public void close() {
        owned.close();
    }
}
