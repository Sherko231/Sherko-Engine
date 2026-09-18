package com.samo.engine.render.opengl.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.samo.engine.core.api.EngineLogger;
import com.samo.engine.core.api.NativeResourceRegistry;
import com.samo.engine.platform.api.GlfwWindow;
import com.samo.engine.platform.api.OpenGlThreadGuard;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class OpenGlResourceOwnershipTest {
    @Test
    void createsAndClosesAllResourceTypesExactlyOnce() {
        OpenGlThreadGuard guard = boundGuard();
        NativeResourceRegistry registry = new NativeResourceRegistry();
        FakeBackend backend = new FakeBackend();

        OpenGlBuffer buffer = OpenGlBuffer.create(guard, registry, backend);
        OpenGlVertexArray vertexArray = OpenGlVertexArray.create(guard, registry, backend);
        OpenGlTexture texture = OpenGlTexture.create(guard, registry, backend);
        OpenGlSampler sampler = OpenGlSampler.create(guard, registry, backend);
        OpenGlFramebuffer framebuffer = OpenGlFramebuffer.create(guard, registry, backend);
        OpenGlShader vertex = OpenGlShader.compile(
                OpenGlShader.Stage.VERTEX, "vertex", guard, registry, backend);
        OpenGlShader fragment = OpenGlShader.compile(
                OpenGlShader.Stage.FRAGMENT, "fragment", guard, registry, backend);
        OpenGlProgram program = OpenGlProgram.link(vertex, fragment, guard, registry, backend);

        program.close();
        fragment.close();
        vertex.close();
        framebuffer.close();
        sampler.close();
        texture.close();
        vertexArray.close();
        buffer.close();

        program.close();
        fragment.close();
        vertex.close();
        framebuffer.close();
        sampler.close();
        texture.close();
        vertexArray.close();
        buffer.close();

        registry.assertNoOpenResources();
        assertEquals(1, backend.deletedPrograms);
        assertEquals(2, backend.deletedShaders);
        assertEquals(1, backend.deletedFramebuffers);
        assertEquals(1, backend.deletedSamplers);
        assertEquals(1, backend.deletedTextures);
        assertEquals(1, backend.deletedVertexArrays);
        assertEquals(1, backend.deletedBuffers);
    }

    @Test
    void workerThreadCreationRejectsBeforeBackendEntry() throws Exception {
        OpenGlThreadGuard guard = boundGuard();
        NativeResourceRegistry registry = new NativeResourceRegistry();
        FakeBackend backend = new FakeBackend();
        AtomicReference<Throwable> failure = new AtomicReference<>();

        Thread worker = Thread.ofPlatform().start(() -> {
            try {
                OpenGlBuffer.create(guard, registry, backend);
            } catch (Throwable actual) {
                failure.set(actual);
            }
        });
        worker.join(5_000L);

        assertTrue(failure.get() instanceof IllegalStateException);
        assertEquals(0, backend.createdBuffers);
        registry.assertNoOpenResources();
    }

    @Test
    void workerThreadCloseRejectsBeforeRegistryMutationAndOwnerCanStillClose() throws Exception {
        OpenGlThreadGuard guard = boundGuard();
        NativeResourceRegistry registry = new NativeResourceRegistry();
        FakeBackend backend = new FakeBackend();
        OpenGlBuffer buffer = OpenGlBuffer.create(guard, registry, backend);
        AtomicReference<Throwable> failure = new AtomicReference<>();

        Thread worker = Thread.ofPlatform().start(() -> {
            try {
                buffer.close();
            } catch (Throwable actual) {
                failure.set(actual);
            }
        });
        worker.join(5_000L);

        assertTrue(failure.get() instanceof IllegalStateException);
        assertEquals(0, backend.deletedBuffers);
        assertThrows(IllegalStateException.class, registry::assertNoOpenResources);

        buffer.close();
        assertEquals(1, backend.deletedBuffers);
        registry.assertNoOpenResources();
    }

    @Test
    void registryRegistrationFailureDeletesNewHandleOnce() {
        OpenGlThreadGuard guard = boundGuard();
        NativeResourceRegistry registry = new NativeResourceRegistry();
        FakeBackend backend = new FakeBackend();
        NativeResourceRegistry.Registration existing =
                registry.register("OpenGL buffer", 11L, () -> { });

        IllegalStateException failure = assertThrows(
                IllegalStateException.class,
                () -> OpenGlBuffer.create(guard, registry, backend));

        assertTrue(failure.getMessage().contains("already registered"));
        assertEquals(1, backend.deletedBuffers);
        existing.close();
        registry.assertNoOpenResources();
    }

    @Test
    void shaderCompileFailureDeletesShaderWithoutRegisteringIt() {
        OpenGlThreadGuard guard = boundGuard();
        NativeResourceRegistry registry = new NativeResourceRegistry();
        FakeBackend backend = new FakeBackend();
        backend.shaderCompileSucceeded = false;
        backend.shaderInfoLog = "fixture compile error";

        IllegalStateException failure = assertThrows(
                IllegalStateException.class,
                () -> OpenGlShader.compile(
                        OpenGlShader.Stage.VERTEX,
                        "fixture/broken.vert",
                        "broken",
                        guard,
                        registry,
                        backend));

        assertTrue(failure.getMessage().contains("fixture/broken.vert"));
        assertTrue(failure.getMessage().contains("VERTEX"));
        assertTrue(failure.getMessage().contains("fixture compile error"));
        assertEquals(1, backend.deletedShaders);
        registry.assertNoOpenResources();
    }

    @Test
    void programLinkFailureDeletesProgramButLeavesCallerShadersOwned() {
        OpenGlThreadGuard guard = boundGuard();
        NativeResourceRegistry registry = new NativeResourceRegistry();
        FakeBackend backend = new FakeBackend();
        OpenGlShader vertex = OpenGlShader.compile(
                OpenGlShader.Stage.VERTEX, "vertex", guard, registry, backend);
        OpenGlShader fragment = OpenGlShader.compile(
                OpenGlShader.Stage.FRAGMENT, "fragment", guard, registry, backend);
        backend.programLinkSucceeded = false;
        backend.programInfoLog = "fixture link error";

        IllegalStateException failure = assertThrows(
                IllegalStateException.class,
                () -> OpenGlProgram.link(
                        "fixture-program",
                        vertex,
                        fragment,
                        guard,
                        registry,
                        backend));

        assertTrue(failure.getMessage().contains("fixture-program"));
        assertTrue(failure.getMessage().contains("fixture link error"));
        assertEquals(1, backend.deletedPrograms);
        assertEquals(0, backend.deletedShaders);
        assertThrows(IllegalStateException.class, registry::assertNoOpenResources);

        fragment.close();
        vertex.close();
        assertEquals(2, backend.deletedShaders);
        registry.assertNoOpenResources();
    }

    @Test
    void deleteFailurePropagatesOnceAndIsNeverRetried() {
        OpenGlThreadGuard guard = boundGuard();
        NativeResourceRegistry registry = new NativeResourceRegistry();
        FakeBackend backend = new FakeBackend();
        RuntimeException deleteFailure = new IllegalStateException("delete failed");
        backend.bufferDeleteFailure = deleteFailure;
        OpenGlBuffer buffer = OpenGlBuffer.create(guard, registry, backend);

        RuntimeException actual = assertThrows(RuntimeException.class, buffer::close);
        assertSame(deleteFailure, actual);
        assertEquals(1, backend.deletedBuffers);

        buffer.close();
        assertEquals(1, backend.deletedBuffers);
        IllegalStateException registryFailure =
                assertThrows(IllegalStateException.class, registry::assertNoOpenResources);
        assertTrue(registryFailure.getMessage().contains("CLOSE_FAILED"));
    }

    @Test
    void zeroCreateHandleFailsWithoutRegistration() {
        OpenGlThreadGuard guard = boundGuard();
        NativeResourceRegistry registry = new NativeResourceRegistry();
        FakeBackend backend = new FakeBackend();
        backend.bufferHandle = 0;

        IllegalStateException failure = assertThrows(
                IllegalStateException.class,
                () -> OpenGlBuffer.create(guard, registry, backend));

        assertTrue(failure.getMessage().contains("handle 0"));
        assertEquals(0, backend.deletedBuffers);
        registry.assertNoOpenResources();
    }

    private static OpenGlThreadGuard boundGuard() {
        GlfwWindow window = new GlfwWindow(
                1,
                1,
                "guard fixture",
                new EngineLogger(event -> { }),
                new NativeResourceRegistry());
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
        private int bufferHandle = 11;
        private int vertexArrayHandle = 12;
        private int textureHandle = 13;
        private int samplerHandle = 14;
        private int framebufferHandle = 15;
        private int nextShaderHandle = 20;
        private int programHandle = 30;
        private boolean shaderCompileSucceeded = true;
        private boolean programLinkSucceeded = true;
        private String shaderInfoLog = "";
        private String programInfoLog = "";
        private RuntimeException bufferDeleteFailure;
        private int createdBuffers;
        private int deletedBuffers;
        private int deletedVertexArrays;
        private int deletedTextures;
        private int deletedSamplers;
        private int deletedFramebuffers;
        private int deletedShaders;
        private int deletedPrograms;
        private final List<String> trace = new ArrayList<>();

        @Override
        public int createBuffer() {
            createdBuffers++;
            return bufferHandle;
        }

        @Override
        public void deleteBuffer(int handle) {
            deletedBuffers++;
            if (bufferDeleteFailure != null) {
                throw bufferDeleteFailure;
            }
        }

        @Override
        public void allocateDynamicBufferStorage(int handle, long capacityBytes) {
            throw new UnsupportedOperationException("not used by P5-T03");
        }

        @Override
        public void uploadBufferSubData(int handle, long offsetBytes, java.nio.ByteBuffer data) {
            throw new UnsupportedOperationException("not used by P5-T03");
        }

        @Override
        public long createFence() {
            throw new UnsupportedOperationException("not used by P5-T03");
        }

        @Override
        public OpenGlResourceBackend.FenceStatus fenceStatus(long fenceHandle) {
            throw new UnsupportedOperationException("not used by P5-T03");
        }

        @Override
        public void deleteFence(long fenceHandle) {
            throw new UnsupportedOperationException("not used by P5-T03");
        }

        @Override
        public int createVertexArray() {
            return vertexArrayHandle;
        }

        @Override
        public void deleteVertexArray(int handle) {
            deletedVertexArrays++;
        }

        @Override
        public int createTexture() {
            return textureHandle;
        }

        @Override
        public void deleteTexture(int handle) {
            deletedTextures++;
        }

        @Override
        public int createSampler() {
            return samplerHandle;
        }

        @Override
        public void deleteSampler(int handle) {
            deletedSamplers++;
        }

        @Override
        public int createFramebuffer() {
            return framebufferHandle;
        }

        @Override
        public void deleteFramebuffer(int handle) {
            deletedFramebuffers++;
        }

        @Override
        public int createShader(int shaderType) {
            trace.add("create-shader:" + shaderType);
            return nextShaderHandle++;
        }

        @Override
        public void shaderSource(int shader, String source) {
            trace.add("shader-source:" + shader);
        }

        @Override
        public void compileShader(int shader) {
            trace.add("compile-shader:" + shader);
        }

        @Override
        public boolean shaderCompileSucceeded(int shader) {
            return shaderCompileSucceeded;
        }

        @Override
        public String shaderInfoLog(int shader) {
            return shaderInfoLog;
        }

        @Override
        public void deleteShader(int shader) {
            deletedShaders++;
        }

        @Override
        public int createProgram() {
            return programHandle;
        }

        @Override
        public void attachShader(int program, int shader) {
            trace.add("attach:" + program + ":" + shader);
        }

        @Override
        public void linkProgram(int program) {
            trace.add("link:" + program);
        }

        @Override
        public boolean programLinkSucceeded(int program) {
            return programLinkSucceeded;
        }

        @Override
        public String programInfoLog(int program) {
            return programInfoLog;
        }

        @Override
        public void detachShader(int program, int shader) {
            trace.add("detach:" + program + ":" + shader);
        }

        @Override
        public void deleteProgram(int program) {
            deletedPrograms++;
        }
    }
}
