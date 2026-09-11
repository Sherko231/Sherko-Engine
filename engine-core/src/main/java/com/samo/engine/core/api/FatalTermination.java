package com.samo.engine.core.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.IntConsumer;

/**
 * Coordinates one orderly fatal shutdown before process termination.
 *
 * <p>The caller must invoke this coordinator on the lifecycle/native-affinity
 * thread that owns the supplied subsystems and resource registry. Shutdown is
 * synchronous and one-shot: diagnostics, reverse subsystem cleanup, registry
 * verification, failure reporting, and log flushing are all attempted before
 * the termination action runs.
 */
public final class FatalTermination {
    private static final int FATAL_EXIT_STATUS = 1;

    private final EngineLogger logger;
    private final IntConsumer terminator;
    private final AtomicReference<State> state = new AtomicReference<>(State.READY);

    /**
     * Creates a coordinator that terminates the process with exit status {@code 1}.
     *
     * @param logger synchronous structured logger to use for fatal diagnostics
     * @throws NullPointerException if {@code logger} is null
     */
    public FatalTermination(EngineLogger logger) {
        this(logger, System::exit);
    }

    FatalTermination(EngineLogger logger, IntConsumer terminator) {
        this.logger = Objects.requireNonNull(logger, "logger");
        this.terminator = Objects.requireNonNull(terminator, "terminator");
    }

    /**
     * Logs a fatal diagnostic, attempts orderly reverse shutdown, verifies native
     * ownership, flushes diagnostics, and then terminates with exit status {@code 1}.
     *
     * <p>The supplied subsystem order is the dependency-first order that completed
     * startup. This method snapshots and validates it before any side effect, then
     * visits it in strict reverse order. Cleanup and diagnostic failures are retained
     * but never prevent later cleanup or the final termination attempt.
     *
     * @param message nonblank fatal diagnostic message, preserved as supplied
     * @param context structured context for the fatal and cleanup-failure events
     * @param initializationOrder dependency-first successfully started subsystems
     * @param resourceRegistry registry to verify after subsystem cleanup
     * @throws NullPointerException for null required arguments or list elements
     * @throws IllegalArgumentException for a blank message or repeated subsystem instance
     * @throws IllegalStateException if this coordinator was already claimed, or if a
     *     test termination action returns normally
     */
    public void terminate(
            String message,
            EngineLogger.Context context,
            List<EngineSubsystem> initializationOrder,
            NativeResourceRegistry resourceRegistry) {
        String fatalMessage = Objects.requireNonNull(message, "message");
        if (fatalMessage.isBlank()) {
            throw new IllegalArgumentException("message must not be blank");
        }
        EngineLogger.Context fatalContext = Objects.requireNonNull(context, "context");
        List<EngineSubsystem> ordered = snapshotAndValidate(initializationOrder);
        NativeResourceRegistry registry = Objects.requireNonNull(resourceRegistry, "resourceRegistry");

        if (!state.compareAndSet(State.READY, State.TERMINATING)) {
            throw new IllegalStateException("Fatal termination is already in progress or completed");
        }

        List<Throwable> failures = new ArrayList<>();
        captureFailure(
                failures,
                () -> logger.log(EngineLogger.Level.FATAL, fatalMessage, fatalContext));

        for (int index = ordered.size() - 1; index >= 0; index--) {
            EngineSubsystem subsystem = ordered.get(index);
            captureFailure(failures, subsystem::stop);
            captureFailure(failures, subsystem::close);
        }

        captureFailure(failures, registry::assertNoOpenResources);

        int reportableFailureCount = failures.size();
        for (int index = 0; index < reportableFailureCount; index++) {
            Throwable failure = failures.get(index);
            int failureNumber = index + 1;
            captureFailure(
                    failures,
                    () -> logger.log(
                            EngineLogger.Level.ERROR,
                            failureMessage(failureNumber, failure),
                            fatalContext));
        }

        captureFailure(failures, logger::flush);
        terminateProcess(failures);
    }

    private static List<EngineSubsystem> snapshotAndValidate(List<EngineSubsystem> initializationOrder) {
        List<EngineSubsystem> snapshot = List.copyOf(
                Objects.requireNonNull(initializationOrder, "initializationOrder"));
        Set<EngineSubsystem> identities = Collections.newSetFromMap(new IdentityHashMap<>());
        for (EngineSubsystem subsystem : snapshot) {
            if (!identities.add(subsystem)) {
                throw new IllegalArgumentException("initializationOrder repeats a subsystem instance");
            }
        }
        return snapshot;
    }

    private static String failureMessage(int index, Throwable failure) {
        return "Fatal cleanup failure "
                + index
                + ": "
                + failure.getClass().getName()
                + ": "
                + String.valueOf(failure.getMessage());
    }

    private static void captureFailure(List<Throwable> failures, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException | Error failure) {
            failures.add(failure);
        }
    }

    private void terminateProcess(List<Throwable> failures) {
        try {
            terminator.accept(FATAL_EXIT_STATUS);
        } catch (RuntimeException | Error terminationFailure) {
            state.set(State.TERMINATED);
            addSuppressedFailures(terminationFailure, failures);
            throw terminationFailure;
        }

        IllegalStateException returned =
                new IllegalStateException("Fatal termination action returned normally");
        state.set(State.TERMINATED);
        addSuppressedFailures(returned, failures);
        throw returned;
    }

    private static void addSuppressedFailures(Throwable primary, List<Throwable> failures) {
        for (Throwable failure : failures) {
            if (failure != primary) {
                primary.addSuppressed(failure);
            }
        }
    }

    private enum State {
        READY,
        TERMINATING,
        TERMINATED
    }
}
