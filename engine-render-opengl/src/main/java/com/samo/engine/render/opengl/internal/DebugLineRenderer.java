package com.samo.engine.render.opengl.internal;

import com.samo.engine.core.api.DebugFrame;
import com.samo.engine.core.api.NativeResourceRegistry;
import com.samo.engine.platform.api.OpenGlThreadGuard;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

final class DebugLineRenderer implements AutoCloseable {
    private final OpenGlThreadGuard threadGuard;
    private final OpenGlResourceBackend resourceBackend;
    private final OpenGlDrawBackend drawBackend;
    private final OpenGlVertexArray vertexArray;
    private final OpenGlBuffer vertexBuffer;
    private final OpenGlShader vertexShader;
    private final OpenGlShader fragmentShader;
    private final OpenGlProgram program;
    private final ByteBuffer vertexBytes = ByteBuffer.allocateDirect(DebugLineVertexPacker.MAX_BYTES).order(ByteOrder.nativeOrder());
    private boolean closeAttempted;

    private DebugLineRenderer(OpenGlThreadGuard threadGuard, OpenGlResourceBackend resourceBackend, OpenGlDrawBackend drawBackend, OpenGlVertexArray vertexArray,
        OpenGlBuffer vertexBuffer, OpenGlShader vertexShader, OpenGlShader fragmentShader, OpenGlProgram program) {
        this.threadGuard = threadGuard;
        this.resourceBackend = resourceBackend;
        this.drawBackend = drawBackend;
        this.vertexArray = vertexArray;
        this.vertexBuffer = vertexBuffer;
        this.vertexShader = vertexShader;
        this.fragmentShader = fragmentShader;
        this.program = program;
    }

    static DebugLineRenderer create(OpenGlThreadGuard threadGuard, NativeResourceRegistry registry, OpenGlBackendSet backends, int cameraBufferHandle,
        SrgbPresentationMode presentationMode, String vertexSource, String fragmentSource) {
        OpenGlThreadGuard guard = Objects.requireNonNull(threadGuard, "threadGuard");
        NativeResourceRegistry resources = Objects.requireNonNull(registry, "registry");
        OpenGlBackendSet backendSet = Objects.requireNonNull(backends, "backends");
        OpenGlResourceBackend gl = backendSet.resourceBackend();
        OpenGlDrawBackend draw = backendSet.drawBackend();
        OpenGlUniformBlockReflectionBackend reflection = backendSet.reflectionBackend();
        SrgbPresentationMode mode = Objects.requireNonNull(presentationMode, "presentationMode");
        String vertSource = Objects.requireNonNull(vertexSource, "vertexSource");
        String fragSource = Objects.requireNonNull(fragmentSource, "fragmentSource");
        guard.assertOwnerThread();

        OpenGlVertexArray vao = null;
        OpenGlBuffer vertices = null;
        OpenGlShader vertex = null;
        OpenGlShader fragment = null;
        OpenGlProgram linkedProgram = null;
        try {
            vao = OpenGlVertexArray.create(guard, resources, gl);
            vertices = OpenGlBuffer.create(guard, resources, gl);
            gl.allocateDynamicBufferStorage(vertices.handle(), DebugLineVertexPacker.MAX_BYTES);

            vertex = OpenGlShader.compile(OpenGlShader.Stage.VERTEX, "shaders/p5/debug-lines.vert", vertSource, guard, resources, gl);
            fragment = OpenGlShader.compile(OpenGlShader.Stage.FRAGMENT, "shaders/p5/debug-lines.frag", mode.fragmentSource(fragSource), guard, resources, gl);
            linkedProgram = OpenGlProgram.link("p5-debug-lines-program", vertex, fragment, guard, resources, gl);

            UniformBlockLayoutVerifier.verifyCameraOnly(linkedProgram.handle(), guard, reflection);
            draw.configureDebugLineAttributes(vao.handle(), vertices.handle());
            draw.bindUniformBuffer(CameraMatricesUniformBlock.BINDING, cameraBufferHandle);

            return new DebugLineRenderer(guard, gl, draw, vao, vertices, vertex, fragment, linkedProgram);
        } catch (RuntimeException | Error failure) {
            suppressClose(failure, linkedProgram);
            suppressClose(failure, fragment);
            suppressClose(failure, vertex);
            suppressClose(failure, vertices);
            suppressClose(failure, vao);
            throw failure;
        }
    }

    void render(DebugFrame frame, int framebufferWidth, int framebufferHeight) {
        threadGuard.assertOwnerThread();
        requireOpen();
        DebugFrame debugFrame = Objects.requireNonNull(frame, "frame");
        if (debugFrame.primitives().isEmpty()) {
            return;
        }

        vertexBytes.clear();
        int vertexCount = DebugLineVertexPacker.write(debugFrame, vertexBytes);
        vertexBytes.flip();
        resourceBackend.uploadBufferSubData(vertexBuffer.handle(), 0L, vertexBytes);

        drawBackend.setViewport(0, 0, framebufferWidth, framebufferHeight);
        drawBackend.applyDebugLineState();
        drawBackend.useProgram(program.handle());
        drawBackend.bindVertexArray(vertexArray.handle());
        try {
            drawBackend.drawDebugLines(vertexCount);
        } finally {
            drawBackend.bindDefaultVertexArray();
            drawBackend.useDefaultProgram();
        }
    }

    private void requireOpen() {
        if (closeAttempted) {
            throw new IllegalStateException("debug line renderer is closed");
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
            CleanupFailureSuppression.addSuppressedUnlessSame(failure, cleanupFailure);
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
            CleanupFailureSuppression.addSuppressedUnlessSame(first, failures.get(index));
        }
        if (first instanceof RuntimeException runtimeFailure) {
            throw runtimeFailure;
        }
        throw (Error) first;
    }
}
