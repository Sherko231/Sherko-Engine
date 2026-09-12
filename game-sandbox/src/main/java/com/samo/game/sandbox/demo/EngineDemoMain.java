package com.samo.game.sandbox.demo;

import com.samo.engine.core.api.EngineClock;
import com.samo.engine.core.api.EngineLogger;
import com.samo.engine.core.api.FixedStepAccumulator;
import com.samo.engine.core.api.FixedStepCatchUpPolicy;
import com.samo.engine.core.api.NativeResourceRegistry;
import com.samo.engine.platform.api.GlfwWindow;
import com.samo.engine.platform.api.InputKey;
import com.samo.engine.platform.api.InputSnapshot;
import com.samo.engine.platform.api.WindowMode;
import com.samo.engine.platform.api.WindowSizeListener;
import java.util.List;

/** Runs the owner-facing scripted engine sandbox through production public APIs only. */
public final class EngineDemoMain {
    private static final long DIAGNOSTIC_INTERVAL_NANOS = EngineDemoTimeline.SECOND_NANOS;
    private static final String SANDBOX_SUBSYSTEM = "game-sandbox";

    private EngineDemoMain() {
    }

    public static void main(String[] args) throws InterruptedException {
        printTimeline();

        EngineLogger logger = new EngineLogger(event -> {
            Long simulationTick = event.context().simulationTick();
            if (simulationTick == null) {
                System.out.printf(
                        "[%s] [%s] %s%n",
                        event.level(),
                        event.context().subsystem(),
                        event.message());
            } else {
                System.out.printf(
                        "[%s] [%s] [tick=%d] %s%n",
                        event.level(),
                        event.context().subsystem(),
                        simulationTick,
                        event.message());
            }
        });
        NativeResourceRegistry nativeResources = new NativeResourceRegistry();
        WindowSizeListener sizeListener = new WindowSizeListener() {
            @Override
            public void onLogicalWindowSizeChanged(int width, int height) {
                log(logger, EngineLogger.Level.INFO, "Logical window size changed to %dx%d".formatted(width, height));
            }

            @Override
            public void onFramebufferSizeChanged(int width, int height) {
                log(logger, EngineLogger.Level.INFO, "Framebuffer size changed to %dx%d".formatted(width, height));
            }
        };

        GlfwWindow window = new GlfwWindow(
                1280,
                720,
                "Sherko Engine Sandbox",
                logger,
                nativeResources,
                sizeListener);

        boolean started = false;
        Throwable primaryFailure = null;
        try {
            window.initialize();
            window.start();
            started = true;
            runDemo(window, logger);
        } catch (InterruptedException failure) {
            primaryFailure = failure;
            Thread.currentThread().interrupt();
            throw failure;
        } catch (RuntimeException | Error failure) {
            primaryFailure = failure;
            throw failure;
        } finally {
            cleanup(window, nativeResources, logger, started, primaryFailure);
        }
    }

    private static void runDemo(GlfwWindow window, EngineLogger logger) throws InterruptedException {
        EngineClock clock = new EngineClock();
        FixedStepAccumulator accumulator = new FixedStepAccumulator();
        FixedStepCatchUpPolicy catchUpPolicy = new FixedStepCatchUpPolicy();
        List<EngineDemoTimeline.Step> steps = EngineDemoTimeline.steps();

        long elapsedDemoNanos = 0L;
        long cumulativeTicks = 0L;
        long nextDiagnosticNanos = DIAGNOSTIC_INTERVAL_NANOS;
        long inputFrameId = 0L;
        double diagnosticMouseDeltaX = 0.0;
        double diagnosticMouseDeltaY = 0.0;
        InputSnapshot latestInput = null;
        int nextStep = 0;

        clock.sampleElapsedNanos();
        while (elapsedDemoNanos < EngineDemoTimeline.DEMO_DURATION_NANOS) {
            long elapsedNanos = clock.sampleElapsedNanos();
            elapsedDemoNanos = saturatingAdd(elapsedDemoNanos, elapsedNanos);
            cumulativeTicks += catchUpPolicy.advance(accumulator, elapsedNanos);

            window.pollEvents();
            latestInput = window.captureInputSnapshot(inputFrameId++);
            diagnosticMouseDeltaX += latestInput.mouseDeltaX();
            diagnosticMouseDeltaY += latestInput.mouseDeltaY();

            while (nextStep < steps.size() && elapsedDemoNanos >= steps.get(nextStep).atNanos()) {
                execute(window, logger, steps.get(nextStep).action(), cumulativeTicks);
                nextStep++;
            }

            if (elapsedDemoNanos >= nextDiagnosticNanos) {
                log(
                        logger,
                        EngineLogger.Level.DEBUG,
                        "t=%.1fs, interpolationAlpha=%.3f, inputFrame=%d, focused=%s, cursorCaptured=%s, "
                                + "WASD=[%s,%s,%s,%s], mouseDeltaSinceLastDiagnostic=(%.2f,%.2f) "
                                + "(sandbox diagnostic; not FPS/benchmark evidence)"
                                .formatted(
                                        elapsedDemoNanos / 1_000_000_000.0,
                                        accumulator.interpolationAlpha(),
                                        latestInput.frameId(),
                                        latestInput.focused(),
                                        latestInput.cursorCaptured(),
                                        latestInput.keyHeld(InputKey.W),
                                        latestInput.keyHeld(InputKey.A),
                                        latestInput.keyHeld(InputKey.S),
                                        latestInput.keyHeld(InputKey.D),
                                        diagnosticMouseDeltaX,
                                        diagnosticMouseDeltaY),
                        cumulativeTicks);
                diagnosticMouseDeltaX = 0.0;
                diagnosticMouseDeltaY = 0.0;
                do {
                    nextDiagnosticNanos += DIAGNOSTIC_INTERVAL_NANOS;
                } while (nextDiagnosticNanos <= elapsedDemoNanos);
            }

            Thread.sleep(5L);
        }
    }

