package com.samo.engine.render.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class RenderCullingCountersTest {
    @Test
    void acceptsConsistentCounts() {

        RenderCullingCounters counters = new RenderCullingCounters(2, 1, 1, 1);

        assertEquals(2, counters.testedCandidates());
        assertEquals(1, counters.visibleCandidates());
        assertEquals(1, counters.culledCandidates());
        assertEquals(1, counters.submittedDraws());

    }

    @Test
    void rejectsNegativeOrInconsistentCounts() {

        assertThrows(IllegalArgumentException.class, () -> new RenderCullingCounters(-1, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new RenderCullingCounters(2, 1, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> new RenderCullingCounters(1, 1, 0, 2));

    }
}
