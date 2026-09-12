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

The repository has a working Java 25 multi-project build with 17 declared Gradle subprojects: the 16 production-target engine/game/support modules defined by `ENGINE_SCOPE.md`, plus the experimental `feasibility-spikes` subproject. `engine-core` implements the shared lifecycle, dependency-ordering/startup-rollback, timing, configuration, native-resource diagnostics, structured logging, and fatal-termination foundation completed in Phase 2. `engine-platform-lwjgl` contains the P3-T01 production `GlfwWindow` lifecycle boundary, P3-T02's renderer-neutral logical-window/framebuffer-size delivery, P3-T03's in-place windowed/borderless/exclusive primary-monitor mode transitions, P3-T04's focus-loss-safe cursor capture plus bounded held-key/button safety state, P3-T05's raw/fallback relative mouse acquisition, and P3-T06's public immutable renderer-frame `InputSnapshot` boundary over the selected LWJGL 3.4.3 GLFW/OpenGL stack. P2-T11 and Issue #135 remain test/evidence-only paths. Other concrete engine subsystems remain skeletons. `game-sandbox` owns the P3-T04A owner-facing scripted engine demo and now observes the P3-T06 public input snapshot API while its future game-rules/vertical-slice responsibilities remain largely skeletal. `game-client` and `game-server` provide minimal executable composition roots for the foundation state. The root project is a build, quality, and task-aggregation project with no Java source tree and no spike runtime dependencies.

| Module | Intended responsibility | Current state | Direct project dependencies |
| --- | --- | --- | --- |
| `engine-core` | Lifecycle, time, IDs, events, math/spatial contracts | Lifecycle (P2-T01), dependency ordering (P2-T02), startup rollback (P2-T03), monotonic clock (P2-T04), fixed-step accumulator (P2-T05), bounded catch-up policy (P2-T06), interpolation alpha exposure (P2-T07), typed config validation (P2-T08), layered config loading (P2-T09), native-resource registry (P2-T10), structured logging boundary (P2-T12), orderly fatal termination (P2-T13); P2-T11 and #135 add test/evidence paths only; other responsibilities planned | None |
| `engine-platform-lwjgl` | GLFW/window/input and platform-native boundary | P3-T01 production `GlfwWindow` lifecycle; P3-T02 logical/framebuffer size separation and owner-thread polling; P3-T03 in-place primary-monitor windowed/borderless/exclusive transitions; P3-T04 focus-loss-safe cursor capture and held-input cleanup; P3-T05 raw/fallback relative mouse acquisition; P3-T06 immutable renderer-frame `InputSnapshot` plus engine-defined key/button vocabulary; later action/controller/command responsibilities planned | `engine-core` |
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
| `game-sandbox` | Game rules/vertical-slice content plus owner-facing manual engine demo | P3-T04A scripted demo implemented and P3-T06 public input diagnostics added; future game rules/content remain skeletal | Published/runtime graph remains `engine-core`, `engine-world`, `engine-physics-jolt`, `engine-network-api`, `engine-ui`; demo additionally uses a non-consumable `engine-platform-lwjgl` configuration |
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
- `game-sandbox` may compile/run owner-facing demos against public engine APIs through dedicated non-consumable demo configurations, but those dependencies must not be published through the sandbox runtime consumed by `game-server`.
- `engine-editor` may consume runtime modules, but runtime modules must not depend on the editor.
- Asset authoring/import dependencies belong in offline tooling; shipped gameplay consumes cooked formats.
- Cross-module Java imports target only the destination module's declared API package root; `.internal` packages are never public contracts.
- Production engine/game modules must not depend on `feasibility-spikes`; its external dependencies exist only to reproduce Phase 0 evidence.

## Runtime composition target

The client will compose platform/input, OpenGL rendering, runtime UI, audio, world/physics, game rules, and either IP or Steam networking. The server will compose world/physics, game rules, and networking without graphics/audio. Server authority owns gameplay state and dynamic physics; clients predict/present but do not submit authoritative transforms.

P1-T09 establishes only the runnable composition roots and their Gradle tasks. The current client entry point intentionally does not yet instantiate `GlfwWindow`; P3-T01 defines the reusable production platform ownership boundary first, P3-T02 adds bounded size-event/polling, P3-T03 adds display-mode transitions, P3-T04 adds focus-loss/cursor-capture safety, P3-T05 adds relative mouse acquisition, and P3-T06 adds the public renderer-frame hardware snapshot boundary. The snapshot remains client/platform state: P3-T09 retains the tick-aligned device-neutral `PlayerInputCommand` boundary used by headless/replay flows. P3-T04A composes public platform capabilities only in the owner-facing `game-sandbox` demo. Its platform classpath is deliberately non-consumable/non-exported so `game-server` remains headless. Later composition work decides when the real client owns the platform subsystem. `game-server` additionally verifies that its runtime classpath contains no platform, renderer, audio, GLFW, OpenGL, or OpenAL dependencies.

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

