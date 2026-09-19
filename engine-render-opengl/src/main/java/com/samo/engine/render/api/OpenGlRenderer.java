package com.samo.engine.render.api;

import com.samo.engine.core.api.DebugTextCounter;
import com.samo.engine.core.api.EngineLogger;
import com.samo.engine.core.api.NativeResourceRegistry;
import com.samo.engine.platform.api.OpenGlThreadGuard;
import com.samo.engine.render.opengl.internal.IndexedStaticMeshPipeline;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import org.joml.Matrix4fc;

/**
 * Minimal production OpenGL renderer composition for the fixed indexed reference mesh.
 *
 * <p>The current internal path draws the same owned mesh through two renderer-internal material
 * values, samples one renderer-owned sRGB reference texture, and writes linear shader output through
 * exactly one presentation sRGB encode, using hardware when the default buffer is sRGB or an internal
 * fallback when it is linear. The material values remain implementation details until stable runtime
 * asset/resource references exist. P5-T10 adds an immutable renderer-facing frame packet that
 * snapshots camera matrices and framebuffer size without retaining mutable world/gameplay objects.
 * P5-T11 derives a CPU view frustum from that snapshot, tests the fixed reference mesh world AABB
 * through the accepted Phase 4 geometry semantics, and exposes latest-successful-frame culling
 * counters. P5-T14 adds bounded point/spot light submission through `RenderFramePacket` with a
 * configurable maximum of 1-8 local lights per frame. This API intentionally does not expose native
 * handles, arbitrary meshes/textures/materials, world components, or asset loading.
 */
public final class OpenGlRenderer implements AutoCloseable {
    private final IndexedStaticMeshPipeline pipeline;

    private OpenGlRenderer(IndexedStaticMeshPipeline pipeline) {
        this.pipeline = pipeline;
    }

    public static OpenGlRenderer create(
            OpenGlThreadGuard threadGuard,
            NativeResourceRegistry nativeResources) {
        return create(
                threadGuard,
                nativeResources,
                new EngineLogger(event -> System.err.printf(
                        "[%s] [%s] %s%n",
                        event.level(),
                        event.context().subsystem(),
                        event.message())),
                8);
    }

    public static OpenGlRenderer create(
            OpenGlThreadGuard threadGuard,
            NativeResourceRegistry nativeResources,
            EngineLogger logger,
            int maxLocalLights) {
        OpenGlThreadGuard guard = Objects.requireNonNull(threadGuard, "threadGuard");
        NativeResourceRegistry registry = Objects.requireNonNull(nativeResources, "nativeResources");
        EngineLogger engineLogger = Objects.requireNonNull(logger, "logger");
        if (maxLocalLights < 1 || maxLocalLights > 8) {
            throw new IllegalArgumentException("maxLocalLights must be within [1,8]");
        }
        return new OpenGlRenderer(IndexedStaticMeshPipeline.createProduction(
                guard,
                registry,
                engineLogger,
                maxLocalLights,
                loadShader("shaders/p5/basic.vert"),
                loadShader("shaders/p5/basic.frag"),
                loadShader("shaders/p5/debug-lines.vert"),
                loadShader("shaders/p5/debug-lines.frag")));
    }

    public void render(RenderFramePacket frame) {
        pipeline.render(Objects.requireNonNull(frame, "frame"));
    }

    /**
     * Returns immutable counters from the latest successfully completed render call.
     *
     * <p>A failed render does not replace the previously published counters.
     */
    public RenderCullingCounters lastCullingCounters() {
        return pipeline.lastCullingCounters();
    }

    /**
     * Returns the bounded text-counter snapshot from the latest successfully completed render call.
     *
     * <p>A failed render leaves the previously published snapshot unchanged.
     */
    public List<DebugTextCounter> lastDebugTextCounters() {
        return pipeline.lastDebugTextCounters();
    }

    public void render(
            Matrix4fc view,
            Matrix4fc projection,
            int framebufferWidth,
            int framebufferHeight) {
        render(new RenderFramePacket(view, projection, framebufferWidth, framebufferHeight));
    }

    @Override
    public void close() {
        pipeline.close();
    }

    private static String loadShader(String path) {
        try (InputStream stream = OpenGlRenderer.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException("Missing renderer shader resource: " + path);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalStateException("Failed to load renderer shader resource: " + path, failure);
        }
    }
}
