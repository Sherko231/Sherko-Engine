package com.samo.engine.core.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.LongSupplier;
import org.junit.jupiter.api.Test;

class EngineClockTest {
    @Test
    void returnsIncrementalElapsedNanosecondsFromInjectedReadings() {

        EngineClock clock = new EngineClock(sequence(100L, 160L, 160L, 225L));

        assertEquals(0L, clock.sampleElapsedNanos());
        assertEquals(60L, clock.sampleElapsedNanos());
        assertEquals(0L, clock.sampleElapsedNanos());
        assertEquals(65L, clock.sampleElapsedNanos());

    }

    @Test
    void acceptsNegativeAbsoluteReadingsWhenElapsedTimeMovesForward() {

        EngineClock clock = new EngineClock(sequence(-10L, -5L));

        assertEquals(0L, clock.sampleElapsedNanos());
        assertEquals(5L, clock.sampleElapsedNanos());

    }

    @Test
    void supportsForwardElapsedTimeAcrossSignedLongWraparound() {

        EngineClock clock = new EngineClock(sequence(Long.MAX_VALUE - 2L, Long.MIN_VALUE + 2L));

        assertEquals(0L, clock.sampleElapsedNanos());
        assertEquals(5L, clock.sampleElapsedNanos());

    }

    @Test
    void rejectsRegressionWithoutReplacingLastAcceptedReading() {

        EngineClock clock = new EngineClock(sequence(100L, 90L, 125L));

        assertEquals(0L, clock.sampleElapsedNanos());
        assertThrows(IllegalStateException.class, clock::sampleElapsedNanos);
        assertEquals(25L, clock.sampleElapsedNanos());

    }

    @Test
    void rejectsNullSourceBeforeSampling() {

        assertThrows(NullPointerException.class, () -> new EngineClock(null));

    }

    @Test
    void propagatesSourceFailureByIdentityAndPreservesBaseline() {

        RuntimeException failure = new RuntimeException("source failed");
        Queue<Object> values = new ArrayDeque<>();
        values.add(100L);
        values.add(failure);
        values.add(130L);
        LongSupplier source = () -> {
            Object value = values.remove();
            if (value instanceof RuntimeException runtime) {
                throw runtime;
            }
            return (Long) value;
        };
        EngineClock clock = new EngineClock(source);

        assertEquals(0L, clock.sampleElapsedNanos());
        assertSame(failure, assertThrows(RuntimeException.class, clock::sampleElapsedNanos));
        assertEquals(30L, clock.sampleElapsedNanos());

    }

    @Test
    void propagatesSourceErrorByIdentityAndPreservesBaseline() {

        AssertionError failure = new AssertionError("source failed");
        Queue<Object> values = new ArrayDeque<>();
        values.add(100L);
        values.add(failure);
        values.add(130L);
        LongSupplier source = () -> {
            Object value = values.remove();
            if (value instanceof Error error) {
                throw error;
            }
            return (Long) value;
        };
        EngineClock clock = new EngineClock(source);

        assertEquals(0L, clock.sampleElapsedNanos());
        assertSame(failure, assertThrows(AssertionError.class, clock::sampleElapsedNanos));
        assertEquals(30L, clock.sampleElapsedNanos());

    }

    @Test
    void readsSourceExactlyOncePerSampleAttempt() {

        AtomicInteger calls = new AtomicInteger();
        EngineClock clock = new EngineClock(() -> {
            int call = calls.incrementAndGet();
            return switch (call) {
                case 1 -> 100L;
                case 2 -> 90L;
                default -> 125L;
            };
        });

        assertEquals(0L, clock.sampleElapsedNanos());
        assertEquals(1, calls.get());
        assertThrows(IllegalStateException.class, clock::sampleElapsedNanos);
        assertEquals(2, calls.get());
        assertEquals(25L, clock.sampleElapsedNanos());
        assertEquals(3, calls.get());

    }

    @Test
    void defaultClockCanEstablishABaseline() {

        EngineClock clock = new EngineClock();

        assertEquals(0L, clock.sampleElapsedNanos());

    }

    private static LongSupplier sequence(long... readings) {

        Queue<Long> values = new ArrayDeque<>();
        for (long reading : readings) {
            values.add(reading);
        }
        return values::remove;

    }
}
