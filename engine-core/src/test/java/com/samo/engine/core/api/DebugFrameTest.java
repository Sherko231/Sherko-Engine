package com.samo.engine.core.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class DebugFrameTest {
    @Test
    void emptyFrameCarriesNoDiagnostics() {

        assertEquals(List.of(), DebugFrame.EMPTY.primitives());
        assertEquals(List.of(), DebugFrame.EMPTY.textCounters());

    }

    @Test
    void snapshotsOrderedPrimitivesAndCountersAtExactMaximum() {

        DebugLine line = new DebugLine(new Vector3f(0.0f, 0.0f, 0.0f), new Vector3f(1.0f, 0.0f, 0.0f), new DebugColor(1.0f, 0.0f, 0.0f));
        List<DebugPrimitive> primitives = new ArrayList<>(Collections.nCopies(DebugFrame.MAX_PRIMITIVES, line));
        DebugTextCounter counter = new DebugTextCounter("render/draws", 2L);
        List<DebugTextCounter> counters = new ArrayList<>(Collections.nCopies(DebugFrame.MAX_TEXT_COUNTERS, counter));

        DebugFrame frame = new DebugFrame(primitives, counters);
        primitives.clear();
        counters.clear();

        assertEquals(DebugFrame.MAX_PRIMITIVES, frame.primitives().size());
        assertEquals(DebugFrame.MAX_TEXT_COUNTERS, frame.textCounters().size());
        assertSame(line, frame.primitives().getFirst());
        assertSame(counter, frame.textCounters().getFirst());
        assertThrows(UnsupportedOperationException.class, () -> frame.primitives().add(line));
        assertThrows(UnsupportedOperationException.class, () -> frame.textCounters().add(counter));

    }

    @Test
    void rejectsOverCapacityNullsAndInvalidValues() {

        DebugLine line = new DebugLine(0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, new DebugColor(0.0f, 1.0f, 0.0f));
        DebugTextCounter counter = new DebugTextCounter("tick", 1L);

        assertThrows(IllegalArgumentException.class, () -> new DebugFrame(Collections.nCopies(DebugFrame.MAX_PRIMITIVES + 1, line), List.of()));
        assertThrows(IllegalArgumentException.class, () -> new DebugFrame(List.of(), Collections.nCopies(DebugFrame.MAX_TEXT_COUNTERS + 1, counter)));
        assertThrows(NullPointerException.class, () -> new DebugFrame(java.util.Arrays.asList((DebugPrimitive) null), List.of()));
        assertThrows(NullPointerException.class, () -> new DebugFrame(List.of(), java.util.Arrays.asList((DebugTextCounter) null)));
        assertThrows(IllegalArgumentException.class, () -> new DebugColor(Float.NaN, 0.0f, 0.0f));
        assertThrows(IllegalArgumentException.class, () -> new DebugLine(Float.NaN, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, new DebugColor(1.0f, 1.0f, 1.0f)));
        assertThrows(IllegalArgumentException.class, () -> new DebugRay(new Ray3f(new Vector3f(), new Vector3f(0.0f, 0.0f, -1.0f)), 0.0f, new DebugColor(1.0f, 1.0f, 1.0f)));

    }

    @Test
    void validatesBoundedAsciiCounterLabels() {

        assertEquals("net/rtt_ms", new DebugTextCounter("net/rtt_ms", -1L).label());
        assertEquals(7L, new DebugTextCounter("tick:server", 7L).value());

        assertThrows(IllegalArgumentException.class, () -> new DebugTextCounter("", 0L));
        assertThrows(IllegalArgumentException.class, () -> new DebugTextCounter("x".repeat(DebugTextCounter.MAX_LABEL_LENGTH + 1), 0L));
        assertThrows(IllegalArgumentException.class, () -> new DebugTextCounter("not allowed", 0L));
        assertThrows(IllegalArgumentException.class, () -> new DebugTextCounter("é", 0L));

    }

    @Test
    void wrapsAcceptedSpatialPrimitivesWithoutChangingTheirSemantics() {

        DebugColor color = new DebugColor(0.25f, 0.5f, 0.75f);
        Aabb3f aabb = new Aabb3f(new Vector3f(-1.0f, -2.0f, -3.0f), new Vector3f(1.0f, 2.0f, 3.0f));
        Sphere3f sphere = new Sphere3f(new Vector3f(1.0f, 2.0f, 3.0f), 2.0f);
        Ray3f ray = new Ray3f(new Vector3f(1.0f, 2.0f, 3.0f), new Vector3f(0.0f, 0.0f, -2.0f));

        assertSame(aabb, new DebugAabb(aabb, color).bounds());
        assertSame(sphere, new DebugSphere(sphere, color).sphere());
        DebugRay debugRay = new DebugRay(ray, 5.0f, color);
        assertSame(ray, debugRay.ray());
        assertEquals(5.0f, debugRay.lengthMeters());

    }
}
