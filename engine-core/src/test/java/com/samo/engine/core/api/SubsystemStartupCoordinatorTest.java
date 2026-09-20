package com.samo.engine.core.api;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class SubsystemStartupCoordinatorTest {
    @Test
    void acceptsEmptyOrderAndRejectsNullsBeforeHooks() {
        assertDoesNotThrow(() -> SubsystemStartupCoordinator.start(List.of()));

        Probe probe = new Probe("only", new ArrayList<>());
        assertThrows(NullPointerException.class, () -> SubsystemStartupCoordinator.start(null));
        assertThrows(NullPointerException.class, () -> SubsystemStartupCoordinator.start(java.util.Arrays.asList(probe, null)));
        assertEquals(List.of(), probe.trace);
    }

    @Test
    void startsInSuppliedOrderAndLeavesSuccessfulLifetimeWithCaller() {
        List<String> trace = new ArrayList<>();
        Probe assets = new Probe("assets", trace);
        Probe renderer = new Probe("renderer", trace);
        Probe gameplay = new Probe("gameplay", trace);

        SubsystemStartupCoordinator.start(List.of(assets, renderer, gameplay));

        assertEquals(List.of(
                "assets.initialize", "assets.start",
                "renderer.initialize", "renderer.start",
                "gameplay.initialize", "gameplay.start"), trace);

        gameplay.stop();
        gameplay.close();
        renderer.stop();
        renderer.close();
        assets.stop();
        assets.close();

        assertEquals(List.of(
                "assets.initialize", "assets.start",
                "renderer.initialize", "renderer.start",
                "gameplay.initialize", "gameplay.start",
                "gameplay.stop", "gameplay.close",
                "renderer.stop", "renderer.close",
                "assets.stop", "assets.close"), trace);
    }

    @Test
    void initializationFailureClosesFailingSubsystemThenRollsBackStartedSubsystemsInReverse() {
        List<String> trace = new ArrayList<>();
        Probe assets = new Probe("assets", trace);
        Probe renderer = new Probe("renderer", trace);
        Probe gameplay = new Probe("gameplay", trace);
        IllegalStateException failure = new IllegalStateException("gameplay init failed");
        gameplay.fail("initialize", failure);

        assertSame(failure, assertThrows(IllegalStateException.class,
                () -> SubsystemStartupCoordinator.start(List.of(assets, renderer, gameplay))));

        assertEquals(List.of(
                "assets.initialize", "assets.start",
                "renderer.initialize", "renderer.start",
                "gameplay.initialize", "gameplay.close",
                "renderer.stop", "renderer.close",
                "assets.stop", "assets.close"), trace);
        assertEquals(1, gameplay.closeCalls);
        assertEquals(1, renderer.closeCalls);
        assertEquals(1, assets.closeCalls);
    }

    @Test
    void startFailureClosesCurrentThenRollsBackEarlierStartedSubsystems() {
        List<String> trace = new ArrayList<>();
        Probe first = new Probe("first", trace);
        Probe second = new Probe("second", trace);
        AssertionError failure = new AssertionError("second start failed");
        second.fail("start", failure);

        assertSame(failure, assertThrows(AssertionError.class,
                () -> SubsystemStartupCoordinator.start(List.of(first, second))));

        assertEquals(List.of(
                "first.initialize", "first.start",
                "second.initialize", "second.start", "second.close",
                "first.stop", "first.close"), trace);
    }

    @Test
    void firstFailureDoesNotTouchLaterSubsystems() {
        List<String> trace = new ArrayList<>();
        Probe first = new Probe("first", trace);
        Probe later = new Probe("later", trace);
        RuntimeException failure = new RuntimeException("first init failed");
        first.fail("initialize", failure);

        assertSame(failure, assertThrows(RuntimeException.class,
                () -> SubsystemStartupCoordinator.start(List.of(first, later))));

        assertEquals(List.of("first.initialize", "first.close"), trace);
        assertEquals(0, later.initializeCalls);
    }

    @Test
    void cleanupFailuresAreSuppressedWithoutReplacingOriginalFailure() {
        List<String> trace = new ArrayList<>();
        Probe first = new Probe("first", trace);
        Probe second = new Probe("second", trace);
        Probe third = new Probe("third", trace);

        RuntimeException original = new RuntimeException("third init failed");
        RuntimeException failedClose = new RuntimeException("third close failed");
        IllegalStateException secondStop = new IllegalStateException("second stop failed");
        AssertionError secondClose = new AssertionError("second close failed");
        RuntimeException firstClose = new RuntimeException("first close failed");

        third.fail("initialize", original);
        third.fail("close", failedClose);
        second.fail("stop", secondStop);
        second.fail("close", secondClose);
        first.fail("close", firstClose);

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> SubsystemStartupCoordinator.start(List.of(first, second, third)));

        assertSame(original, thrown);
        assertArrayEquals(new Throwable[] {failedClose, secondStop, secondClose, firstClose},
                thrown.getSuppressed());
        assertEquals(List.of(
                "first.initialize", "first.start",
                "second.initialize", "second.start",
                "third.initialize", "third.close",
                "second.stop", "second.close",
                "first.stop", "first.close"), trace);
    }

    @Test
    void snapshotsInputBeforeHooksCanMutateCallerList() {
        List<String> trace = new ArrayList<>();
        List<EngineSubsystem> mutable = new ArrayList<>();
        Probe first = new Probe("first", trace);
        Probe second = new Probe("second", trace);
        first.onInitializeAction = mutable::clear;
        mutable.add(first);
        mutable.add(second);

        SubsystemStartupCoordinator.start(mutable);

        assertEquals(List.of(
                "first.initialize", "first.start",
                "second.initialize", "second.start"), trace);
        assertEquals(List.of(), mutable);

        second.stop();
        second.close();
        first.stop();
        first.close();
    }

    @Test
    void doesNotSelfSuppressWhenCleanupRethrowsPrimaryFailureInstance() {
        List<String> trace = new ArrayList<>();
        Probe probe = new Probe("only", trace);
        RuntimeException failure = new RuntimeException("same instance");
        probe.fail("initialize", failure);
        probe.fail("close", failure);

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> SubsystemStartupCoordinator.start(List.of(probe)));

        assertSame(failure, thrown);
        assertEquals(0, thrown.getSuppressed().length);
    }

    private static final class Probe extends EngineSubsystem {
        private final String id;
        private final List<String> trace;
        private final java.util.Map<String, Throwable> failures = new java.util.HashMap<>();
        private Runnable onInitializeAction = () -> { };
        private int initializeCalls;
        private int closeCalls;

        private Probe(String id, List<String> trace) {
            this.id = id;
            this.trace = trace;
        }

        private void fail(String phase, Throwable failure) {
            failures.put(phase, failure);
        }

        @Override
        protected void onInitialize() {
            initializeCalls++;
            record("initialize");
            onInitializeAction.run();
            throwIfConfigured("initialize");
        }

        @Override
        protected void onStart() {
            record("start");
            throwIfConfigured("start");
        }

        @Override
        protected void onStop() {
            record("stop");
            throwIfConfigured("stop");
        }

        @Override
        protected void onClose() {
            closeCalls++;
            record("close");
            throwIfConfigured("close");
        }

        private void record(String phase) {
            trace.add(id + "." + phase);
        }

        private void throwIfConfigured(String phase) {
            Throwable failure = failures.get(phase);
            if (failure instanceof RuntimeException runtime) {
                throw runtime;
            }
            if (failure instanceof Error error) {
                throw error;
            }
        }
    }
}
