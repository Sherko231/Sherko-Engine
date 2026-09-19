package com.samo.engine.render.opengl.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.samo.engine.core.api.CameraMatrices;
import com.samo.engine.core.api.DebugColor;
import com.samo.engine.core.api.DebugFrame;
import com.samo.engine.core.api.DebugLine;
import com.samo.engine.core.api.EngineLogger;
import com.samo.engine.core.api.NativeResourceRegistry;
import com.samo.engine.platform.api.GlfwWindow;
import com.samo.engine.platform.api.OpenGlDebugMode;
import com.samo.engine.platform.api.WindowSizeListener;
import com.samo.engine.render.api.OpenGlRenderer;
import com.samo.engine.render.api.RenderCullingCounters;
import com.samo.engine.render.api.RenderFramePacket;
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

class Phase5ExitNativeTest {
    private static final String ENABLE_ENV = "SHERKO_P5_T18_NATIVE";
    private static final int WIDTH = 640;
    private static final int HEIGHT = 360;
    private static final int BYTE_TOLERANCE = 8;
    private static final int EXPECTED_NEAR_PANEL_BYTE = litSrgbByte(
            128,
            0.8f * (float) (1.0 / Math.sqrt(2.0)));
    private static final Path REPORT_PATH =
            Path.of("build", "reports", "p5", "p5-t18-exit.txt");
    private static final Path CAPTURE_A_PATH =
            Path.of("build", "reports", "p5", "p5-t18-room-camera-a.png");
    private static final Path CAPTURE_B_PATH =
            Path.of("build", "reports", "p5", "p5-t18-room-camera-b.png");

    @Test
    void provesIntegratedTexturedRoomDepthCameraLightingDebugAndViewModel() throws Exception {
        assumeTrue(Boolean.parseBoolean(System.getenv(ENABLE_ENV)),
                () -> "Set " + ENABLE_ENV + "=true to run the P5-T18 native acceptance");
        assertTrue(System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("windows"));

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
                "Sherko Engine P5 Exit",
                new EngineLogger(event -> { }),
                registry,
                sizeListener,
                OpenGlDebugMode.FAIL_ON_HIGH_SEVERITY);

