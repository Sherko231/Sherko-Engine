package com.samo.engine.render.opengl.internal;

import com.samo.engine.core.api.NativeResourceRegistry;
import com.samo.engine.platform.api.OpenGlThreadGuard;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.joml.Matrix4f;

final class ViewModelRenderer implements AutoCloseable {
    private static final int VERTEX_COUNT = 6;
    private static final int FLOATS_PER_VERTEX = 6;
    private static final int VERTEX_BYTES = VERTEX_COUNT * FLOATS_PER_VERTEX * Float.BYTES;

    private static final float COLOR_RED = 0.95f;
    private static final float COLOR_GREEN = 0.55f;
    private static final float COLOR_BLUE = 0.15f;

    private final OpenGlThreadGuard threadGuard;
    private final OpenGlResourceBackend resourceBackend;
    private final OpenGlDrawBackend drawBackend;
    private final OpenGlVertexArray vertexArray;
    private final OpenGlBuffer vertexBuffer;
    private final OpenGlBuffer cameraBuffer;
    private final OpenGlShader vertexShader;
    private final OpenGlShader fragmentShader;
    private final OpenGlProgram program;
    private final int worldCameraBufferHandle;
    private final ByteBuffer cameraBytes =
            ByteBuffer.allocateDirect(CameraUniformBlock.SIZE_BYTES).order(ByteOrder.nativeOrder());
    private final Matrix4f identityView = new Matrix4f();
    private final Matrix4f projection = new Matrix4f();
    private boolean closeAttempted;

    private ViewModelRenderer(
            OpenGlThreadGuard threadGuard,
            OpenGlResourceBackend resourceBackend,
            OpenGlDrawBackend drawBackend,
            OpenGlVertexArray vertexArray,
            OpenGlBuffer vertexBuffer,
            OpenGlBuffer cameraBuffer,
            OpenGlShader vertexShader,
            OpenGlShader fragmentShader,
            OpenGlProgram program,
            int worldCameraBufferHandle) {
        this.threadGuard = threadGuard;
        this.resourceBackend = resourceBackend;
        this.drawBackend = drawBackend;
        this.vertexArray = vertexArray;
        this.vertexBuffer = vertexBuffer;
        this.cameraBuffer = cameraBuffer;
        this.vertexShader = vertexShader;
        this.fragmentShader = fragmentShader;
        this.program = program;
        this.worldCameraBufferHandle = worldCameraBufferHandle;
    }

    static ViewModelRenderer create(
            OpenGlThreadGuard threadGuard,
            NativeResourceRegistry registry,
            OpenGlResourceBackend resourceBackend,
            OpenGlDrawBackend drawBackend,
            OpenGlUniformBlockReflectionBackend reflectionBackend,
            int worldCameraBufferHandle,
            PresentationMode presentationMode,
            String vertexSource,
            String fragmentSource) {
        OpenGlThreadGuard guard = Objects.requireNonNull(threadGuard, "threadGuard");
        NativeResourceRegistry resources = Objects.requireNonNull(registry, "registry");
        OpenGlResourceBackend gl = Objects.requireNonNull(resourceBackend, "resourceBackend");
        OpenGlDrawBackend draw = Objects.requireNonNull(drawBackend, "drawBackend");
        OpenGlUniformBlockReflectionBackend reflection =
                Objects.requireNonNull(reflectionBackend, "reflectionBackend");
        PresentationMode mode = Objects.requireNonNull(presentationMode, "presentationMode");
        String vertSource = Objects.requireNonNull(vertexSource, "vertexSource");
        String fragSource = Objects.requireNonNull(fragmentSource, "fragmentSource");
        if (worldCameraBufferHandle <= 0) {
            throw new IllegalArgumentException("worldCameraBufferHandle must be positive");
        }
        guard.assertOwnerThread();

        OpenGlVertexArray vao = null;
        OpenGlBuffer vertices = null;
        OpenGlBuffer camera = null;
        OpenGlShader vertex = null;
        OpenGlShader fragment = null;
        OpenGlProgram linkedProgram = null;
        try {
            vao = OpenGlVertexArray.create(guard, resources, gl);
            vertices = OpenGlBuffer.create(guard, resources, gl);
            gl.allocateDynamicBufferStorage(vertices.handle(), VERTEX_BYTES);
            gl.uploadBufferSubData(vertices.handle(), 0L, fixtureVertices());

            camera = OpenGlBuffer.create(guard, resources, gl);
            gl.allocateDynamicBufferStorage(camera.handle(), CameraUniformBlock.SIZE_BYTES);

            vertex = OpenGlShader.compile(
                    OpenGlShader.Stage.VERTEX,
                    "shaders/p5/view-model.vert",
                    vertSource,
                    guard,
                    resources,
                    gl);
            fragment = OpenGlShader.compile(
                    OpenGlShader.Stage.FRAGMENT,
                    "shaders/p5/view-model.frag",
                    mode.fragmentSource(fragSource),
                    guard,
                    resources,
                    gl);
            linkedProgram = OpenGlProgram.link(
                    "p5-view-model-program",
                    vertex,
                    fragment,
                    guard,
                    resources,
                    gl);

            UniformBlockLayoutVerifier.verifyCameraOnly(
                    linkedProgram.handle(), guard, reflection);
            draw.configureViewModelAttributes(vao.handle(), vertices.handle());

            return new ViewModelRenderer(
                    guard,
                    gl,
                    draw,
                    vao,
                    vertices,
                    camera,
                    vertex,
                    fragment,
                    linkedProgram,
                    worldCameraBufferHandle);
        } catch (RuntimeException | Error failure) {
            suppressClose(failure, linkedProgram);
            suppressClose(failure, fragment);
            suppressClose(failure, vertex);
            suppressClose(failure, camera);
            suppressClose(failure, vertices);
            suppressClose(failure, vao);
            throw failure;
        }
    }

