package com.samo.engine.platform.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.samo.engine.core.api.EngineLogger;
import com.samo.engine.core.api.NativeResourceRegistry;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL43;

class GlfwWindowDebugNativeTest {
    private static final String ENABLE_ENV = "SHERKO_P5_T01_NATIVE";
    private static final Path REPORT_PATH = Path.of("build", "reports", "p5", "p5-t01-opengl-debug.txt");

    @Test
    void capturesRealHighSeverityOpenGlErrorAndSurfacesOwnerThreadFailure() throws Exception {

        assumeTrue(Boolean.parseBoolean(System.getenv(ENABLE_ENV)), () -> "Set " + ENABLE_ENV + "=true to run the P5-T01 native acceptance");
        assertTrue(System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("windows"), "P5-T01 native acceptance targets Windows x64");

        List<EngineLogger.Event> events = new ArrayList<>();
        NativeResourceRegistry registry = new NativeResourceRegistry();
        GlfwWindow window = new GlfwWindow(640, 360, "Sherko Engine P5-T01 Native Acceptance", new EngineLogger(events::add), registry, OpenGlDebugMode.FAIL_ON_HIGH_SEVERITY);

        boolean started = false;
        boolean stopped = false;
        boolean closed = false;
        IllegalStateException observedFailure = null;
        try {
            window.initialize();
            window.start();
            started = true;

            int contextFlags = GL11.glGetInteger(GL30.GL_CONTEXT_FLAGS);
            assertTrue((contextFlags & GL43.GL_CONTEXT_FLAG_DEBUG_BIT) != 0, "production debug mode must create an OpenGL debug context");

            GL11.glEnable(-1);

            observedFailure = assertThrows(IllegalStateException.class, window::pollEvents);
            assertTrue(observedFailure.getMessage().contains("source=API"));
            assertTrue(observedFailure.getMessage().contains("type=ERROR"));
            assertTrue(observedFailure.getMessage().contains("severity=HIGH"));

            EngineLogger.Event debugEvent = events.stream().filter(event -> event.message().startsWith("OpenGL debug")).findFirst()
                .orElseThrow(() -> new AssertionError("expected one OpenGL debug event"));
            assertEquals(EngineLogger.Level.ERROR, debugEvent.level());
            assertTrue(debugEvent.message().contains("source=API"));
            assertTrue(debugEvent.message().contains("type=ERROR"));
            assertTrue(debugEvent.message().contains("severity=HIGH"));

            window.pollEvents();

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

        writeReport(observedFailure, events);

    }

    private static boolean attemptCleanup(Runnable cleanup) {

        try {
            cleanup.run();
            return true;
        } catch (RuntimeException | Error cleanupFailure) {
            return false;
        }

    }

    private static void writeReport(IllegalStateException observedFailure, List<EngineLogger.Event> events) throws IOException {

        Files.createDirectories(REPORT_PATH.getParent());
        EngineLogger.Event debugEvent = events.stream().filter(event -> event.message().startsWith("OpenGL debug")).findFirst().orElseThrow();
        List<String> lines = List.of("task=P5-T01", "result=PASS", "debug.context.flag=PASS", "intentional.invalid.call=glEnable(-1)", "debug.event.level=" + debugEvent.level(),
            "debug.event.message=" + debugEvent.message().replace('\n', ' ').replace('\r', ' '),
            "owner.thread.failure=" + observedFailure.getMessage().replace('\n', ' ').replace('\r', ' '), "failure.consumed.once=true",
            "engine.commit=" + environmentOr("GITHUB_SHA", "unknown"), "java.version=" + System.getProperty("java.version"), "os.name=" + System.getProperty("os.name"),
            "os.arch=" + System.getProperty("os.arch"), "native.resource.registry.empty.after.cleanup=true",
            "evidence.scope=single production OpenGL debug callback acceptance; not renderer correctness, soak, or release-performance evidence");
        Files.write(REPORT_PATH, lines, StandardCharsets.UTF_8);

    }

    private static String environmentOr(String key, String fallback) {

        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value;

    }
}
