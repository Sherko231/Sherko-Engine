package com.samo.engine.render.opengl.internal;

import com.samo.engine.core.api.CameraMatrices;
import com.samo.engine.core.api.DebugColor;
import com.samo.engine.core.api.DebugFrame;
import com.samo.engine.core.api.DebugLine;
import com.samo.engine.core.api.DebugPrimitive;
import com.samo.engine.core.api.DebugRay;
import com.samo.engine.core.api.EngineClock;
import com.samo.engine.core.api.EngineLogger;
import com.samo.engine.core.api.NativeResourceRegistry;
import com.samo.engine.core.api.Ray3f;
import com.samo.engine.platform.api.GlfwWindow;
import com.samo.engine.platform.api.InputKey;
import com.samo.engine.platform.api.InputSnapshot;
import com.samo.engine.platform.api.OpenGlDebugMode;
import com.samo.engine.platform.api.OpenGlThreadGuard;
import com.samo.engine.platform.api.WindowSizeListener;
import com.samo.engine.render.api.OpenGlRenderer;
import com.samo.engine.render.api.RenderFramePacket;
import com.samo.engine.render.api.RenderLocalLight;
import com.samo.engine.render.api.RenderPointLight;
import com.samo.engine.render.api.RenderSpotLight;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL41;

/**
 * Standalone owner-facing visual demo for already-implemented Phase 5 renderer behavior.
 *
 * <p>This is intentionally not a production/public renderer API. The normal game sandbox and
 * production OpenGlRenderer composition remain unchanged.
 */
public final class RendererVisualDemo {
    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;
    private static final DebugColor POINT_DEBUG = new DebugColor(1.0f, 0.45f, 0.10f);
    private static final DebugColor SPOT_DEBUG = new DebugColor(0.10f, 0.75f, 1.0f);

    private RendererVisualDemo() {
    }

    public static void main(String[] args) {
        NativeResourceRegistry registry = new NativeResourceRegistry();
        int[] framebuffer = {WIDTH, HEIGHT};
        WindowSizeListener sizes = new WindowSizeListener() {
            @Override
            public void onLogicalWindowSizeChanged(int width, int height) {
            }

            @Override
            public void onFramebufferSizeChanged(int width, int height) {
                framebuffer[0] = width;
                framebuffer[1] = height;
            }
        };

        EngineLogger logger = new EngineLogger(event -> System.out.printf(
                "[%s] [%s] %s%n",
                event.level(),
                event.context().subsystem(),
                event.message()));

        GlfwWindow window = new GlfwWindow(
                WIDTH,
                HEIGHT,
                "Sherko Renderer Visual Demo",
                logger,
                registry,
                sizes,
                OpenGlDebugMode.FAIL_ON_HIGH_SEVERITY);

        boolean started = false;
        boolean stopped = false;
        boolean closed = false;
        try {
            window.initialize();
            window.start();
            started = true;
            window.pollEvents();

            System.out.println("Renderer visual demo");
            System.out.println("  LEFT panel  = OPAQUE");
            System.out.println("  RIGHT panel = TRANSPARENT (alpha blended)");
            System.out.println("  Orange cross = moving POINT light");
            System.out.println("  Cyan cross + ray = moving SPOT light");
            System.out.println("  ESC or Ctrl+Q = exit");

            try (OpenGlRenderer renderer =
                            OpenGlRenderer.create(window.openGlThreadGuard(), registry);
                    MaterialComparisonOverlay overlay =
                            MaterialComparisonOverlay.create(window.openGlThreadGuard(), registry)) {
                runLoop(window, renderer, overlay, framebuffer);
            }

            window.stop();
            stopped = true;
            window.close();
            closed = true;
            registry.assertNoOpenResources();
        } finally {
            if (!closed) {
                if (started && !stopped) {
                    try {
                        window.stop();
                    } catch (RuntimeException | Error ignored) {
                    }
                }
                try {
                    window.close();
                } catch (RuntimeException | Error ignored) {
                }
            }
        }

        registry.assertNoOpenResources();
    }

