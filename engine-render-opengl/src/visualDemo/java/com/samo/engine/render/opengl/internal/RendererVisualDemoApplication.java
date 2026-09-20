package com.samo.engine.render.opengl.internal;

import com.samo.engine.core.api.EngineLogger;
import com.samo.engine.core.api.NativeResourceRegistry;
import com.samo.engine.platform.api.GlfwWindow;
import com.samo.engine.platform.api.OpenGlDebugMode;
import com.samo.engine.render.api.OpenGlRenderer;

final class RendererVisualDemoApplication {
    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;

    private RendererVisualDemoApplication() {
    }

    static void run() {
        NativeResourceRegistry registry = new NativeResourceRegistry();
        RendererVisualDemoFramebufferSize framebuffer = new RendererVisualDemoFramebufferSize(WIDTH, HEIGHT);

        EngineLogger logger = new EngineLogger(event -> System.out.printf("[%s] [%s] %s%n", event.level(), event.context().subsystem(), event.message()));

        GlfwWindow window = new GlfwWindow(WIDTH, HEIGHT, "Sherko Renderer Visual Demo", logger, registry, framebuffer, OpenGlDebugMode.FAIL_ON_HIGH_SEVERITY);

        boolean started = false;
        boolean stopped = false;
        boolean closed = false;
        try {
            window.initialize();
            window.start();
            started = true;
            window.pollEvents();

            printOwnerInstructions();

            try (OpenGlRenderer renderer = OpenGlRenderer.create(window.openGlThreadGuard(), registry);
                MaterialComparisonOverlay overlay = MaterialComparisonOverlay.create(window.openGlThreadGuard(), registry)) {
                RendererVisualDemoLoop.run(window, renderer, overlay, framebuffer);
            }

            window.stop();
            stopped = true;
            window.close();
            closed = true;
            registry.assertNoOpenResources();
        } finally {
            if (!closed) {
                if (started && !stopped) {
                    try {
                        window.stop();
                    } catch (RuntimeException | Error ignored) {
                    }
                }
                try {
                    window.close();
                } catch (RuntimeException | Error ignored) {
                }
            }
        }

        registry.assertNoOpenResources();
    }

    private static void printOwnerInstructions() {
        System.out.println("Renderer visual demo");
        System.out.println("  LEFT panel  = OPAQUE");
        System.out.println("  RIGHT panel = TRANSPARENT (alpha blended)");
        System.out.println("  Orange cross = moving POINT light");
        System.out.println("  Cyan cross + ray = moving SPOT light");
        System.out.println("  ESC or Ctrl+Q = exit");
    }
}
