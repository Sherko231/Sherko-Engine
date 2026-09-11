# GLFW/OpenGL window

`com.samo.engine.platform.api.GlfwWindow` is the current production window/context boundary.

It extends `EngineSubsystem` and owns one GLFW window plus one OpenGL 4.6 Core context for one subsystem lifetime.

## Constructor

```java
public GlfwWindow(
        int width,
        int height,
        String title,
        EngineLogger logger,
        NativeResourceRegistry nativeResources)
```

Construction performs no native work.

Validation:

- width must be positive;
- height must be positive;
- title must be non-null and nonblank;
- logger must be non-null;
- registry must be non-null.

## Normal lifetime

```java
GlfwWindow window = new GlfwWindow(
        1280,
        720,
        "My Game",
        logger,
        nativeResources);

window.initialize();
window.start();

// context/window are active here

window.stop();
window.close();
```

All native-bearing lifecycle calls must remain on the thread that initialized this `GlfwWindow`.

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
- shows the window only after those checks succeed.

Expected log message forms:

```text
OpenGL version: <actual GL_VERSION>
OpenGL renderer: <actual GL_RENDERER>
```

## What `stop()` / `close()` do

`stop()` hides the window, detaches its context, and clears thread-local OpenGL capabilities.

`close()` performs terminal cleanup, including window-registration close/destruction, GLFW termination, restoration of the previous GLFW error callback, and freeing only the callback owned by this `GlfwWindow`.

## What is intentionally not exposed yet

`GlfwWindow` currently has no public API for:

- raw GLFW handle access;
- buffer swapping;
- event polling;
- logical/framebuffer size events;
- fullscreen transitions;
- keyboard/mouse/controller input;
- raw mouse capture;
- OpenGL debug callback;
- multi-window/shared-context management;
- restartability.

Those belong to later bounded tasks and must not be inferred from the underlying LWJGL library.

See [Create a window example](../EXAMPLES/CREATE_A_WINDOW.md).
