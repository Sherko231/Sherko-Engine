package com.samo.engine.render.opengl.internal;

import com.samo.engine.core.api.Aabb3f;
import com.samo.engine.core.api.DebugFrame;
import com.samo.engine.core.api.DebugTextCounter;
import com.samo.engine.core.api.EngineLogger;
import com.samo.engine.core.api.Frustum3f;
import com.samo.engine.core.api.NativeResourceRegistry;
import com.samo.engine.platform.api.OpenGlThreadGuard;
import com.samo.engine.render.api.RenderCullingCounters;
import com.samo.engine.render.api.RenderFramePacket;
import com.samo.engine.render.api.RenderLocalLight;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;

public final class IndexedStaticMeshPipeline implements AutoCloseable {
    private static final int ROOM_VERTEX_COUNT = 24;
    private static final int ROOM_INDEX_COUNT = 36;
    private static final int VERTEX_BYTES = ROOM_VERTEX_COUNT * 8 * Float.BYTES;
    private static final int INDEX_BYTES = ROOM_INDEX_COUNT * Integer.BYTES;
    private static final int PROGRAM_KEY_REFERENCE = 0;
    private static final int MATERIAL_KEY_BASELINE = 0;
    private static final int MATERIAL_KEY_TINTED = 1;
    private static final int MESH_KEY_REFERENCE = 0;
    private static final DirectionalLight REFERENCE_DIRECTIONAL_LIGHT =
            new DirectionalLight(0.0f, -1.0f, -1.0f, 1.0f, 1.0f, 1.0f, 0.8f);
    private static final Aabb3f REFERENCE_MESH_WORLD_BOUNDS =
            new Aabb3f(
                    new Vector3f(-2.0f, -1.5f, -3.0f),
                    new Vector3f(2.0f, 1.5f, 0.5f));

    private final OpenGlThreadGuard threadGuard;
    private final OpenGlResourceBackend resourceBackend;
    private final OpenGlDrawBackend drawBackend;
    private final OpenGlVertexArray vertexArray;
    private final OpenGlBuffer vertexBuffer;
    private final OpenGlBuffer indexBuffer;
    private final OpenGlBuffer cameraBuffer;
    private final OpenGlBuffer perFrameBuffer;
    private final OpenGlBuffer localLightBuffer;
    private final OpenGlTexture referenceTexture;
    private final OpenGlSampler referenceSampler;
    private final OpenGlShader vertexShader;
    private final OpenGlShader fragmentShader;
    private final OpenGlProgram program;
    private final DebugLineRenderer debugLineRenderer;
    private final ViewModelRenderer viewModelRenderer;
    private final RendererMaterial baselineMaterial;
    private final RendererMaterial tintedMaterial;
    private final PresentationMode presentationMode;
    private final CpuFrustumCuller frustumCuller = new CpuFrustumCuller();
    private final DrawSubmissionSorter submissionSorter = new DrawSubmissionSorter();
    private final LocalLightSelection localLightSelection;
    private final ByteBuffer cameraBytes =
            ByteBuffer.allocateDirect(CameraUniformBlock.SIZE_BYTES).order(ByteOrder.nativeOrder());
    private final ByteBuffer perFrameBytes =
            ByteBuffer.allocateDirect(PerFrameUniformBlock.SIZE_BYTES).order(ByteOrder.nativeOrder());
    private final ByteBuffer localLightBytes =
            ByteBuffer.allocateDirect(LocalLightUniformBlock.SIZE_BYTES).order(ByteOrder.nativeOrder());
    private final Matrix4f submittedView = new Matrix4f();
    private final Matrix4f submittedProjection = new Matrix4f();
    private RenderCullingCounters lastCullingCounters = RenderCullingCounters.EMPTY;
    private List<DebugTextCounter> lastDebugTextCounters = List.of();
    private boolean closeAttempted;

