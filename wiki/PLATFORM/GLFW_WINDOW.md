# GLFW/OpenGL window

`com.samo.engine.platform.api.GlfwWindow` is the current production window/context and hardware-input producer boundary.

It extends `EngineSubsystem` and owns one GLFW window plus one OpenGL 4.6 Core context for one subsystem lifetime. It exposes bounded owner-thread event polling, keeps logical window dimensions separate from framebuffer pixel dimensions, can switch the same native window/context among windowed, borderless-fullscreen, and exclusive-fullscreen modes, owns focus-loss-safe cursor capture, and can produce one immutable hardware `InputSnapshot` for a caller-defined renderer frame.

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
Use `OpenGlDebugMode.FAIL_ON_HIGH_SEVERITY` only for explicit development/test diagnostics. Existing constructors default to `OpenGlDebugMode.DISABLED`:

```java
public GlfwWindow(
        int width,
        int height,
        String title,
        EngineLogger logger,
        NativeResourceRegistry nativeResources,
        OpenGlDebugMode openGlDebugMode)
```

A corresponding overload also accepts both `WindowSizeListener` and `OpenGlDebugMode`.

Construction performs no native work.

Validation:

- width must be positive;
- height must be positive;
- title must be non-null and nonblank;
- logger must be non-null;
- registry must be non-null;
- the explicit `WindowSizeListener` must be non-null;
- the explicit `OpenGlDebugMode` must be non-null.

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

Current policy:

- capture uses GLFW's disabled-cursor mode while the window is focused;
- if GLFW raw mouse motion is supported, capture enables it; otherwise relative movement uses the documented disabled-cursor position-delta fallback;
- releasing capture restores the normal cursor and clears pending relative motion;
- focus loss clears held keyboard/mouse-button state and records release edges for supported inputs that were held;
- stale pending press edges and pending mouse motion are cleared on focus loss;
- focus loss releases effective cursor/raw capture;
- focus regain does **not** automatically capture the cursor again;
- the caller must explicitly invoke `setCursorCaptured(true)` after focus regain when gameplay should resume.

The explicit-recapture rule prevents the pointer from unexpectedly locking when a user returns from Alt+Tab. Game/UI composition remains responsible for deciding when gameplay should resume.

If native cursor/raw release fails inside a focus callback, that failure is not thrown through the native callback boundary. The window clears safety/input state first, stages the original failure, and throws it once from the owning `pollEvents()` call after GLFW polling returns.

## Renderer-frame input snapshot

P3-T06 adds:

```java
public InputSnapshot captureInputSnapshot(long frameId)
```

The method is legal only while STARTED and on the owner thread. `frameId` is caller-owned and must be non-negative.

`captureInputSnapshot(...)` does **not** call GLFW polling. A renderer-frame loop should call `pollEvents()` first, then capture exactly the frame view it wants consumers to share:

```java
window.pollEvents();
InputSnapshot input = window.captureInputSnapshot(frameId++);
```

A successful snapshot contains:

- the supplied frame ID;
- current focus/effective cursor-capture state;
- held supported keys/buttons;
- supported key/button press and release edges retained since the prior successful snapshot;
- accumulated relative mouse X/Y movement since the prior successful snapshot.

A successful snapshot consumes pending press/release edges and mouse delta for the next interval. Held levels remain unchanged, and snapshot capture does not reset the relative-motion baseline. A press and release that both occur between two snapshots can therefore appear as `pressed=true`, `released=true`, `held=false` in the next snapshot instead of being lost.

The snapshot and its `InputKey` / `InputMouseButton` vocabulary expose no GLFW/LWJGL types or integer constants. See [Renderer-frame input snapshots](INPUT.md) for the complete consumer contract.

## OpenGL debug mode

`OpenGlDebugMode.DISABLED` is the default and does not request a debug context or install an OpenGL debug callback.

`OpenGlDebugMode.FAIL_ON_HIGH_SEVERITY` is intended for development/test use. It requests a GLFW OpenGL debug context during `initialize()`, verifies the actual context debug flag during `start()`, and installs one callback owned by the same `GlfwWindow` lifecycle.

Each driver message is normalized into engine-owned source/type/severity names and logged through `EngineLogger`. High-severity messages are staged as failures; they are not thrown from inside the native callback. The next owner-thread `pollEvents()` throws that staged failure once. Medium severity logs at WARN; low/notification logs at DEBUG.

