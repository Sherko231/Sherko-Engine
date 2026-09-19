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
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import javax.imageio.ImageIO;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

class DirectionalLightNativeTest {
    private static final String ENABLE_ENV = "SHERKO_P5_T13_NATIVE";
    private static final int WIDTH = 640;
    private static final int HEIGHT = 360;
    private static final float EXPECTED_DIFFUSE = 0.8f * (float) (1.0 / Math.sqrt(2.0));
    private static final int EXPECTED_BASELINE_SRGB_BYTE = 98;
    private static final int BYTE_TOLERANCE = 8;
    private static final Path REPORT_PATH =
            Path.of("build", "reports", "p5", "p5-t13-directional-light.txt");
    private static final Path CAPTURE_PATH =
            Path.of("build", "reports", "p5", "p5-t13-directional-light.png");

    @Test
    void shadesControlledSceneWithOneUnshadowedDirectionalLight() throws Exception {
        assumeTrue(Boolean.parseBoolean(System.getenv(ENABLE_ENV)),
                () -> "Set " + ENABLE_ENV + "=true to run the P5-T13 native acceptance");
        assertTrue(System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("windows"),
                "P5-T13 native acceptance targets Windows x64");

        NativeResourceRegistry registry = new NativeResourceRegistry();
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
                "Sherko Engine P5-T13 Directional Light Acceptance",
                new EngineLogger(event -> { }),
                registry,
                sizeListener,
                OpenGlDebugMode.FAIL_ON_HIGH_SEVERITY);

        boolean started = false;
        boolean stopped = false;
        boolean closed = false;
        int[] baseline = null;
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

            try (OpenGlRenderer renderer =
                    OpenGlRenderer.create(window.openGlThreadGuard(), registry)) {
                renderer.render(view, projection, framebufferWidth, framebufferHeight);

                assertEquals(
                        1,
                        renderer.lastCullingCounters().submittedDraws(),
                        "P5-T13 must preserve the single full-frame world indexed submission");

                baseline = readPixel(framebufferWidth / 2, framebufferHeight / 2);

                for (int channel = 0; channel < 3; channel++) {
                    assertTrue(
                            Math.abs(baseline[channel] - EXPECTED_BASELINE_SRGB_BYTE)
                                    <= BYTE_TOLERANCE,
                            "Directional light baseline mismatch: rgb="
                                    + rgb(baseline)
                                    + " expected="
                                    + EXPECTED_BASELINE_SRGB_BYTE
                                    + "±"
                                    + BYTE_TOLERANCE);
                }
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
            writeReport(baseline);
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

    private static void writeReport(int[] baseline) throws IOException {
        Files.createDirectories(REPORT_PATH.getParent());
        Files.write(REPORT_PATH, List.of(
                "task=P5-T13",
                "result=PASS",
                "light.type=directional-unshadowed",
                "light.direction.semantic=world-space-ray-travel-direction",
                "light.direction.normalized=0.0,-0.70710677,-0.70710677",
                "light.color.linear=1.0,1.0,1.0",
                "light.intensity=0.8",
                "surface.normal=0.0,0.0,1.0",
                "expected.diffuse.factor=" + EXPECTED_DIFFUSE,
                "expected.baseline.srgb.byte=" + EXPECTED_BASELINE_SRGB_BYTE,
                "baseline.rgb=" + rgb(baseline),
                "world.submitted.draws=1",
                "viewport.restored=true",
                "program.unbound=true",
                "vertex.array.unbound=true",
                "framebuffer.srgb.disabled.after.render=true",
                "capture=p5-t13-directional-light.png",
                "native.resource.registry.empty.after.cleanup=true",
                "engine.commit=" + environmentOr("GITHUB_SHA", "unknown"),
                "java.version=" + System.getProperty("java.version"),
                "os.name=" + System.getProperty("os.name"),
                "os.arch=" + System.getProperty("os.arch"),
                "evidence.scope=one fixed renderer-owned unshadowed directional light shades the current two-material reference scene in linear space before the existing P5-T08 presentation encode; no shadows, local lights, HDR, PBR, public light API, world ownership, asset pipeline, or performance claim"));
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
