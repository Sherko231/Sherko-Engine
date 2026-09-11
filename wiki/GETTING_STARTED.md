# Getting started

Sherko Engine is currently a Java 25 multi-project engine under active development. The public API is still growing, so this guide documents only functionality already implemented in production modules.

## Requirements

- Java 25.
- Windows x64 for the current native platform target.
- A GPU/driver capable of OpenGL 4.6 Core for the current `GlfwWindow` production path.
- The repository Gradle build when working directly from source.

## Main public packages available today

```text
com.samo.engine.core.api
com.samo.engine.platform.api
```

`engine-core` currently contains lifecycle, subsystem ordering/startup, timing, configuration, native ownership diagnostics, structured logging, and fatal-shutdown foundations.

`engine-platform-lwjgl` currently exposes the first concrete production platform subsystem: `GlfwWindow`.

## The lifecycle pattern

Production subsystems derive from `EngineSubsystem`. A normal owned lifetime is:

```java
subsystem.initialize();
subsystem.start();

// use the running subsystem

subsystem.stop();
subsystem.close();
```

Important rules:

- Lifecycle calls are externally serialized by the owner.
- `EngineSubsystem` is not thread-safe.
- A running subsystem must be stopped before normal close.
- A failed initialize/start/stop still permits a terminal `close()` attempt.
- Restartability is not implied.

See [Lifecycle](CORE/LIFECYCLE.md).

## First visible engine object: a GLFW window

The current production window API is:

```java
GlfwWindow window = new GlfwWindow(
        1280,
        720,
        "My Game",
        logger,
        nativeResources);
```

Construction validates arguments but performs no native work. GLFW/OpenGL creation starts at `initialize()`, and the context becomes active at `start()`.

See [GLFW/OpenGL window](PLATFORM/GLFW_WINDOW.md) and [Create a window example](EXAMPLES/CREATE_A_WINDOW.md).

## Before using an API

Check [Current API index](API_INDEX.md). If an API is not listed there, inspect current production source before assuming it exists. Do not infer future APIs from the roadmap.
