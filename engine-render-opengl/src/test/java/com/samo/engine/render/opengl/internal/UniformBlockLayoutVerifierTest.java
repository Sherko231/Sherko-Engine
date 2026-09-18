package com.samo.engine.render.opengl.internal;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.samo.engine.core.api.EngineLogger;
import com.samo.engine.core.api.NativeResourceRegistry;
import com.samo.engine.platform.api.GlfwWindow;
import com.samo.engine.platform.api.OpenGlThreadGuard;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class UniformBlockLayoutVerifierTest {
    @Test
    void acceptsExpectedSizesAndBindings() {
        FakeReflectionBackend backend = expectedBackend();
        UniformBlockLayoutVerifier.verify(7, boundGuard(), backend);
    }

    @Test
    void missingBlockFailsClearly() {
        FakeReflectionBackend backend = expectedBackend();
        backend.indices.put(CameraUniformBlock.GLSL_BLOCK_NAME, -1);

        IllegalStateException failure = assertThrows(
                IllegalStateException.class,
                () -> UniformBlockLayoutVerifier.verify(7, boundGuard(), backend));

        assertTrue(failure.getMessage().contains(CameraUniformBlock.GLSL_BLOCK_NAME));
        assertTrue(failure.getMessage().contains("Missing"));
    }

    @Test
    void wrongSizeFailsClearly() {
        FakeReflectionBackend backend = expectedBackend();
        backend.sizes.put(0, CameraUniformBlock.SIZE_BYTES + 16);

        IllegalStateException failure = assertThrows(
                IllegalStateException.class,
                () -> UniformBlockLayoutVerifier.verify(7, boundGuard(), backend));

        assertTrue(failure.getMessage().contains(CameraUniformBlock.GLSL_BLOCK_NAME));
        assertTrue(failure.getMessage().contains("expected=128"));
        assertTrue(failure.getMessage().contains("actual=144"));
    }

    @Test
    void wrongBindingFailsClearly() {
        FakeReflectionBackend backend = expectedBackend();
        backend.bindings.put(1, 5);

        IllegalStateException failure = assertThrows(
                IllegalStateException.class,
                () -> UniformBlockLayoutVerifier.verify(7, boundGuard(), backend));

        assertTrue(failure.getMessage().contains(PerFrameUniformBlock.GLSL_BLOCK_NAME));
        assertTrue(failure.getMessage().contains("expected=1"));
        assertTrue(failure.getMessage().contains("actual=5"));
    }

    private static FakeReflectionBackend expectedBackend() {
        FakeReflectionBackend backend = new FakeReflectionBackend();
        backend.indices.put(CameraUniformBlock.GLSL_BLOCK_NAME, 0);
        backend.indices.put(PerFrameUniformBlock.GLSL_BLOCK_NAME, 1);
        backend.sizes.put(0, CameraUniformBlock.SIZE_BYTES);
        backend.sizes.put(1, PerFrameUniformBlock.SIZE_BYTES);
        backend.bindings.put(0, CameraUniformBlock.BINDING);
        backend.bindings.put(1, PerFrameUniformBlock.BINDING);
        return backend;
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

    private static final class FakeReflectionBackend implements OpenGlUniformBlockReflectionBackend {
        private final Map<String, Integer> indices = new HashMap<>();
        private final Map<Integer, Integer> sizes = new HashMap<>();
        private final Map<Integer, Integer> bindings = new HashMap<>();

        @Override
        public int uniformBlockIndex(int programHandle, String blockName) {
            return indices.getOrDefault(blockName, -1);
        }

        @Override
        public int uniformBlockDataSize(int programHandle, int blockIndex) {
            return sizes.getOrDefault(blockIndex, -1);
        }

        @Override
        public int uniformBlockBinding(int programHandle, int blockIndex) {
            return bindings.getOrDefault(blockIndex, -1);
        }
    }
}
