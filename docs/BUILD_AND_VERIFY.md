# Sherko Engine Build and Verification

This file centralizes repeatable commands and the evidence expected from them. Run Windows commands on Windows x64 when native libraries are involved. CI is authoritative only when the configured repository self-hosted Windows x64 runner actually executes the jobs. The current CI lifecycle is defined by `AGENTS.md`, `docs/CI_LIFECYCLE.md`, and the current `## CI gate` section below; older task-specific sections retain historical evidence language where useful.

## Environment baseline

- JDK: Temurin/OpenJDK 25 through the Gradle toolchain.
- Build: repository Gradle Wrapper.
- CI runner: repository-scoped self-hosted Windows x64 runner selected by `[self-hosted, Windows, X64]`.
- Runner availability: the runner is manually operated and must be online before required CI can execute; queued/unstarted jobs are not verification evidence.
- Runtime target: Windows x64.

Use `./gradlew` on Unix-like shells for non-native configuration checks and `.\gradlew.bat` on Windows.

## Routine verification matrix

| Purpose | Windows command | Expected evidence |
| --- | --- | --- |
| Show declared projects | `.\gradlew.bat projects` | 17 Gradle subprojects appear: the 16 production-target modules plus experimental `feasibility-spikes`. |
| Compile/test every module and run the root quality gate | `.\gradlew.bat buildAllModules` | Root `check` and every subproject `build` complete. |
| Run the root quality gate | `.\gradlew.bat check` | Checkstyle, architecture-boundary verification through `test-support`, and the Phase 0 source-exclusion boundary pass. |
| Run root and subproject tests | `.\gradlew.bat test` | Aggregate JUnit Platform test tasks pass, including `test-support` architecture tests and the `game-sandbox` scripted-demo timeline test when present. |
| Run only the architecture boundary suite | `.\gradlew.bat :test-support:test --tests "com.samo.architecture.ModulePackageBoundaryTest" --rerun-tasks` | The Gradle-derived subproject registry, production package ownership, main/test imports, fully qualified references, and boundary regressions pass. |
| Generate and verify JaCoCo reports | `.\gradlew.bat verifyJacocoReports` | Tests run for all configured test-bearing engine modules and each produces XML plus HTML coverage reports. |
| Run client foundation entry point | `.\gradlew.bat :game-client:runClient` | Client foundation process starts and exits cleanly. |
| Run headless server foundation entry point | `.\gradlew.bat :game-server:runServer` | Server foundation process starts in headless mode and exits cleanly. |
| Report client version metadata | `.\gradlew.bat :game-client:runClient --args="--version"` | Client reports executable, engine commit, protocol version, asset version, Java version, and native libraries available on its runtime classpath. |
| Report server version metadata | `.\gradlew.bat :game-server:runServer --args="--version"` | Server reports the same shared identifiers plus its executable-specific native-library list. |
| Verify headless server dependency boundary | `.\gradlew.bat :game-server:verifyHeadlessServerRuntime` | Server runtime classpath contains no platform/render/audio projects or GLFW/OpenGL/OpenAL artifacts, including after owner-facing sandbox demo dependencies are resolved. |
| Run owner-facing sandbox demo | `.\gradlew.bat :game-sandbox:runEngineDemo` | Human-observation path opens the scripted production window demo on an interactive Windows desktop; this is not FPS, benchmark, soak, leak-proof, or CI acceptance evidence. |
| Run CI-native lifecycle smoke locally | `$env:ALSOFT_DRIVERS="null"; .\gradlew.bat runWindowsNativeCiSmoke; .\gradlew.bat runJoltLifecycleSpike -PjoltSpikeCycles=1; Remove-Item Env:ALSOFT_DRIVERS` | Historical root task aliases delegate to `feasibility-spikes`; GLFW/OpenAL/Jolt lifecycle smoke completes. |
| Verify Java selection | `.\gradlew.bat javaToolchains` | Java 25 toolchain is available/selected. |
| Resolve committed locks | `.\gradlew.bat resolveAndLockAllDependencies` | Resolution completes without changing locks after any authorized lock update has been committed. |

## P2-T01 lifecycle verification

Issue #64 adds the production `EngineSubsystem` contract in `engine-core`. Run the full routine matrix above; `buildAllModules` includes the root `check` gate. Also run the focused acceptance suite:

```powershell
.\gradlew.bat :engine-core:test --tests "com.samo.engine.core.api.EngineSubsystemTest" --rerun-tasks
```

The JUnit 6 suite exercises successful phase order, invalid calls before hooks, early cleanup, runtime-exception/error propagation, cleanup after failed setup/activation/stopping, repeated or failed close, and reentrant calls. Test counters represent synthetic owned resources, not native leak evidence.

Focused outputs are `engine-core/build/test-results/test/TEST-com.samo.engine.core.api.EngineSubsystemTest.xml` and `engine-core/build/reports/tests/test/index.html`. The unit-test CI job uploads these as `engine-subsystem-tests`; the existing coverage job retains JaCoCo output for all configured engine modules. The filtered run replaces that job's engine-core test report after the full aggregate suite has run; the full coverage job remains unfiltered.

The build job additionally runs `resolveAndLockAllDependencies` without `--write-locks` and verifies that tracked lockfiles did not change. No lockfile or dependency change is expected for this task. Historical completed-task evidence may reference the CI lifecycle that existed when that task merged; current work follows the CI gate near the end of this document.

## P2-T02 dependency-order verification

Issue #72 adds `SubsystemGraph` without lifecycle orchestration or new dependencies. Run the full routine matrix above and the combined focused suite:

```powershell
.\gradlew.bat :engine-core:test --tests "com.samo.engine.core.api.EngineSubsystemTest" --tests "com.samo.engine.core.api.SubsystemGraphTest" --rerun-tasks
```

The graph suite tests dependency-first ordering, deterministic declaration-order traversal, shared/disconnected prerequisites, null/blank/duplicate/missing inputs, reference identity, defensive copies, repeatability and a 10,000-node chain. A cyclic synthetic caller prints the exact closed cycle diagnostic and proves no lifecycle hook runs, even for an earlier valid disconnected component. A successful synthetic caller exercises returned ordering with real `EngineSubsystem` guards and explicit cleanup; this is not rollback, native-resource evidence or the P2 phase exit.

Graph XML: `engine-core/build/test-results/test/TEST-com.samo.engine.core.api.SubsystemGraphTest.xml`. HTML: `engine-core/build/reports/tests/test/index.html`. The existing `engine-subsystem-tests` artifact now includes both lifecycle and graph XML/HTML, and `jacoco-reports` retains unfiltered coverage. The combined filtered command supersedes the lifecycle-only CI rerun so both focused reports survive together; the lifecycle-only command above remains usable on its own.

No lockfile change is expected. Use the current CI gate for new work; queued jobs or local toolchain/download failures cannot be represented as passing evidence.

## P2-T03 startup rollback verification

Issue #73 adds stateless `SubsystemStartup` coordination for an already resolved dependency-first order. Run the full routine matrix above and the combined focused suite:

```powershell
.\gradlew.bat :engine-core:test --tests "com.samo.engine.core.api.EngineSubsystemTest" --tests "com.samo.engine.core.api.SubsystemGraphTest" --tests "com.samo.engine.core.api.SubsystemStartupTest" --rerun-tasks
```

The rollback suite uses handwritten global hook traces to verify per-subsystem initialize/start activation, close of the currently failing subsystem, reverse stop/close of previously started subsystems, first-element isolation, caller-owned successful shutdown, input snapshotting, original throwable identity, continued cleanup after rollback failures, deterministic suppressed-failure order, and self-suppression avoidance.

Startup XML: `engine-core/build/test-results/test/TEST-com.samo.engine.core.api.SubsystemStartupTest.xml`. HTML remains `engine-core/build/reports/tests/test/index.html`. The `engine-subsystem-tests` artifact includes lifecycle, graph, and startup XML/HTML; `jacoco-reports` remains unfiltered.

These tests use synthetic Java subsystems. They do not establish native restartability, native leak freedom, sustained stability, or D-030's 60-second integrated Phase 2 exit gate. No dependency or lockfile change is expected.

## P2-T04 EngineClock verification

Issue #74 adds `EngineClock` as an elapsed-nanosecond sampler only. Run the full routine matrix above and the focused acceptance suite:

```powershell
.\gradlew.bat :engine-core:test --tests "com.samo.engine.core.api.EngineClockTest" --rerun-tasks
```

The clock suite uses deterministic injected readings and handwritten expectations for first-sample zero, incremental positive deltas, equal readings, negative absolute source values, forward signed-`long` wraparound, regression rejection without baseline mutation, null source, source `RuntimeException`/`Error` identity with baseline preservation, one source read per sample attempt, and default-constructor baseline establishment. It does not sleep or infer expected values from production output.

Also rerun the prior lifecycle/graph/startup suite to guard existing `engine-core` behavior:

```powershell
.\gradlew.bat :engine-core:test --tests "com.samo.engine.core.api.EngineSubsystemTest" --tests "com.samo.engine.core.api.SubsystemGraphTest" --tests "com.samo.engine.core.api.SubsystemStartupTest" --rerun-tasks
```

CI runs those four suites together after aggregate `test` so all focused XML files survive in one report directory. Clock XML is `engine-core/build/test-results/test/TEST-com.samo.engine.core.api.EngineClockTest.xml`; HTML remains `engine-core/build/reports/tests/test/index.html`. The `engine-subsystem-tests` artifact name is retained for continuity and now includes lifecycle, graph, startup, and clock XML/HTML. `jacoco-reports` remains the unfiltered coverage artifact.

