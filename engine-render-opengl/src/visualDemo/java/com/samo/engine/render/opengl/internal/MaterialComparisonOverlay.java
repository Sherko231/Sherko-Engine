package com.samo.engine.render.opengl.internal;

import com.samo.engine.core.api.NativeResourceRegistry;
import com.samo.engine.platform.api.OpenGlThreadGuard;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import org.lwjgl.opengl.GL41;

final class MaterialComparisonOverlay implements AutoCloseable {
    private static final int VERTEX_BYTES = 4 * 8 * Float.BYTES;
    private static final int INDEX_BYTES = 6 * Integer.BYTES;

    private static final String VERTEX_SHADER = """
        #version 460 core
        layout(location = 0) in vec3 position;
        layout(location = 1) uniform float horizontalOffset;
        void main() {
            gl_Position = vec4(position.x + horizontalOffset, position.y, position.z, 1.0);
        }
        """;

    private static final String FRAGMENT_SHADER = """
        #version 460 core
        layout(location = 0) uniform vec4 materialColorMultiplier;
        layout(location = 0) out vec4 color;

        vec3 linearToSrgb(vec3 linearColor) {
            bvec3 cutoff = lessThanEqual(linearColor, vec3(0.0031308));
            vec3 lower = linearColor * 12.92;
            vec3 upper = 1.055 * pow(linearColor, vec3(1.0 / 2.4)) - 0.055;
            return mix(upper, lower, cutoff);
        }

        void main() {
        #ifdef SHERKO_MANUAL_SRGB_ENCODE
            color = vec4(
                linearToSrgb(materialColorMultiplier.rgb),
                materialColorMultiplier.a);
        #else
            color = materialColorMultiplier;
        #endif
        }
        """;

    private final OpenGlDrawBackend draw;
    private final OpenGlVertexArray vertexArray;
    private final OpenGlBuffer vertexBuffer;
    private final OpenGlBuffer indexBuffer;
    private final OpenGlTexture texture;
    private final OpenGlSampler sampler;
    private final OpenGlShader vertexShader;
    private final OpenGlShader fragmentShader;
    private final OpenGlProgram program;
    private final RenderMaterialDescriptor opaque;
    private final RenderMaterialDescriptor transparent;
    private final SrgbPresentationMode presentationMode;
    private boolean closed;

    private MaterialComparisonOverlay(OpenGlDrawBackend draw, OpenGlVertexArray vertexArray, OpenGlBuffer vertexBuffer, OpenGlBuffer indexBuffer, OpenGlTexture texture,
        OpenGlSampler sampler, OpenGlShader vertexShader, OpenGlShader fragmentShader, OpenGlProgram program, RenderMaterialDescriptor opaque, RenderMaterialDescriptor transparent,
        SrgbPresentationMode presentationMode) {
        this.draw = draw;
        this.vertexArray = vertexArray;
        this.vertexBuffer = vertexBuffer;
        this.indexBuffer = indexBuffer;
        this.texture = texture;
        this.sampler = sampler;
        this.vertexShader = vertexShader;
        this.fragmentShader = fragmentShader;
        this.program = program;
        this.opaque = opaque;
        this.transparent = transparent;
        this.presentationMode = presentationMode;
    }

    static MaterialComparisonOverlay create(OpenGlThreadGuard guard, NativeResourceRegistry registry) {
        OpenGlResourceBackend resources = new LwjglOpenGlResourceBackend();
        OpenGlDrawBackend draw = new LwjglOpenGlDrawBackend();

        OpenGlVertexArray vao = null;
        OpenGlBuffer vertices = null;
        OpenGlBuffer indices = null;
        OpenGlTexture texture = null;
        OpenGlSampler sampler = null;
        OpenGlShader vertex = null;
        OpenGlShader fragment = null;
        OpenGlProgram program = null;
        try {
            vao = OpenGlVertexArray.create(guard, registry, resources);

            vertices = OpenGlBuffer.create(guard, registry, resources);
            resources.allocateDynamicBufferStorage(vertices.handle(), VERTEX_BYTES);
            resources.uploadBufferSubData(vertices.handle(), 0L, overlayVertices());

            indices = OpenGlBuffer.create(guard, registry, resources);
            resources.allocateDynamicBufferStorage(indices.handle(), INDEX_BYTES);
            resources.uploadBufferSubData(indices.handle(), 0L, overlayIndices());

            draw.configurePositionNormalUvAttributes(vao.handle(), vertices.handle());
            draw.bindElementBuffer(vao.handle(), indices.handle());

            texture = OpenGlTexture.createRgba8(guard, registry, resources, TextureColorEncoding.LINEAR_DATA, 1, 1,
                ByteBuffer.allocateDirect(4).put((byte) 255).put((byte) 255).put((byte) 255).put((byte) 255).flip());
            sampler = OpenGlSampler.createLinearClamp(guard, registry, resources);

            SrgbPresentationMode mode = SrgbPresentationMode.fromDefaultFramebufferEncoding(draw.defaultFramebufferColorEncoding());

            vertex = OpenGlShader.compile(OpenGlShader.Stage.VERTEX, "visual-demo.vert", VERTEX_SHADER, guard, registry, resources);
            fragment = OpenGlShader.compile(OpenGlShader.Stage.FRAGMENT, "visual-demo.frag", mode.fragmentSource(FRAGMENT_SHADER), guard, registry, resources);
            program = OpenGlProgram.link("visual-demo-program", vertex, fragment, guard, registry, resources);

            MaterialTextureBinding binding = new MaterialTextureBinding(0, texture.handle(), sampler.handle());
            RenderMaterialDescriptor opaque = new RenderMaterialDescriptor(MaterialShaderVariant.TEXTURED_REFERENCE, List.of(binding),
                new MaterialScalars(0.10f, 0.62f, 1.0f, 1.0f), MaterialBlendMode.OPAQUE, MaterialDepthMode.DISABLED, MaterialCullMode.NONE);
            RenderMaterialDescriptor transparent = new RenderMaterialDescriptor(MaterialShaderVariant.TEXTURED_REFERENCE, List.of(binding),
                new MaterialScalars(0.10f, 0.62f, 1.0f, 0.32f), MaterialBlendMode.ALPHA_BLEND, MaterialDepthMode.DISABLED, MaterialCullMode.NONE);

            return new MaterialComparisonOverlay(draw, vao, vertices, indices, texture, sampler, vertex, fragment, program, opaque, transparent, mode);
        } catch (RuntimeException | Error failure) {
            closeSuppressing(failure, program);
            closeSuppressing(failure, fragment);
            closeSuppressing(failure, vertex);
            closeSuppressing(failure, sampler);
            closeSuppressing(failure, texture);
            closeSuppressing(failure, indices);
            closeSuppressing(failure, vertices);
            closeSuppressing(failure, vao);
            throw failure;
        }
    }

