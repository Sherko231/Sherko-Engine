package com.samo.engine.platform.api;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.lwjgl.glfw.GLFW;

/** Owns GLFW cursor-capture, raw-motion, rearm, and retryable normalization state. */
final class GlfwCursorCaptureController {
    private final GlfwNativeBackend backend;
    private final GlfwMouseMotionTracker mouseMotion;

    private boolean captureRequested;
    private boolean captureEffective;
    private boolean captureNeedsExplicitRearm;
    private boolean cursorNormalizationPending;
    private boolean rawMouseMotionEnabled;

    GlfwCursorCaptureController(GlfwNativeBackend backend, GlfwMouseMotionTracker mouseMotion) {

        this.backend = backend;
        this.mouseMotion = mouseMotion;

    }

    boolean effectivelyCaptured() {

        return captureEffective;

    }

    boolean rawMouseMotionEnabled() {

        return rawMouseMotionEnabled;

    }

    boolean rawMouseMotionSupported() {

        return backend.rawMouseMotionSupported();

    }

    void reset() {

        mouseMotion.reset();
        captureRequested = false;
        captureEffective = false;
        captureNeedsExplicitRearm = false;
        cursorNormalizationPending = false;
        rawMouseMotionEnabled = false;

    }

    void setCaptured(long windowHandle, boolean focused, boolean captured) {

        if (captured) {
            if (!focused) {
                mouseMotion.reset();
                captureRequested = true;
                captureEffective = false;
                captureNeedsExplicitRearm = true;
                return;
            }
            if (captureRequested && captureEffective && !captureNeedsExplicitRearm) {
                return;
            }
            captureWithRawMotion(windowHandle);
            return;
        }

        if (!captureRequested && !captureEffective && !cursorNormalizationPending && !rawMouseMotionEnabled) {
            mouseMotion.reset();
            captureNeedsExplicitRearm = false;
            return;
        }

        releaseDirect(windowHandle);

    }

    void onFocusLost(long windowHandle, Consumer<Throwable> failureSink) {

        mouseMotion.reset();
        if (captureRequested) {
            captureNeedsExplicitRearm = true;
        }
        if (!captureEffective && !rawMouseMotionEnabled && !cursorNormalizationPending) {
            return;
        }

        captureEffective = false;
        if (rawMouseMotionEnabled) {
            try {
                backend.setRawMouseMotion(windowHandle, false);
                rawMouseMotionEnabled = false;
            } catch (RuntimeException | Error failure) {
                failureSink.accept(failure);
            }
        }
        if (cursorNormalizationPending) {
            try {
                backend.setCursorMode(windowHandle, GLFW.GLFW_CURSOR_NORMAL);
                cursorNormalizationPending = false;
            } catch (RuntimeException | Error failure) {
                failureSink.accept(failure);
            }
        }

    }

    void releaseForCleanup(long windowHandle, List<Throwable> failures) {

        mouseMotion.reset();
        captureEffective = false;
        if (rawMouseMotionEnabled && runCleanup(failures, () -> backend.setRawMouseMotion(windowHandle, false))) {
            rawMouseMotionEnabled = false;
        }
        if (cursorNormalizationPending && runCleanup(failures, () -> backend.setCursorMode(windowHandle, GLFW.GLFW_CURSOR_NORMAL))) {
            cursorNormalizationPending = false;
        }
        captureRequested = false;
        captureNeedsExplicitRearm = false;

    }

    private void captureWithRawMotion(long windowHandle) {

        boolean previousRequested = captureRequested;
        boolean previousEffective = captureEffective;
        boolean previousRearm = captureNeedsExplicitRearm;
        boolean previousNormalizationPending = cursorNormalizationPending;
        boolean previousRaw = rawMouseMotionEnabled;
        boolean cursorDisabled = false;
        boolean rawEnableAttempted = false;
        boolean rawEnabledThisAttempt = false;

        try {
            backend.setCursorMode(windowHandle, GLFW.GLFW_CURSOR_DISABLED);
            cursorDisabled = true;
            cursorNormalizationPending = true;
            if (backend.rawMouseMotionSupported()) {
                rawEnableAttempted = true;
                backend.setRawMouseMotion(windowHandle, true);
                rawEnabledThisAttempt = true;
            }
            mouseMotion.reset();
            captureRequested = true;
            captureEffective = true;
            captureNeedsExplicitRearm = false;
            rawMouseMotionEnabled = rawEnabledThisAttempt;
        } catch (RuntimeException | Error failure) {
            mouseMotion.reset();
            if (rawEnableAttempted) {
                try {
                    backend.setRawMouseMotion(windowHandle, false);
                } catch (RuntimeException | Error rollbackFailure) {
                    addSuppressedUnlessSame(failure, rollbackFailure);
                }
            }
            if (cursorDisabled) {
                try {
                    backend.setCursorMode(windowHandle, GLFW.GLFW_CURSOR_NORMAL);
                    cursorNormalizationPending = false;
                } catch (RuntimeException | Error rollbackFailure) {
                    addSuppressedUnlessSame(failure, rollbackFailure);
                }
            } else {
                cursorNormalizationPending = previousNormalizationPending;
            }
            captureRequested = previousRequested;
            captureEffective = previousEffective;
            captureNeedsExplicitRearm = previousRearm;
            rawMouseMotionEnabled = previousRaw;
            throw failure;
        }

    }

    private void releaseDirect(long windowHandle) {

        mouseMotion.reset();
        captureRequested = false;
        captureEffective = false;
        captureNeedsExplicitRearm = false;

        List<Throwable> failures = new ArrayList<>();
        if (rawMouseMotionEnabled && runCleanup(failures, () -> backend.setRawMouseMotion(windowHandle, false))) {
            rawMouseMotionEnabled = false;
        }
        if (cursorNormalizationPending && runCleanup(failures, () -> backend.setCursorMode(windowHandle, GLFW.GLFW_CURSOR_NORMAL))) {
            cursorNormalizationPending = false;
        }
        throwCleanupFailure(failures);

    }

    private static boolean runCleanup(List<Throwable> failures, Runnable cleanup) {

        try {
            cleanup.run();
            return true;
        } catch (RuntimeException | Error failure) {
            failures.add(failure);
            return false;
        }

    }

    private static void throwCleanupFailure(List<Throwable> failures) {

        if (failures.isEmpty()) {
            return;
        }
        Throwable primary = failures.getFirst();
        for (int index = 1; index < failures.size(); index++) {
            addSuppressedUnlessSame(primary, failures.get(index));
        }
        if (primary instanceof RuntimeException runtimeFailure) {
            throw runtimeFailure;
        }
        throw (Error) primary;

    }

    private static void addSuppressedUnlessSame(Throwable primary, Throwable suppressed) {

        if (primary != suppressed) {
            primary.addSuppressed(suppressed);
        }

    }
}
