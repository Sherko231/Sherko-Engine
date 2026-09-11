package com.samo.engine.core.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FatalTerminationTest {
    private static final EngineLogger.Context CONTEXT =
            new EngineLogger.Context(12L, 34L, "core", null, null);

    @Test
    void happyPathLogsShutsDownInReverseVerifiesFlushesAndTerminates() {
        List<String> trace = new ArrayList<>();
        NativeResourceRegistry registry = new NativeResourceRegistry();
        ProbeSubsystem first = new ProbeSubsystem("first", registry, trace, 11L);
        ProbeSubsystem second = new ProbeSubsystem("second", registry, trace, 22L);
        start(first, second);
        trace.clear();

        EngineLogger logger = new EngineLogger(new TraceSink(trace));
        TerminationSignal signal = new TerminationSignal();
        FatalTermination fatal = new FatalTermination(logger, status -> {
            trace.add("terminate:" + status);
            registry.assertNoOpenResources();
            throw signal;
        });

        assertSame(
                signal,
                assertThrows(
                        TerminationSignal.class,
                        () -> fatal.terminate("fatal", CONTEXT, List.of(first, second), registry)));

        assertEquals(
                List.of(
                        "log:FATAL:fatal",
                        "second:stop",
                        "second:close",
                        "second:resource-close",
                        "first:stop",
                        "first:close",
                        "first:resource-close",
                        "flush",
                        "terminate:1"),
                trace);
        assertEquals(1, first.resourceCloses.get());
        assertEquals(1, second.resourceCloses.get());
    }

    @Test
    void stopFailureStillClosesThatSubsystemAndContinuesReverseCleanup() {
        List<String> trace = new ArrayList<>();
        NativeResourceRegistry registry = new NativeResourceRegistry();
        ProbeSubsystem first = new ProbeSubsystem("first", registry, trace, 1L);
        ProbeSubsystem second = new ProbeSubsystem("second", registry, trace, 2L);
        second.stopFailure = new IllegalStateException("stop failed");
        start(first, second);
        trace.clear();

        List<EngineLogger.Event> events = new ArrayList<>();
        EngineLogger logger = new EngineLogger(new CapturingSink(events, trace));
        TerminationSignal signal = new TerminationSignal();
        FatalTermination fatal = new FatalTermination(logger, status -> {
            trace.add("terminate:" + status);
            throw signal;
        });

        TerminationSignal actual = assertThrows(
                TerminationSignal.class,
                () -> fatal.terminate("fatal", CONTEXT, List.of(first, second), registry));

        assertSame(signal, actual);
        assertEquals(1, actual.getSuppressed().length);
        assertSame(second.stopFailure, actual.getSuppressed()[0]);
        assertTrue(trace.indexOf("second:stop") < trace.indexOf("second:close"));
        assertTrue(trace.indexOf("second:close") < trace.indexOf("first:stop"));
        assertTrue(trace.indexOf("first:close") < trace.indexOf("flush"));
        assertEquals(1, second.resourceCloses.get());
        assertEquals(1, first.resourceCloses.get());
        assertEquals(1, events.stream().filter(event -> event.level() == EngineLogger.Level.ERROR).count());
        EngineLogger.Event error = events.stream()
                .filter(event -> event.level() == EngineLogger.Level.ERROR)
                .findFirst()
                .orElseThrow();
        assertSame(CONTEXT, error.context());
        assertTrue(error.message().contains("1"));
        assertTrue(error.message().contains(second.stopFailure.getClass().getName()));
        assertTrue(error.message().contains("stop failed"));
    }

    @Test
    void closeFailureRemainsVisibleThroughRegistryVerificationAndDoesNotPreventTermination() {
        List<String> trace = new ArrayList<>();
        NativeResourceRegistry registry = new NativeResourceRegistry();
        ProbeSubsystem subsystem = new ProbeSubsystem("one", registry, trace, 7L);
        RuntimeException releaseFailure = new IllegalStateException("release failed");
        subsystem.resourceCloseFailure = releaseFailure;
        start(subsystem);
        trace.clear();

        List<EngineLogger.Event> events = new ArrayList<>();
        EngineLogger logger = new EngineLogger(new CapturingSink(events, trace));
        TerminationSignal signal = new TerminationSignal();
        FatalTermination fatal = new FatalTermination(logger, status -> {
            trace.add("terminate:" + status);
            throw signal;
        });

        TerminationSignal actual = assertThrows(
                TerminationSignal.class,
                () -> fatal.terminate("fatal", CONTEXT, List.of(subsystem), registry));

        assertSame(signal, actual);
        assertEquals(2, actual.getSuppressed().length);
        assertSame(releaseFailure, actual.getSuppressed()[0]);
        assertTrue(actual.getSuppressed()[1] instanceof IllegalStateException);
        assertTrue(actual.getSuppressed()[1].getMessage().contains("CLOSE_FAILED"));
        assertEquals(2, events.stream().filter(event -> event.level() == EngineLogger.Level.ERROR).count());
        assertTrue(trace.indexOf("one:resource-close") < trace.indexOf("flush"));
        assertTrue(trace.indexOf("flush") < trace.indexOf("terminate:1"));
    }

    @Test
    void fatalWriteRuntimeExceptionDoesNotPreventCleanupFlushOrTermination() {
        verifyInitialFatalWriteFailure(new IllegalStateException("fatal write runtime"));
    }

    @Test
    void fatalWriteErrorDoesNotPreventCleanupFlushOrTermination() {
        verifyInitialFatalWriteFailure(new AssertionError("fatal write error"));
    }

    @Test
    void cleanupErrorReportingFailureIsCapturedOnceWithoutRecursiveRetry() {
        List<String> trace = new ArrayList<>();
        NativeResourceRegistry registry = new NativeResourceRegistry();
        ProbeSubsystem subsystem = new ProbeSubsystem("one", registry, trace, 3L);
        subsystem.stopFailure = new IllegalStateException("stop failed");
        start(subsystem);
        trace.clear();

        RuntimeException reportingFailure = new RuntimeException("report failed");
        AtomicInteger errorAttempts = new AtomicInteger();
        EngineLogger logger = new EngineLogger(new EngineLogger.Sink() {
            @Override
            public void write(EngineLogger.Event event) {
                trace.add("log:" + event.level());
                if (event.level() == EngineLogger.Level.ERROR) {
                    errorAttempts.incrementAndGet();
                    throw reportingFailure;
                }
            }

            @Override
            public void flush() {
                trace.add("flush");
            }
        });
        TerminationSignal signal = new TerminationSignal();
        FatalTermination fatal = new FatalTermination(logger, status -> {
            trace.add("terminate:" + status);
            throw signal;
        });

        TerminationSignal actual = assertThrows(
                TerminationSignal.class,
                () -> fatal.terminate("fatal", CONTEXT, List.of(subsystem), registry));

        assertSame(signal, actual);
        assertEquals(1, errorAttempts.get());
        assertEquals(2, actual.getSuppressed().length);
        assertSame(subsystem.stopFailure, actual.getSuppressed()[0]);
        assertSame(reportingFailure, actual.getSuppressed()[1]);
        assertEquals(1, trace.stream().filter("log:ERROR"::equals).count());
        assertTrue(trace.indexOf("flush") < trace.indexOf("terminate:1"));
    }

    @Test
    void flushFailuresDoNotPreventTerminationAndPreserveSuppressedIdentity() {
        RuntimeException runtime = new RuntimeException("flush runtime");
        verifyFlushFailure(runtime);
        AssertionError error = new AssertionError("flush error");
        verifyFlushFailure(error);
    }

    @Test
    void terminatorRuntimeExceptionAndErrorPropagateByIdentityWithPriorFailuresSuppressed() {
        verifyTerminatorFailure(new IllegalStateException("terminate runtime"));
        verifyTerminatorFailure(new AssertionError("terminate error"));
    }

    @Test
    void returningTerminatorProducesTerminalIllegalStateExceptionWithPriorFailures() {
        List<String> trace = new ArrayList<>();
        NativeResourceRegistry registry = new NativeResourceRegistry();
        ProbeSubsystem subsystem = new ProbeSubsystem("one", registry, trace, 4L);
        subsystem.stopFailure = new IllegalStateException("stop failed");
        start(subsystem);
        trace.clear();
        EngineLogger logger = new EngineLogger(new TraceSink(trace));
        AtomicInteger terminations = new AtomicInteger();
        FatalTermination fatal = new FatalTermination(logger, status -> {
            trace.add("terminate:" + status);
            terminations.incrementAndGet();
        });

        IllegalStateException returned = assertThrows(
                IllegalStateException.class,
                () -> fatal.terminate("fatal", CONTEXT, List.of(subsystem), registry));

        assertTrue(returned.getMessage().contains("returned normally"));
        assertEquals(1, returned.getSuppressed().length);
        assertSame(subsystem.stopFailure, returned.getSuppressed()[0]);
        assertEquals(1, terminations.get());

        int traceSize = trace.size();
        assertThrows(
                IllegalStateException.class,
                () -> fatal.terminate("again", CONTEXT, List.of(subsystem), registry));
        assertEquals(traceSize, trace.size());
        assertEquals(1, terminations.get());
    }

    @Test
    void invalidArgumentsAndDuplicateSubsystemsFailBeforeAnySideEffect() {
        List<String> trace = new ArrayList<>();
        NativeResourceRegistry registry = new NativeResourceRegistry();
        ProbeSubsystem subsystem = new ProbeSubsystem("one", registry, trace, 5L);
        EngineLogger logger = new EngineLogger(new TraceSink(trace));
        AtomicInteger terminations = new AtomicInteger();
        FatalTermination fatal = new FatalTermination(logger, status -> terminations.incrementAndGet());

        assertThrows(NullPointerException.class, () -> fatal.terminate(null, CONTEXT, List.of(), registry));
        assertThrows(IllegalArgumentException.class, () -> fatal.terminate("   ", CONTEXT, List.of(), registry));
        assertThrows(NullPointerException.class, () -> fatal.terminate("fatal", null, List.of(), registry));
        assertThrows(NullPointerException.class, () -> fatal.terminate("fatal", CONTEXT, null, registry));
        assertThrows(
                NullPointerException.class,
                () -> fatal.terminate("fatal", CONTEXT, Arrays.asList(subsystem, null), registry));
        assertThrows(
                IllegalArgumentException.class,
                () -> fatal.terminate("fatal", CONTEXT, List.of(subsystem, subsystem), registry));
        assertThrows(NullPointerException.class, () -> fatal.terminate("fatal", CONTEXT, List.of(), null));

        assertEquals(List.of(), trace);
        assertEquals(0, terminations.get());
    }

    @Test
    void reentrantInvocationIsRejectedWithoutDuplicateCleanupOrTermination() {
        List<String> trace = new ArrayList<>();
        NativeResourceRegistry registry = new NativeResourceRegistry();
        EngineLogger logger = new EngineLogger(new TraceSink(trace));
        AtomicInteger terminations = new AtomicInteger();
        AtomicReference<FatalTermination> fatalRef = new AtomicReference<>();
        AtomicReference<Throwable> reentrantFailure = new AtomicReference<>();
        ProbeSubsystem subsystem = new ProbeSubsystem("one", registry, trace, 6L);
        subsystem.onStopCallback = () -> reentrantFailure.set(assertThrows(
                IllegalStateException.class,
                () -> fatalRef.get().terminate("nested", CONTEXT, List.of(subsystem), registry)));
        start(subsystem);
        trace.clear();

        FatalTermination fatal = new FatalTermination(logger, status -> {
            trace.add("terminate:" + status);
            terminations.incrementAndGet();
            throw new TerminationSignal();
        });
        fatalRef.set(fatal);

        assertThrows(
                TerminationSignal.class,
                () -> fatal.terminate("fatal", CONTEXT, List.of(subsystem), registry));

        assertTrue(reentrantFailure.get() instanceof IllegalStateException);
        assertEquals(1, terminations.get());
        assertEquals(1, trace.stream().filter("one:stop"::equals).count());
        assertEquals(1, trace.stream().filter("one:close"::equals).count());
    }

    @Test
    void laterInvocationAfterThrowingTerminatorIsRejectedWithoutSideEffects() {
        List<String> trace = new ArrayList<>();
        NativeResourceRegistry registry = new NativeResourceRegistry();
        ProbeSubsystem subsystem = new ProbeSubsystem("one", registry, trace, 8L);
        start(subsystem);
        trace.clear();
        EngineLogger logger = new EngineLogger(new TraceSink(trace));
        AtomicInteger terminations = new AtomicInteger();
        TerminationSignal signal = new TerminationSignal();
        FatalTermination fatal = new FatalTermination(logger, status -> {
            terminations.incrementAndGet();
            throw signal;
        });

        assertSame(
                signal,
                assertThrows(
                        TerminationSignal.class,
                        () -> fatal.terminate("fatal", CONTEXT, List.of(subsystem), registry)));
        int traceSize = trace.size();

        assertThrows(
                IllegalStateException.class,
                () -> fatal.terminate("again", CONTEXT, List.of(subsystem), registry));
        assertEquals(traceSize, trace.size());
        assertEquals(1, terminations.get());
    }

    @Test
    void publicConstructorTerminatesChildJvmOnlyAfterCleanupAndFlush(@TempDir Path tempDir) throws Exception {
        Path marker = tempDir.resolve("fatal-order.txt");
        String javaExecutable = Path.of(
                        System.getProperty("java.home"),
                        "bin",
                        isWindows() ? "java.exe" : "java")
                .toString();
        Process process = new ProcessBuilder(
                        javaExecutable,
                        "-cp",
                        System.getProperty("java.class.path"),
                        FatalTerminationChildProcess.class.getName(),
                        marker.toString())
                .redirectErrorStream(true)
                .start();

        boolean finished = process.waitFor(Duration.ofSeconds(15));
        if (!finished) {
            process.destroyForcibly();
        }

        assertTrue(finished, "child JVM must terminate within the bounded timeout");
        assertEquals(1, process.exitValue());
        assertEquals(
                List.of("fatal", "stop", "close", "resource-close", "flush"),
                Files.readAllLines(marker));
    }

    private static void verifyInitialFatalWriteFailure(Throwable failure) {
        List<String> trace = new ArrayList<>();
        NativeResourceRegistry registry = new NativeResourceRegistry();
        ProbeSubsystem subsystem = new ProbeSubsystem("one", registry, trace, 31L);
        start(subsystem);
        trace.clear();
        AtomicInteger writes = new AtomicInteger();
        EngineLogger logger = new EngineLogger(new EngineLogger.Sink() {
            @Override
            public void write(EngineLogger.Event event) {
                trace.add("log:" + event.level());
                if (writes.getAndIncrement() == 0) {
                    throwUnchecked(failure);
                }
            }

            @Override
            public void flush() {
                trace.add("flush");
            }
        });
        TerminationSignal signal = new TerminationSignal();
        FatalTermination fatal = new FatalTermination(logger, status -> {
            trace.add("terminate:" + status);
            registry.assertNoOpenResources();
            throw signal;
        });

        TerminationSignal actual = assertThrows(
                TerminationSignal.class,
                () -> fatal.terminate("fatal", CONTEXT, List.of(subsystem), registry));

        assertSame(signal, actual);
        assertEquals(1, actual.getSuppressed().length);
        assertSame(failure, actual.getSuppressed()[0]);
        assertEquals(2, writes.get());
        assertTrue(trace.contains("one:close"));
        assertTrue(trace.indexOf("flush") < trace.indexOf("terminate:1"));
    }

    private static void verifyFlushFailure(Throwable failure) {
        NativeResourceRegistry registry = new NativeResourceRegistry();
        EngineLogger logger = new EngineLogger(new EngineLogger.Sink() {
            @Override
            public void write(EngineLogger.Event event) {
            }

            @Override
            public void flush() {
                throwUnchecked(failure);
            }
        });
        TerminationSignal signal = new TerminationSignal();
        FatalTermination fatal = new FatalTermination(logger, status -> {
            assertEquals(1, status);
            throw signal;
        });

        TerminationSignal actual = assertThrows(
                TerminationSignal.class,
                () -> fatal.terminate("fatal", CONTEXT, List.of(), registry));

        assertSame(signal, actual);
        assertEquals(1, actual.getSuppressed().length);
        assertSame(failure, actual.getSuppressed()[0]);
    }

    private static void verifyTerminatorFailure(Throwable terminationFailure) {
        NativeResourceRegistry registry = new NativeResourceRegistry();
        RuntimeException earlier = new IllegalStateException("fatal write failed");
        EngineLogger logger = new EngineLogger(new EngineLogger.Sink() {
            @Override
            public void write(EngineLogger.Event event) {
                if (event.level() == EngineLogger.Level.FATAL) {
                    throw earlier;
                }
            }
        });
        FatalTermination fatal = new FatalTermination(logger, status -> throwUnchecked(terminationFailure));

        Throwable actual = assertThrows(
                terminationFailure instanceof Error ? Error.class : RuntimeException.class,
                () -> fatal.terminate("fatal", CONTEXT, List.of(), registry));

        assertSame(terminationFailure, actual);
        assertEquals(1, actual.getSuppressed().length);
        assertSame(earlier, actual.getSuppressed()[0]);
    }

    private static void start(ProbeSubsystem... subsystems) {
        for (ProbeSubsystem subsystem : subsystems) {
            subsystem.initialize();
            subsystem.start();
        }
    }

    private static void throwUnchecked(Throwable failure) {
        if (failure instanceof RuntimeException runtime) {
            throw runtime;
        }
        throw (Error) failure;
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    private static class TraceSink implements EngineLogger.Sink {
        protected final List<String> trace;

        TraceSink(List<String> trace) {
            this.trace = trace;
        }

        @Override
        public void write(EngineLogger.Event event) {
            trace.add("log:" + event.level() + ":" + event.message());
        }

        @Override
        public void flush() {
            trace.add("flush");
        }
    }

    private static final class CapturingSink extends TraceSink {
        private final List<EngineLogger.Event> events;

        CapturingSink(List<EngineLogger.Event> events, List<String> trace) {
            super(trace);
            this.events = events;
        }

        @Override
        public void write(EngineLogger.Event event) {
            events.add(event);
            super.write(event);
        }
    }

    private static final class ProbeSubsystem extends EngineSubsystem {
        private final String name;
        private final NativeResourceRegistry registry;
        private final List<String> trace;
        private final long handle;
        private final AtomicInteger resourceCloses = new AtomicInteger();
        private NativeResourceRegistry.Registration resource;
        private Throwable stopFailure;
        private Throwable resourceCloseFailure;
        private Runnable onStopCallback;

        ProbeSubsystem(String name, NativeResourceRegistry registry, List<String> trace, long handle) {
            this.name = name;
            this.registry = registry;
            this.trace = trace;
            this.handle = handle;
        }

        @Override
        protected void onInitialize() {
            resource = registry.register(name, handle, () -> {
                trace.add(name + ":resource-close");
                resourceCloses.incrementAndGet();
                if (resourceCloseFailure != null) {
                    throwUnchecked(resourceCloseFailure);
                }
            });
        }

        @Override
        protected void onStart() {
        }

        @Override
        protected void onStop() {
            trace.add(name + ":stop");
            if (onStopCallback != null) {
                onStopCallback.run();
            }
            if (stopFailure != null) {
                throwUnchecked(stopFailure);
            }
        }

        @Override
        protected void onClose() {
            trace.add(name + ":close");
            resource.close();
        }
    }

    private static final class TerminationSignal extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}
