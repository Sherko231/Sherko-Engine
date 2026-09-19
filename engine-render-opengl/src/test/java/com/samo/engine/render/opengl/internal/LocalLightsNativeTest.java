package com.samo.engine.render.opengl.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.samo.engine.core.api.CameraMatrices;
import com.samo.engine.core.api.EngineLogger;
import com.samo.engine.core.api.NativeResourceRegistry;
import com.samo.engine.platform.api.GlfwWindow;
import com.samo.engine.platform.api.OpenGlDebugMode;
import com.samo.engine.platform.api.WindowSizeListener;
import com.samo.engine.render.api.OpenGlRenderer;
import com.samo.engine.render.api.RenderFramePacket;
import com.samo.engine.render.api.RenderPointLight;
import com.samo.engine.render.api.RenderSpotLight;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.imageio.ImageIO;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

class LocalLightsNativeTest {
    private static final String ENABLE_ENV = "SHERKO_P5_T14_NATIVE";
    private static final int WIDTH = 640;
    private static final int HEIGHT = 360;
    private static final int MAX_LOCAL_LIGHTS = 2;
    private static final int INPUT_SRGB_BYTE = 128;
    private static final float DIRECTIONAL_DIFFUSE =
            0.8f * (float) (1.0 / Math.sqrt(2.0));
    private static final float LOCAL_RANGE_METERS = 100.0f;
    private static final float LOCAL_DISTANCE_METERS = 1.0f;
    private static final float LOCAL_INTENSITY = 0.1f;
    private static final float SPOT_INNER_RADIANS = 0.2f;
    private static final float SPOT_OUTER_RADIANS = 0.5f;
    private static final float SPOT_INNER_COSINE = (float) Math.cos(SPOT_INNER_RADIANS);
    private static final float SPOT_OUTER_COSINE = (float) Math.cos(SPOT_OUTER_RADIANS);
    private static final float SPOT_ALIGNMENT =
            (SPOT_INNER_COSINE + SPOT_OUTER_COSINE) * 0.5f;
    private static final float SPOT_DIRECTION_X =
            (float) Math.sqrt(1.0f - SPOT_ALIGNMENT * SPOT_ALIGNMENT);
    private static final float SPOT_DIRECTION_Z = -SPOT_ALIGNMENT;
    private static final float SPOT_CONE_ATTENUATION = 0.5f;
    private static final float LOCAL_RANGE_ATTENUATION =
            (1.0f - LOCAL_DISTANCE_METERS / LOCAL_RANGE_METERS)
                    * (1.0f - LOCAL_DISTANCE_METERS / LOCAL_RANGE_METERS);
    private static final float EXPECTED_ILLUMINATION =
            DIRECTIONAL_DIFFUSE
                    + LOCAL_INTENSITY * LOCAL_RANGE_ATTENUATION
                    + LOCAL_INTENSITY * LOCAL_RANGE_ATTENUATION * SPOT_CONE_ATTENUATION;
    private static final int EXPECTED_BASELINE_SRGB_BYTE =
            litSrgbByte(INPUT_SRGB_BYTE, EXPECTED_ILLUMINATION);
    private static final int BYTE_TOLERANCE = 8;
    private static final Path REPORT_PATH =
            Path.of("build", "reports", "p5", "p5-t14-local-lights.txt");
    private static final Path CAPTURE_PATH =
            Path.of("build", "reports", "p5", "p5-t14-local-lights.png");

