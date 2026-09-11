# GLFW/OpenGL window

`com.samo.engine.platform.api.GlfwWindow` is the current production window/context boundary.

It extends `EngineSubsystem` and owns one GLFW window plus one OpenGL 4.6 Core context for one subsystem lifetime. It also exposes bounded owner-thread event polling and keeps logical window dimensions separate from framebuffer pixel dimensions.

## Constructors

The original constructor remains available:

```java
public GlfwWindow(
        int width,
        int height,
        String title,
        EngineLogger logger,
        NativeResourceRegistry nativeResources)
```

Use the size-aware overload when a caller needs window/framebuffer dimensions:

```java
public GlfwWindow(
        int width,
        int height,
        String title,
        EngineLogger logger,
        NativeResourceRegistry nativeResources,
        WindowSizeListener sizeListener)
```

Construction performs no native work.

Validation:

- width must be positive;
- height must be positive;
- title must be non-null and nonblank;
- logger must be non-null;
- registry must be non-null;
- the explicit `WindowSizeListener` must be non-null.

## Size listener

`WindowSizeListener` is renderer-neutral:

```java
public interface WindowSizeListener {
    void onLogicalWindowSizeChanged(int width, int height);
    void onFramebufferSizeChanged(int width, int height);
}
```

The channels are deliberately separate:

- logical window dimensions use GLFW screen-coordinate units and are suitable for window/layout reasoning;
- framebuffer dimensions are pixel dimensions and are the dimensions renderer-side code should use for pixel-sized targets or viewports;
- framebuffer dimensions may contain a zero axis, including `0x0`, while a window is minimized;
- negative dimensions are treated as a platform-contract failure and are not delivered to the listener.

Do not infer framebuffer pixels from logical dimensions or vice versa. They may be equal on a 100% scaling environment and different under DPI scaling.

## Normal lifetime

```java
WindowSizeListener sizes = new WindowSizeListener() {
    @Override
    public void onLogicalWindowSizeChanged(int width, int height) {
        // Window/layout size in screen coordinates.
    }

    @Override
    public void onFramebufferSizeChanged(int width, int height) {
        // Pixel size for renderer-facing work.
    }
};

GlfwWindow window = new GlfwWindow(
        1280,
        720,
        "My Game",
        logger,
        nativeResources,
        sizes);

window.initialize();
window.start();

window.pollEvents();

window.stop();
window.close();
```

All native-bearing lifecycle calls and `pollEvents()` must remain on the thread that initialized this `GlfwWindow`.

## What `initialize()` does

The current production contract:

- captures the owner/lifecycle thread;
- installs a task-owned GLFW error callback while preserving any previous callback;
- initializes GLFW;
- resets default hints;
- requests OpenGL 4.6 Core;
- requests forward compatibility;
- creates a hidden, resizable window;
- registers the nonzero GLFW window handle in the supplied `NativeResourceRegistry`.

## What `start()` does

- makes the context current;
- creates LWJGL OpenGL capabilities;
- verifies actual OpenGL 4.6 support;
- queries nonblank `GL_VERSION` and `GL_RENDERER`;
- logs both through `EngineLogger` at INFO with `subsystem=platform`;
- installs owned logical-window and framebuffer-size callbacks;
- queries the actual initial logical size and framebuffer size independently and stages them for delivery;
- enables owner-thread event polling;
- shows the window only after setup succeeds.

Expected log message forms:

```text
OpenGL version: <actual GL_VERSION>
OpenGL renderer: <actual GL_RENDERER>
```

## Event polling and delivery

```java
public void pollEvents()
```

`pollEvents()` is legal only while the window is successfully started. It:

1. verifies the initializing/owner thread;
2. calls GLFW event polling once;
3. delivers the latest pending logical size first;
4. delivers the latest pending framebuffer size second.

Native GLFW callbacks do not invoke consumer code directly. They only stage the latest dimensions. Multiple native notifications in one poll may therefore coalesce to the latest value per channel.

Listener `RuntimeException` or `Error` failures propagate to the caller unchanged. There is no asynchronous worker and no thread-safety guarantee; callers externally serialize access under the existing platform ownership contract.

## What `stop()` / `close()` do

`stop()` disables event polling, clears undelivered staged sizes, releases the owned size callbacks, hides the window, detaches its context, and clears thread-local OpenGL capabilities. Cleanup continues through later steps if an earlier cleanup action fails.

`close()` performs terminal cleanup, including any remaining size-callback cleanup, window-registration close/destruction, GLFW termination, restoration of the previous GLFW error callback, and freeing only callbacks owned by this `GlfwWindow`.

## What is intentionally not exposed yet

`GlfwWindow` still has no public API for:

- raw GLFW handle access;
- buffer swapping;
- fullscreen transitions;
- focus policy;
- keyboard/mouse/controller input;
- raw mouse capture;
- content-scale callbacks as a production API;
- OpenGL debug callback;
- multi-window/shared-context management;
- restartability.

Those belong to later bounded tasks and must not be inferred from the underlying LWJGL library.

See [Create a window example](../EXAMPLES/CREATE_A_WINDOW.md).