These tests establish deterministic elapsed-time semantics only. They do not implement or prove the P2-T05 fixed-step accumulator, P2-T06 catch-up limits, P2-T07 interpolation, frame pacing, concurrency, native timing behavior, or D-030's 60-second integrated headless-loop exit gate. No dependency or lockfile change is expected.

## P2-T05 fixed-step accumulator verification

Issue #75 adds `FixedStepAccumulator` as the exact 60 Hz conversion from elapsed nanoseconds to newly due whole simulation ticks. Run the full routine matrix above and the focused acceptance suite:

```powershell
.\gradlew.bat :engine-core:test --tests "com.samo.engine.core.api.FixedStepAccumulatorTest" --rerun-tasks
```

The accumulator suite uses exact integer-nanosecond partitions and handwritten/independent expectations. One-second totals partitioned into 30, 60, 144, and irregular frame sequences each yield exactly 60 cumulative ticks; longer equivalent totals yield the same cumulative progress. Additional tests cover sub-tick accumulation, exact fractional carry across a tick boundary, zero input preserving progress, negative-input rejection without state mutation, and `Long.MAX_VALUE` against an independent `BigInteger` oracle. Tests do not sleep, sample a clock, use floating-point expected values, impose catch-up limits, or expose interpolation.

Also rerun all existing `engine-core` lifecycle/timing acceptance suites together:

```powershell
.\gradlew.bat :engine-core:test --tests "com.samo.engine.core.api.EngineSubsystemTest" --tests "com.samo.engine.core.api.SubsystemGraphTest" --tests "com.samo.engine.core.api.SubsystemStartupTest" --tests "com.samo.engine.core.api.EngineClockTest" --tests "com.samo.engine.core.api.FixedStepAccumulatorTest" --rerun-tasks
```

Accumulator XML: `engine-core/build/test-results/test/TEST-com.samo.engine.core.api.FixedStepAccumulatorTest.xml`. HTML remains `engine-core/build/reports/tests/test/index.html`. CI retains lifecycle, graph, startup, clock, and accumulator XML plus HTML in the existing `engine-subsystem-tests` artifact; `jacoco-reports` remains the ordinary unfiltered coverage artifact.

P2-T05 proves cadence-independent 60 Hz accumulation only. It does not clamp incoming frame gaps, cap catch-up work, expose interpolation alpha, pace frames, execute simulation callbacks, or satisfy D-030's 60-second integrated headless-loop exit gate. P2-T06 and P2-T07 remain separate tasks. No dependency or lockfile change is expected.

## P2-T06 bounded catch-up verification

Issue #76 adds `FixedStepCatchUpPolicy` above the existing accumulator. The default policy clamps a single elapsed duration to 250,000,000 ns and returns at most 5 whole simulation steps from one update; elapsed time above the clamp and whole due ticks above the step cap are discarded, while accepted fractional sub-tick progress remains in `FixedStepAccumulator`.

Run the focused acceptance suite:

```powershell
.\gradlew.bat :engine-core:test --tests "com.samo.engine.core.api.FixedStepCatchUpPolicyTest" --rerun-tasks
```

The suite uses integer-nanosecond expectations to cover the default constants, below/exactly-at/above frame-gap boundaries, exactly-at/above step-cap boundaries, a deterministic two-second stall, proof that dropped whole steps and clamped elapsed do not become later backlog, fractional remainder preservation across a capped call, zero elapsed, negative elapsed without mutation, null accumulator, and invalid explicit limits. It uses no sleep, scheduler, floating-point timing oracle, interpolation, callback, or clock sampling.

Also rerun all current `engine-core` lifecycle/timing suites together:

```powershell
.\gradlew.bat :engine-core:test --tests "com.samo.engine.core.api.EngineSubsystemTest" --tests "com.samo.engine.core.api.SubsystemGraphTest" --tests "com.samo.engine.core.api.SubsystemStartupTest" --tests "com.samo.engine.core.api.EngineClockTest" --tests "com.samo.engine.core.api.FixedStepAccumulatorTest" --tests "com.samo.engine.core.api.FixedStepCatchUpPolicyTest" --rerun-tasks
```

Catch-up XML: `engine-core/build/test-results/test/TEST-com.samo.engine.core.api.FixedStepCatchUpPolicyTest.xml`. CI includes it in the existing `engine-subsystem-tests` evidence artifact. `jacoco-reports` remains the ordinary unfiltered coverage artifact.

P2-T06 proves bounded per-update recovery policy only. It does not expose interpolation alpha, pace frames, execute simulation callbacks, integrate a runtime loop, configure limits through P2-T08/P2-T09, or satisfy D-030's 60-second Phase 2 exit gate by itself. No dependency or lockfile change is expected.

## P2-T07 interpolation-alpha verification

Issue #77 adds the read-only `FixedStepAccumulator.interpolationAlpha()` presentation query while preserving integer/rational fixed-step simulation progression.

Run the focused acceptance suite:

```powershell
.\gradlew.bat :engine-core:test --tests "com.samo.engine.core.api.FixedStepInterpolationTest" --rerun-tasks
```

The suite uses handwritten integer-derived expectations for fresh state, exact whole-tick boundaries, exact half and three-quarter fractional states, a near-one upper bound, repeated-query stability, zero elapsed, negative-input state preservation, P2-T06 catch-up capping with retained half-step alpha, frame-gap clamping, and a synthetic caller trace that counts simulation only in whole ticks while reading render alpha separately. It does not use sleeps, scheduler timing, OpenGL/renderer code, transform interpolation, variable simulation deltas, or production output as its own oracle.

Also rerun all current `engine-core` lifecycle/timing suites together:

```powershell
.\gradlew.bat :engine-core:test --tests "com.samo.engine.core.api.EngineSubsystemTest" --tests "com.samo.engine.core.api.SubsystemGraphTest" --tests "com.samo.engine.core.api.SubsystemStartupTest" --tests "com.samo.engine.core.api.EngineClockTest" --tests "com.samo.engine.core.api.FixedStepAccumulatorTest" --tests "com.samo.engine.core.api.FixedStepCatchUpPolicyTest" --tests "com.samo.engine.core.api.FixedStepInterpolationTest" --rerun-tasks
```

Interpolation XML: `engine-core/build/test-results/test/TEST-com.samo.engine.core.api.FixedStepInterpolationTest.xml`. CI includes it in the existing `engine-subsystem-tests` evidence artifact; `jacoco-reports` remains unfiltered.

P2-T07 proves only the simulation/render timing separation and normalized retained fraction. It does not implement a renderer, transform interpolation, callbacks, pacing, a runtime loop, configuration, or D-030's 60-second integrated Phase 2 exit gate. No dependency or lockfile change is expected.

## P2-T08 typed configuration verification

Issue #78 adds one fixed startup schema for already-resolved raw values. It validates fullscreen width/height and the locked 60 Hz simulation rate before subsystem startup; source layering remains P2-T09.

Run the focused acceptance suite:

```powershell
.\gradlew.bat :engine-core:test --tests "com.samo.engine.core.api.EngineConfigSchemaTest" --rerun-tasks
```

The suite uses handwritten expectations for defaults, explicit typed values, inclusive width/height bounds, rejected out-of-range values, the exact-60 tick-rate contract, trimmed/malformed integer input, unknown keys, deterministic multi-error aggregation, source preservation, immutable success/error collections, programmer-contract null handling, and a synthetic caller sequence proving a validation failure occurs before any `EngineSubsystem.initialize()` hook can execute.

Also rerun the current focused engine-core regression set together:

```powershell
.\gradlew.bat :engine-core:test --tests "com.samo.engine.core.api.EngineSubsystemTest" --tests "com.samo.engine.core.api.SubsystemGraphTest" --tests "com.samo.engine.core.api.SubsystemStartupTest" --tests "com.samo.engine.core.api.EngineClockTest" --tests "com.samo.engine.core.api.FixedStepAccumulatorTest" --tests "com.samo.engine.core.api.FixedStepCatchUpPolicyTest" --tests "com.samo.engine.core.api.FixedStepInterpolationTest" --tests "com.samo.engine.core.api.EngineConfigSchemaTest" --rerun-tasks
```

Config XML: `engine-core/build/test-results/test/TEST-com.samo.engine.core.api.EngineConfigSchemaTest.xml`. CI includes it in `engine-subsystem-tests`; `jacoco-reports` remains unfiltered.

P2-T08 proves validation of one effective raw map only. It does not load or merge config sources, change source precedence, make tick rate configurable, perform runtime hot reload, or satisfy D-030's 60-second integrated Phase 2 gate. No dependency or lockfile change is expected.

## P2-T09 layered configuration verification

Issue #79 adds `EngineConfigLoader`, which composes the fixed startup precedence `EngineConfigSchema` defaults < game file < user file < command-line overrides and validates the final effective raw map once through the existing P2-T08 schema.

Run the focused acceptance suite:

```powershell
.\gradlew.bat :engine-core:test --tests "com.samo.engine.core.api.EngineConfigLoaderTest" --rerun-tasks
```

The suite uses temporary UTF-8 files and handwritten expectations for all four precedence levels, fallback when each higher layer is removed, independent keys falling through different layers, missing optional files, normalized `path:line` and `command line` source provenance, an invalid lower value replaced by a valid higher value, malformed lines, blank keys, duplicate keys, comments/blank lines, first-separator behavior, programmer-contract nulls, an existing unreadable/non-file path, immutable successful output, and a synthetic startup trace proving loading/validation failure prevents subsystem initialization.

Also rerun the complete focused `engine-core` lifecycle/timing/config regression set together:

```powershell
.\gradlew.bat :engine-core:test --tests "com.samo.engine.core.api.EngineSubsystemTest" --tests "com.samo.engine.core.api.SubsystemGraphTest" --tests "com.samo.engine.core.api.SubsystemStartupTest" --tests "com.samo.engine.core.api.EngineClockTest" --tests "com.samo.engine.core.api.FixedStepAccumulatorTest" --tests "com.samo.engine.core.api.FixedStepCatchUpPolicyTest" --tests "com.samo.engine.core.api.FixedStepInterpolationTest" --tests "com.samo.engine.core.api.EngineConfigSchemaTest" --tests "com.samo.engine.core.api.EngineConfigLoaderTest" --rerun-tasks
```

Loader XML: `engine-core/build/test-results/test/TEST-com.samo.engine.core.api.EngineConfigLoaderTest.xml`. CI includes it in `engine-subsystem-tests`; `jacoco-reports` remains unfiltered.

P2-T09 proves only the fixed four-level startup source precedence, bounded UTF-8 `key=value` parsing, source attribution, and validate-after-merge behavior. It does not add environment variables, raw argv parsing, OS path discovery, config persistence, Java `Properties` escaping/continuations, hot reload, mutable settings, configurable tick rate, subsystem startup orchestration, or D-030's 60-second integrated Phase 2 gate. No dependency or lockfile change is expected.

## P2-T10 native-resource registry verification

Issue #80 adds `NativeResourceRegistry` as a pure-Java diagnostic registry for explicitly owned native handles. It does not add or call a native binding.

Run the focused acceptance suite:

```powershell
.\gradlew.bat :engine-core:test --tests "com.samo.engine.core.api.NativeResourceRegistryTest" --rerun-tasks
```

The suite uses synthetic opaque handles and counted closers with handwritten expectations. It verifies empty shutdown, successful close/removal, idempotent repeated close, intentionally leaked-resource failure without force cleanup, deterministic registration-order diagnostics, normalized type and captured allocation-site reporting, duplicate live identity rejection, same numeric handle under different resource types, identity reuse after successful release, invalid zero/blank/null contracts, nonzero negative opaque handles, exact `RuntimeException`/`Error` propagation from closers, terminal non-retried close failure remaining visible as `CLOSE_FAILED`, reentrant-close rejection, and repeated non-mutating verification.

Also rerun the complete focused `engine-core` lifecycle/timing/config/native-ownership regression set together:

```powershell
.\gradlew.bat :engine-core:test --tests "com.samo.engine.core.api.EngineSubsystemTest" --tests "com.samo.engine.core.api.SubsystemGraphTest" --tests "com.samo.engine.core.api.SubsystemStartupTest" --tests "com.samo.engine.core.api.EngineClockTest" --tests "com.samo.engine.core.api.FixedStepAccumulatorTest" --tests "com.samo.engine.core.api.FixedStepCatchUpPolicyTest" --tests "com.samo.engine.core.api.FixedStepInterpolationTest" --tests "com.samo.engine.core.api.EngineConfigSchemaTest" --tests "com.samo.engine.core.api.EngineConfigLoaderTest" --tests "com.samo.engine.core.api.NativeResourceRegistryTest" --rerun-tasks
```

Registry XML: `engine-core/build/test-results/test/TEST-com.samo.engine.core.api.NativeResourceRegistryTest.xml`. CI includes it in `engine-subsystem-tests`; `jacoco-reports` remains unfiltered.

P2-T10 proves Java bookkeeping and shutdown diagnostics only. It does not prove actual GLFW/OpenGL/Jolt/OpenAL/Steam resources are leak-free or restartable, does not add force-close-all behavior or thread-affinity dispatch, and does not satisfy P0-T13/P0-T14 or D-030's 60-second integrated Phase 2 gate. No dependency or lockfile change is expected.

## P2-T11 allocation-metric verification

Issue #81 adds a test-only allocation-observability benchmark using Java 25 JFR `jdk.ObjectAllocationSample`. It adds no production API and no dependency.

Run the focused acceptance benchmark:

```powershell
.\gradlew.bat :engine-core:test --tests "com.samo.engine.core.api.AllocationMetricBenchmarkTest" --rerun-tasks
```

The benchmark warms each workload before recording, then measures a uniquely named dedicated platform thread in a separate JFR recording window. Only that thread's allocation samples are attributed to the channel. `weight` values are summed as sampled heap-allocation pressure and divided by measured iteration count for an estimated bytes-per-iteration value. Simulation-tick and synthetic/headless render-frame channels are recorded independently, and an allocating control plus a nonallocating arithmetic control exercise evidence availability and attribution.

The focused run must create:

`engine-core/build/reports/allocation/p2-t11-allocation-metric.txt`

The report records Java version, measurement source, `estimate=true`, warm-up/measured iteration counts, sample counts, sampled weight bytes, durations, estimated bytes/tick and bytes/frame, control observations, and explicit limitations. Numeric JFR results are machine/run dependent; tests do not assert an exact live sampled value. The allocating control must produce at least one usable positive-weight sample or the benchmark fails instead of fabricating zero evidence.

This metric covers sampled Java heap allocation pressure only. It does not measure direct/native/GPU allocations, retained heap, GC pause cost, or exact object-by-object allocation. The render workload is synthetic/headless and is not OpenGL-renderer evidence. P2-T11 defines no allocation budget and does not satisfy P0-T13/P0-T14 or D-030's 60-second P2 gate.

CI runs `AllocationMetricBenchmarkTest` with the existing focused engine-core suite and uploads both its JUnit XML and `p2-t11-allocation-metric.txt` in the retained engine-core evidence artifact. `jacoco-reports` remains the ordinary unfiltered coverage artifact.

## P2-T12 structured logging verification

Issue #82 adds the JDK-only synchronous `EngineLogger` structured logging boundary in `engine-core` and no external logging dependency or persisted format.

Run the focused acceptance suite:

```powershell
.\gradlew.bat :engine-core:test --tests "com.samo.engine.core.api.EngineLoggerTest" --rerun-tasks
```

The suite uses handwritten capturing, failing, and blocking sinks. It verifies all required context fields, caller-thread ID/name capture, nullable missing fields, string normalization, negative frame/tick rejection, connection-field filtering independent of message text, forwarding of every severity, invalid-input rejection before sink invocation, exact sink `RuntimeException`/`Error` propagation, explicit flush delegation, and serialized sink callbacks across concurrent caller threads while preserving each event's original caller identity.

Also rerun the complete current focused `engine-core` regression set together:

```powershell
.\gradlew.bat :engine-core:test --tests "com.samo.engine.core.api.EngineSubsystemTest" --tests "com.samo.engine.core.api.SubsystemGraphTest" --tests "com.samo.engine.core.api.SubsystemStartupTest" --tests "com.samo.engine.core.api.EngineClockTest" --tests "com.samo.engine.core.api.FixedStepAccumulatorTest" --tests "com.samo.engine.core.api.FixedStepCatchUpPolicyTest" --tests "com.samo.engine.core.api.FixedStepInterpolationTest" --tests "com.samo.engine.core.api.EngineConfigSchemaTest" --tests "com.samo.engine.core.api.EngineConfigLoaderTest" --tests "com.samo.engine.core.api.NativeResourceRegistryTest" --tests "com.samo.engine.core.api.AllocationMetricBenchmarkTest" --tests "com.samo.engine.core.api.EngineLoggerTest" --rerun-tasks
```

Logger XML: `engine-core/build/test-results/test/TEST-com.samo.engine.core.api.EngineLoggerTest.xml`. CI includes it in the existing `engine-subsystem-tests` evidence artifact; `jacoco-reports` remains unfiltered. P2-T12 does not create a file/console log artifact because it defines no persisted log format.

P2-T12 proves the typed in-memory event/sink contract only. It does not establish threshold routing, persistence, rotation, asynchronous logging, a global singleton, concrete runtime call sites, or fatal process termination. P2-T13 owns orderly fatal assertion/shutdown behavior. No dependency or lockfile change is expected.

## P2-T13 orderly fatal-termination verification

Issue #83 adds the JDK-only `FatalTermination` coordinator in `engine-core`. It composes the existing subsystem lifecycle, native-resource registry, and structured logger without changing their APIs, adding dependencies, or selecting a persisted log format.

Run the focused acceptance suite:

```powershell
.\gradlew.bat :engine-core:test --tests "com.samo.engine.core.api.FatalTerminationTest" --rerun-tasks
```

The suite uses handwritten lifecycle/log traces, counted synthetic resource closers, failing sinks, and an injected package-private terminator seam to verify the exact fatal sequence, reverse subsystem cleanup, stop-then-close behavior after stop failure, non-force-closing registry verification, ordered cleanup-failure diagnostics, flush ordering, unchecked logging/cleanup/verification/flush failure containment, exact terminator throwable identity with suppressed failures, returning-terminator rejection, and one-shot reentrant/concurrent/later-call rejection.

The same focused JUnit suite launches a bounded child JVM for the real public constructor. The child calls production `System.exit(1)` only after recording FATAL receipt, subsystem stop, subsystem close/resource release, and logger flush. Do not invoke the public fatal path directly from the Gradle/JUnit process outside that child harness.

Also rerun the complete current focused `engine-core` regression set together:

```powershell
.\gradlew.bat :engine-core:test --tests "com.samo.engine.core.api.EngineSubsystemTest" --tests "com.samo.engine.core.api.SubsystemGraphTest" --tests "com.samo.engine.core.api.SubsystemStartupTest" --tests "com.samo.engine.core.api.EngineClockTest" --tests "com.samo.engine.core.api.FixedStepAccumulatorTest" --tests "com.samo.engine.core.api.FixedStepCatchUpPolicyTest" --tests "com.samo.engine.core.api.FixedStepInterpolationTest" --tests "com.samo.engine.core.api.EngineConfigSchemaTest" --tests "com.samo.engine.core.api.EngineConfigLoaderTest" --tests "com.samo.engine.core.api.NativeResourceRegistryTest" --tests "com.samo.engine.core.api.AllocationMetricBenchmarkTest" --tests "com.samo.engine.core.api.EngineLoggerTest" --tests "com.samo.engine.core.api.FatalTerminationTest" --rerun-tasks
```