    @Test
    void rendersFirstBoundedPointAndSpotLightsAndDropsOverflowDeterministically() throws Exception {
        assumeTrue(Boolean.parseBoolean(System.getenv(ENABLE_ENV)),
                () -> "Set " + ENABLE_ENV + "=true to run the P5-T14 native acceptance");
        assertTrue(System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("windows"),
                "P5-T14 native acceptance targets Windows x64");

        NativeResourceRegistry registry = new NativeResourceRegistry();
        List<EngineLogger.Event> rendererEvents = new ArrayList<>();
        EngineLogger rendererLogger = new EngineLogger(rendererEvents::add);
        int[] framebufferSize = {WIDTH, HEIGHT};
        WindowSizeListener sizeListener = new WindowSizeListener() {
            @Override
            public void onLogicalWindowSizeChanged(int width, int height) {
            }

            @Override
            public void onFramebufferSizeChanged(int width, int height) {
                framebufferSize[0] = width;
                framebufferSize[1] = height;
            }
        };

        GlfwWindow window = new GlfwWindow(
                WIDTH,
                HEIGHT,
                "Sherko Engine P5-T14 Local Lights Acceptance",
                new EngineLogger(event -> { }),
                registry,
                sizeListener,
                OpenGlDebugMode.FAIL_ON_HIGH_SEVERITY);

        boolean started = false;
        boolean stopped = false;
        boolean closed = false;
        int[] baseline = null;
        int[] tinted = null;
        try {
            window.initialize();
            window.start();
            started = true;
            window.pollEvents();

            int framebufferWidth = framebufferSize[0];
            int framebufferHeight = framebufferSize[1];
            assertTrue(framebufferWidth > 0 && framebufferHeight > 0,
                    "Native acceptance requires a visible non-zero framebuffer");

            Matrix4f view = CameraMatrices.view(
                    new Vector3f(0.0f, 0.0f, 2.0f),
                    new Vector3f(0.0f, 0.0f, -1.0f),
                    new Vector3f(0.0f, 1.0f, 0.0f),
                    new Matrix4f());
            Matrix4f projection = CameraMatrices.perspective(
                    (float) Math.toRadians(70.0),
                    (float) framebufferWidth / framebufferHeight,
                    0.1f,
                    100.0f,
                    new Matrix4f());

            RenderPointLight point = new RenderPointLight(
                    0.0f,
                    0.0f,
                    0.0f,
                    1.0f,
                    1.0f,
                    1.0f,
                    LOCAL_INTENSITY,
                    LOCAL_RANGE_METERS);
            RenderSpotLight spot = new RenderSpotLight(
                    0.0f,
                    0.0f,
                    0.0f,
                    SPOT_DIRECTION_X,
                    0.0f,
                    SPOT_DIRECTION_Z,
                    1.0f,
                    1.0f,
                    1.0f,
                    LOCAL_INTENSITY,
                    LOCAL_RANGE_METERS,
                    SPOT_INNER_RADIANS,
                    SPOT_OUTER_RADIANS);
            RenderPointLight overflowRed = new RenderPointLight(
                    0.0f,
                    0.0f,
                    0.0f,
                    1.0f,
                    0.0f,
                    0.0f,
                    1.0f,
                    LOCAL_RANGE_METERS);
            RenderFramePacket frame = new RenderFramePacket(
                    view,
                    projection,
                    framebufferWidth,
                    framebufferHeight,
                    List.of(point, spot, overflowRed));

            try (OpenGlRenderer renderer = OpenGlRenderer.create(
                    window.openGlThreadGuard(),
                    registry,
                    rendererLogger,
                    MAX_LOCAL_LIGHTS)) {
                renderer.render(frame);

                assertEquals(
                        2,
                        renderer.lastCullingCounters().submittedDraws(),
                        "P5-T14 must preserve the two current world indexed submissions");

                assertEquals(1, rendererEvents.size());
                EngineLogger.Event warning = rendererEvents.getFirst();
                assertEquals(EngineLogger.Level.WARN, warning.level());
                assertEquals("renderer", warning.context().subsystem());
                assertEquals(
                        "Local light limit exceeded: submitted=3 accepted=2 dropped=1 configuredMax=2",
                        warning.message());

                baseline = readPixel(framebufferWidth / 4, framebufferHeight / 2);
                tinted = readPixel((framebufferWidth * 3) / 4, framebufferHeight / 2);

                for (int channel = 0; channel < 3; channel++) {
                    assertTrue(
                            Math.abs(baseline[channel] - EXPECTED_BASELINE_SRGB_BYTE)
                                    <= BYTE_TOLERANCE,
                            "Bounded local-light baseline mismatch: rgb="
                                    + rgb(baseline)
                                    + " expected="
                                    + EXPECTED_BASELINE_SRGB_BYTE
                                    + "±"
                                    + BYTE_TOLERANCE);
                }
                assertTrue(
                        Math.abs(baseline[0] - baseline[1]) <= 2
                                && Math.abs(baseline[0] - baseline[2]) <= 2,
                        "Dropped red overflow light must not tint the accepted neutral baseline: rgb="
                                + rgb(baseline));
                assertTrue(
                        tinted[0] - tinted[1] >= 15 && tinted[0] - tinted[2] >= 15,
                        "Tinted material must remain red-biased under local lighting: rgb="
                                + rgb(tinted));

                assertFalse(
                        GL11.glIsEnabled(GL30.GL_FRAMEBUFFER_SRGB),
                        "Renderer must disable GL_FRAMEBUFFER_SRGB after render");
                assertEquals(0, GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM));
                assertEquals(0, GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING));

                IntBuffer viewport = ByteBuffer.allocateDirect(4 * Integer.BYTES)
                        .order(java.nio.ByteOrder.nativeOrder())
                        .asIntBuffer();
                GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
                assertEquals(0, viewport.get(0));
                assertEquals(0, viewport.get(1));
                assertEquals(framebufferWidth, viewport.get(2));
                assertEquals(framebufferHeight, viewport.get(3));

                captureBackBuffer(framebufferWidth, framebufferHeight);
                window.pollEvents();
                window.present();
            }

