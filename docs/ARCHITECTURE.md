# Sherko Engine Architecture

This document describes intended module responsibilities and the architecture actually present in the repository. Product constraints live in `ENGINE_SCOPE.md`; durable decisions live in `docs/DECISIONS.md`; changing progress lives in `docs/DEVELOPMENT_STATUS.md`.

## Maturity vocabulary

| State | Meaning |
| --- | --- |
| Implemented | Production-oriented source and tests exist for the stated responsibility. |
| Skeleton | Gradle module and dependency boundary exist, but production subsystem code does not. |
| Experimental | Disposable feasibility code exists and must not be treated as production architecture. |
| Planned | Roadmap contract exists, but the repository does not yet implement it. |

## Current repository architecture

The repository has a working Java 25 multi-project build with 17 declared Gradle subprojects: the 16 production-target engine/game/support modules defined by `ENGINE_SCOPE.md`, plus the experimental `feasibility-spikes` subproject. `engine-core` implements the single-subsystem lifecycle contract, graph-only dependency ordering, bounded startup rollback coordination, monotonic elapsed-time sampling, exact 60 Hz fixed-step accumulation, bounded frame-gap/catch-up policy, renderer-facing interpolation alpha, typed startup configuration validation, and deterministic layered startup configuration loading; concrete engine subsystems and `game-sandbox` remain skeletons, while `game-client` and `game-server` provide minimal executable composition roots for the foundation state. The root project is now a build, quality, and task-aggregation project with no Java source tree and no spike runtime dependencies.

| Module | Intended responsibility | Current state | Direct project dependencies |
| --- | --- | --- | --- |
| `engine-core` | Lifecycle, time, IDs, events, math/spatial contracts | Lifecycle (P2-T01), dependency ordering (P2-T02), startup rollback (P2-T03), monotonic clock (P2-T04), fixed-step accumulator (P2-T05), bounded catch-up policy (P2-T06), interpolation alpha exposure (P2-T07), typed config validation (P2-T08), layered config loading (P2-T09); other responsibilities planned | None |
| `engine-platform-lwjgl` | GLFW/window/input and platform-native boundary | Skeleton | `engine-core` |
| `engine-assets` | Runtime asset handles/formats and loading contracts | Skeleton | `engine-core` |
| `engine-ui` | Renderer-neutral runtime HUD/menu model and draw data | Skeleton | `engine-core`, `engine-assets` |
| `engine-render-opengl` | OpenGL renderer and runtime-UI draw adapter | Skeleton | `engine-core`, `engine-platform-lwjgl`, `engine-assets`, `engine-ui` |
| `engine-world` | Scene/world/component/prefab runtime | Skeleton | `engine-core`, `engine-assets` |
| `engine-physics-jolt` | Jolt-backed physics adapter and ownership | Skeleton | `engine-core` |
| `engine-audio-openal` | OpenAL-backed positional audio | Skeleton | `engine-core` |
| `engine-network-api` | Transport-neutral session/message contracts | Skeleton | `engine-core` |
| `engine-network-ip` | Direct IP transport adapter | Skeleton | `engine-core`, `engine-network-api` |
| `engine-steam` | Steam social/session and transport integration | Skeleton | `engine-core`, `engine-network-api` |
| `engine-editor` | Internal authoring/debug tooling | Skeleton | `engine-core`, `engine-assets`, `engine-world`, `engine-render-opengl` |
| `game-sandbox` | Game rules and vertical-slice content | Skeleton | `engine-core`, `engine-world`, `engine-physics-jolt`, `engine-network-api`, `engine-ui` |
| `game-client` | Client composition root | Minimal executable foundation entry point | `game-sandbox`, platform, render, audio, IP, Steam adapters |
| `game-server` | Headless/listen-server composition root | Minimal executable headless foundation entry point | `game-sandbox`, IP and Steam adapters |
| `test-support` | Shared JUnit 6/AssertJ support plus repository architecture verification | Implemented build/test support | None |
| `feasibility-spikes` | Disposable Phase 0 native/network feasibility executables | Experimental | None; external native/library dependencies only |

`feasibility-spikes` is not a seventeenth production engine module. It is deliberately outside the locked 16-module runtime target in `ENGINE_SCOPE.md`, and no production client/server module depends on it.

## Package/API boundary contract

`config/architecture/module-boundaries.properties` is the machine-readable registry for all 17 declared Gradle subprojects. Every declared subproject, including the experimental feasibility module, declares:

- one owned package root;
- one public cross-module API root under that package root;
- one internal implementation root under that package root.

