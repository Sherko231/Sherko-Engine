package com.samo.engine.core.api;

/**
 * Converts elapsed nanoseconds into whole fixed-rate simulation ticks.
 *
 * <p>The accumulator retains exact fractional progress between calls. The caller owns execution of
 * the returned ticks and externally serializes calls.
 */
public final class FixedStepAccumulator {
    /** Fixed simulation rate from the engine scope. */
    public static final int TICKS_PER_SECOND = 60;

    private static final long NANOS_PER_SECOND = 1_000_000_000L;

    private long scaledRemainder;

    /** Creates an empty 60 Hz fixed-step accumulator. */
    public FixedStepAccumulator() {
    }

    /**
     * Adds one non-negative elapsed duration and returns newly due whole simulation ticks.
     *
     * <p>Fractional progress is represented as tick-nanosecond units over one billion, preserving
     * the exact rational rate of 60 ticks per second without floating-point arithmetic or rounding
     * one simulation step to an integer nanosecond duration.
     *
     * @param elapsedNanos elapsed monotonic time to accumulate
     * @return newly due whole simulation ticks
     * @throws IllegalArgumentException if {@code elapsedNanos} is negative
     */
    public long advance(long elapsedNanos) {
        if (elapsedNanos < 0L) {
            throw new IllegalArgumentException("elapsedNanos must be non-negative");
        }

        long wholeSeconds = elapsedNanos / NANOS_PER_SECOND;
        long nanosRemainder = elapsedNanos % NANOS_PER_SECOND;

        long ticks = wholeSeconds * TICKS_PER_SECOND;
        long scaledProgress = nanosRemainder * TICKS_PER_SECOND + scaledRemainder;

        ticks += scaledProgress / NANOS_PER_SECOND;
        scaledRemainder = scaledProgress % NANOS_PER_SECOND;
        return ticks;
    }

    /**
     * Returns renderer-facing fractional progress toward the next fixed simulation tick.
     *
     * <p>This is a read-only normalized presentation value in {@code [0.0, 1.0)}. Fixed-step
     * accumulation itself remains exact integer/rational arithmetic; querying alpha neither mutates
     * nor consumes retained progress.
     *
     * @return retained fractional progress normalized to {@code [0.0, 1.0)}
     */
    public double interpolationAlpha() {
        return scaledRemainder / (double) NANOS_PER_SECOND;
    }
}