    private IndexedStaticMeshPipeline(
            OpenGlThreadGuard threadGuard,
            OpenGlResourceBackend resourceBackend,
            OpenGlDrawBackend drawBackend,
            OpenGlVertexArray vertexArray,
            OpenGlBuffer vertexBuffer,
            OpenGlBuffer indexBuffer,
            OpenGlBuffer cameraBuffer,
            OpenGlBuffer perFrameBuffer,
            OpenGlBuffer localLightBuffer,
            OpenGlTexture referenceTexture,
            OpenGlSampler referenceSampler,
            OpenGlShader vertexShader,
            OpenGlShader fragmentShader,
            OpenGlProgram program,
            DebugLineRenderer debugLineRenderer,
            ViewModelRenderer viewModelRenderer,
            RendererMaterial baselineMaterial,
            RendererMaterial tintedMaterial,
            PresentationMode presentationMode,
            EngineLogger logger,
            int maxLocalLights) {
        this.threadGuard = threadGuard;
        this.resourceBackend = resourceBackend;
        this.drawBackend = drawBackend;
        this.vertexArray = vertexArray;
        this.vertexBuffer = vertexBuffer;
        this.indexBuffer = indexBuffer;
        this.cameraBuffer = cameraBuffer;
        this.perFrameBuffer = perFrameBuffer;
        this.localLightBuffer = localLightBuffer;
        this.referenceTexture = referenceTexture;
        this.referenceSampler = referenceSampler;
        this.vertexShader = vertexShader;
        this.fragmentShader = fragmentShader;
        this.program = program;
        this.debugLineRenderer = debugLineRenderer;
        this.viewModelRenderer = viewModelRenderer;
        this.baselineMaterial = baselineMaterial;
        this.tintedMaterial = tintedMaterial;
        this.presentationMode = presentationMode;
        this.localLightSelection = new LocalLightSelection(logger, maxLocalLights);
    }

    public static IndexedStaticMeshPipeline createProduction(
            OpenGlThreadGuard threadGuard,
            NativeResourceRegistry registry,
            String vertexSource,
            String fragmentSource,
            String debugVertexSource,
            String debugFragmentSource,
            String viewModelVertexSource,
            String viewModelFragmentSource) {
        return createProduction(
                threadGuard,
                registry,
                new EngineLogger(event -> { }),
                LocalLightSelection.SHADER_CAPACITY,
                vertexSource,
                fragmentSource,
                debugVertexSource,
                debugFragmentSource,
                viewModelVertexSource,
                viewModelFragmentSource);
    }

    public static IndexedStaticMeshPipeline createProduction(
            OpenGlThreadGuard threadGuard,
            NativeResourceRegistry registry,
            EngineLogger logger,
            int maxLocalLights,
            String vertexSource,
            String fragmentSource,
            String debugVertexSource,
            String debugFragmentSource,
            String viewModelVertexSource,
            String viewModelFragmentSource) {
        return create(
                threadGuard,
                registry,
                new LwjglOpenGlResourceBackend(),
                new LwjglOpenGlDrawBackend(),
                new LwjglOpenGlUniformBlockReflectionBackend(),
                logger,
                maxLocalLights,
                vertexSource,
                fragmentSource,
                debugVertexSource,
                debugFragmentSource,
                viewModelVertexSource,
                viewModelFragmentSource);
    }

    static IndexedStaticMeshPipeline create(
            OpenGlThreadGuard threadGuard,
            NativeResourceRegistry registry,
            OpenGlResourceBackend resourceBackend,
            OpenGlDrawBackend drawBackend,
            OpenGlUniformBlockReflectionBackend reflectionBackend,
            String vertexSource,
            String fragmentSource) {
        return create(
                threadGuard,
                registry,
                resourceBackend,
                drawBackend,
                reflectionBackend,
                new EngineLogger(event -> { }),
                LocalLightSelection.SHADER_CAPACITY,
                vertexSource,
                fragmentSource,
                "#version 460 core\nvoid main() {}",
                "#version 460 core\nvoid main() {}",
                "#version 460 core\nvoid main() {}",
                "#version 460 core\nvoid main() {}");
    }

    static IndexedStaticMeshPipeline create(
            OpenGlThreadGuard threadGuard,
            NativeResourceRegistry registry,
            OpenGlResourceBackend resourceBackend,
            OpenGlDrawBackend drawBackend,
            OpenGlUniformBlockReflectionBackend reflectionBackend,
            EngineLogger logger,
            int maxLocalLights,
            String vertexSource,
            String fragmentSource) {
        return create(
                threadGuard,
                registry,
                resourceBackend,
                drawBackend,
                reflectionBackend,
                logger,
                maxLocalLights,
                vertexSource,
                fragmentSource,
                "#version 460 core\nvoid main() {}",
                "#version 460 core\nvoid main() {}",
                "#version 460 core\nvoid main() {}",
                "#version 460 core\nvoid main() {}");
    }