The callback is released before context detachment and LWJGL capability clearing during stop, failed start, or close. No raw OpenGL callback/handle is exposed to consumers.

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

long frameId = 0L;
window.pollEvents();
InputSnapshot input = window.captureInputSnapshot(frameId++);

window.setCursorCaptured(true);
window.setWindowMode(WindowMode.BORDERLESS_FULLSCREEN);
window.pollEvents();
input = window.captureInputSnapshot(frameId++);

// If focus is lost, capture is released automatically.
// After focus returns, recapture is explicit:
window.setCursorCaptured(true);

window.setWindowMode(WindowMode.WINDOWED);
window.setCursorCaptured(false);
window.stop();
window.close();
```

All native-bearing lifecycle calls, `pollEvents()`, `captureInputSnapshot(...)`, `setWindowMode(...)`, and `setCursorCaptured(...)` must remain on the thread that initialized this `GlfwWindow`.

## What `initialize()` does

The current production contract:

- captures the owner/lifecycle thread;
- installs a task-owned GLFW error callback while preserving any previous callback;
- initializes GLFW;
- resets default hints;
- requests OpenGL 4.6 Core;
- requests forward compatibility;
- when debug mode is enabled, requests a GLFW OpenGL debug context;
- creates a hidden, resizable window;
- registers the nonzero GLFW window handle in the supplied `NativeResourceRegistry`.

## What `start()` does

- makes the context current;
- creates LWJGL OpenGL capabilities;
- verifies actual OpenGL 4.6 support;
- queries nonblank `GL_VERSION` and `GL_RENDERER`;
- logs both through `EngineLogger` at INFO with `subsystem=platform`;
- when debug mode is enabled, verifies the actual debug-context flag and installs the owned OpenGL debug callback;
- installs owned logical-window and framebuffer-size callbacks;
- installs owned window-focus, key, mouse-button, and cursor-position callbacks;
- queries initial native focus state;
- queries the actual initial logical size and framebuffer size independently and stages them for delivery;
- establishes `WINDOWED` as the initial public window mode;
- establishes uncaptured/empty hardware input state;
- enables owner-thread event polling/window-mode/cursor-capture/snapshot operations;
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
3. propagates any staged OpenGL debug failure once;
4. propagates any staged focus/cursor/raw failure once;
5. delivers the latest pending logical size first;
6. delivers the latest pending framebuffer size second.

Native size callbacks do not invoke consumer code directly. They only stage the latest dimensions. Multiple native notifications in one poll may therefore coalesce to the latest value per channel. Mode changes continue to use this same delivery path for resulting logical/framebuffer notifications.

Focus/key/mouse-button/cursor-position callbacks update platform-owned hardware state only. Consumers read that state through the immutable `InputSnapshot` boundary rather than receiving native callbacks or querying GLFW directly.

Listener `RuntimeException` or `Error` failures propagate to the caller unchanged. There is no asynchronous worker and no thread-safety guarantee; callers externally serialize access under the existing platform ownership contract.

## What `stop()` / `close()` do

`stop()` disables event polling/window-mode/cursor-capture/snapshot operations, clears undelivered staged sizes and saved windowed restore geometry, clears held/pending input and relative-motion state, restores a normal cursor when capture is effectively active, releases owned focus/key/mouse-button/cursor-position, size, and OpenGL debug callbacks, hides the window, detaches its context, and clears thread-local OpenGL capabilities. Cleanup continues through later steps if an earlier cleanup action fails.

`close()` performs terminal cleanup, including any remaining input/size-callback cleanup, window-registration close/destruction, GLFW termination, restoration of the previous GLFW error callback, and freeing only callbacks owned by this `GlfwWindow`.

## What is intentionally not exposed yet

`GlfwWindow` still has no public API for:

- raw GLFW window/monitor handle access;
- buffer swapping;
- choosing a non-primary monitor;
- custom fullscreen resolution or refresh-rate selection;
- arbitrary GLFW key/button codes;
- a public raw-mouse-mode toggle;
- data-driven action bindings/transitions;
- controller input/curves/dead zones;
- tick-aligned `PlayerInputCommand` / replay input;
- content-scale callbacks as a production API;
- multi-window/shared-context management;
- restartability.

Those belong to later bounded tasks and must not be inferred from the underlying LWJGL library.

See [Renderer-frame input snapshots](INPUT.md) and [Create a window example](../EXAMPLES/CREATE_A_WINDOW.md).