            window.stop();
            stopped = true;
            window.close();
            closed = true;
            registry.assertNoOpenResources();
            writeReport(baseline, tinted);
        } finally {
            if (!closed) {
                if (started && !stopped) {
                    attemptCleanup(window::stop);
                }
                attemptCleanup(window::close);
            }
        }

        registry.assertNoOpenResources();
    }

    private static int[] readPixel(int x, int y) {
        ByteBuffer pixel = ByteBuffer.allocateDirect(4);
        GL11.glReadBuffer(GL11.GL_BACK);
        GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);
        GL11.glReadPixels(x, y, 1, 1, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixel);
        return new int[] {
            Byte.toUnsignedInt(pixel.get(0)),
            Byte.toUnsignedInt(pixel.get(1)),
            Byte.toUnsignedInt(pixel.get(2)),
            Byte.toUnsignedInt(pixel.get(3))
        };
    }

    private static void captureBackBuffer(int width, int height) throws IOException {
        ByteBuffer pixels = ByteBuffer.allocateDirect(width * height * 4);
        GL11.glReadBuffer(GL11.GL_BACK);
        GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);
        GL11.glReadPixels(0, 0, width, height, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            int sourceY = height - 1 - y;
            for (int x = 0; x < width; x++) {
                int offset = (sourceY * width + x) * 4;
                int red = Byte.toUnsignedInt(pixels.get(offset));
                int green = Byte.toUnsignedInt(pixels.get(offset + 1));
                int blue = Byte.toUnsignedInt(pixels.get(offset + 2));
                int alpha = Byte.toUnsignedInt(pixels.get(offset + 3));
                image.setRGB(x, y, (alpha << 24) | (red << 16) | (green << 8) | blue);
            }
        }

        Files.createDirectories(CAPTURE_PATH.getParent());
        if (!ImageIO.write(image, "png", CAPTURE_PATH.toFile())) {
            throw new IOException("PNG writer unavailable");
        }
    }

    private static void writeReport(int[] baseline, int[] tinted) throws IOException {
        Files.createDirectories(REPORT_PATH.getParent());
        Files.write(REPORT_PATH, List.of(
                "task=P5-T14",
                "result=PASS",
                "configured.max.local.lights=" + MAX_LOCAL_LIGHTS,
                "submitted.local.lights=3",
                "accepted.local.lights=2",
                "dropped.local.lights=1",
                "warning.count=1",
                "point.position=0.0,0.0,1.0",
                "point.range.meters=" + LOCAL_RANGE_METERS,
                "point.intensity=" + LOCAL_INTENSITY,
                "spot.position=0.0,0.0,1.0",
                "spot.direction=" + SPOT_DIRECTION_X + ",0.0," + SPOT_DIRECTION_Z,
                "spot.inner.radians=" + SPOT_INNER_RADIANS,
                "spot.outer.radians=" + SPOT_OUTER_RADIANS,
                "spot.alignment=" + SPOT_ALIGNMENT,
                "spot.cone.attenuation=" + SPOT_CONE_ATTENUATION,
                "spot.range.meters=" + LOCAL_RANGE_METERS,
                "spot.intensity=" + LOCAL_INTENSITY,
                "reference.distance.meters=" + LOCAL_DISTANCE_METERS,
                "reference.range.attenuation=" + LOCAL_RANGE_ATTENUATION,
                "reference.directional.diffuse=" + DIRECTIONAL_DIFFUSE,
                "reference.expected.illumination=" + EXPECTED_ILLUMINATION,
                "reference.expected.baseline.srgb.byte=" + EXPECTED_BASELINE_SRGB_BYTE,
                "baseline.rgb=" + rgb(baseline),
                "tinted.rgb=" + rgb(tinted),
                "world.submitted.draws=2",
                "viewport.restored=true",
                "program.unbound=true",
                "vertex.array.unbound=true",
                "framebuffer.srgb.disabled.after.render=true",
                "capture=p5-t14-local-lights.png",
                "native.resource.registry.empty.after.cleanup=true",
                "engine.commit=" + environmentOr("GITHUB_SHA", "unknown"),
                "java.version=" + System.getProperty("java.version"),
                "os.name=" + System.getProperty("os.name"),
                "os.arch=" + System.getProperty("os.arch"),
                "evidence.scope=bounded first-two local-light submission with one point and one centered spot plus deterministic dropped overflow red light; additive linear Lambert/range/cone lighting before the accepted presentation encode; no shadows, Forward+, clustered lighting, PBR, HDR, world ownership, or performance claim"));
    }

    private static int litSrgbByte(int srgbByte, double illumination) {
        double encoded = srgbByte / 255.0;
        double linear = encoded <= 0.04045
                ? encoded / 12.92
                : Math.pow((encoded + 0.055) / 1.055, 2.4);
        double litLinear = Math.min(1.0, linear * illumination);
        double output = litLinear <= 0.0031308
                ? litLinear * 12.92
                : 1.055 * Math.pow(litLinear, 1.0 / 2.4) - 0.055;
        return (int) Math.round(output * 255.0);
    }

    private static String rgb(int[] pixel) {
        return pixel[0] + "," + pixel[1] + "," + pixel[2];
    }

    private static boolean attemptCleanup(Runnable cleanup) {
        try {
            cleanup.run();
            return true;
        } catch (RuntimeException | Error cleanupFailure) {
            return false;
        }
    }

    private static String environmentOr(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value;
    }
}
