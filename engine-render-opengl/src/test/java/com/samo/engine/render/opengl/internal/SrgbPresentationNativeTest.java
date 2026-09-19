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
import org.lwjgl.opengl.GL21;
import org.lwjgl.opengl.GL30;

class SrgbPresentationNativeTest {
    private static final String ENABLE_ENV = "SHERKO_P5_T15_NATIVE";
    private static final int WIDTH = 640;
    private static final int HEIGHT = 360;
    private static final int BYTE_TOLERANCE = 5;
    private static final float CLEAR_RED_LINEAR = 0.08f;
    private static final float CLEAR_GREEN_LINEAR = 0.10f;
    private static final float CLEAR_BLUE_LINEAR = 0.14f;
    private static final int CLEAR_RED_EXPECTED = encodedByte(CLEAR_RED_LINEAR);
    private static final int CLEAR_GREEN_EXPECTED = encodedByte(CLEAR_GREEN_LINEAR);
    private static final int CLEAR_BLUE_EXPECTED = encodedByte(CLEAR_BLUE_LINEAR);
    private static final int BASELINE_EXPECTED = litSrgbByte(
            128,
            0.8f * (float) (1.0 / Math.sqrt(2.0)));
    private static final Path REPORT_PATH =
            Path.of("build", "reports", "p5", "p5-t15-srgb-presentation.txt");
    private static final Path CAPTURE_PATH =
            Path.of("build", "reports", "p5", "p5-t15-srgb-presentation.png");

    @Test
    void presentsKnownLinearClearAndLitReferenceWithExactlyOneSrgbEncode() throws Exception {
        assumeTrue(Boolean.parseBoolean(System.getenv(ENABLE_ENV)),
                () -> "Set " + ENABLE_ENV + "=true to run the P5-T15 native acceptance");
        assertTrue(System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("windows"),
                "P5-T15 native acceptance targets Windows x64");

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
                "Sherko Engine P5-T15 sRGB Presentation",
                new EngineLogger(event -> { }),
                registry,
                sizeListener,
                OpenGlDebugMode.FAIL_ON_HIGH_SEVERITY);