Code inside a module may use its own implementation packages. Cross-module source references, whether imports or fully qualified names, must target the other module's declared API root. References to another module's internal root are architectural violations even when the Gradle project dependency itself is otherwise valid.

`ModulePackageBoundaryTest` lives under `test-support/src/test` because the root has no Java source tree. The `test-support` Gradle test task supplies the complete sorted `rootProject.subprojects` inventory; the test validates the registry against that live list rather than maintaining a second hard-coded list. It requires every scanned production Java file to declare a package under its owning module root, then uses the Java 25 compiler-tree API to inspect imports plus fully qualified references from both `src/main/java` and `src/test/java`. Overlapping roots are assigned to the most-specific owner before the source module is excluded.

Test-source package ownership is intentionally not enforced because the Phase 1 module smoke tests share the `com.samo.testing` package from `test-support`; their cross-module references are still checked. This remains source-level verification: it does not inspect compiled bytecode, reflective class names in strings/resources, or generated sources outside the conventional main/test Java trees. A deliberate fixture under `config/architecture/fixtures/` represents a forbidden `game-client -> engine-platform-lwjgl.internal` import and remains opt-in negative evidence; in-suite regressions cover a fully qualified shortcut, a missing production package, and overlapping network roots.

These package roots define boundaries, not future subsystem interfaces. P1-T10A does not create a reusable feasibility API or promote spike code into production architecture.

## Dependency rules

- Dependencies point from composition/game layers toward engine APIs and adapters, never from engine modules into game modules.
- `engine-core` is the lowest shared layer and must not depend on LWJGL, OpenGL, Jolt, OpenAL, Steam, or game code.
- `engine-ui` exposes renderer-neutral state/draw data; it must not expose OpenGL or imgui-java types.
- `engine-network-api` contains no socket/Steam implementation details.
- `game-server` must remain runnable without window, renderer, or audio modules.
- `engine-editor` may consume runtime modules, but runtime modules must not depend on the editor.
- Asset authoring/import dependencies belong in offline tooling; shipped gameplay consumes cooked formats.
- Cross-module Java imports target only the destination module's declared API package root; `.internal` packages are never public contracts.
- Production engine/game modules must not depend on `feasibility-spikes`; its external dependencies exist only to reproduce Phase 0 evidence.

## Runtime composition target

The client will compose platform/input, OpenGL rendering, runtime UI, audio, world/physics, game rules, and either IP or Steam networking. The server will compose world/physics, game rules, and networking without graphics/audio. Server authority owns gameplay state and dynamic physics; clients predict/present but do not submit authoritative transforms.

P1-T09 establishes only the runnable composition roots and their Gradle tasks. The current entry points intentionally initialize no production subsystems because those implementations begin in later phases. `game-server` additionally verifies that its runtime classpath contains no platform, renderer, audio, GLFW, OpenGL, or OpenAL dependencies.

## Single-subsystem lifecycle — P2-T01 / Issue #64

`com.samo.engine.core.api.EngineSubsystem` is an abstract `AutoCloseable` base class in `engine-core`. Final public `initialize`, `start`, `stop`, and `close` methods enforce ordering around protected `onInitialize`, `onStart`, `onStop`, and `onClose` hooks. No dependency or native library is added.

| Operation | Permitted entry state | Successful result |
| --- | --- | --- |
| `initialize()` | NEW | INITIALIZED; resources acquired |
| `start()` | INITIALIZED | STARTED; work active |
| `stop()` | STARTED | STOPPED; work quiescent, resources still owned |
| `close()` | NEW, INITIALIZED, STOPPED, FAILED | CLOSED; release hook attempted |
| repeated `close()` | CLOSED | No-op, including after a failed release hook |

Invalid calls fail before invoking a hook. Transient states reject reentrant lifecycle calls. The owner must externally serialize calls on its lifecycle thread; this is not a concurrent lifecycle manager.

An unchecked initialize/start/stop failure propagates unchanged and leaves FAILED, allowing only explicit close. The close hook must tolerate no setup, partial setup, or failed activation/stopping; it must quiesce remaining activity before releasing resources. A close attempt is terminal even if it throws, avoiding automatic repeated cleanup of potentially invalid native handles. CLOSED means the attempt ended, not that every resource was successfully released; cleanup failures must be reported by the owner.

Each instance has one lifetime, with no restart promise. This does not establish whether a native process-global subsystem supports reinitialization; P0-T14 remains the evidence gate. A successful running instance requires explicit stop before close. Client/server composition roots do not yet instantiate a subsystem.

## Subsystem dependency ordering — P2-T02 / Issue #72