    static IndexedStaticMeshPipeline create(
            OpenGlThreadGuard threadGuard,
            NativeResourceRegistry registry,
            OpenGlResourceBackend resourceBackend,
            OpenGlDrawBackend drawBackend,
            OpenGlUniformBlockReflectionBackend reflectionBackend,
            EngineLogger logger,
            int maxLocalLights,
            String vertexSource,
            String fragmentSource,
            String debugVertexSource,
            String debugFragmentSource,
            String viewModelVertexSource,
            String viewModelFragmentSource) {
        OpenGlThreadGuard guard = Objects.requireNonNull(threadGuard, "threadGuard");
        NativeResourceRegistry resources = Objects.requireNonNull(registry, "registry");
        OpenGlResourceBackend gl = Objects.requireNonNull(resourceBackend, "resourceBackend");
        OpenGlDrawBackend draw = Objects.requireNonNull(drawBackend, "drawBackend");
        OpenGlUniformBlockReflectionBackend reflection =
                Objects.requireNonNull(reflectionBackend, "reflectionBackend");
        EngineLogger engineLogger = Objects.requireNonNull(logger, "logger");
        if (maxLocalLights < 1 || maxLocalLights > LocalLightSelection.SHADER_CAPACITY) {
            throw new IllegalArgumentException(
                    "maxLocalLights must be within [1," + LocalLightSelection.SHADER_CAPACITY + "]");
        }
        String vertSource = Objects.requireNonNull(vertexSource, "vertexSource");
        String fragSource = Objects.requireNonNull(fragmentSource, "fragmentSource");
        String debugVertSource = Objects.requireNonNull(debugVertexSource, "debugVertexSource");
        String debugFragSource = Objects.requireNonNull(debugFragmentSource, "debugFragmentSource");
        String viewModelVertSource =
                Objects.requireNonNull(viewModelVertexSource, "viewModelVertexSource");
        String viewModelFragSource =
                Objects.requireNonNull(viewModelFragmentSource, "viewModelFragmentSource");
        guard.assertOwnerThread();

        OpenGlVertexArray vao = null;
        OpenGlBuffer vertices = null;
        OpenGlBuffer indices = null;
        OpenGlBuffer camera = null;
        OpenGlBuffer perFrame = null;
        OpenGlBuffer localLights = null;
        OpenGlTexture texture = null;
        OpenGlSampler sampler = null;
        OpenGlShader vertex = null;
        OpenGlShader fragment = null;
        OpenGlProgram linkedProgram = null;
        DebugLineRenderer debugRenderer = null;
        ViewModelRenderer viewModelRenderer = null;
        try {
            vao = OpenGlVertexArray.create(guard, resources, gl);

            vertices = OpenGlBuffer.create(guard, resources, gl);
            gl.allocateDynamicBufferStorage(vertices.handle(), VERTEX_BYTES);
            gl.uploadBufferSubData(vertices.handle(), 0L, roomVertices());

            indices = OpenGlBuffer.create(guard, resources, gl);
            gl.allocateDynamicBufferStorage(indices.handle(), INDEX_BYTES);
            gl.uploadBufferSubData(indices.handle(), 0L, roomIndices());

            camera = OpenGlBuffer.create(guard, resources, gl);
            gl.allocateDynamicBufferStorage(camera.handle(), CameraUniformBlock.SIZE_BYTES);

            perFrame = OpenGlBuffer.create(guard, resources, gl);
            gl.allocateDynamicBufferStorage(perFrame.handle(), PerFrameUniformBlock.SIZE_BYTES);

            localLights = OpenGlBuffer.create(guard, resources, gl);
            gl.allocateDynamicBufferStorage(localLights.handle(), LocalLightUniformBlock.SIZE_BYTES);

            texture = OpenGlTexture.createRgba8(
                    guard,
                    resources,
                    gl,
                    TextureColorEncoding.SRGB_COLOR,
                    4,
                    4,
                    referenceRoomTexture());
            sampler = OpenGlSampler.createLinearClamp(guard, resources, gl);

            vertex = OpenGlShader.compile(
                    OpenGlShader.Stage.VERTEX,
                    "shaders/p5/basic.vert",
                    vertSource,
                    guard,
                    resources,
                    gl);
            PresentationMode presentationMode =
                    PresentationMode.fromDefaultFramebufferEncoding(
                            draw.defaultFramebufferColorEncoding());

            fragment = OpenGlShader.compile(
                    OpenGlShader.Stage.FRAGMENT,
                    "shaders/p5/basic.frag",
                    presentationMode.fragmentSource(fragSource),
                    guard,
                    resources,
                    gl);
            linkedProgram = OpenGlProgram.link(
                    "p5-basic-program",
                    vertex,
                    fragment,
                    guard,
                    resources,
                    gl);

            UniformBlockLayoutVerifier.verify(linkedProgram.handle(), guard, reflection);

            draw.configurePositionNormalUvAttributes(vao.handle(), vertices.handle());
            draw.bindElementBuffer(vao.handle(), indices.handle());
            draw.bindUniformBuffer(CameraUniformBlock.BINDING, camera.handle());
            draw.bindUniformBuffer(PerFrameUniformBlock.BINDING, perFrame.handle());
            draw.bindUniformBuffer(LocalLightUniformBlock.BINDING, localLights.handle());

            MaterialTextureBinding referenceBinding =
                    new MaterialTextureBinding(0, texture.handle(), sampler.handle());
            RendererMaterial baselineMaterial = new RendererMaterial(
                    MaterialShaderVariant.TEXTURED_REFERENCE,
                    List.of(referenceBinding),
                    MaterialScalars.identity(),
                    MaterialBlendMode.OPAQUE,
                    MaterialDepthMode.TEST_WRITE,
                    MaterialCullMode.BACK);
            RendererMaterial tintedMaterial = new RendererMaterial(
                    MaterialShaderVariant.TEXTURED_REFERENCE,
                    List.of(referenceBinding),
                    new MaterialScalars(1.0f, 0.35f, 0.35f, 0.80f),
                    MaterialBlendMode.ALPHA_BLEND,
                    MaterialDepthMode.TEST_NO_WRITE,
                    MaterialCullMode.NONE);

            debugRenderer = DebugLineRenderer.create(
                    guard,
                    resources,
                    gl,
                    draw,
                    reflection,
                    camera.handle(),
                    presentationMode,
                    debugVertSource,
                    debugFragSource);

            viewModelRenderer = ViewModelRenderer.create(
                    guard,
                    resources,
                    gl,
                    draw,
                    reflection,
                    camera.handle(),
                    presentationMode,
                    viewModelVertSource,
                    viewModelFragSource);

            return new IndexedStaticMeshPipeline(
                    guard,
                    gl,
                    draw,
                    vao,
                    vertices,
                    indices,
                    camera,
                    perFrame,
                    localLights,
                    texture,
                    sampler,
                    vertex,
                    fragment,
                    linkedProgram,
                    debugRenderer,
                    viewModelRenderer,
                    baselineMaterial,
                    tintedMaterial,
                    presentationMode,
                    engineLogger,
                    maxLocalLights);
        } catch (RuntimeException | Error failure) {
            suppressClose(failure, viewModelRenderer);
            suppressClose(failure, debugRenderer);
            suppressClose(failure, linkedProgram);
            suppressClose(failure, fragment);
            suppressClose(failure, vertex);
            suppressClose(failure, sampler);
            suppressClose(failure, texture);
            suppressClose(failure, localLights);
            suppressClose(failure, perFrame);
            suppressClose(failure, camera);
            suppressClose(failure, indices);
            suppressClose(failure, vertices);
            suppressClose(failure, vao);
            throw failure;
        }
    }

