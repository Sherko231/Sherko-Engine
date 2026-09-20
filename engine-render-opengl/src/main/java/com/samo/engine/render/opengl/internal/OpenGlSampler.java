package com.samo.engine.render.opengl.internal;

import com.samo.engine.core.api.NativeResourceRegistry;
import com.samo.engine.platform.api.OpenGlThreadGuard;

final class OpenGlSampler implements AutoCloseable {
    private final OwnedOpenGlHandle owned;

    private OpenGlSampler(OwnedOpenGlHandle owned) {
        this.owned = owned;
    }

    static OpenGlSampler create(OpenGlThreadGuard guard, NativeResourceRegistry registry, OpenGlResourceBackend backend) {
        guard.assertOwnerThread();
        int handle = backend.createSampler();
        return new OpenGlSampler(OwnedOpenGlHandle.register(
                "OpenGL sampler", handle, guard, registry, backend::deleteSampler));
    }

    static OpenGlSampler createLinearClamp(
            OpenGlThreadGuard guard,
            NativeResourceRegistry registry,
            OpenGlResourceBackend backend) {
        OpenGlSampler sampler = create(guard, registry, backend);
        try {
            backend.configureLinearClampSampler(sampler.handle());
            return sampler;
        } catch (RuntimeException | Error failure) {
            CleanupFailureSuppression.runAndSuppress(failure, sampler::close);
            throw failure;
        }
    }

    int handle() {
        return owned.handle();
    }

    @Override
    public void close() {
        owned.close();
    }
}
