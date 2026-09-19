package com.samo.engine.render.consumer;

import com.samo.engine.core.api.NativeResourceRegistry;
import com.samo.engine.platform.api.OpenGlThreadGuard;
import com.samo.engine.render.api.OpenGlRenderer;
import com.samo.engine.render.api.RenderFramePacket;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

final class RendererPublicApiConsumerFixture {
    private RendererPublicApiConsumerFixture() {
    }

    static void compileOnlyUsage(
            OpenGlThreadGuard threadGuard,
            NativeResourceRegistry nativeResources,
            Matrix4fc view,
            Matrix4fc projection) {
        RenderFramePacket frame = new RenderFramePacket(view, projection, 1280, 720);
        try (OpenGlRenderer renderer = OpenGlRenderer.create(threadGuard, nativeResources)) {
            renderer.render(frame);
        }
    }

    static Matrix4fc jomlRemainsTransitivelyAvailable() {
        return new Matrix4f();
    }
}