Fatal-shutdown XML: `engine-core/build/test-results/test/TEST-com.samo.engine.core.api.FatalTerminationTest.xml`. CI includes it in the existing `engine-subsystem-tests` evidence artifact; the child marker file remains temporary test evidence and is not a production or retained log format. `jacoco-reports` remains unfiltered.

P2-T13 proves Java fatal-shutdown orchestration only. It does not prove actual GLFW/OpenGL/Jolt/OpenAL/Steam native cleanup, long-duration stability, restartability, or D-030's separate 60-second Phase 2 integrated exit gate. P0-T09A/P0-T13/P0-T14 remain independent feasibility gates. No dependency or lockfile change is expected.

## P2 integrated exit-gate verification — Issue #135 / D-030

Issue #135 changes only the Phase 2 integration duration from the original ten minutes to 60 continuous seconds. The gate still requires deterministic fixed 60 Hz simulation ticks, bounded catch-up, orderly lifecycle shutdown, and verified cleanup together. It is an integration-correctness gate, not native soak/restartability evidence.

The long-running JUnit path is deliberately opt-in so ordinary aggregate tests and JaCoCo generation do not each add another minute. Run it explicitly on Windows or another supported Java 25 environment with:

```powershell
$env:SHERKO_P2_EXIT_GATE="true"
.\gradlew.bat :engine-core:test --tests "com.samo.engine.core.api.Phase2IntegratedGateTest" --rerun-tasks
Remove-Item Env:SHERKO_P2_EXIT_GATE
```

Without `SHERKO_P2_EXIT_GATE=true`, the JUnit test is skipped by assumption and **does not count as exit-gate evidence**.

The enabled test must:

- observe at least 60 continuous seconds of runtime;
- use the production `EngineClock`, `FixedStepAccumulator`, and default `FixedStepCatchUpPolicy` in one loop;
- execute simulation only as whole fixed 60 Hz steps returned by the production policy;
- inject one real two-second stall after startup and observe at most the configured five exposed catch-up steps; the expected default result for that stall is exactly five exposed steps after the 250 ms clamp;
- start a synthetic subsystem through `SubsystemStartup`, then stop and close it orderly;
- release its tracked `NativeResourceRegistry` registration through owner cleanup exactly once;
- require `NativeResourceRegistry.assertNoOpenResources()` to pass after cleanup.

The enabled run must create:

`engine-core/build/reports/phase2/p2-exit-60-second-gate.txt`

Require the report to record `result=PASS`, configured/observed duration, fixed tick rate, executed tick count, loop update count, catch-up cap and observed maximum, injected stall observation, lifecycle trace, registry-empty result, exact `GITHUB_SHA` when run in CI, Java/OS environment, and the explicit limitation that this is Java headless integration evidence rather than native soak/stability evidence.

CI enables this test exactly once in the focused `engine-core` evidence step and uploads both its JUnit XML and the report inside `engine-subsystem-tests`. The aggregate `test` and JaCoCo jobs do not enable it, preventing duplicate 60-second runs. Historical Phase 2 closure used the CI lifecycle in force at that time; current tasks follow the final-candidate-heavy plus exact-merge-lightweight lifecycle documented below.

P0-T13 remains the separate 15-minute combined-native sustained-stability gate and P0-T14 remains repeated native lifecycle/restartability evidence. A passing 60-second Java gate must not be represented as satisfying either one.

## P3-T01 production GLFW/OpenGL window verification

Issue #84 adds `com.samo.engine.platform.api.GlfwWindow` as the first production `engine-platform-lwjgl` subsystem. The task uses the existing LWJGL 3.4.3 selection and D-018/D-027/D-028 contracts; it does not introduce another window library, renderer loop, input/event API, or OpenGL debug callback.

Run the deterministic acceptance suite, which uses the package-private backend seam and no real display:

```powershell
.\gradlew.bat :engine-platform-lwjgl:test --tests "com.samo.engine.platform.api.GlfwWindowTest" --rerun-tasks
```

The suite uses handwritten traces/fixtures to verify constructor validation before native calls, the exact OpenGL 4.6 Core GLFW hint sequence, window registration/ownership, actual-version/renderer structured logging, initialize/start rollback, error-callback ownership/restoration, stop cleanup after failure, owner-thread affinity, and empty-registry cleanup. These tests are ownership/failure-policy evidence; they do not establish real GPU/context support.

Run the real native acceptance only on the target Windows x64 / Java 25 environment:

```powershell
$env:SHERKO_P3_T01_NATIVE="true"
.\gradlew.bat :engine-platform-lwjgl:test --tests "com.samo.engine.platform.api.GlfwWindowNativeTest" --rerun-tasks
Remove-Item Env:SHERKO_P3_T01_NATIVE
```

Without `SHERKO_P3_T01_NATIVE=true`, the native JUnit is skipped by assumption and does not count as P3-T01 acceptance evidence.

The enabled native test uses the public production constructor. It creates the hidden-then-shown window/context, independently queries the current OpenGL major/minor plus `GL_VERSION` and `GL_RENDERER`, requires actual OpenGL >=4.6, requires the two captured D-028 INFO events to equal those independently observed strings with `subsystem=platform`, then stops/closes and requires no current context plus `NativeResourceRegistry.assertNoOpenResources()` success.

The enabled run must create:

`engine-platform-lwjgl/build/reports/p3/p3-t01-glfw-window.txt`

Require stable fields for `result=PASS`, requested `4.6 Core`, actual major/minor/version/renderer, exact `GITHUB_SHA` in CI, Java/OS environment, lifecycle cleanup, registry emptiness, and the stated limitation that this is one production window/context lifecycle run rather than P0-T13 soak or P0-T14 repeated-lifecycle evidence.

P3-T01 also changes dependency ownership: after adding the existing LWJGL core/GLFW/OpenGL libraries and Windows natives to `engine-platform-lwjgl`, regenerate locks with `resolveAndLockAllDependencies --write-locks`, inspect `engine-platform-lwjgl/gradle.lockfile`, then run `resolveAndLockAllDependencies` without write mode and require a clean lock diff. The selected LWJGL version and repository project-edge direction must remain unchanged.

CI keeps the deterministic suite in ordinary aggregate `test` with the native test skipped. The existing Windows native job enables `GlfwWindowNativeTest` exactly once, after the historical GLFW/OpenAL smoke, and uploads its JUnit XML plus `p3-t01-glfw-window.txt` as artifact `p3-t01-glfw-window`. This section records the P3-T01 evidence contract; current task merge/closure mechanics are governed by the current CI gate below.

## P3-T02 logical/framebuffer sizing verification

Issue #85 extends the production `GlfwWindow` boundary with `WindowSizeListener` and owner-thread `pollEvents()` while keeping logical window units distinct from framebuffer pixels. It adds no dependency, renderer, fullscreen/input behavior, raw handle, or content-scale callback API.

Run the deterministic acceptance suite:

```powershell
.\gradlew.bat :engine-platform-lwjgl:test --tests "com.samo.engine.platform.api.GlfwWindowTest" --rerun-tasks
```

The suite must cover intentionally unequal logical/framebuffer pairs, initial independent queries, latest-value coalescing and logical-before-framebuffer delivery, valid zero framebuffer axes, negative platform-dimension rejection before consumer delivery, illegal polling state, wrong-thread polling, receiver failure propagation, callback setup/cleanup failure, and the preserved P3-T01 lifecycle/ownership behavior.

Run the real native acceptance only on target Windows x64 / Java 25:

```powershell
$env:SHERKO_P3_T02_NATIVE="true"
.\gradlew.bat :engine-platform-lwjgl:test --tests "com.samo.engine.platform.api.GlfwWindowSizeNativeTest" --rerun-tasks
Remove-Item Env:SHERKO_P3_T02_NATIVE
```

Without `SHERKO_P3_T02_NATIVE=true`, that JUnit test is skipped by assumption and does not count as P3-T02 native evidence.

The enabled test must use the production `GlfwWindow`, resize the real window, poll only through the production `pollEvents()` API, and compare delivered logical/framebuffer values against independent direct GLFW `glfwGetWindowSize` and `glfwGetFramebufferSize` observations from test code. It also records `glfwGetWindowContentScale`. A 100% scaling machine is allowed to report equal logical/framebuffer dimensions; the test/report must state the observed equality honestly rather than fabricating DPI separation. A DPI-scaled environment may naturally produce distinct dimensions.

The enabled run must create:

`engine-platform-lwjgl/build/reports/p3/p3-t02-window-size.txt`

Require stable fields for `task=P3-T02`, `result=PASS`, logical and framebuffer dimensions, whether the observed pairs are distinct, X/Y content scale, exact `GITHUB_SHA` in CI, Java/OS environment, and empty-registry cleanup. This is bounded production window-size integration evidence only; it is not renderer, P0-T13 soak, or P0-T14 repeated-lifecycle evidence.

No dependency or lockfile change is expected for P3-T02. Run `resolveAndLockAllDependencies` without write mode and require the tracked lockfiles to remain unchanged. The routine verification matrix still applies.

CI keeps deterministic P3-T02 coverage inside ordinary aggregate tests. The Windows native job enables `GlfwWindowSizeNativeTest` exactly once after the P3-T01 native window test and uploads its JUnit XML plus report as artifact `p3-t02-window-size`. Current task merge/closure mechanics are governed by the current CI gate below.

## P3-T03 window mode transition verification

Issue #86 extends the D-031/D-032 production `GlfwWindow` boundary with `WindowMode` and owner-thread `setWindowMode(...)`. It adds no dependency, renderer, monitor-selection/custom-resolution API, input/focus behavior, raw handle, or later Phase 3 implementation.

