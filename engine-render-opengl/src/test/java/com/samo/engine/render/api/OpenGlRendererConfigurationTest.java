package com.samo.engine.render.api;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.samo.engine.core.api.EngineLogger;
import com.samo.engine.core.api.NativeResourceRegistry;
import com.samo.engine.platform.api.GlfwWindow;
import org.junit.jupiter.api.Test;

class OpenGlRendererConfigurationTest {
    @Test
    void rejectsLocalLightMaximumBeforeCreatingNativeResources() {
        NativeResourceRegistry registry = new NativeResourceRegistry();
        GlfwWindow window = new GlfwWindow(1, 1, "renderer config fixture", new EngineLogger(event -> {
        }), registry);
        EngineLogger logger = new EngineLogger(event -> {
        });

        assertThrows(IllegalArgumentException.class, () -> OpenGlRenderer.create(window.openGlThreadGuard(), registry, logger, 0));
        registry.assertNoOpenResources();

        assertThrows(IllegalArgumentException.class, () -> OpenGlRenderer.create(window.openGlThreadGuard(), registry, logger, 9));
        registry.assertNoOpenResources();
    }
}
