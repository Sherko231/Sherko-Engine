# Renderer-frame input snapshots

`com.samo.engine.platform.api.InputSnapshot` is the current public hardware-state view produced by a started `GlfwWindow`.

It is deliberately device-level input, not the later data-driven action layer. Consumers read engine-defined keys/buttons and relative mouse movement without importing GLFW or LWJGL.

## Capture once per renderer frame

The intended client pattern is:

```java
long frameId = 0L;

while (running) {
    window.pollEvents();
    InputSnapshot input = window.captureInputSnapshot(frameId++);

    updateUi(input);
    updateGameplayInput(input);
}
```

`captureInputSnapshot(...)`:

- is legal only while `GlfwWindow` is STARTED;
- must run on the same owner thread that initialized the window;
- requires a non-negative caller-owned `frameId`;
- does **not** call GLFW event polling itself;
- returns an immutable snapshot that remains stable after later polls or snapshots.

Multiple systems may therefore read the same `InputSnapshot` object during one renderer frame and observe identical values.

## Snapshot state

Available frame metadata:

```java
input.frameId();
input.focused();
input.cursorCaptured();
input.mouseDeltaX();
input.mouseDeltaY();
```

Keyboard queries use `InputKey`:

```java
input.keyHeld(InputKey.W);
input.keyPressed(InputKey.SPACE);
input.keyReleased(InputKey.ESCAPE);
```

Mouse-button queries use `InputMouseButton`:

```java
input.mouseButtonHeld(InputMouseButton.LEFT);
input.mouseButtonPressed(InputMouseButton.RIGHT);
input.mouseButtonReleased(InputMouseButton.MIDDLE);
```

Passing `null` to a key/button query is a programmer error and fails immediately.

## Supported keyboard vocabulary

`InputKey` currently contains:

- `W`, `A`, `S`, `D`
- `SPACE`
- `LEFT_SHIFT`, `RIGHT_SHIFT`
- `LEFT_CONTROL`, `RIGHT_CONTROL`
- `LEFT_ALT`, `RIGHT_ALT`
- `ESCAPE`
- `E`, `Q`, `R`, `F`

This is a bounded hardware vocabulary for the current Phase 3 input foundation. It is not a binding/action list.

## Supported mouse buttons

`InputMouseButton` currently contains:

- `LEFT`
- `RIGHT`
- `MIDDLE`
- `BUTTON_4`
- `BUTTON_5`

No GLFW integer code is part of the public API.

## Held versus pressed/released

Held state is a level and persists across snapshots while the hardware remains down.

Pressed/released are retained hardware edges since the previous successful snapshot capture. They are consumed by the next successful snapshot.

For example, if `W` is pressed and released entirely between two snapshots, the next snapshot may report:

```java
input.keyHeld(InputKey.W);      // false
input.keyPressed(InputKey.W);   // true
input.keyReleased(InputKey.W);  // true
```

A following snapshot with no new events reports both edges as false.

GLFW key-repeat keeps the key held but does not create another pressed edge.

These are raw hardware edges. Data-driven action transitions across one or more bindings are not implemented yet.

## Relative mouse movement

P3-T05 collects relative cursor movement internally while effective cursor capture is active. P3-T06 exposes that accumulated movement through each snapshot.

A successful snapshot consumes the accumulated X/Y delta. A second snapshot without new eligible movement reports zero.

Snapshot capture does **not** reset the D-035 movement baseline, so motion remains continuous across ordinary frame boundaries. Capture/focus/lifecycle transitions still invalidate the baseline where required to suppress re-entry spikes.

When GLFW raw mouse motion is supported, captured motion uses raw mode. Otherwise the platform uses the documented disabled-cursor position-delta fallback; the fallback does not claim to bypass operating-system pointer acceleration.

## Focus loss

Focus loss follows the existing platform safety rules:

- held keyboard/mouse-button state is cleared;
- held supported inputs produce pending release edges for the next snapshot;
- stale pending presses are discarded;
- pending mouse motion is cleared;
- effective cursor capture/raw mode is released;
- focus regain does not synthesize input or automatically recapture.

A caller must explicitly call `window.setCursorCaptured(true)` when gameplay should resume.

## Headless and replay boundary

`InputSnapshot` belongs to the client/platform hardware boundary in `engine-platform-lwjgl`. The headless server does not depend on this module.

The later P3-T09 `PlayerInputCommand` task owns the device-neutral, tick-aligned replay/network-friendly command boundary. Do not make headless gameplay call GLFW or add a platform dependency to the server merely to construct snapshots.

## Not implemented by this API

`InputSnapshot` does not provide:

- JSON/data-driven input actions;
- action-level pressed/held/released aggregation;
- controller input;
- sensitivity or Y inversion;
- controller dead zones/curves;
- tick-aligned `PlayerInputCommand` records;
- replay or network serialization;
- camera/gameplay behavior.

See [GLFW/OpenGL window](GLFW_WINDOW.md) for lifecycle, polling, focus, cursor-capture, and display-mode ownership.
