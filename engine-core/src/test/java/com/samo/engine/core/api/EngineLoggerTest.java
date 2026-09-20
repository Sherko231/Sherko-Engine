package com.samo.engine.core.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class EngineLoggerTest {

    @Test
    void eventPreservesLevelMessageContextAndCallerThreadIdentity() {

        List<EngineLogger.Event> captured = new ArrayList<>();
        EngineLogger logger = new EngineLogger(captured::add);
        EngineLogger.Context context = new EngineLogger.Context(120L, 240L, " network ", " conn-a ", " player-7 ");
        Thread caller = Thread.currentThread();

        logger.log(EngineLogger.Level.INFO, "received input", context);

        assertEquals(1, captured.size());
        EngineLogger.Event event = captured.getFirst();
        assertEquals(EngineLogger.Level.INFO, event.level());
        assertEquals("received input", event.message());
        assertSame(context, event.context());
        assertEquals(120L, event.context().frame());
        assertEquals(240L, event.context().simulationTick());
        assertEquals("network", event.context().subsystem());
        assertEquals("conn-a", event.context().connection());
        assertEquals("player-7", event.context().entity());
        assertEquals(caller.threadId(), event.threadId());
        assertEquals(caller.getName(), event.threadName());
        assertTrue(event.timestamp() != null);

    }

    @Test
    void emptyContextUsesNullAsTheOnlyMissingRepresentation() {

        EngineLogger.Context context = EngineLogger.Context.empty();

        assertNull(context.frame());
        assertNull(context.simulationTick());
        assertNull(context.subsystem());
        assertNull(context.connection());
        assertNull(context.entity());

    }

    @Test
    void contextAcceptsAbsentAndZeroNumericFieldsAndRejectsNegativeValues() {

        EngineLogger.Context absent = new EngineLogger.Context(null, null, null, null, null);
        EngineLogger.Context zero = new EngineLogger.Context(0L, 0L, null, null, null);

        assertNull(absent.frame());
        assertNull(absent.simulationTick());
        assertEquals(0L, zero.frame());
        assertEquals(0L, zero.simulationTick());
        assertThrows(IllegalArgumentException.class, () -> new EngineLogger.Context(-1L, 0L, null, null, null));
        assertThrows(IllegalArgumentException.class, () -> new EngineLogger.Context(0L, -1L, null, null, null));

    }

    @Test
    void contextNormalizesOptionalStringsAndRejectsBlankPresentValues() {

        EngineLogger.Context context = new EngineLogger.Context(null, null, "  render  ", "  connection-2  ", "  entity-9  ");

        assertEquals("render", context.subsystem());
        assertEquals("connection-2", context.connection());
        assertEquals("entity-9", context.entity());
        assertThrows(IllegalArgumentException.class, () -> new EngineLogger.Context(null, null, "   ", null, null));
        assertThrows(IllegalArgumentException.class, () -> new EngineLogger.Context(null, null, null, "\t", null));
        assertThrows(IllegalArgumentException.class, () -> new EngineLogger.Context(null, null, null, null, "\n"));

    }

    @Test
    void connectionFilteringUsesStructuredFieldNotMessageText() {

        List<EngineLogger.Event> captured = new ArrayList<>();
        EngineLogger logger = new EngineLogger(captured::add);

        logger.log(EngineLogger.Level.INFO, "same message", new EngineLogger.Context(1L, 10L, "network", "conn-a", "entity-a"));
        logger.log(EngineLogger.Level.INFO, "same message", new EngineLogger.Context(1L, 10L, "network", "conn-b", "entity-b"));
        logger.log(EngineLogger.Level.WARN, "same message", new EngineLogger.Context(2L, 11L, "physics", "conn-a", "entity-c"));

        List<EngineLogger.Event> connA = captured.stream().filter(event -> "conn-a".equals(event.context().connection())).toList();

        assertEquals(2, connA.size());
        assertEquals(List.of("entity-a", "entity-c"), connA.stream().map(event -> event.context().entity()).toList());
        assertTrue(connA.stream().allMatch(event -> "same message".equals(event.message())));
        assertFalse(connA.stream().anyMatch(event -> "conn-b".equals(event.context().connection())));

    }

    @Test
    void everySeverityIsForwardedWithoutThresholdFiltering() {

        List<EngineLogger.Event> captured = new ArrayList<>();
        EngineLogger logger = new EngineLogger(captured::add);

        for (EngineLogger.Level level : EngineLogger.Level.values()) {
            logger.log(level, "event", EngineLogger.Context.empty());
        }

        assertEquals(EngineLogger.Level.values().length, captured.size());
        assertEquals(EnumSet.allOf(EngineLogger.Level.class), EnumSet.copyOf(captured.stream().map(EngineLogger.Event::level).toList()));

    }

    @Test
    void invalidLogArgumentsFailBeforeSinkInvocation() {

        AtomicInteger writes = new AtomicInteger();
        EngineLogger logger = new EngineLogger(event -> writes.incrementAndGet());

        assertThrows(NullPointerException.class, () -> new EngineLogger(null));
        assertThrows(NullPointerException.class, () -> logger.log(null, "message", EngineLogger.Context.empty()));
        assertThrows(NullPointerException.class, () -> logger.log(EngineLogger.Level.INFO, null, EngineLogger.Context.empty()));
        assertThrows(NullPointerException.class, () -> logger.log(EngineLogger.Level.INFO, "message", null));
        assertThrows(IllegalArgumentException.class, () -> logger.log(EngineLogger.Level.INFO, " \t\n ", EngineLogger.Context.empty()));
        assertEquals(0, writes.get());

    }

    @Test
    void writeRuntimeExceptionPropagatesByIdentityWithOneAttempt() {

        RuntimeException expected = new RuntimeException("write failed");
        AtomicInteger attempts = new AtomicInteger();
        EngineLogger logger = new EngineLogger(event -> {
            attempts.incrementAndGet();
            throw expected;
        });

        RuntimeException actual = assertThrows(RuntimeException.class, () -> logger.log(EngineLogger.Level.ERROR, "event", EngineLogger.Context.empty()));

        assertSame(expected, actual);
        assertEquals(1, attempts.get());

    }

    @Test
    void writeErrorPropagatesByIdentityWithOneAttempt() {

        AssertionError expected = new AssertionError("write failed");
        AtomicInteger attempts = new AtomicInteger();
        EngineLogger logger = new EngineLogger(event -> {
            attempts.incrementAndGet();
            throw expected;
        });

        AssertionError actual = assertThrows(AssertionError.class, () -> logger.log(EngineLogger.Level.ERROR, "event", EngineLogger.Context.empty()));

        assertSame(expected, actual);
        assertEquals(1, attempts.get());

    }

    @Test
    void flushDelegatesExactlyOnceAndPreservesFailureIdentity() {

        AtomicInteger successfulFlushes = new AtomicInteger();
        EngineLogger successful = new EngineLogger(new EngineLogger.Sink() {
            @Override
            public void write(EngineLogger.Event event) {

            }

            @Override
            public void flush() {

                successfulFlushes.incrementAndGet();

            }
        });
        successful.flush();
        assertEquals(1, successfulFlushes.get());

        RuntimeException runtimeFailure = new RuntimeException("flush runtime");
        AtomicInteger runtimeAttempts = new AtomicInteger();
        EngineLogger runtimeLogger = new EngineLogger(new EngineLogger.Sink() {
            @Override
            public void write(EngineLogger.Event event) {

            }

            @Override
            public void flush() {

                runtimeAttempts.incrementAndGet();
                throw runtimeFailure;

            }
        });
        assertSame(runtimeFailure, assertThrows(RuntimeException.class, runtimeLogger::flush));
        assertEquals(1, runtimeAttempts.get());

        AssertionError errorFailure = new AssertionError("flush error");
        AtomicInteger errorAttempts = new AtomicInteger();
        EngineLogger errorLogger = new EngineLogger(new EngineLogger.Sink() {
            @Override
            public void write(EngineLogger.Event event) {

            }

            @Override
            public void flush() {

                errorAttempts.incrementAndGet();
                throw errorFailure;

            }
        });
        assertSame(errorFailure, assertThrows(AssertionError.class, errorLogger::flush));
        assertEquals(1, errorAttempts.get());

    }

    @Test
    void concurrentCallersAreSerializedAtSinkAndRetainOriginalCallerThread() throws Exception {

        AtomicInteger activeCallbacks = new AtomicInteger();
        AtomicInteger maxActiveCallbacks = new AtomicInteger();
        CountDownLatch firstEntered = new CountDownLatch(1);
        CountDownLatch secondCallerStarted = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        CountDownLatch twoSinkEntries = new CountDownLatch(2);
        List<EngineLogger.Event> captured = new ArrayList<>();

        EngineLogger logger = new EngineLogger(event -> {
            int active = activeCallbacks.incrementAndGet();
            maxActiveCallbacks.accumulateAndGet(active, Math::max);
            synchronized (captured) {
                captured.add(event);
            }
            twoSinkEntries.countDown();
            try {
                if (firstEntered.getCount() > 0) {
                    firstEntered.countDown();
                    if (!releaseFirst.await(5, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("test timed out waiting to release first sink callback");
                    }
                }
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(interrupted);
            } finally {
                activeCallbacks.decrementAndGet();
            }
        });

        AtomicReference<Throwable> firstFailure = new AtomicReference<>();
        AtomicReference<Throwable> secondFailure = new AtomicReference<>();
        Thread first = Thread.ofPlatform().name("logger-caller-one").unstarted(() -> {
            try {
                logger.log(EngineLogger.Level.INFO, "first", EngineLogger.Context.empty());
            } catch (Throwable failure) {
                firstFailure.set(failure);
            }
        });
        Thread second = Thread.ofPlatform().name("logger-caller-two").unstarted(() -> {
            secondCallerStarted.countDown();
            try {
                logger.log(EngineLogger.Level.INFO, "second", EngineLogger.Context.empty());
            } catch (Throwable failure) {
                secondFailure.set(failure);
            }
        });

        first.start();
        assertTrue(firstEntered.await(5, TimeUnit.SECONDS));
        second.start();
        assertTrue(secondCallerStarted.await(5, TimeUnit.SECONDS));

        assertFalse(twoSinkEntries.await(250, TimeUnit.MILLISECONDS), "second sink callback must not overlap the blocked first callback");
        releaseFirst.countDown();
        first.join(5_000L);
        second.join(5_000L);

        assertFalse(first.isAlive());
        assertFalse(second.isAlive());
        assertNull(firstFailure.get());
        assertNull(secondFailure.get());
        assertEquals(1, maxActiveCallbacks.get());
        assertEquals(2, captured.size());

        EngineLogger.Event firstEvent = captured.stream().filter(event -> "first".equals(event.message())).findFirst().orElseThrow();
        EngineLogger.Event secondEvent = captured.stream().filter(event -> "second".equals(event.message())).findFirst().orElseThrow();
        assertEquals(first.threadId(), firstEvent.threadId());
        assertEquals(first.getName(), firstEvent.threadName());
        assertEquals(second.threadId(), secondEvent.threadId());
        assertEquals(second.getName(), secondEvent.threadName());

    }
}
