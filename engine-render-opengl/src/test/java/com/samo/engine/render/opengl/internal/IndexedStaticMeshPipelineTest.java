package com.samo.engine.render.opengl.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.samo.engine.core.api.DebugColor;
import com.samo.engine.core.api.DebugFrame;
import com.samo.engine.core.api.DebugLine;
import com.samo.engine.core.api.DebugTextCounter;
import com.samo.engine.core.api.EngineLogger;
import com.samo.engine.core.api.NativeResourceRegistry;
import com.samo.engine.platform.api.GlfwWindow;
import com.samo.engine.platform.api.OpenGlThreadGuard;
import com.samo.engine.render.api.RenderCullingCounters;
import com.samo.engine.render.api.RenderFramePacket;
import com.samo.engine.render.api.RenderLocalLight;
import com.samo.engine.render.api.RenderPointLight;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL21;

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

        assertEquals(List.of(72L, 12L, 128L, 16L, 528L, 147456L), resources.allocations);
        assertEquals(List.of(
                "position:101:11",
                "element:101:12",
                "ubo:0:13",
                "ubo:1:14",
                "ubo:2:15",
                "debug-position:102:16",
                "ubo:0:13"), draw.trace);
        assertEquals(2, resources.uploads.size());
        assertVertexData(resources.uploads.get(0).bytes());
        assertIndexData(resources.uploads.get(1).bytes());
        assertEquals(1, resources.textureAllocations.size());
        TextureAllocation reference = resources.textureAllocations.getFirst();
        assertEquals(TextureColorEncoding.SRGB_COLOR, reference.colorEncoding());
        assertEquals(1, reference.width());
        assertEquals(1, reference.height());
        assertEquals(List.of(128, 128, 128, 255), reference.unsignedBytes());
        assertEquals(1, resources.configuredSamplers);

        draw.trace.clear();
        resources.uploads.clear();

        pipeline.render(new Matrix4f(), new Matrix4f(), 800, 600);

        assertEquals(3, resources.uploads.size());
        assertEquals(CameraUniformBlock.SIZE_BYTES, resources.uploads.get(0).bytes().length);
        assertEquals(PerFrameUniformBlock.SIZE_BYTES, resources.uploads.get(1).bytes().length);
        assertEquals(LocalLightUniformBlock.SIZE_BYTES, resources.uploads.get(2).bytes().length);
        ByteBuffer localLights = ByteBuffer.wrap(resources.uploads.get(2).bytes())
                .order(ByteOrder.nativeOrder());
        assertEquals(0, localLights.getInt(LocalLightUniformBlock.META_OFFSET_BYTES));
        assertEquals(
                new DirectionalLight(0.0f, -1.0f, -1.0f, 1.0f, 1.0f, 1.0f, 0.8f),
                draw.lastDirectionalLight);
        assertEquals(List.of(
                "viewport:0:0:800x600",
                "srgb:true",
                "clear:true",
                "viewport:0:0:400x600",
                "state:blend=OPAQUE:depth=TEST_WRITE:cull=BACK",
                "texture:0:301:401",
                "scalars:203:1.0,1.0,1.0,1.0",
                "program:203",
                "vao:101",
                "draw:triangles:3:uint:0",
                "vao:0",
                "program:0",
                "texture:0:0:0",
                "viewport:400:0:400x600",
                "state:blend=ALPHA_BLEND:depth=TEST_NO_WRITE:cull=NONE",
                "texture:0:301:401",
                "scalars:203:1.0,0.35,0.35,0.8",
                "program:203",
                "vao:101",
                "draw:triangles:3:uint:0",
                "vao:0",
                "program:0",
                "texture:0:0:0",
                "viewport:0:0:800x600",
                "srgb:false"), draw.trace);

        pipeline.close();
        pipeline.close();
        registry.assertNoOpenResources();
        assertEquals(6, resources.deletedBuffers);
        assertEquals(2, resources.deletedVertexArrays);
        assertEquals(4, resources.deletedShaders);
        assertEquals(2, resources.deletedPrograms);
        assertEquals(1, resources.deletedTextures);
        assertEquals(1, resources.deletedSamplers);
    }

    @Test
    void overMaxPacketWarnsOnceAndUploadsOnlyFirstConfiguredLights() {
        OpenGlThreadGuard guard = boundGuard();
        NativeResourceRegistry registry = new NativeResourceRegistry();
        FakeResourceBackend resources = new FakeResourceBackend();
        FakeDrawBackend draw = new FakeDrawBackend();
        List<EngineLogger.Event> events = new ArrayList<>();
        IndexedStaticMeshPipeline pipeline = IndexedStaticMeshPipeline.create(
                guard,
                registry,
                resources,
                draw,
                new FakeReflectionBackend(),
                new EngineLogger(events::add),
                2,
                "vertex",
                "fragment");
        resources.uploads.clear();
        draw.trace.clear();

        RenderLocalLight first =
                new RenderPointLight(1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 0.5f, 10.0f);
        RenderLocalLight second =
                new RenderPointLight(2.0f, 0.0f, 1.0f, 0.0f, 1.0f, 0.0f, 0.5f, 10.0f);
        RenderLocalLight third =
                new RenderPointLight(3.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.5f, 10.0f);
        RenderFramePacket frame = new RenderFramePacket(
                new Matrix4f(),
                new Matrix4f(),
                800,
                600,
                List.of(first, second, third));

        pipeline.render(frame);

        assertEquals(1, events.size());
        assertEquals(EngineLogger.Level.WARN, events.getFirst().level());
        assertEquals(
                "Local light limit exceeded: submitted=3 accepted=2 dropped=1 configuredMax=2",
                events.getFirst().message());
        ByteBuffer packed = ByteBuffer.wrap(resources.uploads.get(2).bytes())
                .order(ByteOrder.nativeOrder());
        assertEquals(2, packed.getInt(LocalLightUniformBlock.META_OFFSET_BYTES));
        assertEquals(
                1.0f,
                packed.getFloat(LocalLightUniformBlock.POSITION_RANGE_OFFSET_BYTES));
        assertEquals(
                2.0f,
                packed.getFloat(
                        LocalLightUniformBlock.POSITION_RANGE_OFFSET_BYTES
                                + 4 * Float.BYTES));
        assertEquals(
                0.0f,
                packed.getFloat(
                        LocalLightUniformBlock.POSITION_RANGE_OFFSET_BYTES
                                + 2 * 4 * Float.BYTES));
        assertEquals(new RenderCullingCounters(2, 2, 0, 2), pipeline.lastCullingCounters());

        pipeline.close();
        registry.assertNoOpenResources();
    }

    @Test
    void overflowLoggerFailureHappensBeforeUploadOrDrawMutation() {
        OpenGlThreadGuard guard = boundGuard();
        NativeResourceRegistry registry = new NativeResourceRegistry();
        FakeResourceBackend resources = new FakeResourceBackend();
        FakeDrawBackend draw = new FakeDrawBackend();
        EngineLogger failingLogger = new EngineLogger(event -> {
            throw new IllegalStateException("fixture logger failure");
        });
        IndexedStaticMeshPipeline pipeline = IndexedStaticMeshPipeline.create(
                guard,
                registry,
                resources,
                draw,
                new FakeReflectionBackend(),
                failingLogger,
                1,
                "vertex",
                "fragment");
        resources.uploads.clear();
        draw.trace.clear();

        RenderFramePacket frame = new RenderFramePacket(
                new Matrix4f(),
                new Matrix4f(),
                800,
                600,
                List.of(
                        new RenderPointLight(
                                0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.5f, 10.0f),
                        new RenderPointLight(
                                1.0f, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.5f, 10.0f)));

        IllegalStateException failure =
                assertThrows(IllegalStateException.class, () -> pipeline.render(frame));

        assertEquals("fixture logger failure", failure.getMessage());
        assertTrue(resources.uploads.isEmpty());
        assertTrue(draw.trace.isEmpty());
        assertEquals(RenderCullingCounters.EMPTY, pipeline.lastCullingCounters());

        pipeline.close();
        registry.assertNoOpenResources();
    }

    @Test
    void debugFrameUploadsLinesDrawsAfterSceneAndPublishesCounters() {
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

        DebugTextCounter first = new DebugTextCounter("tick", 42L);
        DebugTextCounter second = new DebugTextCounter("net/rtt_ms", 17L);
        DebugFrame debugFrame = new DebugFrame(
                List.of(new DebugLine(
                        -0.5f, 0.0f, 0.5f,
                        0.5f, 0.0f, 0.5f,
                        new DebugColor(0.0f, 1.0f, 0.0f))),
                List.of(first, second));
        RenderFramePacket frame = new RenderFramePacket(
                new Matrix4f(),
                new Matrix4f(),
                800,
                600,
                List.of(),
                debugFrame);

        pipeline.render(frame);

        assertEquals(4, resources.uploads.size());
        assertEquals(
                2 * DebugLineVertexPacker.VERTEX_STRIDE_BYTES,
                resources.uploads.get(3).bytes().length);
        assertTrue(draw.trace.contains("debug-state"));
        assertTrue(draw.trace.contains("program:204"));
        assertTrue(draw.trace.contains("vao:102"));
        assertTrue(draw.trace.contains("draw:lines:2"));
        assertEquals(List.of(first, second), pipeline.lastDebugTextCounters());
        assertEquals(new RenderCullingCounters(2, 2, 0, 2), pipeline.lastCullingCounters());

        pipeline.close();
        registry.assertNoOpenResources();
    }

    @Test
    void debugDrawFailureDoesNotPublishDebugOrCullingCounters() {
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

        DebugTextCounter acceptedCounter = new DebugTextCounter("tick", 1L);
        RenderFramePacket accepted = new RenderFramePacket(
                new Matrix4f(),
                new Matrix4f(),
                800,
                600,
                List.of(),
                new DebugFrame(List.of(), List.of(acceptedCounter)));
        pipeline.render(accepted);
        RenderCullingCounters acceptedCulling = pipeline.lastCullingCounters();

        draw.debugDrawFailure = new IllegalStateException("debug draw failure");
        RenderFramePacket failing = new RenderFramePacket(
                new Matrix4f(),
                new Matrix4f(),
                800,
                600,
                List.of(),
                new DebugFrame(
                        List.of(new DebugLine(
                                -0.5f, 0.0f, 0.5f,
                                0.5f, 0.0f, 0.5f,
                                new DebugColor(0.0f, 1.0f, 0.0f))),
                        List.of(new DebugTextCounter("tick", 2L))));

        assertThrows(IllegalStateException.class, () -> pipeline.render(failing));

        assertEquals(List.of(acceptedCounter), pipeline.lastDebugTextCounters());
        assertEquals(acceptedCulling, pipeline.lastCullingCounters());

        pipeline.close();
        registry.assertNoOpenResources();
    }

    @Test
    void packetConsumptionUsesCapturedMatricesAfterSourceMutation() {
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

        Matrix4f sourceView = new Matrix4f().translation(1.0f, 2.0f, 3.0f);
        Matrix4f sourceProjection = new Matrix4f().scaling(2.0f, 3.0f, 4.0f);
        RenderFramePacket frame = new RenderFramePacket(sourceView, sourceProjection, 800, 600);
        sourceView.identity();
        sourceProjection.identity();

        pipeline.render(frame);

        ByteBuffer camera = ByteBuffer.wrap(resources.uploads.getFirst().bytes())
                .order(ByteOrder.nativeOrder());
        assertEquals(1.0f, camera.getFloat(CameraUniformBlock.VIEW_OFFSET_BYTES + 12 * Float.BYTES));
        assertEquals(2.0f, camera.getFloat(CameraUniformBlock.VIEW_OFFSET_BYTES + 13 * Float.BYTES));
        assertEquals(3.0f, camera.getFloat(CameraUniformBlock.VIEW_OFFSET_BYTES + 14 * Float.BYTES));
        assertEquals(2.0f, camera.getFloat(CameraUniformBlock.PROJECTION_OFFSET_BYTES));
        assertEquals(3.0f, camera.getFloat(CameraUniformBlock.PROJECTION_OFFSET_BYTES + 5 * Float.BYTES));
        assertEquals(4.0f, camera.getFloat(CameraUniformBlock.PROJECTION_OFFSET_BYTES + 10 * Float.BYTES));
        assertEquals("viewport:0:0:800x600", draw.trace.getFirst());

        pipeline.close();
        registry.assertNoOpenResources();
    }

    @Test
    void offCameraReferenceCandidatesProduceNoDrawSubmission() {
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

        pipeline.render(
                new Matrix4f().translation(-10.0f, 0.0f, 0.0f),
                new Matrix4f(),
                800,
                600);

        assertEquals(
                new RenderCullingCounters(2, 0, 2, 0),
                pipeline.lastCullingCounters());
        assertTrue(draw.trace.stream().noneMatch(entry -> entry.startsWith("draw:")));
        assertTrue(draw.trace.stream().noneMatch(entry -> entry.startsWith("state:")));
        assertEquals(List.of(
                "viewport:0:0:800x600",
                "srgb:true",
                "clear:true",
                "viewport:0:0:800x600",
                "srgb:false"), draw.trace);

        pipeline.close();
        registry.assertNoOpenResources();
    }

    @Test
    void failedRenderDoesNotPublishPartialCullingCounters() {
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

        pipeline.render(new Matrix4f(), new Matrix4f(), 800, 600);
        RenderCullingCounters accepted =
                new RenderCullingCounters(2, 2, 0, 2);
        assertEquals(accepted, pipeline.lastCullingCounters());

        draw.drawFailure = new IllegalStateException("fixture draw failure");
        assertThrows(
                IllegalStateException.class,
                () -> pipeline.render(new Matrix4f(), new Matrix4f(), 800, 600));

        assertEquals(accepted, pipeline.lastCullingCounters());

        pipeline.close();
        registry.assertNoOpenResources();
    }

    @Test
    void unsupportedDefaultFramebufferEncodingFailsBeforeFragmentOrProgramCreation() {
        OpenGlThreadGuard guard = boundGuard();
        NativeResourceRegistry registry = new NativeResourceRegistry();
        FakeResourceBackend resources = new FakeResourceBackend();
        FakeDrawBackend draw = new FakeDrawBackend();
        draw.defaultFramebufferEncoding = 0x7fffffff;

        assertThrows(
                IllegalStateException.class,
                () -> IndexedStaticMeshPipeline.create(
                        guard,
                        registry,
                        resources,
                        draw,
                        new FakeReflectionBackend(),
                        "vertex",
                        "#version 460 core\nvoid main() {}"));

        registry.assertNoOpenResources();
        assertEquals(1, resources.deletedShaders);
        assertEquals(0, resources.deletedPrograms);
        assertEquals(1, resources.deletedTextures);
        assertEquals(1, resources.deletedSamplers);
        assertEquals(5, resources.deletedBuffers);
        assertEquals(1, resources.deletedVertexArrays);
    }

    @Test
    void linearDefaultFramebufferUsesSingleManualSrgbEncode() {
        OpenGlThreadGuard guard = boundGuard();
        NativeResourceRegistry registry = new NativeResourceRegistry();
        FakeResourceBackend resources = new FakeResourceBackend();
        FakeDrawBackend draw = new FakeDrawBackend();
        draw.defaultFramebufferEncoding = GL11.GL_LINEAR;
        IndexedStaticMeshPipeline pipeline = IndexedStaticMeshPipeline.create(
                guard,
                registry,
                resources,
                draw,
                new FakeReflectionBackend(),
                "vertex",
                "#version 460 core\nvoid main() {}");
        draw.trace.clear();
        resources.uploads.clear();

        pipeline.render(new Matrix4f(), new Matrix4f(), 800, 600);

        assertEquals(List.of(
                "viewport:0:0:800x600",
                "srgb:false",
                "clear:false",
                "viewport:0:0:400x600",
                "state:blend=OPAQUE:depth=TEST_WRITE:cull=BACK",
                "texture:0:301:401",
                "scalars:203:1.0,1.0,1.0,1.0",
                "program:203",
                "vao:101",
                "draw:triangles:3:uint:0",
                "vao:0",
                "program:0",
                "texture:0:0:0",
                "viewport:400:0:400x600",
                "state:blend=ALPHA_BLEND:depth=TEST_NO_WRITE:cull=NONE",
                "texture:0:301:401",
                "scalars:203:1.0,0.35,0.35,0.8",
                "program:203",
                "vao:101",
                "draw:triangles:3:uint:0",
                "vao:0",
                "program:0",
                "texture:0:0:0",
                "viewport:0:0:800x600",
                "srgb:false"), draw.trace);

        pipeline.close();
        registry.assertNoOpenResources();
    }

    @Test
    void fragmentVariantInjectsManualEncodeOnlyForLinearDefaultFramebuffer() {
        String source = "#version 460 core\nvoid main() {}";

        assertEquals(source, PresentationMode.HARDWARE_SRGB.fragmentSource(source));

        String fallback = PresentationMode.MANUAL_SRGB.fragmentSource(source);
        assertTrue(fallback.startsWith(
                "#version 460 core" + System.lineSeparator() + "#define SHERKO_MANUAL_SRGB_ENCODE 1"));
        assertTrue(fallback.endsWith("\nvoid main() {}"));
    }

    @Test
    void drawFailureStillDisablesFramebufferSrgbAndUnbindsTextureState() {
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
        draw.trace.clear();
        draw.drawFailure = new IllegalStateException("fixture draw failure");

        RuntimeException actual = assertThrows(
                RuntimeException.class,
                () -> pipeline.render(new Matrix4f(), new Matrix4f(), 800, 600));

        assertEquals("fixture draw failure", actual.getMessage());
        assertEquals("srgb:false", draw.trace.getLast());
        assertTrue(draw.trace.contains("viewport:0:0:800x600"));
        assertTrue(draw.trace.contains("texture:0:0:0"));
        assertTrue(draw.trace.contains("vao:0"));
        assertTrue(draw.trace.contains("program:0"));

        pipeline.close();
        registry.assertNoOpenResources();
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
        assertReferenceVertex(data, -0.60f, -0.50f, 0.0f);
        assertReferenceVertex(data, 0.60f, -0.50f, 0.0f);
        assertReferenceVertex(data, 0.0f, 0.60f, 0.0f);
    }

    private static void assertReferenceVertex(ByteBuffer data, float x, float y, float z) {
        assertEquals(x, data.getFloat());
        assertEquals(y, data.getFloat());
        assertEquals(z, data.getFloat());
        assertEquals(0.0f, data.getFloat());
        assertEquals(0.0f, data.getFloat());
        assertEquals(1.0f, data.getFloat());
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

    private record TextureAllocation(
            TextureColorEncoding colorEncoding,
            int width,
            int height,
            List<Integer> unsignedBytes) {
    }

    private static final class FakeReflectionBackend implements OpenGlUniformBlockReflectionBackend {
        private final Map<String, Integer> indices = Map.of(
                CameraUniformBlock.GLSL_BLOCK_NAME, 0,
                PerFrameUniformBlock.GLSL_BLOCK_NAME, 1,
                LocalLightUniformBlock.GLSL_BLOCK_NAME, 2);

        @Override
        public int uniformBlockIndex(int programHandle, String blockName) {
            return indices.getOrDefault(blockName, -1);
        }

        @Override
        public int uniformBlockDataSize(int programHandle, int blockIndex) {
            return switch (blockIndex) {
                case 0 -> CameraUniformBlock.SIZE_BYTES;
                case 1 -> PerFrameUniformBlock.SIZE_BYTES;
                case 2 -> LocalLightUniformBlock.SIZE_BYTES;
                default -> -1;
            };
        }

        @Override
        public int uniformBlockBinding(int programHandle, int blockIndex) {
            return switch (blockIndex) {
                case 0 -> CameraUniformBlock.BINDING;
                case 1 -> PerFrameUniformBlock.BINDING;
                case 2 -> LocalLightUniformBlock.BINDING;
                default -> -1;
            };
        }
    }

    private static final class FakeDrawBackend implements OpenGlDrawBackend {
        private final List<String> trace = new ArrayList<>();
        private RuntimeException drawFailure;
        private RuntimeException debugDrawFailure;
        private int defaultFramebufferEncoding = GL21.GL_SRGB;
        private DirectionalLight lastDirectionalLight;

        @Override
        public void configurePositionAndNormalAttributes(int vertexArray, int vertexBuffer) {
            trace.add("position:" + vertexArray + ":" + vertexBuffer);
        }

        @Override
        public void configureDebugLineAttributes(int vertexArray, int vertexBuffer) {
            trace.add("debug-position:" + vertexArray + ":" + vertexBuffer);
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
        public void setViewport(int x, int y, int width, int height) {
            trace.add("viewport:" + x + ":" + y + ":" + width + "x" + height);
        }

        @Override
        public void applyMaterialState(RendererMaterial material) {
            trace.add("state:blend=" + material.blendMode()
                    + ":depth=" + material.depthMode()
                    + ":cull=" + material.cullMode());
        }

        @Override
        public void applyDebugLineState() {
            trace.add("debug-state");
        }

        @Override
        public int defaultFramebufferColorEncoding() {
            return defaultFramebufferEncoding;
        }

        @Override
        public void setFramebufferSrgbEnabled(boolean enabled) {
            trace.add("srgb:" + enabled);
        }

        @Override
        public void clearFrame(PresentationMode presentationMode) {
            trace.add("clear:" + presentationMode.framebufferSrgbEnabled());
        }

        @Override
        public void bindTextureAndSampler(int unit, int texture, int sampler) {
            trace.add("texture:" + unit + ":" + texture + ":" + sampler);
        }

        @Override
        public void setMaterialScalars(int program, MaterialScalars scalars) {
            trace.add("scalars:" + program + ":"
                    + scalars.redMultiplier() + ","
                    + scalars.greenMultiplier() + ","
                    + scalars.blueMultiplier() + ","
                    + scalars.alphaMultiplier());
        }

        @Override
        public void setDirectionalLight(int program, DirectionalLight light) {
            lastDirectionalLight = light;
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
            if (drawFailure != null) {
                throw drawFailure;
            }
        }

        @Override
        public void drawDebugLines(int vertexCount) {
            trace.add("draw:lines:" + vertexCount);
            if (debugDrawFailure != null) {
                throw debugDrawFailure;
            }
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
        private int nextVertexArray = 101;
        private int nextShader = 201;
        private int nextProgram = 203;
        private int nextTexture = 301;
        private int nextSampler = 401;
        private int allocationCalls;
        private int failAllocationCall;
        private int deletedBuffers;
        private int deletedVertexArrays;
        private int deletedShaders;
        private int deletedPrograms;
        private int deletedTextures;
        private int deletedSamplers;
        private int configuredSamplers;
        private final List<Long> allocations = new ArrayList<>();
        private final List<Upload> uploads = new ArrayList<>();
        private final List<TextureAllocation> textureAllocations = new ArrayList<>();

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
            return nextVertexArray++;
        }

        @Override
        public void deleteVertexArray(int handle) {
            deletedVertexArrays++;
        }

        @Override
        public int createTexture() {
            return nextTexture++;
        }

        @Override
        public void deleteTexture(int handle) {
            deletedTextures++;
        }

        @Override
        public void allocateRgba8Texture(
                int handle,
                TextureColorEncoding colorEncoding,
                int width,
                int height,
                ByteBuffer rgbaBytes) {
            ByteBuffer copy = rgbaBytes.duplicate();
            List<Integer> bytes = new ArrayList<>();
            while (copy.hasRemaining()) {
                bytes.add(Byte.toUnsignedInt(copy.get()));
            }
            textureAllocations.add(new TextureAllocation(colorEncoding, width, height, List.copyOf(bytes)));
        }

        @Override
        public int createSampler() {
            return nextSampler++;
        }

        @Override
        public void deleteSampler(int handle) {
            deletedSamplers++;
        }

        @Override
        public void configureLinearClampSampler(int handle) {
            configuredSamplers++;
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
