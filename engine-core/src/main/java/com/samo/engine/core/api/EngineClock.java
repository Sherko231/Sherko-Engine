package com.samo.engine.core.api;

import java.util.Objects;
import java.util.function.LongSupplier;

/**
 * Samples elapsed nanoseconds from a monotonic time source.
 *
 * <p>
 * The first successful sample establishes the baseline and returns zero. Later samples return
 * the elapsed nanoseconds since the last accepted sample. Calls are expected to be externally
 * serialized by the runtime loop.
 */
public final class EngineClock {
    private final LongSupplier nanoTimeSource;
    private boolean sampled;
    private long previousAccepted;

    /**
     * Creates a clock backed by {@link System#nanoTime()}.
     */
    public EngineClock() {

        this(System::nanoTime);

    }

    /**
     * Creates a clock backed by the supplied nanosecond source.
     *
     * @param nanoTimeSource
     *            monotonic nanosecond source
     * @throws NullPointerException
     *             if the source is null
     */
    public EngineClock(LongSupplier nanoTimeSource) {

        this.nanoTimeSource = Objects.requireNonNull(nanoTimeSource, "nanoTimeSource");

    }

    /**
     * Samples elapsed nanoseconds since the previous accepted reading.
     *
     * <p>
     * The first successful sample returns zero. A negative signed elapsed difference is rejected
     * and does not replace the previous accepted reading. Ordinary {@code long} subtraction is used
     * so a forward interval smaller than {@code 2^63} nanoseconds remains valid across the signed
     * {@code long} boundary.
     *
     * @return non-negative elapsed nanoseconds since the previous accepted sample
     * @throws IllegalStateException
     *             if the source regresses outside the supported elapsed range
     * @throws RuntimeException
     *             if the configured source throws one
     * @throws Error
     *             if the configured source throws one
     */
    public long sampleElapsedNanos() {

        long current = nanoTimeSource.getAsLong();
        if (!sampled) {
            previousAccepted = current;
            sampled = true;
            return 0L;
        }

        long elapsed = current - previousAccepted;
        if (elapsed < 0L) {
            throw new IllegalStateException("Monotonic clock source regressed");
        }

        previousAccepted = current;
        return elapsed;

    }
}
