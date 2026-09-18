package com.samo.game.sandbox;

import com.samo.engine.core.api.EngineClock;
import com.samo.engine.core.api.EngineLogger;
import com.samo.engine.core.api.FixedStepAccumulator;
import com.samo.engine.core.api.FixedStepCatchUpPolicy;
import com.samo.engine.core.api.InputResponseSettings;
import com.samo.engine.core.api.CameraMatrices;
import com.samo.engine.core.api.NativeResourceRegistry;
import com.samo.engine.core.api.PlayerInputCommand;
import com.samo.engine.platform.api.GlfwWindow;
import com.samo.engine.platform.api.OpenGlDebugMode;
import com.samo.engine.platform.api.InputAction;
import com.samo.engine.platform.api.InputActionBindings;
import com.samo.engine.platform.api.InputActionEvaluator;
import com.samo.engine.platform.api.InputActionSnapshot;
import com.samo.engine.platform.api.InputActionState;
import com.samo.engine.platform.api.InputKey;
import com.samo.engine.platform.api.InputSnapshot;
import com.samo.engine.platform.api.PlayerInputCommandSampler;
import com.samo.engine.platform.api.WindowMode;
import com.samo.engine.platform.api.WindowSizeListener;
import com.samo.engine.render.api.OpenGlRenderer;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.EnumSet;
import java.util.Objects;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Persistent owner-facing Sherko Engine playground using public production APIs only. */
public final class SandboxMain {
    private static final long SECOND_NANOS = 1_000_000_000L;
    private static final long DIAGNOSTIC_INTERVAL_NANOS = SECOND_NANOS;
    private static final String SANDBOX_SUBSYSTEM = "game-sandbox";
    private static final double[] MOUSE_SENSITIVITIES = {0.5d, 1.0d, 2.0d};

    private SandboxMain() {
    }

    public static void main(String[] args) throws InterruptedException {
        printControls();

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
        SandboxFramebufferSize framebufferSize = new SandboxFramebufferSize(1280, 720);
        InputResponseSettings responseSettings = InputResponseSettings.defaults();
        InputActionEvaluator actionEvaluator = new InputActionEvaluator(loadSandboxBindings(), responseSettings);
        PlayerInputCommandSampler commandSampler = new PlayerInputCommandSampler();
        WindowSizeListener sizeListener = new WindowSizeListener() {
            @Override
            public void onLogicalWindowSizeChanged(int width, int height) {
                log(logger, EngineLogger.Level.INFO, "Logical window size changed to %dx%d".formatted(width, height));
            }

            @Override
            public void onFramebufferSizeChanged(int width, int height) {
                framebufferSize.update(width, height);
                log(logger, EngineLogger.Level.INFO, "Framebuffer size changed to %dx%d".formatted(width, height));
            }
        };

        GlfwWindow window = new GlfwWindow(
                1280,
                720,
                "Sherko Engine Sandbox",
                logger,
                nativeResources,
                sizeListener,
                OpenGlDebugMode.FAIL_ON_HIGH_SEVERITY);

        boolean started = false;
        OpenGlRenderer renderer = null;
        Throwable primaryFailure = null;
        try {
            window.initialize();
            window.start();
            started = true;
            renderer = OpenGlRenderer.create(window.openGlThreadGuard(), nativeResources);
            runSandbox(
                    window,
                    renderer,
                    framebufferSize,
                    logger,
                    actionEvaluator,
                    commandSampler,
                    responseSettings);
        } catch (InterruptedException failure) {
            primaryFailure = failure;
            Thread.currentThread().interrupt();
            throw failure;
        } catch (RuntimeException | Error failure) {
            primaryFailure = failure;
            throw failure;
        } finally {
            cleanup(window, renderer, nativeResources, logger, started, primaryFailure);
        }
    }

