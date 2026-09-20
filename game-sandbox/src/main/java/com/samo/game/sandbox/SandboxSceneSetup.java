package com.samo.game.sandbox;

import com.samo.engine.core.api.Aabb3f;
import com.samo.engine.core.api.CameraMatrices;
import com.samo.engine.core.api.DebugAabb;
import com.samo.engine.core.api.DebugColor;
import com.samo.engine.core.api.DebugFrame;
import com.samo.engine.core.api.DebugLine;
import com.samo.engine.core.api.DebugPrimitive;
import com.samo.engine.core.api.DebugRay;
import com.samo.engine.core.api.DebugSphere;
import com.samo.engine.core.api.DebugTextCounter;
import com.samo.engine.core.api.Ray3f;
import com.samo.engine.core.api.Sphere3f;
import com.samo.engine.render.api.RenderFramePacket;
import com.samo.engine.render.api.RenderLocalLight;
import com.samo.engine.render.api.RenderPointLight;
import com.samo.engine.render.api.RenderSpotLight;
import java.util.List;
import org.joml.Matrix4f;
import org.joml.Vector3f;

final class SandboxSceneSetup {
    static final float VERTICAL_FOV_RADIANS = (float) Math.toRadians(70.0);
    static final float NEAR_PLANE_METERS = 0.1f;
    static final float FAR_PLANE_METERS = 100.0f;

    private final Matrix4f view = new Matrix4f();
    private final Matrix4f projection = new Matrix4f();
    private final List<RenderLocalLight> localLights = sandboxLocalLights();
    private final List<DebugPrimitive> debugPrimitives = sandboxDebugPrimitives();

    RenderFramePacket frame(SandboxCamera camera, SandboxFramebufferSize framebufferSize, long cumulativeTicks, long inputFrameId) {

        float aspectRatio = (float) framebufferSize.width() / framebufferSize.height();
        camera.view(view);
        CameraMatrices.perspective(VERTICAL_FOV_RADIANS, aspectRatio, NEAR_PLANE_METERS, FAR_PLANE_METERS, projection);
        DebugFrame debugFrame = new DebugFrame(debugPrimitives,
            List.of(new DebugTextCounter("simulation/tick", cumulativeTicks), new DebugTextCounter("input/frame", inputFrameId)));
        return new RenderFramePacket(view, projection, framebufferSize.width(), framebufferSize.height(), localLights, debugFrame);

    }

    static List<DebugPrimitive> sandboxDebugPrimitives() {

        return List.of(new DebugLine(-0.9f, 0.75f, 0.4f, 0.9f, 0.75f, 0.4f, new DebugColor(0.0f, 1.0f, 0.0f)),
            new DebugAabb(new Aabb3f(new Vector3f(-0.90f, -0.80f, 0.20f), new Vector3f(-0.50f, -0.40f, 0.60f)), new DebugColor(1.0f, 0.85f, 0.0f)),
            new DebugSphere(new Sphere3f(new Vector3f(0.0f, 0.60f, 0.30f), 0.18f), new DebugColor(0.0f, 0.85f, 1.0f)),
            new DebugRay(new Ray3f(new Vector3f(0.35f, -0.65f, 0.40f), new Vector3f(1.0f, 0.0f, 0.0f)), 0.60f, new DebugColor(1.0f, 0.0f, 1.0f)));

    }

    static List<RenderLocalLight> sandboxLocalLights() {

        RenderPointLight point = new RenderPointLight(0.40f, 0.30f, 1.20f, 1.0f, 0.45f, 0.20f, 0.35f, 4.0f);
        RenderSpotLight spot = new RenderSpotLight(-0.40f, 0.20f, 1.50f, 0.40f, -0.20f, -1.50f, 0.20f, 0.45f, 1.0f, 0.30f, 5.0f, 0.25f, 0.60f);
        return List.of(point, spot);

    }
}