    private static void runLoop(
            GlfwWindow window,
            OpenGlRenderer renderer,
            MaterialComparisonOverlay overlay,
            int[] framebuffer) {
        Matrix4f view = CameraMatrices.view(
                new Vector3f(0.0f, 0.0f, 2.0f),
                new Vector3f(0.0f, 0.0f, -1.0f),
                new Vector3f(0.0f, 1.0f, 0.0f),
                new Matrix4f());
        Matrix4f projection = new Matrix4f();

        EngineClock clock = new EngineClock();
        clock.sampleElapsedNanos();
        long elapsedNanos = 0L;
        long inputFrame = 0L;
        boolean exit = false;

        while (!exit) {
            elapsedNanos = saturatingAdd(elapsedNanos, clock.sampleElapsedNanos());
            double seconds = elapsedNanos / 1_000_000_000.0;

            window.pollEvents();
            InputSnapshot input = window.captureInputSnapshot(inputFrame++);
            exit = input.keyPressed(InputKey.ESCAPE)
                    || (input.keyPressed(InputKey.Q)
                            && (input.keyHeld(InputKey.LEFT_CONTROL)
                                    || input.keyHeld(InputKey.RIGHT_CONTROL)));

            int width = framebuffer[0];
            int height = framebuffer[1];
            if (!exit && width > 0 && height > 0) {
                CameraMatrices.perspective(
                        (float) Math.toRadians(70.0),
                        (float) width / height,
                        0.1f,
                        100.0f,
                        projection);

                DemoLighting lighting = lightingAt(seconds);
                RenderFramePacket frame = new RenderFramePacket(
                        view,
                        projection,
                        width,
                        height,
                        lighting.lights(),
                        new DebugFrame(lighting.debugPrimitives(), List.of()));

                renderer.render(frame);
                overlay.render(width, height);
                window.present();
            }
        }
    }

    static DemoLighting lightingAt(double seconds) {
        float pointX = (float) (Math.sin(seconds * 0.85) * 1.35);
        float pointY = 0.45f + (float) (Math.cos(seconds * 1.10) * 0.30);
        float pointZ = -0.55f + (float) (Math.cos(seconds * 0.85) * 0.35);

        float spotX = (float) (Math.cos(seconds * 0.60) * 1.25);
        float spotY = 0.95f + (float) (Math.sin(seconds * 0.75) * 0.20);
        float spotZ = -0.25f + (float) (Math.sin(seconds * 0.60) * 0.30);

        Vector3f spotPosition = new Vector3f(spotX, spotY, spotZ);
        Vector3f spotDirection =
                new Vector3f(0.0f, 0.0f, -2.25f).sub(spotPosition).normalize();

        RenderPointLight point = new RenderPointLight(
                pointX,
                pointY,
                pointZ,
                1.0f,
                0.28f,
                0.06f,
                1.0f,
                4.5f);
        RenderSpotLight spot = new RenderSpotLight(
                spotX,
                spotY,
                spotZ,
                spotDirection.x,
                spotDirection.y,
                spotDirection.z,
                0.10f,
                0.60f,
                1.0f,
                1.0f,
                5.0f,
                (float) Math.toRadians(12.0),
                (float) Math.toRadians(28.0));

        ArrayList<DebugPrimitive> debug = new ArrayList<>();
        addCross(debug, new Vector3f(pointX, pointY, pointZ), 0.16f, POINT_DEBUG);
        addCross(debug, spotPosition, 0.16f, SPOT_DEBUG);
        debug.add(new DebugRay(
                new Ray3f(spotPosition, spotDirection),
                2.3f,
                SPOT_DEBUG));

        return new DemoLighting(List.of(point, spot), List.copyOf(debug));
    }

