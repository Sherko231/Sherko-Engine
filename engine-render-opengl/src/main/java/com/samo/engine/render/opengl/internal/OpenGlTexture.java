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

    static OpenGlTexture createRgba8(
            OpenGlThreadGuard guard,
            NativeResourceRegistry registry,
            OpenGlResourceBackend backend,
            TextureColorEncoding colorEncoding,
            int width,
            int height,
            java.nio.ByteBuffer rgbaBytes) {
        if (colorEncoding == null) {
            throw new NullPointerException("colorEncoding");
        }
        if (rgbaBytes == null) {
            throw new NullPointerException("rgbaBytes");
        }
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("texture dimensions must be positive");
        }
        int expectedBytes = Math.multiplyExact(Math.multiplyExact(width, height), 4);
        if (rgbaBytes.remaining() != expectedBytes) {
            throw new IllegalArgumentException(
                    "RGBA8 byte count mismatch: expected=" + expectedBytes + " actual=" + rgbaBytes.remaining());
        }

        OpenGlTexture texture = create(guard, registry, backend);
        try {
            backend.allocateRgba8Texture(
                    texture.handle(),
                    colorEncoding,
                    width,
                    height,
                    rgbaBytes);
            return texture;
        } catch (RuntimeException | Error failure) {
            CleanupFailureSuppression.runAndSuppress(failure, texture::close);
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
