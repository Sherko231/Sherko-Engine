package com.samo.engine.core.api;

/**
 * Enforces one subsystem lifetime: initialize, start, stop, then close.
 *
 * <p>
 * The owner must serialize all calls on its lifecycle thread. This class is
 * not thread-safe and provides no restart or native reinitialization guarantee.
 * Reentrant lifecycle calls from hooks are rejected.
 *
 * <p>
 * Initialization acquires resources; starting activates work; stopping
 * quiesces work; closing releases resources. An unstarted subsystem can be
 * closed directly. A running subsystem must be stopped explicitly before close.
 * No other subsystem is coordinated or rolled back by this class.
 *
 * <p>
 * An unchecked initialize/start/stop hook failure is propagated unchanged and
 * prevents further forward progress, but the owner can still close. Closing is
 * attempted at most once, even if its hook fails. A failed close does not prove
 * resource release; the owner must report that failure.
 */
public abstract class EngineSubsystem implements AutoCloseable {
    private State state = State.NEW;

    /** Creates an uninitialized subsystem without acquiring resources. */
    protected EngineSubsystem() {

    }

    /**
     * Acquires resources once.
     *
     * @throws IllegalStateException
     *             unless this instance is new
     */
    public final void initialize() {

        transition("initialize", State.NEW, State.INITIALIZING, State.INITIALIZED, this::onInitialize);

    }

    /**
     * Activates successfully initialized resources once.
     *
     * @throws IllegalStateException
     *             unless initialization completed successfully
     */
    public final void start() {

        transition("start", State.INITIALIZED, State.STARTING, State.STARTED, this::onStart);

    }

    /**
     * Quiesces a successfully started subsystem once without releasing its resources.
     *
     * @throws IllegalStateException
     *             unless starting completed successfully
     */
    public final void stop() {

        transition("stop", State.STARTED, State.STOPPING, State.STOPPED, this::onStop);

    }

    /**
     * Attempts resource release once, including after partial initialization or
     * failed activation/stopping. Further calls after this attempt return normally.
     *
     * @throws IllegalStateException
     *             if running or inside any lifecycle hook
     */
    @Override
    public final void close() {

        if (state == State.CLOSED) {
            return;
        }
        switch (state) {
            case NEW, INITIALIZED, STOPPED, FAILED -> {
                state = State.CLOSING;
                try {
                    onClose();
                } finally {
                    state = State.CLOSED;
                }
            }
            default -> throw invalidOperation("close");
        }

    }

    /** Acquires resources, retaining ownership information even if setup fails. */
    protected abstract void onInitialize();

    /** Activates work using initialized resources. */
    protected abstract void onStart();

    /** Quiesces work so resources can subsequently be released. */
    protected abstract void onStop();

    /**
     * Releases owned resources, tolerating no initialization, partial setup, or
     * failed start/stop. If activation or stopping failed, this hook must also
     * quiesce any remaining work before release. It must not call lifecycle methods.
     */
    protected abstract void onClose();

    private void transition(String operation, State expected, State entering, State completed, Runnable hook) {

        if (state != expected) {
            throw invalidOperation(operation);
        }
        state = entering;
        try {
            hook.run();
            state = completed;
        } catch (RuntimeException | Error failure) {
            state = State.FAILED;
            throw failure;
        }

    }

    private IllegalStateException invalidOperation(String operation) {

        return new IllegalStateException("Cannot " + operation + " subsystem in state " + state);

    }

    private enum State {
        NEW, INITIALIZING, INITIALIZED, STARTING, STARTED, STOPPING, STOPPED, FAILED, CLOSING, CLOSED
    }
}