The graph never calls lifecycle methods, checks subsystem state, owns resources, or performs cleanup. The caller must resolve the entire graph before invoking any hooks and retains explicit lifecycle ownership under D-018. Structural immutability does not make the referenced subsystems immutable. Synthetic composition tests exercise the real lifecycle guards but do not by themselves satisfy D-030's 60-second integrated Phase 2 gate or prove native safety.

## Coordinated startup rollback — P2-T03 / Issue #73

`com.samo.engine.core.api.SubsystemStartup` is a stateless utility above D-018 and D-019. It accepts an already-resolved dependency-first `List<EngineSubsystem>`, snapshots the complete list before any hook executes, then calls `initialize()` and `start()` on each subsystem before advancing to the next.

Successful startup does not transfer ownership or register a normal shutdown callback. The composition owner remains responsible for reverse-order `stop()` and `close()` during ordinary shutdown.

If initialize/start fails, the exact `RuntimeException` or `Error` remains primary. The failing subsystem receives one `close()` attempt. Every previously started subsystem is then visited in reverse order and receives `stop()` followed by `close()`; close is still attempted when stop fails because D-018 transitions a failed stop to FAILED, which permits explicit close. Rollback failures are appended to the original failure with `addSuppressed` in cleanup-attempt order, except the same throwable instance is not self-suppressed. Cleanup continues after rollback failures.

This is intentionally not a general lifecycle manager, dependency injection framework, restart mechanism, or native-lifecycle proof. It adds no state accessor and does not change `EngineSubsystem` or `SubsystemGraph`. Synthetic Java tests establish ordering and failure preservation only; P0-T14 remains an independent native lifecycle gate, and D-030's 60-second integrated Phase 2 gate is separate evidence.

## Monotonic elapsed-time sampling — P2-T04 / Issue #74

`com.samo.engine.core.api.EngineClock` is the first production timing primitive in `engine-core`. It exposes a default constructor backed by `System.nanoTime()`, an injectable `LongSupplier` constructor for deterministic tests, and `sampleElapsedNanos()`.

Construction performs no source read. The first successful call establishes the baseline and returns `0`. Each later call reads the source once and computes `current - previousAccepted` with ordinary Java `long` arithmetic. Zero elapsed is valid. A negative signed difference fails with `IllegalStateException` and leaves the previous accepted reading intact, so a later valid sample is measured from the last accepted baseline. The source's absolute value may be negative because `System.nanoTime()` has an arbitrary origin.

Ordinary two's-complement subtraction is deliberate: a forward interval smaller than `2^63` nanoseconds remains a positive difference even when the raw source crosses `Long.MAX_VALUE` to `Long.MIN_VALUE`. Source `RuntimeException` or `Error` propagates unchanged before any baseline update. The clock does not expose floating-point seconds and does not impose synchronization; the runtime loop externally serializes calls.

`EngineClock` does not own the fixed-step accumulator, the locked 60 Hz target, frame-gap clamping, catch-up limits, render interpolation, sleeping/pacing, frame identity, wall-clock/calendar time, profiling, or subsystem lifecycle. Those remain separate Phase 2 contracts. P2-T04 tests prove deterministic elapsed-time semantics only; D-030/#135 owns the 60-second integrated fixed-tick evidence.

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

The policy owns no clock, simulation callback, tick numbering, cumulative simulation counter, pacing, lifecycle, synchronization, or interpolation. P2-T08/P2-T09 own configuration. D-030/#135 integrates this policy with the real clock/accumulator/lifecycle path for the 60-second Phase 2 exit evidence.

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

## Explicit native-resource ownership diagnostics — P2-T10 / Issue #80

`NativeResourceRegistry` gives later native wrappers one engine-core ownership diagnostic without depending on any native library. A successful registration normalizes a nonblank resource type, requires an opaque nonzero `long` handle, captures the first allocation call-site frame outside the registry, stores the caller-supplied `Runnable` closer, and returns a nested `Registration` implementing `AutoCloseable`.

A live identity is `(resourceType, handle)`. Duplicate live identities fail before ownership changes. Different resource types may reuse the same numeric handle because native APIs have separate namespaces, and an identity may be registered again after a successful release.

