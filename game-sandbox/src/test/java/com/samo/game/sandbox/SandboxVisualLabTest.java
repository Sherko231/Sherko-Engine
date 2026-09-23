package com.samo.game.sandbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.samo.engine.assets.api.MaterialAsset;
import com.samo.engine.assets.api.ResourceHandleState;
import com.samo.engine.core.api.DebugFrame;
import com.samo.engine.core.api.DebugLine;
import com.samo.engine.core.api.DebugPrimitive;
import com.samo.engine.render.api.RenderLocalLight;
import com.samo.engine.render.api.RenderPointLight;
import com.samo.engine.render.api.RenderSpotLight;
import java.util.List;
import org.junit.jupiter.api.Test;

final class SandboxVisualLabTest {
    @Test
    void visualLabBuildsAStableBoundedScene() {

        SandboxAssetLabVisualState state = new SandboxAssetLabVisualState(ResourceHandleState.READY, ResourceHandleState.LOADING, SandboxAssetLabSignal.VALID_RELOAD);

        List<DebugPrimitive> first = SandboxVisualLab.primitives(state, 120L);
        List<DebugPrimitive> second = SandboxVisualLab.primitives(state, 120L);

        assertEquals(32, first.size());
        assertTrue(first.size() < DebugFrame.MAX_PRIMITIVES);
        assertEquals(first.size(), second.size());
        for (int index = 0; index < first.size(); index++) {
            assertEquals(first.get(index).getClass(), second.get(index).getClass());
        }

        DebugLine firstScanner = assertInstanceOf(DebugLine.class, first.getLast());
        DebugLine laterScanner = assertInstanceOf(DebugLine.class, SandboxVisualLab.primitives(state, 180L).getLast());
        assertNotEquals(firstScanner.startX(), laterScanner.startX());

    }

    @Test
    void beaconColorsAreDeterministicForLifecycleAndActions() {

        assertEquals(SandboxVisualLab.READY, SandboxVisualLab.stateColor(ResourceHandleState.READY));
        assertEquals(SandboxVisualLab.LOADING, SandboxVisualLab.stateColor(ResourceHandleState.LOADING));
        assertEquals(SandboxVisualLab.RELEASED, SandboxVisualLab.stateColor(ResourceHandleState.FAILED));
        assertEquals(SandboxVisualLab.RELEASED, SandboxVisualLab.stateColor(ResourceHandleState.RELEASED));

        assertEquals(SandboxVisualLab.VALID_RELOAD, SandboxVisualLab.signalColor(SandboxAssetLabSignal.VALID_RELOAD));
        assertEquals(SandboxVisualLab.FAILED_RELOAD, SandboxVisualLab.signalColor(SandboxAssetLabSignal.FAILED_RELOAD));
        assertEquals(SandboxVisualLab.MISSING_FALLBACK, SandboxVisualLab.signalColor(SandboxAssetLabSignal.MISSING_FALLBACK));
        assertEquals(SandboxVisualLab.HANDLE_RELOAD, SandboxVisualLab.signalColor(SandboxAssetLabSignal.HANDLE_RELOAD));

    }

    @Test
    void sceneUsesFourLightsAndMaterialStillDrivesPrimaryPair() {

        MaterialAsset material = new MaterialAsset("sandbox-lit", 0.2f, 0.4f, 0.8f, 1.0f);
        List<RenderLocalLight> lights = SandboxSceneSetup.sandboxLocalLights(material);

        assertEquals(4, lights.size());
        RenderPointLight materialPoint = assertInstanceOf(RenderPointLight.class, lights.get(0));
        RenderSpotLight materialSpot = assertInstanceOf(RenderSpotLight.class, lights.get(1));
        assertEquals(0.2f, materialPoint.red());
        assertEquals(0.4f, materialPoint.green());
        assertEquals(0.8f, materialPoint.blue());
        assertEquals(0.8f, materialSpot.red());
        assertEquals(0.2f, materialSpot.green());
        assertEquals(0.4f, materialSpot.blue());

    }
}
