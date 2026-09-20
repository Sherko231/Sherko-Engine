package com.samo.engine.render.opengl.internal;

import com.samo.engine.core.api.NativeResourceRegistry;
import com.samo.engine.platform.api.OpenGlThreadGuard;
import java.util.Objects;

final class OpenGlProgram implements AutoCloseable {
    private final OwnedOpenGlHandle owned;

    private OpenGlProgram(OwnedOpenGlHandle owned) {
        this.owned = owned;
    }

    static OpenGlProgram link(
            OpenGlShader vertexShader,
            OpenGlShader fragmentShader,
            OpenGlThreadGuard guard,
            NativeResourceRegistry registry,
            OpenGlResourceBackend backend) {
        return link("<program>", vertexShader, fragmentShader, guard, registry, backend);
    }

    static OpenGlProgram link(
            String programLabel,
            OpenGlShader vertexShader,
            OpenGlShader fragmentShader,
            OpenGlThreadGuard guard,
            NativeResourceRegistry registry,
            OpenGlResourceBackend backend) {
        String label = Objects.requireNonNull(programLabel, "programLabel");
        OpenGlShader vertex = Objects.requireNonNull(vertexShader, "vertexShader");
        OpenGlShader fragment = Objects.requireNonNull(fragmentShader, "fragmentShader");
        guard.assertOwnerThread();

        int handle = backend.createProgram();
        if (handle == 0) {
            throw new IllegalStateException("OpenGL program creation returned handle 0");
        }

        boolean vertexAttached = false;
        boolean fragmentAttached = false;
        try {
            backend.attachShader(handle, vertex.handle());
            vertexAttached = true;
            backend.attachShader(handle, fragment.handle());
            fragmentAttached = true;
            backend.linkProgram(handle);
            if (!backend.programLinkSucceeded(handle)) {
                throw new IllegalStateException(
                        "OpenGL program link failed for " + label + ": " + backend.programInfoLog(handle));
            }
            backend.detachShader(handle, vertex.handle());
            vertexAttached = false;
            backend.detachShader(handle, fragment.handle());
            fragmentAttached = false;
        } catch (RuntimeException | Error failure) {
            if (fragmentAttached) {
                suppressCleanup(failure, () -> backend.detachShader(handle, fragment.handle()));
            }
            if (vertexAttached) {
                suppressCleanup(failure, () -> backend.detachShader(handle, vertex.handle()));
            }
            suppressCleanup(failure, () -> backend.deleteProgram(handle));
            throw failure;
        }

        return new OpenGlProgram(OwnedOpenGlHandle.register(
                "OpenGL program", handle, guard, registry, backend::deleteProgram));
    }

    int handle() {
        return owned.handle();
    }

    private static void suppressCleanup(Throwable failure, Runnable cleanup) {
        CleanupFailureSuppression.runAndSuppress(failure, cleanup);
    }

    @Override
    public void close() {
        owned.close();
    }
}