`Registration.close()` runs the closer synchronously on the caller thread. Successful close removes that exact registration and repeated close is a no-op. If the closer throws a `RuntimeException` or `Error`, the original throwable propagates unchanged, the attempt is terminal, and the registration remains tracked as `CLOSE_FAILED`; automatic retry is intentionally forbidden to avoid a possible double-free. Reentrant close while `CLOSING` is rejected.

`assertNoOpenResources()` is a non-cleaning debug-shutdown verifier. Empty registries pass. Otherwise it throws `IllegalStateException` with the tracked count and deterministic registration-order entries containing normalized type, decimal handle, state, and allocation site. Verification never invokes a closer, removes an entry, or changes ownership state. Build-mode detection and composition-root integration are outside P2-T10; a later debug shutdown path decides when to call the verifier.

The registry is not thread-safe. Registration, close, and verification remain externally serialized, preserving caller/native thread-affinity. Synthetic Java tests establish bookkeeping and diagnostics only; they are not evidence that GLFW/OpenGL/Jolt/OpenAL/Steam resources are leak-free, sustained-stable, or restartable. P0-T13/P0-T14 remain separate native evidence gates; D-030/#135 owns the 60-second Java headless integration-cleanup gate.

## Allocation observability evidence — P2-T11 / Issue #81

P2-T11 deliberately adds no runtime allocation-metric service or public engine API. `AllocationMetricBenchmarkTest` lives in `engine-core` test source and uses the Java 25 JFR `jdk.ObjectAllocationSample` event selected by the existing profiling baseline.

The benchmark measures controlled simulation-tick and synthetic/headless render-frame workloads in separate recording windows after warm-up. Each window runs on a uniquely named dedicated platform thread, and only samples attributed to that thread contribute. Positive JFR `weight` values are summed and divided by measured iteration count to report estimated bytes per iteration. The report also records sample count and measurement duration.

An allocating control must produce usable sampled evidence; otherwise the benchmark fails rather than treating absent samples as zero. A nonallocating arithmetic control is also measured, but a zero sampled result is only an observation and not proof of mathematical zero allocation.

This evidence is sampled Java-heap allocation pressure, not an exact per-call counter. It excludes direct/native allocations, GPU/driver memory, retained-heap size, and GC pause cost. The render channel is not evidence from the future OpenGL renderer. No product budget or threshold is established by P2-T11, and no durable architecture decision is added.

## Structured runtime logging boundary — P2-T12 / Issue #82

`EngineLogger` is the shared `engine-core` structured logging seam selected by D-028. It owns no persistence format or logging backend. A caller supplies one `Sink`, and each valid `log(...)` call synchronously creates an immutable `Event` containing `Instant.now()`, severity, message, the actual calling thread ID/name, and one immutable `Context` snapshot before invoking `Sink.write`.

`Context` carries nullable frame, simulation tick, subsystem, connection, and entity fields. Present frame/tick values must be nonnegative. Present string values are `String.strip()` normalized and must remain nonblank; `null` is the only missing-field representation. Connection/entity/subsystem remain opaque strings until later networking/world tasks define stronger domain ID types.

Every severity from `DEBUG` through `FATAL` is forwarded; the logger performs no threshold routing. `FATAL` is only a label and does not terminate the process by itself. `flush()` explicitly delegates to the sink. One private synchronization boundary serializes `write` and `flush` callbacks across concurrent callers while event thread fields still identify the original caller. Sink unchecked failures propagate unchanged and are neither retried nor swallowed.

The sink is caller-owned and is not closed by `EngineLogger`. P2-T12 adds no background worker, queue, buffering, retry/drop policy, shutdown hook, global singleton, file/console/JSON format, rotation policy, or composition-root wiring. P2-T13 composes this boundary without changing those ownership or format decisions.

## Orderly fatal termination — P2-T13 / Issue #83

`FatalTermination` is the D-029 one-shot fatal-shutdown coordinator in `engine-core`. The public constructor receives the existing `EngineLogger`; production termination uses `System.exit(1)`. A package-private `IntConsumer` constructor exists only as a unit-test seam so tests can inspect ordering without terminating the test JVM.

A valid `terminate(message, context, initializationOrder, resourceRegistry)` call validates and snapshots all caller inputs before any side effect, including identity-based duplicate subsystem rejection. The supplied order is the dependency-first order that completed startup successfully. After atomically claiming the coordinator, fatal shutdown runs synchronously on the calling lifecycle/native-affinity thread and does not hold an internal lock across user callbacks.

The exact orchestration is: emit one structured `FATAL` event; visit the subsystem snapshot in strict reverse order and attempt `stop()` then `close()` for each; invoke the existing non-cleaning `NativeResourceRegistry.assertNoOpenResources()` exactly once; emit best-effort structured `ERROR` events for failures captured before the reporting pass; flush the logger once; then invoke termination status `1`. If stop fails, D-018 leaves the subsystem FAILED so close remains legal and is still attempted. Registry verification never force-closes leaked entries.

