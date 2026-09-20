package com.samo.engine.core.api;

import java.time.Instant;
import java.util.Objects;

/**
 * Synchronous structured runtime logging boundary with caller-owned sink semantics.
 *
 * <p>
 * The logger captures timestamp and caller-thread identity automatically. Sink callbacks
 * are serialized across concurrent callers, but the logger owns no background thread,
 * buffering, retry policy, persistence format, or sink lifetime.
 */
public final class EngineLogger {
    private final Sink sink;
    private final Object sinkLock = new Object();

    /**
     * Creates a logger that forwards every valid event to the supplied caller-owned sink.
     *
     * @param sink
     *            synchronous event sink
     * @throws NullPointerException
     *             if {@code sink} is null
     */
    public EngineLogger(Sink sink) {
        this.sink = Objects.requireNonNull(sink, "sink");
    }

    /**
     * Emits one structured event synchronously.
     *
     * @param level
     *            severity label
     * @param message
     *            nonblank message preserved as supplied
     * @param context
     *            immutable structured context
     * @throws NullPointerException
     *             if level, message, or context is null
     * @throws IllegalArgumentException
     *             if message is blank
     */
    public void log(Level level, String message, Context context) {
        Level eventLevel = Objects.requireNonNull(level, "level");
        String eventMessage = Objects.requireNonNull(message, "message");
        Context eventContext = Objects.requireNonNull(context, "context");
        if (eventMessage.isBlank()) {
            throw new IllegalArgumentException("message must not be blank");
        }

        Thread caller = Thread.currentThread();
        Event event = new Event(Instant.now(), eventLevel, eventMessage, caller.threadId(), caller.getName(), eventContext);

        synchronized (sinkLock) {
            sink.write(event);
        }
    }

    /** Flushes the caller-owned sink synchronously. */
    public void flush() {
        synchronized (sinkLock) {
            sink.flush();
        }
    }

    /** Fixed severity labels. The logger performs no threshold filtering. */
    public enum Level {
        DEBUG, INFO, WARN, ERROR, FATAL
    }

    /**
     * Optional structured context for one event. Missing values are represented only by
     * {@code null}.
     */
    public record Context(Long frame, Long simulationTick, String subsystem, String connection, String entity) {

        /** Validates numeric fields and normalizes present string fields with {@link String#strip()}. */
        public Context {
            if (frame != null && frame < 0L) {
                throw new IllegalArgumentException("frame must be non-negative when present");
            }
            if (simulationTick != null && simulationTick < 0L) {
                throw new IllegalArgumentException("simulationTick must be non-negative when present");
            }
            subsystem = normalizeOptional("subsystem", subsystem);
            connection = normalizeOptional("connection", connection);
            entity = normalizeOptional("entity", entity);
        }

        /** Returns a context with every optional field absent. */
        public static Context empty() {
            return new Context(null, null, null, null, null);
        }

        private static String normalizeOptional(String fieldName, String value) {
            if (value == null) {
                return null;
            }
            String normalized = value.strip();
            if (normalized.isEmpty()) {
                throw new IllegalArgumentException(fieldName + " must not be blank when present");
            }
            return normalized;
        }
    }

    /** Immutable structured event passed to the sink. */
    public record Event(Instant timestamp, Level level, String message, long threadId, String threadName, Context context) {
    }

    /** Caller-owned synchronous event sink. */
    @FunctionalInterface
    public interface Sink {
        /** Writes one event synchronously. */
        void write(Event event);

        /** Flushes any sink-owned buffered state. The default sink has nothing to flush. */
        default void flush() {
        }
    }
}
