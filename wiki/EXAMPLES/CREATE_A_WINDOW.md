# Example: create a production window

This is the smallest practical example using the current production public API, including separated logical/framebuffer sizing, in-place display-mode changes, and focus-loss-safe cursor capture.

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

            // Gameplay may explicitly request cursor capture while focused.
            window.setCursorCaptured(true);

            // These transitions reuse the same GLFW window/OpenGL context.
            window.setWindowMode(WindowMode.BORDERLESS_FULLSCREEN);
            window.pollEvents();

            window.setWindowMode(WindowMode.EXCLUSIVE_FULLSCREEN);
            window.pollEvents();

            // Restores the position/size captured before the first fullscreen change.
            window.setWindowMode(WindowMode.WINDOWED);
            window.pollEvents();

            // If the user Alt+Tabs away, GlfwWindow releases effective capture and
            // clears its internal held key/button safety state. Focus regain never
            // recaptures automatically; caller policy explicitly re-arms when appropriate.
            window.setCursorCaptured(true);

            window.setCursorCaptured(false);
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

The display-mode calls target the primary monitor's current video mode. Borderless fullscreen uses an undecorated, monitor-detached window at the monitor origin; exclusive fullscreen attaches the same native window to that monitor. Returning to windowed restores the captured windowed geometry.

`setCursorCaptured(true)` requests GLFW disabled-cursor mode only while the window is focused. If focus is lost, the platform boundary restores a normal cursor and clears its internal held keyboard/mouse-button state. Returning focus does not automatically recapture. The caller must explicitly request capture again when gameplay should resume.

All lifecycle, polling, mode-change, and cursor-capture calls stay on the initializing owner thread.

## Why the example is still not a renderer or input loop

`GlfwWindow.pollEvents()`, `setWindowMode(...)`, and `setCursorCaptured(...)` are bounded platform operations. The public API still intentionally does not expose buffer swapping, renderer ownership, monitor selection/custom video modes, raw GLFW handles, raw mouse motion, public key/button snapshots, action mapping, or player commands.

Adding direct LWJGL calls around `GlfwWindow` would bypass the engine abstraction boundary and pull later roadmap tasks into caller code.

Use this example for the currently implemented lifecycle, size-event, window-mode, and cursor-capture API only. Later wiki pages should extend it when the corresponding production APIs actually exist.