    void render(int framebufferWidth, int framebufferHeight) {
        if (closed) {
            throw new IllegalStateException("Material comparison overlay is closed");
        }

        draw.setViewport(0, 0, framebufferWidth, framebufferHeight);
        draw.setFramebufferSrgbEnabled(presentationMode.framebufferSrgbEnabled());
        try {
            drawPanel(opaque, -0.45f);
            drawPanel(transparent, 0.45f);
        } finally {
            draw.bindDefaultVertexArray();
            draw.useDefaultProgram();
            draw.bindTextureAndSampler(0, 0, 0);
            draw.setViewport(0, 0, framebufferWidth, framebufferHeight);
            draw.setFramebufferSrgbEnabled(false);
        }
    }

    private void drawPanel(RenderMaterialDescriptor material, float horizontalOffset) {
        draw.applyMaterialState(material);
        draw.bindTextureAndSampler(0, texture.handle(), sampler.handle());
        draw.setMaterialScalars(program.handle(), material.scalars());
        GL41.glProgramUniform1f(program.handle(), 1, horizontalOffset);
        draw.useProgram(program.handle());
        draw.bindVertexArray(vertexArray.handle());
        draw.drawIndexedTriangles(6);
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        ArrayList<Throwable> failures = new ArrayList<>();
        closeInto(failures, program);
        closeInto(failures, fragmentShader);
        closeInto(failures, vertexShader);
        closeInto(failures, sampler);
        closeInto(failures, texture);
        closeInto(failures, indexBuffer);
        closeInto(failures, vertexBuffer);
        closeInto(failures, vertexArray);
        if (!failures.isEmpty()) {
            Throwable first = failures.getFirst();
            for (int index = 1; index < failures.size(); index++) {
                first.addSuppressed(failures.get(index));
            }
            if (first instanceof RuntimeException runtimeFailure) {
                throw runtimeFailure;
            }
            throw (Error) first;
        }
    }

    private static ByteBuffer overlayVertices() {
        ByteBuffer data = ByteBuffer.allocateDirect(VERTEX_BYTES).order(ByteOrder.nativeOrder());
        putVertex(data, -0.35f, -0.05f, 0.0f);
        putVertex(data, 0.35f, -0.05f, 0.0f);
        putVertex(data, 0.35f, 0.48f, 0.0f);
        putVertex(data, -0.35f, 0.48f, 0.0f);
        return data.flip();
    }

    private static void putVertex(ByteBuffer data, float x, float y, float z) {
        data.putFloat(x).putFloat(y).putFloat(z);
        data.putFloat(0.0f).putFloat(0.0f).putFloat(1.0f);
        data.putFloat(0.0f).putFloat(0.0f);
    }

    private static ByteBuffer overlayIndices() {
        ByteBuffer data = ByteBuffer.allocateDirect(INDEX_BYTES).order(ByteOrder.nativeOrder());
        data.putInt(0).putInt(1).putInt(2);
        data.putInt(0).putInt(2).putInt(3);
        return data.flip();
    }

    private static void closeInto(List<Throwable> failures, AutoCloseable value) {
        if (value == null) {
            return;
        }
        try {
            value.close();
        } catch (RuntimeException | Error failure) {
            failures.add(failure);
        } catch (Exception impossible) {
            throw new AssertionError(impossible);
        }
    }

    private static void closeSuppressing(Throwable failure, AutoCloseable value) {
        if (value == null) {
            return;
        }
        try {
            value.close();
        } catch (RuntimeException | Error cleanupFailure) {
            if (cleanupFailure != failure) {
                failure.addSuppressed(cleanupFailure);
            }
        } catch (Exception impossible) {
            throw new AssertionError(impossible);
        }
    }
}