Run the deterministic acceptance suite:

```powershell
.\gradlew.bat :engine-platform-lwjgl:test --tests "com.samo.engine.platform.api.GlfwWindowTest" --rerun-tasks
```

The suite must cover lifecycle/thread/null rejection before native transition work, same-mode no-op, windowed-geometry capture/restore, primary-monitor borderless and exclusive target values, direct borderless/exclusive transitions without losing the original windowed restore geometry, fresh geometry capture after a completed return to windowed, invalid monitor/video-mode data, original failure identity plus one rollback attempt/suppressed rollback failure, and the preserved P3-T01/P3-T02 lifecycle/size behavior.

Run the real native acceptance only on target Windows x64 / Java 25:

```powershell
$env:SHERKO_P3_T03_NATIVE="true"
.\gradlew.bat :engine-platform-lwjgl:test --tests "com.samo.engine.platform.api.GlfwWindowModeNativeTest" --rerun-tasks
Remove-Item Env:SHERKO_P3_T03_NATIVE
```

Without `SHERKO_P3_T03_NATIVE=true`, that JUnit test is skipped by assumption and does not count as P3-T03 native evidence.

The enabled test starts from the production windowed `GlfwWindow` and executes exactly 20 successful transitions as five repetitions of:

1. `BORDERLESS_FULLSCREEN`
2. `WINDOWED`
3. `EXCLUSIVE_FULLSCREEN`
4. `WINDOWED`

After every transition the test independently requires `glfwGetCurrentContext()` to remain the original nonzero window/context handle, requires `GL_VERSION` to remain nonblank, checks direct GLFW monitor/window state against the requested mode, and checks positive expected logical dimensions. Every return to windowed must restore the originally captured window position and size. The final stop/close path requires no current context and `NativeResourceRegistry.assertNoOpenResources()` success.

The enabled run must create:

`engine-platform-lwjgl/build/reports/p3/p3-t03-window-modes.txt`

Require stable fields for `task=P3-T03`, `result=PASS`, `transition.count=20`, the exact repeated transition sequence, primary-monitor/current-video-mode observations, initial/restored window geometry, all-transition context preservation, GL version, exact `GITHUB_SHA` in CI, Java/OS environment, and empty-registry cleanup. This is one bounded production 20-transition acceptance run; it is not renderer evidence, P0-T13 soak evidence, or P0-T14 repeated-lifecycle evidence.

No dependency, dependency-ownership, or lockfile change is expected for P3-T03. Run `resolveAndLockAllDependencies` without write mode and require tracked lockfiles to remain unchanged. The routine verification matrix still applies.

CI keeps deterministic P3-T03 coverage inside ordinary aggregate tests. The Windows native job enables `GlfwWindowModeNativeTest` exactly once after the P3-T02 native size test and uploads its JUnit XML plus report as artifact `p3-t03-window-modes`. Current task merge/closure mechanics are governed by the current CI gate below; stale/cancelled superseded PR runs are never passing evidence for the final candidate.

## P3-T04 focus-loss input safety verification

Issue #87 extends the D-031/D-032/D-033 `GlfwWindow` boundary with owner-thread `setCursorCaptured(boolean)` and internally owned focus/key/mouse-button safety state. It adds no dependency, lockfile/module-edge change, raw mouse, public `InputSnapshot`, action mapping, controller policy, renderer behavior, or P3-T05+ implementation.

Run the deterministic regression and focused suites together:

```powershell
.\gradlew.bat :engine-platform-lwjgl:test --tests "com.samo.engine.platform.api.GlfwWindowTest" --tests "com.samo.engine.platform.api.GlfwWindowFocusTest" --rerun-tasks
```

The focused suite must prove started/owner-thread capture rules, idempotent capture/release, key press/repeat/release, mouse-button press/release, safe invalid indices, focus-loss clearing of all tracked held state before cursor release, no cursor mutation when capture is inactive, no automatic recapture after focus regain, explicit recapture, one-shot staged propagation of a cursor-release failure from `pollEvents()`, partial start cleanup, and one-time input-callback release. Existing P3-T01/P3-T02/P3-T03 behavior remains covered by `GlfwWindowTest` and aggregate tests.

Run the real native acceptance only on target Windows x64 / Java 25:

```powershell
$env:SHERKO_P3_T04_NATIVE="true"
.\gradlew.bat :engine-platform-lwjgl:test --tests "com.samo.engine.platform.api.GlfwWindowFocusNativeTest" --rerun-tasks
Remove-Item Env:SHERKO_P3_T04_NATIVE
```

Without `SHERKO_P3_T04_NATIVE=true`, that JUnit test is skipped by assumption and does not count as P3-T04 native evidence.

The enabled native test starts the public production `GlfwWindow`, requests cursor capture, independently verifies GLFW disabled-cursor mode, transfers real window focus to a test-only second GLFW helper window, polls the production API until focus loss is observed, verifies the production cursor is normal, restores focus, verifies no automatic recapture, explicitly requests capture again, and verifies disabled-cursor mode returns. The helper is test-only and does not justify a production raw-handle API.

The enabled run must create:

`engine-platform-lwjgl/build/reports/p3/p3-t04-focus-loss.txt`

Require stable fields for `task=P3-T04`, `result=PASS`, focus-transfer mechanism, cursor mode before loss/after loss/after regain/after explicit recapture, `no.auto.recapture=true`, exact `GITHUB_SHA` in CI, Java/OS/arch, empty-registry cleanup, the retained manual Alt+Tab held-key scenario, and the evidence limitation that P3-T04 does not yet expose public `InputSnapshot` gameplay state.

No dependency or lockfile change is expected for P3-T04. Run `resolveAndLockAllDependencies` without write mode and require tracked lockfiles to remain unchanged. The routine verification matrix still applies.

CI keeps deterministic P3-T04 coverage in ordinary aggregate tests. The Windows native job enables `GlfwWindowFocusNativeTest` exactly once after P3-T03 native verification, uploads its JUnit XML and report as artifact `p3-t04-focus-loss`, then continues the preserved Jolt lifecycle smoke. Current task merge/closure mechanics are governed by the current CI gate below.

## P3-T05 raw/fallback relative mouse verification

Issue #88 extends the D-031 through D-034 `GlfwWindow` boundary with D-035 internal relative mouse acquisition. It adds no public mouse-delta API, `InputSnapshot`, action mapping, controller policy, renderer behavior, dependency, lockfile, or module edge.

Run the deterministic regression/focused suite:

```powershell
.\gradlew.bat :engine-platform-lwjgl:test --tests "com.samo.engine.platform.api.GlfwWindowTest" --tests "com.samo.engine.platform.api.GlfwWindowFocusTest" --tests "com.samo.engine.platform.api.GlfwWindowMouseMotionTest" --rerun-tasks
```

The focused suite must prove that cursor-position events are ignored before effective capture and while unfocused; the first eligible sample establishes a zero-delta baseline; later signed X/Y deltas accumulate independently of absolute screen position; release/focus loss clear pending motion and invalidate the baseline; focus regain never auto-enables raw/capture; explicit recapture restarts from a zero-delta baseline; raw-supported capture enables raw mode; forced unsupported selection uses the fallback without attempting unsupported enablement; raw-enable failures preserve the original throwable while rolling back best-effort; focus-loss raw/cursor failures are staged and propagated once from `pollEvents()`; and callback/start/stop/close cleanup releases ownership exactly once.

Run the real native acceptance only on target Windows x64 / Java 25:

```powershell
$env:SHERKO_P3_T05_NATIVE="true"
.\gradlew.bat :engine-platform-lwjgl:test --tests "com.samo.engine.platform.api.GlfwWindowMouseMotionNativeTest" --rerun-tasks
Remove-Item Env:SHERKO_P3_T05_NATIVE
```

Without `SHERKO_P3_T05_NATIVE=true`, that JUnit test is skipped by assumption and does not count as P3-T05 native evidence.

The native test uses the public production `GlfwWindow` lifecycle/capture path and direct test-only GLFW observations as the oracle. It records whether the machine supports raw motion, requires raw mode off before capture, requires raw mode on during capture when supported (and off otherwise), requires raw mode off after explicit release and real focus loss, requires focus regain not to auto-recapture/re-enable raw, and requires the expected raw state after explicit recapture. It ends with normal release/stop/close and `NativeResourceRegistry.assertNoOpenResources()` success.

Do **not** use `glfwSetCursorPos()` as proof of physical raw-device motion. GLFW raw mode does not guarantee that a programmatic cursor-position request produces a raw callback equivalent to hardware movement. Exact delta arithmetic, baseline reset, and forced fallback selection are therefore deterministic-test evidence; the native acceptance proves real GLFW raw-mode/focus/lifecycle integration. This evidence split is intentional and must be stated honestly.

The enabled run must create:

`engine-platform-lwjgl/build/reports/p3/p3-t05-mouse-motion.txt`

Require stable fields for `task=P3-T05`, `result=PASS`, raw support, raw mode before/during/after capture/release/focus transitions, no auto-raw enable on focus regain, fallback deterministic-coverage attribution, exact `GITHUB_SHA` in CI, Java/OS/arch, empty-registry cleanup, and limitations. The fallback must not claim OS pointer-acceleration bypass, and no evidence may claim the P3-T06 public `InputSnapshot` exists.

No dependency or lockfile change is expected. Run `resolveAndLockAllDependencies` without write mode and require tracked lockfiles to remain unchanged. In the heavy final-candidate CI, the Windows native job enables `GlfwWindowMouseMotionNativeTest` after P3-T04, uploads JUnit XML plus the report as artifact `p3-t05-mouse-motion`, and then continues the preserved Jolt smoke.