    void render(int framebufferWidth, int framebufferHeight) {
        threadGuard.assertOwnerThread();
        requireOpen();

        ViewModelProjection.build(framebufferWidth, framebufferHeight, projection);
        cameraBytes.clear();
        CameraUniformBlock.write(identityView, projection, cameraBytes);
        cameraBytes.flip();
        resourceBackend.uploadBufferSubData(cameraBuffer.handle(), 0L, cameraBytes);

        drawBackend.setViewport(0, 0, framebufferWidth, framebufferHeight);
        drawBackend.clearDepthOnly();
        drawBackend.applyViewModelState();
        try {
            drawBackend.bindUniformBuffer(CameraUniformBlock.BINDING, cameraBuffer.handle());
            drawBackend.useProgram(program.handle());
            drawBackend.bindVertexArray(vertexArray.handle());
            drawBackend.drawViewModelTriangles(VERTEX_COUNT);
        } finally {
            drawBackend.bindDefaultVertexArray();
            drawBackend.useDefaultProgram();
            drawBackend.bindUniformBuffer(CameraUniformBlock.BINDING, worldCameraBufferHandle);
        }
    }

    private static ByteBuffer fixtureVertices() {
        ByteBuffer data = ByteBuffer.allocateDirect(VERTEX_BYTES).order(ByteOrder.nativeOrder());
        putVertex(data, -0.12f, -0.23f, -0.50f);
        putVertex(data, 0.36f, -0.23f, -0.50f);
        putVertex(data, 0.36f, -0.06f, -0.50f);
        putVertex(data, -0.12f, -0.23f, -0.50f);
        putVertex(data, 0.36f, -0.06f, -0.50f);
        putVertex(data, -0.12f, -0.06f, -0.50f);
        return data.flip();
    }

    private static void putVertex(ByteBuffer data, float x, float y, float z) {
        data.putFloat(x).putFloat(y).putFloat(z);
        data.putFloat(COLOR_RED).putFloat(COLOR_GREEN).putFloat(COLOR_BLUE);
    }

    private void requireOpen() {
        if (closeAttempted) {
            throw new IllegalStateException("view-model renderer is closed");
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
        closeInto(failures, program);
        closeInto(failures, fragmentShader);
        closeInto(failures, vertexShader);
        closeInto(failures, cameraBuffer);
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
            CleanupFailures.addSuppressedUnlessSame(failure, cleanupFailure);
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
            CleanupFailures.addSuppressedUnlessSame(first, failures.get(index));
        }
        if (first instanceof RuntimeException runtimeFailure) {
            throw runtimeFailure;
        }
        throw (Error) first;
    }
}
