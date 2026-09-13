# Platform input

`com.samo.engine.platform.api.InputSnapshot` is the public renderer-frame hardware-state view produced by a started `GlfwWindow`. P3-T07 additionally provides immutable, data-driven gameplay binding metadata loaded from strict versioned JSON.

The hardware snapshot and binding configuration are deliberately separate from action evaluation. Consumers can describe gameplay actions without importing GLFW/LWJGL or Jackson, but P3-T08 still owns action-level pressed/held/released and analog aggregation.

## Capture hardware once per renderer frame

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

## Supported hardware vocabulary

`InputKey` currently contains:

- `W`, `A`, `S`, `D`
- `SPACE`
- `LEFT_SHIFT`, `RIGHT_SHIFT`
- `LEFT_CONTROL`, `RIGHT_CONTROL`
- `LEFT_ALT`, `RIGHT_ALT`
- `ESCAPE`
- `E`, `Q`, `R`, `F`

`InputMouseButton` currently contains:

- `LEFT`
- `RIGHT`
- `MIDDLE`
- `BUTTON_4`
- `BUTTON_5`

No GLFW integer code is part of the public API.

## Held versus pressed/released hardware edges

Held state is a level and persists across snapshots while the hardware remains down.

Pressed/released are retained hardware edges since the previous successful snapshot capture. They are consumed by the next successful snapshot.

For example, if `W` is pressed and released entirely between two snapshots, the next snapshot may report:

```java
input.keyHeld(InputKey.W);      // false
input.keyPressed(InputKey.W);   // true
input.keyReleased(InputKey.W);  // true
```

A following snapshot with no new events reports both edges as false. GLFW key-repeat keeps the key held but does not create another pressed edge.

These remain raw hardware edges. Action-level transition semantics are not implemented by P3-T07.

## Relative mouse movement

P3-T05 collects relative cursor movement internally while effective cursor capture is active. P3-T06 exposes that accumulated movement through each snapshot.

A successful snapshot consumes the accumulated X/Y delta. A second snapshot without new eligible movement reports zero. Snapshot capture does **not** reset the D-035 movement baseline, so motion remains continuous across ordinary frame boundaries.

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

## Gameplay actions

P3-T07 defines exactly these `InputAction` values:

- `MOVE`
- `LOOK`
- `JUMP`
- `CROUCH`
- `SPRINT`
- `INTERACT`
- `GRAB`
- `THROW`
- `PRIMARY_USE`
- `PAUSE`
- `PUSH_TO_TALK`

`MOVE` and `LOOK` report `InputActionValueType.VECTOR2`. The remaining nine actions report `InputActionValueType.DIGITAL`.

The type is part of the action itself:

```java
InputAction.MOVE.valueType(); // VECTOR2
InputAction.JUMP.valueType(); // DIGITAL
```

## Binding descriptors

`InputBinding` is immutable and connects one control to one action component with a finite non-zero signed scale.

Supported control descriptors are:

```java
new InputBinding.KeyControl(InputKey.W)
new InputBinding.MouseButtonControl(InputMouseButton.LEFT)
new InputBinding.MouseDeltaControl(InputBinding.MouseDeltaAxis.X)
```

Components use `InputActionComponent`:

- digital actions require `VALUE`;
- vector actions require `X` or `Y`.

For example:

```java
InputBinding forward = new InputBinding(
        new InputBinding.KeyControl(InputKey.W),
        InputActionComponent.Y,
        1.0);
```

P3-T07 stores this metadata only. It does not evaluate an `InputSnapshot`, combine simultaneous bindings, or derive action transitions.

## Loading bindings from JSON

Load one complete immutable binding set with:

```java
InputActionBindings bindings = InputActionBindings.load(path);
List<InputBinding> moveBindings = bindings.bindingsFor(InputAction.MOVE);
```

The loader uses Jackson internally, but Jackson types are not exposed in the public API.

The schema is versioned. Current files require:

```json
{
  "schemaVersion": 1,
  "actions": [
    {
      "action": "MOVE",
      "bindings": [
        {"type":"KEY","key":"W","component":"Y","scale":1.0},
        {"type":"KEY","key":"S","component":"Y","scale":-1.0},
        {"type":"KEY","key":"D","component":"X","scale":1.0},
        {"type":"KEY","key":"A","component":"X","scale":-1.0}
      ]
    },
    {
      "action": "LOOK",
      "bindings": [
        {"type":"MOUSE_DELTA","axis":"X","component":"X","scale":1.0},
        {"type":"MOUSE_DELTA","axis":"Y","component":"Y","scale":1.0}
      ]
    },
    {
      "action": "JUMP",
      "bindings": [
        {"type":"KEY","key":"SPACE","component":"VALUE","scale":1.0}
      ]
    }
  ]
}
```

A real accepted document must contain all eleven actions exactly once. The abbreviated snippet above demonstrates the field shapes only.

Supported binding `type` values are:

- `KEY` with `key`, `component`, and `scale`;
- `MOUSE_BUTTON` with `button`, `component`, and `scale`;
- `MOUSE_DELTA` with `axis`, `component`, and `scale`.

The loader is intentionally strict. Unknown properties, unknown enum/control names, missing or duplicate actions, empty action binding lists, exact duplicate binding descriptors, invalid action components, zero/non-finite scale, malformed JSON, an unsupported schema version, or a missing/unreadable file fail without returning a partial set. User-file failures surface as `InputBindingLoadException`.

The returned map/lists are immutable and defensively owned:

```java
bindings.asMap();
bindings.bindingsFor(InputAction.JUMP);
```

## Headless and replay boundary

`InputSnapshot` and P3-T07 binding metadata currently belong to the client/platform input module. The headless server does not depend on `engine-platform-lwjgl`.

The later P3-T09 `PlayerInputCommand` task owns the device-neutral, tick-aligned replay/network-friendly command boundary. Do not make headless gameplay call GLFW or add a platform dependency to the server merely to construct snapshots or load bindings.

## Not implemented yet

The current input APIs do not provide:

- action-level pressed/held/released or analog aggregation;
- simultaneous-binding conflict/aggregation semantics;
- controller input;
- sensitivity or Y inversion;
- controller dead zones/curves;
- tick-aligned `PlayerInputCommand` records;
- replay or network serialization;
- camera/gameplay behavior.

See [GLFW/OpenGL window](GLFW_WINDOW.md) for lifecycle, polling, focus, cursor-capture, and display-mode ownership.
