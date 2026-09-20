package com.samo.engine.render.opengl.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.samo.engine.core.api.DebugFrame;
import com.samo.engine.core.api.DebugTextCounter;
import com.samo.engine.render.api.RenderCullingCounters;
import java.util.List;
import org.junit.jupiter.api.Test;

class RendererFrameDiagnosticsTest {
    @Test
    void startsEmptyAndPublishesOnlyTheCompletedFrameSnapshot() {

        RendererFrameDiagnostics diagnostics = new RendererFrameDiagnostics();

        assertEquals(RenderCullingCounters.EMPTY, diagnostics.lastCullingCounters());
        assertEquals(List.of(), diagnostics.lastDebugTextCounters());

        DebugTextCounter counter = new DebugTextCounter("tick", 42L);
        ReferenceSceneVisibilityPlanner.VisibilityPlan visibilityPlan = new ReferenceSceneVisibilityPlanner.VisibilityPlan(List.of(), 1, 0, 1);

        diagnostics.publish(visibilityPlan, 0, new DebugFrame(List.of(), List.of(counter)));

        assertEquals(new RenderCullingCounters(1, 0, 1, 0), diagnostics.lastCullingCounters());
        assertEquals(List.of(counter), diagnostics.lastDebugTextCounters());

    }
}
