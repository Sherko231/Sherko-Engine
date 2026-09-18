package com.samo.engine.render.opengl.internal;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.samo.engine.core.api.EngineLogger;
import com.samo.engine.core.api.NativeResourceRegistry;
import com.samo.engine.platform.api.GlfwWindow;
import com.samo.engine.platform.api.OpenGlDebugMode;
import com.samo.engine.platform.api.OpenGlThreadGuard;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL45;

class BoundedDynamicBufferUploaderNativeTest {
    private static final String ENABLE_ENV = "SHERKO_P5_T04_NATIVE";
    private static final Path REPORT_PATH =
            Path.of("build", "reports", "p5", "p5-t04-bounded-upload.txt");

    @Test
    void uploadsWrapsSynchronizesAndReadsBackOnRealOpenGl46() throws Exception {
        assumeTrue(Boolean.parseBoolean(System.getenv(ENABLE_ENV)),
                () -> "Set " + ENABLE_ENV + "=true to run the P5-T04 native acceptance");
        assertTrue(System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("windows"),
                "P5-T04 native acceptance targets Windows x64");

        NativeResourceRegistry registry = new NativeResourceRegistry();
        GlfwWindow window = new GlfwWindow(
                640,
                360,
                "Sherko Engine P5-T04 Native Acceptance",
                new EngineLogger(event -> { }),
                registry,
                OpenGlDebugMode.FAIL_ON_HIGH_SEVERITY);

        boolean started = false;
        boolean stopped = false;
        boolean closed = false;
        try {
            window.initialize();
            window.start();
            started = true;

            OpenGlThreadGuard guard = window.openGlThreadGuard();
            OpenGlResourceBackend backend = new LwjglOpenGlResourceBackend();

            try (BoundedDynamicBufferUploader uploader =
                    BoundedDynamicBufferUploader.create(3, 8, guard, registry, backend)) {
                BoundedDynamicBufferUploader.Slice first = uploader.upload(bytes(1, 2, 3));
                BoundedDynamicBufferUploader.Slice second = uploader.upload(bytes(4, 5));
                BoundedDynamicBufferUploader.Slice third = uploader.upload(bytes(6, 7, 8, 9));

                assertEquals(0L, first.offsetBytes());
                assertEquals(8L, second.offsetBytes());
                assertEquals(16L, third.offsetBytes());

                uploader.markSubmitted(first);
                uploader.markSubmitted(second);
                uploader.markSubmitted(third);

                GL11.glFinish();

                BoundedDynamicBufferUploader.Slice wrapped = uploader.upload(bytes(10, 11, 12));
                assertEquals(0L, wrapped.offsetBytes());
                uploader.markSubmitted(wrapped);

                GL11.glFinish();

                ByteBuffer readback = ByteBuffer.allocateDirect(24);
                GL45.glGetNamedBufferSubData(uploader.bufferHandle(), 0L, readback);
                byte[] bytes = new byte[24];
                readback.get(bytes);

                assertArrayEquals(new byte[] {10, 11, 12}, slice(bytes, 0, 3));
                assertArrayEquals(new byte[] {4, 5}, slice(bytes, 8, 2));
                assertArrayEquals(new byte[] {6, 7, 8, 9}, slice(bytes, 16, 4));
            }

            window.stop();
            stopped = true;
            window.close();
            closed = true;
            registry.assertNoOpenResources();
        } finally {
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

    private static ByteBuffer bytes(int... values) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(values.length);
        for (int value : values) {
            buffer.put((byte) value);
        }
        return buffer.flip();
    }

    private static byte[] slice(byte[] source, int offset, int length) {
        return java.util.Arrays.copyOfRange(source, offset, offset + length);
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
                "task=P5-T04",
                "result=PASS",
                "strategy=fixed-slot ring with per-submitted-slot GLsync fence",
                "slot.count=3",
                "slot.capacity.bytes=8",
                "total.capacity.bytes=24",
                "round.robin.offsets=0,8,16,0",
                "fence.reuse.after.glFinish=PASS",
                "buffer.readback=PASS",
                "thread.affinity=PASS",
                "native.resource.registry.empty.after.cleanup=true",
                "persistent.mapping=false",
                "engine.commit=" + environmentOr("GITHUB_SHA", "unknown"),
                "java.version=" + System.getProperty("java.version"),
                "os.name=" + System.getProperty("os.name"),
                "os.arch=" + System.getProperty("os.arch"),
                "evidence.scope=bounded upload correctness only; no performance or persistent-mapping claim"),
                StandardCharsets.UTF_8);
    }

    private static String environmentOr(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value;
    }
}
