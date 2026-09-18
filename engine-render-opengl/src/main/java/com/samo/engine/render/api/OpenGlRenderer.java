package com.samo.engine.render.api;

import com.samo.engine.core.api.NativeResourceRegistry;
import com.samo.engine.platform.api.OpenGlThreadGuard;
import com.samo.engine.render.opengl.internal.IndexedStaticMeshPipeline;
import com.samo.engine.render.opengl.internal.LwjglOpenGlDrawBackend;
import com.samo.engine.render.opengl.internal.LwjglOpenGlResourceBackend;
import com.samo.engine.render.opengl.internal.LwjglOpenGlUniformBlockReflectionBackend;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.joml.Matrix4fc;

/**
 * Minimal production OpenGL renderer composition for the first indexed static mesh.
 *
 * <p>This API intentionally does not expose native handles, arbitrary meshes, materials, world
 * submission, or asset loading.
 */
public final class OpenGlRenderer implements AutoCloseable {
    private final IndexedStaticMeshPipeline pipeline;

    private OpenGlRenderer(IndexedStaticMeshPipeline pipeline) {
        this.pipeline = pipeline;
    }

    public static OpenGlRenderer create(
            OpenGlThreadGuard threadGuard,
            NativeResourceRegistry nativeResources) {
        OpenGlThreadGuard guard = Objects.requireNonNull(threadGuard, "threadGuard");
        NativeResourceRegistry registry = Objects.requireNonNull(nativeResources, "nativeResources");
        return new OpenGlRenderer(IndexedStaticMeshPipeline.create(
                guard,
                registry,
                new LwjglOpenGlResourceBackend(),
                new LwjglOpenGlDrawBackend(),
                new LwjglOpenGlUniformBlockReflectionBackend(),
                loadShader("shaders/p5/basic.vert"),
                loadShader("shaders/p5/basic.frag")));
    }

    public void render(
            Matrix4fc view,
            Matrix4fc projection,
            int framebufferWidth,
            int framebufferHeight) {
        pipeline.render(view, projection, framebufferWidth, framebufferHeight);
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