    private static void runSandbox(
            GlfwWindow window,
            OpenGlRenderer renderer,
            SandboxFramebufferSize framebufferSize,
            EngineLogger logger,
            InputActionEvaluator actionEvaluator,
            PlayerInputCommandSampler commandSampler,
            InputResponseSettings initialResponseSettings) throws InterruptedException {
        EngineClock clock = new EngineClock();
        FixedStepAccumulator accumulator = new FixedStepAccumulator();
        FixedStepCatchUpPolicy catchUpPolicy = new FixedStepCatchUpPolicy();

        long cumulativeTicks = 0L;
        long nextDiagnosticNanos = DIAGNOSTIC_INTERVAL_NANOS;
        long elapsedSandboxNanos = 0L;
        long inputFrameId = 0L;
        double diagnosticMouseDeltaX = 0.0d;
        double diagnosticMouseDeltaY = 0.0d;
        PlayerInputCommand latestCommand = null;
        WindowMode currentWindowMode = WindowMode.WINDOWED;
        InputResponseSettings responseSettings = initialResponseSettings;
        int sensitivityIndex = indexOfSensitivity(responseSettings.mouseSensitivity());
        boolean exitRequested = false;
        Matrix4f view = CameraMatrices.view(
                new Vector3f(0.0f, 0.0f, 2.0f),
                new Vector3f(0.0f, 0.0f, -1.0f),
                new Vector3f(0.0f, 1.0f, 0.0f),
                new Matrix4f());
        Matrix4f projection = new Matrix4f();

        clock.sampleElapsedNanos();
        while (!exitRequested) {
            long elapsedNanos = clock.sampleElapsedNanos();
            elapsedSandboxNanos = saturatingAdd(elapsedSandboxNanos, elapsedNanos);
            long dueTicks = catchUpPolicy.advance(accumulator, elapsedNanos);

            window.pollEvents();
            InputSnapshot latestInput = window.captureInputSnapshot(inputFrameId++);

            SandboxControls.Input ownerInput = new SandboxControls.Input(
                    latestInput.keyPressed(InputKey.F),
                    latestInput.keyPressed(InputKey.R),
                    latestInput.keyPressed(InputKey.Q),
                    latestInput.keyHeld(InputKey.RIGHT_SHIFT),
                    latestInput.keyHeld(InputKey.LEFT_CONTROL),
                    latestInput.keyHeld(InputKey.RIGHT_CONTROL));
            EnumSet<SandboxControls.Action> ownerActions = SandboxControls.resolve(ownerInput);

            if (ownerActions.contains(SandboxControls.Action.CYCLE_WINDOW_MODE)) {
                currentWindowMode = nextWindowMode(currentWindowMode);
                window.setWindowMode(currentWindowMode);
                log(logger, EngineLogger.Level.INFO, "Sandbox window mode -> " + currentWindowMode, cumulativeTicks);
            }
            if (ownerActions.contains(SandboxControls.Action.TOGGLE_CURSOR_CAPTURE)) {
                boolean requestedCapture = !latestInput.cursorCaptured();
                window.setCursorCaptured(requestedCapture);
                log(
                        logger,
                        EngineLogger.Level.INFO,
                        "Sandbox cursor capture requested -> " + requestedCapture,
                        cumulativeTicks);
            }
            if (ownerActions.contains(SandboxControls.Action.CYCLE_MOUSE_SENSITIVITY)) {
                sensitivityIndex = (sensitivityIndex + 1) % MOUSE_SENSITIVITIES.length;
                responseSettings = new InputResponseSettings(
                        MOUSE_SENSITIVITIES[sensitivityIndex],
                        responseSettings.invertMouseY(),
                        responseSettings.controllerDeadZone(),
                        responseSettings.controllerCurveExponent());
                actionEvaluator.setResponseSettings(responseSettings);
                log(
                        logger,
                        EngineLogger.Level.INFO,
                        "Mouse sensitivity -> %.2f".formatted(responseSettings.mouseSensitivity()),
                        cumulativeTicks);
            }
            if (ownerActions.contains(SandboxControls.Action.TOGGLE_MOUSE_Y_INVERSION)) {
                responseSettings = new InputResponseSettings(
                        responseSettings.mouseSensitivity(),
                        !responseSettings.invertMouseY(),
                        responseSettings.controllerDeadZone(),
                        responseSettings.controllerCurveExponent());
                actionEvaluator.setResponseSettings(responseSettings);
                log(
                        logger,
                        EngineLogger.Level.INFO,
                        "Mouse Y inversion -> " + responseSettings.invertMouseY(),
                        cumulativeTicks);
            }
            exitRequested = ownerActions.contains(SandboxControls.Action.EXIT);

            InputActionSnapshot latestActions = actionEvaluator.evaluate(latestInput);
            commandSampler.submit(latestActions);
            diagnosticMouseDeltaX += latestInput.mouseDeltaX();
            diagnosticMouseDeltaY += latestInput.mouseDeltaY();

            for (long offset = 1L; offset <= dueTicks; offset++) {
                latestCommand = commandSampler.nextCommand(cumulativeTicks + offset);
            }
            cumulativeTicks += dueTicks;

            if (!exitRequested && framebufferSize.width() > 0 && framebufferSize.height() > 0) {
                float aspectRatio = (float) framebufferSize.width() / framebufferSize.height();
                CameraMatrices.perspective(
                        (float) Math.toRadians(70.0),
                        aspectRatio,
                        0.1f,
                        100.0f,
                        projection);
                renderer.render(
                        view,
                        projection,
                        framebufferSize.width(),
                        framebufferSize.height());
                window.present();
            }

            if (elapsedSandboxNanos >= nextDiagnosticNanos) {
                InputActionState move = latestActions.state(InputAction.MOVE);
                InputActionState jump = latestActions.state(InputAction.JUMP);
                InputActionState interact = latestActions.state(InputAction.INTERACT);
                String commandDiagnostic = latestCommand == null
                        ? "tickCommand=none"
                        : "tickCommand=%d MOVE=(%.1f,%.1f) LOOK=(%.2f,%.2f)"
                                .formatted(
                                        latestCommand.tickId(),
                                        latestCommand.moveX(),
                                        latestCommand.moveY(),
                                        latestCommand.lookX(),
                                        latestCommand.lookY());
                String diagnosticMessage = SandboxDiagnosticFormatter.format(
                        new SandboxDiagnosticFormatter.DiagnosticValues(
                                elapsedSandboxNanos / 1_000_000_000.0,
                                accumulator.interpolationAlpha(),
                                latestInput.frameId(),
                                latestInput.focused(),
                                latestInput.cursorCaptured(),
                                currentWindowMode.name(),
                                responseSettings.mouseSensitivity(),
                                responseSettings.invertMouseY(),
                                latestInput.keyHeld(InputKey.W),
                                latestInput.keyHeld(InputKey.A),
                                latestInput.keyHeld(InputKey.S),
                                latestInput.keyHeld(InputKey.D),
                                move.x(),
                                move.y(),
                                jump.pressed(),
                                jump.held(),
                                jump.released(),
                                interact.pressed(),
                                interact.held(),
                                interact.released(),
                                commandDiagnostic,
                                diagnosticMouseDeltaX,
                                diagnosticMouseDeltaY));
                log(logger, EngineLogger.Level.DEBUG, diagnosticMessage, cumulativeTicks);
                diagnosticMouseDeltaX = 0.0d;
                diagnosticMouseDeltaY = 0.0d;
                do {
                    nextDiagnosticNanos += DIAGNOSTIC_INTERVAL_NANOS;
                } while (nextDiagnosticNanos <= elapsedSandboxNanos);
            }

            if (!exitRequested) {
                Thread.sleep(5L);
            }
        }
        log(logger, EngineLogger.Level.INFO, "Sandbox exit requested by Ctrl+Q", cumulativeTicks);
    }