    public void render(RenderFramePacket frame) {
        threadGuard.assertOwnerThread();
        requireOpen();
        RenderFramePacket snapshot = Objects.requireNonNull(frame, "frame");
        List<RenderLocalLight> selectedLights = localLightSelection.select(snapshot.localLights());
        snapshot.copyViewTo(submittedView);
        snapshot.copyProjectionTo(submittedProjection);
        renderSnapshot(
                submittedView,
                submittedProjection,
                snapshot.framebufferWidth(),
                snapshot.framebufferHeight(),
                selectedLights,
                snapshot.debugFrame());
    }

    public void render(
            Matrix4fc view,
            Matrix4fc projection,
            int framebufferWidth,
            int framebufferHeight) {
        render(new RenderFramePacket(view, projection, framebufferWidth, framebufferHeight));
    }

    private void renderSnapshot(
            Matrix4fc viewMatrix,
            Matrix4fc projectionMatrix,
            int framebufferWidth,
            int framebufferHeight,
            List<RenderLocalLight> localLights,
            DebugFrame debugFrame) {
        Frustum3f frustum = ViewFrustumExtractor.extract(viewMatrix, projectionMatrix);

        cameraBytes.clear();
        CameraUniformBlock.write(viewMatrix, projectionMatrix, cameraBytes);
        cameraBytes.flip();
        resourceBackend.uploadBufferSubData(cameraBuffer.handle(), 0L, cameraBytes);

        perFrameBytes.clear();
        PerFrameUniformBlock.write(framebufferWidth, framebufferHeight, perFrameBytes);
        perFrameBytes.flip();
        resourceBackend.uploadBufferSubData(perFrameBuffer.handle(), 0L, perFrameBytes);

        localLightBytes.clear();
        LocalLightUniformBlock.write(localLights, localLightBytes);
        localLightBytes.flip();
        resourceBackend.uploadBufferSubData(localLightBuffer.handle(), 0L, localLightBytes);

        int testedCandidates = 0;
        int visibleCandidates = 0;
        int culledCandidates = 0;
        int submittedDraws = 0;
        ArrayList<DrawSubmission> visibleSubmissions = new ArrayList<>(1);
        float referenceDepth = cameraDepth(viewMatrix, REFERENCE_MESH_WORLD_BOUNDS);

        testedCandidates++;
        if (frustumCuller.isVisible(frustum, REFERENCE_MESH_WORLD_BOUNDS)) {
            visibleCandidates++;
            visibleSubmissions.add(new DrawSubmission(
                    baselineMaterial,
                    PROGRAM_KEY_REFERENCE,
                    MATERIAL_KEY_BASELINE,
                    MESH_KEY_REFERENCE,
                    referenceDepth,
                    0,
                    0,
                    0,
                    framebufferWidth,
                    framebufferHeight));
        } else {
            culledCandidates++;
        }

        List<DrawSubmission> orderedSubmissions = submissionSorter.sort(visibleSubmissions);

        drawBackend.setViewport(0, 0, framebufferWidth, framebufferHeight);
        drawBackend.setFramebufferSrgbEnabled(presentationMode.framebufferSrgbEnabled());
        try {
            drawBackend.clearFrame(presentationMode);
            for (DrawSubmission submission : orderedSubmissions) {
                drawMaterial(
                        submission.material(),
                        submission.viewportX(),
                        submission.viewportY(),
                        submission.viewportWidth(),
                        submission.viewportHeight());
                submittedDraws++;
            }
            debugLineRenderer.render(debugFrame, framebufferWidth, framebufferHeight);
            viewModelRenderer.render(framebufferWidth, framebufferHeight);
        } finally {
            drawBackend.setViewport(0, 0, framebufferWidth, framebufferHeight);
            drawBackend.setFramebufferSrgbEnabled(false);
        }

        lastCullingCounters = new RenderCullingCounters(
                testedCandidates,
                visibleCandidates,
                culledCandidates,
                submittedDraws);
        lastDebugTextCounters = debugFrame.textCounters();
    }

