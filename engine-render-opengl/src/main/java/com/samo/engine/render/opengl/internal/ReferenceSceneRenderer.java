package com.samo.engine.render.opengl.internal;

import com.samo.engine.core.api.DebugFrame;
import com.samo.engine.core.api.DebugTextCounter;
import com.samo.engine.core.api.Frustum3f;
import com.samo.engine.core.api.EngineLogger;
import com.samo.engine.core.api.NativeResourceRegistry;
import com.samo.engine.platform.api.OpenGlThreadGuard;
import com.samo.engine.render.api.RenderCullingCounters;
import com.samo.engine.render.api.RenderFramePacket;
import com.samo.engine.render.api.RenderLocalLight;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

public final class ReferenceSceneRenderer implements AutoCloseable {
    private final OpenGlThreadGuard threadGuard;
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
    private final LocalLightSelector localLightSelector;
    private final RendererFrameUniformUploader frameUniformUploader;
    private final ReferenceSceneVisibilityPlanner visibilityPlanner;
    private final ReferenceSceneDrawExecutor drawExecutor;
    private final RendererFrameDiagnostics frameDiagnostics = new RendererFrameDiagnostics();
    private final Matrix4f submittedView = new Matrix4f();
    private final Matrix4f submittedProjection = new Matrix4f();
    private boolean closeAttempted;

    private ReferenceSceneRenderer(
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
            RenderMaterialDescriptor baselineMaterial,
            PresentationMode presentationMode,
            EngineLogger logger,
            int maxLocalLights) {
        this.threadGuard = threadGuard;
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
        this.localLightSelector = new LocalLightSelector(logger, maxLocalLights);
        this.frameUniformUploader = new RendererFrameUniformUploader(
                resourceBackend,
                cameraBuffer.handle(),
                perFrameBuffer.handle(),
                localLightBuffer.handle());
        this.visibilityPlanner = new ReferenceSceneVisibilityPlanner(
                baselineMaterial,
                new CpuFrustumCuller(),
                new DrawSubmissionSorter());
        this.drawExecutor = new ReferenceSceneDrawExecutor(
                drawBackend,
                program.handle(),
                vertexArray.handle(),
                debugLineRenderer,
                viewModelRenderer,
                presentationMode);
    }

    public static ReferenceSceneRenderer createProduction(
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
                LocalLightSelector.SHADER_CAPACITY,
                vertexSource,
                fragmentSource,
                debugVertexSource,
                debugFragmentSource,
                viewModelVertexSource,
                viewModelFragmentSource);
    }

    public static ReferenceSceneRenderer createProduction(
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

    static ReferenceSceneRenderer create(
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
                LocalLightSelector.SHADER_CAPACITY,
                vertexSource,
                fragmentSource,
                "#version 460 core\nvoid main() {}",
                "#version 460 core\nvoid main() {}",
                "#version 460 core\nvoid main() {}",
                "#version 460 core\nvoid main() {}");
    }

    static ReferenceSceneRenderer create(
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

    static ReferenceSceneRenderer create(
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
        if (maxLocalLights < 1 || maxLocalLights > LocalLightSelector.SHADER_CAPACITY) {
            throw new IllegalArgumentException(
                    "maxLocalLights must be within [1," + LocalLightSelector.SHADER_CAPACITY + "]");
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
            gl.allocateDynamicBufferStorage(vertices.handle(), ReferenceRoomFixture.VERTEX_BYTES);
            gl.uploadBufferSubData(vertices.handle(), 0L, ReferenceRoomFixture.vertices());

            indices = OpenGlBuffer.create(guard, resources, gl);
            gl.allocateDynamicBufferStorage(indices.handle(), ReferenceRoomFixture.INDEX_BYTES);
            gl.uploadBufferSubData(indices.handle(), 0L, ReferenceRoomFixture.indices());

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
                    ReferenceRoomFixture.TEXTURE_WIDTH,
                    ReferenceRoomFixture.TEXTURE_HEIGHT,
                    ReferenceRoomFixture.textureRgba());
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
            RenderMaterialDescriptor baselineMaterial = new RenderMaterialDescriptor(
                    MaterialShaderVariant.TEXTURED_REFERENCE,
                    List.of(referenceBinding),
                    MaterialScalars.identity(),
                    MaterialBlendMode.OPAQUE,
                    MaterialDepthMode.TEST_WRITE,
                    MaterialCullMode.BACK);
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

            return new ReferenceSceneRenderer(
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
        List<RenderLocalLight> selectedLights = localLightSelector.select(snapshot.localLights());
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
        Frustum3f frustum = visibilityPlanner.extractFrustum(viewMatrix, projectionMatrix);

        frameUniformUploader.upload(
                viewMatrix,
                projectionMatrix,
                framebufferWidth,
                framebufferHeight,
                localLights);

        ReferenceSceneVisibilityPlanner.VisibilityPlan visibilityPlan =
                visibilityPlanner.plan(
                        viewMatrix,
                        frustum,
                        framebufferWidth,
                        framebufferHeight);

        int submittedDraws = drawExecutor.execute(
                visibilityPlan.orderedSubmissions(),
                debugFrame,
                framebufferWidth,
                framebufferHeight);

        frameDiagnostics.publish(visibilityPlan, submittedDraws, debugFrame);
    }

    public RenderCullingCounters lastCullingCounters() {
        return frameDiagnostics.lastCullingCounters();
    }

    public List<DebugTextCounter> lastDebugTextCounters() {
        return frameDiagnostics.lastDebugTextCounters();
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
