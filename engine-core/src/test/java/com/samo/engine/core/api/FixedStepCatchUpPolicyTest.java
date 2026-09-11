package com.samo.engine.core.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class FixedStepCatchUpPolicyTest {
    @Test
    void exposesDefaultBounds() {
        assertEquals(250_000_000L, FixedStepCatchUpPolicy.DEFAULT_MAX_FRAME_GAP_NANOS);
        assertEquals(5, FixedStepCatchUpPolicy.DEFAULT_MAX_STEPS_PER_UPDATE);
    }

    @Test
    void clampsOnlyAboveTheFrameGapBoundary() {
        FixedStepCatchUpPolicy policy = new FixedStepCatchUpPolicy(100_000_000L, 100);

        assertEquals(5L, policy.advance(new FixedStepAccumulator(), 99_999_999L));
        assertEquals(6L, policy.advance(new FixedStepAccumulator(), 100_000_000L));
        assertEquals(6L, policy.advance(new FixedStepAccumulator(), 100_000_001L));
    }

    @Test
    void capsOnlyAboveTheStepBoundary() {
        FixedStepCatchUpPolicy policy = new FixedStepCatchUpPolicy(1_000_000_000L, 5);

        assertEquals(5L, policy.advance(new FixedStepAccumulator(), 83_333_334L));
        assertEquals(5L, policy.advance(new FixedStepAccumulator(), 100_000_000L));
    }

    @Test
    void defaultPolicyBoundsTwoSecondStallToFiveSteps() {
        FixedStepCatchUpPolicy policy = new FixedStepCatchUpPolicy();

        assertEquals(5L, policy.advance(new FixedStepAccumulator(), 2_000_000_000L));
    }

    @Test
    void discardedWholeStepsDoNotBecomeFutureBacklog() {
        FixedStepCatchUpPolicy policy = new FixedStepCatchUpPolicy(1_000_000_000L, 5);
        FixedStepAccumulator accumulator = new FixedStepAccumulator();

        assertEquals(5L, policy.advance(accumulator, 100_000_000L));
        assertEquals(0L, policy.advance(accumulator, 0L));
        assertEquals(1L, policy.advance(accumulator, 16_666_667L));
    }

    @Test
    void elapsedBeyondFrameClampIsDiscarded() {
        FixedStepCatchUpPolicy policy = new FixedStepCatchUpPolicy();
        FixedStepAccumulator accumulator = new FixedStepAccumulator();

        assertEquals(5L, policy.advance(accumulator, 2_000_000_000L));
        assertEquals(0L, policy.advance(accumulator, 0L));
        assertEquals(1L, policy.advance(accumulator, 16_666_667L));
    }

    @Test
    void preservesFractionalProgressAcrossCappedCall() {
        FixedStepCatchUpPolicy policy = new FixedStepCatchUpPolicy(1_000_000_000L, 5);
        FixedStepAccumulator accumulator = new FixedStepAccumulator();

        assertEquals(5L, policy.advance(accumulator, 100_000_001L));
        assertEquals(0L, policy.advance(accumulator, 16_666_665L));
        assertEquals(1L, policy.advance(accumulator, 1L));
    }

    @Test
    void zeroElapsedPreservesFractionalProgress() {
        FixedStepCatchUpPolicy policy = new FixedStepCatchUpPolicy();
        FixedStepAccumulator accumulator = new FixedStepAccumulator();

        assertEquals(0L, policy.advance(accumulator, 16_666_666L));
        assertEquals(0L, policy.advance(accumulator, 0L));
        assertEquals(1L, policy.advance(accumulator, 1L));
    }

    @Test
    void negativeElapsedIsRejectedWithoutMutatingAccumulator() {
        FixedStepCatchUpPolicy policy = new FixedStepCatchUpPolicy();
        FixedStepAccumulator accumulator = new FixedStepAccumulator();

        assertEquals(0L, policy.advance(accumulator, 16_000_000L));
        assertThrows(IllegalArgumentException.class, () -> policy.advance(accumulator, -1L));
        assertEquals(1L, policy.advance(accumulator, 666_667L));
    }

    @Test
    void nullAccumulatorIsRejected() {
        FixedStepCatchUpPolicy policy = new FixedStepCatchUpPolicy();

        assertThrows(NullPointerException.class, () -> policy.advance(null, 0L));
    }

    @Test
    void explicitConstructorRejectsNonPositiveLimits() {
        assertThrows(IllegalArgumentException.class, () -> new FixedStepCatchUpPolicy(0L, 1));
        assertThrows(IllegalArgumentException.class, () -> new FixedStepCatchUpPolicy(-1L, 1));
        assertThrows(IllegalArgumentException.class, () -> new FixedStepCatchUpPolicy(1L, 0));
        assertThrows(IllegalArgumentException.class, () -> new FixedStepCatchUpPolicy(1L, -1));
    }
}
