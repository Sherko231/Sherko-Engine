package com.samo.engine.render.opengl.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.samo.engine.core.api.EngineLogger;
import com.samo.engine.core.api.NativeResourceRegistry;
import com.samo.engine.platform.api.GlfwWindow;
import com.samo.engine.platform.api.OpenGlThreadGuard;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;

class IndexedStaticMeshPipelineTest {
    @Test
    void configuresKnownIndexedMeshAndExactDrawState() {
        OpenGlThreadGuard guard = boundGuard();
        NativeResourceRegistry registry = new NativeResourceRegistry();
        FakeResourceBackend resources = new FakeResourceBackend();
        FakeDrawBackend draw = new FakeDrawBackend();
        FakeReflectionBackend reflection = new FakeReflectionBackend();

        IndexedStaticMeshPipeline pipeline = IndexedStaticMeshPipeline.create(
                guard,
                registry,
                resources,
                draw,
                reflection,
                "vertex",
                "fragment");

        assertEquals(List.of(36L, 12L, 128L, 16L), resources.allocations);
        assertEquals(List.of(
                "position:101:11",
                "element:101:12",
                "ubo:0:13",
                "ubo:1:14"), draw.trace);
        assertEquals(2, resources.uploads.size());
        assertVertexData(resources.uploads.get(0).bytes());
        assertIndexData(resources.uploads.get(1).bytes());

        draw.trace.clear();
        resources.uploads.clear();

        pipeline.render(new Matrix4f(), new Matrix4f(), 800, 600);

        assertEquals(2, resources.uploads.size());
        assertEquals(CameraUniformBlock.SIZE_BYTES, resources.uploads.get(0).bytes().length);
        assertEquals(PerFrameUniformBlock.SIZE_BYTES, resources.uploads.get(1).bytes().length);
        assertEquals(List.of(
                "viewport:800x600",
                "state:depth-less:cull-back:front-ccw",
                "clear",
                "program:203",
                "vao:101",
                "draw:triangles:3:uint:0",
                "vao:0",
                "program:0"), draw.trace);

        pipeline.close();
        pipeline.close();
        registry.assertNoOpenResources();
        assertEquals(4, resources.deletedBuffers);
        assertEquals(1, resources.deletedVertexArrays);
        assertEquals(2, resources.deletedShaders);
        assertEquals(1, resources.deletedPrograms);
    }

    @Test
    void invalidFramebufferSizeFailsBeforeUploadOrDrawMutation() {
        OpenGlThreadGuard guard = boundGuard();
        NativeResourceRegistry registry = new NativeResourceRegistry();
        FakeResourceBackend resources = new FakeResourceBackend();
        FakeDrawBackend draw = new FakeDrawBackend();
        IndexedStaticMeshPipeline pipeline = IndexedStaticMeshPipeline.create(
                guard,
                registry,
                resources,
                draw,
                new FakeReflectionBackend(),
                "vertex",
                "fragment");
        resources.uploads.clear();
        draw.trace.clear();

        assertThrows(
                IllegalArgumentException.class,
                () -> pipeline.render(new Matrix4f(), new Matrix4f(), 0, 600));

        assertTrue(resources.uploads.isEmpty());
        assertTrue(draw.trace.isEmpty());
        pipeline.close();
        registry.assertNoOpenResources();
    }

    @Test
    void wrongThreadRenderFailsBeforeUploadOrDrawMutation() throws Exception {
        OpenGlThreadGuard guard = boundGuard();
        NativeResourceRegistry registry = new NativeResourceRegistry();
        FakeResourceBackend resources = new FakeResourceBackend();
        FakeDrawBackend draw = new FakeDrawBackend();
        IndexedStaticMeshPipeline pipeline = IndexedStaticMeshPipeline.create(
                guard,
                registry,
                resources,
                draw,
                new FakeReflectionBackend(),
                "vertex",
                "fragment");
        resources.uploads.clear();
        draw.trace.clear();
        AtomicReference<Throwable> failure = new AtomicReference<>();

        Thread worker = Thread.ofPlatform().start(() -> {
            try {
                pipeline.render(new Matrix4f(), new Matrix4f(), 800, 600);
            } catch (Throwable actual) {
                failure.set(actual);
            }
        });
        worker.join(5_000L);

        assertTrue(failure.get() instanceof IllegalStateException);
        assertTrue(resources.uploads.isEmpty());
        assertTrue(draw.trace.isEmpty());

        pipeline.render(new Matrix4f(), new Matrix4f(), 800, 600);
        pipeline.close();
        registry.assertNoOpenResources();
    }

    @Test
    void partialCreationFailureClosesAlreadyOwnedResources() {
        OpenGlThreadGuard guard = boundGuard();
        NativeResourceRegistry registry = new NativeResourceRegistry();
        FakeResourceBackend resources = new FakeResourceBackend();
        resources.failAllocationCall = 2;

        assertThrows(
                IllegalStateException.class,
                () -> IndexedStaticMeshPipeline.create(
                        guard,
                        registry,
                        resources,
                        new FakeDrawBackend(),
                        new FakeReflectionBackend(),
                        "vertex",
                        "fragment"));

        registry.assertNoOpenResources();
        assertEquals(2, resources.deletedBuffers);
        assertEquals(1, resources.deletedVertexArrays);
        assertEquals(0, resources.deletedShaders);
        assertEquals(0, resources.deletedPrograms);
    }

