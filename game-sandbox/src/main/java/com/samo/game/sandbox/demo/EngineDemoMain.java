package com.samo.game.sandbox.demo;

import com.samo.engine.core.api.EngineClock;
import com.samo.engine.core.api.EngineLogger;
import com.samo.engine.core.api.FixedStepAccumulator;
import com.samo.engine.core.api.FixedStepCatchUpPolicy;
import com.samo.engine.core.api.NativeResourceRegistry;
import com.samo.engine.platform.api.GlfwWindow;
import com.samo.engine.platform.api.WindowMode;
import com.samo.engine.platform.api.WindowSizeListener;
import java.util.List;

/** Runs the owner-facing scripted engine sandbox through production public APIs only. */
public final class EngineDemoMain {
    private static final long DIAGNOSTIC_INTERVAL_NANOS = EngineDemoTimeline.SECOND_NANOS;

    private EngineDemoMain() {
    }

    public static void main(String[] args) throws InterruptedException {
        printTimeline();

        EngineLogger logger = new EngineLogger(event -> System.out.printf(
                "[%s] [%s] %s%n",
                event.level(),
                event.context().subsystem(),
                event.message()));
        NativeResourceRegistry nativeResources = new NativeResourceRegistry();
        WindowSizeListener sizeListener = new WindowSizeListener() {
            @Override
            public void onLogicalWindowSizeChanged(int width, int height) {
                System.out.printf("[sandbox] logical window = %dx%d%n", width, height);
            }

            @Override
            public void onFramebufferSizeChanged(int width, int height) {
                System.out.printf("[sandbox] framebuffer = %dx%d%n", width, height);
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
            runDemo(window);
        } catch (InterruptedException failure) {
            primaryFailure = failure;
            Thread.currentThread().interrupt();
            throw failure;
        } catch (RuntimeException | Error failure) {
            primaryFailure = failure;
            throw failure;
        } finally {
            cleanup(window, nativeResources, started, primaryFailure);
        }
    }

    private static void runDemo(GlfwWindow window) throws InterruptedException {
        EngineClock clock = new EngineClock();
        FixedStepAccumulator accumulator = new FixedStepAccumulator();
        FixedStepCatchUpPolicy catchUpPolicy = new FixedStepCatchUpPolicy();
        List<EngineDemoTimeline.Step> steps = EngineDemoTimeline.steps();

        long elapsedDemoNanos = 0L;
        long cumulativeTicks = 0L;
        long nextDiagnosticNanos = DIAGNOSTIC_INTERVAL_NANOS;
        int nextStep = 0;

        clock.sampleElapsedNanos();
        while (elapsedDemoNanos < EngineDemoTimeline.DEMO_DURATION_NANOS) {
            long elapsedNanos = clock.sampleElapsedNanos();
            elapsedDemoNanos = saturatingAdd(elapsedDemoNanos, elapsedNanos);
            cumulativeTicks += catchUpPolicy.advance(accumulator, elapsedNanos);

            window.pollEvents();

            while (nextStep < steps.size() && elapsedDemoNanos >= steps.get(nextStep).atNanos()) {
                execute(window, steps.get(nextStep).action());
                nextStep++;
            }

            if (elapsedDemoNanos >= nextDiagnosticNanos) {
                System.out.printf(
                        "[sandbox diagnostics] t=%.1fs, simulationTicks=%d, interpolationAlpha=%.3f (not FPS/benchmark evidence)%n",
                        elapsedDemoNanos / 1_000_000_000.0,
                        cumulativeTicks,
                        accumulator.interpolationAlpha());
                do {
                    nextDiagnosticNanos += DIAGNOSTIC_INTERVAL_NANOS;
                } while (nextDiagnosticNanos <= elapsedDemoNanos);
            }

            Thread.sleep(5L);
        }
    }

    private static void execute(GlfwWindow window, EngineDemoTimeline.Action action) {
        switch (action) {
            case BORDERLESS_FULLSCREEN -> {
                System.out.println("[sandbox] -> BORDERLESS_FULLSCREEN");
                window.setWindowMode(WindowMode.BORDERLESS_FULLSCREEN);
            }
            case WINDOWED -> {
                System.out.println("[sandbox] -> WINDOWED");
                window.setWindowMode(WindowMode.WINDOWED);
            }
            case EXCLUSIVE_FULLSCREEN -> {
                System.out.println("[sandbox] -> EXCLUSIVE_FULLSCREEN");
                window.setWindowMode(WindowMode.EXCLUSIVE_FULLSCREEN);
            }
            case CAPTURE_CURSOR -> {
                System.out.println("[sandbox] -> cursor capture ON");
                System.out.println(
                        "[sandbox] Alt+Tab away and back now: capture should release on focus loss and must not auto-recapture.");
                window.setCursorCaptured(true);
            }
            case RELEASE_CURSOR -> {
                System.out.println("[sandbox] -> cursor capture OFF");
                window.setCursorCaptured(false);
            }
            case SHUTDOWN -> System.out.println("[sandbox] -> orderly shutdown");
        }
    }

    private static void cleanup(
            GlfwWindow window,
            NativeResourceRegistry nativeResources,
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
        System.out.println("Timeline:");
        System.out.println("  0-5s   WINDOWED: resize/DPI observation");
        System.out.println("  5s     BORDERLESS_FULLSCREEN");
        System.out.println(" 10s     WINDOWED");
        System.out.println(" 15s     EXCLUSIVE_FULLSCREEN");
        System.out.println(" 20s     WINDOWED");
        System.out.println(" 25s     cursor capture ON; manually Alt+Tab away/back");
        System.out.println(" 34s     cursor capture OFF");
        System.out.println(" 38s     shutdown + native-resource leak assertion");
        System.out.println();
    }
}