Unchecked failures from the initial fatal log, stop/close hooks, registry verification, failure-report logs, and flush are accumulated in encounter order instead of aborting later cleanup. Failure-report logging failures are captured but not recursively re-logged. If the test terminator throws, its exact throwable remains primary with prior failures suppressed; if it returns, the coordinator throws terminal `IllegalStateException` because continuing after an expected process exit is invalid.

Each `FatalTermination` instance is one-shot. Reentrant, concurrent, and later calls are rejected before they can duplicate logging, cleanup, verification, flushing, or termination. The coordinator creates no worker, executor, JVM shutdown hook, global singleton, persisted log format, force-close API, module edge, or composition-root wiring.

JUnit tests use handwritten traces/counters plus a real child JVM. The child uses the public constructor and proves exit status `1` occurs only after a synthetic subsystem stops, closes its registered resource, and flushes its sink. This is Java orchestration evidence only; it does not establish native GLFW/OpenGL/Jolt/OpenAL/Steam cleanup, sustained stability, or restartability. D-030/#135 separately owns the 60-second integrated Phase 2 gate.

## Phase 2 integrated exit evidence — Issue #135 / D-030

`Phase2IntegratedGateTest` is a test-only integration harness in `engine-core`; it adds no production API, dependency, module edge, lifecycle manager, or force-close behavior. When explicitly enabled, it starts a synthetic `EngineSubsystem` through `SubsystemStartup`, registers one synthetic owned handle through `NativeResourceRegistry`, and then runs a headless loop with the production `EngineClock`, `FixedStepAccumulator`, and default `FixedStepCatchUpPolicy`.

The gate runs for at least 60 continuous seconds. Normal iterations sample monotonic elapsed time and execute only the whole simulation steps returned by the exact 60 Hz accumulator/policy combination. Once after startup it injects a real two-second delay; the following clock sample must expose no more than the default five catch-up steps, demonstrating the 250 ms input clamp and five-step cap through the integrated path rather than an isolated policy test.

After the duration completes, the harness stops and closes the subsystem, the owner close releases the tracked registration exactly once, and `NativeResourceRegistry.assertNoOpenResources()` must pass. The retained report at `engine-core/build/reports/phase2/p2-exit-60-second-gate.txt` records observed duration, executed fixed ticks, loop update count, stall/catch-up observations, lifecycle trace, cleanup result, commit/environment, and evidence limits.

The test is opt-in for ordinary Gradle test runs and is explicitly enabled once by the CI evidence step so routine aggregate/coverage tasks do not duplicate the 60-second delay. A passing gate proves Java headless integration correctness for the Phase 2 contracts only. It does not exercise actual GLFW/OpenGL/Jolt/OpenAL/Steam ownership, does not claim sustained native stability or restartability, and does not replace P0-T13 or P0-T14.

## Production GLFW/OpenGL window lifecycle — P3-T01 / Issue #84

`com.samo.engine.platform.api.GlfwWindow` is the first concrete production platform subsystem and the D-031 ownership boundary. Construction validates positive dimensions, a nonblank preserved title, an `EngineLogger`, and a caller-owned `NativeResourceRegistry` without making a native call. The original P3-T01 public surface adds no raw GLFW handle, LWJGL capability type, event API, swap/poll API, size API, fullscreen API, or input API.

`initialize()` captures the lifecycle/native-affinity thread, installs one task-owned GLFW error callback while retaining any previous callback for later restoration, initializes GLFW, resets hints, and requests an OpenGL 4.6 Core forward-compatible context in a hidden resizable window. The nonzero GLFW window handle is immediately registered in the caller-owned D-027 registry, and that registration owns `glfwDestroyWindow`. Partial initialization rolls back all state acquired by that attempt while preserving the original unchecked failure and suppressing cleanup failures in attempt order.

`start()` requires the same owner thread, makes the context current, creates LWJGL capabilities, requires actual `OpenGL46` support, queries nonblank `GL_VERSION` and `GL_RENDERER`, emits exactly two D-028 INFO events with `subsystem=platform`, then shows the window. A start failure detaches the current context and clears the thread's LWJGL capabilities while retaining the window/GLFW ownership for D-018 close.

