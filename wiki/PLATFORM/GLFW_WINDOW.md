# GLFW/OpenGL window

`com.samo.engine.platform.api.GlfwWindow` is the current production window/context boundary.

It extends `EngineSubsystem` and owns one GLFW window plus one OpenGL 4.6 Core context for one subsystem lifetime. It exposes bounded owner-thread event polling, keeps logical window dimensions separate from framebuffer pixel dimensions, can switch the same native window/context among windowed, borderless-fullscreen, and exclusive-fullscreen modes, and owns focus-loss-safe cursor capture.

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

## Window modes

The public display-mode enum is:

```java
public enum WindowMode {
    WINDOWED,
    BORDERLESS_FULLSCREEN,
    EXCLUSIVE_FULLSCREEN
}
```

A successfully started window changes mode with:

```java
window.setWindowMode(WindowMode.BORDERLESS_FULLSCREEN);
```

`setWindowMode` must run on the same owner thread that initialized the window. Calls before successful `start()` or after `stop()` fail. Passing `null` fails before native work, and requesting the already-active mode is a no-op.

P3-T03 intentionally targets the primary monitor only:

- leaving `WINDOWED` captures the current window position and logical size for later restoration;
- `BORDERLESS_FULLSCREEN` keeps the window detached from a monitor, disables decoration, and sizes/positions it to the primary monitor's current video mode and origin;
- `EXCLUSIVE_FULLSCREEN` attaches the same GLFW window to the primary monitor using that monitor's current video-mode dimensions and refresh rate;
- returning to `WINDOWED` detaches the window, re-enables decoration, and restores the captured windowed position/size;
- direct borderless ↔ exclusive changes preserve the same captured windowed restore geometry;
- after a successful return to windowed, a later fullscreen entry captures the then-current windowed geometry again.

The native window and OpenGL context are not recreated during a successful transition. If a backend transition throws a `RuntimeException` or `Error`, the original failure remains primary and one best-effort rollback to the previous mode is attempted; a rollback failure is attached as suppressed.

Monitor selection, custom resolution/refresh-rate selection, and raw monitor/window handles are not public APIs.

## Cursor capture and focus loss

A started window can request gameplay-style cursor capture:

```java
window.setCursorCaptured(true);
```

and release it with:

```java
window.setCursorCaptured(false);
```

`setCursorCaptured(...)` is legal only while STARTED and on the same owner thread that initialized the window.

Current P3-T04 policy:

- capture uses GLFW's disabled-cursor mode while the window is focused;
- releasing capture restores the normal cursor;
- focus loss immediately clears the platform boundary's internally tracked held keyboard/mouse-button state;
- focus loss releases effective cursor capture;
- focus regain does **not** automatically capture the cursor again;
- the caller must explicitly invoke `setCursorCaptured(true)` after focus regain when gameplay should resume;
- no public key/button snapshot or action API exists yet, so the tracked hardware state is intentionally internal until P3-T06.

The explicit-recapture rule prevents the pointer from unexpectedly locking when a user returns from Alt+Tab. Game/UI composition remains responsible for deciding when gameplay should resume.

If native cursor release fails inside a focus callback, that failure is not thrown through the native callback boundary. The window clears held input state first, stages the original failure, and throws it once from the owning `pollEvents()` call after GLFW polling returns.

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

window.setCursorCaptured(true);
window.setWindowMode(WindowMode.BORDERLESS_FULLSCREEN);
window.pollEvents();

// If focus is lost, capture is released automatically.
// After focus returns, recapture is explicit:
window.setCursorCaptured(true);

window.setWindowMode(WindowMode.WINDOWED);
window.setCursorCaptured(false);
window.stop();
window.close();
```

All native-bearing lifecycle calls, `pollEvents()`, `setWindowMode(...)`, and `setCursorCaptured(...)` must remain on the thread that initialized this `GlfwWindow`.

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
- installs owned window-focus, key, and mouse-button callbacks;
- queries initial native focus state;
- queries the actual initial logical size and framebuffer size independently and stages them for delivery;
- establishes `WINDOWED` as the initial public window mode;
- establishes uncaptured cursor/input state;
- enables owner-thread event polling/window-mode/cursor-capture operations;
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
3. propagates any staged focus/cursor failure once;
4. delivers the latest pending logical size first;
5. delivers the latest pending framebuffer size second.

Native size callbacks do not invoke consumer code directly. They only stage the latest dimensions. Multiple native notifications in one poll may therefore coalesce to the latest value per channel. Mode changes continue to use this same P3-T02 delivery path for resulting logical/framebuffer notifications.

Focus/key/mouse-button callbacks update platform-owned safety state only; there is still no public input snapshot/action callback surface.

Listener `RuntimeException` or `Error` failures propagate to the caller unchanged. There is no asynchronous worker and no thread-safety guarantee; callers externally serialize access under the existing platform ownership contract.

## What `stop()` / `close()` do

`stop()` disables event polling/window-mode/cursor-capture operations, clears undelivered staged sizes and saved windowed restore geometry, clears held input state, restores a normal cursor when capture is effectively active, releases owned focus/key/mouse-button callbacks and size callbacks, hides the window, detaches its context, and clears thread-local OpenGL capabilities. Cleanup continues through later steps if an earlier cleanup action fails.

`close()` performs terminal cleanup, including any remaining input/size-callback cleanup, window-registration close/destruction, GLFW termination, restoration of the previous GLFW error callback, and freeing only callbacks owned by this `GlfwWindow`.

## What is intentionally not exposed yet

`GlfwWindow` still has no public API for:

- raw GLFW window/monitor handle access;
- buffer swapping;
- choosing a non-primary monitor;
- custom fullscreen resolution or refresh-rate selection;
- public keyboard/mouse/controller state snapshots;
- raw mouse motion;
- action bindings/transitions;
- controller curves/dead zones;
- content-scale callbacks as a production API;
- OpenGL debug callback;
- multi-window/shared-context management;
- restartability.

Those belong to later bounded tasks and must not be inferred from the underlying LWJGL library.

See [Create a window example](../EXAMPLES/CREATE_A_WINDOW.md).
