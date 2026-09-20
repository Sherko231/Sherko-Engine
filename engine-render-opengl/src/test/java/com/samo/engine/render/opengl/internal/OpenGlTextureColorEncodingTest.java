package com.samo.engine.render.opengl.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.samo.engine.core.api.EngineLogger;
import com.samo.engine.core.api.NativeResourceRegistry;
import com.samo.engine.platform.api.GlfwWindow;
import com.samo.engine.platform.api.OpenGlThreadGuard;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL21;

class OpenGlTextureColorEncodingTest {
    @Test
    void mapsDisplayColorAndLinearDataToDistinctOpenGlFormats() {
        assertEquals(GL21.GL_SRGB8_ALPHA8, TextureColorEncoding.SRGB_COLOR.internalFormat());
        assertEquals(GL11.GL_RGBA8, TextureColorEncoding.LINEAR_DATA.internalFormat());
    }

    @Test
    void forwardsRequestedEncodingAndPreservesOwnedCleanup() {
        OpenGlThreadGuard guard = boundGuard();
        NativeResourceRegistry registry = new NativeResourceRegistry();
        FakeBackend backend = new FakeBackend();

        try (OpenGlTexture color = OpenGlTexture.createRgba8(guard, registry, backend, TextureColorEncoding.SRGB_COLOR, 1, 1, rgba(128, 128, 128, 255));
            OpenGlTexture data = OpenGlTexture.createRgba8(guard, registry, backend, TextureColorEncoding.LINEAR_DATA, 1, 1, rgba(128, 128, 128, 255))) {
            assertEquals(TextureColorEncoding.SRGB_COLOR, backend.firstEncoding);
            assertEquals(TextureColorEncoding.LINEAR_DATA, backend.secondEncoding);
        }

        registry.assertNoOpenResources();
        assertEquals(2, backend.deletedTextures);
    }

    @Test
    void rejectsWrongRgbaByteCountBeforeNativeCreation() {
        OpenGlThreadGuard guard = boundGuard();
        NativeResourceRegistry registry = new NativeResourceRegistry();
        FakeBackend backend = new FakeBackend();

        assertThrows(IllegalArgumentException.class,
            () -> OpenGlTexture.createRgba8(guard, registry, backend, TextureColorEncoding.SRGB_COLOR, 1, 1, ByteBuffer.allocateDirect(3)));

        assertEquals(0, backend.createdTextures);
        registry.assertNoOpenResources();
    }

    private static ByteBuffer rgba(int red, int green, int blue, int alpha) {
        ByteBuffer bytes = ByteBuffer.allocateDirect(4);
        bytes.put((byte) red).put((byte) green).put((byte) blue).put((byte) alpha);
        return bytes.flip();
    }

    private static OpenGlThreadGuard boundGuard() {
        GlfwWindow window = new GlfwWindow(1, 1, "guard fixture", new EngineLogger(event -> {
        }), new NativeResourceRegistry());
        OpenGlThreadGuard guard = window.openGlThreadGuard();
        try {
            Method bind = OpenGlThreadGuard.class.getDeclaredMethod("bindOwnerThread", Thread.class);
            bind.setAccessible(true);
            bind.invoke(guard, Thread.currentThread());
            return guard;
        } catch (NoSuchMethodException | IllegalAccessException failure) {
            throw new AssertionError(failure);
        } catch (InvocationTargetException failure) {
            throw new AssertionError(failure.getCause());
        }
    }

    private static final class FakeBackend implements OpenGlResourceBackend {
        private int createdTextures;
        private int deletedTextures;
        private TextureColorEncoding firstEncoding;
        private TextureColorEncoding secondEncoding;

        @Override
        public int createTexture() {
            return ++createdTextures;
        }

        @Override
        public void deleteTexture(int handle) {
            deletedTextures++;
        }

        @Override
        public void allocateRgba8Texture(int handle, TextureColorEncoding colorEncoding, int width, int height, ByteBuffer rgbaBytes) {
            if (firstEncoding == null) {
                firstEncoding = colorEncoding;
            } else {
                secondEncoding = colorEncoding;
            }
        }

        @Override
        public int createBuffer() {
            throw unsupported();
        }

        @Override
        public void deleteBuffer(int handle) {
            throw unsupported();
        }

        @Override
        public void allocateDynamicBufferStorage(int handle, long capacityBytes) {
            throw unsupported();
        }

        @Override
        public void uploadBufferSubData(int handle, long offsetBytes, ByteBuffer data) {
            throw unsupported();
        }

        @Override
        public long createFence() {
            throw unsupported();
        }

        @Override
        public FenceStatus fenceStatus(long fenceHandle) {
            throw unsupported();
        }

        @Override
        public void deleteFence(long fenceHandle) {
            throw unsupported();
        }

        @Override
        public int createVertexArray() {
            throw unsupported();
        }

        @Override
        public void deleteVertexArray(int handle) {
            throw unsupported();
        }

        @Override
        public int createSampler() {
            throw unsupported();
        }

        @Override
        public void deleteSampler(int handle) {
            throw unsupported();
        }

        @Override
        public int createFramebuffer() {
            throw unsupported();
        }

        @Override
        public void deleteFramebuffer(int handle) {
            throw unsupported();
        }

        @Override
        public int createShader(int shaderType) {
            throw unsupported();
        }

        @Override
        public void shaderSource(int shader, String source) {
            throw unsupported();
        }

        @Override
        public void compileShader(int shader) {
            throw unsupported();
        }

        @Override
        public boolean shaderCompileSucceeded(int shader) {
            throw unsupported();
        }

        @Override
        public String shaderInfoLog(int shader) {
            throw unsupported();
        }

        @Override
        public void deleteShader(int shader) {
            throw unsupported();
        }

        @Override
        public int createProgram() {
            throw unsupported();
        }

        @Override
        public void attachShader(int program, int shader) {
            throw unsupported();
        }

        @Override
        public void linkProgram(int program) {
            throw unsupported();
        }

        @Override
        public boolean programLinkSucceeded(int program) {
            throw unsupported();
        }

        @Override
        public String programInfoLog(int program) {
            throw unsupported();
        }

        @Override
        public void detachShader(int program, int shader) {
            throw unsupported();
        }

        @Override
        public void deleteProgram(int program) {
            throw unsupported();
        }

        private static UnsupportedOperationException unsupported() {
            return new UnsupportedOperationException("not used by texture color encoding fixture");
        }
    }
}