`com.samo.engine.core.api.SubsystemGraph` snapshots a list of nested `Registration` records: an exact case-sensitive nonblank ID, an `EngineSubsystem` reference, and ordered prerequisite IDs. Lists are defensively copied. The constructor rejects duplicate IDs, repeated subsystem instances by identity, and missing dependencies after indexing the full list; forward references are valid. A registration rejects null/blank IDs and duplicate prerequisite IDs.

`initializationOrder()` returns an unmodifiable list of the original subsystem references in depth-first postorder, visiting roots in registration order and prerequisites in declaration order. Shared prerequisites appear once. An explicit heap traversal stack handles deep graphs without recursive call-stack growth. Each call uses fresh traversal state.

A cycle anywhere, including a self-cycle or later disconnected component, fails with `IllegalStateException` before any order is returned. Its message contains the closed dependent-to-dependency path, excluding any incoming noncyclic tail, for example `Subsystem dependency cycle: A -> B -> C -> A`. The caller can print/log that diagnostic; no logging framework is introduced.

The graph never calls lifecycle methods, checks subsystem state, owns resources, or performs cleanup. The caller must resolve the entire graph before invoking any hooks and retains explicit lifecycle ownership under D-018. Structural immutability does not make the referenced subsystems immutable. Synthetic composition tests exercise the real lifecycle guards but do not prove native safety or the ten-minute P2 phase gate.

## Coordinated startup rollback — P2-T03 / Issue #73

`com.samo.engine.core.api.SubsystemStartup` is a stateless utility above D-018 and D-019. It accepts an already-resolved dependency-first `List<EngineSubsystem>`, snapshots the complete list before any hook executes, then calls `initialize()` and `start()` on each subsystem before advancing to the next.

Successful startup does not transfer ownership or register a normal shutdown callback. The composition owner remains responsible for reverse-order `stop()` and `close()` during ordinary shutdown.

If initialize/start fails, the exact `RuntimeException` or `Error` remains primary. The failing subsystem receives one `close()` attempt. Every previously started subsystem is then visited in reverse order and receives `stop()` followed by `close()`; close is still attempted when stop fails because D-018 transitions a failed stop to FAILED, which permits explicit close. Rollback failures are appended to the original failure with `addSuppressed` in cleanup-attempt order, except the same throwable instance is not self-suppressed. Cleanup continues after rollback failures.

This is intentionally not a general lifecycle manager, dependency injection framework, restart mechanism, or native-lifecycle proof. It adds no state accessor and does not change `EngineSubsystem` or `SubsystemGraph`. Synthetic Java tests establish ordering and failure preservation only; P0-T14 and the ten-minute P2 phase exit remain separate evidence gates.

## Monotonic elapsed-time sampling — P2-T04 / Issue #74

`com.samo.engine.core.api.EngineClock` is the first production timing primitive in `engine-core`. It exposes a default constructor backed by `System.nanoTime()`, an injectable `LongSupplier` constructor for deterministic tests, and `sampleElapsedNanos()`.

Construction performs no source read. The first successful call establishes the baseline and returns `0`. Each later call reads the source once and computes `current - previousAccepted` with ordinary Java `long` arithmetic. Zero elapsed is valid. A negative signed difference fails with `IllegalStateException` and leaves the previous accepted reading intact, so a later valid sample is measured from the last accepted baseline. The source's absolute value may be negative because `System.nanoTime()` has an arbitrary origin.

Ordinary two's-complement subtraction is deliberate: a forward interval smaller than `2^63` nanoseconds remains a positive difference even when the raw source crosses `Long.MAX_VALUE` to `Long.MIN_VALUE`. Source `RuntimeException` or `Error` propagates unchanged before any baseline update. The clock does not expose floating-point seconds and does not impose synchronization; the runtime loop externally serializes calls.

`EngineClock` does not own the fixed-step accumulator, the locked 60 Hz target, frame-gap clamping, catch-up limits, render interpolation, sleeping/pacing, frame identity, wall-clock/calendar time, profiling, or subsystem lifecycle. Those remain separate Phase 2 tasks. P2-T04 tests prove only deterministic elapsed-time semantics; they do not satisfy the P2 ten-minute integrated fixed-tick exit gate.

## Fixed-step simulation accumulation — P2-T05 / Issue #75

`com.samo.engine.core.api.FixedStepAccumulator` converts non-negative elapsed nanoseconds into newly due whole simulation ticks at exactly 60 ticks per second. It keeps one exact fractional remainder expressed in integer tick-nanosecond units over a one-billion denominator. The implementation decomposes each elapsed duration into whole seconds plus a sub-second remainder, so every non-negative `long` input can be processed without multiplying an arbitrary elapsed value by 60 and overflowing.

