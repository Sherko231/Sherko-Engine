package com.samo.engine.core.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FatalTerminationCoordinatorTest {
    private static final EngineLogger.Context CONTEXT = new EngineLogger.Context(12L, 34L, "core", null, null);

    @Test
    void happyPathLogsCleansReverseOrderVerifiesFlushesThenTerminates() {
        List<String> trace = new ArrayList<>();
        NativeResourceRegistry registry = new NativeResourceRegistry();
        ProbeSubsystem first = new ProbeSubsystem("first", registry, trace, 11L);
        ProbeSubsystem second = new ProbeSubsystem("second", registry, trace, 22L);
        start(first, second);
        trace.clear();

        EngineLogger logger = new EngineLogger(new TraceSink(trace));
        TerminationSignal signal = new TerminationSignal();
        FatalTerminationCoordinator fatal = new FatalTerminationCoordinator(logger, status -> {
            trace.add("terminate:" + status);
            registry.assertNoOpenResources();
            throw signal;
        });

        assertSame(signal, assertThrows(TerminationSignal.class, () -> fatal.terminate("fatal", CONTEXT, List.of(first, second), registry)));

        assertEquals(
            List.of("log:FATAL:fatal", "second:stop", "second:close", "second:resource-close", "first:stop", "first:close", "first:resource-close", "flush", "terminate:1"), trace);
        assertEquals(1, first.resourceCloses.get());
        assertEquals(1, second.resourceCloses.get());
    }

    @Test
    void stopFailureStillClosesAndReportsBeforeFlushAndTermination() {
        List<String> trace = new ArrayList<>();
        List<EngineLogger.Event> events = new ArrayList<>();
        NativeResourceRegistry registry = new NativeResourceRegistry();
        ProbeSubsystem first = new ProbeSubsystem("first", registry, trace, 1L);
        ProbeSubsystem second = new ProbeSubsystem("second", registry, trace, 2L);
        second.stopFailure = new IllegalStateException("stop failed");
        start(first, second);
        trace.clear();

        EngineLogger logger = new EngineLogger(new CapturingSink(events, trace));
        TerminationSignal signal = new TerminationSignal();
        FatalTerminationCoordinator fatal = new FatalTerminationCoordinator(logger, status -> {
            trace.add("terminate:" + status);
            throw signal;
        });

        TerminationSignal actual = assertThrows(TerminationSignal.class, () -> fatal.terminate("fatal", CONTEXT, List.of(first, second), registry));

        assertSame(signal, actual);
        assertEquals(1, actual.getSuppressed().length);
        assertSame(second.stopFailure, actual.getSuppressed()[0]);
        assertTrue(trace.indexOf("second:stop") < trace.indexOf("second:close"));
        assertTrue(trace.indexOf("second:close") < trace.indexOf("first:stop"));
        assertTrue(trace.indexOf("first:close") < trace.indexOf("flush"));
        assertEquals(1, first.resourceCloses.get());
        assertEquals(1, second.resourceCloses.get());

        EngineLogger.Event error = events.stream().filter(event -> event.level() == EngineLogger.Level.ERROR).findFirst().orElseThrow();
        assertSame(CONTEXT, error.context());
        assertTrue(error.message().contains("1"));
        assertTrue(error.message().contains(second.stopFailure.getClass().getName()));
        assertTrue(error.message().contains("stop failed"));
    }

    @Test
    void closeFailureRemainsTrackedAndRegistryFailureAlsoReachesTerminator() {
        List<String> trace = new ArrayList<>();
        List<EngineLogger.Event> events = new ArrayList<>();
        NativeResourceRegistry registry = new NativeResourceRegistry();
        ProbeSubsystem subsystem = new ProbeSubsystem("one", registry, trace, 7L);
        RuntimeException releaseFailure = new IllegalStateException("release failed");
        subsystem.resourceCloseFailure = releaseFailure;
        start(subsystem);
        trace.clear();

        EngineLogger logger = new EngineLogger(new CapturingSink(events, trace));
        TerminationSignal signal = new TerminationSignal();
        FatalTerminationCoordinator fatal = new FatalTerminationCoordinator(logger, status -> {
            trace.add("terminate:" + status);
            throw signal;
        });

        TerminationSignal actual = assertThrows(TerminationSignal.class, () -> fatal.terminate("fatal", CONTEXT, List.of(subsystem), registry));

        assertEquals(2, actual.getSuppressed().length);
        assertSame(releaseFailure, actual.getSuppressed()[0]);
        assertTrue(actual.getSuppressed()[1] instanceof IllegalStateException);
        assertTrue(actual.getSuppressed()[1].getMessage().contains("CLOSE_FAILED"));
        assertEquals(2, events.stream().filter(event -> event.level() == EngineLogger.Level.ERROR).count());
        assertTrue(trace.indexOf("flush") < trace.indexOf("terminate:1"));
    }

    @Test
    void initialFatalWriteRuntimeExceptionAndErrorDoNotSkipCleanupOrTermination() {
        verifyInitialFatalWriteFailure(new IllegalStateException("fatal write runtime"));
        verifyInitialFatalWriteFailure(new AssertionError("fatal write error"));
    }

    @Test
    void errorReportingFailureIsCapturedOnceWithoutRecursiveRetry() {
        List<String> trace = new ArrayList<>();
        NativeResourceRegistry registry = new NativeResourceRegistry();
        ProbeSubsystem subsystem = new ProbeSubsystem("one", registry, trace, 3L);
        subsystem.stopFailure = new IllegalStateException("stop failed");
        start(subsystem);
        trace.clear();

        RuntimeException reportFailure = new RuntimeException("report failed");
        AtomicInteger errorWrites = new AtomicInteger();
        EngineLogger logger = new EngineLogger(new EngineLogger.Sink() {
            @Override
            public void write(EngineLogger.Event event) {
                trace.add("log:" + event.level());
                if (event.level() == EngineLogger.Level.ERROR) {
                    errorWrites.incrementAndGet();
                    throw reportFailure;
                }
            }

            @Override
            public void flush() {
                trace.add("flush");
            }
        });
        TerminationSignal signal = new TerminationSignal();
        FatalTerminationCoordinator fatal = new FatalTerminationCoordinator(logger, status -> {
            trace.add("terminate:" + status);
            throw signal;
        });

        TerminationSignal actual = assertThrows(TerminationSignal.class, () -> fatal.terminate("fatal", CONTEXT, List.of(subsystem), registry));

        assertEquals(1, errorWrites.get());
        assertEquals(2, actual.getSuppressed().length);
        assertSame(subsystem.stopFailure, actual.getSuppressed()[0]);
        assertSame(reportFailure, actual.getSuppressed()[1]);
        assertTrue(trace.indexOf("flush") < trace.indexOf("terminate:1"));
    }

    @Test
    void flushRuntimeExceptionAndErrorDoNotPreventTermination() {
        verifyFlushFailure(new IllegalStateException("flush runtime"));
        verifyFlushFailure(new AssertionError("flush error"));
    }

    @Test
    void terminatorRuntimeExceptionAndErrorPropagateByIdentityWithPriorFailuresSuppressed() {
        verifyTerminatorFailure(new IllegalStateException("terminate runtime"));
        verifyTerminatorFailure(new AssertionError("terminate error"));
    }

    @Test
    void returningTerminatorIsTerminalAndCarriesPriorFailures() {
        List<String> trace = new ArrayList<>();
        NativeResourceRegistry registry = new NativeResourceRegistry();
        ProbeSubsystem subsystem = new ProbeSubsystem("one", registry, trace, 4L);
        subsystem.stopFailure = new IllegalStateException("stop failed");
        start(subsystem);
        trace.clear();
        AtomicInteger terminations = new AtomicInteger();
        FatalTerminationCoordinator fatal = new FatalTerminationCoordinator(new EngineLogger(new TraceSink(trace)), status -> {
            trace.add("terminate:" + status);
            terminations.incrementAndGet();
        });

        IllegalStateException returned = assertThrows(IllegalStateException.class, () -> fatal.terminate("fatal", CONTEXT, List.of(subsystem), registry));

        assertTrue(returned.getMessage().contains("returned normally"));
        assertEquals(1, returned.getSuppressed().length);
        assertSame(subsystem.stopFailure, returned.getSuppressed()[0]);
        int traceSize = trace.size();

        assertThrows(IllegalStateException.class, () -> fatal.terminate("again", CONTEXT, List.of(subsystem), registry));
        assertEquals(traceSize, trace.size());
        assertEquals(1, terminations.get());
    }

    @Test
    void invalidInputsAndDuplicateSubsystemsFailBeforeAnySideEffect() {
        List<String> trace = new ArrayList<>();
        NativeResourceRegistry registry = new NativeResourceRegistry();
        ProbeSubsystem subsystem = new ProbeSubsystem("one", registry, trace, 5L);
        AtomicInteger terminations = new AtomicInteger();
        FatalTerminationCoordinator fatal = new FatalTerminationCoordinator(new EngineLogger(new TraceSink(trace)), status -> terminations.incrementAndGet());

        assertThrows(NullPointerException.class, () -> fatal.terminate(null, CONTEXT, List.of(), registry));
        assertThrows(IllegalArgumentException.class, () -> fatal.terminate("   ", CONTEXT, List.of(), registry));
        assertThrows(NullPointerException.class, () -> fatal.terminate("fatal", null, List.of(), registry));
        assertThrows(NullPointerException.class, () -> fatal.terminate("fatal", CONTEXT, null, registry));
        assertThrows(NullPointerException.class, () -> fatal.terminate("fatal", CONTEXT, Arrays.asList(subsystem, null), registry));
        assertThrows(IllegalArgumentException.class, () -> fatal.terminate("fatal", CONTEXT, List.of(subsystem, subsystem), registry));
        assertThrows(NullPointerException.class, () -> fatal.terminate("fatal", CONTEXT, List.of(), null));

        assertEquals(List.of(), trace);
        assertEquals(0, terminations.get());
    }

    @Test
    void reentrantAndLaterCallsCannotDuplicateCleanupOrTermination() {
        List<String> trace = new ArrayList<>();
        NativeResourceRegistry registry = new NativeResourceRegistry();
        EngineLogger logger = new EngineLogger(new TraceSink(trace));
        AtomicReference<FatalTerminationCoordinator> fatalRef = new AtomicReference<>();
        AtomicReference<Throwable> nestedFailure = new AtomicReference<>();
        AtomicInteger terminations = new AtomicInteger();
        ProbeSubsystem subsystem = new ProbeSubsystem("one", registry, trace, 6L);
        subsystem.onStopCallback = () -> nestedFailure
            .set(assertThrows(IllegalStateException.class, () -> fatalRef.get().terminate("nested", CONTEXT, List.of(subsystem), registry)));
        start(subsystem);
        trace.clear();

        FatalTerminationCoordinator fatal = new FatalTerminationCoordinator(logger, status -> {
            terminations.incrementAndGet();
            throw new TerminationSignal();
        });
        fatalRef.set(fatal);

        assertThrows(TerminationSignal.class, () -> fatal.terminate("fatal", CONTEXT, List.of(subsystem), registry));
        int traceSize = trace.size();

        assertTrue(nestedFailure.get() instanceof IllegalStateException);
        assertEquals(1, trace.stream().filter("one:stop"::equals).count());
        assertEquals(1, trace.stream().filter("one:close"::equals).count());
        assertThrows(IllegalStateException.class, () -> fatal.terminate("later", CONTEXT, List.of(subsystem), registry));
        assertEquals(traceSize, trace.size());
        assertEquals(1, terminations.get());
    }

    @Test
    void concurrentSecondCallIsRejectedWhileFirstOwnsFatalSequence() throws Exception {
        NativeResourceRegistry registry = new NativeResourceRegistry();
        CountDownLatch fatalWriteEntered = new CountDownLatch(1);
        CountDownLatch releaseFatalWrite = new CountDownLatch(1);
        AtomicInteger terminations = new AtomicInteger();
        AtomicReference<Throwable> firstResult = new AtomicReference<>();
        EngineLogger logger = new EngineLogger(event -> {
            if (event.level() == EngineLogger.Level.FATAL) {
                fatalWriteEntered.countDown();
                await(releaseFatalWrite);
            }
        });
        FatalTerminationCoordinator fatal = new FatalTerminationCoordinator(logger, status -> {
            terminations.incrementAndGet();
            throw new TerminationSignal();
        });

        Thread first = Thread.ofPlatform().start(() -> {
            try {
                fatal.terminate("first", CONTEXT, List.of(), registry);
            } catch (Throwable failure) {
                firstResult.set(failure);
            }
        });
        assertTrue(fatalWriteEntered.await(5, TimeUnit.SECONDS));

        assertThrows(IllegalStateException.class, () -> fatal.terminate("second", CONTEXT, List.of(), registry));
        releaseFatalWrite.countDown();
        first.join(5_000L);

        assertTrue(firstResult.get() instanceof TerminationSignal);
        assertEquals(1, terminations.get());
    }

    @Test
    void publicConstructorExitsChildJvmOnlyAfterResourceCleanupAndFlush(@TempDir Path tempDir) throws Exception {
        Path marker = tempDir.resolve("fatal-order.txt");
        String javaExecutable = Path.of(System.getProperty("java.home"), "bin", isWindows() ? "java.exe" : "java").toString();
        String childClasspath = String.join(File.pathSeparator, codeSourcePath(FatalTerminationCoordinatorChildProcess.class), codeSourcePath(FatalTerminationCoordinator.class));
        Process process = new ProcessBuilder(javaExecutable, "-cp", childClasspath, FatalTerminationCoordinatorChildProcess.class.getName(), marker.toString())
            .redirectErrorStream(true).start();

        boolean finished = process.waitFor(15, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
        }

        assertTrue(finished, "child JVM must terminate within the bounded timeout");
        assertEquals(1, process.exitValue());
        assertEquals(List.of("fatal", "stop", "close", "resource-close", "flush"), Files.readAllLines(marker));
    }

    private static String codeSourcePath(Class<?> type) throws Exception {
        return Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
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
        FatalTerminationCoordinator fatal = new FatalTerminationCoordinator(logger, status -> {
            trace.add("terminate:" + status);
            registry.assertNoOpenResources();
            throw signal;
        });

        TerminationSignal actual = assertThrows(TerminationSignal.class, () -> fatal.terminate("fatal", CONTEXT, List.of(subsystem), registry));

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
        FatalTerminationCoordinator fatal = new FatalTerminationCoordinator(logger, status -> {
            assertEquals(1, status);
            throw signal;
        });

        TerminationSignal actual = assertThrows(TerminationSignal.class, () -> fatal.terminate("fatal", CONTEXT, List.of(), registry));

        assertEquals(1, actual.getSuppressed().length);
        assertSame(failure, actual.getSuppressed()[0]);
    }

    private static void verifyTerminatorFailure(Throwable terminationFailure) {
        NativeResourceRegistry registry = new NativeResourceRegistry();
        RuntimeException earlier = new IllegalStateException("fatal write failed");
        EngineLogger logger = new EngineLogger(event -> {
            if (event.level() == EngineLogger.Level.FATAL) {
                throw earlier;
            }
        });
        FatalTerminationCoordinator fatal = new FatalTerminationCoordinator(logger, status -> throwUnchecked(terminationFailure));

        Throwable actual;
        if (terminationFailure instanceof Error) {
            actual = assertThrows(Error.class, () -> fatal.terminate("fatal", CONTEXT, List.of(), registry));
        } else {
            actual = assertThrows(RuntimeException.class, () -> fatal.terminate("fatal", CONTEXT, List.of(), registry));
        }

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

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("test latch timed out");
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(interrupted);
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
