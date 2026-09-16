# Platform input

`com.samo.engine.platform.api.InputSnapshot` is the public renderer-frame hardware-state view produced by a started `GlfwWindow`. P3-T07 adds immutable data-driven action-binding metadata, P3-T08 evaluates those inputs into immutable renderer-frame gameplay action state, P3-T09 bridges that state into device-neutral simulation-tick commands, and P3-T10 adds deterministic input-response settings without changing the tick-command contract.

The layers remain deliberately separate:

1. `GlfwWindow` / `InputSnapshot` own hardware observation;
2. `InputActionBindings` owns configuration metadata;
3. `engine-core` `InputResponseSettings` owns deterministic response math;
4. caller-owned `InputActionEvaluator` owns renderer-frame response application, action aggregation, and transitions;
5. caller-owned `PlayerInputCommandSampler` bridges renderer-frame action state to simulation ticks;
6. `engine-core` `PlayerInputCommand` + `PlayerInputCommandCodec` own the headless/replay-friendly tick command and explicit binary replay/storage encoding.

No public input API exposes GLFW/LWJGL or Jackson types. `InputResponseSettings`, `PlayerInputCommand`, and its codec are intentionally in `engine-core`, so shared/headless code can use those contracts without depending on `engine-platform-lwjgl`.

## Capture, evaluate, and sample

The intended client pattern is:

```java
InputActionBindings bindings = InputActionBindings.load(bindingPath);
InputResponseSettings response = new InputResponseSettings(1.5, true, 0.2, 2.0);
InputActionEvaluator evaluator = new InputActionEvaluator(bindings, response);
PlayerInputCommandSampler sampler = new PlayerInputCommandSampler();
long frameId = 0L;
long tickId = 0L;

while (running) {
    long elapsedNanos = clock.sampleElapsedNanos();
    long dueTicks = catchUpPolicy.advance(accumulator, elapsedNanos);

    window.pollEvents();
    InputSnapshot hardware = window.captureInputSnapshot(frameId++);
    InputActionSnapshot actions = evaluator.evaluate(hardware);
    sampler.submit(actions);

    for (long i = 0; i < dueTicks; i++) {
        PlayerInputCommand command = sampler.nextCommand(tickId++);
        simulate(command);
    }
}
```

`captureInputSnapshot(...)`:

- is legal only while `GlfwWindow` is STARTED;
- must run on the same owner thread that initialized the window;
- requires a non-negative caller-owned `frameId`;
- does **not** call GLFW event polling itself;
- returns an immutable snapshot that remains stable after later polls or snapshots.

`InputActionEvaluator.evaluate(...)`:

- accepts one immutable hardware snapshot;
- evaluates every required `InputAction` using its immutable binding set;
- applies the current `InputResponseSettings` to mouse-delta controls before binding scale/aggregation;
- requires strictly increasing frame IDs after the first successful evaluation; gaps are allowed;
- returns a complete immutable `InputActionSnapshot` using the same source frame ID;
- does not mutate its previous-frame baseline if evaluation fails.

`PlayerInputCommandSampler`:

- accepts successful `InputActionSnapshot` values through `submit(...)`;
- requires strictly increasing submitted frame IDs after the first successful submit;
- requires at least one successful submit before the first `nextCommand(...)`;
- requires non-negative, strictly increasing tick IDs after the first successful command;
- is caller-owned and externally serialized, not a shared thread-safe service;
- does not own the clock, fixed-step accumulator, catch-up policy, or simulation execution.

## Snapshot state

Available hardware-frame metadata:

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

No GLFW integer code is part of the public API. Controller hardware vocabulary is not implemented yet.

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

These are hardware edges. `InputActionEvaluator` converts them into action-level state using aggregate action activity plus the one-frame-tap rule described below.

## Relative mouse movement

P3-T05 collects relative cursor movement internally while effective cursor capture is active. P3-T06 exposes that accumulated movement through each snapshot.

A successful snapshot consumes the accumulated X/Y delta. A second snapshot without new eligible movement reports zero. Snapshot capture does **not** reset the D-035 movement baseline, so motion remains continuous across ordinary frame boundaries.

When GLFW raw mouse motion is supported, captured motion uses raw mode. Otherwise the platform uses the documented disabled-cursor position-delta fallback; the fallback does not claim to bypass operating-system pointer acceleration.

## Input response settings

`com.samo.engine.core.api.InputResponseSettings` is an immutable device-response value:

```java
InputResponseSettings response = new InputResponseSettings(
        2.0,   // mouseSensitivity
        true,  // invertMouseY
        0.2,   // controllerDeadZone
        2.0);  // controllerCurveExponent
```

