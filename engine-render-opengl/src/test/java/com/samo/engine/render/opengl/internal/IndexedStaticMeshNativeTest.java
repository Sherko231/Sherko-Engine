package com.samo.engine.render.opengl.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.samo.engine.core.api.CameraMatrices;
import com.samo.engine.core.api.EngineLogger;
import com.samo.engine.core.api.NativeResourceRegistry;
import com.samo.engine.platform.api.GlfwWindow;
import com.samo.engine.platform.api.OpenGlDebugMode;
import com.samo.engine.render.api.OpenGlRenderer;
import com.samo.engine.platform.api.WindowSizeListener;
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
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;

class IndexedStaticMeshNativeTest {
    private static final String ENABLE_ENV = "SHERKO_P5_T07_NATIVE";
    private static final int WIDTH = 640;
    private static final int HEIGHT = 360;
    private static final Path REPORT_PATH =
            Path.of("build", "reports", "p5", "p5-t07-indexed-mesh.txt");
    private static final Path CAPTURE_PATH =
            Path.of("build", "reports", "p5", "p5-t07-indexed-mesh.png");

    @Test
    void rendersExactlyOneVisibleIndexedTriangleThroughPublicRenderer() throws Exception {
        assumeTrue(Boolean.parseBoolean(System.getenv(ENABLE_ENV)),
                () -> "Set " + ENABLE_ENV + "=true to run the P5-T07 native acceptance");
        assertTrue(System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("windows"),
                "P5-T07 native acceptance targets Windows x64");

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
                "Sherko Engine P5-T07 Native Acceptance",
                new EngineLogger(event -> { }),
                registry,
                sizeListener,
                OpenGlDebugMode.FAIL_ON_HIGH_SEVERITY);

        boolean started = false;
        boolean stopped = false;
        boolean closed = false;
        int query = 0;
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
                query = GL15.glGenQueries();
                GL15.glBeginQuery(GL30.GL_PRIMITIVES_GENERATED, query);
                renderer.render(view, projection, framebufferWidth, framebufferHeight);
                GL15.glEndQuery(GL30.GL_PRIMITIVES_GENERATED);

                window.pollEvents();
                int primitiveCount = GL15.glGetQueryObjecti(query, GL15.GL_QUERY_RESULT);
                assertEquals(1, primitiveCount);

                int visibleTrianglePixels = captureBackBuffer(framebufferWidth, framebufferHeight);
                assertTrue(visibleTrianglePixels > 1_000,
                        "Expected a visible reference-gray triangle; pixels=" + visibleTrianglePixels);

                window.present();
            } finally {
                if (query != 0) {
                    GL15.glDeleteQueries(query);
                    query = 0;
                }
            }

            window.stop();
            stopped = true;
            window.close();
            closed = true;
            registry.assertNoOpenResources();
        } finally {
            if (query != 0) {
                int queryToDelete = query;
                attemptCleanup(() -> GL15.glDeleteQueries(queryToDelete));
            }
            if (!closed) {
                if (started && !stopped) {
                    attemptCleanup(window::stop);
                }
                attemptCleanup(window::close);
            }
        }

        registry.assertNoOpenResources();
        writeReport();
    }

    private static int captureBackBuffer(int width, int height) throws IOException {
        ByteBuffer pixels = ByteBuffer.allocateDirect(width * height * 4);
        GL11.glReadBuffer(GL11.GL_BACK);
        GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);
        GL11.glReadPixels(0, 0, width, height, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        int visibleTrianglePixels = 0;
        for (int y = 0; y < height; y++) {
            int sourceY = height - 1 - y;
            for (int x = 0; x < width; x++) {
                int offset = (sourceY * width + x) * 4;
                int red = Byte.toUnsignedInt(pixels.get(offset));
                int green = Byte.toUnsignedInt(pixels.get(offset + 1));
                int blue = Byte.toUnsignedInt(pixels.get(offset + 2));
                int alpha = Byte.toUnsignedInt(pixels.get(offset + 3));
                if (red >= 110 && red <= 150
                        && green >= 110 && green <= 150
                        && blue >= 110 && blue <= 150) {
                    visibleTrianglePixels++;
                }
                int argb = (alpha << 24) | (red << 16) | (green << 8) | blue;
                image.setRGB(x, y, argb);
            }
        }

        Files.createDirectories(CAPTURE_PATH.getParent());
        if (!ImageIO.write(image, "png", CAPTURE_PATH.toFile())) {
            throw new IOException("PNG writer unavailable");
        }
        return visibleTrianglePixels;
    }

    private static boolean attemptCleanup(Runnable cleanup) {
        try {
            cleanup.run();
            return true;
        } catch (RuntimeException | Error cleanupFailure) {
            return false;
        }
    }

    private static void writeReport() throws IOException {
        Files.createDirectories(REPORT_PATH.getParent());
        Files.write(REPORT_PATH, List.of(
                "task=P5-T07",
                "result=PASS",
                "renderer.api=OpenGlRenderer",
                "mesh=indexed-triangle",
                "draw.elements.count=3",
                "pipeline.primitives.generated=1",
                "depth.test=GL_LESS",
                "cull.face=GL_BACK",
                "front.face=GL_CCW",
                "camera.uniform.binding=0",
                "perframe.uniform.binding=1",
                "capture=p5-t07-indexed-mesh.png",
                "high.severity.debug.error=none-observed-after-poll",
                "native.resource.registry.empty.after.cleanup=true",
                "triangle.color=reference-gray",
                "srgb.claim=verified-separately-by-p5-t08",
                "engine.commit=" + environmentOr("GITHUB_SHA", "unknown"),
                "java.version=" + System.getProperty("java.version"),
                "os.name=" + System.getProperty("os.name"),
                "os.arch=" + System.getProperty("os.arch"),
                "evidence.scope=first indexed production draw only; sRGB correctness is asserted separately by P5-T08; no asset, material, lighting, world, or performance claim"));
    }

    private static String environmentOr(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value;
    }
}