`stop()` hides the window, detaches its current context, and clears thread-local capabilities, attempting all cleanup steps even when an earlier step fails. `close()` is D-018 terminal cleanup: it handles NEW, failed initialization/start, or STOPPED state, releases any remaining context state, closes the native-registry registration exactly once, terminates the owned GLFW session, restores the previous GLFW error callback without freeing it, and frees only the callback created by this instance. Native-bearing lifecycle hooks remain on the initialize thread; no dispatcher, worker, shutdown hook, global window manager, multi-window/shared-context contract, or restartability claim is introduced.

The platform module places the selected LWJGL 3.4.3 core/GLFW/OpenGL libraries plus Windows natives on its production classpath. Its repository project dependency remains only `engine-core`; `engine-core` itself stays native-library free. Deterministic tests use a package-private backend seam to verify ordering/failure cleanup without a display, while an opt-in Windows x64 native test uses the public constructor, independently observes OpenGL >=4.6 and version/renderer strings, proves log equality, and verifies no current context plus an empty native-resource registry after close. This one lifecycle run is production integration evidence, not P0-T13 soak or P0-T14 repeated-lifecycle evidence.

P3-T01 intentionally did not install the OpenGL debug callback (P5-T01), run a frame/render loop, swap buffers/poll events, expose framebuffer/logical sizes, change fullscreen mode, capture input, or implement the Phase 3 replay exit gate. P3-T02 adds only the bounded polling/size behavior described next and does not retroactively broaden P3-T01's original acceptance.

## Logical window and framebuffer sizing — P3-T02 / Issue #85

D-032 extends `GlfwWindow` without exposing GLFW/LWJGL types. `WindowSizeListener` has two separate callbacks: `onLogicalWindowSizeChanged(width, height)` for GLFW logical/screen-coordinate dimensions and `onFramebufferSizeChanged(width, height)` for framebuffer pixel dimensions. The existing five-argument constructor remains source-compatible and uses an internal no-op listener; a six-argument overload accepts the explicit listener.

After the P3-T01 context/version/logging checks succeed, `start()` installs one owned GLFW window-size callback and one owned framebuffer-size callback, queries the actual initial logical and framebuffer sizes independently, stages both initial values, enables event polling, and then shows the window. Native callbacks never invoke consumer code. They stage only the latest pair for their own channel, so multiple notifications in one native poll may coalesce without mixing logical units and framebuffer pixels.

`GlfwWindow.pollEvents()` is a public bounded platform operation, not a renderer loop. It is legal only while the window is STARTED, requires the D-031 owner thread, calls GLFW event polling exactly once, then delivers the latest pending logical value followed by the latest pending framebuffer value. Listener `RuntimeException`/`Error` failures remain caller-visible. The implementation creates no asynchronous dispatcher, background worker, concurrent queue, or thread-safety promise.

Zero dimensions are valid platform states, especially framebuffer `0x0` while minimized, and are delivered normally. Negative dimensions from the platform boundary are treated as `IllegalStateException` contract violations before reaching the listener. Initial-size query failures or callback-installation failures fail `start()` and release any size callback/context state acquired by that attempt. `stop()` disables polling, clears undelivered staged sizes, releases both owned size callbacks, then continues hide/context/capability cleanup even if an earlier cleanup operation fails. Terminal `close()` also attempts any remaining callback cleanup before the native window is destroyed.

The size path is renderer-neutral. Renderer-facing code may consume framebuffer pixels later, while logical dimensions remain available for window/layout semantics, but P3-T02 introduces no renderer dependency, viewport mutation, buffer swapping, fullscreen mode, focus/input state, content-scale callback API, raw handle, multi-window management, or native restartability claim. Deterministic tests deliberately use unequal logical/framebuffer pairs; the Windows native acceptance compares production listener delivery against direct GLFW logical/framebuffer queries and records content scale without requiring the machine to have non-100% DPI scaling.

## Windowed and fullscreen mode transitions — P3-T03 / Issue #86

D-033 extends the same D-031/D-032 `GlfwWindow` boundary with public `WindowMode` and owner-thread `setWindowMode(WindowMode)`. Calls are legal only while STARTED; null is rejected before native work and requesting the already-active mode is a no-op. Successful transitions mutate the existing GLFW window in place and do not recreate its OpenGL context.

When leaving `WINDOWED`, the platform captures the current GLFW window position plus positive logical width/height as the restore geometry. `BORDERLESS_FULLSCREEN` disables decoration and uses a monitor-detached window at the primary monitor origin/current video-mode dimensions with no forced refresh. `EXCLUSIVE_FULLSCREEN` attaches the same window to the primary monitor through `glfwSetWindowMonitor` using that monitor's current video-mode dimensions and refresh. Returning to `WINDOWED` re-enables decoration, detaches from a monitor, restores the captured geometry, then clears it so a later fullscreen entry captures the then-current windowed state. Direct borderless/exclusive transitions preserve the same original restore geometry.

