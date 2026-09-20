package com.samo.game.sandbox;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.samo.engine.core.api.DebugTextCounter;
import com.samo.engine.core.api.PlayerInputCommand;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class SandboxDiagnosticsTest {
    @Test
    void accumulatesElapsedTimeWithSaturationAndKeepsDiagnosticCadence() {
        SandboxDiagnostics diagnostics = new SandboxDiagnostics();

        diagnostics.advanceElapsed(400_000_000L);
        diagnostics.advanceElapsed(600_000_000L);

        assertEquals(1_000_000_000L, diagnostics.elapsedSandboxNanos());
        assertEquals(1_000_000_000L, diagnostics.nextDiagnosticNanos());
        assertEquals(Long.MAX_VALUE, SandboxDiagnostics.saturatingAdd(Long.MAX_VALUE - 2L, 5L));
    }

    @Test
    void formatsCommandAndDebugCountersExactlyAsExistingDiagnosticPath() {
        assertEquals("tickCommand=none", SandboxDiagnostics.formatCommandDiagnostic(null));
        assertEquals(
                "tickCommand=42 MOVE=(-1.0,1.0) LOOK=(2.50,-3.75)",
                SandboxDiagnostics.formatCommandDiagnostic(command(42L, -1.0d, 1.0d, 2.5d, -3.75d)));
        assertEquals(
                "debugCounters=[simulation/tick=42,input/frame=456]",
                SandboxDiagnostics.formatDebugCounters(List.of(
                        new DebugTextCounter("simulation/tick", 42L),
                        new DebugTextCounter("input/frame", 456L))));
    }

    private static PlayerInputCommand command(
            long tick,
            double moveX,
            double moveY,
            double lookX,
            double lookY) {
        Map<PlayerInputCommand.DigitalAction, PlayerInputCommand.DigitalState> states =
                new EnumMap<>(PlayerInputCommand.DigitalAction.class);
        for (PlayerInputCommand.DigitalAction action : PlayerInputCommand.DigitalAction.values()) {
            states.put(action, new PlayerInputCommand.DigitalState(0.0d, false, false, false));
        }
        return new PlayerInputCommand(tick, moveX, moveY, lookX, lookY, states);
    }
}
