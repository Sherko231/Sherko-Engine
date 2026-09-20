package com.samo.game.sandbox;

import com.samo.engine.core.api.DebugTextCounter;
import com.samo.engine.core.api.EngineLogger;
import com.samo.engine.core.api.FixedStepAccumulator;
import com.samo.engine.core.api.PlayerInputCommand;
import com.samo.engine.platform.api.InputAction;
import com.samo.engine.platform.api.InputActionSnapshot;
import com.samo.engine.platform.api.InputActionState;
import com.samo.engine.platform.api.InputKey;
import com.samo.engine.platform.api.InputSnapshot;
import com.samo.engine.render.api.OpenGlRenderer;
import com.samo.engine.render.api.RenderCullingCounters;
import java.util.List;

final class SandboxDiagnostics {
    static final long DIAGNOSTIC_INTERVAL_NANOS = 1_000_000_000L;

    private long nextDiagnosticNanos = DIAGNOSTIC_INTERVAL_NANOS;
    private long elapsedSandboxNanos;
    private double mouseDeltaX;
    private double mouseDeltaY;

    void advanceElapsed(long elapsedNanos) {

        elapsedSandboxNanos = saturatingAdd(elapsedSandboxNanos, elapsedNanos);

    }

    void recordMouseDelta(InputSnapshot latestInput) {

        mouseDeltaX += latestInput.mouseDeltaX();
        mouseDeltaY += latestInput.mouseDeltaY();

    }

    boolean publishIfDue(FixedStepAccumulator accumulator, InputSnapshot latestInput, InputActionSnapshot latestActions, PlayerInputCommand latestCommand,
        SandboxControlState controlState, OpenGlRenderer renderer, EngineLogger logger, long cumulativeTicks) {

        if (elapsedSandboxNanos < nextDiagnosticNanos) {
            return false;
        }

        InputActionState move = latestActions.state(InputAction.MOVE);
        InputActionState jump = latestActions.state(InputAction.JUMP);
        InputActionState interact = latestActions.state(InputAction.INTERACT);
        String commandDiagnostic = formatCommandDiagnostic(latestCommand);
        RenderCullingCounters renderCounters = renderer.lastCullingCounters();
        String debugCounterDiagnostic = formatDebugCounters(renderer.lastDebugTextCounters());
        String diagnosticMessage = SandboxDiagnosticFormatter.format(new SandboxDiagnosticFormatter.DiagnosticValues(elapsedSandboxNanos / 1_000_000_000.0,
            accumulator.interpolationAlpha(), latestInput.frameId(), latestInput.focused(), latestInput.cursorCaptured(), controlState.currentWindowMode().name(),
            controlState.responseSettings().mouseSensitivity(), controlState.responseSettings().invertMouseY(), latestInput.keyHeld(InputKey.W), latestInput.keyHeld(InputKey.A),
            latestInput.keyHeld(InputKey.S), latestInput.keyHeld(InputKey.D), move.x(), move.y(), jump.pressed(), jump.held(), jump.released(), interact.pressed(), interact.held(),
            interact.released(), commandDiagnostic, mouseDeltaX, mouseDeltaY, renderCounters.testedCandidates(), renderCounters.visibleCandidates(),
            renderCounters.culledCandidates(), renderCounters.submittedDraws(), debugCounterDiagnostic));
        SandboxMain.log(logger, EngineLogger.Level.DEBUG, diagnosticMessage, cumulativeTicks);

        mouseDeltaX = 0.0d;
        mouseDeltaY = 0.0d;
        do {
            nextDiagnosticNanos += DIAGNOSTIC_INTERVAL_NANOS;
        } while (nextDiagnosticNanos <= elapsedSandboxNanos);
        return true;

    }

    long elapsedSandboxNanos() {

        return elapsedSandboxNanos;

    }

    long nextDiagnosticNanos() {

        return nextDiagnosticNanos;

    }

    double mouseDeltaX() {

        return mouseDeltaX;

    }

    double mouseDeltaY() {

        return mouseDeltaY;

    }

    static String formatCommandDiagnostic(PlayerInputCommand command) {

        return command == null
            ? "tickCommand=none"
            : "tickCommand=%d MOVE=(%.1f,%.1f) LOOK=(%.2f,%.2f)".formatted(command.tickId(), command.moveX(), command.moveY(), command.lookX(), command.lookY());

    }

    static String formatDebugCounters(List<DebugTextCounter> counters) {

        StringBuilder result = new StringBuilder("debugCounters=[");
        for (int index = 0; index < counters.size(); index++) {
            if (index > 0) {
                result.append(',');
            }
            DebugTextCounter counter = counters.get(index);
            result.append(counter.label()).append('=').append(counter.value());
        }
        return result.append(']').toString();

    }

    static long saturatingAdd(long left, long right) {

        if (Long.MAX_VALUE - left < right) {
            return Long.MAX_VALUE;
        }
        return left + right;

    }
}