The primary monitor is deliberately the only production target in this task. Missing monitor/video-mode data or non-positive current-mode dimensions/refresh fail before committing the corresponding transition. Desktop X/Y coordinates may be negative and are preserved. No raw monitor/window handle, monitor-selection API, custom resolution/refresh selector, renderer presentation API, multi-window policy, focus/input behavior, or later Phase 3 API is exposed.

If a backend transition throws `RuntimeException` or `Error`, the original throwable remains primary and Java-visible mode state remains the previous mode. The platform makes one best-effort rollback attempt to the previous mode/geometry and suppresses any distinct rollback failure on the original throwable. P3-T02 size callbacks remain the only public logical/framebuffer notification path for dimensions caused by mode changes.

Deterministic tests use the existing package-private backend seam to prove capture/restore, direct fullscreen-mode transitions, fresh recapture after returning windowed, invalid monitor state, owner-thread/lifecycle rejection, same-mode no-op, and failure/rollback semantics without a display. The opt-in Windows x64 `GlfwWindowModeNativeTest` runs the production API through exactly 20 successful mode changes (five `BORDERLESS_FULLSCREEN -> WINDOWED -> EXCLUSIVE_FULLSCREEN -> WINDOWED` cycles), verifies after every step that the original context remains current and OpenGL remains usable, checks native monitor/window state and geometry, and requires final cleanup with an empty `NativeResourceRegistry`. This bounded run is not P0-T13 soak or P0-T14 repeated-lifecycle evidence.

## Focus-loss input safety and cursor capture — P3-T04 / Issue #87

D-034 adds the public owner-thread `GlfwWindow.setCursorCaptured(boolean)` operation and keeps the pre-snapshot hardware safety state inside `engine-platform-lwjgl`. While STARTED, a focused window may request GLFW disabled-cursor capture; explicit release restores the normal cursor. Focus, key, and mouse-button callbacks are installed and owned by the same window lifecycle and are released during stop/start-failure cleanup.

The platform tracks bounded internal held-state arrays for GLFW key and mouse-button indices. Key PRESS/REPEAT and mouse PRESS mark held; RELEASE clears held; invalid indices are ignored. On focus loss the platform marks the window unfocused, clears all held keys/buttons before any cursor operation, and if capture is effectively active requests `GLFW_CURSOR_NORMAL`. A previous capture request is remembered only to require explicit policy re-arm; focus regain never automatically recaptures. A later explicit `setCursorCaptured(true)` is required before gameplay pointer lock resumes.

Native focus callbacks never invoke game code. If cursor release throws while handling focus loss, held state is already cleared, effective capture is treated conservatively as off, and the original unchecked failure is staged instead of crossing the native callback boundary. The owning `pollEvents()` propagates that staged failure once after GLFW returns. Direct public capture calls still propagate backend unchecked failures unchanged and update requested/effective Java state only after successful native mutation.

P3-T04 itself intentionally added no raw mouse motion, public focus listener, public snapshot, action transitions, controller mapping, player commands, renderer behavior, or game pause/menu policy. Later P3 tasks extend this internal hardware state without changing the historical P3-T04 acceptance.

## Relative mouse-motion acquisition — P3-T05 / Issue #88

D-035 extends the existing D-031 through D-034 `GlfwWindow` ownership boundary with relative mouse-motion acquisition while effective cursor capture is active. The window owns one cursor-position callback alongside the focus/key/button callbacks. Cursor-position samples are considered only while the window is focused and cursor capture is effectively active.

On explicit successful capture, `GlfwWindow` first requests `GLFW_CURSOR_DISABLED`. If GLFW reports raw mouse motion support, the same window then enables `GLFW_RAW_MOUSE_MOTION`; otherwise raw mode remains off and successive disabled-cursor positions form the fallback relative stream. The fallback is screen-bound independent under GLFW disabled-cursor semantics, but it does not claim to bypass OS pointer acceleration. Raw mode is disabled whenever effective capture ends.

Relative accumulation uses a baseline to suppress discontinuity spikes. The first eligible position sample after start, capture/re-capture, focus/capture transition, or baseline invalidation establishes the baseline and contributes zero. Later eligible samples contribute signed `current - previous` X/Y deltas. Release, focus loss, stop/close, and startup cleanup clear pending motion and invalidate the baseline. Focus regain alone never re-enables capture or raw mode; D-034 still requires explicit recapture.

Native callbacks do not invoke gameplay code. Raw-enable failure during a direct capture attempt preserves the original unchecked throwable and performs best-effort raw/cursor rollback without committing requested/effective capture state. Focus-loss cleanup clears held/motion state before attempting native raw/cursor release; native cleanup failures are staged and propagate once from owner-thread `pollEvents()` using the existing D-034 callback-failure boundary.

