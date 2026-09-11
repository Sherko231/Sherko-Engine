# Timing and fixed-step simulation

The Phase 2 timing foundation is built around three public types:

- `EngineClock`
- `FixedStepAccumulator`
- `FixedStepCatchUpPolicy`

## `EngineClock`

`EngineClock` samples elapsed time from a monotonic nanosecond source. Use it for engine elapsed-time measurement rather than wall-clock time.

The clock reports elapsed nanoseconds; the caller owns the update loop and decides when to sample.

## `FixedStepAccumulator`

`FixedStepAccumulator` converts elapsed nanoseconds into whole fixed simulation ticks while retaining fractional progress for interpolation.

Current engine simulation rate: **60 Hz**.

Conceptually:

```text
real elapsed time
      |
      v
FixedStepAccumulator
      |
      +--> whole 60 Hz simulation steps
      |
      +--> interpolation alpha for rendering/presentation
```

The caller executes the returned fixed steps. The accumulator itself does not run game logic.

## `FixedStepCatchUpPolicy`

Use the catch-up policy before exposing large time gaps to fixed simulation.

Current accepted baseline:

- frame-gap clamp: 250 ms;
- maximum fixed steps exposed by one update: 5;
- excess whole ticks are discarded;
- fractional progress is retained.

This prevents one large stall from producing an unbounded simulation catch-up spiral.

## Intended loop shape

At the current engine stage the timing pieces are foundations rather than a complete public runtime-loop abstraction. A caller/runtime composition layer is expected to:

1. sample monotonic elapsed time;
2. apply the catch-up policy;
3. feed bounded elapsed time into the fixed-step accumulator;
4. execute each exposed 60 Hz simulation tick;
5. use interpolation alpha for renderer-facing interpolation.

Do not invent a different tick rate through configuration: `simulation.tickRate` is currently validated against the locked 60 Hz rate.

## What this page does not promise

- no general scheduler;
- no broad job system;
- no deterministic native-physics lockstep;
- no completed render loop yet.