Across accepted calls, cumulative due ticks equal `floor(totalAcceptedElapsedNanos * 60 / 1_000_000_000)`. Zero elapsed is valid and preserves the remainder. Negative elapsed throws `IllegalArgumentException` before mutation. The runtime loop externally serializes calls.

The accumulator owns neither a clock nor tick execution. A caller typically samples `EngineClock`, passes the elapsed result to `advance`, then executes its fixed simulation update the returned number of times. Tick numbering and cumulative simulation counters remain caller-owned.

P2-T05 established exact fractional ownership but intentionally left interpolation exposure to P2-T07. Frame-gap clamping, catch-up limits, pacing, callbacks, lifecycle integration, and configurable tick rates remain separate concerns.

## Bounded frame-gap and catch-up policy — P2-T06 / Issue #76

`com.samo.engine.core.api.FixedStepCatchUpPolicy` composes with a caller-owned `FixedStepAccumulator`. The default policy accepts at most 250,000,000 ns from one update and exposes at most 5 whole simulation steps from that update. An explicit constructor accepts different strictly positive limits without changing the semantics.

`advance(accumulator, elapsedNanos)` rejects a null accumulator and negative elapsed input before mutation. Otherwise it clamps the elapsed duration to the configured frame-gap limit, advances the supplied accumulator exactly once with that clamped value, and returns at most the configured step cap.

Two kinds of recovery time are deliberately discarded: elapsed nanoseconds beyond the frame-gap clamp never reach the accumulator, and whole due ticks above the step cap are not returned or carried as future backlog. Fractional sub-tick progress from the accepted elapsed duration remains preserved inside `FixedStepAccumulator`. This keeps D-022's exact rational 60 Hz arithmetic separate from the recovery policy while preventing a spiral-of-death catch-up queue.

The default 2-second-stall behavior is therefore bounded: 2,000,000,000 ns is clamped to 250 ms; the accumulator makes 15 ticks due at 60 Hz; the policy exposes exactly 5 and discards the other 10 whole ticks. A later ordinary frame starts without those 10 ticks queued.

The policy owns no clock, simulation callback, tick numbering, cumulative simulation counter, pacing, lifecycle, synchronization, or interpolation. P2-T08/P2-T09 own configuration, and the deterministic ten-minute headless-loop phase exit remains separate evidence.

## Render interpolation alpha — P2-T07 / Issue #77

`FixedStepAccumulator.interpolationAlpha()` exposes the accumulator's retained sub-tick progress as a read-only `double` in `[0.0, 1.0)`. The value is computed from the existing exact integer remainder as `scaledRemainder / 1_000_000_000.0`; querying it neither mutates nor consumes progress.

The simulation/render boundary remains explicit. `advance(long)` still returns only whole fixed 60 Hz ticks for simulation. A caller executes those whole simulation updates, then reads interpolation alpha separately for renderer presentation between its previous/current simulated states. P2-T07 does not create transform interpolation, a renderer dependency, callbacks, a variable simulation-delta API, or a runtime loop.

Floating point exists only at the presentation boundary. Fixed-step accumulation, whole-tick due calculation, frame-gap clamping, and catch-up policy remain integer/rational under D-022 and D-023. Fresh or exact-boundary state yields `0.0`; retained half/quarter fractions map directly to normalized alpha, and the exact remainder invariant keeps the value strictly below `1.0`.

After P2-T06 recovery, alpha reflects only the retained fraction from accepted/clamped elapsed time. Whole due ticks discarded by the step cap and elapsed time discarded by the frame-gap clamp are not represented as alpha and do not become backlog. Calls remain externally serialized; no thread-safety promise is added.

## Typed startup configuration validation — P2-T08 / Issue #78

`EngineConfigSchema` validates one already-resolved `Map<String, ConfigEntry>` before subsystem startup. It does not load files or merge sources. The initial canonical keys are `fullscreen.width`, `fullscreen.height`, and `simulation.tickRate`; each key is represented by a public `ConfigKey<Integer>` with a documented default.

Defaults are 1920x1080 and 60 Hz. Width accepts `320..16384`, height accepts `200..16384`, and the tick-rate key accepts exactly 60. The tick-rate key therefore validates the locked timing contract rather than making simulation cadence configurable.