Deterministic `GlfwWindowMouseMotionTest` proves ignored pre-capture/unfocused events, zero first sample, signed accumulation, absolute-position independence, raw-supported and forced-unsupported fallback selection, release/focus baseline reset, explicit recapture, failure rollback/staging, callback ownership, and lifecycle cleanup. The opt-in Windows x64 `GlfwWindowMouseMotionNativeTest` verifies real GLFW raw-mode state before/during/after capture, release, focus transfer, regain, and explicit recapture plus registry cleanup. It intentionally does not treat programmatic `glfwSetCursorPos` as a physical raw-device oracle; deterministic tests own exact delta arithmetic while the native test proves real mode/focus/lifecycle integration.

## Renderer-frame hardware input snapshot — P3-T06 / Issue #89

D-036 adds public `InputSnapshot`, `InputKey`, and `InputMouseButton` under `com.samo.engine.platform.api` plus owner-thread `GlfwWindow.captureInputSnapshot(long frameId)`. The snapshot is a client/platform hardware view; no new module dependency is introduced and `game-server` remains independent of `engine-platform-lwjgl`.

Snapshot capture is legal only while `GlfwWindow` is STARTED, requires the D-031 owner thread, rejects a negative caller-owned frame ID, and performs no native poll. Callers choose the renderer-frame sampling point by invoking `pollEvents()` and then `captureInputSnapshot(frameId)`. The returned object defensively copies its state and remains immutable after later polls, focus changes, or captures.

The public key/button vocabulary is engine-defined rather than raw GLFW integer codes. The initial keyboard vocabulary covers W/A/S/D, Space, left/right Shift/Control/Alt, Escape, E/Q/R/F; the mouse vocabulary covers left/right/middle plus buttons 4/5. Native codes outside that vocabulary remain internal and do not leak into the public API.

`GlfwWindow` now retains raw hardware edge booleans between successful snapshot captures. PRESS marks held plus pending pressed; RELEASE clears held plus pending released; key REPEAT preserves held state without generating another pressed edge. A complete press/release between two snapshots can therefore appear as pressed=true, released=true, held=false in the next snapshot. Successful capture consumes pending edge bits but not held levels.

D-035 mouse accumulation is exposed in the snapshot and consumed on successful capture. Snapshot capture clears only the accumulated delta, not the motion baseline, so ordinary frame boundaries preserve continuous relative motion. Focus loss converts currently held supported keys/buttons into release edges, discards stale pending presses, clears pending mouse motion, and preserves D-034's explicit-recapture behavior. Focus regain synthesizes no state.

Validation failures happen before consumption. Snapshot capture is pure Java and adds no native cleanup/rollback path. `InputSnapshot` is intentionally not the headless/tick/replay format: P3-T07/P3-T08 own action semantics, while P3-T09 owns `PlayerInputCommand` and replay/network-friendly device-neutral input. This separation prevents a platform dependency from entering server composition.

Deterministic `InputSnapshotTest` and `GlfwWindowInputSnapshotTest` cover defensive immutability, edge retention/consumption, key-repeat semantics, mouse-delta one-shot consumption without baseline reset, focus-loss releases, validation-before-consumption, stable shared snapshots, and proof that snapshot capture does not poll GLFW. Existing P3-T04/P3-T05 native acceptance continues to verify the real callback/focus/raw ingestion path because P3-T06 itself makes no new native call.

## Owner-facing engine sandbox demo — P3-T04A / Issue #149

`game-sandbox` is the canonical owner-facing manual demo surface. `EngineDemoMain` composes only public production APIs: `GlfwWindow`, logical/framebuffer size delivery, window-mode transitions, cursor capture/focus-loss behavior, P3-T06 `InputSnapshot`/`InputKey`, `EngineClock`, `FixedStepAccumulator`, `FixedStepCatchUpPolicy`, structured logging, and `NativeResourceRegistry` verification. The scripted timeline lets the owner observe current engine behavior before renderer/gameplay layers exist.

The sandbox must not become a second engine architecture. It may not import another module's internal package, call LWJGL/native APIs directly, or expose a new production API merely to make a demo easier. When a future task adds a human-observable capability that can be demonstrated through its already-authorized public API, the same PR updates the sandbox. When that is not possible without pulling later roadmap work forward, the PR records `Sandbox impact: none — <reason>`.

