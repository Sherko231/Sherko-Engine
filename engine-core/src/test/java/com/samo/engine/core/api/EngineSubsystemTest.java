package com.samo.engine.core.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class EngineSubsystemTest {
    @Test
    void runsAllPhasesInOrderAndClosesThroughAutoCloseable() throws Exception {
        Probe subsystem = new Probe();
        try (AutoCloseable owned = subsystem) {
            subsystem.initialize();
            assertEquals(List.of("initialize"), subsystem.calls);
            subsystem.start();
            assertTrue(subsystem.active);
            subsystem.stop();
            assertEquals(1, subsystem.resources);
        }

        assertEquals(List.of("initialize", "start", "stop", "close"), subsystem.calls);
        assertReleased(subsystem);
        subsystem.close();
        assertEquals(4, subsystem.calls.size());
    }

    @ParameterizedTest
    @CsvSource({
        "0, start", "0, stop",
        "1, initialize", "1, stop",
        "2, initialize", "2, start", "2, close",
        "3, initialize", "3, start", "3, stop",
        "4, initialize", "4, start", "4, stop"
    })
    void invalidStableStateCallsDoNotInvokeHooksOrDamageProgress(int completedPhases, String operation) {
        Probe subsystem = new Probe();
        advance(subsystem, completedPhases);
        List<String> before = List.copyOf(subsystem.calls);

        IllegalStateException failure = assertThrows(IllegalStateException.class, () -> invoke(subsystem, operation));

        assertTrue(failure.getMessage().contains(operation));
        assertEquals(before, subsystem.calls);
        for (int index = completedPhases; index < PHASES.size(); index++) {
            invoke(subsystem, PHASES.get(index));
        }
        assertEquals(PHASES, subsystem.calls);
        assertReleased(subsystem);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    void closesWithoutStartingAndDoesNotRepeatCleanup(int completedPhases) {
        Probe subsystem = new Probe();
        advance(subsystem, completedPhases);

        subsystem.close();
        subsystem.close();

        List<String> expected = new ArrayList<>(PHASES.subList(0, completedPhases));
        expected.add("close");
        assertEquals(expected, subsystem.calls);
        assertReleased(subsystem);
        assertForwardProgressRejected(subsystem);
    }

    @ParameterizedTest
    @CsvSource({"initialize, false", "start, false", "stop, false",
        "initialize, true", "start, true", "stop, true"})
    void hookFailurePropagatesUnchangedAndStillAllowsExplicitCleanup(String phase, boolean error) {
        Probe subsystem = new Probe();
        Throwable failure = error ? new AssertionError("hook failure") : new IllegalArgumentException("hook failure");
        subsystem.failurePhase = phase;
        subsystem.failure = failure;
        advance(subsystem, PHASES.indexOf(phase));

        assertSame(failure, assertThrows(Throwable.class, () -> invoke(subsystem, phase)));
        List<String> before = List.copyOf(subsystem.calls);
        assertForwardProgressRejected(subsystem);
        assertEquals(before, subsystem.calls);
        assertEquals(1, subsystem.resources);

        subsystem.close();
        subsystem.close();

        List<String> expected = new ArrayList<>(before);
        expected.add("close");
        assertEquals(expected, subsystem.calls);
        assertReleased(subsystem);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void failingCloseIsTerminalAndNeverRetriesItsHook(boolean error) {
        Probe subsystem = new Probe();
        subsystem.initialize();
        Throwable failure = error ? new AssertionError("close failure") : new IllegalStateException("close failure");
        subsystem.failurePhase = "close";
        subsystem.failure = failure;

        assertSame(failure, assertThrows(Throwable.class, subsystem::close));
        subsystem.close();
        assertForwardProgressRejected(subsystem);

        assertEquals(List.of("initialize", "close"), subsystem.calls);
        assertEquals(1, subsystem.resources, "A failed cleanup must not be mistaken for successful release");
    }

    @ParameterizedTest
    @ValueSource(strings = {"initialize", "start", "stop", "close"})
    void rejectsEveryReentrantOperationWhileEachHookIsExecuting(String phase) {
        Probe subsystem = new Probe();
        subsystem.reentrantPhase = phase;

        advance(subsystem, 4);

        assertEquals(4, subsystem.reentrantRejections);
        assertEquals(PHASES, subsystem.calls);
        assertReleased(subsystem);
    }

    private static final List<String> PHASES = List.of("initialize", "start", "stop", "close");

    private static void advance(EngineSubsystem subsystem, int count) {
        for (int index = 0; index < count; index++) {
            invoke(subsystem, PHASES.get(index));
        }
    }

    private static void invoke(EngineSubsystem subsystem, String phase) {
        switch (phase) {
            case "initialize" -> subsystem.initialize();
            case "start" -> subsystem.start();
            case "stop" -> subsystem.stop();
            case "close" -> subsystem.close();
            default -> throw new IllegalArgumentException(phase);
        }
    }

    private static void assertForwardProgressRejected(Probe subsystem) {
        assertThrows(IllegalStateException.class, subsystem::initialize);
        assertThrows(IllegalStateException.class, subsystem::start);
        assertThrows(IllegalStateException.class, subsystem::stop);
    }

    private static void assertReleased(Probe subsystem) {
        assertEquals(0, subsystem.resources);
        assertFalse(subsystem.active);
    }

    private static final class Probe extends EngineSubsystem {
        private final List<String> calls = new ArrayList<>();
        private String failurePhase;
        private Throwable failure;
        private String reentrantPhase;
        private int reentrantRejections;
        private int resources;
        private boolean active;

        @Override
        protected void onInitialize() {
            resources++;
            record("initialize");
        }

        @Override
        protected void onStart() {
            active = true;
            record("start");
        }

        @Override
        protected void onStop() {
            record("stop");
            active = false;
        }

        @Override
        protected void onClose() {
            record("close");
            active = false;
            resources = 0;
        }

        private void record(String phase) {
            calls.add(phase);
            if (phase.equals(reentrantPhase)) {
                for (String nested : PHASES) {
                    assertThrows(IllegalStateException.class, () -> invoke(this, nested));
                    reentrantRejections++;
                }
            }
            if (phase.equals(failurePhase)) {
                if (failure instanceof RuntimeException runtime) {
                    throw runtime;
                }
                throw (Error) failure;
            }
        }
    }
}