    private static void addCross(
            List<DebugPrimitive> destination,
            Vector3f center,
            float halfExtent,
            DebugColor color) {
        destination.add(new DebugLine(
                center.x - halfExtent, center.y, center.z,
                center.x + halfExtent, center.y, center.z,
                color));
        destination.add(new DebugLine(
                center.x, center.y - halfExtent, center.z,
                center.x, center.y + halfExtent, center.z,
                color));
        destination.add(new DebugLine(
                center.x, center.y, center.z - halfExtent,
                center.x, center.y, center.z + halfExtent,
                color));
    }

    private static long saturatingAdd(long left, long right) {
        if (right > 0L && left > Long.MAX_VALUE - right) {
            return Long.MAX_VALUE;
        }
        return left + right;
    }

    record DemoLighting(
            List<RenderLocalLight> lights,
            List<DebugPrimitive> debugPrimitives) {
    }

    private static final class MaterialComparisonOverlay implements AutoCloseable {
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
        private final PresentationMode presentationMode;
        private boolean closed;

        private MaterialComparisonOverlay(
                OpenGlDrawBackend draw,
                OpenGlVertexArray vertexArray,
                OpenGlBuffer vertexBuffer,
                OpenGlBuffer indexBuffer,
                OpenGlTexture texture,
                OpenGlSampler sampler,
                OpenGlShader vertexShader,
                OpenGlShader fragmentShader,
                OpenGlProgram program,
                RenderMaterialDescriptor opaque,
                RenderMaterialDescriptor transparent,
                PresentationMode presentationMode) {
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

        static MaterialComparisonOverlay create(
                OpenGlThreadGuard guard,
                NativeResourceRegistry registry) {
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

                texture = OpenGlTexture.createRgba8(
                        guard,
                        registry,
                        resources,
                        TextureColorEncoding.LINEAR_DATA,
                        1,
                        1,
                        ByteBuffer.allocateDirect(4)
                                .put((byte) 255)
                                .put((byte) 255)
                                .put((byte) 255)
                                .put((byte) 255)
                                .flip());
                sampler = OpenGlSampler.createLinearClamp(guard, registry, resources);

                PresentationMode mode = PresentationMode.fromDefaultFramebufferEncoding(
                        draw.defaultFramebufferColorEncoding());

                vertex = OpenGlShader.compile(
                        OpenGlShader.Stage.VERTEX,
                        "visual-demo.vert",
                        VERTEX_SHADER,
                        guard,
                        registry,
                        resources);
                fragment = OpenGlShader.compile(
                        OpenGlShader.Stage.FRAGMENT,
                        "visual-demo.frag",
                        mode.fragmentSource(FRAGMENT_SHADER),
                        guard,
                        registry,
                        resources);
                program = OpenGlProgram.link(
                        "visual-demo-program",
                        vertex,
                        fragment,
                        guard,
                        registry,
                        resources);

                MaterialTextureBinding binding =
                        new MaterialTextureBinding(0, texture.handle(), sampler.handle());
                RenderMaterialDescriptor opaque = new RenderMaterialDescriptor(
                        MaterialShaderVariant.TEXTURED_REFERENCE,
                        List.of(binding),
                        new MaterialScalars(0.10f, 0.62f, 1.0f, 1.0f),
                        MaterialBlendMode.OPAQUE,
                        MaterialDepthMode.DISABLED,
                        MaterialCullMode.NONE);
                RenderMaterialDescriptor transparent = new RenderMaterialDescriptor(
                        MaterialShaderVariant.TEXTURED_REFERENCE,
                        List.of(binding),
                        new MaterialScalars(0.10f, 0.62f, 1.0f, 0.32f),
                        MaterialBlendMode.ALPHA_BLEND,
                        MaterialDepthMode.DISABLED,
                        MaterialCullMode.NONE);

                return new MaterialComparisonOverlay(
                        draw,
                        vao,
                        vertices,
                        indices,
                        texture,
                        sampler,
                        vertex,
                        fragment,
                        program,
                        opaque,
                        transparent,
                        mode);
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
}