Final acceptance follows the current CI gate: one passing heavy five-job workflow on the exact final PR candidate, then after merge one passing lightweight exact-merge `master` verifier. A second routine full five-job master matrix is not required. If the active Issue explicitly requires exact-merge native/performance evidence beyond the lightweight verifier, use deliberate `workflow_dispatch` or the task-specific command rather than silently weakening the requirement.

## P3-T06 renderer-frame InputSnapshot verification

Issue #89 extends the D-031 through D-035 `GlfwWindow` boundary with D-036 public immutable `InputSnapshot`, `InputKey`, `InputMouseButton`, and `GlfwWindow.captureInputSnapshot(long)`. It adds no dependency, lockfile/module edge, data-driven action mapping, controller policy, player-command/replay format, renderer behavior, or P3-T07+ implementation.

Run the deterministic snapshot/focus/motion regression suite:

```powershell
.\gradlew.bat :engine-platform-lwjgl:test --tests "com.samo.engine.platform.api.InputSnapshotTest" --tests "com.samo.engine.platform.api.GlfwWindowInputSnapshotTest" --tests "com.samo.engine.platform.api.GlfwWindowFocusTest" --tests "com.samo.engine.platform.api.GlfwWindowMouseMotionTest" --rerun-tasks
```

The suite must prove immutable snapshot stability, exact non-negative caller frame identity, engine-defined key/button queries without public GLFW codes, retained press/release edges, a complete press+release between snapshots, key-repeat without an extra press edge, mouse-button edge equivalents, one-shot relative mouse-delta consumption while preserving the D-035 baseline, focus-loss release synthesis plus stale-press/motion clearing, validation failures before consumption, stable shared reads from one snapshot, and that `captureInputSnapshot(...)` never polls GLFW itself.

Also run the public-demo/headless/dependency checks:

```powershell
.\gradlew.bat :game-sandbox:test --rerun-tasks
.\gradlew.bat :game-server:verifyHeadlessServerRuntime
.\gradlew.bat resolveAndLockAllDependencies
```

No dependency/version/lockfile/module-edge change is expected. The headless check must continue to prove that the sandbox's existing demo-only platform dependency is non-exported and that `game-server` contains no platform/render/audio/GLFW/OpenGL/OpenAL runtime dependency.

P3-T06 adds no new native call, so it does not add a new native-only acceptance test solely for snapshot construction. The exact final PR candidate must still pass the existing `Windows native smoke` job, thereby regressing the real P3-T01 through P3-T05 GLFW/focus/raw ingestion paths on the same candidate. If a future implementation change introduces behavior that cannot truthfully be established by deterministic backend tests plus those existing native paths, refine the active Issue before adding a new native oracle.

The owner-facing manual demo remains:

```powershell
.\gradlew.bat :game-sandbox:runEngineDemo
```

P3-T06 extends that demo to capture one public `InputSnapshot` after each demo-frame `pollEvents()` call and emit bounded once-per-second frame/focus/capture/WASD/mouse-delta diagnostics. This is human observation only; it is not renderer, FPS, performance, soak, or acceptance evidence.

Final acceptance follows the current CI gate: one passing heavy five-job workflow on the exact final PR candidate, then one passing lightweight exact-merge `master` verifier after merge. Do not repeat the routine heavy matrix after merge unless the active Issue explicitly requires stronger exact-merge evidence.

## P3-T07 data-driven action-binding verification

Issue #90 adds D-037 public immutable gameplay-action binding metadata plus strict JSON schema-v1 loading in `engine-platform-lwjgl`. It intentionally adds no action evaluation/transitions, controller input, sensitivity/dead-zone curves, tick-aligned `PlayerInputCommand`, replay/network codec, renderer/camera behavior, or project/module dependency edge.

Run the focused deterministic acceptance suite:

```powershell
.\gradlew.bat :engine-platform-lwjgl:test --tests "com.samo.engine.platform.api.InputActionBindingsTest" --rerun-tasks
```

The suite must cover the committed complete v1 fixture, all eleven required action names, MOVE/LOOK `VECTOR2` classification and all other `DIGITAL` classifications, key/mouse-button/mouse-delta descriptors, defensive ownership/immutable collections, missing/duplicate/unknown/empty actions, malformed JSON, missing/unreadable/null paths, unknown root/action/binding properties, unsupported schema version, unknown binding/control enums, invalid action components, zero/non-finite scale, exact duplicate bindings, and duplicate JSON object fields. Expected descriptors remain handwritten independently of the parser.

Run the architecture/headless checks:

```powershell
.\gradlew.bat :test-support:test --tests "com.samo.architecture.ModulePackageBoundaryTest" --rerun-tasks
.\gradlew.bat :game-server:verifyHeadlessServerRuntime
```

P3-T07 intentionally adds the first production Jackson dependency under the existing scope-approved Jackson JSON technology selection. Regenerate locks once with:

```powershell
.\gradlew.bat resolveAndLockAllDependencies --write-locks
```

Then inspect the complete dependency/lock diff. The expected platform-module additions are `jackson-databind:2.21.2`, `jackson-core:2.21.2`, and the matching `jackson-annotations:2.21`. No other selected dependency version, project dependency edge, or unrelated lockfile should change. After committing the authorized lock update, rerun:

```powershell
.\gradlew.bat resolveAndLockAllDependencies
```

and require no further lock drift.

Public source/API inspection plus the architecture suite must confirm that Jackson, GLFW, and LWJGL classes do not appear in the new binding API signatures. `game-server` must remain independent of `engine-platform-lwjgl` and therefore does not load action binding files directly at this stage.

Sandbox impact is intentionally none: configuration metadata/loading is not meaningfully human-observable until P3-T08 evaluates bindings into action values. Do not add fake action diagnostics or implement T08 semantics solely for a demo.

Before the final PR, run every applicable routine command from the matrix when the authoring environment supports the repository checkout/toolchain. If the current authoring environment cannot execute Gradle, record those commands as not run rather than as passing; final acceptance still requires the exact non-draft PR candidate to pass all five heavy CI jobs on the configured Windows x64 runner.

P3-T07 itself adds no native operation. The existing `Windows native smoke` final-candidate job must still regress P3-T01 through P3-T05 native behavior on the same candidate. After merge, one lightweight exact-merge `master` verifier is required before Issue #90 may close. P3-T08 remains planning-only until then.

## P3-T04A owner-facing sandbox demo verification

Issue #149 turns the existing `game-sandbox` skeleton into the canonical manual owner-observation surface without changing any public engine API. The demo consumes only already-public production APIs and deliberately does not replace tests, native acceptance, phase gates, or CI.

Run the focused deterministic timeline suite:

```powershell
.\gradlew.bat :game-sandbox:test --tests "com.samo.game.sandbox.demo.EngineDemoTimelineTest" --rerun-tasks
```

The suite must independently verify the scripted order and boundaries for `WINDOWED -> BORDERLESS_FULLSCREEN -> WINDOWED -> EXCLUSIVE_FULLSCREEN -> WINDOWED -> cursor capture -> cursor release -> shutdown`. It is a pure timeline test and does not claim native window behavior.

Verify the demo classes and application task are available:

```powershell
.\gradlew.bat :game-sandbox:classes
.\gradlew.bat :game-sandbox:tasks --group application
```

On an interactive Windows x64 desktop, the owner-facing manual run is:

```powershell
.\gradlew.bat :game-sandbox:runEngineDemo
```

The default run is roughly 38 seconds. It prints the timeline before startup, opens the public production `GlfwWindow`, polls events, reports logical/framebuffer dimensions, exercises the already-implemented window modes, enables cursor capture during an explicit Alt+Tab observation window, releases capture, returns to windowed mode, and performs orderly stop/close plus `NativeResourceRegistry.assertNoOpenResources()`.

The current once-per-second sandbox diagnostic also includes the P3-T06 public snapshot frame ID, focus/capture state, W/A/S/D held state, and accumulated public snapshot mouse delta since the prior diagnostic. These values remain **diagnostics**, not FPS, rendering benchmark, performance acceptance, soak evidence, or proof of leak freedom. The current window is intentionally visually empty until renderer work provides a public production presentation path; do not add direct OpenGL/LWJGL calls merely to make the demo look richer.

P3-T04A uses `compileOnly(project(":engine-platform-lwjgl"))` for demo source compilation plus a dedicated resolvable/non-consumable `engineDemoRuntime` used only by `runEngineDemo`. The platform dependency must not be published through `game-sandbox` runtime elements because `game-server` consumes `game-sandbox`. Therefore every sandbox verification must include:

```powershell
.\gradlew.bat :game-server:verifyHeadlessServerRuntime
```

That command must continue to prove that `game-server` runtime contains no `engine-platform-lwjgl`, renderer/audio modules, GLFW, OpenGL, or OpenAL artifacts. If the custom demo configuration requires a lockfile update, regenerate only through the normal dependency-lock workflow, inspect the exact `game-sandbox/gradle.lockfile` diff, and then require ordinary `resolveAndLockAllDependencies` to be clean.

The manual sandbox run is not required to execute inside unattended CI because it is human-observation tooling. The aggregate `test`, `buildAllModules`, architecture gate, coverage gate, headless-server boundary, and all existing native P3 acceptance remain authoritative automated verification. Current merge/closure mechanics follow the current CI gate below.

Future tasks must evaluate sandbox impact under `AGENTS.md`. A human-observable capability that can be demonstrated through already-authorized public production APIs updates `game-sandbox` in the same PR. If the necessary public boundary does not exist, record `Sandbox impact: none — <reason>` instead of exposing internals or implementing later roadmap work.

## Checkstyle boundary

For P1-T05 / Issue #35, the deliberate negative fixture is opt-in and must fail the root check task:

```powershell
.\gradlew.bat check -PcheckstyleIncludeInvalidFixture=true
```

The normal valid command is:

```powershell
.\gradlew.bat check
```

After P1-T10A, the root Checkstyle scan deliberately excludes `feasibility-spikes/src/main/java/**`, which remains Phase 0 experimental evidence. `verifyCheckstyleSourceBoundary` requires the moved spike source set to exist and fails if those sources leak into the production scan. The root has no Java source tree of its own.

## JaCoCo

For P1-T06 / Issue #36, run:

```powershell
.\gradlew.bat verifyJacocoReports
```

JaCoCo is configured for the engine modules selected by the root coverage task. Each configured module writes:

- XML: `<module>/build/reports/jacoco/test/jacocoTestReport.xml`
- HTML: `<module>/build/reports/jacoco/test/html/index.html`

`verifyJacocoReports` fails when either format is missing for any configured module. Coverage is reported for visibility only; no global or per-module minimum percentage is defined yet.

## Architecture boundaries

The D-016 suite now lives in `test-support` so the root can remain source-free. Run:

```powershell
.\gradlew.bat :test-support:test --tests "com.samo.architecture.ModulePackageBoundaryTest" --rerun-tasks
```

The `test-support` test task supplies `repository.root` and the sorted names from `rootProject.subprojects`. The suite requires an exact registry triple for every supplied project, requires each production Java file to declare an owned package, and parses imports plus fully qualified references from both main and test Java trees. Test source packages themselves are not ownership-checked because the shared Phase 1 smoke tests intentionally use `com.samo.testing`; their references are still checked.

The parser is the Java 25 JDK compiler-tree API, not an added dependency. The gate is source-level only: compiled bytecode, reflective names in strings/resources, and generated sources outside `src/main/java` or `src/test/java` are not covered. Regression tests in the same suite prove rejection of a fully qualified internal reference and a package-less production source, and prove most-specific ownership for the overlapping network roots.

The deliberate negative fixture at `config/architecture/fixtures/ForbiddenGameToPlatformShortcut.java` remains disabled by default. To prove the representative `game-client -> engine-platform-lwjgl.internal` shortcut is rejected:

```powershell
$env:JAVA_TOOL_OPTIONS="-Darchitecture.includeInvalidFixture=true"
.\gradlew.bat :test-support:test --tests "com.samo.architecture.ModulePackageBoundaryTest" --rerun-tasks
Remove-Item Env:JAVA_TOOL_OPTIONS
```

That negative run must fail. After clearing `JAVA_TOOL_OPTIONS`, rerun the valid command and require success.

The registry `config/architecture/module-boundaries.properties` contains one `.root`, `.api`, and `.internal` declaration for every declared Gradle subproject. P1-T10A extends it from the 16 production-target subprojects to include experimental `feasibility-spikes`; this does not add a production runtime module.

## Client/server entry points

For P1-T09 / Issue #39:

```powershell
.\gradlew.bat :game-client:runClient
.\gradlew.bat :game-server:runServer
.\gradlew.bat :game-server:verifyHeadlessServerRuntime
```

The current entry points intentionally do not initialize later production subsystems. The headless verification inspects the resolved `game-server` runtime classpath and fails if `engine-platform-lwjgl`, `engine-render-opengl`, `engine-audio-openal`, `lwjgl-glfw`, `lwjgl-opengl`, or `lwjgl-openal` appears. P3-T04A's sandbox demo-only platform configuration must remain non-consumable/non-exported so this command continues to pass.

For P1-T10 / Issue #40, run both reports from the same checkout/build:

```powershell
.\gradlew.bat :game-client:runClient --args="--version"
.\gradlew.bat :game-server:runServer --args="--version"
```

Both reports must contain non-empty `engineCommit`, `protocolVersion`, `assetVersion`, `javaVersion`, and `nativeLibraries`. Shared identifiers must match; CI additionally requires `engineCommit` to equal the exact workflow SHA.

## P1-T10A isolation verification

P1-T10A / Issue #56 changes dependency/source ownership, not spike behavior. Verify:

```powershell
.\gradlew.bat projects
.\gradlew.bat resolveAndLockAllDependencies --write-locks
.\gradlew.bat buildAllModules
.\gradlew.bat test
.\gradlew.bat :test-support:test --tests "com.samo.architecture.ModulePackageBoundaryTest" --rerun-tasks
.\gradlew.bat verifyJacocoReports
.\gradlew.bat :game-client:runClient
.\gradlew.bat :game-server:runServer
.\gradlew.bat :game-server:verifyHeadlessServerRuntime
.\gradlew.bat :game-client:runClient --args="--version"
.\gradlew.bat :game-server:runServer --args="--version"
```

Review `gradle.lockfile` and `feasibility-spikes/gradle.lockfile` after `--write-locks`. The root lock must no longer own LWJGL/Jolt/Snaploader/OSHI/Steamworks runtime dependencies; those belong to the experimental subproject. Existing engine/game lockfiles should not change solely because the spikes moved.

Also confirm no Java source remains under root `src/`, and no production Gradle project depends on `:feasibility-spikes`.

## General change verification

First audit the complete changed-file set. If every changed path ends in `.md`, the change qualifies for the Markdown-only CI exemption: do not run the Gradle build/test matrix solely for that change, and do not require automatic PR-head or merged-`master` build/test CI. Instead, verify the requested documentation content, links/references that matter to the task, consistency with authoritative repository state, and the complete diff audit. Record that no CI run was required by policy; do not call the absence of a run a pass.

If any non-Markdown path is present, the exemption does not apply. For a general documentation/build-boundary pull request that contains any non-Markdown file, the minimum clean verification is:

```powershell
.\gradlew.bat projects
.\gradlew.bat buildAllModules
.\gradlew.bat test
```

Do not use Gradle task counts as durable evidence; counts change when modules/plugins/tasks change.

## Phase integration and next-phase readiness

The phase exit gates in [TECHNICAL_BACKLOG.md](roadmap/TECHNICAL_BACKLOG.md) and milestone outcomes in [ROADMAP.md](../ROADMAP.md) remain the acceptance sources. This procedure adds evidence discipline, not new feature requirements or numerical thresholds. It applies to every phase, including a headless/test-only phase; a visual demo is not universally required.

Before claiming a phase is complete:

1. In the bounded phase-exit Issue/PR, link the applicable existing gate and identify every part of it that must be demonstrated. If required behavior is missing, leave the gate open and identify the bounded follow-up instead of weakening acceptance.
2. Describe one repeatable scenario (or the minimum scenarios needed) through the actual participating systems and public boundaries. Use the sandbox when suitable and available, or a headless/test harness for nonvisual behavior. Mock-only subsystem tests do not demonstrate real integration.
3. Record exact executable commands or manual steps, inputs/assets/seeds, tested commit, environment, original duration/impairment thresholds where specified, expected behavior, and observed results. Add commands to this file when implemented; do not publish hypothetical commands as runnable.
4. Retain relevant logs, reports, traces, captures, and cleanup observations. State what was not exercised. Routine CI, a screenshot, or task checkmarks alone cannot replace an unexecuted integration/duration gate.
5. Record pass/fail and remaining blockers in the Issue/PR; update `DEVELOPMENT_STATUS.md` with the durable conclusion and evidence links. Do not mark the phase complete while part of its exit gate remains unproven.
6. Review the next phase against the demonstrated behavior: are its assumptions and dependencies satisfied, are proposed abstractions needed by its current use cases, and do its acceptance criteria still describe the required outcome? Record the next bounded task and any refinements in the closing Issue/PR. Update backlog definitions and affected executable Issues only when an authorized refinement is needed; never silently change locked scope or decisions.

For the completed P2 phase, D-030 / Issue #135 defined the gate as one headless loop running deterministic fixed 60 Hz simulation ticks for at least 60 continuous seconds with bounded catch-up and verified cleanup. The gate evidence must show those properties together; isolated P2-T01 through P2-T13 suites do not satisfy it. The explicit command and retained report are defined in the P2 integrated exit-gate section above.

For comparison, P3 requires replaying an identical input sequence into headless simulation, while P4 requires spatial tests independent of OpenGL/Jolt. Use those actual gate forms rather than requiring a rendered demo for every phase. Later phases retain their own scene, multiplayer, tooling, and release criteria from the backlog.

During a phase, add a small integration exercise within a task's authorized scope as soon as meaningful behavior is available. If it requires another task's implementation, record the missing dependency and keep the work bounded. Keep findings and review provenance in existing Issues/PRs and the current handoff documents; no parallel management document is required.

## CI gate

The normal non-Markdown lifecycle is intentionally split into one heavy final-candidate gate and one lightweight exact-merge verifier. This is the current policy; `docs/CI_LIFECYCLE.md` gives the concise process view and `AGENTS.md` is the higher-authority agent contract.

`.github/workflows/java25.yml` is triggered for pull requests targeting `master`, pushes to `master`, and `workflow_dispatch`, subject to the existing Markdown-only `paths-ignore`. Job conditions determine which path runs:

- a non-draft PR final candidate runs the heavy five-job matrix;
- `workflow_dispatch` runs the heavy five-job matrix as an explicit escape hatch when a task/release/phase gate needs a deliberate full rerun;
- an ordinary push to `master` runs `Lightweight master verification` only, while the five heavy jobs are skipped;
- a draft PR may create a workflow record, but the heavy jobs remain skipped until the PR is ready/non-draft.

A pull request is Markdown-only only when the complete PR changed-file set is non-empty and every path ends in `.md`. Audit the complete file list before applying the exemption. If any non-Markdown path is present, or a later commit adds one, the normal CI contract applies immediately. The exemption changes only build/runtime execution requirements; it does not waive Issue scope, truth hierarchy, review/architecture rules, documentation consistency, branch/PR discipline, sandbox-impact evaluation, or explicit manual verification from the active Issue.