The platform dependency used by the demo is intentionally non-exported. `game-sandbox` compiles the demo against the public `engine-platform-lwjgl` API and resolves a dedicated non-consumable demo runtime for `runEngineDemo`; that platform dependency is not part of the sandbox runtime elements consumed by `game-server`. The existing headless-server runtime gate remains the proof that this owner-facing demo did not contaminate server composition.

P3-T06 captures one public `InputSnapshot` per demo frame after event polling and prints bounded once-per-second input diagnostics. This observation path does not create a game/action layer and does not change the sandbox's non-authoritative status.

The sandbox is not verification authority. Its timing/input lines are diagnostic only and are explicitly not FPS or benchmark evidence. The current window remains visually empty until renderer work creates a public production presentation path. Unit/native/integration tests, exact final-candidate heavy CI, exact-merge lightweight master verification, and P0 feasibility gates remain separate acceptance evidence.

## Experimental code boundary

Phase 0 code now lives under `feasibility-spikes/src/main/java/com/samo/spike/` and proves isolated capabilities:

- GLFW/OpenGL initialization;
- Jolt lifecycle;
- OpenAL lifecycle;
- localhost UDP and deterministic impairment;
- Steam initialization and FFM flat-API access;
- combined native smoke/soak executables.

The source was relocated without changing its experimental classification or promoting its behavior into reusable engine layers. Spike-only LWJGL/Jolt/Snaploader/OSHI/Steamworks dependencies are owned by `feasibility-spikes`, while root tasks with the historical names delegate to the matching subproject tasks so existing verification commands remain reproducible. P3-T01 separately places only the scope-approved LWJGL core/GLFW/OpenGL dependencies needed by the production platform adapter; P3-T02 through P3-T06 add no dependency. P3-T04A consumes that existing platform stack only through the sandbox's non-consumable demo configuration. None of these tasks promotes the spike executable or its debug/render-loop behavior.

P0-T09A / Issue #42, P0-T13 / Issue #43, and P0-T14 / Issue #44 remain independent follow-up gates. Moving or reusing proven dependency choices does not satisfy those gates or strengthen prior feasibility claims.

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
| Explicit native-resource ownership diagnostics | Implemented by P2-T10 / Issue #80 with JUnit 6 tests |
| Sampled Java-heap allocation observability evidence | P2-T11 test/evidence path; no production API |
| Synchronous structured logging boundary | Implemented by P2-T12 / Issue #82 with JUnit 6 tests |
| One-shot orderly fatal termination | Implemented by P2-T13 / Issue #83 with JUnit 6 + child-JVM tests |
| 60-second integrated Phase 2 headless gate | Completed under Issue #135 / D-030; retained exact-head PR and merged-`master` CI evidence passed |
| Production GLFW/OpenGL window lifecycle | P3-T01 / Issue #84: `GlfwWindow`, deterministic tests, and real Windows native acceptance |
| Logical/framebuffer size separation | P3-T02 / Issue #85: `WindowSizeListener`, owner-thread polling, deterministic tests, and Windows native acceptance path |
| Windowed/borderless/exclusive transitions | P3-T03 / Issue #86: `WindowMode`, in-place owner-thread transitions, deterministic tests, and 20-transition Windows native acceptance path |
| Focus-loss cursor/input safety | P3-T04 / Issue #87: `setCursorCaptured`, owned focus/key/button callbacks, deterministic tests, and real Windows focus-transfer acceptance path |
| Raw/fallback relative mouse acquisition | P3-T05 / Issue #88: internal `GlfwWindow` cursor-position accumulation, raw-mode selection, deterministic fallback/failure tests, and Windows native mode/focus acceptance path |
| Immutable renderer-frame hardware snapshot | P3-T06 / Issue #89: `InputSnapshot`, engine key/button vocabulary, retained hardware edges, relative-delta consumption, and deterministic frame-snapshot tests |
| Owner-facing sandbox demo | P3-T04A / Issue #149 plus P3-T06 diagnostics: public-API demo with non-exported platform runtime; headless-server boundary remains independently verified |
| Other concrete production engine subsystems | Planned: later phases |

## Wiki synchronization

This file remains the architecture authority for module roles, boundaries, and accepted implementation maturity. The [`../wiki/`](../wiki/README.md) directory is a lower-authority consumer guide. Whenever an architecture task adds/removes/renames a public API or changes lifecycle, ownership, thread-affinity, failure, configuration, or other caller-visible semantics, update the relevant wiki pages and examples in the same PR. When no consumer behavior changes, record `Wiki impact: none — <reason>` rather than editing the wiki unnecessarily.

`game-sandbox` is also lower-authority than production code/tests and architecture. Its purpose is owner observation, not specification. Future tasks must keep it synchronized when appropriate under the `AGENTS.md` sandbox rule without using it to justify a production API or architecture change.