Neutral defaults are available through:

```java
InputResponseSettings.defaults(); // 1.0, false, 0.0, 1.0
```

Mouse response is deterministic and ordered:

1. validate the raw delta is finite;
2. multiply by `mouseSensitivity`;
3. for Y only, negate when `invertMouseY` is true;
4. reject non-finite output.

The evaluator applies that response **before** the binding's signed scale and before additive action aggregation. Example: sensitivity `2.0`, Y inversion enabled, raw LOOK `(3,-4)`, X scale `1.5`, Y scale `0.5` produces `(9,4)`.

The existing constructor remains source-compatible:

```java
InputActionEvaluator evaluator = new InputActionEvaluator(bindings);
```

It uses neutral defaults. An explicit response may be provided at construction or replaced later:

```java
InputActionEvaluator evaluator = new InputActionEvaluator(bindings, response);
evaluator.setResponseSettings(otherResponse);
```

Replacement affects only future `evaluate(...)` calls. Already-returned snapshots remain immutable, and the evaluator's previous frame identity/action activity is not reset.

Controller response is currently a pure axis-local scalar helper only:

```java
double shaped = response.applyControllerAxis(rawAxis);
```

The raw axis must be finite and in `[-1,1]`. For magnitude `a` and dead zone `d`:

```text
if a <= d: 0
else: sign(raw) * pow((a - d) / (1 - d), controllerCurveExponent)
```

For dead zone `0.2`, exponent `2.0`, raw `0.6` maps to `0.25`; `-0.6` maps to `-0.25`. This API does **not** mean controller discovery, polling, buttons/axes, callbacks, or action bindings are implemented.

Validation requires finite non-negative mouse sensitivity, dead zone in `[0,1)`, and a finite positive controller exponent.

## Focus loss

Focus loss follows the existing platform safety rules:

- held keyboard/mouse-button state is cleared;
- held supported inputs produce pending release edges for the next snapshot;
- stale pending presses are discarded;
- pending mouse motion is cleared;
- effective cursor capture/raw mode is released;
- focus regain does not synthesize input or automatically recapture.

A caller must explicitly call `window.setCursorCaptured(true)` when gameplay should resume. Evaluating the focus-loss snapshot therefore releases any action whose aggregate becomes inactive; the evaluator adds no second native/focus policy.

## Gameplay actions

The platform API defines exactly these `InputAction` values:

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

```java
InputAction.MOVE.valueType(); // VECTOR2
InputAction.JUMP.valueType(); // DIGITAL
```

The tick-command layer mirrors only the nine digital action names in `PlayerInputCommand.DigitalAction`; MOVE and LOOK have dedicated vector fields.

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

## Action values and aggregation

Every binding contributes independently to its configured component:

- a key binding contributes its signed scale while that key is held;
- a mouse-button binding contributes its signed scale while that button is held;
- a mouse-delta binding contributes `response-shaped mouse delta * signed scale` on the configured axis.

Contributions targeting the same component are added in binding order using ordinary finite Java `double` arithmetic. P3-T10 changes only mouse-delta response before scale/aggregation; there is still no general action clamp or normalization.

For a digital action:

```text
held/currently active <=> value != 0.0
```

For a vector action:

```text
held/currently active <=> x != 0.0 || y != 0.0
```

Exact signed cancellation is therefore inactive. For example, MOVE bindings W=`+Y` and S=`-Y` cancel to Y=0 when both are held.

`InputActionState` exposes:

```java
state.action();
state.pressed();
state.held();
state.released();
state.value();
state.x();
state.y();
```

For DIGITAL actions, `x()` and `y()` are zero. For VECTOR2 actions, `value()` is zero.

Non-finite hardware mouse delta, response overflow, binding multiplication overflow, or non-finite aggregate values are rejected before the evaluator advances its previous-frame state.

## Action transitions

For ordinary sampled transitions:

- `held` equals current aggregate activity;
- `pressed` is true when the action was inactive after the previous successful evaluation and is active now;
- `released` is true when the action was active after the previous successful evaluation and is inactive now.

The transition belongs to the **action aggregate**, not to each physical binding. Therefore:

- pressing a second positive binding while the action is already active does not produce another action press;
- releasing one binding while another keeps the action aggregate active does not produce an action release;
- if opposite signed contributions cancel exactly, the action is inactive.

P3-T06 may retain a complete press+release between two hardware snapshots. P3-T08 preserves that tap only when the **same bound key or mouse button** reports both hardware edges in the current snapshot while the action was previously inactive and ends inactive. The result for that frame is:

```text
pressed=true, held=false, released=true
```

Press evidence from one binding plus release evidence from a different binding does not synthesize a tap because the hardware snapshot does not encode ordering between those controls.

Mouse-delta bindings have no discrete hardware edge. A non-zero response-shaped LOOK delta can therefore produce `pressed=true, held=true`; the first later zero result produces `released=true` if no other LOOK contribution remains active.

## From renderer frames to simulation ticks

Renderer frames and simulation ticks are intentionally not one-to-one. A render frame may produce zero, one, or multiple fixed simulation ticks.

`PlayerInputCommandSampler` handles the mismatch with these rules:

- MOVE X/Y: latest successfully submitted renderer-frame values are copied into every emitted tick command until replaced by a newer frame;
- digital scalar + held: latest successfully submitted values repeat across emitted ticks;
- digital pressed/released: pending edges are OR-retained across submitted frames until the next command, then cleared;
- LOOK X/Y: already response-shaped deltas add across submitted frames until the next command, then reset to zero;
- a second tick without a newer submitted frame receives the same MOVE/digital level state but zero LOOK and no repeated edges.

This preserves a P3-T08 one-frame tap even when no simulation tick occurs in that renderer frame. For example, two renderer frames with evaluated LOOK X values `2.0` and `1.5` plus a complete JUMP tap followed by one simulation tick produce one command with LOOK X=`3.5` and JUMP `pressed=true, held=false, released=true`.

A duplicate/decreasing submitted frame ID or duplicate/decreasing tick ID fails without consuming pending state. Non-finite submitted values or LOOK accumulation overflow are also rejected before sampler state advances.

## `PlayerInputCommand`

`PlayerInputCommand` is the device-neutral simulation-tick value in `engine-core`:

```java
command.tickId();
command.moveX();
command.moveY();
command.lookX();
command.lookY();
command.digitalState(PlayerInputCommand.DigitalAction.JUMP);
```

Each digital state contains:

```java
state.value();
state.pressed();
state.held();
state.released();
```

The command requires a non-negative tick ID and finite analog/scalar values. Its complete nine-action digital state is defensively owned and immutable.

## Replay/storage codec

`PlayerInputCommandCodec` uses caller-supplied `ByteBuffer` and explicit field encoding. It does not use Java object serialization.

Version 1 is exactly 126 bytes:

```text
4 bytes  magic "SPIC"
2 bytes  version = 1
2 bytes  reserved = 0
8 bytes  tickId
32 bytes MOVE/LOOK doubles
72 bytes nine digital scalar doubles
6 bytes  pressed/held/released 16-bit masks
```

The encoded field order is always big-endian regardless of the caller buffer's configured order. Successful encode/decode advances the caller buffer position by exactly 126 bytes while preserving its configured byte order.

Decode rejects truncated input, bad magic/version/reserved bits, unsupported mask bits, negative tick IDs, or non-finite values without accepting a partial command. The format is a replay/storage contract, not the final production network packet format.

## Frame/tick ordering and failure atomicity

The first successful action evaluation may use any non-negative hardware frame ID. Every later successful call on that evaluator must use a strictly greater frame ID. Duplicate or decreasing IDs are programmer errors; gaps are allowed.

The sampler separately tracks submitted action-frame IDs and emitted simulation tick IDs. Its first emitted command requires a previously submitted frame. Later frame/tick IDs must each increase strictly within their own sequence.

A failed evaluator/sampler operation does not advance its corresponding previous-state/identity baseline. Changing evaluator response settings alone also does not change frame identity/history. Already returned snapshots, states, and commands remain immutable and stable.

## Headless and replay boundary

`InputSnapshot`, action bindings, evaluator, and `PlayerInputCommandSampler` belong to the client/platform module. `InputResponseSettings`, `PlayerInputCommand`, and `PlayerInputCommandCodec` belong to `engine-core`.

The headless server does not depend on `engine-platform-lwjgl`. Replay/headless code consumes core commands only; it must not call GLFW or construct renderer-frame snapshots just to drive simulation.

## Not implemented yet

The current input APIs do not provide:

- controller discovery/polling/buttons/axes/callbacks/action bindings;
- radial controller-stick response;
- mouse acceleration or smoothing;
- per-axis mouse sensitivity;
- settings persistence or runtime settings UI;
- live remapping UI/hot reload;
- production networking integration of `PlayerInputCommand`;
- a production packet layout for tick input commands;
- camera/gameplay behavior;
- UI input-consumption/focus policy.

See [GLFW/OpenGL window](GLFW_WINDOW.md) for lifecycle, polling, focus, cursor-capture, and display-mode ownership.
