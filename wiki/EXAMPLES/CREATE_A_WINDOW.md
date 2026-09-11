# Example: create a production window

This is the smallest practical example using the current production public API, including the separated logical-window and framebuffer-pixel size path.

```java
import com.samo.engine.core.api.EngineLogger;
import com.samo.engine.core.api.NativeResourceRegistry;
import com.samo.engine.platform.api.GlfwWindow;
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

            // A real client platform step calls this once per render-frame event pass.
            // It delivers the independently queried initial logical/framebuffer sizes,
            // then later resize notifications as GLFW reports them.
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

When `start()` succeeds, the logger receives two platform INFO events containing the **actual** OpenGL version and renderer.

The first `pollEvents()` also delivers the current logical window dimensions and framebuffer pixel dimensions through separate listener methods. They may be equal on a 100% scaling environment or different under DPI scaling. Renderer-side pixel work must use the framebuffer channel rather than assuming it equals the logical window size.

A framebuffer size containing a zero axis, including `0x0`, is a valid minimized-window state. It is not a lifecycle failure.

## Why the example is still not a renderer loop

`GlfwWindow.pollEvents()` is now a bounded platform event operation, but the public API still intentionally does not expose buffer swapping, renderer ownership, fullscreen transitions, focus/input state, or raw GLFW handles. Adding direct LWJGL calls around `GlfwWindow` would bypass the engine abstraction boundary and pull later roadmap tasks into caller code.

Use this example for the currently implemented lifecycle and size-event API only. Later wiki pages should extend it when the corresponding production APIs actually exist.