    public RenderCullingCounters lastCullingCounters() {
        return lastCullingCounters;
    }

    public List<DebugTextCounter> lastDebugTextCounters() {
        return lastDebugTextCounters;
    }

    private static float cameraDepth(Matrix4fc viewMatrix, Aabb3f worldBounds) {
        Vector3f minimum = worldBounds.minimum(new Vector3f());
        Vector3f maximum = worldBounds.maximum(new Vector3f());
        Vector3f center = new Vector3f(
                (minimum.x() + maximum.x()) * 0.5f,
                (minimum.y() + maximum.y()) * 0.5f,
                (minimum.z() + maximum.z()) * 0.5f);
        viewMatrix.transformPosition(center);
        float depth = -center.z();
        if (!Float.isFinite(depth)) {
            throw new IllegalArgumentException("camera-space submission depth must be finite");
        }
        return depth;
    }

    private void drawMaterial(RendererMaterial material, int x, int y, int width, int height) {
        int programHandle = programFor(material.shaderVariant());
        drawBackend.setViewport(x, y, width, height);
        drawBackend.applyMaterialState(material);
        for (MaterialTextureBinding textureBinding : material.textures()) {
            drawBackend.bindTextureAndSampler(
                    textureBinding.unit(),
                    textureBinding.textureHandle(),
                    textureBinding.samplerHandle());
        }
        drawBackend.setMaterialScalars(programHandle, material.scalars());
        drawBackend.setDirectionalLight(programHandle, REFERENCE_DIRECTIONAL_LIGHT);
        drawBackend.useProgram(programHandle);
        drawBackend.bindVertexArray(vertexArray.handle());
        try {
            drawBackend.drawIndexedTriangles(ROOM_INDEX_COUNT);
        } finally {
            drawBackend.bindDefaultVertexArray();
            drawBackend.useDefaultProgram();
            for (MaterialTextureBinding textureBinding : material.textures()) {
                drawBackend.bindTextureAndSampler(textureBinding.unit(), 0, 0);
            }
        }
    }

