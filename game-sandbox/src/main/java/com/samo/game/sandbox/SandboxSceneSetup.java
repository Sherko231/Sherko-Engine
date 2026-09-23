package com.samo.game.sandbox;

import com.samo.engine.assets.api.MaterialAsset;
import com.samo.engine.core.api.CameraMatrices;
import com.samo.engine.core.api.DebugFrame;
import com.samo.engine.core.api.DebugPrimitive;
import com.samo.engine.core.api.DebugTextCounter;
import com.samo.engine.render.api.RenderFramePacket;
import com.samo.engine.render.api.RenderLocalLight;
import com.samo.engine.render.api.RenderPointLight;
import com.samo.engine.render.api.RenderSpotLight;
import java.util.List;
import org.joml.Matrix4f;

final class SandboxSceneSetup {
    static final float VERTICAL_FOV_RADIANS = (float) Math.toRadians(70.0);
    static final float NEAR_PLANE_METERS = 0.1f;
    static final float FAR_PLANE_METERS = 100.0f;

    private final Matrix4f view = new Matrix4f();
    private final Matrix4f projection = new Matrix4f();

    RenderFramePacket frame(SandboxCamera camera, SandboxFramebufferSize framebufferSize, long cumulativeTicks, long inputFrameId, MaterialAsset material,
        SandboxAssetLabVisualState assetState) {

        float aspectRatio = (float) framebufferSize.width() / framebufferSize.height();
        camera.view(view);
        CameraMatrices.perspective(VERTICAL_FOV_RADIANS, aspectRatio, NEAR_PLANE_METERS, FAR_PLANE_METERS, projection);
        List<DebugPrimitive> visualLab = SandboxVisualLab.primitives(assetState, cumulativeTicks);
        DebugFrame debugFrame = new DebugFrame(visualLab,
            List.of(new DebugTextCounter("simulation/tick", cumulativeTicks), new DebugTextCounter("input/frame", inputFrameId)));
        return new RenderFramePacket(view, projection, framebufferSize.width(), framebufferSize.height(), sandboxLocalLights(material), debugFrame);

    }

    static List<RenderLocalLight> sandboxLocalLights(MaterialAsset material) {

        SandboxLightPalette palette = SandboxLightPalette.from(material);
        RenderPointLight materialPoint = new RenderPointLight(-1.05f, 0.55f, -1.35f, palette.pointRed(), palette.pointGreen(), palette.pointBlue(), 0.46f, 3.8f);
        RenderSpotLight materialSpot = new RenderSpotLight(1.10f, 0.75f, -0.75f, -1.10f, -0.55f, -0.55f, palette.spotRed(), palette.spotGreen(), palette.spotBlue(), 0.40f,
            4.5f, 0.28f, 0.70f);
        RenderPointLight cyanAccent = new RenderPointLight(0.0f, -0.65f, -1.25f, 0.10f, 0.70f, 1.00f, 0.24f, 2.6f);
        RenderPointLight magentaAccent = new RenderPointLight(0.0f, 1.10f, -2.35f, 1.00f, 0.08f, 0.55f, 0.18f, 2.8f);
        return List.of(materialPoint, materialSpot, cyanAccent, magentaAccent);

    }
}
