package com.samo.engine.render.opengl.internal;

import com.samo.engine.core.api.NativeResourceRegistry;
import com.samo.engine.platform.api.OpenGlThreadGuard;
import java.util.Objects;
import org.lwjgl.opengl.GL20;

final class OpenGlShader implements AutoCloseable {
    enum Stage {
        VERTEX(GL20.GL_VERTEX_SHADER),
        FRAGMENT(GL20.GL_FRAGMENT_SHADER);

        private final int nativeType;

        Stage(int nativeType) {
            this.nativeType = nativeType;
        }
    }

    private final OwnedOpenGlHandle owned;

    private OpenGlShader(OwnedOpenGlHandle owned) {
        this.owned = owned;
    }

    static OpenGlShader compile(
            Stage stage,
            String source,
            OpenGlThreadGuard guard,
            NativeResourceRegistry registry,
            OpenGlResourceBackend backend) {
        Stage shaderStage = Objects.requireNonNull(stage, "stage");
        String shaderSource = Objects.requireNonNull(source, "source");
        guard.assertOwnerThread();

        int handle = backend.createShader(shaderStage.nativeType);
        if (handle == 0) {
            throw new IllegalStateException("OpenGL shader creation returned handle 0");
        }

        try {
            backend.shaderSource(handle, shaderSource);
            backend.compileShader(handle);
            if (!backend.shaderCompileSucceeded(handle)) {
                throw new IllegalStateException("OpenGL shader compilation failed: " + backend.shaderInfoLog(handle));
            }
        } catch (RuntimeException | Error failure) {
            try {
                backend.deleteShader(handle);
            } catch (RuntimeException | Error cleanupFailure) {
                failure.addSuppressed(cleanupFailure);
            }
            throw failure;
        }

        return new OpenGlShader(OwnedOpenGlHandle.register(
                "OpenGL shader", handle, guard, registry, backend::deleteShader));
    }

    int handle() {
        return owned.handle();
    }

    @Override
    public void close() {
        owned.close();
    }
}