    private static void execute(
            GlfwWindow window,
            EngineLogger logger,
            EngineDemoTimeline.Action action,
            long simulationTick) {
        switch (action) {
            case BORDERLESS_FULLSCREEN -> {
                window.setWindowMode(WindowMode.BORDERLESS_FULLSCREEN);
                log(logger, EngineLogger.Level.INFO, "Window mode changed to BORDERLESS_FULLSCREEN", simulationTick);
            }
            case WINDOWED -> {
                window.setWindowMode(WindowMode.WINDOWED);
                log(logger, EngineLogger.Level.INFO, "Window mode changed to WINDOWED", simulationTick);
            }
            case EXCLUSIVE_FULLSCREEN -> {
                window.setWindowMode(WindowMode.EXCLUSIVE_FULLSCREEN);
                log(logger, EngineLogger.Level.INFO, "Window mode changed to EXCLUSIVE_FULLSCREEN", simulationTick);
            }
            case CAPTURE_CURSOR -> {
                window.setCursorCaptured(true);
                log(logger, EngineLogger.Level.INFO, "Cursor capture enabled", simulationTick);
                System.out.println(
                        "[sandbox instruction] Move the mouse / hold W-A-S-D, then Alt+Tab away and back: snapshot diagnostics should show input and capture should not auto-return.");
            }
            case RELEASE_CURSOR -> {
                window.setCursorCaptured(false);
                log(logger, EngineLogger.Level.INFO, "Cursor capture disabled", simulationTick);
            }
            case SHUTDOWN -> log(logger, EngineLogger.Level.INFO, "Scripted orderly shutdown requested", simulationTick);
        }
    }

    private static void cleanup(
            GlfwWindow window,
            NativeResourceRegistry nativeResources,
            EngineLogger logger,
            boolean started,
            Throwable primaryFailure) {
        Throwable cleanupFailure = null;

        if (started) {
            cleanupFailure = attempt(cleanupFailure, () -> window.setCursorCaptured(false));
            cleanupFailure = attempt(cleanupFailure, () -> window.setWindowMode(WindowMode.WINDOWED));
            cleanupFailure = attempt(cleanupFailure, window::stop);
        }
        cleanupFailure = attempt(cleanupFailure, window::close);
        cleanupFailure = attempt(cleanupFailure, nativeResources::assertNoOpenResources);

        if (cleanupFailure == null) {
            log(logger, EngineLogger.Level.INFO, "Sandbox shutdown completed; native resource registry is empty");
            return;
        }
        if (primaryFailure != null) {
            if (cleanupFailure != primaryFailure) {
                primaryFailure.addSuppressed(cleanupFailure);
            }
            return;
        }
        rethrow(cleanupFailure);
    }

    private static Throwable attempt(Throwable accumulated, Runnable action) {
        try {
            action.run();
            return accumulated;
        } catch (RuntimeException | Error failure) {
            if (accumulated == null) {
                return failure;
            }
            if (failure != accumulated) {
                accumulated.addSuppressed(failure);
            }
            return accumulated;
        }
    }

    private static void rethrow(Throwable failure) {
        if (failure instanceof RuntimeException runtimeFailure) {
            throw runtimeFailure;
        }
        throw (Error) failure;
    }

    private static void log(EngineLogger logger, EngineLogger.Level level, String message) {
        logger.log(level, message, sandboxContext(null));
    }

    private static void log(EngineLogger logger, EngineLogger.Level level, String message, long simulationTick) {
        logger.log(level, message, sandboxContext(simulationTick));
    }

    private static EngineLogger.Context sandboxContext(Long simulationTick) {
        return new EngineLogger.Context(null, simulationTick, SANDBOX_SUBSYSTEM, null, null);
    }

    private static long saturatingAdd(long left, long right) {
        if (Long.MAX_VALUE - left < right) {
            return Long.MAX_VALUE;
        }
        return left + right;
    }

    private static void printTimeline() {
        System.out.println("Sherko Engine owner-facing sandbox demo");
        System.out.println(
                "Uses production public APIs only. Current window is intentionally visually empty until renderer work exists.");
        System.out.println("InputSnapshot is captured once per demo frame; bounded diagnostics print once per second.");
        System.out.println("Timeline:");
        System.out.println("  0-5s   WINDOWED: resize/DPI observation");
        System.out.println("  5s     BORDERLESS_FULLSCREEN");
        System.out.println(" 10s     WINDOWED");
        System.out.println(" 15s     EXCLUSIVE_FULLSCREEN");
        System.out.println(" 20s     WINDOWED");
        System.out.println(" 25s     cursor capture ON; move mouse / hold W-A-S-D / manually Alt+Tab away/back");
        System.out.println(" 34s     cursor capture OFF");
        System.out.println(" 38s     shutdown + native-resource leak assertion");
        System.out.println();
    }
}
