package com.samo.game.sandbox;

import com.samo.engine.assets.api.ResourceHandleState;
import com.samo.engine.core.api.Aabb3f;
import com.samo.engine.core.api.DebugAabb;
import com.samo.engine.core.api.DebugColor;
import com.samo.engine.core.api.DebugLine;
import com.samo.engine.core.api.DebugPrimitive;
import com.samo.engine.core.api.DebugSphere;
import com.samo.engine.core.api.Sphere3f;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.joml.Vector3f;

final class SandboxVisualLab {
    static final DebugColor READY = new DebugColor(0.10f, 1.00f, 0.45f);
    static final DebugColor LOADING = new DebugColor(1.00f, 0.78f, 0.08f);
    static final DebugColor RELEASED = new DebugColor(1.00f, 0.12f, 0.08f);
    static final DebugColor VALID_RELOAD = new DebugColor(0.10f, 0.55f, 1.00f);
    static final DebugColor FAILED_RELOAD = new DebugColor(1.00f, 0.05f, 0.75f);
    static final DebugColor MISSING_FALLBACK = new DebugColor(1.00f, 0.40f, 0.05f);
    static final DebugColor HANDLE_RELOAD = new DebugColor(0.70f, 0.95f, 1.00f);
    static final DebugColor IDLE = new DebugColor(0.45f, 0.45f, 0.50f);

    private static final DebugColor GRID_MINOR = new DebugColor(0.08f, 0.25f, 0.32f);
    private static final DebugColor GRID_MAJOR = new DebugColor(0.10f, 0.70f, 0.85f);
    private static final DebugColor FRAME = new DebugColor(0.20f, 0.55f, 1.00f);
    private static final DebugColor HOLOGRAM = new DebugColor(0.10f, 0.95f, 1.00f);

    private SandboxVisualLab() {

    }

    static List<DebugPrimitive> primitives(SandboxAssetLabVisualState state, long simulationTick) {

        Objects.requireNonNull(state, "state");
        List<DebugPrimitive> primitives = new ArrayList<>();
        addFloorGrid(primitives);
        addPortalFrames(primitives);
        addHologram(primitives, simulationTick);
        addStatusBeacons(primitives, state);
        addScanner(primitives, simulationTick);
        return List.copyOf(primitives);

    }

    static DebugColor stateColor(ResourceHandleState state) {

        return switch (Objects.requireNonNull(state, "state")) {
            case READY -> READY;
            case LOADING -> LOADING;
            case RELEASED -> RELEASED;
        };

    }

    static DebugColor signalColor(SandboxAssetLabSignal signal) {

        return switch (Objects.requireNonNull(signal, "signal")) {
            case NONE -> IDLE;
            case VALID_RELOAD -> VALID_RELOAD;
            case FAILED_RELOAD -> FAILED_RELOAD;
            case MISSING_FALLBACK -> MISSING_FALLBACK;
            case HANDLE_RELOAD -> HANDLE_RELOAD;
        };

    }

    private static void addFloorGrid(List<DebugPrimitive> primitives) {

        float floorY = -1.47f;
        for (int xIndex = -4; xIndex <= 4; xIndex++) {
            float x = xIndex * 0.40f;
            DebugColor color = xIndex == 0 ? GRID_MAJOR : GRID_MINOR;
            primitives.add(new DebugLine(x, floorY, 0.25f, x, floorY, -2.85f, color));
        }
        for (int zIndex = 0; zIndex <= 6; zIndex++) {
            float z = 0.15f - zIndex * 0.48f;
            DebugColor color = zIndex == 3 ? GRID_MAJOR : GRID_MINOR;
            primitives.add(new DebugLine(-1.80f, floorY, z, 1.80f, floorY, z, color));
        }

    }

    private static void addPortalFrames(List<DebugPrimitive> primitives) {

        primitives.add(aabb(-1.72f, -1.40f, -2.72f, -1.55f, 1.20f, -2.55f, FRAME));
        primitives.add(aabb(1.55f, -1.40f, -2.72f, 1.72f, 1.20f, -2.55f, FRAME));
        primitives.add(aabb(-1.72f, 1.10f, -2.72f, 1.72f, 1.28f, -2.55f, FRAME));
        primitives.add(aabb(-0.48f, -1.40f, -1.78f, 0.48f, -1.22f, -0.82f, GRID_MAJOR));

    }

    private static void addHologram(List<DebugPrimitive> primitives, long simulationTick) {

        float phase = simulationTick * 0.025f;
        float orbitX = (float) Math.cos(phase) * 0.52f;
        float orbitZ = -1.30f + (float) Math.sin(phase) * 0.30f;

        primitives.add(new DebugSphere(new Sphere3f(new Vector3f(0.0f, 0.15f, -1.30f), 0.34f), HOLOGRAM));
        primitives.add(aabb(-0.28f, -0.13f, -1.58f, 0.28f, 0.43f, -1.02f, FRAME));
        primitives.add(new DebugLine(-0.62f, 0.15f, -1.30f, 0.62f, 0.15f, -1.30f, HOLOGRAM));
        primitives.add(new DebugLine(0.0f, -0.46f, -1.30f, 0.0f, 0.76f, -1.30f, HOLOGRAM));
        primitives.add(new DebugSphere(new Sphere3f(new Vector3f(orbitX, 0.15f, orbitZ), 0.08f), VALID_RELOAD));

    }

    private static void addStatusBeacons(List<DebugPrimitive> primitives, SandboxAssetLabVisualState state) {

        addBeacon(primitives, -0.90f, stateColor(state.meshState()));
        addBeacon(primitives, 0.0f, stateColor(state.materialState()));
        addBeacon(primitives, 0.90f, signalColor(state.lastSignal()));

    }

    private static void addBeacon(List<DebugPrimitive> primitives, float x, DebugColor color) {

        primitives.add(new DebugLine(x, 0.62f, -2.32f, x, 0.90f, -2.32f, color));
        primitives.add(new DebugSphere(new Sphere3f(new Vector3f(x, 1.02f, -2.32f), 0.11f), color));

    }

    private static void addScanner(List<DebugPrimitive> primitives, long simulationTick) {

        float normalized = ((simulationTick % 240L) / 239.0f) * 2.0f - 1.0f;
        float scannerX = normalized * 1.55f;
        primitives.add(new DebugLine(scannerX, -1.44f, -2.45f, scannerX, 1.18f, -2.45f, FAILED_RELOAD));

    }

    private static DebugAabb aabb(float minX, float minY, float minZ, float maxX, float maxY, float maxZ, DebugColor color) {

        return new DebugAabb(new Aabb3f(new Vector3f(minX, minY, minZ), new Vector3f(maxX, maxY, maxZ)), color);

    }
}
