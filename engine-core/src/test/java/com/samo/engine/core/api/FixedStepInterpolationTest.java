package com.samo.engine.core.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FixedStepInterpolationTest {
    @Test
    void freshAccumulatorStartsAtZeroAlpha() {
        FixedStepAccumulator accumulator = new FixedStepAccumulator();

        assertEquals(0.0, accumulator.interpolationAlpha());
    }

    @Test
    void exactWholeTickBoundaryHasZeroAlpha() {
        FixedStepAccumulator accumulator = new FixedStepAccumulator();

        assertEquals(3L, accumulator.advance(50_000_000L));
        assertEquals(0.0, accumulator.interpolationAlpha());
    }

    @Test
    void exposesExactHalfTickFraction() {
        FixedStepAccumulator accumulator = new FixedStepAccumulator();

        assertEquals(1L, accumulator.advance(25_000_000L));
        assertEquals(0.5, accumulator.interpolationAlpha());
    }

    @Test
    void exposesExactThreeQuarterTickFraction() {
        FixedStepAccumulator accumulator = new FixedStepAccumulator();

        assertEquals(0L, accumulator.advance(12_500_000L));
        assertEquals(0.75, accumulator.interpolationAlpha());
    }

    @Test
    void nearNextTickAlphaRemainsBelowOne() {
        FixedStepAccumulator accumulator = new FixedStepAccumulator();

        assertEquals(0L, accumulator.advance(16_666_666L));

        double alpha = accumulator.interpolationAlpha();
        assertEquals(0.999_999_96, alpha);
        assertTrue(alpha < 1.0);
    }

    @Test
    void repeatedQueriesDoNotConsumeProgress() {
        FixedStepAccumulator accumulator = new FixedStepAccumulator();
        accumulator.advance(25_000_000L);

        assertEquals(0.5, accumulator.interpolationAlpha());
        assertEquals(0.5, accumulator.interpolationAlpha());
        assertEquals(0.5, accumulator.interpolationAlpha());
    }

    @Test
    void zeroElapsedPreservesAlpha() {
        FixedStepAccumulator accumulator = new FixedStepAccumulator();
        accumulator.advance(25_000_000L);

        assertEquals(0L, accumulator.advance(0L));
        assertEquals(0.5, accumulator.interpolationAlpha());
    }

    @Test
    void rejectedNegativeElapsedPreservesAlpha() {
        FixedStepAccumulator accumulator = new FixedStepAccumulator();
        accumulator.advance(25_000_000L);

        assertThrows(IllegalArgumentException.class, () -> accumulator.advance(-1L));
        assertEquals(0.5, accumulator.interpolationAlpha());
    }

    @Test
    void cappedCatchUpDropsWholeStepsButRetainsFractionalAlpha() {
        FixedStepAccumulator accumulator = new FixedStepAccumulator();
        FixedStepCatchUpPolicy policy = new FixedStepCatchUpPolicy();

        assertEquals(5L, policy.advance(accumulator, 125_000_000L));
        assertEquals(0.5, accumulator.interpolationAlpha());
        assertEquals(0L, policy.advance(accumulator, 0L));
        assertEquals(0.5, accumulator.interpolationAlpha());
    }

    @Test
    void frameGapClampDiscardsElapsedBeyondAcceptedDuration() {
        FixedStepAccumulator accumulator = new FixedStepAccumulator();
        FixedStepCatchUpPolicy policy = new FixedStepCatchUpPolicy(25_000_000L, 10);

        assertEquals(1L, policy.advance(accumulator, 2_000_000_000L));
        assertEquals(0.5, accumulator.interpolationAlpha());
        assertEquals(0L, policy.advance(accumulator, 0L));
        assertEquals(0.5, accumulator.interpolationAlpha());
    }

    @Test
    void callerKeepsSimulationTicksSeparateFromRenderAlpha() {
        FixedStepAccumulator accumulator = new FixedStepAccumulator();
        long simulationTicks = 0L;

        simulationTicks += accumulator.advance(10_000_000L);
        assertEquals(0L, simulationTicks);
        assertEquals(0.6, accumulator.interpolationAlpha());

        simulationTicks += accumulator.advance(10_000_000L);
        assertEquals(1L, simulationTicks);
        assertEquals(0.2, accumulator.interpolationAlpha());
    }
}
