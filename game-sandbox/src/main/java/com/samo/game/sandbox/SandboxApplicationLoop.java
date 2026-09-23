package com.samo.game.sandbox;

import com.samo.engine.core.api.EngineClock;
import com.samo.engine.core.api.EngineLogger;
import com.samo.engine.core.api.FixedStepAccumulator;
import com.samo.engine.core.api.FixedStepCatchUpPolicy;
import com.samo.engine.core.api.InputResponseSettings;
import com.samo.engine.core.api.PlayerInputCommand;
import com.samo.engine.platform.api.GlfwWindow;
import com.samo.engine.platform.api.InputActionEvaluator;
import com.samo.engine.platform.api.InputActionSnapshot;
import com.samo.engine.platform.api.InputKey;
import com.samo.engine.platform.api.InputSnapshot;
import com.samo.engine.platform.api.PlayerInputCommandSampler;
import com.samo.engine.render.api.OpenGlRenderer;
import com.samo.engine.render.api.RenderFramePacket;
import java.util.EnumSet;
import java.util.Objects;

final class SandboxApplicationLoop {
    private final GlfwWindow window;
    private final OpenGlRenderer renderer;
    private final SandboxFramebufferSize framebufferSize;
    private final EngineLogger logger;
    private final InputActionEvaluator actionEvaluator;
    private final PlayerInputCommandSampler commandSampler;
    private final SandboxAssetLab assetLab;
    private final EngineClock clock = new EngineClock();
    private final FixedStepAccumulator accumulator = new FixedStepAccumulator();
    private final FixedStepCatchUpPolicy catchUpPolicy = new FixedStepCatchUpPolicy();
    private final SandboxControlState controlState;
    private final SandboxCamera camera = new SandboxCamera();
    private final SandboxSceneSetup sceneSetup = new SandboxSceneSetup();
    private final SandboxDiagnostics diagnostics = new SandboxDiagnostics();

    private long cumulativeTicks;
    private long inputFrameId;
    private PlayerInputCommand latestCommand;

    SandboxApplicationLoop(GlfwWindow window, OpenGlRenderer renderer, SandboxFramebufferSize framebufferSize, EngineLogger logger, InputActionEvaluator actionEvaluator,
        PlayerInputCommandSampler commandSampler, InputResponseSettings initialResponseSettings, SandboxAssetLab assetLab) {

        this.window = Objects.requireNonNull(window, "window");
        this.renderer = Objects.requireNonNull(renderer, "renderer");
        this.framebufferSize = Objects.requireNonNull(framebufferSize, "framebufferSize");
        this.logger = Objects.requireNonNull(logger, "logger");
        this.actionEvaluator = Objects.requireNonNull(actionEvaluator, "actionEvaluator");
        this.commandSampler = Objects.requireNonNull(commandSampler, "commandSampler");
        this.assetLab = Objects.requireNonNull(assetLab, "assetLab");
        controlState = new SandboxControlState(initialResponseSettings);

    }

    void run() throws InterruptedException {

        boolean exitRequested = false;
        clock.sampleElapsedNanos();

        while (!exitRequested) {
            long elapsedNanos = clock.sampleElapsedNanos();
            diagnostics.advanceElapsed(elapsedNanos);
            long dueTicks = catchUpPolicy.advance(accumulator, elapsedNanos);

            window.pollEvents();
            InputSnapshot latestInput = window.captureInputSnapshot(inputFrameId++);

            SandboxControls.SandboxControlInput ownerInput =
                new SandboxControls.SandboxControlInput(latestInput.keyPressed(InputKey.F), latestInput.keyPressed(InputKey.R), latestInput.keyPressed(InputKey.Q),
                    latestInput.keyPressed(InputKey.G), latestInput.keyPressed(InputKey.M), latestInput.keyPressed(InputKey.H), latestInput.keyHeld(InputKey.RIGHT_SHIFT),
                    latestInput.keyHeld(InputKey.LEFT_CONTROL), latestInput.keyHeld(InputKey.RIGHT_CONTROL));
            EnumSet<SandboxControls.SandboxAction> ownerActions = SandboxControls.resolve(ownerInput);

            applyAssetActions(ownerActions);
            assetLab.update();

            exitRequested = controlState.apply(ownerActions, latestInput.cursorCaptured(), actionEvaluator, window::setWindowMode, window::setCursorCaptured,
                message -> SandboxMain.log(logger, EngineLogger.Level.INFO, message, cumulativeTicks));

            InputActionSnapshot latestActions = actionEvaluator.evaluate(latestInput);
            commandSampler.submit(latestActions);
            diagnostics.recordMouseDelta(latestInput);

            for (long offset = 1L; offset <= dueTicks; offset++) {
                latestCommand = commandSampler.nextCommand(cumulativeTicks + offset);
                camera.apply(latestCommand);
            }
            cumulativeTicks += dueTicks;

            if (!exitRequested && framebufferSize.width() > 0 && framebufferSize.height() > 0) {
                RenderFramePacket renderFrame = sceneSetup.frame(camera, framebufferSize, cumulativeTicks, latestInput.frameId(), assetLab.currentMaterial());
                renderer.render(renderFrame);
                window.present();
            }

            diagnostics.publishIfDue(accumulator, latestInput, latestActions, latestCommand, controlState, renderer, logger, cumulativeTicks);

            if (!exitRequested) {
                Thread.sleep(5L);
            }
        }

        SandboxMain.log(logger, EngineLogger.Level.INFO, "Sandbox exit requested by Ctrl+Q", cumulativeTicks);

    }

    private void applyAssetActions(EnumSet<SandboxControls.SandboxAction> actions) {

        if (actions.contains(SandboxControls.SandboxAction.CYCLE_VALID_MATERIAL)) {
            assetLab.cycleValidMaterial();
        }
        if (actions.contains(SandboxControls.SandboxAction.DEMONSTRATE_FAILED_MATERIAL_RELOAD)) {
            assetLab.demonstrateFailedReload();
        }
        if (actions.contains(SandboxControls.SandboxAction.DEMONSTRATE_MISSING_MESH)) {
            assetLab.demonstrateMissingMeshFallback();
        }
        if (actions.contains(SandboxControls.SandboxAction.RELOAD_ASSET_HANDLES)) {
            assetLab.reloadHandles();
        }

    }
}
