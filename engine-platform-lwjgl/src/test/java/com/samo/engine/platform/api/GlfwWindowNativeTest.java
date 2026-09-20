package com.samo.engine.platform.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.lwjgl.glfw.GLFW.glfwGetCurrentContext;
import static org.lwjgl.opengl.GL11.GL_RENDERER;
import static org.lwjgl.opengl.GL11.GL_VERSION;
import static org.lwjgl.opengl.GL11.glGetString;
import static org.lwjgl.opengl.GL30.GL_MAJOR_VERSION;
import static org.lwjgl.opengl.GL30.GL_MINOR_VERSION;
import static org.lwjgl.opengl.GL30.glGetInteger;

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

class GlfwWindowNativeTest {
    private static final String ENABLE_ENV = "SHERKO_P3_T01_NATIVE";
    private static final Path REPORT_PATH = Path.of("build", "reports", "p3", "p3-t01-glfw-window.txt");

    @Test
    void createsRealOpenGl46WindowLogsActualIdentityAndCleansOwnership() throws Exception {

        assumeTrue(Boolean.parseBoolean(System.getenv(ENABLE_ENV)), () -> "Set " + ENABLE_ENV + "=true to run the P3-T01 native acceptance");
        assertTrue(System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("windows"), "P3-T01 native acceptance targets Windows x64");

        List<EngineLogger.Event> events = new ArrayList<>();
        NativeResourceRegistry registry = new NativeResourceRegistry();
        GlfwWindow window = new GlfwWindow(640, 360, "Sherko Engine P3-T01 Native Acceptance", new EngineLogger(events::add), registry);

        String actualVersion = null;
        String actualRenderer = null;
        int actualMajor = -1;
        int actualMinor = -1;
        boolean started = false;
        boolean stopAttempted = false;
        boolean closed = false;
        try {
            window.initialize();
            window.start();
            started = true;

            assertTrue(glfwGetCurrentContext() != 0L, "the production window must own a current context while started");
            actualMajor = glGetInteger(GL_MAJOR_VERSION);
            actualMinor = glGetInteger(GL_MINOR_VERSION);
            actualVersion = glGetString(GL_VERSION);
            actualRenderer = glGetString(GL_RENDERER);

            String observedVersion = actualMajor + "." + actualMinor;
            assertTrue(actualMajor > 4 || actualMajor == 4 && actualMinor >= 6, "expected OpenGL >= 4.6 but observed " + observedVersion);
            assertTrue(actualVersion != null && !actualVersion.isBlank());
            assertTrue(actualRenderer != null && !actualRenderer.isBlank());
            assertEquals(2, events.size());
            assertLog(events.get(0), "OpenGL version: " + actualVersion);
            assertLog(events.get(1), "OpenGL renderer: " + actualRenderer);

            stopAttempted = true;
            window.stop();
            window.close();
            closed = true;
            assertEquals(0L, glfwGetCurrentContext());
            registry.assertNoOpenResources();
        } finally {
            if (!closed) {
                if (started && !stopAttempted) {
                    attemptCleanup(window::stop);
                }
                attemptCleanup(window::close);
            }
        }

        writeReport(actualMajor, actualMinor, actualVersion, actualRenderer);

    }

    private static boolean attemptCleanup(Runnable cleanup) {

        try {
            cleanup.run();
            return true;
        } catch (RuntimeException | Error cleanupFailure) {
            return false;
        }

    }

    private static void assertLog(EngineLogger.Event event, String message) {

        assertEquals(EngineLogger.Level.INFO, event.level());
        assertEquals(message, event.message());
        assertEquals("platform", event.context().subsystem());

    }

    private static void writeReport(int actualMajor, int actualMinor, String actualVersion, String actualRenderer) throws IOException {

        Files.createDirectories(REPORT_PATH.getParent());
        List<String> lines = List.of("task=P3-T01", "result=PASS", "requested.context=4.6 Core", "actual.major=" + actualMajor, "actual.minor=" + actualMinor,
            "actual.version=" + actualVersion, "actual.renderer=" + actualRenderer, "engine.commit=" + environmentOr("GITHUB_SHA", "unknown"),
            "java.version=" + System.getProperty("java.version"), "os.name=" + System.getProperty("os.name"), "os.arch=" + System.getProperty("os.arch"), "lifecycle.cleanup=PASS",
            "native.resource.registry.empty.after.cleanup=true",
            "evidence.scope=single production GLFW/OpenGL window-context lifecycle; not P0-T13 soak or P0-T14 repeated lifecycle evidence");
        Files.write(REPORT_PATH, lines, StandardCharsets.UTF_8);

    }

    private static String environmentOr(String key, String fallback) {

        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value;

    }
}
