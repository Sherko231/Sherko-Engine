package com.samo.engine.render.opengl.internal;

/**
 * Standalone owner-facing visual demo for already-implemented Phase 5 renderer behavior.
 *
 * <p>
 * This is intentionally not a production/public renderer API. The normal game sandbox and
 * production OpenGlRenderer composition remain unchanged.
 */
public final class RendererVisualDemo {
    private RendererVisualDemo() {
    }

    public static void main(String[] args) {
        RendererVisualDemoApplication.run();
    }
}
