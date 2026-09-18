package com.samo.engine.render.opengl.internal;

import com.samo.engine.core.api.NativeResourceRegistry;
import com.samo.engine.platform.api.OpenGlThreadGuard;

final class OpenGlTexture implements AutoCloseable {
    private final OwnedOpenGlHandle owned;

    private OpenGlTexture(OwnedOpenGlHandle owned) {
        this.owned = owned;
    }

    static OpenGlTexture create(OpenGlThreadGuard guard, NativeResourceRegistry registry, OpenGlResourceBackend backend) {
        guard.assertOwnerThread();
        int handle = backend.createTexture();
        return new OpenGlTexture(OwnedOpenGlHandle.register(
                "OpenGL texture", handle, guard, registry, backend::deleteTexture));
    }

    @Override
    public void close() {
        owned.close();
    }
}
