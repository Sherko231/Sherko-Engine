package com.samo.engine.render.opengl.internal;

import com.samo.engine.core.api.DebugFrame;
import com.samo.engine.core.api.DebugTextCounter;
import com.samo.engine.render.api.RenderCullingCounters;
import java.util.List;
import java.util.Objects;

final class RendererFrameDiagnostics {
    private RenderCullingCounters lastCullingCounters = RenderCullingCounters.EMPTY;
    private List<DebugTextCounter> lastDebugTextCounters = List.of();

    void publish(
            ReferenceSceneVisibilityPlanner.VisibilityPlan visibilityPlan,
            int submittedDraws,
            DebugFrame debugFrame) {
        ReferenceSceneVisibilityPlanner.VisibilityPlan plan =
                Objects.requireNonNull(visibilityPlan, "visibilityPlan");
        DebugFrame diagnostics = Objects.requireNonNull(debugFrame, "debugFrame");
        lastCullingCounters = new RenderCullingCounters(
                plan.testedCandidates(),
                plan.visibleCandidates(),
                plan.culledCandidates(),
                submittedDraws);
        lastDebugTextCounters = diagnostics.textCounters();
    }

    RenderCullingCounters lastCullingCounters() {
        return lastCullingCounters;
    }

    List<DebugTextCounter> lastDebugTextCounters() {
        return lastDebugTextCounters;
    }
}
