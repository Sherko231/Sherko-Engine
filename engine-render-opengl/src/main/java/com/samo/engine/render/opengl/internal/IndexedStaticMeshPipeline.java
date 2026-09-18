package com.samo.engine.render.opengl.internal;

import com.samo.engine.core.api.NativeResourceRegistry;
import com.samo.engine.platform.api.OpenGlThreadGuard;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.joml.Matrix4fc;

public final class IndexedStaticMeshPipeline implements AutoCloseable {
    private static final int VERTEX_BYTES = 9 * Float.BYTES;
    private static final int INDEX_BYTES = 3 * Integer.BYTES;

    private final OpenGlThreadGuard threadGuard;
    private final OpenGlResourceBackend resourceBackend;
    private final OpenGlDrawBackend drawBackend;
    private final OpenGlVertexArray vertexArray;
    private final OpenGlBuffer vertexBuffer;
    private final OpenGlBuffer indexBuffer;
    private final OpenGlBuffer cameraBuffer;
    private final OpenGlBuffer perFrameBuffer;
    private final OpenGlTexture referenceTexture;
    private final OpenGlSampler referenceSampler;
    private final OpenGlShader vertexShader;
    private final OpenGlShader fragmentShader;
    private final OpenGlProgram program;
    private final boolean hardwareFramebufferSrgb;
    private final ByteBuffer cameraBytes =
            ByteBuffer.allocateDirect(CameraUniformBlock.SIZE_BYTES).order(ByteOrder.nativeOrder());
    private final ByteBuffer perFrameBytes =
            ByteBuffer.allocateDirect(PerFrameUniformBlock.SIZE_BYTES).order(ByteOrder.nativeOrder());
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
            OpenGlTexture referenceTexture,
            OpenGlSampler referenceSampler,
            OpenGlShader vertexShader,
            OpenGlShader fragmentShader,
            OpenGlProgram program,
            boolean hardwareFramebufferSrgb) {
        this.threadGuard = threadGuard;
        this.resourceBackend = resourceBackend;
        this.drawBackend = drawBackend;
        this.vertexArray = vertexArray;
        this.vertexBuffer = vertexBuffer;
        this.indexBuffer = indexBuffer;
        this.cameraBuffer = cameraBuffer;
        this.perFrameBuffer = perFrameBuffer;
        this.referenceTexture = referenceTexture;
        this.referenceSampler = referenceSampler;
        this.vertexShader = vertexShader;
        this.fragmentShader = fragmentShader;
        this.program = program;
        this.hardwareFramebufferSrgb = hardwareFramebufferSrgb;
    }

    public static IndexedStaticMeshPipeline createProduction(
            OpenGlThreadGuard threadGuard,
            NativeResourceRegistry registry,
            String vertexSource,
            String fragmentSource) {
        return create(
                threadGuard,
                registry,
                new LwjglOpenGlResourceBackend(),
                new LwjglOpenGlDrawBackend(),
                new LwjglOpenGlUniformBlockReflectionBackend(),
                vertexSource,
                fragmentSource);
    }

    static IndexedStaticMeshPipeline create(
            OpenGlThreadGuard threadGuard,
            NativeResourceRegistry registry,
            OpenGlResourceBackend resourceBackend,
            OpenGlDrawBackend drawBackend,
            OpenGlUniformBlockReflectionBackend reflectionBackend,
            String vertexSource,
            String fragmentSource) {
        OpenGlThreadGuard guard = Objects.requireNonNull(threadGuard, "threadGuard");
        NativeResourceRegistry resources = Objects.requireNonNull(registry, "registry");
        OpenGlResourceBackend gl = Objects.requireNonNull(resourceBackend, "resourceBackend");
        OpenGlDrawBackend draw = Objects.requireNonNull(drawBackend, "drawBackend");
        OpenGlUniformBlockReflectionBackend reflection =
                Objects.requireNonNull(reflectionBackend, "reflectionBackend");
        String vertSource = Objects.requireNonNull(vertexSource, "vertexSource");
        String fragSource = Objects.requireNonNull(fragmentSource, "fragmentSource");
        guard.assertOwnerThread();

        OpenGlVertexArray vao = null;
        OpenGlBuffer vertices = null;
        OpenGlBuffer indices = null;
        OpenGlBuffer camera = null;
        OpenGlBuffer perFrame = null;
        OpenGlTexture texture = null;
        OpenGlSampler sampler = null;
        OpenGlShader vertex = null;
        OpenGlShader fragment = null;
        OpenGlProgram linkedProgram = null;
        try {
            vao = OpenGlVertexArray.create(guard, resources, gl);

            vertices = OpenGlBuffer.create(guard, resources, gl);
            gl.allocateDynamicBufferStorage(vertices.handle(), VERTEX_BYTES);
            gl.uploadBufferSubData(vertices.handle(), 0L, triangleVertices());

            indices = OpenGlBuffer.create(guard, resources, gl);
            gl.allocateDynamicBufferStorage(indices.handle(), INDEX_BYTES);
            gl.uploadBufferSubData(indices.handle(), 0L, triangleIndices());

            camera = OpenGlBuffer.create(guard, resources, gl);
            gl.allocateDynamicBufferStorage(camera.handle(), CameraUniformBlock.SIZE_BYTES);

            perFrame = OpenGlBuffer.create(guard, resources, gl);
            gl.allocateDynamicBufferStorage(perFrame.handle(), PerFrameUniformBlock.SIZE_BYTES);

            texture = OpenGlTexture.createRgba8(
                    guard,
                    resources,
                    gl,
                    TextureColorEncoding.SRGB_COLOR,
                    1,
                    1,
                    referenceGrayTexture());
            sampler = OpenGlSampler.createLinearClamp(guard, resources, gl);

            vertex = OpenGlShader.compile(
                    OpenGlShader.Stage.VERTEX,
                    "shaders/p5/basic.vert",
                    vertSource,
                    guard,
                    resources,
                    gl);
            int framebufferEncoding = draw.defaultFramebufferColorEncoding();
            boolean hardwareSrgb;
            if (framebufferEncoding == org.lwjgl.opengl.GL21.GL_SRGB) {
                hardwareSrgb = true;
            } else if (framebufferEncoding == org.lwjgl.opengl.GL11.GL_LINEAR) {
                hardwareSrgb = false;
            } else {
                throw new IllegalStateException(
                        "Unsupported default framebuffer color encoding: " + framebufferEncoding);
            }

            fragment = OpenGlShader.compile(
                    OpenGlShader.Stage.FRAGMENT,
                    "shaders/p5/basic.frag",
                    fragmentSourceForPresentation(fragSource, hardwareSrgb),
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

            draw.configurePositionAttribute(vao.handle(), vertices.handle());
            draw.bindElementBuffer(vao.handle(), indices.handle());
            draw.bindUniformBuffer(CameraUniformBlock.BINDING, camera.handle());
            draw.bindUniformBuffer(PerFrameUniformBlock.BINDING, perFrame.handle());

            return new IndexedStaticMeshPipeline(
                    guard,
                    gl,
                    draw,
                    vao,
                    vertices,
                    indices,
                    camera,
                    perFrame,
                    texture,
                    sampler,
                    vertex,
                    fragment,
                    linkedProgram,
                    hardwareSrgb);
        } catch (RuntimeException | Error failure) {
            suppressClose(failure, linkedProgram);
            suppressClose(failure, fragment);
            suppressClose(failure, vertex);
            suppressClose(failure, sampler);
            suppressClose(failure, texture);
            suppressClose(failure, perFrame);
            suppressClose(failure, camera);
            suppressClose(failure, indices);
            suppressClose(failure, vertices);
            suppressClose(failure, vao);
            throw failure;
        }
    }

    public void render(
            Matrix4fc view,
            Matrix4fc projection,
            int framebufferWidth,
            int framebufferHeight) {
        threadGuard.assertOwnerThread();
        requireOpen();
        Matrix4fc viewMatrix = Objects.requireNonNull(view, "view");
        Matrix4fc projectionMatrix = Objects.requireNonNull(projection, "projection");
        if (framebufferWidth <= 0) {
            throw new IllegalArgumentException("framebufferWidth must be positive");
        }
        if (framebufferHeight <= 0) {
            throw new IllegalArgumentException("framebufferHeight must be positive");
        }

        cameraBytes.clear();
        CameraUniformBlock.write(viewMatrix, projectionMatrix, cameraBytes);
        cameraBytes.flip();
        resourceBackend.uploadBufferSubData(cameraBuffer.handle(), 0L, cameraBytes);

        perFrameBytes.clear();
        PerFrameUniformBlock.write(framebufferWidth, framebufferHeight, perFrameBytes);
        perFrameBytes.flip();
        resourceBackend.uploadBufferSubData(perFrameBuffer.handle(), 0L, perFrameBytes);

        drawBackend.setViewport(framebufferWidth, framebufferHeight);
        drawBackend.configureDepthAndBackFaceCull();
        drawBackend.setFramebufferSrgbEnabled(hardwareFramebufferSrgb);
        try {
            drawBackend.clearFrame(hardwareFramebufferSrgb);
            drawBackend.bindTextureAndSampler(0, referenceTexture.handle(), referenceSampler.handle());
            drawBackend.useProgram(program.handle());
            drawBackend.bindVertexArray(vertexArray.handle());
            try {
                drawBackend.drawIndexedTriangle();
            } finally {
                drawBackend.bindDefaultVertexArray();
                drawBackend.useDefaultProgram();
                drawBackend.bindTextureAndSampler(0, 0, 0);
            }
        } finally {
            drawBackend.setFramebufferSrgbEnabled(false);
        }
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
        closeInto(failures, program);
        closeInto(failures, fragmentShader);
        closeInto(failures, vertexShader);
        closeInto(failures, referenceSampler);
        closeInto(failures, referenceTexture);
        closeInto(failures, perFrameBuffer);
        closeInto(failures, cameraBuffer);
        closeInto(failures, indexBuffer);
        closeInto(failures, vertexBuffer);
        closeInto(failures, vertexArray);
        throwCleanupFailure(failures);
    }

    private static ByteBuffer triangleVertices() {
        ByteBuffer data = ByteBuffer.allocateDirect(VERTEX_BYTES).order(ByteOrder.nativeOrder());
        data.putFloat(-0.60f).putFloat(-0.50f).putFloat(0.0f);
        data.putFloat(0.60f).putFloat(-0.50f).putFloat(0.0f);
        data.putFloat(0.0f).putFloat(0.60f).putFloat(0.0f);
        return data.flip();
    }

    private static ByteBuffer triangleIndices() {
        ByteBuffer data = ByteBuffer.allocateDirect(INDEX_BYTES).order(ByteOrder.nativeOrder());
        data.putInt(0).putInt(1).putInt(2);
        return data.flip();
    }

    static String fragmentSourceForPresentation(String fragmentSource, boolean hardwareFramebufferSrgb) {
        String source = Objects.requireNonNull(fragmentSource, "fragmentSource");
        if (hardwareFramebufferSrgb) {
            return source;
        }
        String version = "#version 460 core";
        if (!source.startsWith(version)) {
            throw new IllegalArgumentException(
                    "Fragment shader must start with '" + version + "' for P5-T08 variant injection");
        }
        return version
                + System.lineSeparator()
                + "#define SHERKO_MANUAL_SRGB_ENCODE 1"
                + source.substring(version.length());
    }

    private static ByteBuffer referenceGrayTexture() {
        ByteBuffer data = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder());
        data.put((byte) 128).put((byte) 128).put((byte) 128).put((byte) 255);
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