        boolean started = false;
        boolean stopped = false;
        boolean closed = false;
        int framebufferEncoding = -1;
        int[] clearPixel = null;
        int[] baselinePixel = null;
        try {
            window.initialize();
            window.start();
            started = true;
            window.pollEvents();

            int framebufferWidth = framebufferSize[0];
            int framebufferHeight = framebufferSize[1];
            assertTrue(framebufferWidth > 0 && framebufferHeight > 0);

            framebufferEncoding = GL30.glGetFramebufferAttachmentParameteri(
                    GL30.GL_FRAMEBUFFER,
                    GL11.GL_BACK_LEFT,
                    GL30.GL_FRAMEBUFFER_ATTACHMENT_COLOR_ENCODING);
            assertTrue(
                    framebufferEncoding == GL21.GL_SRGB || framebufferEncoding == GL11.GL_LINEAR,
                    "Default back buffer must report GL_SRGB or GL_LINEAR");

            Matrix4f clearOnlyView = CameraMatrices.view(
                    new Vector3f(0.0f, 0.0f, 2.0f),
                    new Vector3f(0.0f, 0.0f, 1.0f),
                    new Vector3f(0.0f, 1.0f, 0.0f),
                    new Matrix4f());
            Matrix4f roomView = CameraMatrices.view(
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
                renderer.render(clearOnlyView, projection, framebufferWidth, framebufferHeight);
                clearPixel = readPixel(8, 8);

                renderer.render(roomView, projection, framebufferWidth, framebufferHeight);
                baselinePixel = readPixel(framebufferWidth / 4, framebufferHeight / 2);

                assertByte("clear red", clearPixel[0], CLEAR_RED_EXPECTED);
                assertByte("clear green", clearPixel[1], CLEAR_GREEN_EXPECTED);
                assertByte("clear blue", clearPixel[2], CLEAR_BLUE_EXPECTED);
                assertByte("baseline red", baselinePixel[0], BASELINE_EXPECTED);
                assertByte("baseline green", baselinePixel[1], BASELINE_EXPECTED);
                assertByte("baseline blue", baselinePixel[2], BASELINE_EXPECTED);

                assertWrongReferencesOutsideTolerance(clearPixel);

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

                captureBackBuffer(framebufferWidth, framebufferHeight);
                window.present();
            }

            window.stop();
            stopped = true;
            window.close();
            closed = true;
            registry.assertNoOpenResources();
            writeReport(framebufferEncoding, clearPixel, baselinePixel);
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

    private static void assertWrongReferencesOutsideTolerance(int[] actual) {
        int[] missing = {
            Math.round(CLEAR_RED_LINEAR * 255.0f),
            Math.round(CLEAR_GREEN_LINEAR * 255.0f),
            Math.round(CLEAR_BLUE_LINEAR * 255.0f)
        };
        int[] doubled = {
            encodedByte(independentEncodedScalar(CLEAR_RED_LINEAR)),
            encodedByte(independentEncodedScalar(CLEAR_GREEN_LINEAR)),
            encodedByte(independentEncodedScalar(CLEAR_BLUE_LINEAR))
        };
        for (int channel = 0; channel < 3; channel++) {
            assertTrue(Math.abs(actual[channel] - missing[channel]) > BYTE_TOLERANCE);
            assertTrue(Math.abs(actual[channel] - doubled[channel]) > BYTE_TOLERANCE);
        }
    }

    private static void assertByte(String label, int actual, int expected) {
        assertTrue(
                Math.abs(actual - expected) <= BYTE_TOLERANCE,
                label + " expected " + expected + "±" + BYTE_TOLERANCE + " but was " + actual);
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

    private static void writeReport(
            int framebufferEncoding,
            int[] clearPixel,
            int[] baselinePixel) throws IOException {
        int missingRed = Math.round(CLEAR_RED_LINEAR * 255.0f);
        int missingGreen = Math.round(CLEAR_GREEN_LINEAR * 255.0f);
        int missingBlue = Math.round(CLEAR_BLUE_LINEAR * 255.0f);
        int doubleRed = encodedByte(independentEncodedScalar(CLEAR_RED_LINEAR));
        int doubleGreen = encodedByte(independentEncodedScalar(CLEAR_GREEN_LINEAR));
        int doubleBlue = encodedByte(independentEncodedScalar(CLEAR_BLUE_LINEAR));

        Files.createDirectories(REPORT_PATH.getParent());
        Files.write(REPORT_PATH, List.of(
                "task=P5-T15",
                "result=PASS",
                "default.framebuffer.encoding="
                        + (framebufferEncoding == GL21.GL_SRGB ? "GL_SRGB" : "GL_LINEAR"),
                "presentation.mode="
                        + (framebufferEncoding == GL21.GL_SRGB ? "HARDWARE_SRGB" : "MANUAL_SRGB"),
                "clear.linear=0.08,0.10,0.14",
                "clear.expected.srgb="
                        + CLEAR_RED_EXPECTED + "," + CLEAR_GREEN_EXPECTED + "," + CLEAR_BLUE_EXPECTED,
                "clear.actual.rgb=" + rgb(clearPixel),
                "clear.missing.encode.rgb="
                        + missingRed + "," + missingGreen + "," + missingBlue,
                "clear.double.encode.rgb="
                        + doubleRed + "," + doubleGreen + "," + doubleBlue,
                "baseline.expected.srgb.byte=" + BASELINE_EXPECTED,
                "baseline.actual.rgb=" + rgb(baselinePixel),
                "tolerance.bytes=" + BYTE_TOLERANCE,
                "viewport.restored=true",
                "program.unbound=true",
                "vertex.array.unbound=true",
                "framebuffer.srgb.disabled.after.render=true",
                "capture=p5-t15-srgb-presentation.png",
                "native.resource.registry.empty.after.cleanup=true",
                "engine.commit=" + environmentOr("GITHUB_SHA", "unknown"),
                "java.version=" + System.getProperty("java.version"),
                "os.name=" + System.getProperty("os.name"),
                "os.arch=" + System.getProperty("os.arch"),
                "clear.sample.controlled.camera.looks.away.from.room=true",
                "evidence.scope=known linear clear from a controlled camera that culls the room, followed by the accepted lit room reference sample through exactly one IEC sRGB presentation encode; no HDR, tonemapping, fog, bloom, exposure, color grading, offscreen framebuffer, or post-processing claim"));
    }

    private static int litSrgbByte(int srgbByte, double diffuseFactor) {
        double encoded = srgbByte / 255.0;
        double linear = encoded <= 0.04045
                ? encoded / 12.92
                : Math.pow((encoded + 0.055) / 1.055, 2.4);
        return encodedByte((float) (linear * diffuseFactor));
    }

    private static int encodedByte(float linear) {
        return (int) Math.round(independentEncodedScalar(linear) * 255.0);
    }

    private static float independentEncodedScalar(float linear) {
        return (float) (linear <= 0.0031308
                ? linear * 12.92
                : 1.055 * Math.pow(linear, 1.0 / 2.4) - 0.055);
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