    private int programFor(MaterialShaderVariant shaderVariant) {
        return switch (shaderVariant) {
            case TEXTURED_REFERENCE -> program.handle();
        };
    }

    private void requireOpen() {
        if (closeAttempted) {
            throw new IllegalStateException("OpenGL renderer is closed");
        }
    }

    @Override
    public void close() {
        if (closeAttempted) {
            return;
        }
        threadGuard.assertOwnerThread();
        closeAttempted = true;

        List<Throwable> failures = new ArrayList<>();
        closeInto(failures, viewModelRenderer);
        closeInto(failures, debugLineRenderer);
        closeInto(failures, program);
        closeInto(failures, fragmentShader);
        closeInto(failures, vertexShader);
        closeInto(failures, referenceSampler);
        closeInto(failures, referenceTexture);
        closeInto(failures, localLightBuffer);
        closeInto(failures, perFrameBuffer);
        closeInto(failures, cameraBuffer);
        closeInto(failures, indexBuffer);
        closeInto(failures, vertexBuffer);
        closeInto(failures, vertexArray);
        throwCleanupFailure(failures);
    }

    private static ByteBuffer roomVertices() {
        ByteBuffer data = ByteBuffer.allocateDirect(VERTEX_BYTES).order(ByteOrder.nativeOrder());

        putRoomQuad(
                data,
                -2.0f, -1.5f, -3.0f,
                2.0f, -1.5f, -3.0f,
                2.0f, 1.5f, -3.0f,
                -2.0f, 1.5f, -3.0f,
                0.0f, 0.0f, 1.0f);
        putRoomQuad(
                data,
                -2.0f, -1.5f, 0.5f,
                2.0f, -1.5f, 0.5f,
                2.0f, -1.5f, -3.0f,
                -2.0f, -1.5f, -3.0f,
                0.0f, 1.0f, 0.0f);
        putRoomQuad(
                data,
                -2.0f, 1.5f, -3.0f,
                2.0f, 1.5f, -3.0f,
                2.0f, 1.5f, 0.5f,
                -2.0f, 1.5f, 0.5f,
                0.0f, -1.0f, 0.0f);
        putRoomQuad(
                data,
                -2.0f, -1.5f, 0.5f,
                -2.0f, -1.5f, -3.0f,
                -2.0f, 1.5f, -3.0f,
                -2.0f, 1.5f, 0.5f,
                1.0f, 0.0f, 0.0f);
        putRoomQuad(
                data,
                2.0f, -1.5f, -3.0f,
                2.0f, -1.5f, 0.5f,
                2.0f, 1.5f, 0.5f,
                2.0f, 1.5f, -3.0f,
                -1.0f, 0.0f, 0.0f);
        putRoomQuadUv(
                data,
                -1.20f, -0.80f, -1.0f,
                1.20f, -0.80f, -1.0f,
                1.20f, 0.80f, -1.0f,
                -1.20f, 0.80f, -1.0f,
                0.0f, 0.0f, 1.0f,
                0.25f, 0.25f);
        return data.flip();
    }

