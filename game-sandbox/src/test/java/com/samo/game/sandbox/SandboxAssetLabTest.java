package com.samo.game.sandbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.samo.engine.assets.api.MaterialAsset;
import com.samo.engine.assets.api.ResourceHandleState;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

final class SandboxAssetLabTest {
    @Test
    void fixtureLoadsThroughPublicRuntimeBoundaryAndSupportsOwnerDemonstrations() {

        List<String> logs = new ArrayList<>();
        try (SandboxAssetLab lab = SandboxAssetLab.open(logs::add)) {
            awaitReady(lab);
            assertTrue(Files.isRegularFile(lab.cacheRoot().resolve("manifest.json")));
            assertEquals(ResourceHandleState.READY, lab.meshState());
            assertEquals(ResourceHandleState.READY, lab.materialState());
            assertTrue(lab.readySummary().contains("SandboxAssetTriangle"));
            assertTrue(lab.readySummary().contains("vertices=3"));
            assertTrue(lab.readySummary().contains("indices=3"));

            MaterialAsset warm = lab.currentMaterial();
            lab.cycleValidMaterial();
            MaterialAsset cool = lab.currentMaterial();
            assertNotEquals(warm, cool);
            assertEquals(0.15f, cool.redMultiplier());
            assertEquals(0.45f, cool.greenMultiplier());
            assertEquals(1.0f, cool.blueMultiplier());

            lab.demonstrateFailedReload();
            assertEquals(cool, lab.currentMaterial());
            long firstFailureCount = logs.stream().filter(line -> line.contains("HOT_RELOAD_FAILED") && line.contains("preserved=true")).count();
            assertEquals(1L, firstFailureCount);

            lab.demonstrateFailedReload();
            assertEquals(cool, lab.currentMaterial());
            long repeatedFailureCount = logs.stream().filter(line -> line.contains("HOT_RELOAD_FAILED") && line.contains("preserved=true")).count();
            assertEquals(2L, repeatedFailureCount);

            lab.demonstrateMissingMeshFallback();
            assertTrue(logs.stream().anyMatch(line -> line.contains("missing MESH") && line.contains("READY")));
            assertTrue(logs.stream().anyMatch(line -> line.contains("MISSING_CONTENT")));

            lab.reloadHandles();
            assertTrue(logs.stream().anyMatch(line -> line.contains("handles released") && line.contains("RELEASED")));
            awaitReady(lab);
            assertEquals(ResourceHandleState.READY, lab.meshState());
            assertEquals(ResourceHandleState.READY, lab.materialState());
        }

    }

    @Test
    void materialPaletteMapsDeterministicallyToPublicLocalLights() {

        MaterialAsset material = new MaterialAsset("sandbox-lit", 0.2f, 0.4f, 0.8f, 1.0f);
        SandboxLightPalette palette = SandboxLightPalette.from(material);

        assertEquals(0.2f, palette.pointRed());
        assertEquals(0.4f, palette.pointGreen());
        assertEquals(0.8f, palette.pointBlue());
        assertEquals(0.8f, palette.spotRed());
        assertEquals(0.2f, palette.spotGreen());
        assertEquals(0.4f, palette.spotBlue());

    }

    private static void awaitReady(SandboxAssetLab lab) {

        long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
        while ((lab.meshState() == ResourceHandleState.LOADING || lab.materialState() == ResourceHandleState.LOADING) && System.nanoTime() < deadline) {
            Thread.onSpinWait();
        }
        assertFalse(lab.meshState() == ResourceHandleState.LOADING);
        assertFalse(lab.materialState() == ResourceHandleState.LOADING);

    }
}
