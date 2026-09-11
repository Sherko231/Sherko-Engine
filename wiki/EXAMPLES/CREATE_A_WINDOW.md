# Example: create a production window

This is the smallest practical example using the current production public API.

```java
import com.samo.engine.core.api.EngineLogger;
import com.samo.engine.core.api.NativeResourceRegistry;
import com.samo.engine.platform.api.GlfwWindow;

public final class WindowExample {
    public static void main(String[] args) {
        EngineLogger logger = new EngineLogger(event ->
                System.out.printf(
                        "[%s] [%s] %s%n",
                        event.level(),
                        event.context().subsystem(),
                        event.message()));

        NativeResourceRegistry nativeResources = new NativeResourceRegistry();

        GlfwWindow window = new GlfwWindow(
                1280,
                720,
                "Sherko Engine Example",
                logger,
                nativeResources);

        try {
            window.initialize();
            window.start();

            // There is intentionally no public event-polling/render-loop API yet.
            // At the current engine stage this example proves creation/lifecycle only.

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

On the currently verified development environment the retained acceptance evidence observed OpenGL 4.6, but that machine-specific renderer string is evidence, not a portable requirement. The contract requires a real OpenGL 4.6-capable context.

## Why the example does not contain a game loop

The public platform API intentionally does not expose event polling, swap buffers, input, or rendering yet. Adding raw LWJGL calls around `GlfwWindow` would bypass the engine's intended abstraction boundary and would turn future roadmap work into ad-hoc caller code.

Use this example only for current lifecycle/API understanding. Later wiki pages should extend the example when the corresponding production APIs actually exist.