For qualifying Markdown-only pull requests, no automatic heavy PR workflow or post-merge verifier is required solely for that Markdown-only change. Record the complete-diff audit and policy exemption instead; absence of CI is neither a pass nor a failure.

For non-exempt work, finish implementation, focused verification available in the authoring environment, required docs/wiki/sandbox evaluation, self-review, and consistency audit on the task branch **before opening the normal final non-draft PR**. Do not use an open PR as a development scratchpad unless early human/reviewer visibility is specifically useful. This avoids spending heavy runner time on intermediate commits.

Before interpreting or merging a non-exempt final candidate:

1. Read the current PR head SHA and base `master` SHA.
2. Match the heavy workflow run to that exact current candidate. Superseded older-head runs are obsolete evidence.
3. Require all five heavy jobs to complete successfully on the final candidate.
4. If the candidate changes after a pass, the previous pass is obsolete and the changed candidate must run heavy CI again.
5. If `master` advances relative to the tested base before merge, refresh/rebase the branch as required by the active contract and reverify the resulting candidate; do not assume stale-base evidence transfers.
6. Do not add avoidable cosmetic/status/docs commits after the final candidate passes. Required handoff/docs should already be in that candidate.
7. Merge only the tested current head/base candidate.
8. After merge, require `Lightweight master verification` to pass on the exact resulting `master` merge SHA before closing the Issue.
9. Do not rerun the routine heavy matrix after merge. Use `workflow_dispatch` or a task-specific exact-merge command only when the active Issue explicitly requires native/performance/protocol evidence that the lightweight verifier cannot establish.

The heavy five-job PR/manual matrix covers:

- `Build and quality gates`: Java/toolchain reporting, project inventory, committed dependency-lock resolution, all-module build/quality gates, client/server entry points, headless-server runtime boundary, and client/server version compatibility;
- `Unit tests`: root/subproject aggregation plus the focused engine-core evidence suites and retained reports; P3-T07's `InputActionBindingsTest` is also included by the ordinary platform-module test task;
- `Architecture tests`: explicit package/module boundary enforcement;
- `JaCoCo coverage reports`: tests plus XML/HTML coverage generation/upload;
- `Windows native smoke`: historical GLFW/OpenAL/Jolt smoke plus the current bounded production P3 native acceptance sequence, including P3-T05 raw-motion mode/focus evidence when present.

The lightweight `master` verifier is deliberately narrower. On the exact pushed `master` SHA it checks out that commit, sets up Java 25/Gradle, resolves committed dependency locks and fails on drift, verifies the headless server runtime boundary, runs client/server `--version`, requires both reported `engineCommit` values to equal the exact workflow SHA, and requires shared compatibility identifiers to match. It proves exact-merge identity and critical runtime-boundary/version wiring without repeating unit, coverage, architecture, and native suites that already passed on the unchanged final candidate.

The workflow uses top-level concurrency with `cancel-in-progress: true`, keyed by workflow/PR for pull requests and by ref for push/manual runs. A newer commit on the same PR supersedes older queued/in-progress candidate runs; stale cancelled runs neither pass nor fail the current candidate. Never cancel the current final-candidate run merely to save runner time.

A self-hosted run is not an ephemeral clean VM. `actions/checkout` checks out the requested commit into the runner work directory, but machine-level software and caches can persist. The committed Gradle Wrapper, Java 25 setup, dependency locks, explicit task outputs, and repository tests remain the verification contracts; do not infer reproducibility merely from machine state.

Whether GitHub itself blocks a merge is controlled separately by live branch-protection/ruleset settings. Regardless of platform enforcement, `AGENTS.md` forbids merging non-exempt work without the required exact-candidate heavy pass and forbids closing the Issue until the exact-merge lightweight verifier passes.

This CI contract must not be interpreted as P0-T13 sustained-stability evidence, P0-T14 repeated-lifecycle evidence, or P0-T09A end-to-end Steam transport evidence. Steam-dependent checks remain outside unattended CI because they require an authenticated Steam client/account environment.

## Phase 0 feasibility commands

P1-T10A preserves the historical root command names. Each root task below is a compatibility alias to the identically named task in `:feasibility-spikes`; the source/dependencies now live only in that experimental module.

| Task | Command | Classification / output |
| --- | --- | --- |
| P0-T03 OpenGL | `.\gradlew.bat runOpenGL46Spike` | Interactive native feasibility run. |
| P0-T04 Jolt | `.\gradlew.bat runJoltLifecycleSpike` | Native lifecycle/cleanup spike. |
| P0-T05 OpenAL | `.\gradlew.bat runOpenAL3DAudioSpike` | Native audio lifecycle spike. |
| P0-T06 UDP server/client | `.\gradlew.bat runUdpSpikeServer` / `.\gradlew.bat runUdpSpikeClient` | Localhost UDP feasibility pair. |
| P0-T11 impairment | `.\gradlew.bat runNetworkImpairmentHarness` | Deterministic latency/jitter/loss/duplication/reordering suite. |
| P0-T07 Steam init | `.\gradlew.bat runSteamInitSpike` | Requires running Steam client and native access. |
| P0-T09 FFM access | `.\gradlew.bat runSteamFlatApiFfmSpike` | Proves flat-API access, not end-to-end transport. |
| P0-T12 integrated smoke | `.\gradlew.bat runIntegratedNativeSmoke` | 15 seconds; JFR remains at `build/spikes/native-evidence/p0-t12-smoke.jfr`. |
| P0-T13 sustained test | `.\gradlew.bat runIntegratedNativeSoak` | 900 seconds; JFR remains at `build/spikes/native-evidence/p0-t13-soak.jfr`; pending Issue #43. |

The mode-specific impairment aliases (`runNetworkImpairmentLatency`, `Jitter`, `Loss`, `Duplication`, `Reordering`) are also preserved. Steam working files remain under root `build/spikes/steam` so existing evidence paths are not silently changed by the module move.

Read the matching document under `docs/feasibility/` before interpreting a result. A short smoke pass is not long-duration stability evidence.

## Evidence record

For every pull request, list each executed command and result, or for a qualifying Markdown-only change record the complete changed-file audit and that build/test CI was not required by policy. For native/performance/protocol work also record:

- OS, JDK, relevant GPU/driver/native-library versions;
- configured duration, iteration count, or impairment parameters;
- pass/fail criteria and observed result;
- JFR/log/capture path;
- cleanup/leak observations;
- checks skipped because the environment could not support them.

For sandbox/demo work, record automated test/build/headless-boundary results separately from manual human observation. A locally viewed window or console trace is useful owner feedback, not a substitute for final-candidate CI or a performance claim.

Configuration review is not runtime evidence. If a command was not run, write `not run` and why; a Markdown-only policy exemption is a reason, not a passing execution result.

## Dependency changes

After an authorized dependency/version or dependency-ownership change:

```powershell
.\gradlew.bat resolveAndLockAllDependencies --write-locks
.\gradlew.bat buildAllModules
```

Review every changed lockfile. P1-T10A relocates existing dependencies without changing their selected versions. P3-T01 likewise places the already-selected LWJGL 3.4.3 core/GLFW/OpenGL dependencies and Windows natives into `engine-platform-lwjgl`; only that module's ownership-related lock change is expected for P3-T01, and no version-catalog change is authorized. P3-T02, P3-T03, P3-T04, P3-T05, and P3-T06 add no dependency or dependency-ownership change. P3-T04A adds only a non-exported existing-project dependency for the sandbox demo (`compileOnly` plus dedicated non-consumable `engineDemoRuntime`); it must not alter selected library versions or the server runtime. P3-T07 adds the first production Jackson JSON parser dependency authorized by D-037: `jackson-databind:2.21.2` as `implementation` of `engine-platform-lwjgl`, resolving `jackson-core:2.21.2` and `jackson-annotations:2.21`; inspect the version-catalog and platform lock changes explicitly and require no unrelated lock drift. Update `game-sandbox/gradle.lockfile` only if Gradle's lock resolution for its dedicated configuration actually requires it, and inspect that diff explicitly.

## Wiki/API-guide verification

The [`../wiki/`](../wiki/README.md) directory is the consumer-facing guide for already implemented public engine APIs. It does not replace runtime verification, tests, accepted decisions, or the active Issue.

Whenever a task changes public engine API or consumer-visible lifecycle, ownership, threading, failure, configuration, or usage behavior, verification/handoff must also confirm that:

1. the relevant wiki page uses the actual production signatures and behavior;
2. practical examples do not call planned/nonexistent APIs;
3. `wiki/API_INDEX.md` reflects public surface changes;
4. `wiki/LIMITATIONS.md` no longer claims newly implemented behavior is unavailable, and does not claim future behavior exists;
5. relative wiki links resolve by repository path inspection;
6. the PR records `Wiki impact: updated <pages>` or, when truly unaffected, `Wiki impact: none — <reason>`.

Wiki consistency is documentation verification, not evidence that production code works. For a qualifying Markdown-only wiki/docs task, apply the existing Markdown-only CI exemption only after auditing the complete changed-file set; no Gradle execution is required solely to prove prose/link synchronization.

## Sandbox/demo verification

`game-sandbox` is the owner-facing observation path for already implemented engine behavior, not a specification or acceptance authority. For every implementation task, handoff must record either:

- `Sandbox impact: updated <demo/files>` when the new capability is demonstrable through already-authorized public production APIs; or
- `Sandbox impact: none — <reason>` when a meaningful demo would require exposing internals, calling native libraries directly, or implementing a future roadmap task.

Never weaken a production boundary or add a public API solely to satisfy the demo. `game-sandbox/README.md` owns the current manual run instructions and limitations; this file owns the distinction between manual observation and verification evidence.
