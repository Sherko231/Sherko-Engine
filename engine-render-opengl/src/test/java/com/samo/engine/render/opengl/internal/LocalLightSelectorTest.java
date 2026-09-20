package com.samo.engine.render.opengl.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.samo.engine.core.api.EngineLogger;
import com.samo.engine.render.api.RenderLocalLight;
import com.samo.engine.render.api.RenderPointLight;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class LocalLightSelectorTest {
    @Test
    void zeroAndExactlyMaxCountsDoNotWarn() {
        List<EngineLogger.Event> events = new ArrayList<>();
        LocalLightSelector selection =
                new LocalLightSelector(new EngineLogger(events::add), 2);

        assertEquals(List.of(), selection.select(List.of()));

        RenderLocalLight first = point(1.0f);
        RenderLocalLight second = point(2.0f);
        assertEquals(List.of(first, second), selection.select(List.of(first, second)));
        assertEquals(List.of(), events);
    }

    @Test
    void overMaxKeepsFirstLightsAndEmitsExactlyOneBoundedWarning() {
        List<EngineLogger.Event> events = new ArrayList<>();
        LocalLightSelector selection =
                new LocalLightSelector(new EngineLogger(events::add), 2);
        RenderLocalLight first = point(1.0f);
        RenderLocalLight second = point(2.0f);
        RenderLocalLight third = point(3.0f);

        List<RenderLocalLight> accepted = selection.select(List.of(first, second, third));

        assertEquals(2, accepted.size());
        assertSame(first, accepted.get(0));
        assertSame(second, accepted.get(1));
        assertEquals(1, events.size());
        EngineLogger.Event event = events.getFirst();
        assertEquals(EngineLogger.Level.WARN, event.level());
        assertEquals("renderer", event.context().subsystem());
        assertEquals(
                "Local light limit exceeded: submitted=3 accepted=2 dropped=1 configuredMax=2",
                event.message());
    }

    @Test
    void loggerFailurePropagatesInsteadOfReturningPartialSelection() {
        EngineLogger logger = new EngineLogger(event -> {
            throw new IllegalStateException("fixture logger failure");
        });
        LocalLightSelector selection = new LocalLightSelector(logger, 1);

        IllegalStateException failure = assertThrows(
                IllegalStateException.class,
                () -> selection.select(List.of(point(1.0f), point(2.0f))));

        assertEquals("fixture logger failure", failure.getMessage());
    }

    @Test
    void rejectsConfiguredMaximumOutsideFixedCapacity() {
        EngineLogger logger = new EngineLogger(event -> { });

        assertThrows(IllegalArgumentException.class, () -> new LocalLightSelector(logger, 0));
        assertThrows(
                IllegalArgumentException.class,
                () -> new LocalLightSelector(logger, LocalLightSelector.SHADER_CAPACITY + 1));
    }

    private static RenderPointLight point(float x) {
        return new RenderPointLight(x, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.5f, 10.0f);
    }
}
