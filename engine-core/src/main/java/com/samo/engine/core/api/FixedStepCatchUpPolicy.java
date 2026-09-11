package com.samo.engine.core.api;

import java.util.Objects;

/**
 * Bounds elapsed-time spikes and the number of fixed simulation steps exposed by one update.
 *
 * <p>The policy delegates exact 60 Hz accumulation to {@link FixedStepAccumulator}. Elapsed time
 * above the configured frame-gap bound and whole steps above the configured per-update cap are
 * deliberately discarded so callers cannot inherit an unbounded catch-up backlog.
 */
public final class FixedStepCatchUpPolicy {
    /** Default maximum elapsed duration accepted from one update: 250 ms. */
    public static final long DEFAULT_MAX_FRAME_GAP_NANOS = 250_000_000L;

    /** Default maximum number of simulation steps exposed by one update. */
    public static final int DEFAULT_MAX_STEPS_PER_UPDATE = 5;

    private final long maxFrameGapNanos;
    private final int maxStepsPerUpdate;

    /** Creates the default 250 ms / 5-step catch-up policy. */
    public FixedStepCatchUpPolicy() {
        this(DEFAULT_MAX_FRAME_GAP_NANOS, DEFAULT_MAX_STEPS_PER_UPDATE);
    }

    /**
     * Creates a catch-up policy with explicit positive bounds.
     *
     * @param maxFrameGapNanos maximum elapsed duration accepted from one update
     * @param maxStepsPerUpdate maximum whole simulation steps returned from one update
     * @throws IllegalArgumentException if either limit is not strictly positive
     */
    public FixedStepCatchUpPolicy(long maxFrameGapNanos, int maxStepsPerUpdate) {
        if (maxFrameGapNanos <= 0L) {
            throw new IllegalArgumentException("maxFrameGapNanos must be positive");
        }
        if (maxStepsPerUpdate <= 0) {
            throw new IllegalArgumentException("maxStepsPerUpdate must be positive");
        }
        this.maxFrameGapNanos = maxFrameGapNanos;
        this.maxStepsPerUpdate = maxStepsPerUpdate;
    }

    /**
     * Applies frame-gap clamping and the per-update catch-up cap to one elapsed duration.
     *
     * <p>The supplied accumulator is advanced exactly once with the clamped elapsed duration. Any
     * whole steps above the configured cap are discarded rather than retained as later backlog.
     * Fractional progress retained by the accumulator remains untouched.
     *
     * @param accumulator fixed-step accumulator that owns fractional progress
     * @param elapsedNanos non-negative elapsed duration for this update
     * @return whole simulation steps to execute now, never greater than the configured cap
     * @throws NullPointerException if {@code accumulator} is null
     * @throws IllegalArgumentException if {@code elapsedNanos} is negative
     */
    public long advance(FixedStepAccumulator accumulator, long elapsedNanos) {
        Objects.requireNonNull(accumulator, "accumulator");
        if (elapsedNanos < 0L) {
            throw new IllegalArgumentException("elapsedNanos must be non-negative");
        }

        long acceptedElapsedNanos = Math.min(elapsedNanos, maxFrameGapNanos);
        long dueSteps = accumulator.advance(acceptedElapsedNanos);
        return Math.min(dueSteps, (long) maxStepsPerUpdate);
    }
}
