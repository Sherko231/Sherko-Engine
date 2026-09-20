package com.samo.engine.core.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class Phase2IntegratedGateTest {
    private static final String ENABLE_ENV = "SHERKO_P2_EXIT_GATE";
    private static final long REQUIRED_DURATION_NANOS = Duration.ofSeconds(60).toNanos();
    private static final long INJECTED_STALL_MILLIS = 2_000L;
    private static final Path REPORT_PATH = Path.of("build", "reports", "phase2", "p2-exit-60-second-gate.txt");

    @Test
    void runsIntegratedHeadlessLoopForSixtySecondsWithBoundedCatchUpAndVerifiedCleanup() throws Exception {

        assumeTrue(Boolean.parseBoolean(System.getenv(ENABLE_ENV)), () -> "Set " + ENABLE_ENV + "=true to run the 60-second Phase 2 exit gate");

        List<String> lifecycleTrace = new ArrayList<>();
        NativeResourceRegistry registry = new NativeResourceRegistry();
        GateSubsystem subsystem = new GateSubsystem(registry, lifecycleTrace);
        List<EngineSubsystem> initializationOrder = List.of(subsystem);

        SubsystemStartupCoordinator.start(initializationOrder);

        EngineClock clock = new EngineClock();
        FixedStepAccumulator accumulator = new FixedStepAccumulator();
        FixedStepCatchUpPolicy catchUpPolicy = new FixedStepCatchUpPolicy();

        long totalTicks = 0L;
        long loopUpdates = 0L;
        long maxStepsObserved = 0L;
        long stallElapsedNanos = -1L;
        long stallSteps = -1L;
        boolean stallInjected = false;
        Throwable loopFailure = null;

        long wallStart = System.nanoTime();
        clock.sampleElapsedNanos();
        try {
            while (System.nanoTime() - wallStart < REQUIRED_DURATION_NANOS) {
                long wallElapsed = System.nanoTime() - wallStart;
                if (!stallInjected && wallElapsed >= Duration.ofSeconds(5).toNanos()) {
                    Thread.sleep(INJECTED_STALL_MILLIS);
                    stallInjected = true;
                } else {
                    Thread.sleep(1L);
                }

                long elapsedNanos = clock.sampleElapsedNanos();
                long steps = catchUpPolicy.advance(accumulator, elapsedNanos);
                loopUpdates++;
                maxStepsObserved = Math.max(maxStepsObserved, steps);

                if (stallInjected && stallElapsedNanos < 0L && elapsedNanos >= Duration.ofSeconds(1).toNanos()) {
                    stallElapsedNanos = elapsedNanos;
                    stallSteps = steps;
                }

                for (long step = 0L; step < steps; step++) {
                    totalTicks++;
                }
            }
        } catch (RuntimeException | Error failure) {
            loopFailure = failure;
            throw failure;
        } finally {
            try {
                shutdownReverse(initializationOrder);
                registry.assertNoOpenResources();
            } catch (RuntimeException | Error cleanupFailure) {
                if (loopFailure != null && cleanupFailure != loopFailure) {
                    loopFailure.addSuppressed(cleanupFailure);
                } else {
                    throw cleanupFailure;
                }
            }
        }

        long wallDurationNanos = System.nanoTime() - wallStart;

        assertTrue(wallDurationNanos >= REQUIRED_DURATION_NANOS, "integrated gate must run for at least 60 continuous seconds");
        assertTrue(stallInjected, "the bounded catch-up stimulus must execute");
        assertTrue(stallElapsedNanos >= Duration.ofSeconds(1).toNanos(), "the injected stall must be visible through EngineClock");
        assertEquals(FixedStepCatchUpPolicy.DEFAULT_MAX_STEPS_PER_UPDATE, stallSteps, "the two-second stall must be capped to the default per-update step limit");
        assertTrue(maxStepsObserved <= FixedStepCatchUpPolicy.DEFAULT_MAX_STEPS_PER_UPDATE, "no update may expose more than the bounded catch-up limit");
        assertTrue(totalTicks > 0L, "the integrated loop must execute fixed simulation ticks");
        assertTrue(loopUpdates > totalTicks, "the headless loop should sample more often than the 60 Hz fixed simulation executes");
        assertEquals(List.of("initialize", "start", "stop", "close", "resource-close"), lifecycleTrace);
        assertTrue(subsystem.resourceClosed, "the registered resource must close during owner cleanup");
        assertFalse(subsystem.resourceCloseRepeated, "the registered resource must close exactly once");

        writeReport(new GateEvidence(wallDurationNanos, loopUpdates, totalTicks, maxStepsObserved, stallElapsedNanos, stallSteps, lifecycleTrace));

    }

    private static void shutdownReverse(List<EngineSubsystem> initializationOrder) {

        for (int index = initializationOrder.size() - 1; index >= 0; index--) {
            EngineSubsystem subsystem = initializationOrder.get(index);
            subsystem.stop();
            subsystem.close();
        }

    }

    private static void writeReport(GateEvidence evidence) throws IOException {

        Files.createDirectories(REPORT_PATH.getParent());
        String commit = environmentOr("GITHUB_SHA", "unknown");
        List<String> lines = List.of("gate=P2 integrated exit", "result=PASS", "configured.duration.seconds=60", "observed.duration.nanos=" + evidence.wallDurationNanos(),
            "observed.duration.seconds=" + String.format(Locale.ROOT, "%.3f", evidence.wallDurationNanos() / 1_000_000_000.0),
            "fixed.tick.rate.hz=" + FixedStepAccumulator.TICKS_PER_SECOND, "fixed.tick.execution.integer.steps.only=true", "executed.fixed.ticks=" + evidence.totalTicks(),
            "loop.updates=" + evidence.loopUpdates(), "catchup.max.steps.configured=" + FixedStepCatchUpPolicy.DEFAULT_MAX_STEPS_PER_UPDATE,
            "catchup.max.steps.observed=" + evidence.maxStepsObserved(), "stall.requested.millis=" + INJECTED_STALL_MILLIS,
            "stall.observed.elapsed.nanos=" + evidence.stallElapsedNanos(), "stall.exposed.steps=" + evidence.stallSteps(),
            "lifecycle.trace=" + String.join(",", evidence.lifecycleTrace()), "native.resource.registry.empty.after.cleanup=true", "engine.commit=" + commit,
            "java.version=" + System.getProperty("java.version"), "os.name=" + System.getProperty("os.name"), "os.arch=" + System.getProperty("os.arch"),
            "evidence.scope=Java headless integration correctness; not native soak/stability evidence", "p0.t13.p0.t14.replaced=false");
        Files.write(REPORT_PATH, lines, StandardCharsets.UTF_8);

    }

    private static String environmentOr(String key, String fallback) {

        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value;

    }

    private record GateEvidence(long wallDurationNanos, long loopUpdates, long totalTicks, long maxStepsObserved, long stallElapsedNanos, long stallSteps,
        List<String> lifecycleTrace) {
    }

    private static final class GateSubsystem extends EngineSubsystem {
        private final NativeResourceRegistry registry;
        private final List<String> trace;
        private NativeResourceRegistry.Registration registration;
        private boolean resourceClosed;
        private boolean resourceCloseRepeated;

        private GateSubsystem(NativeResourceRegistry registry, List<String> trace) {

            this.registry = registry;
            this.trace = trace;

        }

        @Override
        protected void onInitialize() {

            trace.add("initialize");
            registration = registry.register("phase2-gate", 135L, () -> {
                if (resourceClosed) {
                    resourceCloseRepeated = true;
                }
                resourceClosed = true;
                trace.add("resource-close");
            });

        }

        @Override
        protected void onStart() {

            trace.add("start");

        }

        @Override
        protected void onStop() {

            trace.add("stop");

        }

        @Override
        protected void onClose() {

            trace.add("close");
            registration.close();

        }
    }
}