    private static int indexOfSensitivity(double sensitivity) {
        for (int index = 0; index < MOUSE_SENSITIVITIES.length; index++) {
            if (Double.compare(MOUSE_SENSITIVITIES[index], sensitivity) == 0) {
                return index;
            }
        }
        return 0;
    }

    private static WindowMode nextWindowMode(WindowMode current) {
        return switch (current) {
            case WINDOWED -> WindowMode.BORDERLESS_FULLSCREEN;
            case BORDERLESS_FULLSCREEN -> WindowMode.EXCLUSIVE_FULLSCREEN;
            case EXCLUSIVE_FULLSCREEN -> WindowMode.WINDOWED;
        };
    }

    private static InputActionBindings loadSandboxBindings() {
        try (InputStream source = Objects.requireNonNull(
                SandboxMain.class.getResourceAsStream("/input/action-bindings-v1.json"),
                "sandbox action bindings resource")) {
            Path tempFile = Files.createTempFile("sherko-engine-sandbox-bindings-", ".json");
            try {
                Files.copy(source, tempFile, StandardCopyOption.REPLACE_EXISTING);
                return InputActionBindings.load(tempFile);
            } finally {
                deleteSandboxBindingsTempFile(tempFile);
            }
        } catch (IOException failure) {
            throw new IllegalStateException("Failed to materialize sandbox action bindings", failure);
        }
    }

    private static void deleteSandboxBindingsTempFile(Path tempFile) {
        try {
            Files.deleteIfExists(tempFile);
        } catch (IOException cleanupFailure) {
            tempFile.toFile().deleteOnExit();
        }
    }

    private static void cleanup(
            GlfwWindow window,
            OpenGlRenderer renderer,
            NativeResourceRegistry nativeResources,
            EngineLogger logger,
            boolean started,
            Throwable primaryFailure) {
        Throwable cleanupFailure = null;

        if (renderer != null) {
            cleanupFailure = attempt(cleanupFailure, renderer::close);
        }
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

    private static void printControls() {
        System.out.println("Sherko Engine persistent sandbox playground");
        System.out.println("Uses production public APIs only; it stays open until you exit with Ctrl+Q.");
        System.out.println("The production renderer draws one indexed white triangle on a dark background.");
        System.out.println();
        System.out.println("Owner controls:");
        System.out.println("  F               cycle WINDOWED / BORDERLESS_FULLSCREEN / EXCLUSIVE_FULLSCREEN");
        System.out.println("  R               toggle cursor capture");
        System.out.println("  Right Shift + F cycle mouse sensitivity 0.5 / 1.0 / 2.0");
        System.out.println("  Right Shift + R toggle mouse Y inversion");
        System.out.println("  Ctrl + Q        exit sandbox cleanly");
        System.out.println();
        System.out.println("Gameplay/input bindings remain active at the same time: W/A/S/D, mouse, Space, E, mouse buttons, etc.");
        System.out.println();
    }

    private static final class SandboxFramebufferSize {
        private int width;
        private int height;

        private SandboxFramebufferSize(int width, int height) {
            this.width = width;
            this.height = height;
        }

        void update(int width, int height) {
            this.width = width;
            this.height = height;
        }

        int width() {
            return width;
        }

        int height() {
            return height;
        }
    }
}
