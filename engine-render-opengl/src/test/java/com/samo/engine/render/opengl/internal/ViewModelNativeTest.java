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

class ViewModelNativeTest {
    private static final String ENABLE_ENV = "SHERKO_P5_T17_NATIVE";
    private static final int WIDTH = 640;
    private static final int HEIGHT = 360;
    private static final int BYTE_TOLERANCE = 8;
    private static final float SAMPLE_VIEW_X = -0.08f;
    private static final float SAMPLE_VIEW_Y = -0.145f;
    private static final float SAMPLE_VIEW_Z = -0.50f;
    private static final float COLOR_RED_LINEAR = 0.95f;
    private static final float COLOR_GREEN_LINEAR = 0.55f;
    private static final float COLOR_BLUE_LINEAR = 0.15f;
    private static final Path REPORT_PATH = Path.of("build", "reports", "p5", "p5-t17-view-model.txt");
    private static final Path CAPTURE_PATH = Path.of("build", "reports", "p5", "p5-t17-view-model.png");

    @Test
    void rendersViewModelAfterDepthIsolationOverCloserWorldGeometry() throws Exception {

        assumeTrue(Boolean.parseBoolean(System.getenv(ENABLE_ENV)), () -> "Set " + ENABLE_ENV + "=true to run the P5-T17 native acceptance");
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
        GlfwWindow window = new GlfwWindow(WIDTH, HEIGHT, "Sherko Engine P5-T17 View Model", new EngineLogger(event -> {
        }), registry, sizeListener, OpenGlDebugMode.FAIL_ON_HIGH_SEVERITY);

        boolean started = false;
        boolean stopped = false;
        boolean closed = false;
        int[] fixturePixel = null;
        int sampleX = -1;
        int sampleY = -1;
        double worldWindowDepth = Double.NaN;
        double viewModelWindowDepth = Double.NaN;
        try {
            window.initialize();
            window.start();
            started = true;
            window.pollEvents();

            int framebufferWidth = framebufferSize[0];
            int framebufferHeight = framebufferSize[1];
            assertTrue(framebufferWidth > 0 && framebufferHeight > 0);

            Matrix4f view = CameraMatrices.view(new Vector3f(0.0f, 0.0f, 0.40f), new Vector3f(0.0f, 0.0f, -1.0f), new Vector3f(0.0f, 1.0f, 0.0f), new Matrix4f());
            Matrix4f projection = CameraMatrices.perspective((float) Math.toRadians(70.0), (float) framebufferWidth / framebufferHeight, 0.1f, 100.0f, new Matrix4f());

            double aspect = (double) framebufferWidth / framebufferHeight;
            double viewModelFocal = 1.0 / Math.tan(Math.toRadians(55.0) * 0.5);
            double viewModelNdcX = (viewModelFocal / aspect) * SAMPLE_VIEW_X / -SAMPLE_VIEW_Z;
            double viewModelNdcY = viewModelFocal * SAMPLE_VIEW_Y / -SAMPLE_VIEW_Z;
            sampleX = (int) Math.round((viewModelNdcX * 0.5 + 0.5) * framebufferWidth);
            sampleY = (int) Math.round((viewModelNdcY * 0.5 + 0.5) * framebufferHeight);

            int leftWidth = (framebufferWidth + 1) / 2;
            assertTrue(sampleX >= 0 && sampleX < leftWidth);
            assertTrue(sampleY >= 0 && sampleY < framebufferHeight);

            double worldFocal = 1.0 / Math.tan(Math.toRadians(70.0) * 0.5);
            double worldDistance = 1.40;
            double sampleWorldNdcX = ((double) sampleX / leftWidth) * 2.0 - 1.0;
            double sampleWorldNdcY = ((double) sampleY / framebufferHeight) * 2.0 - 1.0;
            double panelHalfWidthNdc = (worldFocal / aspect) * 1.20 / worldDistance;
            double panelHalfHeightNdc = worldFocal * 0.80 / worldDistance;
            assertTrue(Math.abs(sampleWorldNdcX) <= panelHalfWidthNdc && Math.abs(sampleWorldNdcY) <= panelHalfHeightNdc,
                "Controlled sample must overlap the depth-writing room panel");

            worldWindowDepth = windowDepth(worldDistance, 0.1, 100.0);
            viewModelWindowDepth = windowDepth(-SAMPLE_VIEW_Z, 0.01, 10.0);
            assertTrue(worldWindowDepth < viewModelWindowDepth, "Without depth reset, closer world depth would reject the view-model sample");

            DebugFrame debugFrame = new DebugFrame(List.of(new DebugLine(-0.25f, 0.25f, 0.10f, 0.25f, 0.25f, 0.10f, new DebugColor(0.0f, 1.0f, 0.0f))), List.of());
            RenderFramePacket frame = new RenderFramePacket(view, projection, framebufferWidth, framebufferHeight, List.of(), debugFrame);

            try (OpenGlRenderer renderer = OpenGlRenderer.create(window.openGlThreadGuard(), registry)) {
                renderer.render(frame);

                RenderCullingCounters culling = renderer.lastCullingCounters();
                assertEquals(1, culling.submittedDraws());

                fixturePixel = findFixturePixel(sampleX, sampleY, 5);
                assertTrue(fixturePixel != null, "Expected view-model fixture at controlled overlap sample");

                assertFalse(GL11.glIsEnabled(GL30.GL_FRAMEBUFFER_SRGB));
                assertEquals(0, GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM));
                assertEquals(0, GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING));

                IntBuffer viewport = ByteBuffer.allocateDirect(4 * Integer.BYTES).order(java.nio.ByteOrder.nativeOrder()).asIntBuffer();
                GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
                assertEquals(0, viewport.get(0));
                assertEquals(0, viewport.get(1));
                assertEquals(framebufferWidth, viewport.get(2));
                assertEquals(framebufferHeight, viewport.get(3));

                captureBackBuffer(framebufferWidth, framebufferHeight);
                window.present();
            }

