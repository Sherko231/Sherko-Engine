package com.samo.engine.core.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Starts an already ordered set of subsystems and rolls back partial startup on failure.
 *
 * <p>
 * The supplied order is snapshotted before hooks run. Callers normally obtain it from
 * {@link SubsystemGraph#initializationOrder()}. Successful startup keeps ownership with the
 * caller; this utility performs cleanup only when initialization or startup fails.
 */
public final class SubsystemStartupCoordinator {
    private SubsystemStartupCoordinator() {

    }

    /**
     * Initializes and starts each subsystem in the supplied order.
     *
     * <p>
     * If initialization or startup fails, the failing subsystem is closed and all previously
     * started subsystems are stopped and closed in reverse order. Rollback failures are attached
     * as suppressed exceptions to the original failure, which is rethrown unchanged.
     *
     * @param initializationOrder
     *            dependency-first subsystem order
     * @throws NullPointerException
     *             if the list or any element is null
     * @throws RuntimeException
     *             if a lifecycle hook throws a runtime exception
     * @throws Error
     *             if a lifecycle hook throws an error
     */
    public static void start(List<EngineSubsystem> initializationOrder) {

        List<EngineSubsystem> ordered = List.copyOf(initializationOrder);
        List<EngineSubsystem> started = new ArrayList<>(ordered.size());

        for (EngineSubsystem subsystem : ordered) {
            Objects.requireNonNull(subsystem, "subsystem");
            try {
                subsystem.initialize();
                subsystem.start();
                started.add(subsystem);
            } catch (RuntimeException | Error failure) {
                rollback(failure, subsystem, started);
                throw failure;
            }
        }

    }

    private static void rollback(Throwable primary, EngineSubsystem failed, List<EngineSubsystem> started) {

        tryCleanup(primary, failed::close);
        for (int index = started.size() - 1; index >= 0; index--) {
            EngineSubsystem subsystem = started.get(index);
            tryCleanup(primary, subsystem::stop);
            tryCleanup(primary, subsystem::close);
        }

    }

    private static void tryCleanup(Throwable primary, Runnable cleanup) {

        try {
            cleanup.run();
        } catch (RuntimeException | Error cleanupFailure) {
            if (cleanupFailure != primary) {
                primary.addSuppressed(cleanupFailure);
            }
        }

    }
}
