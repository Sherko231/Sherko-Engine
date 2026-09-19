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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import javax.imageio.ImageIO;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL21;
import org.lwjgl.opengl.GL30;

class SrgbColorPathNativeTest {
    private static final String ENABLE_ENV = "SHERKO_P5_T08_NATIVE";
    private static final int WIDTH = 640;
    private static final int HEIGHT = 360;
    private static final int REFERENCE_INPUT_SRGB_BYTE = 128;
    private static final float REFERENCE_DIFFUSE_FACTOR =
            0.8f * (float) (1.0 / Math.sqrt(2.0));
    private static final int REFERENCE_OUTPUT_SRGB_BYTE =
            litSrgbByte(REFERENCE_INPUT_SRGB_BYTE, REFERENCE_DIFFUSE_FACTOR);
    private static final int BYTE_TOLERANCE = 8;
    private static final Path REPORT_PATH =
            Path.of("build", "reports", "p5", "p5-t08-srgb.txt");
    private static final Path CAPTURE_PATH =
            Path.of("build", "reports", "p5", "p5-t08-srgb.png");

    @Test
    void decodesSrgbTextureAndEncodesDefaultFramebufferExactlyOnce() throws Exception {
        assumeTrue(Boolean.parseBoolean(System.getenv(ENABLE_ENV)),
                () -> "Set " + ENABLE_ENV + "=true to run the P5-T08 native acceptance");
        assertTrue(System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("windows"),
                "P5-T08 native acceptance targets Windows x64");

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
                "Sherko Engine P5-T08 sRGB Acceptance",
                new EngineLogger(event -> { }),
                registry,
                sizeListener,
                OpenGlDebugMode.FAIL_ON_HIGH_SEVERITY);