    private static void assertVertexData(byte[] bytes) {
        ByteBuffer data = ByteBuffer.wrap(bytes).order(ByteOrder.nativeOrder());
        assertEquals(-0.60f, data.getFloat());
        assertEquals(-0.50f, data.getFloat());
        assertEquals(0.0f, data.getFloat());
        assertEquals(0.60f, data.getFloat());
        assertEquals(-0.50f, data.getFloat());
        assertEquals(0.0f, data.getFloat());
        assertEquals(0.0f, data.getFloat());
        assertEquals(0.60f, data.getFloat());
        assertEquals(0.0f, data.getFloat());
    }

    private static void assertIndexData(byte[] bytes) {
        ByteBuffer data = ByteBuffer.wrap(bytes).order(ByteOrder.nativeOrder());
        assertEquals(0, data.getInt());
        assertEquals(1, data.getInt());
        assertEquals(2, data.getInt());
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

    private record Upload(int handle, long offset, byte[] bytes) {
    }

    private static final class FakeReflectionBackend implements OpenGlUniformBlockReflectionBackend {
        private final Map<String, Integer> indices = Map.of(
                CameraUniformBlock.GLSL_BLOCK_NAME, 0,
                PerFrameUniformBlock.GLSL_BLOCK_NAME, 1);

        @Override
        public int uniformBlockIndex(int programHandle, String blockName) {
            return indices.getOrDefault(blockName, -1);
        }

        @Override
        public int uniformBlockDataSize(int programHandle, int blockIndex) {
            return blockIndex == 0 ? CameraUniformBlock.SIZE_BYTES : PerFrameUniformBlock.SIZE_BYTES;
        }

        @Override
        public int uniformBlockBinding(int programHandle, int blockIndex) {
            return blockIndex == 0 ? CameraUniformBlock.BINDING : PerFrameUniformBlock.BINDING;
        }
    }

    private static final class FakeDrawBackend implements OpenGlDrawBackend {
        private final List<String> trace = new ArrayList<>();

        @Override
        public void configurePositionAttribute(int vertexArray, int vertexBuffer) {
            trace.add("position:" + vertexArray + ":" + vertexBuffer);
        }

        @Override
        public void bindElementBuffer(int vertexArray, int indexBuffer) {
            trace.add("element:" + vertexArray + ":" + indexBuffer);
        }

        @Override
        public void bindUniformBuffer(int bindingIndex, int buffer) {
            trace.add("ubo:" + bindingIndex + ":" + buffer);
        }

        @Override
        public void setViewport(int width, int height) {
            trace.add("viewport:" + width + "x" + height);
        }

        @Override
        public void configureDepthAndBackFaceCull() {
            trace.add("state:depth-less:cull-back:front-ccw");
        }

        @Override
        public void clearFrame() {
            trace.add("clear");
        }

        @Override
        public void useProgram(int program) {
            trace.add("program:" + program);
        }

        @Override
        public void bindVertexArray(int vertexArray) {
            trace.add("vao:" + vertexArray);
        }

        @Override
        public void drawIndexedTriangle() {
            trace.add("draw:triangles:3:uint:0");
        }

        @Override
        public void bindDefaultVertexArray() {
            trace.add("vao:0");
        }

        @Override
        public void useDefaultProgram() {
            trace.add("program:0");
        }
    }

    private static final class FakeResourceBackend implements OpenGlResourceBackend {
        private int nextBuffer = 11;
        private int nextShader = 201;
        private int nextProgram = 203;
        private int allocationCalls;
        private int failAllocationCall;
        private int deletedBuffers;
        private int deletedVertexArrays;
        private int deletedShaders;
        private int deletedPrograms;
        private final List<Long> allocations = new ArrayList<>();
        private final List<Upload> uploads = new ArrayList<>();

        @Override
        public int createBuffer() {
            return nextBuffer++;
        }

        @Override
        public void deleteBuffer(int handle) {
            deletedBuffers++;
        }

        @Override
        public void allocateDynamicBufferStorage(int handle, long capacityBytes) {
            allocationCalls++;
            if (allocationCalls == failAllocationCall) {
                throw new IllegalStateException("fixture allocation failure");
            }
            allocations.add(capacityBytes);
        }

        @Override
        public void uploadBufferSubData(int handle, long offsetBytes, ByteBuffer data) {
            ByteBuffer copy = data.duplicate();
            byte[] bytes = new byte[copy.remaining()];
            copy.get(bytes);
            uploads.add(new Upload(handle, offsetBytes, bytes));
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
            return 101;
        }

        @Override
        public void deleteVertexArray(int handle) {
            deletedVertexArrays++;
        }

        @Override
        public int createTexture() {
            throw unsupported();
        }

        @Override
        public void deleteTexture(int handle) {
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
            return nextShader++;
        }

        @Override
        public void shaderSource(int shader, String source) {
        }

        @Override
        public void compileShader(int shader) {
        }

        @Override
        public boolean shaderCompileSucceeded(int shader) {
            return true;
        }

        @Override
        public String shaderInfoLog(int shader) {
            return "";
        }

        @Override
        public void deleteShader(int shader) {
            deletedShaders++;
        }

        @Override
        public int createProgram() {
            return nextProgram++;
        }

        @Override
        public void attachShader(int program, int shader) {
        }

        @Override
        public void linkProgram(int program) {
        }

        @Override
        public boolean programLinkSucceeded(int program) {
            return true;
        }

        @Override
        public String programInfoLog(int program) {
            return "";
        }

        @Override
        public void detachShader(int program, int shader) {
        }

        @Override
        public void deleteProgram(int program) {
            deletedPrograms++;
        }

        private static UnsupportedOperationException unsupported() {
            return new UnsupportedOperationException("not used by P5-T07 fixture");
        }
    }
}