        boolean started = false;
        boolean stopped = false;
        boolean closed = false;
        int[] nearPanel = null;
        int[] floor = null;
        int[] ceiling = null;
        int[] shiftedCenter = null;
        int[] debugGreen = null;
        int[] viewModelOrange = null;
        try {
            window.initialize();
            window.start();
            started = true;
            window.pollEvents();

            int framebufferWidth = framebufferSize[0];
            int framebufferHeight = framebufferSize[1];
            assertTrue(framebufferWidth > 0 && framebufferHeight > 0);

            Matrix4f projection = CameraMatrices.perspective(
                    (float) Math.toRadians(70.0),
                    (float) framebufferWidth / framebufferHeight,
                    0.1f,
                    100.0f,
                    new Matrix4f());
            Matrix4f viewA = CameraMatrices.view(
                    new Vector3f(0.0f, 0.0f, 2.0f),
                    new Vector3f(0.0f, 0.0f, -1.0f),
                    new Vector3f(0.0f, 1.0f, 0.0f),
                    new Matrix4f());
            Matrix4f viewB = CameraMatrices.view(
                    new Vector3f(1.5f, 0.0f, 2.0f),
                    new Vector3f(0.0f, 0.0f, -1.0f),
                    new Vector3f(0.0f, 1.0f, 0.0f),
                    new Matrix4f());

            DebugFrame debugFrame = new DebugFrame(
                    List.of(new DebugLine(
                            -0.40f, 1.0f, 0.0f,
                            0.40f, 1.0f, 0.0f,
                            new DebugColor(0.0f, 1.0f, 0.0f))),
                    List.of());

            try (OpenGlRenderer renderer =
                    OpenGlRenderer.create(window.openGlThreadGuard(), registry)) {
                RenderFramePacket frameA = new RenderFramePacket(
                        viewA,
                        projection,
                        framebufferWidth,
                        framebufferHeight,
                        List.of(),
                        debugFrame);
                renderer.render(frameA);

                RenderCullingCounters cullingA = renderer.lastCullingCounters();
                assertEquals(1, cullingA.submittedDraws());

                int centerX = framebufferWidth / 2;
                int centerY = framebufferHeight / 2;
                nearPanel = readPixel(centerX, centerY);
                floor = readPixel(centerX, framebufferHeight / 4);
                ceiling = readPixel(centerX, (framebufferHeight * 3) / 4);

                assertNeutralNearPanel(nearPanel);
                assertTrue(maxChannel(floor) > 25, "Expected lit textured floor: " + rgb(floor));
                assertTrue(maxChannel(ceiling) < 25, "Expected unlit ceiling surface: " + rgb(ceiling));
                assertTrue(colorDistance(floor, nearPanel) > 10,
                        "Mapped texture must vary across visible room surfaces");

                debugGreen = findGreenDominantPixel(
                        framebufferWidth,
                        framebufferHeight,
                        framebufferWidth / 2,
                        (framebufferHeight * 6) / 7,
                        8);
                assertTrue(debugGreen != null, "Expected P5-T16 debug geometry to coexist");

                viewModelOrange = findOrangePixel(
                        framebufferWidth,
                        framebufferHeight,
                        framebufferWidth / 2,
                        framebufferHeight / 4,
                        framebufferWidth / 3,
                        framebufferHeight / 3);
                assertTrue(viewModelOrange != null, "Expected P5-T17 view-model fixture to coexist");

                captureBackBuffer(framebufferWidth, framebufferHeight, CAPTURE_A_PATH);

                RenderFramePacket frameB = new RenderFramePacket(
                        viewB,
                        projection,
                        framebufferWidth,
                        framebufferHeight,
                        List.of(),
                        debugFrame);
                renderer.render(frameB);
                shiftedCenter = readPixel(centerX, centerY);

                assertTrue(
                        colorDistance(nearPanel, shiftedCenter) > 12,
                        "Known camera translation must change the projected room sample: before="
                                + rgb(nearPanel) + " after=" + rgb(shiftedCenter));
                assertTrue(
                        channelSpread(shiftedCenter) > 10,
                        "Shifted center must expose non-uniform mapped back-wall color: "
                                + rgb(shiftedCenter));

                assertFalse(GL11.glIsEnabled(GL30.GL_FRAMEBUFFER_SRGB));
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

                captureBackBuffer(framebufferWidth, framebufferHeight, CAPTURE_B_PATH);
                window.present();
            }

            window.stop();
            stopped = true;
            window.close();
            closed = true;
            registry.assertNoOpenResources();
            writeReport(nearPanel, floor, ceiling, shiftedCenter, debugGreen, viewModelOrange);
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

    private static void assertNeutralNearPanel(int[] pixel) {
        for (int channel = 0; channel < 3; channel++) {
            assertTrue(
                    Math.abs(pixel[channel] - EXPECTED_NEAR_PANEL_BYTE) <= BYTE_TOLERANCE,
                    "Depth-writing near panel expected " + EXPECTED_NEAR_PANEL_BYTE
                            + "±" + BYTE_TOLERANCE + " but was " + rgb(pixel));
        }
        assertTrue(channelSpread(pixel) <= 2, "Near panel reference sample must remain neutral");
    }

    private static int[] findGreenDominantPixel(
            int width,
            int height,
            int centerX,
            int centerY,
            int radius) {
        for (int y = Math.max(0, centerY - radius); y <= Math.min(height - 1, centerY + radius); y++) {
            for (int x = Math.max(0, centerX - radius); x <= Math.min(width - 1, centerX + radius); x++) {
                int[] pixel = readPixel(x, y);
                if (pixel[1] >= 180
                        && pixel[1] - pixel[0] >= 80
                        && pixel[1] - pixel[2] >= 80) {
                    return pixel;
                }
            }
        }
        return null;
    }

    private static int[] findOrangePixel(
            int width,
            int height,
            int centerX,
            int centerY,
            int halfWidth,
            int halfHeight) {
        int expectedRed = encodedByte(0.95f);
        int expectedGreen = encodedByte(0.55f);
        int expectedBlue = encodedByte(0.15f);
        int minX = Math.max(0, centerX - halfWidth);
        int maxX = Math.min(width - 1, centerX + halfWidth);
        int minY = Math.max(0, centerY - halfHeight);
        int maxY = Math.min(height - 1, centerY + halfHeight);
        for (int y = minY; y <= maxY; y += 2) {
            for (int x = minX; x <= maxX; x += 2) {
                int[] pixel = readPixel(x, y);
                if (Math.abs(pixel[0] - expectedRed) <= BYTE_TOLERANCE
                        && Math.abs(pixel[1] - expectedGreen) <= BYTE_TOLERANCE
                        && Math.abs(pixel[2] - expectedBlue) <= BYTE_TOLERANCE) {
                    return pixel;
                }
            }
        }
        return null;
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

    private static void captureBackBuffer(int width, int height, Path path) throws IOException {
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
        Files.createDirectories(path.getParent());
        if (!ImageIO.write(image, "png", path.toFile())) {
            throw new IOException("PNG writer unavailable");
        }
    }

    private static void writeReport(
            int[] nearPanel,
            int[] floor,
            int[] ceiling,
            int[] shiftedCenter,
            int[] debugGreen,
            int[] viewModelOrange) throws IOException {
        Files.createDirectories(REPORT_PATH.getParent());
        Files.write(REPORT_PATH, List.of(
                "task=P5-T18",
                "result=PASS",
                "room.fixture=renderer-owned-fixed-validation-room",
                "room.texture=4x4-non-uniform-srgb-mapped-uv",
                "room.surfaces=back-wall,floor,ceiling,left-wall,right-wall,near-depth-panel",
                "room.near.panel.expected.srgb.byte=" + EXPECTED_NEAR_PANEL_BYTE,
                "room.near.panel.actual.rgb=" + rgb(nearPanel),
                "room.floor.rgb=" + rgb(floor),
                "room.ceiling.rgb=" + rgb(ceiling),
                "room.depth.occlusion.near-panel-over-back-wall=true",
                "camera.pose.a.position=0.0,0.0,2.0",
                "camera.pose.b.position=1.5,0.0,2.0",
                "camera.shifted.center.rgb=" + rgb(shiftedCenter),
                "camera.projected.sample.changed=true",
                "directional.light.contribution=verified-by-neutral-panel-reference",
                "debug.geometry.coexists.rgb=" + rgb(debugGreen),
                "view.model.coexists.rgb=" + rgb(viewModelOrange),
                "world.submitted.draws=1",
                "viewport.restored=true",
                "program.unbound=true",
                "vertex.array.unbound=true",
                "framebuffer.srgb.disabled.after.render=true",
                "capture.pose.a=p5-t18-room-camera-a.png",
                "capture.pose.b=p5-t18-room-camera-b.png",
                "native.resource.registry.empty.after.cleanup=true",
                "engine.commit=" + environmentOr("GITHUB_SHA", "unknown"),
                "java.version=" + System.getProperty("java.version"),
                "os.name=" + System.getProperty("os.name"),
                "os.arch=" + System.getProperty("os.arch"),
                "evidence.scope=Phase 5 exit integration only: fixed internal textured room, depth occlusion, known camera poses, accepted directional lighting, D-063 presentation, P5-T16 debug geometry and P5-T17 view-model coexistence; no public asset/resource API, world/ECS ownership, gameplay, physics, HUD, render graph, shadows, HDR, tonemapping, fog, or performance claim"));
    }

    private static int maxChannel(int[] rgb) {
        return Math.max(rgb[0], Math.max(rgb[1], rgb[2]));
    }

    private static int channelSpread(int[] rgb) {
        int max = Math.max(rgb[0], Math.max(rgb[1], rgb[2]));
        int min = Math.min(rgb[0], Math.min(rgb[1], rgb[2]));
        return max - min;
    }

    private static int colorDistance(int[] left, int[] right) {
        return Math.max(
                Math.abs(left[0] - right[0]),
                Math.max(
                        Math.abs(left[1] - right[1]),
                        Math.abs(left[2] - right[2])));
    }

    private static int litSrgbByte(int srgbByte, double diffuseFactor) {
        double encoded = srgbByte / 255.0;
        double linear = encoded <= 0.04045
                ? encoded / 12.92
                : Math.pow((encoded + 0.055) / 1.055, 2.4);
        return encodedByte((float) (linear * diffuseFactor));
    }

    private static int encodedByte(float linear) {
        double encoded = linear <= 0.0031308
                ? linear * 12.92
                : 1.055 * Math.pow(linear, 1.0 / 2.4) - 0.055;
        return (int) Math.round(encoded * 255.0);
    }

    private static String rgb(int[] pixel) {
        return pixel[0] + "," + pixel[1] + "," + pixel[2];
    }

    private static boolean attemptCleanup(Runnable cleanup) {
        try {
            cleanup.run();
            return true;
        } catch (RuntimeException | Error failure) {
            return false;
        }
    }

    private static String environmentOr(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value;
    }
}