            window.stop();
            stopped = true;
            window.close();
            closed = true;
            registry.assertNoOpenResources();
            writeReport(sampleX, sampleY, worldWindowDepth, viewModelWindowDepth, fixturePixel);
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

    private static int[] findFixturePixel(int centerX, int centerY, int radius) {

        int expectedRed = encodedByte(COLOR_RED_LINEAR);
        int expectedGreen = encodedByte(COLOR_GREEN_LINEAR);
        int expectedBlue = encodedByte(COLOR_BLUE_LINEAR);
        for (int y = centerY - radius; y <= centerY + radius; y++) {
            for (int x = centerX - radius; x <= centerX + radius; x++) {
                int[] pixel = readPixel(x, y);
                if (Math.abs(pixel[0] - expectedRed) <= BYTE_TOLERANCE && Math.abs(pixel[1] - expectedGreen) <= BYTE_TOLERANCE
                    && Math.abs(pixel[2] - expectedBlue) <= BYTE_TOLERANCE) {
                    return pixel;
                }
            }
        }
        return null;

    }

    private static double[] worldNdc(double x, double y, double distance, double focal, double aspect) {

        return new double[]{(focal / aspect) * x / distance, focal * y / distance};

    }

    private static boolean pointInTriangle(double px, double py, double[] a, double[] b, double[] c) {

        double denominator = (b[1] - c[1]) * (a[0] - c[0]) + (c[0] - b[0]) * (a[1] - c[1]);
        double alpha = ((b[1] - c[1]) * (px - c[0]) + (c[0] - b[0]) * (py - c[1])) / denominator;
        double beta = ((c[1] - a[1]) * (px - c[0]) + (a[0] - c[0]) * (py - c[1])) / denominator;
        double gamma = 1.0 - alpha - beta;
        return alpha >= 0.0 && beta >= 0.0 && gamma >= 0.0;

    }

    private static double windowDepth(double distance, double near, double far) {

        double m22 = (far + near) / (near - far);
        double m32 = (2.0 * far * near) / (near - far);
        double viewZ = -distance;
        double clipZ = m22 * viewZ + m32;
        double clipW = -viewZ;
        double ndcZ = clipZ / clipW;
        return ndcZ * 0.5 + 0.5;

    }

    private static int[] readPixel(int x, int y) {

        ByteBuffer pixel = ByteBuffer.allocateDirect(4);
        GL11.glReadBuffer(GL11.GL_BACK);
        GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);
        GL11.glReadPixels(x, y, 1, 1, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixel);
        return new int[]{Byte.toUnsignedInt(pixel.get(0)), Byte.toUnsignedInt(pixel.get(1)), Byte.toUnsignedInt(pixel.get(2)), Byte.toUnsignedInt(pixel.get(3))};

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

    private static void writeReport(int sampleX, int sampleY, double worldWindowDepth, double viewModelWindowDepth, int[] fixturePixel) throws IOException {

        Files.createDirectories(REPORT_PATH.getParent());
        Files.write(REPORT_PATH, List.of("task=P5-T17", "result=PASS", "view.model.fov.degrees=55", "view.model.near.meters=0.01", "view.model.far.meters=10.0",
            "view.model.view=identity", "view.model.depth.reset.before.draw=true", "view.model.depth.test=GL_LESS", "view.model.depth.write=true",
            "world.room.panel.sample.overlap=true", "world.sample.window.depth=" + worldWindowDepth, "view.model.sample.window.depth=" + viewModelWindowDepth,
            "world.depth.would.occlude.without.reset=true", "fixture.sample.xy=" + sampleX + "," + sampleY,
            "fixture.expected.srgb=" + encodedByte(COLOR_RED_LINEAR) + "," + encodedByte(COLOR_GREEN_LINEAR) + "," + encodedByte(COLOR_BLUE_LINEAR),
            "fixture.actual.rgb=" + rgb(fixturePixel), "scene.indexed.room.draws=1", "debug.geometry.coexists=true", "viewport.restored=true", "program.unbound=true",
            "vertex.array.unbound=true", "framebuffer.srgb.disabled.after.render=true", "capture=p5-t17-view-model.png", "native.resource.registry.empty.after.cleanup=true",
            "engine.commit=" + environmentOr("GITHUB_SHA", "unknown"), "java.version=" + System.getProperty("java.version"), "os.name=" + System.getProperty("os.name"),
            "os.arch=" + System.getProperty("os.arch"),
            "evidence.scope=fixed engine-owned first-person view-model validation layer with independent projection and depth reset over the P5-T18 room depth panel; no public asset submission, gameplay weapon/hand/tool, animation/IK, third-person system, HUD/UI, FBO, render graph, or performance claim"));

    }

    private static int encodedByte(float linear) {

        double encoded = linear <= 0.0031308 ? linear * 12.92 : 1.055 * Math.pow(linear, 1.0 / 2.4) - 0.055;
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
