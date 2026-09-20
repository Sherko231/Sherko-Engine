package com.samo.engine.platform.api;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryUtil;

/** Owns GLFW window-mode transition planning, restore geometry, native application, and rollback. */
final class GlfwWindowModeController {
    private final GlfwNativeBackend backend;

    private WindowMode currentMode = WindowMode.WINDOWED;
    private GlfwWindowGeometry windowedRestoreGeometry;

    GlfwWindowModeController(GlfwNativeBackend backend) {
        this.backend = backend;
    }

    void reset() {
        currentMode = WindowMode.WINDOWED;
        windowedRestoreGeometry = null;
    }

    void clearRestoreGeometry() {
        windowedRestoreGeometry = null;
    }

    void setMode(long windowHandle, WindowMode requestedMode) {
        if (requestedMode == currentMode) {
            return;
        }

        WindowMode previousMode = currentMode;
        GlfwWindowGeometry previousRestoreGeometry = windowedRestoreGeometry;
        GlfwWindowGeometry candidateRestoreGeometry = previousRestoreGeometry;
        if (previousMode == WindowMode.WINDOWED) {
            candidateRestoreGeometry = captureWindowedGeometry(windowHandle);
        }

        GlfwWindowTransitionPlan requestedPlan = planTransition(requestedMode, candidateRestoreGeometry);
        try {
            applyTransition(windowHandle, requestedPlan);
            currentMode = requestedMode;
            if (requestedMode == WindowMode.WINDOWED) {
                windowedRestoreGeometry = null;
            } else if (previousMode == WindowMode.WINDOWED) {
                windowedRestoreGeometry = candidateRestoreGeometry;
            }
        } catch (RuntimeException | Error failure) {
            try {
                applyTransition(windowHandle, planTransition(previousMode, candidateRestoreGeometry));
            } catch (RuntimeException | Error rollbackFailure) {
                addSuppressedUnlessSame(failure, rollbackFailure);
            }
            currentMode = previousMode;
            windowedRestoreGeometry = previousRestoreGeometry;
            throw failure;
        }
    }

    private GlfwWindowGeometry captureWindowedGeometry(long windowHandle) {
        GlfwPosition position = backend.queryWindowPosition(windowHandle);
        GlfwDimensions logicalSize = backend.queryLogicalSize(windowHandle);
        if (logicalSize.width() <= 0 || logicalSize.height() <= 0) {
            throw new IllegalStateException(
                    "GLFW reported non-positive windowed restore dimensions: "
                            + logicalSize.width() + "x" + logicalSize.height());
        }
        return new GlfwWindowGeometry(
                position.x(),
                position.y(),
                logicalSize.width(),
                logicalSize.height());
    }

    private GlfwWindowTransitionPlan planTransition(
            WindowMode mode,
            GlfwWindowGeometry restoreGeometry) {
        return switch (mode) {
            case WINDOWED -> {
                if (restoreGeometry == null) {
                    throw new IllegalStateException("Windowed restore geometry is unavailable");
                }
                validateRestoreGeometry(restoreGeometry);
                yield new GlfwWindowTransitionPlan(
                        WindowMode.WINDOWED,
                        true,
                        MemoryUtil.NULL,
                        restoreGeometry.x(),
                        restoreGeometry.y(),
                        restoreGeometry.width(),
                        restoreGeometry.height(),
                        GLFW.GLFW_DONT_CARE);
            }
            case BORDERLESS_FULLSCREEN -> {
                GlfwMonitorTarget monitor = queryPrimaryMonitorTarget();
                yield new GlfwWindowTransitionPlan(
                        WindowMode.BORDERLESS_FULLSCREEN,
                        false,
                        MemoryUtil.NULL,
                        monitor.position().x(),
                        monitor.position().y(),
                        monitor.videoMode().width(),
                        monitor.videoMode().height(),
                        GLFW.GLFW_DONT_CARE);
            }
            case EXCLUSIVE_FULLSCREEN -> {
                GlfwMonitorTarget monitor = queryPrimaryMonitorTarget();
                yield new GlfwWindowTransitionPlan(
                        WindowMode.EXCLUSIVE_FULLSCREEN,
                        null,
                        monitor.handle(),
                        0,
                        0,
                        monitor.videoMode().width(),
                        monitor.videoMode().height(),
                        monitor.videoMode().refreshRate());
            }
        };
    }

    private GlfwMonitorTarget queryPrimaryMonitorTarget() {
        long monitor = backend.primaryMonitor();
        if (monitor == MemoryUtil.NULL) {
            throw new IllegalStateException("GLFW primary monitor is unavailable");
        }
        GlfwVideoMode videoMode = backend.queryVideoMode(monitor);
        if (videoMode == null) {
            throw new IllegalStateException("GLFW primary monitor video mode is unavailable");
        }
        if (videoMode.width() <= 0 || videoMode.height() <= 0 || videoMode.refreshRate() <= 0) {
            throw new IllegalStateException(
                    "GLFW reported invalid primary monitor video mode: "
                            + videoMode.width() + "x" + videoMode.height() + "@" + videoMode.refreshRate());
        }
        GlfwPosition position = backend.queryMonitorPosition(monitor);
        return new GlfwMonitorTarget(monitor, position, videoMode);
    }

    private void applyTransition(long windowHandle, GlfwWindowTransitionPlan plan) {
        if (plan.decorated() != null) {
            backend.setDecorated(windowHandle, plan.decorated());
        }
        backend.setWindowMonitor(
                windowHandle,
                plan.monitor(),
                plan.x(),
                plan.y(),
                plan.width(),
                plan.height(),
                plan.refreshRate());
    }

    private static void validateRestoreGeometry(GlfwWindowGeometry geometry) {
        if (geometry.width() <= 0 || geometry.height() <= 0) {
            throw new IllegalStateException(
                    "Windowed restore dimensions must be positive: "
                            + geometry.width() + "x" + geometry.height());
        }
    }

    private static void addSuppressedUnlessSame(Throwable primary, Throwable suppressed) {
        if (primary != suppressed) {
            primary.addSuppressed(suppressed);
        }
    }
}