`ConfigSource` carries opaque caller-owned diagnostic text, while `ConfigEntry` pairs that source with one raw value. Integer parsing trims leading/trailing whitespace. Missing known keys use defaults. Unknown keys, malformed integers, out-of-range dimensions, and non-60 tick rates are user configuration errors. `EngineConfigSchema.validate` examines the complete supplied map, preserves input iteration order for errors, and throws one `ConfigValidationException` with an immutable ordered `ConfigError` list if any user error exists. Null programmer-contract inputs fail before ordinary validation.

Successful validation returns an immutable map containing all three canonical keys and typed integer values. Validation invokes no subsystem lifecycle hook. P2-T08 intentionally has no filesystem, JSON/properties, environment, CLI, hot-reload, or mutable-settings service; P2-T09 adds only the fixed source layering described below.

## Layered startup configuration — P2-T09 / Issue #79

`EngineConfigLoader` composes one startup configuration using fixed precedence `EngineConfigSchema` defaults < game file < user file < command-line overrides. Engine defaults remain owned by D-025 and are not duplicated into a separate raw layer. The caller supplies both optional file paths plus an already-parsed command-line map; the loader does not discover OS paths or parse raw argv tokens.

Game and user files use a bounded UTF-8 line-oriented `key=value` format. Blank lines and comments whose first non-whitespace character is `#` are ignored. The first `=` separates a trimmed key from the raw value; values may contain later `=` characters. Blank keys, missing separators, and duplicate keys within one physical file fail with an `IllegalArgumentException` naming the normalized file path and 1-based line. Missing files are skipped, while existing unreadable paths propagate `IOException`.

Every file value carries a `ConfigSource` of `<normalized-path>:<line>`. Command-line values use `ConfigSource("command line")`. Higher layers replace lower entries by key. After all layers are merged, the loader calls `EngineConfigSchema.validate(...)` once; therefore an invalid lower value hidden by a valid higher value does not fail, while the winning invalid value retains its source in D-025 diagnostics. Successful output remains the immutable typed map owned by the schema.

The loader invokes no lifecycle method and adds no dependency, environment-variable layer, generic provider framework, Java `Properties` escaping semantics, persistence, hot reload, or mutable settings service.

## Experimental code boundary

Phase 0 code now lives under `feasibility-spikes/src/main/java/com/samo/spike/` and proves isolated capabilities:

- GLFW/OpenGL initialization;
- Jolt lifecycle;
- OpenAL lifecycle;
- localhost UDP and deterministic impairment;
- Steam initialization and FFM flat-API access;
- combined native smoke/soak executables.

The source was relocated without changing its experimental classification or promoting its behavior into reusable engine layers. Spike-only LWJGL/Jolt/Snaploader/OSHI/Steamworks dependencies are owned by `feasibility-spikes`, while root tasks with the historical names delegate to the matching subproject tasks so existing verification commands remain reproducible.

P0-T09A / Issue #42, P0-T13 / Issue #43, and P0-T14 / Issue #44 remain independent follow-up gates. Moving their supporting code does not satisfy those gates or strengthen prior feasibility claims.

## Architecture verification status

| Property | Status |
| --- | --- |
| 16 production-target modules declared | Implemented |
| Experimental `feasibility-spikes` subproject isolated | Implemented by P1-T10A / Issue #56 |
| Root contains no Java source/runtime spike dependencies | Implemented by P1-T10A / Issue #56 |
| Shared Java 25/test conventions | Implemented |
| Dependency locking/version catalog | Implemented |
| Automated package/module boundary test | Implemented by P1-T07; relocated by P1-T10A; hardened by Issue #62 |
| Client/server executable composition roots | Implemented by P1-T09 / Issue #39 |
| Single-subsystem lifecycle order | Implemented by P2-T01 / Issue #64 with JUnit 6 tests |
| Subsystem dependency ordering without lifecycle side effects | Implemented by P2-T02 / Issue #72 with JUnit 6 tests |
| Coordinated partial-startup rollback | Implemented by P2-T03 / Issue #73 with JUnit 6 tests |
| Monotonic elapsed-time sampling | Implemented by P2-T04 / Issue #74 with JUnit 6 tests |
| Exact 60 Hz fixed-step accumulation | Implemented by P2-T05 / Issue #75 with JUnit 6 tests |
| Bounded frame-gap/catch-up recovery policy | Implemented by P2-T06 / Issue #76 with JUnit 6 tests |
| Renderer-facing interpolation alpha | Implemented by P2-T07 / Issue #77 with JUnit 6 tests |
| Typed startup configuration validation | Implemented by P2-T08 / Issue #78 with JUnit 6 tests |
| Layered startup configuration precedence | Implemented by P2-T09 / Issue #79 with JUnit 6 tests |
| Concrete production engine subsystems | Planned: later Phase 2+ tasks |