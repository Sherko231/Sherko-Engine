package com.samo.game.sandbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

final class SandboxDiagnosticFormatterTest {
    @Test
    void rendersAllPeriodicDiagnosticValuesWithoutFormatPlaceholders() {
        String message = SandboxDiagnosticFormatter.format(new SandboxDiagnosticFormatter.DiagnosticValues(
                12.34d,
                0.625d,
                456L,
                true,
                false,
                "WINDOWED",
                1.0d,
                true,
                true,
                false,
                false,
                true,
                -1.0d,
                1.0d,
                true,
                false,
                true,
                false,
                true,
                false,
                "tickCommand=42 MOVE=(-1.0,1.0) LOOK=(2.50,-3.75)",
                4.25d,
                -5.5d,
                2,
                2,
                0,
                2,
                "debugCounters=[simulation/tick=42,input/frame=456]"));

        assertEquals(
                "sandboxTime=12.3s, interpolationAlpha=0.625, inputFrame=456, focused=true, cursorCaptured=false, "
                        + "mode=WINDOWED, sensitivity=1.00, invertY=true, WASD=[true,false,false,true], "
                        + "frameMOVE=(-1.0,1.0), JUMP[p=true,h=false,r=true], INTERACT[p=false,h=true,r=false], "
                        + "tickCommand=42 MOVE=(-1.0,1.0) LOOK=(2.50,-3.75), mouseDelta=(4.25,-5.50), "
                        + "renderCull[tested=1,visible=1,culled=0,draws=1], "
                        + "debugCounters=[simulation/tick=42,input/frame=456] "
                        + "(sandbox diagnostic; not FPS/benchmark/replay acceptance evidence)",
                message);
        assertFalse(message.contains("%"));
    }
}