        boolean started = false;
        boolean stopped = false;
        boolean closed = false;
        try {
            window.initialize();
            window.start();
            started = true;
            window.pollEvents();

            int framebufferWidth = framebufferSize[0];
            int framebufferHeight = framebufferSize[1];
            assertTrue(framebufferWidth > 0 && framebufferHeight > 0,
                    "Native acceptance requires a visible non-zero framebuffer");

            int framebufferEncoding = GL30.glGetFramebufferAttachmentParameteri(
                    GL30.GL_FRAMEBUFFER,
                    GL11.GL_BACK_LEFT,
                    GL30.GL_FRAMEBUFFER_ATTACHMENT_COLOR_ENCODING);
            assertTrue(
                    framebufferEncoding == GL21.GL_SRGB || framebufferEncoding == GL11.GL_LINEAR,
                    "Default back buffer must report GL_SRGB or GL_LINEAR color encoding, but was "
                            + framebufferEncoding);

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

            int red;
            int green;
            int blue;
            try (OpenGlRenderer renderer =
                    OpenGlRenderer.create(window.openGlThreadGuard(), registry)) {
                renderer.render(view, projection, framebufferWidth, framebufferHeight);
                assertFalse(
                        GL11.glIsEnabled(GL30.GL_FRAMEBUFFER_SRGB),
                        "Renderer must not leak GL_FRAMEBUFFER_SRGB state after render");
                window.pollEvents();

                int[] baselinePixel = readPixel(framebufferWidth / 4, framebufferHeight / 2);
                red = baselinePixel[0];
                green = baselinePixel[1];
                blue = baselinePixel[2];

                assertReferenceByte("red", red);
                assertReferenceByte("green", green);
                assertReferenceByte("blue", blue);
                assertTrue(Math.abs(red - green) <= 2 && Math.abs(red - blue) <= 2,
                        "Reference texture must remain neutral gray: rgb=" + red + "," + green + "," + blue);

                captureBackBuffer(framebufferWidth, framebufferHeight);
                window.present();
            }

            window.stop();
            stopped = true;
            window.close();
            closed = true;
            registry.assertNoOpenResources();

            writeReport(framebufferEncoding, red, green, blue);
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

    private static void assertReferenceByte(String channel, int actual) {
        assertTrue(
                Math.abs(actual - REFERENCE_OUTPUT_SRGB_BYTE) <= BYTE_TOLERANCE,
                channel + " expected " + REFERENCE_OUTPUT_SRGB_BYTE + "±" + BYTE_TOLERANCE + " but was " + actual);
    }

    private static int[] readPixel(int x, int y) {
        ByteBuffer pixel = ByteBuffer.allocateDirect(4);
        GL11.glReadBuffer(GL11.GL_BACK);
        GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);
        GL11.glReadPixels(
                x,
                y,
                1,
                1,
                GL11.GL_RGBA,
                GL11.GL_UNSIGNED_BYTE,
                pixel);
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
                int argb = (alpha << 24) | (red << 16) | (green << 8) | blue;
                image.setRGB(x, y, argb);
            }
        }

        Files.createDirectories(CAPTURE_PATH.getParent());
        if (!ImageIO.write(image, "png", CAPTURE_PATH.toFile())) {
            throw new IOException("PNG writer unavailable");
        }
    }

    private static void writeReport(int framebufferEncoding, int red, int green, int blue)
            throws IOException {
        double litLinear = srgbToLinear(REFERENCE_INPUT_SRGB_BYTE / 255.0) * REFERENCE_DIFFUSE_FACTOR;
        int missingEncode = (int) Math.round(litLinear * 255.0);
        int missingDecodeOrDoubleGamma = srgbByteFromLinear(
                (REFERENCE_INPUT_SRGB_BYTE / 255.0) * REFERENCE_DIFFUSE_FACTOR);

        Files.createDirectories(REPORT_PATH.getParent());
        Files.write(REPORT_PATH, List.of(
                "task=P5-T08",
                "result=PASS",
                "default.framebuffer.encoding=" + framebufferEncoding,
                "framebuffer.encoding.mode="
                        + (framebufferEncoding == GL21.GL_SRGB ? "GL_SRGB" : "GL_LINEAR"),
                "presentation.encode.mode="
                        + (framebufferEncoding == GL21.GL_SRGB ? "hardware-framebuffer" : "manual-fragment"),
                "texture.encoding=GL_SRGB8_ALPHA8",
                "reference.input.srgb.byte=" + REFERENCE_INPUT_SRGB_BYTE,
                "reference.directional.diffuse.factor=" + REFERENCE_DIFFUSE_FACTOR,
                "reference.expected.output.srgb.byte=" + REFERENCE_OUTPUT_SRGB_BYTE,
                "reference.output.rgb=" + red + "," + green + "," + blue,
                "reference.tolerance.bytes=" + BYTE_TOLERANCE,
                "wrong.missing.encode.approx.byte=" + missingEncode,
                "wrong.missing.decode.or.double.gamma.approx.byte=" + missingDecodeOrDoubleGamma,
                "capture=p5-t08-srgb.png",
                "native.resource.registry.empty.after.cleanup=true",
                "engine.commit=" + environmentOr("GITHUB_SHA", "unknown"),
                "java.version=" + System.getProperty("java.version"),
                "os.name=" + System.getProperty("os.name"),
                "os.arch=" + System.getProperty("os.arch"),
                "evidence.scope=fixed renderer reference texture decode, known P5-T13 linear diffuse multiplication, plus exactly one presentation sRGB encode; hardware on GL_SRGB default buffers, fragment fallback on GL_LINEAR default buffers; no HDR, tonemapping, arbitrary materials, assets, or post-processing claim"));
    }

    private static int litSrgbByte(int srgbByte, double diffuseFactor) {
        double linear = srgbToLinear(srgbByte / 255.0) * diffuseFactor;
        return srgbByteFromLinear(linear);
    }

    private static double srgbToLinear(double encoded) {
        return encoded <= 0.04045
                ? encoded / 12.92
                : Math.pow((encoded + 0.055) / 1.055, 2.4);
    }

    private static int srgbByteFromLinear(double linear) {
        double encoded = linear <= 0.0031308
                ? linear * 12.92
                : 1.055 * Math.pow(linear, 1.0 / 2.4) - 0.055;
        return (int) Math.round(encoded * 255.0);
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
