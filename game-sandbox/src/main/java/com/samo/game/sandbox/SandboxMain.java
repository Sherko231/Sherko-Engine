package com.samo.game.sandbox;

import com.samo.engine.core.api.EngineLogger;
import com.samo.engine.core.api.InputResponseSettings;
import com.samo.engine.core.api.NativeResourceRegistry;
import com.samo.engine.platform.api.GlfwWindow;
import com.samo.engine.platform.api.InputActionBindings;
import com.samo.engine.platform.api.InputActionEvaluator;
import com.samo.engine.platform.api.OpenGlDebugMode;
import com.samo.engine.platform.api.PlayerInputCommandSampler;
import com.samo.engine.platform.api.WindowMode;
import com.samo.engine.platform.api.WindowSizeListener;
import com.samo.engine.render.api.OpenGlRenderer;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

/** Persistent owner-facing Sherko Engine playground using public production APIs only. */
public final class SandboxMain {
    private static final String SANDBOX_SUBSYSTEM = "game-sandbox";

    private SandboxMain() {

    }

    public static void main(String[] args) throws InterruptedException {

        printControls();

        EngineLogger logger = createLogger();
        NativeResourceRegistry nativeResources = new NativeResourceRegistry();
        SandboxFramebufferSize framebufferSize = new SandboxFramebufferSize(1280, 720);
        InputResponseSettings responseSettings = InputResponseSettings.defaults();
        InputActionEvaluator actionEvaluator = new InputActionEvaluator(loadSandboxBindings(), responseSettings);
        PlayerInputCommandSampler commandSampler = new PlayerInputCommandSampler();
        WindowSizeListener sizeListener = createSizeListener(framebufferSize, logger);
        GlfwWindow window = new GlfwWindow(1280, 720, "Sherko Engine Sandbox", logger, nativeResources, sizeListener, OpenGlDebugMode.FAIL_ON_HIGH_SEVERITY);

        boolean started = false;
        OpenGlRenderer renderer = null;
        SandboxAssetLab assetLab = null;
        Throwable primaryFailure = null;
        try {
            window.initialize();
            window.start();
            started = true;
            renderer = OpenGlRenderer.create(window.openGlThreadGuard(), nativeResources, logger, 4);
            assetLab = createAssetLab(logger);
            new SandboxApplicationLoop(window, renderer, framebufferSize, logger, actionEvaluator, commandSampler, responseSettings, assetLab).run();
        } catch (InterruptedException failure) {
            primaryFailure = failure;
            Thread.currentThread().interrupt();
            throw failure;
        } catch (RuntimeException | Error failure) {
            primaryFailure = failure;
            throw failure;
        } finally {
            cleanup(window, renderer, assetLab, nativeResources, logger, started, primaryFailure);
        }

    }

    private static SandboxAssetLab createAssetLab(EngineLogger logger) {

        return SandboxAssetLab.open(message -> log(logger, EngineLogger.Level.INFO, message));

    }

    private static EngineLogger createLogger() {

        return new EngineLogger(event -> {
            Long simulationTick = event.context().simulationTick();
            if (simulationTick == null) {
                System.out.printf("[%s] [%s] %s%n", event.level(), event.context().subsystem(), event.message());
            } else {
                System.out.printf("[%s] [%s] [tick=%d] %s%n", event.level(), event.context().subsystem(), simulationTick, event.message());
            }
        });

    }

    private static WindowSizeListener createSizeListener(SandboxFramebufferSize framebufferSize, EngineLogger logger) {

        return new WindowSizeListener() {
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

    }

    private static InputActionBindings loadSandboxBindings() {

        try (InputStream source = Objects.requireNonNull(SandboxMain.class.getResourceAsStream("/input/action-bindings-v1.json"), "sandbox action bindings resource")) {
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

    private static void cleanup(GlfwWindow window, OpenGlRenderer renderer, SandboxAssetLab assetLab, NativeResourceRegistry nativeResources, EngineLogger logger,
        boolean started, Throwable primaryFailure) {

        Throwable cleanupFailure = null;

        if (assetLab != null) {
            cleanupFailure = attempt(cleanupFailure, assetLab::close);
        }
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
            log(logger, EngineLogger.Level.INFO, "Sandbox shutdown completed; Asset Lab closed and native resource registry is empty");
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

    static void log(EngineLogger logger, EngineLogger.Level level, String message) {

        logger.log(level, message, sandboxContext(null));

    }

    static void log(EngineLogger logger, EngineLogger.Level level, String message, long simulationTick) {

        logger.log(level, message, sandboxContext(simulationTick));

    }

    private static EngineLogger.Context sandboxContext(Long simulationTick) {

        return new EngineLogger.Context(null, simulationTick, SANDBOX_SUBSYSTEM, null, null);

    }

    private static void printControls() {

        System.out.println("Sherko Engine persistent sandbox playground");
        System.out.println("Uses production public APIs only; it stays open until you exit with Ctrl+Q.");
        System.out.println("The production renderer draws the accepted Phase 5 reference room.");
        System.out.println("Phase 6 Asset Lab opens only a bundled cooked cache + manifest through public AssetLoaders.");
        System.out.println("READY MaterialAsset RGB values drive the existing public point/spot lights only as a visualization;");
        System.out.println("this is not arbitrary MaterialAsset submission to the renderer.");
        System.out.println();
        System.out.println("Phase 6 runtime demonstrations:");
        System.out.println("  async cooked MESH + MATERIAL loading, typed handles, stable AssetId, structured diagnostics");
        System.out.println("  missing-content fallback, handle release/reload, valid/invalid MATERIAL hot reload");
        System.out.println("Offline Phase 6 capabilities remain represented by accepted tests/docs, not fake runtime visuals:");
        System.out.println("  source metadata, cooker, glTF conversion/tangents, SMES/STEX/SAUD, mip/audio cooking, dependency graph,");
        System.out.println("  renderer-internal owner-thread mesh upload and shader reload.");
        System.out.println();
        System.out.println("Owner controls:");
        System.out.println("  E               switch valid MATERIAL WARM / COOL; room-light palette changes live");
        System.out.println("  Right Shift + E attempt invalid MATERIAL reload; prior READY value stays active");
        System.out.println("  Alt + E         request missing MESH and show READY fallback + MISSING_CONTENT");
        System.out.println("  Ctrl + E        release current MESH/MATERIAL handles and start fresh async loads");
        System.out.println("  F               cycle WINDOWED / BORDERLESS_FULLSCREEN / EXCLUSIVE_FULLSCREEN");
        System.out.println("  R               toggle cursor capture");
        System.out.println("  Right Shift + F cycle mouse sensitivity 0.5 / 1.0 / 2.0");
        System.out.println("  Right Shift + R toggle mouse Y inversion");
        System.out.println("  Ctrl + Q        exit sandbox cleanly");
        System.out.println();
        System.out.println("W/A/S/D move the rendered camera and mouse LOOK changes yaw/pitch; other input bindings remain active.");
        System.out.println();

    }
}
