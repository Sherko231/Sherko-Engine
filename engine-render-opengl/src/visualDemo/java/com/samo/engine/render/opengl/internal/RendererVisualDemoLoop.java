package com.samo.engine.render.opengl.internal;

import com.samo.engine.core.api.CameraMatrices;
import com.samo.engine.core.api.DebugFrame;
import com.samo.engine.core.api.EngineClock;
import com.samo.engine.platform.api.GlfwWindow;
import com.samo.engine.platform.api.InputKey;
import com.samo.engine.platform.api.InputSnapshot;
import com.samo.engine.render.api.OpenGlRenderer;
import com.samo.engine.render.api.RenderFramePacket;
import java.util.List;
import org.joml.Matrix4f;
import org.joml.Vector3f;

final class RendererVisualDemoLoop {
    private RendererVisualDemoLoop() {
    }

    static void run(
            GlfwWindow window,
            OpenGlRenderer renderer,
            MaterialComparisonOverlay overlay,
            RendererVisualDemoFramebufferSize framebuffer) {
        Matrix4f view = CameraMatrices.view(
                new Vector3f(0.0f, 0.0f, 2.0f),
                new Vector3f(0.0f, 0.0f, -1.0f),
                new Vector3f(0.0f, 1.0f, 0.0f),
                new Matrix4f());
        Matrix4f projection = new Matrix4f();

        EngineClock clock = new EngineClock();
        clock.sampleElapsedNanos();
        long elapsedNanos = 0L;
        long inputFrame = 0L;
        boolean exit = false;

        while (!exit) {
            elapsedNanos = saturatingAdd(elapsedNanos, clock.sampleElapsedNanos());
            double seconds = elapsedNanos / 1_000_000_000.0;

            window.pollEvents();
            InputSnapshot input = window.captureInputSnapshot(inputFrame++);
            exit = input.keyPressed(InputKey.ESCAPE)
                    || (input.keyPressed(InputKey.Q)
                            && (input.keyHeld(InputKey.LEFT_CONTROL)
                                    || input.keyHeld(InputKey.RIGHT_CONTROL)));

            int width = framebuffer.width();
            int height = framebuffer.height();
            if (!exit && width > 0 && height > 0) {
                CameraMatrices.perspective(
                        (float) Math.toRadians(70.0),
                        (float) width / height,
                        0.1f,
                        100.0f,
                        projection);

                AnimatedDemoLighting.LightingFrame lighting = AnimatedDemoLighting.at(seconds);
                RenderFramePacket frame = new RenderFramePacket(
                        view,
                        projection,
                        width,
                        height,
                        lighting.lights(),
                        new DebugFrame(lighting.debugPrimitives(), List.of()));

                renderer.render(frame);
                overlay.render(width, height);
                window.present();
            }
        }
    }

    static long saturatingAdd(long left, long right) {
        if (right > 0L && left > Long.MAX_VALUE - right) {
            return Long.MAX_VALUE;
        }
        return left + right;
    }
}
