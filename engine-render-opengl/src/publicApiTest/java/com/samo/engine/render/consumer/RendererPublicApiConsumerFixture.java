package com.samo.engine.render.consumer;

import com.samo.engine.core.api.DebugColor;
import com.samo.engine.core.api.DebugFrame;
import com.samo.engine.core.api.DebugLine;
import com.samo.engine.core.api.DebugTextCounter;
import com.samo.engine.core.api.EngineLogger;
import com.samo.engine.core.api.NativeResourceRegistry;
import com.samo.engine.platform.api.OpenGlThreadGuard;
import com.samo.engine.render.api.OpenGlRenderer;
import com.samo.engine.render.api.RenderCullingCounters;
import com.samo.engine.render.api.RenderFramePacket;
import com.samo.engine.render.api.RenderLocalLight;
import com.samo.engine.render.api.RenderPointLight;
import com.samo.engine.render.api.RenderSpotLight;
import java.util.List;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

final class RendererPublicApiConsumerFixture {
    private RendererPublicApiConsumerFixture() {

    }

    static void compileOnlyUsage(OpenGlThreadGuard threadGuard, NativeResourceRegistry nativeResources, Matrix4fc view, Matrix4fc projection) {

        RenderLocalLight point = new RenderPointLight(0.0f, 0.0f, 1.0f, 1.0f, 0.5f, 0.25f, 0.5f, 4.0f);
        RenderLocalLight spot = new RenderSpotLight(0.0f, 0.0f, 1.0f, 0.0f, 0.0f, -1.0f, 0.25f, 0.5f, 1.0f, 0.5f, 5.0f, 0.2f, 0.5f);
        DebugFrame debugFrame = new DebugFrame(List.of(new DebugLine(-1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, new DebugColor(0.0f, 1.0f, 0.0f))),
            List.of(new DebugTextCounter("tick", 7L)));
        RenderFramePacket frame = new RenderFramePacket(view, projection, 1280, 720, List.of(point, spot), debugFrame);
        EngineLogger logger = new EngineLogger(event -> {
        });
        try (OpenGlRenderer renderer = OpenGlRenderer.create(threadGuard, nativeResources, logger, 4)) {
            renderer.render(frame);
            RenderCullingCounters counters = renderer.lastCullingCounters();
            counters.submittedDraws();
            renderer.lastDebugTextCounters().getFirst().value();
        }

    }

    static Matrix4fc jomlRemainsTransitivelyAvailable() {

        return new Matrix4f();

    }
}