    private static void putRoomQuad(
            ByteBuffer data,
            float x0, float y0, float z0,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float x3, float y3, float z3,
            float nx, float ny, float nz) {
        putRoomQuadUv(
                data,
                x0, y0, z0,
                x1, y1, z1,
                x2, y2, z2,
                x3, y3, z3,
                nx, ny, nz,
                1.0f, 1.0f);
    }

    private static void putRoomQuadUv(
            ByteBuffer data,
            float x0, float y0, float z0,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float x3, float y3, float z3,
            float nx, float ny, float nz,
            float maxU, float maxV) {
        putRoomVertex(data, x0, y0, z0, nx, ny, nz, 0.0f, 0.0f);
        putRoomVertex(data, x1, y1, z1, nx, ny, nz, maxU, 0.0f);
        putRoomVertex(data, x2, y2, z2, nx, ny, nz, maxU, maxV);
        putRoomVertex(data, x3, y3, z3, nx, ny, nz, 0.0f, maxV);
    }

    private static void putRoomVertex(
            ByteBuffer data,
            float x, float y, float z,
            float nx, float ny, float nz,
            float u, float v) {
        data.putFloat(x).putFloat(y).putFloat(z);
        data.putFloat(nx).putFloat(ny).putFloat(nz);
        data.putFloat(u).putFloat(v);
    }

    private static ByteBuffer roomIndices() {
        ByteBuffer data = ByteBuffer.allocateDirect(INDEX_BYTES).order(ByteOrder.nativeOrder());
        for (int quad = 0; quad < 6; quad++) {
            int base = quad * 4;
            data.putInt(base);
            data.putInt(base + 1);
            data.putInt(base + 2);
            data.putInt(base);
            data.putInt(base + 2);
            data.putInt(base + 3);
        }
        return data.flip();
    }

    private static ByteBuffer referenceRoomTexture() {
        int[] rgba = {
            128, 128, 128, 255,  70, 150, 210, 255,  210, 110, 70, 255,   70, 150, 210, 255,
             70, 150, 210, 255, 210, 110, 70, 255,    70, 150, 210, 255, 210, 110, 70, 255,
            210, 110, 70, 255,   70, 150, 210, 255,  210, 110, 70, 255,   70, 150, 210, 255,
             70, 150, 210, 255, 210, 110, 70, 255,    70, 150, 210, 255, 210, 110, 70, 255
        };
        ByteBuffer data = ByteBuffer.allocateDirect(rgba.length).order(ByteOrder.nativeOrder());
        for (int component : rgba) {
            data.put((byte) component);
        }
        return data.flip();
    }

    private static void suppressClose(Throwable failure, AutoCloseable resource) {
        if (resource == null) {
            return;
        }
        try {
            resource.close();
        } catch (RuntimeException | Error cleanupFailure) {
            if (cleanupFailure != failure) {
                failure.addSuppressed(cleanupFailure);
            }
        } catch (Exception impossible) {
            throw new AssertionError(impossible);
        }
    }

    private static void closeInto(List<Throwable> failures, AutoCloseable resource) {
        try {
            resource.close();
        } catch (RuntimeException | Error failure) {
            failures.add(failure);
        } catch (Exception impossible) {
            throw new AssertionError(impossible);
        }
    }

    private static void throwCleanupFailure(List<Throwable> failures) {
        if (failures.isEmpty()) {
            return;
        }
        Throwable first = failures.getFirst();
        for (int index = 1; index < failures.size(); index++) {
            Throwable suppressed = failures.get(index);
            if (suppressed != first) {
                first.addSuppressed(suppressed);
            }
        }
        if (first instanceof RuntimeException runtimeFailure) {
            throw runtimeFailure;
        }
        throw (Error) first;
    }
}
