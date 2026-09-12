# Example: create a production window

This is the smallest practical example using the current production public API, including separated logical/framebuffer sizing and in-place display-mode changes.

```java
import com.samo.engine.core.api.EngineLogger;
import com.samo.engine.core.api.NativeResourceRegistry;
import com.samo.engine.platform.api.GlfwWindow;
import com.samo.engine.platform.api.WindowMode;
import com.samo.engine.platform.api.WindowSizeListener;

public final class WindowExample {
    public static void main(String[] args) {
        EngineLogger logger = new EngineLogger(event ->
                System.out.printf(
                        "[%s] [%s] %s%n",
                        event.level(),
                        event.context().subsystem(),
                        event.message()));

        NativeResourceRegistry nativeResources = new NativeResourceRegistry();
        WindowSizeListener sizes = new WindowSizeListener() {
            @Override
            public void onLogicalWindowSizeChanged(int width, int height) {
                System.out.printf("logical=%dx%d%n", width, height);
            }

            @Override
            public void onFramebufferSizeChanged(int width, int height) {
                System.out.printf("framebuffer=%dx%d%n", width, height);
            }
        };

        GlfwWindow window = new GlfwWindow(
                1280,
                720,
                "Sherko Engine Example",
                logger,
                nativeResources,
                sizes);

        try {
            window.initialize();
            window.start();

            // One owner-thread platform event pass.
            window.pollEvents();

            // These transitions reuse the same GLFW window/OpenGL context.
            window.setWindowMode(WindowMode.BORDERLESS_FULLSCREEN);
            window.pollEvents();

            window.setWindowMode(WindowMode.EXCLUSIVE_FULLSCREEN);
            window.pollEvents();

            // Restores the position/size captured before the first fullscreen change.
            window.setWindowMode(WindowMode.WINDOWED);
            window.pollEvents();

            window.stop();
        } finally {
            window.close();
            nativeResources.assertNoOpenResources();
        }
    }
}
```

## What you should observe

When `start()` succeeds, the logger receives two platform INFO events containing the actual OpenGL version and renderer.

The first `pollEvents()` also delivers the current logical window dimensions and framebuffer pixel dimensions through separate listener methods. They may be equal on a 100% scaling environment or different under DPI scaling. Renderer-side pixel work must use the framebuffer channel rather than assuming it equals the logical window size.

A framebuffer size containing a zero axis, including `0x0`, is a valid minimized-window state. It is not a lifecycle failure.

The display-mode calls target the primary monitor's current video mode. Borderless fullscreen uses an undecorated, monitor-detached window at the monitor origin; exclusive fullscreen attaches the same native window to that monitor. Returning to windowed restores the captured windowed geometry. All lifecycle, polling, and mode-change calls stay on the initializing owner thread.

## Why the example is still not a renderer loop

`GlfwWindow.pollEvents()` and `setWindowMode(...)` are bounded platform operations. The public API still intentionally does not expose buffer swapping, renderer ownership, monitor selection/custom video modes, focus/input state, or raw GLFW handles. Adding direct LWJGL calls around `GlfwWindow` would bypass the engine abstraction boundary and pull later roadmap tasks into caller code.

Use this example for the currently implemented lifecycle, size-event, and window-mode API only. Later wiki pages should extend it when the corresponding production APIs actually exist.
