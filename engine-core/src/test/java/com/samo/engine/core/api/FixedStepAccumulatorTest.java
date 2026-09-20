package com.samo.engine.core.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigInteger;
import org.junit.jupiter.api.Test;

class FixedStepAccumulatorTest {
    private static final long NANOS_PER_SECOND = 1_000_000_000L;

    @Test
    void producesEqualSimulationProgressAcrossRenderCadences() {
        assertEquals(60L, runEvenPartition(NANOS_PER_SECOND, 30));
        assertEquals(60L, runEvenPartition(NANOS_PER_SECOND, 60));
        assertEquals(60L, runEvenPartition(NANOS_PER_SECOND, 144));
        assertEquals(60L, runIrregularPartition(NANOS_PER_SECOND));
    }

    @Test
    void equivalentLongerTotalsProduceEqualTickCounts() {
        long totalNanos = 10L * NANOS_PER_SECOND;

        assertEquals(600L, runEvenPartition(totalNanos, 300));
        assertEquals(600L, runEvenPartition(totalNanos, 1_440));
        assertEquals(600L, runIrregularPartition(totalNanos));
    }

    @Test
    void retainsSubTickDurationsUntilAWholeTickIsDue() {
        FixedStepAccumulator accumulator = new FixedStepAccumulator();

        assertEquals(0L, accumulator.advance(8_000_000L));
        assertEquals(0L, accumulator.advance(8_000_000L));
        assertEquals(1L, accumulator.advance(666_667L));
    }

    @Test
    void preservesFractionalRemainderAcrossTickBoundary() {
        FixedStepAccumulator accumulator = new FixedStepAccumulator();

        assertEquals(1L, accumulator.advance(16_666_667L));
        assertEquals(0L, accumulator.advance(16_666_666L));
        assertEquals(1L, accumulator.advance(1L));
    }

    @Test
    void zeroInputPreservesFractionalProgress() {
        FixedStepAccumulator accumulator = new FixedStepAccumulator();

        assertEquals(0L, accumulator.advance(16_666_666L));
        assertEquals(0L, accumulator.advance(0L));
        assertEquals(1L, accumulator.advance(1L));
    }

    @Test
    void negativeInputIsRejectedWithoutMutatingState() {
        FixedStepAccumulator accumulator = new FixedStepAccumulator();

        assertEquals(0L, accumulator.advance(16_000_000L));
        assertThrows(IllegalArgumentException.class, () -> accumulator.advance(-1L));
        assertEquals(1L, accumulator.advance(666_667L));
    }

    @Test
    void acceptsLongMaxValueWithoutOverflow() {
        FixedStepAccumulator accumulator = new FixedStepAccumulator();
        BigInteger expected = BigInteger.valueOf(Long.MAX_VALUE).multiply(BigInteger.valueOf(FixedStepAccumulator.TICKS_PER_SECOND)).divide(BigInteger.valueOf(NANOS_PER_SECOND));

        assertEquals(expected.longValueExact(), accumulator.advance(Long.MAX_VALUE));
    }

    private static long runEvenPartition(long totalNanos, int frameCount) {
        FixedStepAccumulator accumulator = new FixedStepAccumulator();
        long base = totalNanos / frameCount;
        long extra = totalNanos % frameCount;
        long ticks = 0L;

        for (int frame = 0; frame < frameCount; frame++) {
            long elapsed = base + (frame < extra ? 1L : 0L);
            ticks += accumulator.advance(elapsed);
        }
        return ticks;
    }

    private static long runIrregularPartition(long totalNanos) {
        long[] prefix = {1L, 10_000_000L, 123_456_789L, 250_000_000L, 333_333_333L};
        long prefixTotal = 0L;
        for (long elapsed : prefix) {
            prefixTotal += elapsed;
        }

        FixedStepAccumulator accumulator = new FixedStepAccumulator();
        long ticks = 0L;
        for (long elapsed : prefix) {
            ticks += accumulator.advance(elapsed);
        }
        ticks += accumulator.advance(totalNanos - prefixTotal);
        return ticks;
    }
}
