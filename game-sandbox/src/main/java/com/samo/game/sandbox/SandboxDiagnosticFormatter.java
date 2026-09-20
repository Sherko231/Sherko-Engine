package com.samo.game.sandbox;

import java.util.Locale;
import java.util.Objects;

/** Pure formatter for the owner-facing periodic sandbox diagnostic. */
final class SandboxDiagnosticFormatter {
    private SandboxDiagnosticFormatter() {
    }

    static String format(DiagnosticValues values) {
        Objects.requireNonNull(values, "values");
        return String.format(Locale.ROOT, "sandboxTime=%.1fs, interpolationAlpha=%.3f, inputFrame=%d, focused=%s, cursorCaptured=%s, "
            + "mode=%s, sensitivity=%.2f, invertY=%s, WASD=[%s,%s,%s,%s], frameMOVE=(%.1f,%.1f), " + "JUMP[p=%s,h=%s,r=%s], INTERACT[p=%s,h=%s,r=%s], %s, mouseDelta=(%.2f,%.2f), "
            + "renderCull[tested=%d,visible=%d,culled=%d,draws=%d], %s " + "(sandbox diagnostic; not FPS/benchmark/replay acceptance evidence)", values.sandboxTimeSeconds(),
            values.interpolationAlpha(), values.inputFrame(), values.focused(), values.cursorCaptured(), values.mode(), values.sensitivity(), values.invertY(), values.wHeld(),
            values.aHeld(), values.sHeld(), values.dHeld(), values.moveX(), values.moveY(), values.jumpPressed(), values.jumpHeld(), values.jumpReleased(),
            values.interactPressed(), values.interactHeld(), values.interactReleased(), values.commandDiagnostic(), values.mouseDeltaX(), values.mouseDeltaY(),
            values.testedCandidates(), values.visibleCandidates(), values.culledCandidates(), values.submittedDraws(), values.debugCounters());
    }

    record DiagnosticValues(double sandboxTimeSeconds, double interpolationAlpha, long inputFrame, boolean focused, boolean cursorCaptured, String mode, double sensitivity,
        boolean invertY, boolean wHeld, boolean aHeld, boolean sHeld, boolean dHeld, double moveX, double moveY, boolean jumpPressed, boolean jumpHeld, boolean jumpReleased,
        boolean interactPressed, boolean interactHeld, boolean interactReleased, String commandDiagnostic, double mouseDeltaX, double mouseDeltaY, int testedCandidates,
        int visibleCandidates, int culledCandidates, int submittedDraws, String debugCounters) {
        DiagnosticValues {
            Objects.requireNonNull(mode, "mode");
            Objects.requireNonNull(commandDiagnostic, "commandDiagnostic");
            Objects.requireNonNull(debugCounters, "debugCounters");
            if (testedCandidates < 0 || visibleCandidates < 0 || culledCandidates < 0 || submittedDraws < 0) {
                throw new IllegalArgumentException("render counters must be non-negative");
            }
        }
    }
}
