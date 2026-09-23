# Sherko Engine Build and Verification

This file centralizes repeatable commands and the evidence expected from them. Run Windows commands on Windows x64 when native libraries are involved. All current CI jobs use GitHub-hosted Windows. The native acceptance job provisions a pinned Mesa software OpenGL stack inside its ephemeral runner before executing the unchanged Windows/WGL suites. The current CI lifecycle is defined by `AGENTS.md`, `docs/CI_LIFECYCLE.md`, and the current `## CI gate` section below; older task-specific sections retain historical evidence language where useful.

## Environment baseline

- JDK: Temurin/OpenJDK 25 through the Gradle toolchain.
- Build: repository Gradle Wrapper.
- CI runner: GitHub-hosted `windows-latest` for all heavy jobs and exact-master verification.
- Native graphics setup: `Windows native smoke` downloads the pinned Mesa 26.1.8 Windows MSVC archive, verifies its SHA-256, installs its WGL/OpenGL DLLs into the ephemeral runner, and selects llvmpipe software rendering before executing native acceptance.
- Native evidence scope: hosted Mesa proves the exercised Windows/WGL/OpenGL correctness and lifecycle paths when the unchanged suites pass; it is not evidence of physical-GPU performance, a vendor GPU driver, or minimum hardware.
- Runtime target: Windows x64.

Use `./gradlew` on Unix-like shells for non-native configuration checks and `.\gradlew.bat` on Windows.

## Routine verification matrix

| Purpose | Windows command | Expected evidence |
| --- | --- | --- |
| Show declared projects | `.\gradlew.bat projects` | 17 Gradle subprojects appear: the 16 production-target modules plus experimental `feasibility-spikes`. |
| Compile/test every module and run the root quality gate | `.\gradlew.bat buildAllModules` | Root `check` and every subproject `build` complete. |
| Run the root quality gate | `.\gradlew.bat check` | Checkstyle, architecture-boundary verification through `test-support`, and the Phase 0 source-exclusion boundary pass. |
| Run root and subproject tests | `.\gradlew.bat test` | Aggregate JUnit Platform test tasks pass, including `test-support` architecture tests and current `game-sandbox` deterministic control tests. |
| Run only the architecture boundary suite | `.\gradlew.bat :test-support:test --tests "com.samo.architecture.ModulePackageBoundaryTest" --rerun-tasks` | The Gradle-derived subproject registry, production package ownership, main/test imports, fully qualified references, and boundary regressions pass. |
| Generate and verify JaCoCo reports | `.\gradlew.bat verifyJacocoReports` | Tests run for all configured test-bearing engine modules and each produces XML plus HTML coverage reports. |
| Run client foundation entry point | `.\gradlew.bat :game-client:runClient` | Client foundation process starts and exits cleanly. |
| Run headless server foundation entry point | `.\gradlew.bat :game-server:runServer` | Server foundation process starts in headless mode and exits cleanly. |
| Report client version metadata | `.\gradlew.bat :game-client:runClient --args="--version"` | Client reports executable, engine commit, protocol version, asset version, Java version, and native libraries available on its runtime classpath. |
| Report server version metadata | `.\gradlew.bat :game-server:runServer --args="--version"` | Server reports the same shared identifiers plus its executable-specific native-library list. |
| Verify headless server dependency boundary | `.\gradlew.bat :game-server:verifyHeadlessServerRuntime` | Server runtime classpath contains no platform/render/audio projects or GLFW/OpenGL/OpenAL artifacts, including after persistent sandbox dependencies are resolved. |
| Run persistent owner-facing sandbox | `.\gradlew.bat :game-sandbox:runSandbox` | Interactive playground opens on an interactive Windows desktop and remains owner-controlled until `Ctrl+Q`; this is not FPS, benchmark, soak, leak-proof, replay, or CI acceptance evidence. |
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

Issue #73 adds stateless `SubsystemStartupCoordinator` coordination for an already resolved dependency-first order. Run the full routine matrix above and the combined focused suite:

```powershell
.\gradlew.bat :engine-core:test --tests "com.samo.engine.core.api.EngineSubsystemTest" --tests "com.samo.engine.core.api.SubsystemGraphTest" --tests "com.samo.engine.core.api.SubsystemStartupCoordinatorTest" --rerun-tasks
```

The rollback suite uses handwritten global hook traces to verify per-subsystem initialize/start activation, close of the currently failing subsystem, reverse stop/close of previously started subsystems, first-element isolation, caller-owned successful shutdown, input snapshotting, original throwable identity, continued cleanup after rollback failures, deterministic suppressed-failure order, and self-suppression avoidance.

Startup XML: `engine-core/build/test-results/test/TEST-com.samo.engine.core.api.SubsystemStartupCoordinatorTest.xml`. HTML remains `engine-core/build/reports/tests/test/index.html`. The `engine-subsystem-tests` artifact includes lifecycle, graph, and startup XML/HTML; `jacoco-reports` remains unfiltered.

These tests use synthetic Java subsystems. They do not establish native restartability, native leak freedom, sustained stability, or D-030's 60-second integrated Phase 2 exit gate. No dependency or lockfile change is expected.

## P2-T04 EngineClock verification

Issue #74 adds `EngineClock` as an elapsed-nanosecond sampler only. Run the full routine matrix above and the focused acceptance suite:

```powershell
.\gradlew.bat :engine-core:test --tests "com.samo.engine.core.api.EngineClockTest" --rerun-tasks
```

The clock suite uses deterministic injected readings and handwritten expectations for first-sample zero, incremental positive deltas, equal readings, negative absolute source values, forward signed-`long` wraparound, regression rejection without baseline mutation, null source, source `RuntimeException`/`Error` identity with baseline preservation, one source read per sample attempt, and default-constructor baseline establishment. It does not sleep or infer expected values from production output.

Also rerun the prior lifecycle/graph/startup suite to guard existing `engine-core` behavior:

```powershell
.\gradlew.bat :engine-core:test --tests "com.samo.engine.core.api.EngineSubsystemTest" --tests "com.samo.engine.core.api.SubsystemGraphTest" --tests "com.samo.engine.core.api.SubsystemStartupCoordinatorTest" --rerun-tasks
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
.\gradlew.bat :engine-core:test --tests "com.samo.engine.core.api.EngineSubsystemTest" --tests "com.samo.engine.core.api.SubsystemGraphTest" --tests "com.samo.engine.core.api.SubsystemStartupCoordinatorTest" --tests "com.samo.engine.core.api.EngineClockTest" --tests "com.samo.engine.core.api.FixedStepAccumulatorTest" --rerun-tasks
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
.\gradlew.bat :engine-core:test --tests "com.samo.engine.core.api.EngineSubsystemTest" --tests "com.samo.engine.core.api.SubsystemGraphTest" --tests "com.samo.engine.core.api.SubsystemStartupCoordinatorTest" --tests "com.samo.engine.core.api.EngineClockTest" --tests "com.samo.engine.core.api.FixedStepAccumulatorTest" --tests "com.samo.engine.core.api.FixedStepCatchUpPolicyTest" --rerun-tasks
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
.\gradlew.bat :engine-core:test --tests "com.samo.engine.core.api.EngineSubsystemTest" --tests "com.samo.engine.core.api.SubsystemGraphTest" --tests "com.samo.engine.core.api.SubsystemStartupCoordinatorTest" --tests "com.samo.engine.core.api.EngineClockTest" --tests "com.samo.engine.core.api.FixedStepAccumulatorTest" --tests "com.samo.engine.core.api.FixedStepCatchUpPolicyTest" --tests "com.samo.engine.core.api.FixedStepInterpolationTest" --rerun-tasks
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
.\gradlew.bat :engine-core:test --tests "com.samo.engine.core.api.EngineSubsystemTest" --tests "com.samo.engine.core.api.SubsystemGraphTest" --tests "com.samo.engine.core.api.SubsystemStartupCoordinatorTest" --tests "com.samo.engine.core.api.EngineClockTest" --tests "com.samo.engine.core.api.FixedStepAccumulatorTest" --tests "com.samo.engine.core.api.FixedStepCatchUpPolicyTest" --tests "com.samo.engine.core.api.FixedStepInterpolationTest" --tests "com.samo.engine.core.api.EngineConfigSchemaTest" --rerun-tasks
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
.\gradlew.bat :engine-core:test --tests "com.samo.engine.core.api.EngineSubsystemTest" --tests "com.samo.engine.core.api.SubsystemGraphTest" --tests "com.samo.engine.core.api.SubsystemStartupCoordinatorTest" --tests "com.samo.engine.core.api.EngineClockTest" --tests "com.samo.engine.core.api.FixedStepAccumulatorTest" --tests "com.samo.engine.core.api.FixedStepCatchUpPolicyTest" --tests "com.samo.engine.core.api.FixedStepInterpolationTest" --tests "com.samo.engine.core.api.EngineConfigSchemaTest" --tests "com.samo.engine.core.api.EngineConfigLoaderTest" --rerun-tasks
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
.\gradlew.bat :engine-core:test --tests "com.samo.engine.core.api.EngineSubsystemTest" --tests "com.samo.engine.core.api.SubsystemGraphTest" --tests "com.samo.engine.core.api.SubsystemStartupCoordinatorTest" --tests "com.samo.engine.core.api.EngineClockTest" --tests "com.samo.engine.core.api.FixedStepAccumulatorTest" --tests "com.samo.engine.core.api.FixedStepCatchUpPolicyTest" --tests "com.samo.engine.core.api.FixedStepInterpolationTest" --tests "com.samo.engine.core.api.EngineConfigSchemaTest" --tests "com.samo.engine.core.api.EngineConfigLoaderTest" --tests "com.samo.engine.core.api.NativeResourceRegistryTest" --rerun-tasks
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
.\gradlew.bat :engine-core:test --tests "com.samo.engine.core.api.EngineSubsystemTest" --tests "com.samo.engine.core.api.SubsystemGraphTest" --tests "com.samo.engine.core.api.SubsystemStartupCoordinatorTest" --tests "com.samo.engine.core.api.EngineClockTest" --tests "com.samo.engine.core.api.FixedStepAccumulatorTest" --tests "com.samo.engine.core.api.FixedStepCatchUpPolicyTest" --tests "com.samo.engine.core.api.FixedStepInterpolationTest" --tests "com.samo.engine.core.api.EngineConfigSchemaTest" --tests "com.samo.engine.core.api.EngineConfigLoaderTest" --tests "com.samo.engine.core.api.NativeResourceRegistryTest" --tests "com.samo.engine.core.api.AllocationMetricBenchmarkTest" --tests "com.samo.engine.core.api.EngineLoggerTest" --rerun-tasks
```

Logger XML: `engine-core/build/test-results/test/TEST-com.samo.engine.core.api.EngineLoggerTest.xml`. CI includes it in the existing `engine-subsystem-tests` evidence artifact; `jacoco-reports` remains unfiltered. P2-T12 does not create a file/console log artifact because it defines no persisted log format.

P2-T12 proves the typed in-memory event/sink contract only. It does not establish threshold routing, persistence, rotation, asynchronous logging, a global singleton, concrete runtime call sites, or fatal process termination. P2-T13 owns orderly fatal assertion/shutdown behavior. No dependency or lockfile change is expected.

## P2-T13 orderly fatal-termination verification

Issue #83 adds the JDK-only `FatalTerminationCoordinator` coordinator in `engine-core`. It composes the existing subsystem lifecycle, native-resource registry, and structured logger without changing their APIs, adding dependencies, or selecting a persisted log format.

Run the focused acceptance suite:

```powershell
.\gradlew.bat :engine-core:test --tests "com.samo.engine.core.api.FatalTerminationCoordinatorTest" --rerun-tasks
```

The suite uses handwritten lifecycle/log traces, counted synthetic resource closers, failing sinks, and an injected package-private terminator seam to verify the exact fatal sequence, reverse subsystem cleanup, stop-then-close behavior after stop failure, non-force-closing registry verification, ordered cleanup-failure diagnostics, flush ordering, unchecked logging/cleanup/verification/flush failure containment, exact terminator throwable identity with suppressed failures, returning-terminator rejection, and one-shot reentrant/concurrent/later-call rejection.

The same focused JUnit suite launches a bounded child JVM for the real public constructor. The child calls production `System.exit(1)` only after recording FATAL receipt, subsystem stop, subsystem close/resource release, and logger flush. Do not invoke the public fatal path directly from the Gradle/JUnit process outside that child harness.

Also rerun the complete current focused `engine-core` regression set together:

```powershell
.\gradlew.bat :engine-core:test --tests "com.samo.engine.core.api.EngineSubsystemTest" --tests "com.samo.engine.core.api.SubsystemGraphTest" --tests "com.samo.engine.core.api.SubsystemStartupCoordinatorTest" --tests "com.samo.engine.core.api.EngineClockTest" --tests "com.samo.engine.core.api.FixedStepAccumulatorTest" --tests "com.samo.engine.core.api.FixedStepCatchUpPolicyTest" --tests "com.samo.engine.core.api.FixedStepInterpolationTest" --tests "com.samo.engine.core.api.EngineConfigSchemaTest" --tests "com.samo.engine.core.api.EngineConfigLoaderTest" --tests "com.samo.engine.core.api.NativeResourceRegistryTest" --tests "com.samo.engine.core.api.AllocationMetricBenchmarkTest" --tests "com.samo.engine.core.api.EngineLoggerTest" --tests "com.samo.engine.core.api.FatalTerminationCoordinatorTest" --rerun-tasks
```

Fatal-shutdown XML: `engine-core/build/test-results/test/TEST-com.samo.engine.core.api.FatalTerminationCoordinatorTest.xml`. CI includes it in the existing `engine-subsystem-tests` evidence artifact; the child marker file remains temporary test evidence and is not a production or retained log format. `jacoco-reports` remains unfiltered.

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
- start a synthetic subsystem through `SubsystemStartupCoordinator`, then stop and close it orderly;
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

## P5R-T03 GLFW native backend decomposition verification

Issue #263 refactors only the production platform native seam: public `GlfwWindow` remains the facade, while package-private `GlfwNativeBackend`, `LwjglGlfwNativeBackend`, and callback registration/sink types own the extracted backend/native plumbing.

Run the complete deterministic platform suite:

```powershell
.\gradlew.bat :engine-platform-lwjgl:test --rerun-tasks
```

Run the architecture boundary regression:

```powershell
.\gradlew.bat :test-support:test --tests "com.samo.architecture.ModulePackageBoundaryTest" --rerun-tasks
```

The deterministic fake-backend tests must preserve their existing scenarios/assertions; only the package-private seam names change. Repository/source review must also confirm that the public `GlfwWindow` declarations are unchanged, no extracted backend/helper is `public`, and no stale nested `GlfwWindow.Backend` or callback-seam references remain in the T03 branch.

The task is non-Markdown. The exact final PR head therefore requires the normal five-job heavy CI matrix, including the existing hosted-Windows native platform regressions. After merge, the exact merged `master` SHA requires the normal Lightweight verifier before Issue #263 can close. These checks preserve the already accepted P3 window/input/native behavior; T03 adds no new native acceptance scenario.

## P5R-T04 GLFW input/focus/cursor decomposition verification

Issue #264 refactors only the internal input/focus/cursor state behind the unchanged public `GlfwWindow` facade. Package-private `GlfwInputState`, `GlfwMouseMotionTracker`, and `GlfwCursorCaptureController` own the extracted state while P5R-T05 size/window-mode responsibilities remain in the facade.

Run the complete deterministic platform suite:

```powershell
.\gradlew.bat :engine-platform-lwjgl:test --rerun-tasks
```

Run the architecture boundary regression:

```powershell
.\gradlew.bat :test-support:test --tests "com.samo.architecture.ModulePackageBoundaryTest" --rerun-tasks
```

The existing focus, input-snapshot, and mouse-motion scenarios/assertions must remain behaviorally unchanged. Repository/source review must also confirm that the public `GlfwWindow` constructor/method declaration set is unchanged; all three extracted collaborators remain package-private; focus regain still requires explicit recapture; failed raw/cursor cleanup preserves retry state; and `WindowGeometry`, `MonitorTarget`, `TransitionPlan`, logical/framebuffer staging, and window-mode transition code remain in `GlfwWindow` for P5R-T05.

The task adds no new native behavior, dependency, module edge, or public input surface. The exact final PR head therefore requires the normal five-job heavy CI matrix, including the existing P3 focus-loss/raw-motion/input Windows native regressions. After merge, the exact merged `master` SHA requires the normal Lightweight verifier before Issue #264 can close.

## P5R-T05 GLFW window-mode/size decomposition verification

Issue #265 refactors only internal window-mode, restore-geometry, monitor-targeting, transition-planning/application, and deferred size-delivery responsibilities behind the unchanged public `GlfwWindow` facade.

Run the complete deterministic platform suite:

```powershell
.\gradlew.bat :engine-platform-lwjgl:test --rerun-tasks
```

Run the architecture boundary regression:

```powershell
.\gradlew.bat :test-support:test --tests "com.samo.architecture.ModulePackageBoundaryTest" --rerun-tasks
```

The existing P3-T02/P3-T03 deterministic scenarios and assertions must remain behaviorally unchanged. Repository/source review must also confirm that the public `GlfwWindow` constructor/method declaration set is unchanged; `GlfwWindowModeController`, `GlfwDeferredSizeDelivery`, and the extracted GLFW value records are package-private; accepted T04 input/focus/cursor collaborators are unchanged; and no P5R-T06 input-binding work appears.

Behavioral review must preserve: same-mode no-op; original windowed geometry across direct fullscreen switches; fresh geometry after return to windowed; primary-monitor/current-video-mode borderless/exclusive targeting; original transition failure identity plus one rollback attempt and distinct rollback suppression; initial independent logical/framebuffer queries; post-poll logical-before-framebuffer delivery; independent latest-value coalescing; zero framebuffer axes; and negative-dimension rejection before listener delivery.

The task adds no new native behavior, dependency, module edge, public API, monitor-selection policy, or renderer behavior. The exact final PR head therefore requires the normal five-job heavy CI matrix including the existing hosted-Windows P3-T02 size and P3-T03 window-mode native regressions. After merge, the exact merged `master` SHA requires the normal Lightweight verifier before Issue #265 can close.

Accepted P5R-T05 evidence: final PR head `0657c2848f3686828610d22375ab030007ab4c8e` passed all five required jobs in run #454 / `35498045572`; PR #317 merged as `e85ed173372013effd72e57b52c52e6e0e813d28`; exact merged-master Lightweight verification passed in run #455 / `35498247856`.

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

CI keeps deterministic P3-T03 coverage inside ordinary aggregate tests. The Windows native job enables `GlfwWindowModeNativeTest` exactly once after P3-T02 native verification, uploads its JUnit XML plus the report as artifact `p3-t03-window-modes`, then continues the preserved Jolt lifecycle smoke. Current task merge/closure mechanics are governed by the current CI gate below; stale/cancelled superseded PR runs are never passing evidence for the final candidate.

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

CI keeps deterministic P3-T04 coverage in ordinary aggregate tests. The Windows native job enables `GlfwWindowFocusNativeTest` exactly once after P3-T03 native verification, uploads its JUnit XML and report as artifact `p3-t04-focus-loss`, then continues the preserved Jolt smoke. Current task merge/closure mechanics are governed by the current CI gate below.

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

Also run the public-sandbox/headless/dependency checks:

```powershell
.\gradlew.bat :game-sandbox:test --rerun-tasks
.\gradlew.bat :game-server:verifyHeadlessServerRuntime
.\gradlew.bat resolveAndLockAllDependencies
```

No dependency/version/lockfile/module-edge change is expected. The headless check must continue to prove that the sandbox's platform dependency is non-exported and that `game-server` contains no platform/render/audio/GLFW/OpenGL/OpenAL runtime dependency.

P3-T06 adds no new native call, so it does not add a new native-only acceptance test solely for snapshot construction. The exact final PR candidate must still pass the existing `Windows native smoke` job, thereby regressing the real P3-T01 through P3-T05 GLFW/focus/raw ingestion paths on the same candidate. If a future implementation change introduces behavior that cannot truthfully be established by deterministic backend tests plus those existing native paths, refine the active Issue before adding a new native oracle.

Current owner-facing manual observation uses the persistent playground:

```powershell
.\gradlew.bat :game-sandbox:runSandbox
```

The playground captures one public `InputSnapshot` after each poll and emits bounded once-per-second frame/focus/capture/WASD/mouse-delta diagnostics while leaving the owner in control. This is human observation only; it is not renderer, FPS, performance, soak, or acceptance evidence. Historical P3-T06 evidence may reference the former `runEngineDemo` alias. P5R-T17 removes that compatibility task; current manual observation uses only `runSandbox`.

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

Sandbox impact is intentionally none for the historical P3-T07 task itself: configuration metadata/loading was not meaningfully human-observable until P3-T08 evaluated bindings into action values. The current persistent playground consumes the accepted binding/evaluation path; do not retroactively change P3-T07 acceptance.

Before the final PR, run every applicable routine command from the matrix when the authoring environment supports the repository checkout/toolchain. If the current authoring environment cannot execute Gradle, record those commands as not run rather than as passing; final acceptance still requires the exact non-draft PR candidate to pass all five heavy CI jobs on the configured Windows x64 runner.

P3-T07 itself adds no native operation. The existing `Windows native smoke` final-candidate job must still regress P3-T01 through P3-T05 native behavior on the same candidate. After merge, one lightweight exact-merge `master` verifier is required before Issue #90 may close. P3-T08 remains planning-only until then.

## P5R-T06 input-binding loading/parsing/validation decomposition verification

Issue #266 refactors only the package-private implementation behind the accepted P3-T07 action-binding contract. Public APIs, strict JSON schema version 1, error semantics, committed fixture, Jackson dependency/version, module edges, action evaluation, tick-command behavior, wiki usage, and sandbox behavior remain unchanged.

Run the focused deterministic acceptance suite:

```powershell
.\\gradlew.bat :engine-platform-lwjgl:test --tests "com.samo.engine.platform.api.InputActionBindingsTest" --rerun-tasks
```

Run the architecture/headless checks:

```powershell
.\\gradlew.bat :test-support:test --tests "com.samo.architecture.ModulePackageBoundaryTest" --rerun-tasks
.\\gradlew.bat :game-server:verifyHeadlessServerRuntime
.\\gradlew.bat resolveAndLockAllDependencies
```

Require no dependency-lock drift. Repository/source review must confirm the public `InputActionBindings`, `InputBinding`, `InputBindingLoadException`, action/component/value/control declaration set is unchanged; `InputActionBindingsJsonParser` and `InputActionBindingsValidator` are package-private; `InputActionBindingsLoader` is narrowed to file loading; the committed schema-v1 fixture is unchanged; and no P5R-T07+ work appears.

Behavioral review must preserve missing/unreadable/null-path behavior, malformed JSON and duplicate-object-field wrapping, exact schema version/field/control vocabulary, document-level duplicate detection, component/scale validation, complete-action validation, public-constructor exception types/messages, and existing `InputBindingLoadException` path/context/cause meaning.

The task changes Java source, so the exact final PR head requires the normal five-job heavy matrix. After merge, the exact merged `master` SHA requires the normal Lightweight verifier before Issue #266 can close.

Accepted P5R-T06 evidence: final PR head `791488bfcbae9573cebdb5fe5c8318d238b79c0f` passed all five required jobs in run #456 / `35499854090`; PR #319 merged as `96c9a331f2e4a5e1807ce830121d7ff714877316`; exact merged-master Lightweight verification passed in run #457 / `35500042897`.

Wiki impact: none — supported public API/schema/error semantics/usage remain unchanged. Sandbox impact: none — the current playground consumes the same public binding/evaluation path.

## P5R-T09 spatial/math naming audit verification

Issue #269 is an audit-only KEEP decision across engine-core transform, camera, screen-ray, geometry, and quantization vocabulary. `docs/SPATIAL_CONVENTIONS.md` is the controlling semantic reference and must remain byte-unchanged.

Before merging, verify the complete branch diff against `master` contains only `.md` paths; confirm no Java/test/Gradle/workflow/resource/wiki/sandbox path changed; confirm the retained names align with live source, D-041/D-045/D-046/D-047, `docs/refactor/SYMBOL_INVENTORY.md`, and `docs/refactor/BOUNDARY_AUDIT.md`; and confirm P5R-T10 / Issue #270 is next while P6-T01 remains blocked.

If the complete diff remains Markdown-only, the AGENTS.md Markdown-only CI exemption applies. Do not introduce a spatial source change merely to manufacture executable verification for an audit whose accepted result is no code churn.

Wiki impact: none — public names, signatures, spatial semantics, failures, and intended usage remain unchanged. Sandbox impact: none — no engine capability or owner-visible behavior changes.

## P5R-T08 core naming audit verification

Issue #268 is an audit-only KEEP decision across `engine-core` configuration, logging, timing, and native-resource ownership vocabulary. It intentionally introduces no Java, Gradle, workflow, resource, dependency, module-edge, public-API, configuration-format, wiki, or sandbox change.

Before merging, verify the complete branch diff against `master` contains only `.md` paths. Cross-check the retained names against live source, `docs/refactor/SYMBOL_INVENTORY.md`, and `docs/refactor/BOUNDARY_AUDIT.md`; confirm D-021 through D-028 behavior/ownership contracts are unchanged; confirm P5R-T09 / Issue #269 is next and P6-T01 remains blocked.

If the complete diff remains Markdown-only, the AGENTS.md Markdown-only CI exemption applies. Do not introduce a source change solely to manufacture executable verification for a naming audit whose accepted result is no code churn.

Wiki impact: none — public names, signatures, behavior, config/failure semantics, and intended usage remain unchanged. Sandbox impact: none — no capability or owner-visible behavior changes.

## P5R-T07 core lifecycle naming verification

Issue #267 applies the D-066 public type renames `SubsystemStartupCoordinator` and `FatalTerminationCoordinator` only. Run the focused lifecycle suites:

```powershell
.\\gradlew.bat :engine-core:test --tests "com.samo.engine.core.api.EngineSubsystemTest" --tests "com.samo.engine.core.api.SubsystemGraphTest" --tests "com.samo.engine.core.api.SubsystemStartupCoordinatorTest" --tests "com.samo.engine.core.api.FatalTerminationCoordinatorTest" --tests "com.samo.engine.core.api.Phase2IntegratedGateTest" --rerun-tasks
```

Run architecture/headless/dependency checks:

```powershell
.\\gradlew.bat :test-support:test --tests "com.samo.architecture.ModulePackageBoundaryTest" --rerun-tasks
.\\gradlew.bat :game-server:verifyHeadlessServerRuntime
.\\gradlew.bat resolveAndLockAllDependencies
```

Repository review must confirm that the former production/test filenames are gone; no compatibility alias remains; the new coordinator files differ from their accepted predecessors only by the authorized declaring/constructor/type-reference names; existing lifecycle assertions/scenarios remain equivalent; D-020/D-029 historical decision rows remain intact; and the API index plus lifecycle/subsystem-composition/fatal-termination wiki pages use the new public names.

The task changes supported public API names and Java source, so the final candidate requires the five-job heavy CI matrix on its exact head. After merge, the exact merged `master` SHA requires Lightweight verification before Issue #267 can close. Independent review is required by policy for the public rename; if unavailable, record that honestly with remaining risk. Sandbox impact is none because the persistent sandbox does not directly consume either lifecycle coordinator.

Accepted P5R-T07 evidence: final PR head `682f2744d6585c3cefa551fb0264243b8db4ce90` passed all five required jobs in run #458 / `35501201828`; PR #321 merged as `ae2a40943b51f5fbeca554e52b9322a52c08dc2a`; exact merged-master Lightweight verification passed in run #459 / `35501486272`. Independent review was not performed because no separate reviewer/agent was available in this connector-only execution environment; the recorded mitigation is repository-wide reference audit, reverse-name byte-equivalence review, wiki synchronization, and exact-candidate CI.


## P3-T04A historical sandbox-origin verification

Issue #149 originally turned the `game-sandbox` skeleton into the canonical manual owner-observation surface without changing any public engine API. Its original acceptance used a scripted timeline and `EngineDemoTimelineTest`. That historical evidence remains valid for the task as it merged, but **it is not the current sandbox contract**. Issue #165 supersedes the presentation/maintenance model with the persistent cumulative playground described in `AGENTS.md` and `game-sandbox/README.md`.

Historical P3-T04A references to `EngineDemoTimelineTest`, the approximately 38-second run, `engineDemoRuntime`, and `runEngineDemo` describe the original merged implementation only. Do not treat those historical names as current commands or recreate the timed sequence. Current sandbox verification is defined in the next section.

## Persistent sandbox playground verification — Issue #165

Issue #165 replaced the scripted presentation model without adding a new engine public API and made `SandboxMain` / `runSandbox` canonical. P5R-T17 later removes the obsolete `EngineDemoMain` / `runEngineDemo` compatibility delegate after repository-reference verification.

Run the focused pure-Java control suite:

```powershell
.\gradlew.bat :game-sandbox:test --tests "com.samo.game.sandbox.SandboxControlsTest" --rerun-tasks
```

The suite verifies the owner-control mapping independently of native GLFW behavior: plain `F` cycles window-mode intent, plain `R` toggles cursor-capture intent, `Right Shift + F` selects mouse-sensitivity cycling, `Right Shift + R` selects Y inversion, plain `Q` does not exit, either Control + Q selects explicit exit, and independent controls may coexist in one frame.

Verify sandbox classes/tasks and server isolation:

```powershell
.\gradlew.bat :game-sandbox:classes
.\gradlew.bat :game-sandbox:tasks --group application
.\gradlew.bat :game-server:verifyHeadlessServerRuntime
.\gradlew.bat resolveAndLockAllDependencies
```

The canonical interactive owner run is:

```powershell
.\gradlew.bat :game-sandbox:runSandbox
```

It runs until `Ctrl+Q`. Current controls are documented in `game-sandbox/README.md`; owner-visible interaction is manual observation, not automated acceptance. Since P5-T08, the sandbox renders the existing indexed triangle as a neutral-gray sRGB reference through the public `OpenGlRenderer` and presents it through `GlfwWindow.present()`. The sandbox still makes no direct OpenGL/LWJGL calls. Do not add future gameplay camera/controller/UI/world/physics/network features or public APIs solely to make the sandbox richer.

`game-sandbox` uses `compileOnly` for `engine-platform-lwjgl`. Its renderer compile-only dependency explicitly targets `engine-render-opengl` `runtimeElements` with `isTransitive = false` so Gradle/IntelliJ model the in-repository renderer as a module dependency while D-055's default renderer `apiElements` remains API-only for ordinary consumers. The dedicated resolvable/non-consumable `sandboxRuntime` remains the runtime source for `runSandbox` and the legacy alias. These sandbox-only platform/renderer dependencies must not be published through runtime elements consumed by `game-server`; `verifyHeadlessServerRuntime` remains the explicit boundary check.

After P5-T07B / Issue #224, also verify sandbox compile wiring and the original public-renderer boundary together:

```powershell
.\\gradlew.bat :game-sandbox:compileJava --rerun-tasks
.\\gradlew.bat :game-sandbox:check --rerun-tasks
.\\gradlew.bat :engine-render-opengl:verifyPublicApiBoundary --rerun-tasks
.\\gradlew.bat :test-support:test --tests "com.samo.architecture.ModulePackageBoundaryTest" --rerun-tasks
.\\gradlew.bat :game-server:verifyHeadlessServerRuntime
```

Manual IDE verification is separate from CI: reload/sync the Gradle project in IntelliJ and confirm `SandboxMain` resolves `OpenGlRenderer` without adding a manual Project Structure dependency.

No dependency/version change is expected from Issue #165; only the custom configuration/task naming changes. `resolveAndLockAllDependencies` must not introduce unrelated drift. The final candidate is non-Markdown and therefore requires the ordinary exact-head heavy five-job matrix, followed after merge by the lightweight exact-merge master verifier before Issue #165 closes.

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

The current entry points intentionally do not initialize later production subsystems. The headless verification inspects the resolved `game-server` runtime classpath and fails if `engine-platform-lwjgl`, `engine-render-opengl`, `engine-audio-openal`, `lwjgl-glfw`, `lwjgl-opengl`, or `lwjgl-openal` appears. The sandbox's platform configuration must remain non-consumable/non-exported so this command continues to pass.

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

## P5-T01 OpenGL debug callback verification

Issue #183 adds optional production OpenGL debug diagnostics to `GlfwWindow` without adding a renderer draw/resource API. Existing constructors keep debug output disabled. The explicit `OpenGlDebugMode.FAIL_ON_HIGH_SEVERITY` path requests a debug context, installs the owned callback after OpenGL capabilities exist, and surfaces staged high-severity failures from owner-thread `pollEvents()`.

Focused verification:

```powershell
.\gradlew.bat :engine-platform-lwjgl:test --tests "com.samo.engine.platform.api.GlfwWindowTest" --rerun-tasks
```

Real Windows x64 acceptance:

```powershell
$env:SHERKO_P5_T01_NATIVE="true"
.\gradlew.bat :engine-platform-lwjgl:test --tests "com.samo.engine.platform.api.GlfwWindowDebugNativeTest" --rerun-tasks
```

The native test requires an OpenGL 4.6 debug context, issues one intentional invalid `glEnable(-1)` call, verifies the resulting callback is API / ERROR / HIGH severity, requires the owner-thread `pollEvents()` failure policy to fire once, verifies cleanup, and writes:

`engine-platform-lwjgl/build/reports/p5/p5-t01-opengl-debug.txt`

This is bounded debug-callback acceptance only. It is not renderer correctness, performance, soak, or release-hardware evidence.

## P5-T02 OpenGL thread-ownership verification

Issue #184 adds `OpenGlThreadGuard` as the single non-owning thread-affinity authority shared by `GlfwWindow` and future renderer/OpenGL wrappers.

Focused verification:

```powershell
.\gradlew.bat :engine-platform-lwjgl:test --tests "com.samo.engine.platform.api.OpenGlThreadGuardTest" --tests "com.samo.engine.platform.api.GlfwWindowTest" --rerun-tasks
```

The focused tests must prove pre-initialize rejection, one-time binding, owner-thread success, worker-thread rejection before representative backend activity, continued owner-thread usability after rejection, and delegation of the existing window lifecycle rule to the same guard.

P5-T02 does not add a separate native acceptance executable because it introduces no new native OpenGL operation. The normal five-job final-candidate CI still applies, including the existing Windows native smoke and P5-T01 debug-callback acceptance.

## P5-T03 OpenGL resource ownership verification

Issue #185 adds internal `engine-render-opengl` wrappers for buffers, vertex arrays, textures, samplers, shaders, programs, and framebuffers. They consume `OpenGlThreadGuard` and `NativeResourceRegistry`; they do not expose a public renderer-resource API or implement drawing/upload/material behavior.

Focused failure-injection verification:

```powershell
.\gradlew.bat :engine-render-opengl:test --tests "com.samo.engine.render.opengl.internal.OpenGlResourceOwnershipTest" --rerun-tasks
```

Real Windows x64 acceptance:

```powershell
$env:SHERKO_P5_T03_NATIVE="true"
.\gradlew.bat :engine-render-opengl:test --tests "com.samo.engine.render.opengl.internal.OpenGlResourceNativeTest" --rerun-tasks
```

The native test creates all seven resource wrapper types on the production OpenGL 4.6 context, compiles one vertex and one fragment shader, links one program, closes resources in reverse dependency order, verifies the shared native-resource registry after window cleanup, and writes:

`engine-render-opengl/build/reports/p5/p5-t03-opengl-resources.txt`

This evidence proves bounded resource lifetime/cleanup only. It does not prove draw correctness, upload policy, material behavior, asset loading, performance, or renderer-loop behavior.

## P5-T04 bounded dynamic-buffer upload verification

Issue #186 adds an internal fixed-slot ring uploader backed by one P5-T03 owned OpenGL buffer and P5-T02 thread affinity. It uses explicit slot capacity/count, bounded sub-data writes, and one registered GLsync fence per submitted slot.

Focused verification:

```powershell
.\gradlew.bat :engine-render-opengl:test --tests "com.samo.engine.render.opengl.internal.BoundedDynamicBufferUploaderTest" --rerun-tasks
```

Real Windows x64 acceptance:

```powershell
$env:SHERKO_P5_T04_NATIVE="true"
.\gradlew.bat :engine-render-opengl:test --tests "com.samo.engine.render.opengl.internal.BoundedDynamicBufferUploaderNativeTest" --rerun-tasks
```

The native test uploads distinct patterns through a three-slot ring, inserts real OpenGL sync objects, forces completion only inside the test evidence path, wraps to slot zero, validates bounded buffer readback, verifies cleanup, and writes:

`engine-render-opengl/build/reports/p5/p5-t04-bounded-upload.txt`

This proves bounded upload correctness/synchronization only. It does not establish performance, streaming-allocator behavior, or persistent-mapping superiority.

## P5-T05 GLSL validation verification

Issue #187 adds one committed Phase 5 runtime shader pair and validates it twice: offline during build verification with locked LWJGL Shaderc, then on the real OpenGL 4.6 runtime path.

Offline validation:

```powershell
.\gradlew.bat :engine-render-opengl:validateGlsl --rerun-tasks
```

The task validates:
- `shaders/p5/basic.vert`;
- `shaders/p5/basic.frag`;
- one deliberately broken test fixture that must fail with source/stage diagnostics.

No machine-global `glslangValidator`, editor plugin, SDK path, or PATH configuration is required.

Focused runtime-diagnostic verification remains part of the normal renderer tests.

Real Windows x64 acceptance:

```powershell
$env:SHERKO_P5_T05_NATIVE="true"
.\gradlew.bat :engine-render-opengl:test --tests "com.samo.engine.render.opengl.internal.GlslRuntimeValidationNativeTest" --rerun-tasks
```

The native acceptance loads the same committed runtime shader resources, compiles vertex/fragment stages through `OpenGlShader`, links them through `OpenGlProgram`, verifies cleanup, and writes:

`engine-render-opengl/build/reports/p5/p5-t05-glsl-runtime.txt`

This proves GLSL validation/compile/link safety only. It does not establish uniform reflection, materials, shader variants, hot reload, asset-cooking behavior, or draw correctness.

## P5-T06 camera/per-frame uniform block verification

Issue #188 defines two internal std140 block ABIs consumed by the committed Phase 5 vertex shader:

- `CameraBlock`: binding 0, 128 bytes, view at offset 0, projection at offset 64;
- `PerFrameBlock`: binding 1, 16 bytes, framebuffer width/height/inverse vector at offset 0.

Focused packing/reflection tests:

```powershell
.\gradlew.bat :engine-render-opengl:test --tests "com.samo.engine.render.opengl.internal.UniformBlockPackingTest" --tests "com.samo.engine.render.opengl.internal.UniformBlockLayoutVerifierTest" --rerun-tasks
```

The modified committed GLSL must also continue passing P5-T05 offline validation:

```powershell
.\gradlew.bat :engine-render-opengl:validateGlsl --rerun-tasks
```

Real Windows x64 acceptance:

```powershell
$env:SHERKO_P5_T06_NATIVE="true"
.\gradlew.bat :engine-render-opengl:test --tests "com.samo.engine.render.opengl.internal.UniformBlockNativeTest" --rerun-tasks
```

The native test compiles/links the committed shader pair, reflects exact block sizes/bindings, packs representative D-045-compatible view/projection matrices plus framebuffer dimensions, verifies cleanup, and writes:

`engine-render-opengl/build/reports/p5/p5-t06-uniform-blocks.txt`

This proves the uniform-block ABI/reflection contract only. It does not establish a UBO allocator, world camera ownership, material blocks, or draw correctness.

## P5R-T10 reference-scene renderer decomposition verification

Issue #270 renames the internal fixed reference-room implementation and extracts only fixed CPU-side room fixture data. Public `OpenGlRenderer` behavior/signatures, GL resource ownership/order, shader ABI, material/light state, culling/sorting, presentation, diagnostics, and Phase 5 visual output remain unchanged.

Focused verification:

```powershell
.\\gradlew.bat :engine-render-opengl:test --tests "com.samo.engine.render.opengl.internal.ReferenceSceneRendererTest" --rerun-tasks
.\\gradlew.bat :engine-render-opengl:verifyPublicApiBoundary --rerun-tasks
.\\gradlew.bat :test-support:test --tests "com.samo.architecture.ModulePackageBoundaryTest" --rerun-tasks
.\\gradlew.bat :engine-render-opengl:validateGlsl --rerun-tasks
.\\gradlew.bat resolveAndLockAllDependencies
```

Repository/source review must confirm `ReferenceRoomFixture` is package-private, `ReferenceSceneRenderer` remains excluded from the API artifact despite Java-public visibility, old production/test class paths are gone, and the fixture's 24 vertices / 36 indices / 4x4 sRGB texture / world AABB remain byte/meaning compatible. No T11+ frame-orchestration responsibility may be extracted here.

The task changes Java/build/workflow/test source, so the exact final PR head requires the normal five-job heavy matrix including Windows native regressions. After merge, the exact merged `master` SHA requires the normal Lightweight verifier before Issue #270 can close.

Accepted P5R-T10 evidence: final PR head `a3d376eaf786c63e2e8e7e823020c4d2b6312d2b` passed all five required jobs in run #460 / `35505791294`, including the hosted-Windows renderer/native regressions and P5-T18 Phase 5 exit integration. PR #325 merged as `53d31e4b8dea6fd9b26dd64a2df859d929befad6`; exact merged-master Lightweight verification passed in run #461 / `35506050544`.

Wiki impact: none — supported public renderer API and intended usage are unchanged. Sandbox impact: none — the sandbox continues using `OpenGlRenderer` unchanged.

## P5R-T11 renderer frame-orchestration decomposition verification

Issue #271 keeps the public `OpenGlRenderer` contract and `ReferenceSceneRenderer` native-resource ownership unchanged while extracting package-private non-owning owners for frame-uniform upload, visibility/submission planning, draw execution, and latest-success diagnostics publication.

Focused verification:

```powershell
.\\gradlew.bat :engine-render-opengl:test --tests "com.samo.engine.render.opengl.internal.ReferenceSceneRendererTest" --rerun-tasks
.\\gradlew.bat :engine-render-opengl:test --tests "com.samo.engine.render.opengl.internal.RendererFrameDiagnosticsTest" --rerun-tasks
.\\gradlew.bat :engine-render-opengl:test --rerun-tasks
.\\gradlew.bat :engine-render-opengl:validateGlsl --rerun-tasks
.\\gradlew.bat :engine-render-opengl:verifyPublicApiBoundary --rerun-tasks
.\\gradlew.bat :test-support:test --tests "com.samo.architecture.ModulePackageBoundaryTest" --rerun-tasks
.\\gradlew.bat resolveAndLockAllDependencies
```

Source/test review must confirm the extracted collaborators are package-private, do not implement `AutoCloseable`, and do not own/delete native resources; `ReferenceSceneRenderer` retains creation/rollback/close ordering. Existing deterministic renderer tests remain the primary behavior regression for upload ordering, culling/sorting, world/debug/view-model order, GL-state restoration, failure-before-mutation rules, latest-success diagnostics, idempotent close, and partial-creation cleanup. `RendererFrameDiagnosticsTest` directly covers the extracted diagnostics owner's empty initial state and completed-frame publication.

The task changes Java source, so the exact final PR head requires the normal five-job heavy matrix including Windows native regressions. After merge, the exact merged `master` SHA requires Lightweight master verification before Issue #271 can close. No separate native artifact is introduced because T11 changes internal decomposition only; the existing renderer/native Phase 5 regressions remain authoritative.

Accepted P5R-T11 evidence: final PR head `2512677512e657de4f82b64e09f0b74fabd75c95` passed all five required jobs in run #463 / `35507186284`, including Windows native regressions and P5-T18 integration. PR #327 merged as `71ae263026f674bdfda2462f4bfb8c077ef24098`; exact merged-master Lightweight verification passed in run #464 / `35507442998`.

Wiki impact: none — public renderer API and consumer usage are unchanged. Sandbox impact: none — the persistent sandbox continues through public `OpenGlRenderer` unchanged.

## P5R-T12 renderer internal naming verification

Issue #272 is a naming-only internal refactor. It renames `RendererMaterial` to `RenderMaterialDescriptor`, `MaterialStatePolicy` to `OpenGlMaterialStatePolicy`, and `LocalLightSelection` to `LocalLightSelector`. Submission, culling, directional-light, subordinate material vocabulary, shader ABI, resource ownership, public API, and runtime behavior remain unchanged.

Focused verification:

```powershell
.\gradlew.bat :engine-render-opengl:test --tests "com.samo.engine.render.opengl.internal.RenderMaterialDescriptorTest" --rerun-tasks
.\gradlew.bat :engine-render-opengl:test --tests "com.samo.engine.render.opengl.internal.DrawSubmissionSorterTest" --rerun-tasks
.\gradlew.bat :engine-render-opengl:test --tests "com.samo.engine.render.opengl.internal.LocalLightSelectorTest" --rerun-tasks
.\gradlew.bat :engine-render-opengl:test --tests "com.samo.engine.render.opengl.internal.ReferenceSceneRendererTest" --rerun-tasks
.\gradlew.bat :engine-render-opengl:test --rerun-tasks
.\gradlew.bat :engine-render-opengl:validateGlsl --rerun-tasks
.\gradlew.bat :engine-render-opengl:verifyPublicApiBoundary --rerun-tasks
.\gradlew.bat :test-support:test --tests "com.samo.architecture.ModulePackageBoundaryTest" --rerun-tasks
.\gradlew.bat resolveAndLockAllDependencies
```

Source review must confirm no production/test/workflow reference remains to the three replaced symbols, while `DrawSubmission`, `DrawSubmissionSorter`, `CpuFrustumCuller`, `DirectionalLight`, and the subordinate material names remain intentionally unchanged. The P5-T09 native workflow now invokes `RenderMaterialDescriptorNativeTest`; retained report/image artifact names remain `p5-t09-materials` for historical evidence continuity. Local-light warning text, configured capacity 1–8, first-N truncation, draw ordering, material GL mappings, and shader outputs must remain byte/meaning compatible.

Because Java/test/workflow source changes, the exact final PR head requires the normal five-job heavy matrix including Windows native regressions. After merge, the exact merged `master` SHA requires Lightweight verification before Issue #272 can close.

Accepted P5R-T12 evidence: final PR head `85de9922f0fef5bbedc17add1bc41b1065d94f4a` passed all five required jobs in run #465 / `35508358983`, including the renamed P5-T09 native material regression, P5-T14 local-light regression, and P5-T18 integration. PR #329 merged as `aa3cccf885216f4e955c5cf671a96bb73702af8d`; exact merged-master Lightweight verification passed in run #466 / `35508613959`.

Wiki impact: none — no supported public API or consumer usage changes. Sandbox impact: none — owner-facing usage and behavior are unchanged.

## P5R-T13 uniform/color/presentation naming verification

Issue #273 is an internal naming-only refactor. It renames `CameraUniformBlock` to `CameraMatricesUniformBlock`, `PerFrameUniformBlock` to `FramebufferMetricsUniformBlock`, and `PresentationMode` to `SrgbPresentationMode`. `LocalLightUniformBlock`, `UniformBlockLayoutVerifier`, `SrgbTransfer`, `TextureColorEncoding`, shader block names, binding points, byte layouts, transfer math, and presentation encode count remain unchanged.

Focused verification:

```powershell
.\gradlew.bat :engine-render-opengl:test --tests "com.samo.engine.render.opengl.internal.UniformBlockPackingTest" --rerun-tasks
.\gradlew.bat :engine-render-opengl:test --tests "com.samo.engine.render.opengl.internal.UniformBlockLayoutVerifierTest" --rerun-tasks
.\gradlew.bat :engine-render-opengl:test --tests "com.samo.engine.render.opengl.internal.SrgbPresentationModeTest" --rerun-tasks
.\gradlew.bat :engine-render-opengl:test --tests "com.samo.engine.render.opengl.internal.SrgbTransferTest" --rerun-tasks
.\gradlew.bat :engine-render-opengl:test --tests "com.samo.engine.render.opengl.internal.OpenGlTextureColorEncodingTest" --rerun-tasks
.\gradlew.bat :engine-render-opengl:test --tests "com.samo.engine.render.opengl.internal.ReferenceSceneRendererTest" --rerun-tasks
.\gradlew.bat :engine-render-opengl:test --rerun-tasks
.\gradlew.bat :engine-render-opengl:validateGlsl --rerun-tasks
.\gradlew.bat :engine-render-opengl:verifyPublicApiBoundary --rerun-tasks
.\gradlew.bat :test-support:test --tests "com.samo.architecture.ModulePackageBoundaryTest" --rerun-tasks
.\gradlew.bat resolveAndLockAllDependencies
```

Source review must confirm no production/test reference remains to the three replaced Java type symbols while the literal GLSL ABI names `CameraBlock`, `PerFrameBlock`, and `LocalLightBlock` remain unchanged. Bindings 0/1/2, sizes 128/16/528, existing std140 offsets/capacity, `SHERKO_MANUAL_SRGB_ENCODE`, IEC sRGB threshold/constants, and `SRGB_COLOR`/`LINEAR_DATA` texture mappings must remain identical.

Because Java/test source changes, the exact final PR head requires the normal five-job heavy matrix including Windows native regressions. After merge, the exact merged `master` SHA requires Lightweight master verification before Issue #273 can close.

Accepted P5R-T13 evidence: final PR head `ad89e199face9e4ad90da54a021de772c039d6ad` passed all five required jobs in run #467 / `35510543823`, including P5-T06 uniform-block ABI/reflection, P5-T15 sRGB presentation, and P5-T18 integration. PR #331 merged as `4d45a9468d96b72bdbfc911647bf57e622f64558`; exact merged-master Lightweight verification passed in run #468 / `35510816660`.

Wiki impact: none — supported renderer API and consumer usage are unchanged. Sandbox impact: none — owner-facing controls and rendering behavior are unchanged.

## P5-T07 first indexed static mesh verification

Issue #189 introduces the first public production renderer path and window-owned presentation.

Focused deterministic tests:

```powershell
.\gradlew.bat :engine-platform-lwjgl:test --tests "com.samo.engine.platform.api.GlfwWindowTest" --rerun-tasks
.\gradlew.bat :engine-render-opengl:test --tests "com.samo.engine.render.opengl.internal.ReferenceSceneRendererTest" --rerun-tasks
```

The renderer test verifies exact one-time VAO/VBO/EBO/UBO setup, camera/per-frame binding indices 0/1, full-framebuffer viewport, depth `GL_LESS`, back-face culling, CCW front faces, exactly one indexed triangle draw, wrong-thread/invalid-frame rejection before draw mutation, idempotent close, and partial-creation cleanup.

The committed shaders must continue to pass P5-T05 offline validation:

```powershell
.\gradlew.bat :engine-render-opengl:validateGlsl --rerun-tasks
```

Real Windows x64 acceptance:

```powershell
$env:SHERKO_P5_T07_NATIVE="true"
.\gradlew.bat :engine-render-opengl:test --tests "com.samo.engine.render.opengl.internal.ReferenceSceneRendererNativeTest" --rerun-tasks
```

The native test renders through public `OpenGlRenderer`, requires exactly one generated primitive through an OpenGL pipeline query, polls the production debug window after the draw so staged high-severity errors surface, reads back the default back buffer, retains a PNG with visible triangle pixels, presents through `GlfwWindow.present()`, verifies cleanup, and writes:

- `engine-render-opengl/build/reports/p5/p5-t07-indexed-mesh.txt`
- `engine-render-opengl/build/reports/p5/p5-t07-indexed-mesh.png`

This is the authorized equivalent retained capture for P5-T07. It proves only the first indexed production draw; it does not establish sRGB correctness, arbitrary mesh loading, materials, lighting, world/ECS submission, or performance.

Owner-visible sandbox:

```powershell
.\gradlew.bat :game-sandbox:runSandbox
```

The persistent sandbox should show one white indexed triangle on a dark background while preserving the existing controls.

## P5-T08 sRGB color-path verification

Issue #190 establishes the first explicit decode/encode color-space contract while keeping texture/material APIs internal.

Focused verification:

```powershell
.\\gradlew.bat :engine-platform-lwjgl:test --tests "com.samo.engine.platform.api.GlfwWindowTest" --rerun-tasks
.\\gradlew.bat :engine-render-opengl:test --tests "com.samo.engine.render.opengl.internal.ReferenceSceneRendererTest" --rerun-tasks
.\\gradlew.bat :engine-render-opengl:test --tests "com.samo.engine.render.opengl.internal.OpenGlTextureColorEncodingTest" --rerun-tasks
.\\gradlew.bat :engine-render-opengl:validateGlsl --rerun-tasks
```

The deterministic tests require the window's `GLFW_SRGB_CAPABLE` hint, exact `GL_SRGB8_ALPHA8` versus `GL_RGBA8` format selection, the fixed renderer reference texture using the sRGB-color path, and both presentation modes: hardware `GL_FRAMEBUFFER_SRGB` on an `GL_SRGB` default buffer and one manual fragment encode with framebuffer sRGB disabled on an `GL_LINEAR` default buffer.

Windows native acceptance runs `SrgbColorPathNativeTest` with `SHERKO_P5_T08_NATIVE=true`. It records whether the production default back buffer is `GL_SRGB` or `GL_LINEAR`, renders through public `OpenGlRenderer`, reads back the triangle center, and requires RGB bytes within ±8 of encoded gray 128 in either mode. Run #381 demonstrated the real linear-default-buffer case, which is now an explicitly supported fallback instead of a failed capability assumption. The band remains deliberately far from approximate missing-encode (~55) and missing-decode/double-encode (~188) outcomes. It retains:

- `engine-render-opengl/build/reports/p5/p5-t08-srgb.txt`
- `engine-render-opengl/build/reports/p5/p5-t08-srgb.png`

This proves only the fixed reference texture decode plus exactly one presentation encode, using hardware on `GL_SRGB` default buffers or the bounded fragment fallback on `GL_LINEAR` default buffers. It does not establish general materials, arbitrary textures, asset/cooker behavior, HDR, tonemapping, fog, or post-processing.

## P5-T07A renderer public-boundary verification

Issue #213 repairs the P5-T07 Gradle/API boundary without changing `OpenGlRenderer` signatures or runtime renderer behavior.

Run the executable consumer/API boundary gate:

```powershell
.\\gradlew.bat :engine-render-opengl:verifyPublicApiBoundary --rerun-tasks
```

This gate compiles `src/publicApiTest/java` against the renderer API-only compile artifact plus only dependencies exported by the renderer's Gradle `api` metadata. The fixture imports and uses `OpenGlRenderer`, `OpenGlThreadGuard`, `NativeResourceRegistry`, and JOML matrix types, so it fails if signature dependencies are hidden as implementation details. The gate also requires `apiElements` not to expose the Java plugin's full-main `classes` secondary artifact, inspects the API jar to require `OpenGlRenderer` and reject renderer `.internal` classes, then inspects the normal runtime jar to require the indexed-mesh implementation and committed P5 shaders remain present.

Continue to run the accepted P5-T07 deterministic renderer regression and architecture boundary test:

```powershell
.\\gradlew.bat :engine-render-opengl:test --tests "com.samo.engine.render.opengl.internal.ReferenceSceneRendererTest" --rerun-tasks
.\\gradlew.bat :test-support:test --tests "com.samo.architecture.ModulePackageBoundaryTest" --rerun-tasks
.\\gradlew.bat :engine-render-opengl:validateGlsl --rerun-tasks
.\\gradlew.bat resolveAndLockAllDependencies
```

No dependency version is selected by P5-T07A. The last command is intentionally without `--write-locks`; unexpected lock drift is a failure to investigate, not an authorized lock refresh.

Sandbox impact: none — the existing sandbox uses the same public `OpenGlRenderer` calls and runtime composition. Wiki impact is limited to clarifying renderer-module consumption; no public signature or lifecycle behavior changes.

## P5-T09 renderer material verification

P5-T09 keeps its material value internal to `engine-render-opengl`; it adds no public material/resource API and no dependency or module edge.

The committed P5 GLSL validator uses Shaderc with an explicit OpenGL target environment (`shaderc_target_env_opengl`, OpenGL 4.5 SPIR-V environment, SPIR-V 1.0) rather than Shaderc's default Vulkan target. This keeps offline validation aligned with the repository's OpenGL runtime contract; runtime OpenGL 4.6 compile/link verification remains authoritative for the actual driver path.

Focused deterministic verification:

```powershell
.\gradlew.bat :engine-render-opengl:test --tests "com.samo.engine.render.opengl.internal.RenderMaterialDescriptorTest" --rerun-tasks
.\gradlew.bat :engine-render-opengl:test --tests "com.samo.engine.render.opengl.internal.ReferenceSceneRendererTest" --rerun-tasks
.\gradlew.bat :engine-render-opengl:validateGlsl --rerun-tasks
.\gradlew.bat :test-support:test --tests "com.samo.architecture.ModulePackageBoundaryTest" --rerun-tasks
.\gradlew.bat resolveAndLockAllDependencies
```

Windows native acceptance is part of `Windows native smoke` with `SHERKO_P5_T09_NATIVE=true` and runs on the hosted Windows/Mesa correctness environment. It must retain:

- `engine-render-opengl/build/reports/p5/p5-t09-materials.txt`;
- `engine-render-opengl/build/reports/p5/p5-t09-materials.png`;
- `engine-render-opengl/build/test-results/test/TEST-com.samo.engine.render.opengl.internal.RenderMaterialDescriptorNativeTest.xml`.

The native acceptance requires two primitives from the same owned indexed mesh, a baseline left material that preserves the P5-T08 encoded gray tolerance, a visibly distinct tinted right material, restored full-frame viewport, unbound program/VAO, disabled framebuffer-sRGB state after render, and an empty native-resource registry after cleanup. This evidence is renderer correctness/lifecycle evidence only; hosted software OpenGL is not physical-GPU performance or vendor-driver qualification.

## P5-T18 Phase 5 exit-integration verification

P5-T18 keeps the public renderer and module graph unchanged. It replaces only the renderer-owned validation geometry/texture fixture and makes the persistent sandbox's existing public input/camera path affect the submitted view matrix.

Focused deterministic verification:

```powershell
.\gradlew.bat :engine-render-opengl:test --tests "com.samo.engine.render.opengl.internal.ReferenceSceneRendererTest" --rerun-tasks
.\gradlew.bat :game-sandbox:test --tests "com.samo.game.sandbox.SandboxCameraTest" --rerun-tasks
.\gradlew.bat :engine-render-opengl:validateGlsl --rerun-tasks
.\gradlew.bat :test-support:test --tests "com.samo.architecture.ModulePackageBoundaryTest" --rerun-tasks
.\gradlew.bat resolveAndLockAllDependencies
```

Windows native acceptance is part of `Windows native smoke` with `SHERKO_P5_T18_NATIVE=true` and runs `Phase5ExitNativeTest`. It retains:

- `engine-render-opengl/build/reports/p5/p5-t18-exit.txt`;
- `engine-render-opengl/build/reports/p5/p5-t18-room-camera-a.png`;
- `engine-render-opengl/build/reports/p5/p5-t18-room-camera-b.png`;
- `engine-render-opengl/build/test-results/test/TEST-com.samo.engine.render.opengl.internal.Phase5ExitNativeTest.xml`.

The native evidence must show the fixed internal room's mapped non-uniform sRGB texture, multiple visible surfaces, the nearer depth panel occluding the farther wall at the controlled reference sample, a known camera translation changing the projected/read-back center sample, the accepted directional-light contribution, P5-T16 debug geometry and P5-T17 view-model coexistence, restored viewport/program/VAO/framebuffer-sRGB state, and an empty native-resource registry after cleanup. The sandbox camera's W/A/S/D and mouse LOOK mapping is covered separately by deterministic `SandboxCameraTest`, because the native renderer evidence intentionally does not create a reverse dependency from `engine-render-opengl` into `game-sandbox`.

After P5-T18 merges and exact-merge verification passes, rerun Phase 5 exit review #250 against that exact `master`. P5-T18 task acceptance alone does not mark Phase 5 complete.

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

The phase exit gates in [TECHNICAL_BACKLOG.md](roadmap/TECHNICAL_BACKLOG.md) and milestone outcomes in [ROADMAP.md](../ROADMAP.md) remain the acceptance sources. This procedure adds evidence discipline, not new feature requirements or numerical thresholds. It applies to every phase, including a headless/test-only phase; a visual sandbox is not universally required.

Before claiming a phase is complete:

1. In the bounded phase-exit Issue/PR, link the applicable existing gate and identify every part of it that must be demonstrated. If required behavior is missing, leave the gate open and identify the bounded follow-up instead of weakening acceptance.
2. Describe one repeatable scenario (or the minimum scenarios needed) through the actual participating systems and public boundaries. Use the persistent sandbox when suitable and available, or a headless/test harness for nonvisual behavior. Mock-only subsystem tests do not demonstrate real integration.
3. Record exact executable commands or manual steps, inputs/assets/seeds, tested commit, environment, original duration/impairment thresholds where specified, expected behavior, and observed results. Add commands to this file when implemented; do not publish hypothetical commands as runnable.
4. Retain relevant logs, reports, traces, captures, and cleanup observations. State what was not exercised. Routine CI, a screenshot, or task checkmarks alone cannot replace an unexecuted integration/duration gate.
5. Record pass/fail and remaining blockers in the Issue/PR; update `DEVELOPMENT_STATUS.md` with the durable conclusion and evidence links. Do not mark the phase complete while part of its exit gate remains unproven.
6. Review the next phase against the demonstrated behavior: are its assumptions and dependencies satisfied, are proposed abstractions needed by its current use cases, and do its acceptance criteria still describe the required outcome? Record the next bounded task and any refinements in the closing Issue/PR. Update backlog definitions and affected executable Issues only when an authorized refinement is needed; never silently change locked scope or decisions.

For the completed P2 phase, D-030 / Issue #135 defined the gate as one headless loop running deterministic fixed 60 Hz simulation ticks for at least 60 continuous seconds with bounded catch-up and verified cleanup. The gate evidence must show those properties together; isolated P2-T01 through P2-T13 suites do not satisfy it. The explicit command and retained report are defined in the P2 integrated exit-gate section above.

For comparison, P3 requires replaying an identical input sequence into headless simulation, while P4 requires spatial tests independent of OpenGL/Jolt. Use those actual gate forms rather than requiring a rendered sandbox for every phase. Later phases retain their own scene, multiplayer, tooling, and release criteria from the backlog.

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

All heavy jobs and the lightweight exact-merge `master` verifier use `windows-latest`. `Windows native smoke` provisions pinned Mesa software OpenGL inside that hosted VM before running the unchanged native suites. The workflow does not use an OpenGL version override and does not skip native acceptance; successful context creation and the existing tests remain the gate.

The heavy five-job PR/manual matrix covers:

- `Build and quality gates`: Java/toolchain reporting, project inventory, committed dependency-lock resolution, all-module build/quality gates, client/server entry points, headless-server runtime boundary, and client/server version compatibility;
- `Unit tests`: root/subproject aggregation plus the focused engine-core evidence suites and retained reports; P3-T07's `InputActionBindingsTest` is also included by the ordinary platform-module test task;
- `Architecture tests`: explicit package/module boundary enforcement;
- `JaCoCo coverage reports`: tests plus XML/HTML coverage generation/upload;
- `Windows native smoke`: historical GLFW/OpenAL/Jolt smoke plus the current bounded production P3 native acceptance sequence, including P3-T05 raw-motion mode/focus evidence when present.

The lightweight `master` verifier is deliberately narrower. On the exact pushed `master` SHA it checks out that commit, sets up Java 25/Gradle, resolves committed dependency locks and fails on drift, verifies the headless server runtime boundary, runs client/server `--version`, requires both reported `engineCommit` values to equal the exact workflow SHA, and requires shared compatibility identifiers to match. It proves exact-merge identity and critical runtime-boundary/version wiring without repeating unit, coverage, architecture, and native suites that already passed on the unchanged final candidate.

The workflow uses top-level concurrency with `cancel-in-progress: true`, keyed by workflow/PR for pull requests and by ref for push/manual runs. A newer commit on the same PR supersedes older queued/in-progress candidate runs; stale cancelled runs neither pass nor fail the current candidate. Never cancel the current final-candidate run merely to save runner time.

The GitHub-hosted native job is an ephemeral Windows VM. Its stock graphics environment is insufficient for the required context, so the workflow installs the pinned Mesa archive only for that job after verifying the archive SHA-256. The committed Gradle Wrapper, Java 25 setup, dependency locks, Mesa pin/hash, explicit task outputs, and repository tests are the verification contracts. Software-rendered CI acceptance does not establish physical-GPU performance or vendor-driver qualification.

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

For sandbox/playground work, record automated test/build/headless-boundary results separately from manual human observation. A locally viewed window or console trace is useful owner feedback, not a substitute for final-candidate CI or a performance claim.

Configuration review is not runtime evidence. If a command was not run, write `not run` and why; a Markdown-only policy exemption is a reason, not a passing execution result.

## Dependency changes

After an authorized dependency/version or dependency-ownership change:

```powershell
.\gradlew.bat resolveAndLockAllDependencies --write-locks
.\gradlew.bat buildAllModules
```

Review every changed lockfile. P1-T10A relocates existing dependencies without changing their selected versions. P3-T01 likewise places the already-selected LWJGL 3.4.3 core/GLFW/OpenGL dependencies and Windows natives into `engine-platform-lwjgl`; only that module's ownership-related lock change is expected for P3-T01, and no version-catalog change is authorized. P3-T02, P3-T03, P3-T04, P3-T05, and P3-T06 add no dependency or dependency-ownership change. P3-T04A originally added only a non-exported existing-project dependency for the sandbox surface (`compileOnly` plus dedicated non-consumable `engineDemoRuntime`). Issue #165 renames that custom resolvable configuration to `sandboxRuntime` and makes `runSandbox` canonical without changing dependency ownership or selected versions. It must not alter the server runtime. P3-T07 adds the first production Jackson JSON parser dependency authorized by D-037: `jackson-databind:2.21.2` as `implementation` of `engine-platform-lwjgl`, resolving `jackson-core:2.21.2` and `jackson-annotations:2.21`; inspect the version-catalog and platform lock changes explicitly and require no unrelated lock drift. Update `game-sandbox/gradle.lockfile` only if Gradle's lock resolution for its dedicated configuration actually requires it, and inspect that diff explicitly.

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

## Persistent sandbox verification policy

`game-sandbox` is the owner-facing persistent cumulative playground for already implemented engine behavior, not a specification or acceptance authority. For every implementation task, handoff must record either:

- `Sandbox impact: updated <sandbox files>` when the new capability is meaningfully usable through already-authorized public production APIs; the update must extend the existing owner-controlled playground and preserve already-usable capabilities unless the task explicitly removes/supersedes them; or
- `Sandbox impact: none — <reason>` when a meaningful playground path would require exposing internals, calling native libraries directly, or implementing a future roadmap task.

Do not reintroduce a fixed-duration timeline, automatic feature tour, disposable per-task executable, or one-feature showcase as the default sandbox model. Prefer owner-controlled interaction and coexistence. A separate subsystem-specific playground is exceptional and requires explicit Issue authorization.

Never weaken a production boundary or add a public API solely to satisfy the sandbox. `game-sandbox/README.md` owns the current manual controls/run instructions and limitations; this file owns the distinction between manual observation and verification evidence.


## P5-T10 immutable frame-submission verification

P5-T10 adds public `RenderFramePacket` without adding a project dependency, production dependency, native handle, public mesh/material handle, asset identity, queue, or cleanup lifecycle. The packet is a complete immutable snapshot of the renderer-facing frame data available at this stage: view matrix, projection matrix, and framebuffer pixel dimensions.

Focused verification:

```powershell
.\gradlew.bat :engine-render-opengl:test --tests "com.samo.engine.render.api.RenderFramePacketTest" --rerun-tasks
.\gradlew.bat :engine-render-opengl:test --tests "com.samo.engine.render.opengl.internal.ReferenceSceneRendererTest" --rerun-tasks
.\gradlew.bat :engine-render-opengl:verifyPublicApiBoundary --rerun-tasks
.\gradlew.bat :engine-render-opengl:validateGlsl --rerun-tasks
.\gradlew.bat :test-support:test --tests "com.samo.architecture.ModulePackageBoundaryTest" --rerun-tasks
.\gradlew.bat resolveAndLockAllDependencies
```

`RenderFramePacketTest` must prove constructor validation, defensive copies of mutable source matrices, and non-aliasing of matrices copied back out. `IndexedStaticMeshPipelineTest` must prove controlled consumption uses the captured packet values after caller source mutation while preserving established P5 draw/state behavior.

The existing P5-T09 Windows native acceptance remains a regression over the same production `OpenGlRenderer` path because the compatibility overload constructs a `RenderFramePacket` before entering the pipeline. P5-T10 adds no new native behavior or visual requirement, so no separate native artifact is required beyond the ordinary heavy matrix.

Ordering/lifetime contract: packet construction creates one complete frame snapshot; renderer consumption is synchronous in caller invocation order; the renderer does not retain packets after `render(...)` returns. Packets own no native resources and require no `close()`. This is not an async queue or network snapshot contract.


## P5-T11 CPU frustum-culling verification

P5-T11 adds no project or production dependency. It reuses public P4 `Aabb3f`, `Plane3f`, and `Frustum3f` semantics and the accepted P5-T10 frame snapshot. The fixed reference mesh remains renderer-owned.

Focused verification:

```powershell
.\gradlew.bat :engine-render-opengl:test --tests "com.samo.engine.render.opengl.internal.CpuFrustumCullerTest" --rerun-tasks
.\gradlew.bat :engine-render-opengl:test --tests "com.samo.engine.render.opengl.internal.ViewFrustumExtractorTest" --rerun-tasks
.\gradlew.bat :engine-render-opengl:test --tests "com.samo.engine.render.opengl.internal.ReferenceSceneRendererTest" --rerun-tasks
.\gradlew.bat :engine-render-opengl:test --tests "com.samo.engine.render.api.RenderCullingCountersTest" --rerun-tasks
.\gradlew.bat :game-sandbox:test --tests "com.samo.game.sandbox.SandboxDiagnosticFormatterTest" --rerun-tasks
.\gradlew.bat :engine-render-opengl:verifyPublicApiBoundary --rerun-tasks
.\gradlew.bat :test-support:test --tests "com.samo.architecture.ModulePackageBoundaryTest" --rerun-tasks
.\gradlew.bat resolveAndLockAllDependencies
```

Required deterministic evidence:
- identity clip-space fixture: inside and exact boundary-contact AABBs remain visible; clearly outside AABBs are rejected;
- translated camera fixture: world bounds move correctly relative to the extracted frustum;
- controlled renderer fixture: two off-camera candidates produce zero material-state applications and zero indexed draws;
- latest-success counters remain unchanged when a later render fails;
- all-culled frames still restore the full framebuffer viewport and disable framebuffer-sRGB state.

The ordinary five-job final-candidate CI remains the authoritative repository verification. The existing Windows native P5 regression remains applicable because the normal sandbox/reference camera keeps the committed fixed mesh visible; P5-T11 adds no new native API or required capture artifact.


## P5-T12 deterministic draw-ordering verification

P5-T12 adds no project dependency, production dependency, public renderer signature, resource identity, or native ownership change. Ordering is package-internal and runs only after accepted P5-T11 culling.

Focused verification:

```powershell
.\gradlew.bat :engine-render-opengl:test --tests "com.samo.engine.render.opengl.internal.DrawSubmissionSorterTest" --rerun-tasks
.\gradlew.bat :engine-render-opengl:test --tests "com.samo.engine.render.opengl.internal.ReferenceSceneRendererTest" --rerun-tasks
.\gradlew.bat :engine-render-opengl:verifyPublicApiBoundary --rerun-tasks
.\gradlew.bat :test-support:test --tests "com.samo.architecture.ModulePackageBoundaryTest" --rerun-tasks
.\gradlew.bat resolveAndLockAllDependencies
```

Required deterministic evidence:
- opaque fixtures order by logical program key, material key, mesh key, then original sequence;
- transparent fixtures order by descending camera-space depth, then stable logical keys and sequence;
- mixed fixtures always place opaque submissions before transparent submissions;
- empty/singleton inputs remain deterministic;
- source collections and material identities remain unchanged by sorting;
- the existing controlled renderer trace still consumes the opaque baseline candidate before the transparent tinted candidate after P5-T11 culling.

The ordinary five-job final-candidate CI remains authoritative. Existing Windows native P5 evidence remains applicable because P5-T12 changes only CPU ordering of the already-visible fixed scene and adds no new native API or visual content.

## P5-T13 directional-light verification

P5-T13 adds one package-internal renderer-owned unshadowed directional light. It adds no project dependency, production dependency, public renderer signature, public resource identity, or native-resource owner. The fixed light stores normalized D-041 world-space ray-travel direction, linear RGB, and bounded SDR intensity. Lighting multiplication occurs in linear space before the existing P5-T08 single presentation encode.

Focused verification:

```powershell
.\gradlew.bat :engine-render-opengl:test --tests "com.samo.engine.render.opengl.internal.DirectionalLightTest" --rerun-tasks
.\gradlew.bat :engine-render-opengl:test --tests "com.samo.engine.render.opengl.internal.ReferenceSceneRendererTest" --rerun-tasks
.\gradlew.bat :engine-render-opengl:validateGlsl --rerun-tasks
.\gradlew.bat :engine-render-opengl:verifyPublicApiBoundary --rerun-tasks
.\gradlew.bat :test-support:test --tests "com.samo.architecture.ModulePackageBoundaryTest" --rerun-tasks
.\gradlew.bat resolveAndLockAllDependencies
```

Required deterministic evidence:
- `DirectionalLightTest` independently verifies normalization and the handwritten Lambert response for front/angled/perpendicular/back-facing normals plus invalid inputs;
- the pipeline fixture verifies the renderer uploads one fixed normalized light while preserving two-material draw ordering, culling counters, material state, and cleanup;
- the existing P5-T08 regression now includes the known linear diffuse multiplier before exactly one sRGB presentation encode rather than assuming an unlit final pixel;
- the existing P5-T09 regression retains its two-material/state assertions using the light-adjusted neutral baseline;
- Windows native acceptance runs `DirectionalLightNativeTest` with `SHERKO_P5_T13_NATIVE=true`, preserves two indexed draws and state/native-resource cleanup, and retains:
  - `engine-render-opengl/build/reports/p5/p5-t13-directional-light.txt`;
  - `engine-render-opengl/build/reports/p5/p5-t13-directional-light.png`.

The ordinary five-job exact-head PR CI matrix remains authoritative. P5-T13 acceptance additionally requires the retained Windows directional-light artifact above, then exact-merge Lightweight master verification after merge. The native capture is correctness evidence only; Mesa software rendering does not establish physical-GPU performance.

## P5-T14 bounded local-light verification

P5-T14 adds public immutable point/spot renderer submissions while preserving the existing forward-renderer/module boundaries. The per-frame combined local-light maximum is configurable from 1 through 8; shader/storage capacity is fixed at 8. Overflow preserves packet order, accepts the first configured N values, emits exactly one renderer WARN for that render call, and drops the remainder before local-light buffer upload or draw-state mutation.

Focused verification:

```powershell
.\gradlew.bat :engine-render-opengl:test --tests "com.samo.engine.render.api.RenderLocalLightTest" --rerun-tasks
.\gradlew.bat :engine-render-opengl:test --tests "com.samo.engine.render.api.RenderFramePacketTest" --rerun-tasks
.\gradlew.bat :engine-render-opengl:test --tests "com.samo.engine.render.api.OpenGlRendererConfigurationTest" --rerun-tasks
.\gradlew.bat :engine-render-opengl:test --tests "com.samo.engine.render.opengl.internal.LocalLightSelectorTest" --rerun-tasks
.\gradlew.bat :engine-render-opengl:test --tests "com.samo.engine.render.opengl.internal.LocalLightUniformBlockTest" --rerun-tasks
.\gradlew.bat :engine-render-opengl:test --tests "com.samo.engine.render.opengl.internal.ReferenceSceneRendererTest" --rerun-tasks
.\gradlew.bat :game-sandbox:test --rerun-tasks
.\gradlew.bat :engine-render-opengl:validateGlsl --rerun-tasks
.\gradlew.bat :engine-render-opengl:verifyPublicApiBoundary --rerun-tasks
.\gradlew.bat :test-support:test --tests "com.samo.architecture.ModulePackageBoundaryTest" --rerun-tasks
.\gradlew.bat resolveAndLockAllDependencies
```

Deterministic evidence must establish:

- `RenderPointLight` / `RenderSpotLight` reject non-finite or out-of-contract data and spot direction is normalized once at construction;
- `RenderFramePacket` snapshots the ordered local-light list defensively while the original four-argument constructor produces an empty list;
- configured maximum 0 or 9 fails before native renderer-resource creation, while values 1–8 are the bounded supported domain;
- zero and exactly-max selection emit no warning;
- over-max selection keeps the first N values, emits exactly one WARN containing submitted/accepted/dropped/configured-max counts, and a warning-sink failure occurs before local-light upload/draw mutation;
- the fixed std140 `LocalLightBlock` is 528 bytes at binding 2, packs eight-entry position/range, direction/type, color/intensity, cone-cosine arrays plus count, and zero-fills unused/stale entries deterministically;
- `verifyPublicApiBoundary` compiles a consumer using the new point/spot values, local-light packet constructor, and renderer configuration overload against the API-only artifact.

Windows native acceptance runs `LocalLightsNativeTest` with `SHERKO_P5_T14_NATIVE=true`. The fixture configures max=2 and submits three lights in order: one point light, one spot light whose cone alignment lies exactly halfway between the inner/outer cosine thresholds (expected smoothstep factor 0.5), and one strong red overflow point light. It requires one overflow WARN, exactly two indexed draws, neutral expected baseline brightness from the independently calculated directional + point-range + partial-spot contribution, and proves the dropped red light cannot tint that baseline. It also requires viewport/program/VAO/framebuffer-sRGB cleanup and an empty native-resource registry. Retained artifacts:

- `engine-render-opengl/build/reports/p5/p5-t14-local-lights.txt`
- `engine-render-opengl/build/reports/p5/p5-t14-local-lights.png`
- `engine-render-opengl/build/test-results/test/TEST-com.samo.engine.render.opengl.internal.LocalLightsNativeTest.xml`

The existing P5-T08, P5-T09, and P5-T13 native regressions remain in the same Windows native job and exercise the empty-local-light compatibility path.

The ordinary five-job exact-head PR CI matrix remains authoritative, followed by exact-merge Lightweight master verification after merge. Hosted Windows native evidence uses pinned Mesa llvmpipe for correctness only and does not establish physical-GPU performance.

## P5-T15 finalized gamma/sRGB presentation verification

P5-T15 does not replace P5-T08 texture decode semantics. It finalizes the default-framebuffer presentation policy around one package-internal mode selected from the actual back-buffer encoding.

Focused verification:

```powershell
.\gradlew.bat :engine-render-opengl:test --tests "com.samo.engine.render.opengl.internal.SrgbTransferTest" --rerun-tasks
.\gradlew.bat :engine-render-opengl:test --tests "com.samo.engine.render.opengl.internal.SrgbPresentationModeTest" --rerun-tasks
.\gradlew.bat :engine-render-opengl:test --tests "com.samo.engine.render.opengl.internal.ReferenceSceneRendererTest" --rerun-tasks
.\gradlew.bat :engine-render-opengl:test --tests "com.samo.engine.render.opengl.internal.OpenGlTextureColorEncodingTest" --rerun-tasks
.\gradlew.bat :engine-render-opengl:validateGlsl --rerun-tasks
.\gradlew.bat :engine-render-opengl:verifyPublicApiBoundary --rerun-tasks
.\gradlew.bat :test-support:test --tests "com.samo.architecture.ModulePackageBoundaryTest" --rerun-tasks
.\gradlew.bat resolveAndLockAllDependencies
```

The deterministic fixtures require the exact IEC sRGB lower/upper branches, threshold behavior, finite `[0,1]` input rejection, mapping of actual `GL_SRGB` and `GL_LINEAR` default-buffer encodings to one presentation mode, rejection of unsupported encodings, unchanged shader source for hardware encoding, and exactly one injected manual-encode define for the linear-default-buffer fallback. Existing `GL_SRGB8_ALPHA8` versus `GL_RGBA8` texture-format tests remain authoritative for D-056 sampling semantics.

Windows native acceptance runs `SrgbPresentationNativeTest` with `SHERKO_P5_T15_NATIVE=true`. It renders through public `OpenGlRenderer`, reads a background pixel outside the reference geometry, and compares production linear clear RGB `0.08/0.10/0.14` with independently calculated IEC sRGB bytes. The expected bytes are approximately `80/89/105`; approximate missing-encode bytes are `20/26/36`, and double-encode bytes are approximately `152/160/172`. The written tolerance is ±5 bytes, deliberately far from both wrong paths. The same fixture reads the current baseline lit reference pixel to preserve D-056/D-061 composition and verifies framebuffer-sRGB/program/VAO/viewport cleanup plus an empty native-resource registry.

Retained artifacts:

- `engine-render-opengl/build/reports/p5/p5-t15-srgb-presentation.txt`
- `engine-render-opengl/build/reports/p5/p5-t15-srgb-presentation.png`
- `engine-render-opengl/build/test-results/test/TEST-com.samo.engine.render.opengl.internal.SrgbPresentationNativeTest.xml`

The earlier P5-T08 sRGB native acceptance stays in the same Windows job and remains a required regression. The ordinary exact-head five-job PR matrix plus exact-merge Lightweight verification remains authoritative.

## P5-T16 renderer-neutral debug geometry verification

P5-T16 adds bounded per-frame debug diagnostics without adding a module edge or production dependency. Public debug values live in `engine-core`; OpenGL adaptation remains internal to `engine-render-opengl`.

Focused verification:

```powershell
.\gradlew.bat :engine-core:test --tests "com.samo.engine.core.api.DebugFrameTest" --rerun-tasks
.\gradlew.bat :engine-render-opengl:test --tests "com.samo.engine.render.api.RenderFramePacketTest" --rerun-tasks
.\gradlew.bat :engine-render-opengl:test --tests "com.samo.engine.render.opengl.internal.DebugLineVertexPackerTest" --rerun-tasks
.\gradlew.bat :engine-render-opengl:test --tests "com.samo.engine.render.opengl.internal.ReferenceSceneRendererTest" --rerun-tasks
.\gradlew.bat :game-sandbox:test --rerun-tasks
.\gradlew.bat :engine-render-opengl:validateGlsl --rerun-tasks
.\gradlew.bat :engine-render-opengl:verifyPublicApiBoundary --rerun-tasks
.\gradlew.bat :test-support:test --tests "com.samo.architecture.ModulePackageBoundaryTest" --rerun-tasks
.\gradlew.bat resolveAndLockAllDependencies
```

Deterministic fixtures establish:

- `DebugFrame` accepts zero and exact-capacity snapshots, rejects more than 64 primitives or 16 counters, rejects null elements, and defensively snapshots caller lists;
- debug colors/lines/ray lengths and ASCII counter labels reject non-finite/out-of-contract values;
- accepted P4 `Aabb3f`, `Sphere3f`, and `Ray3f` values are wrapped rather than redefined;
- line expansion is deterministic: line=1 segment, AABB=12 segments, sphere=3 x 16 segments, ray=1 segment; the worst-case 64-sphere frame exactly fills the fixed 6144-vertex / 147456-byte buffer;
- the pipeline uploads/draws debug geometry after the current scene, uses the dedicated debug draw state/program/VAO, publishes text counters only after a successful full render, and leaves prior debug/culling snapshots unchanged when the debug draw fails;
- the public API consumer compiles using `DebugFrame`, the packet debug constructor, and `lastDebugTextCounters()` without importing renderer internals;
- committed debug-line shaders and the manual-sRGB fallback variant pass locked Shaderc validation.

Windows native acceptance runs `DebugGeometryNativeTest` with `SHERKO_P5_T16_NATIVE=true`. It submits one line, AABB, sphere, ray, and two text counters through public production APIs. A closer green line crosses a small controlled region around framebuffer center; at least one pixel in that region must be strongly green-dominant. The renderer-published counter list must exactly match submitted order/values, current scene culling diagnostics must still report two indexed submissions, and viewport/program/VAO/framebuffer-sRGB cleanup plus an empty native-resource registry remain required.

Retained artifacts:

- `engine-render-opengl/build/reports/p5/p5-t16-debug-geometry.txt`
- `engine-render-opengl/build/reports/p5/p5-t16-debug-geometry.png`
- `engine-render-opengl/build/test-results/test/TEST-com.samo.engine.render.opengl.internal.DebugGeometryNativeTest.xml`

The ordinary exact-head five-job PR matrix plus exact-merge Lightweight verification remains authoritative. Native evidence is correctness-only and does not establish GPU performance.

## P5-T17 first-person view-model verification

P5-T17 adds one bounded internal view-model validation layer after the accepted world/debug stages. It does not change the public renderer submission surface or D-041/D-045 world-camera semantics.

Focused verification:

```powershell
.\gradlew.bat :engine-render-opengl:test --tests "com.samo.engine.render.opengl.internal.ViewModelProjectionFactoryTest" --rerun-tasks
.\gradlew.bat :engine-render-opengl:test --tests "com.samo.engine.render.opengl.internal.ReferenceSceneRendererTest" --rerun-tasks
.\gradlew.bat :engine-render-opengl:validateGlsl --rerun-tasks
.\gradlew.bat :engine-render-opengl:verifyPublicApiBoundary --rerun-tasks
.\gradlew.bat :game-sandbox:test --rerun-tasks
.\gradlew.bat :test-support:test --tests "com.samo.architecture.ModulePackageBoundaryTest" --rerun-tasks
.\gradlew.bat resolveAndLockAllDependencies
```

Deterministic fixtures establish:

- view-model projection uses identity view, exactly 55° vertical FOV, current framebuffer aspect, 0.01 m near and 10 m far with independent expected matrix terms;
- the view-model stage remains outside world draw sorting/culling and executes after world/debug work;
- the pass performs a depth-only reset, then applies depth test `GL_LESS`, depth writes enabled, blending/culling disabled;
- dedicated camera binding/program/VAO are restored to world/default state even when the view-model draw fails;
- failed view-model rendering does not publish new P5-T11 culling or P5-T16 text-counter snapshots;
- dedicated view-model VAO/VBO/camera-UBO/shaders/program participate in normal rollback/close/native-registry ownership;
- committed view-model shaders plus the P5-T15 manual-sRGB fallback variant pass locked Shaderc validation;
- the runtime artifact retains the committed view-model shader resources.

Windows native acceptance runs `ViewModelNativeTest` with `SHERKO_P5_T17_NATIVE=true`. A controlled world camera places the current depth-writing baseline triangle over a known sample inside the camera-relative fixture. Independent geometry math proves the sample lies inside the world triangle, and independent projection math proves world window depth is closer than the view-model depth, so the fixture would fail `GL_LESS` without depth isolation. After the production render, the same region must contain the expected fixed orange fixture color within ±8 bytes. One P5-T16 debug line also coexists in the frame, current world culling diagnostics still report two indexed scene draws, and viewport/program/VAO/framebuffer-sRGB cleanup plus an empty native-resource registry remain required.

Retained artifacts:

- `engine-render-opengl/build/reports/p5/p5-t17-view-model.txt`
- `engine-render-opengl/build/reports/p5/p5-t17-view-model.png`
- `engine-render-opengl/build/test-results/test/TEST-com.samo.engine.render.opengl.internal.ViewModelNativeTest.xml`

The ordinary exact-head five-job PR matrix plus exact-merge Lightweight verification remains authoritative. Native evidence is correctness-only and establishes no performance or gameplay claim.



## P5R-T14 OpenGL ownership cleanup verification

Issue #274 is a package-private ownership-cleanup refactor. It keeps all accepted resource-wrapper/backend names and D-049/D-050/D-051 lifecycle behavior unchanged, renames only `CleanupFailures` to `CleanupFailureSuppression`, and centralizes equivalent rollback suppression through one narrow helper.

Focused verification:

```powershell
.\gradlew.bat :engine-render-opengl:test --tests "com.samo.engine.render.opengl.internal.CleanupFailureSuppressionTest" --rerun-tasks
.\gradlew.bat :engine-render-opengl:test --tests "com.samo.engine.render.opengl.internal.OpenGlResourceOwnershipTest" --rerun-tasks
.\gradlew.bat :engine-render-opengl:test --tests "com.samo.engine.render.opengl.internal.BoundedDynamicBufferUploaderTest" --rerun-tasks
.\gradlew.bat :engine-render-opengl:test --tests "com.samo.engine.render.opengl.internal.OpenGlTextureColorEncodingTest" --rerun-tasks
.\gradlew.bat :engine-render-opengl:test --tests "com.samo.engine.render.opengl.internal.ReferenceSceneRendererTest" --rerun-tasks
.\gradlew.bat :engine-render-opengl:test --rerun-tasks
.\gradlew.bat :engine-render-opengl:validateGlsl --rerun-tasks
.\gradlew.bat :engine-render-opengl:verifyPublicApiBoundary --rerun-tasks
.\gradlew.bat :test-support:test --tests "com.samo.architecture.ModulePackageBoundaryTest" --rerun-tasks
.\gradlew.bat resolveAndLockAllDependencies
```

Source review must confirm no production/test reference remains to `CleanupFailures`, all resource/backend wrapper names remain unchanged, and no module/dependency/resource/shader/public API changes are present. Existing P5-T03/P5-T04 ownership tests remain authoritative for create/register/delete, owner-thread rejection, idempotent close, fence cleanup, leak detection, and failure ordering.

Because Java/test source changes, the exact final PR head requires the normal five-job heavy matrix including Windows native P5-T03/P5-T04 regressions. After merge, the exact merged `master` SHA requires Lightweight master verification before Issue #274 can close.

Accepted P5R-T14 evidence: final PR head `c82e0276b57773d1524b47eb581c3a1a3bef520d` passed all five required jobs in run #469 / `35511357500`, including P5-T03 resource ownership, P5-T04 bounded dynamic upload, and P5-T18 integration. PR #333 merged as `750e36a678ef70e497d019beafa0c0bf97d56324`; exact merged-master Lightweight verification passed in run #470 / `35511620539`.

Wiki impact: none — supported renderer API and consumer usage are unchanged. Sandbox impact: none — owner-facing behavior and controls are unchanged.


## P5R-T15 debug/view-model internal verification

Issue #275 is a package-private responsibility/naming refactor. `DebugLineVertexPacker`, `DebugLineRenderer`, and `ViewModelRenderer` remain canonical; `ViewModelProjection` becomes `ViewModelProjectionFactory`; and the fixed six-vertex validation fixture packing moves to `ViewModelFixtureVertexPacker`.

Focused verification:

```powershell
.\gradlew.bat :engine-render-opengl:test --tests "com.samo.engine.render.opengl.internal.DebugLineVertexPackerTest" --rerun-tasks
.\gradlew.bat :engine-render-opengl:test --tests "com.samo.engine.render.opengl.internal.ViewModelFixtureVertexPackerTest" --rerun-tasks
.\gradlew.bat :engine-render-opengl:test --tests "com.samo.engine.render.opengl.internal.ViewModelProjectionFactoryTest" --rerun-tasks
.\gradlew.bat :engine-render-opengl:test --tests "com.samo.engine.render.opengl.internal.ReferenceSceneRendererTest" --rerun-tasks
.\gradlew.bat :engine-render-opengl:test --rerun-tasks
.\gradlew.bat :engine-render-opengl:validateGlsl --rerun-tasks
.\gradlew.bat :engine-render-opengl:verifyPublicApiBoundary --rerun-tasks
.\gradlew.bat :test-support:test --tests "com.samo.architecture.ModulePackageBoundaryTest" --rerun-tasks
.\gradlew.bat resolveAndLockAllDependencies
```

Source review must confirm no production/test reference remains to `ViewModelProjection`, debug packing remains unchanged, and the extracted fixture bytes remain the same six positions with fixed linear RGB `0.95/0.55/0.15`. Spatial review must confirm D-041/D-045 conventions plus D-065 55° vertical FOV, framebuffer aspect, 0.01 m near, 10 m far, identity view, conventional OpenGL depth, and world -> debug -> depth-reset -> view-model ordering remain unchanged.

Because Java/test source changes, the exact final PR head requires the normal five-job heavy matrix including Windows native P5-T16/P5-T17/P5-T18 regressions. After merge, the exact merged `master` SHA requires Lightweight master verification before Issue #275 can close.

Accepted P5R-T15 evidence: final PR head `96510a58644eea158c75c3327d3ac1959c0ab9ef` passed all five required jobs in run #471 / `35512198379`, including P5-T16 debug geometry, P5-T17 view-model, and P5-T18 integration. PR #335 merged as `9c41a1db72834d162f58f503a9d03aa0fd00add3`; exact merged-master Lightweight verification passed in run #472 / `35512467254`.

Wiki impact: none — supported debug/render APIs and consumer usage are unchanged. Sandbox impact: none — observable debug/view-model behavior and controls are unchanged.


## P5R-T16 SandboxMain decomposition verification

Issue #276 decomposes only `game-sandbox` package-private responsibilities. Public `SandboxMain`, the `runSandbox` task, owner controls, README instructions, camera/focus behavior, renderer usage, diagnostics, and shutdown behavior remain unchanged. T17-owned compatibility/naming cleanup is explicitly deferred.

Focused verification:

```powershell
.\gradlew.bat :game-sandbox:test --tests "com.samo.game.sandbox.SandboxCameraTest" --rerun-tasks
.\gradlew.bat :game-sandbox:test --tests "com.samo.game.sandbox.SandboxControlsTest" --rerun-tasks
.\gradlew.bat :game-sandbox:test --tests "com.samo.game.sandbox.SandboxDiagnosticFormatterTest" --rerun-tasks
.\gradlew.bat :game-sandbox:test --tests "com.samo.game.sandbox.SandboxFramebufferSizeTest" --rerun-tasks
.\gradlew.bat :game-sandbox:test --rerun-tasks
.\gradlew.bat :game-sandbox:classes --rerun-tasks
.\gradlew.bat :engine-render-opengl:verifyPublicApiBoundary --rerun-tasks
.\gradlew.bat :test-support:test --tests "com.samo.architecture.ModulePackageBoundaryTest" --rerun-tasks
.\gradlew.bat resolveAndLockAllDependencies
```

Direct sandbox unit tests remain inside the existing test dependency boundary. `engine-platform-lwjgl` and `engine-render-opengl` are intentional `compileOnly` / sandbox-runtime dependencies for `game-sandbox`; T16 does not add Gradle test dependencies just to reference extracted package-private collaborators from tests. The preserved camera/control/diagnostic formatter tests, the new framebuffer-state test, full sandbox tests/classes, full repository build, and heavy CI remain the bounded verification set.

At T16 acceptance, source review confirmed `SandboxMain` no longer owned the per-frame loop, owner-control state application, fixed-scene construction, periodic diagnostic state, or nested framebuffer-size state; the then-existing `EngineDemoMain`, `runEngineDemo`, `SandboxControls.Action/Input`, sandbox resource paths, build dependencies, and owner-facing controls were unchanged by T16. P5R-T17 subsequently owns the compatibility removal and nested control-name cleanup.

Spatial review must confirm the D-041/D-045 camera basis is unchanged, W/A/S/D plus mouse LOOK still flow through `PlayerInputCommand` -> `SandboxCamera`, and the world projection remains 70° vertical FOV, framebuffer aspect, 0.1 m near, and 100 m far. Scene review must retain the same fixed local lights, debug primitive order/values/colors, and debug text-counter order. Diagnostic review must retain field order/wording/meaning, mouse-delta reset only after publication, deadline catch-up semantics, and the explicit non-FPS/non-benchmark qualifier.

Because Java/test source changes, the exact final PR head requires the normal five-job heavy matrix. The Windows native Phase 5 integration remains a regression check for sandbox-driven camera/render assumptions. After merge, the exact merged `master` SHA requires Lightweight master verification before Issue #276 can close.

Accepted P5R-T16 evidence: initial head `58a6ee520f3f3c9ebdce7a7611da2b06ea2f49e6` failed Unit tests in run #473 / `35514092541` because added direct tests crossed the existing sandbox compile-only dependency boundary; the tests were removed without changing Gradle/dependency metadata, so that run is superseded. Corrected final head `8705d6b031b9bb437c74405f5963a60aa731b175` passed all five required jobs in run #479 / `35514285664`, including Windows native P5-T16/P5-T17/P5-T18 regressions. PR #337 merged as `c4d9675e7c03cf046bc684b668ac1117af0f23fe`; exact merged-master Lightweight verification passed in run #480 / `35514559730`.

Wiki impact: none — no supported public engine API or consumer usage changes. Sandbox impact: structural only — `runSandbox`, controls, README instructions, output, and observable behavior remain unchanged, so no owner-facing README content change is required.


## P5R-T17 sandbox naming and compatibility verification

Issue #277 removes only the verified-obsolete sandbox compatibility entry point/task and applies a naming-only cleanup to nested owner-control types. Canonical `SandboxMain` / `runSandbox`, controls, runtime behavior, resources, diagnostics, and all T16 helper responsibilities remain unchanged.

Focused verification:

```powershell
.\gradlew.bat :game-sandbox:test --tests "com.samo.game.sandbox.SandboxControlsTest" --rerun-tasks
.\gradlew.bat :game-sandbox:test --tests "com.samo.game.sandbox.SandboxCameraTest" --rerun-tasks
.\gradlew.bat :game-sandbox:test --tests "com.samo.game.sandbox.SandboxDiagnosticFormatterTest" --rerun-tasks
.\gradlew.bat :game-sandbox:test --tests "com.samo.game.sandbox.SandboxFramebufferSizeTest" --rerun-tasks
.\gradlew.bat :game-sandbox:test --rerun-tasks
.\gradlew.bat :game-sandbox:classes --rerun-tasks
.\gradlew.bat :game-sandbox:tasks --all
.\gradlew.bat :test-support:test --tests "com.samo.architecture.ModulePackageBoundaryTest" --rerun-tasks
.\gradlew.bat resolveAndLockAllDependencies
```

Source/reference review must prove that `EngineDemoMain.java` and the live `runEngineDemo` Gradle task are gone, `runSandbox` still targets `com.samo.game.sandbox.SandboxMain`, no non-historical documentation claims the alias still exists, and no `SandboxControls.Action` / `SandboxControls.Input` references remain. Historical P3-T04A evidence may still mention the old command when explicitly labeled historical.

Because Java/test/build source changes, the exact final PR head requires the normal five-job heavy matrix. After merge, the exact merged `master` SHA requires Lightweight master verification before Issue #277 can close.

Accepted P5R-T17 evidence: final PR head `31172c12de2eb1a5c78d75a74b1a58268f67faa3` passed all five required jobs in run #482 / `35518803707`, including Windows native P5-T16/P5-T17/P5-T18 regressions. Earlier run #481 / `35518792283` was cancelled after a documentation-only head advance and is not acceptance evidence. PR #339 merged as `62e9bbd1683557193a6afe27e9b08fbacc32212a`; exact merged-master Lightweight verification passed in run #483 / `35519165178`.

Wiki impact: none — the removed surface is a sandbox executable compatibility alias, not supported engine consumer API. Sandbox impact: launch guidance now documents only the canonical `runSandbox`; controls/output/capabilities remain unchanged.


## P5R-T18 renderer visual-demo decomposition verification

Issue #278 decomposes only the standalone renderer `visualDemo` source set. Public `RendererVisualDemo`, `:engine-render-opengl:runRendererVisualDemo`, production renderer source/API, dependencies, and module edges remain unchanged.

Run the focused deterministic suites:

```powershell
.\gradlew.bat :engine-render-opengl:test --tests "com.samo.engine.render.opengl.internal.AnimatedDemoLightingTest" --tests "com.samo.engine.render.opengl.internal.RendererVisualDemoFramebufferSizeTest" --tests "com.samo.engine.render.opengl.internal.RendererVisualDemoLoopTest" --rerun-tasks
.\gradlew.bat :engine-render-opengl:test --rerun-tasks
.\gradlew.bat :engine-render-opengl:check --rerun-tasks
.\gradlew.bat check
```

`AnimatedDemoLightingTest` protects the accepted two-light ordering, trajectories, normalized spot direction, exact initial intensity/range values, seven debug primitives, and terminal debug ray. `RendererVisualDemoFramebufferSizeTest` proves logical-size callbacks do not replace framebuffer dimensions while framebuffer callbacks replace both values. `RendererVisualDemoLoopTest` protects the pre-existing saturating elapsed-time arithmetic moved out of the entry point.

The normal exact-head five-job PR matrix remains required because Java source/tests changed. After merge, require the Lightweight master verification on the exact merge SHA before Issue #278 closes. The standalone interactive demo is owner-observation only and does not replace automated/native renderer acceptance.

Accepted P5R-T18 verification evidence: the connected agent environment could not clone the repository because outbound DNS resolution for `github.com` failed, so no local Gradle result is claimed. Initial run #484 / `35523166142` exposed a malformed literal `\\n` in `AnimatedDemoLightingTest`; that candidate is superseded. Corrected final head `5f6ff0c1dfb05a078486abf05f4f10860f14dac4` passed Build and quality gates, Unit tests, Architecture tests, JaCoCo coverage reports, and Windows native smoke in run #485 / `35523275737`. PR #341 merged as `9fa3a5c831cd0d9884b7b6a89c26029a3d41a6dd`, and exact-merge Lightweight master verification passed in run #486 / `35523595439`.


## P5R-T19 client/server version-report naming verification

Issue #279 is a naming-only executable cleanup. `ClientMain`, `ServerMain`, `runClient`, `runServer`, version metadata generation, compatibility keys, and module dependencies remain unchanged. The old internal helper names are replaced by `ClientVersionReport` and `ServerVersionReport`.

Focused verification:

```powershell
.\gradlew.bat :game-client:classes :game-server:classes --rerun-tasks
.\gradlew.bat :game-client:runClient
.\gradlew.bat :game-server:runServer
.\gradlew.bat -q :game-client:runClient --args="--version"
.\gradlew.bat -q :game-server:runServer --args="--version"
.\gradlew.bat :game-server:verifyHeadlessServerRuntime
.\gradlew.bat :test-support:test --tests "com.samo.architecture.ModulePackageBoundaryTest" --rerun-tasks
.\gradlew.bat resolveAndLockAllDependencies
```

The final non-Markdown PR must pass the repository five-job matrix. Its Build job directly executes both default entry points, the headless server boundary, and the existing end-to-end version-report verifier requiring the six nonblank keys `executable`, `engineCommit`, `protocolVersion`, `assetVersion`, `javaVersion`, and `nativeLibraries`; shared client/server values must match and `engineCommit` must equal the exact GitHub candidate SHA. After merge, exact merged `master` must pass Lightweight verification before Issue #279 closes.

Wiki impact: none — no supported engine API changes.
Sandbox impact: none — no sandbox behavior changes.

Accepted P5R-T19 verification evidence: final PR head `47ae52f78f9f9d99d462975266337025b84ae9f6` passed Build and quality gates, Unit tests, Architecture tests, JaCoCo coverage reports, and Windows native smoke in run #487 / `35524411941`. The Build job passed both default executable runs, `verifyHeadlessServerRuntime`, and the end-to-end six-key version-report verifier. PR #343 merged as `7215fc12c123325adb62521e971f5dc4965064d7`; exact merged-master Lightweight verification passed in run #488 / `35524646827`, including committed dependency locks, headless-server runtime isolation, and exact-merge version reporting.


## P5R-T20 feasibility-spike naming/isolation verification

Issue #280 keeps the Phase 0/P1 feasibility module experimental. The only Java rename is `IntegratedNativeSoakSpike` -> `IntegratedNativeEvidenceHarness`; both integrated native Gradle task names, P0-T12/P0-T13 durations, evidence labels, JFR paths, external dependency versions, and runtime behavior remain unchanged.

Focused verification:

```powershell
.\gradlew.bat :feasibility-spikes:classes --rerun-tasks
.\gradlew.bat :feasibility-spikes:check --rerun-tasks
.\gradlew.bat :feasibility-spikes:tasks --all
.\gradlew.bat :test-support:test --tests "com.samo.architecture.ModulePackageBoundaryTest" --rerun-tasks
.\gradlew.bat resolveAndLockAllDependencies
```

`:feasibility-spikes:check` includes `verifyFeasibilitySpikeIsolation`, which fails when any other declared subproject configuration contains a Gradle `ProjectDependency` targeting `:feasibility-spikes`. Compilation protects the renamed entry class/wiring. Architecture verification remains a source-level cross-module implementation-boundary check. Repository search must additionally confirm no production Java references `com.samo.spike.*` and no non-historical live reference remains to `IntegratedNativeSoakSpike`.

Do not run P0-T13's 900-second sustained evidence merely for this naming/isolation refactor; T20 creates no new native-stability claim. The ordinary exact-head five-job PR matrix remains required because Java/build source changes. After merge, the exact merged `master` SHA requires Lightweight verification before Issue #280 closes.

Wiki impact: none — no supported engine API changes.
Sandbox impact: none — no sandbox behavior changes.


Accepted P5R-T20 verification evidence: final PR head `244041127c1851d477f557c92734c981ee5649b7` passed Build and quality gates, Unit tests, Architecture tests, JaCoCo coverage reports, and Windows native smoke in run #489 / `35525360797`. The Build job passed `Build all modules`, which executes the experimental module's `check` and therefore `verifyFeasibilitySpikeIsolation`; the Windows native job also passed the retained root GLFW/OpenAL lifecycle smoke alias. PR #345 merged as `510e61d6d44eab4cb986d5c03078138aaa40a020`, and exact merged-master Lightweight verification passed in run #490 / `35525718982`. No P0-T13 900-second sustained run is claimed or required by this naming/isolation refactor.


## P5R-T21 public API naming audit verification

Issue #301 is an audit-only public naming pass. Fresh review of the supported P1-P5 API roots records 60 public top-level types plus 15 supported nested public types in `docs/refactor/PUBLIC_API_NAMING_AUDIT.md`, including their declared public constructors/operations/queries/constants, implicit record accessors, interface methods/defaults, and enum vocabularies.

The current candidate selects no rename and changes no Java, Gradle, resource, workflow, dependency, wiki, or sandbox path. Verification is therefore documentary/source consistency rather than a new runtime claim:

- compare the audit against current source under `com.samo.engine.core.api`, `com.samo.engine.platform.api`, and `com.samo.engine.render.api`;
- reconcile with `docs/refactor/NAMING_STANDARD.md`, `SYMBOL_INVENTORY.md`, `BOUNDARY_AUDIT.md`, accepted decisions, and `docs/SPATIAL_CONVENTIONS.md`;
- compare source names and consumer meaning with `wiki/API_INDEX.md`, `wiki/LIMITATIONS.md`, relevant usage pages, and current sandbox imports/usage;
- search for stale pre-D-066 lifecycle names and treat only explicit historical/rename-provenance references as valid;
- inspect the complete PR file list before using the Markdown-only exemption.

If every changed path ends in `.md`, the `AGENTS.md` Markdown-only CI exemption applies: no heavy five-job PR matrix and no post-merge Lightweight verifier are required. The absence of those runs is expected and must not be reported as a test pass. If any non-Markdown path appears, the exemption immediately ceases to apply.

Wiki impact: none — no supported public API name/signature/usage changes.
Sandbox impact: none — no capability or owner-facing usage changes.


Accepted P5R-T21 verification evidence: final audit head `32886cb82e69c199ed6747630aa42b12ac55b125` was merged through PR #347 as `d79f6d6c18490c839157770e51e9908eb2f7e13d`. The complete PR diff contained exactly 8 changed paths and every path ended in `.md`, so the documented Markdown-only exemption applied. No heavy PR CI or post-merge Lightweight run was required or claimed. Source/wiki/boundary review found no justified public rename and no source/wiki/sandbox/runtime change.


## P5R-T22 test/fixture/evidence naming verification

Issue #302 changes no production code. Fresh audit covers all 99 current test classes and all current `@Test` method names. Existing test vocabulary already matches accepted P5R production names; the only justified change is the negative GLSL fixture `shaders/p5/broken.frag` -> `shaders/p5/invalid-syntax.frag` plus `rejectsInvalidSyntaxFixtureWithDiagnostics`.

Focused verification:

```powershell
.\gradlew.bat :engine-render-opengl:test --tests "com.samo.engine.render.opengl.internal.GlslOfflineValidationTest" --rerun-tasks
.\gradlew.bat :engine-render-opengl:test --rerun-tasks
.\gradlew.bat check
.\gradlew.bat resolveAndLockAllDependencies
```

Repository search must find no live `broken.frag` reference. Historical `SHERKO_P*_*` environment variables, phase/task report/capture filenames, workflow artifact names, and phase-gate class names remain unchanged because they are evidence provenance rather than current implementation vocabulary.

Because Java test/resource paths change, the exact final PR head requires the normal five-job matrix. After merge, the exact merged `master` SHA requires Lightweight verification before Issue #302 closes.

Wiki impact: none — no public API or consumer usage changes.
Sandbox impact: none — no owner-facing behavior changes.


Accepted P5R-T22 verification evidence: final head `5bda2120526a40c4176aa161ba1492060118860f` passed Build and quality gates, Unit tests, Architecture tests, JaCoCo coverage reports, and Windows native smoke in run #491 / `35530836855`. The accepted resource rename preserved identical fixture bytes/Git blob `fe6d5138a34d2a8903ab88b73b54110b25b6adeb`. PR #349 merged as `387549c83ffa6af9d8d69b0373772e84ac23053c`; exact merged-master Lightweight verification passed in run #492 / `35532808303`. Historical CI/evidence identifiers remained unchanged.


## P5R-T23 internal package audit verification

Issue #303 is documentation/source-analysis only. The final candidate proposes no Java/package/build/resource/workflow/wiki/sandbox change.

Verification consists of:

- recursive production Java tree inventory of explicit `.internal` roots;
- declaration review of the 11 platform implementation collaborators deferred to T23, confirming they remain package-private;
- direct facade/helper reference review for `GlfwWindow` and `InputActionBindings`;
- renderer internal source-reference analysis showing all 42 production internal types are one connected component;
- visual-demo source review confirming package-private dependency on production renderer internals;
- client/server internal-root inventory;
- complete PR file-list inspection before using the Markdown-only exemption.

If every final changed path ends in `.md`, the `AGENTS.md` Markdown-only exemption applies: no five-job heavy PR matrix and no post-merge Lightweight verifier are required. No unrun Gradle/runtime command is claimed as passing.

Wiki impact: none — no public package or usage changes.
Sandbox impact: none — no owner-facing behavior changes.


Accepted P5R-T23 verification evidence: final audit head `d6ce4a096c47ec4226fc070ff65f2a76eba4486e` merged through PR #351 as `36352d874c04c383ce53527001e279f7634f973b`. The complete PR diff contained 9 Markdown paths only, so the documented Markdown-only exemption applied. No heavy PR CI or post-merge Lightweight run was required or claimed. The audit introduced no Java/package/build/runtime change.


## P5R-T24 pattern/scalability hardening verification

Issue #304 adds package-private `OpenGlBackendSet` and replaces the renderer's repeated resource/draw/reflection backend parameter cluster with that composition object. The individual backend interfaces, LWJGL implementations, public renderer API, native behavior, module edges, dependencies, and workflows remain unchanged.

Focused verification:

```powershell
.\gradlew.bat :engine-render-opengl:test --tests "com.samo.engine.render.opengl.internal.OpenGlBackendSetTest" --rerun-tasks
.\gradlew.bat :engine-render-opengl:test --tests "com.samo.engine.render.opengl.internal.ReferenceSceneRendererTest" --rerun-tasks
.\gradlew.bat :engine-render-opengl:test --rerun-tasks
.\gradlew.bat check
.\gradlew.bat resolveAndLockAllDependencies
```

`OpenGlBackendSetTest` verifies injected adapter identity, null rejection, and that `production()` composes the current LWJGL resource/draw/reflection adapters. Existing `ReferenceSceneRendererTest` coverage verifies resource ownership, draw/state order, shader/uniform behavior, diagnostics, local-light limits, failure cleanup, sRGB presentation, view-model/debug rendering, and test-fake injection through the new set.

Because production Java and tests change, the exact final PR head requires the normal five-job matrix. After merge, the exact merged `master` SHA requires Lightweight verification before Issue #304 closes.

Wiki impact: none — no supported API/usage changes.
Sandbox impact: none — no owner-facing behavior changes.


Accepted P5R-T24 verification evidence: final head `aa43d2fee5e4f4f08723ba1323232eb7c8dc5005` passed Build and quality gates, Unit tests, Architecture tests, JaCoCo coverage reports, and Windows native smoke in run #493 / `35534567287`. PR #353 merged as `3d7a3fbf302b1b4caf46ca780aeb68f651810fa1`; exact merged-master Lightweight verification passed in run #494 / `35534881306`. The accepted `OpenGlBackendSet` composition introduced no public API, module/dependency, workflow, native behavior, shader ABI, wiki, or sandbox change.


## P5R-T25 final cleanup audit

Issue #305 is a repository-wide final stale-name/dead-code/compatibility/refactor-scaffolding audit after P5R-T01 through T24.

Verification is static/reference based because the refined candidate changes documentation only and does not modify executable behavior:

- search for every obsolete identifier listed in `docs/refactor/FINAL_CLEANUP_AUDIT.md` and classify remaining hits as intentional historical provenance or current-name containment;
- confirm removed `EngineDemoMain` / `runEngineDemo` and superseded fixture/type names are absent from live source/resources/tasks;
- confirm retained P5R helpers have current production/test consumers as appropriate;
- search authored Java for `@Deprecated` compatibility shims;
- inspect README/status/roadmap/backlog/refactor records for consistent T25/T26/P6 ordering;
- inspect the complete PR changed-file list and require every changed path to end in `.md`.

If those conditions hold, the AGENTS.md Markdown-only exemption applies. Do not run or claim the five-job heavy matrix or exact-merge Lightweight verification for this candidate; record them as not required by policy. If any non-Markdown path enters the complete diff, the exemption no longer applies and the normal full verification lifecycle becomes mandatory.

Wiki impact: none — no supported API/usage change.
Sandbox impact: none — no capability or owner-facing behavior change.


Accepted P5R-T25 verification evidence: final audit head `12681d0a1261926a32961d86ee541c1376e7ff7f` merged through PR #355 as `9ab1ef0d449c7ee5c390767c3ba0b58f967b7c6a`. The complete PR diff contained 8 Markdown paths only. Repository search/reference review found no live stale implementation name, no authored-Java `@Deprecated` compatibility shim, no orphaned P5R helper, and no avoidable compatibility alias. The documented Markdown-only exemption therefore required neither the heavy PR matrix nor post-merge Lightweight verification; neither unrun check is claimed as passing.


## P5R-T26 Phase 5R exit review

Issue #306 is the mandatory Phase 5R gate before P6-T01 may be materialized. The durable review record is `docs/refactor/PHASE_5R_EXIT_REVIEW.md`.

The candidate intentionally includes `docs/refactor/P5R_EXIT_REVIEW_EVIDENCE.txt`, a non-executable text manifest. Because the complete diff is therefore not Markdown-only, the normal PR workflow must run rather than being suppressed by `paths-ignore`.

Required final-candidate evidence:

1. Confirm P5R-T01 through T25 are accepted and no open predecessor maintenance Issue restores deferred behavior.
2. Reconcile the current `:game-sandbox:runSandbox` -> `SandboxMain` and `:engine-render-opengl:runRendererVisualDemo` -> `RendererVisualDemo` wiring, README guidance, deterministic sandbox/visual-demo tests, visual-demo compilation, and retained Phase 5 native integration. Interactive execution may be recorded only if actually performed.
3. Reconcile public/internal/module/native-ownership/spatial/persisted/config/wire/protocol boundaries against current source/tests/decisions/audits.
4. Review Phase 6 assumptions without implementing or materializing P6 work.
5. Open the final non-draft PR and require all five workflow jobs on its exact head/base: Build and quality gates, Unit tests, Architecture tests, JaCoCo coverage reports, Windows native smoke.
6. Merge only that verified exact candidate.
7. Require Lightweight master verification on the exact merge SHA.
8. Only then change the review disposition from PENDING to PASS, mark T26 accepted, close Issue #306, and unblock P6-T01 for fresh materialization/refinement.

Any discovered source/behavior/contract defect blocks PASS and must be split into a separate bounded maintenance Issue rather than repaired inside T26.

Wiki impact: none expected — review/status only.
Sandbox impact: review only — no source/behavior change authorized.


Accepted P5R-T26 Phase 5R exit evidence: final candidate `40f2bb91ae3afeeee07d7f8be92dd7b088419fa0` passed Build and quality gates, Unit tests, Architecture tests, JaCoCo coverage reports, and Windows native smoke in PR run #495 / `35537699489`. PR #357 merged as `5e0cac4e7748a66b7c2d19e0cf444eaca0a49fce`; exact merged-master Lightweight verification passed in run #496 / `35537989459`, including committed dependency locks, headless-server runtime isolation, and exact-merge client/server version reporting. Phase 5R exit result: PASS. Interactive sandbox/visual-demo windows were not executed in the connected review environment and are not claimed as manual visual evidence; entry wiring, deterministic tests/compilation, and retained native renderer regressions were reconciled and verified.


## Java formatting

The repository-wide Java formatter is Spotless `8.10.2` using Eclipse JDT formatter `4.40` with the committed profile `config/formatter/sherko-eclipse-java.xml`.

| Purpose | Windows command | Expected evidence |
| --- | --- | --- |
| Apply canonical Java formatting | `.\gradlew.bat spotlessApply` | All authored Java is rewritten to the committed compact Sherko Engine style. |
| Verify committed Java formatting | `.\gradlew.bat spotlessCheck` | No Java file differs from formatter output. |
| Verify through the normal quality gate | `.\gradlew.bat check` | `spotlessCheck` runs together with the existing Checkstyle and architecture/source-boundary checks. |

The formatter covers production, tests, visual-demo sources, public-API tests, feasibility spikes, and committed Java fixtures while excluding generated `build/` and Gradle working directories.

PRE-P6 Issue #359 bootstrap evidence: after repository-wide normalization, a clean verification-only run confirmed `spotlessApply` idempotence, `spotlessCheck` success, and lexical equivalence for all 241 changed Java files after ignoring whitespace/comments while preserving literal/text-block contents. The earlier text-block mismatch in `MaterialComparisonOverlay` was rejected and corrected by setting Eclipse `text_block_indentation=3` (preserve) and restoring the original shader text blocks before the clean run.


Accepted PRE-P6 Issue #359 evidence: PR #360 exact head `d13d78995f37701692c74f0197d9151c0108a414` passed the full five-job matrix in run #501 / `35541101685`. The PR merged as `4425e5eb9c1d7faf4706946b28443e59f9c7f5c5`; exact-merge Lightweight master verification passed in run #502 / `35541422410`. Prior verification-only bootstrap run #6 proved `spotlessApply` idempotence, `spotlessCheck` success, and lexical equivalence for all 241 changed Java files. The temporary bootstrap workflow was removed before PR #360. Canonical formatting commands remain `.\gradlew.bat spotlessApply` and `.\gradlew.bat spotlessCheck`.


## P6-T01 stable AssetId verification

Issue #362 introduces the first production `engine-assets` API: path-independent 128-bit `AssetId`.

Focused Windows verification:

```powershell
.\gradlew.bat spotlessApply
.\gradlew.bat :engine-assets:test --tests "com.samo.engine.assets.api.AssetIdTest" --rerun-tasks
.\gradlew.bat spotlessCheck
.\gradlew.bat check
.\gradlew.bat resolveAndLockAllDependencies
```

The focused test must prove exact 128-bit canonical text round-trip, generated canonical text, deterministic rejection of null/noncanonical text, validity of the all-zero value, and the P6-T01 acceptance scenario: changing a metadata-like source path while retaining the same `AssetId` leaves an existing identity reference unchanged. The fixture is test-only and must not become a P6-T02 metadata implementation.

Because P6-T01 adds public Java source/tests and consumer documentation, the final candidate requires the normal five-job PR matrix on the exact head: Build and quality gates, Unit tests, Architecture tests, JaCoCo coverage reports, and Windows native smoke. After merge, require Lightweight master verification on the exact merge SHA before closing Issue #362.

Connector execution note: the GitHub connector environment cannot execute the Gradle wrapper directly. Focused commands must therefore be recorded as not locally executed unless run by an available repository workflow; the exact final-candidate GitHub Actions matrix remains mandatory and is not replaced by source inspection.


Accepted P6-T01 verification evidence: after the initial candidate correctly failed Spotless, the formatter-only correction produced final PR #363 head `ae34d0cd84be2899fecd821f9af485a6f4c1f084`. That exact head passed Build and quality gates, Unit tests, Architecture tests, JaCoCo coverage reports, and Windows native smoke in run #504 / `35698629954`. PR #363 merged as `37f3c163d2bb492ee08f6306e07b017c6ea26119`; exact-merge Lightweight master verification passed in run #505 / `35700148367`. P6-T01 acceptance is therefore complete. The connector environment did not separately execute the documented focused Gradle command locally; the passing repository-wide Unit tests and quality gates include the committed `AssetIdTest` and Spotless enforcement.


## P6-T02 source metadata verification

Issue #365 defines strict versioned source metadata loading in `engine-assets`.

Focused Windows verification:

```powershell
.\gradlew.bat spotlessApply
.\gradlew.bat :engine-assets:test --tests "com.samo.engine.assets.api.SourceAssetMetadataTest" --rerun-tasks
.\gradlew.bat spotlessCheck
.\gradlew.bat check
.\gradlew.bat resolveAndLockAllDependencies
```

Focused acceptance covers all eight schema-v1 asset types, exact persisted P6-T01 identity, field-order independence, unsupported-version `upgrade required` failure, malformed/missing/unknown/duplicate/wrong-type fields, noncanonical IDs, unknown asset types, missing files, null path, and direct-construction validation. The test oracle uses handwritten schema-v1 JSON and explicit expected values/messages rather than production serialization output.

Dependency review must confirm `engine-assets` reuses repository-selected Jackson 2.21.2 as implementation-only, no version-catalog drift occurs, and only lock configurations that newly resolve Jackson change. No P6-T03+ cooker/import/runtime behavior is authorized.

Because P6-T02 adds public Java API, persisted-format semantics, implementation dependency ownership, tests, locks, and wiki guidance, the final candidate requires the normal exact-head five-job PR matrix. After merge, exact merged `master` requires Lightweight verification before Issue #365 closes.

Connector execution note: this GitHub-only environment cannot run the Gradle wrapper directly; focused commands must be recorded as not locally executed unless repository CI executes equivalent coverage. Exact final-candidate CI remains mandatory.


Accepted P6-T02 verification evidence: the initial PR candidate failed only the Spotless formatting gate; the formatter-only correction produced final PR #366 head `ab15ccb5f1b03f9b2d655ce846afb6f849f87e4b`. That exact head passed Build and quality gates, Unit tests, Architecture tests, JaCoCo coverage reports, and Windows native smoke in run #507 / `35701724532`. The Build job also resolved the committed dependency locks successfully, confirming the expected existing Jackson 2.21.2 graph changes without lock drift. PR #366 merged as `f91956c7d455b01d0895ca787bb110b802083ec1`; exact-merge Lightweight master verification passed in run #508 / `35702272507`, including committed dependency locks, the headless-server runtime boundary, and exact-merge client/server version reporting. P6-T02 acceptance is therefore complete. The connector environment did not separately execute the documented focused Gradle command locally; the passing repository-wide Unit tests and quality gates include `SourceAssetMetadataTest` and Spotless enforcement.


## P6-T03 asset cooker verification

Issue #368 introduces the deterministic command-line cooker foundation in `engine-assets`.

Focused Windows verification:

```powershell
.\gradlew.bat spotlessApply
.\gradlew.bat :engine-assets:test --tests "com.samo.engine.assets.internal.AssetCookerTest" --rerun-tasks
.\gradlew.bat :engine-assets:runAssetCooker --args="<fixture-input> <fresh-fixture-output>"
.\gradlew.bat spotlessCheck
.\gradlew.bat check
.\gradlew.bat resolveAndLockAllDependencies
```

Focused acceptance covers exact CLI arity, valid recursive sidecar discovery, ignored unpaired ordinary files, missing/non-directory input, pre-existing output preservation, input/output overlap, empty metadata sets, missing source pairs, zero-byte sources, malformed/unsupported metadata, duplicate identities, deterministic AssetId-sorted manifest bytes across repeated clean cooks, byte-for-byte opaque payload copying, actual positive emitted byte size, and deterministic cleanup of a newly created output tree through the package-private filesystem seam.

Review must confirm no dependency-lock drift, no new module edge/dependency, no P6-T04+ importer/coordinate/tangent/final-binary/decode/runtime-resource behavior, and no public Java API change. Because P6-T03 changes executable Java/build behavior and introduces durable persisted cache/manifest conventions, the final candidate requires the normal exact-head five-job PR matrix. After merge, exact merged `master` requires Lightweight verification before Issue #368 closes.

Connector execution note: this GitHub-only environment cannot run the Gradle wrapper directly; focused commands are not locally claimed as passing unless repository CI executes equivalent coverage. Exact final-candidate CI remains mandatory.


Accepted P6-T03 verification evidence: two earlier PR candidates failed only the repository Spotless formatting gate and became obsolete after formatter-only corrections. Final PR #369 head `1843321e380302c9748fd55a61db5353076c6e3e` passed Build and quality gates, Unit tests, Architecture tests, JaCoCo coverage reports, and Windows native smoke in run #511 / `35705042938`. The Build job resolved committed dependency locks successfully with no lockfile drift or new dependency/module edge. PR #369 merged as `3d73c0de0cc534a95021b38560f8690e24a18065`; exact-merge Lightweight master verification passed in run #512 / `35705539253`, including committed dependency locks, the headless-server runtime boundary, and exact-merge client/server version reporting. P6-T03 acceptance is therefore complete. The connector environment did not separately execute the documented focused Gradle cooker command locally; repository-wide tests/quality gates include `AssetCookerTest`, Spotless enforcement, and the committed `runAssetCooker` build configuration.


## P6-T04 Assimp glTF mesh import verification

Issue #371 activates the scope-locked LWJGL 3.4.3 Assimp binding inside the offline `engine-assets` cooker.

Focused Windows verification:

```powershell
.\gradlew.bat spotlessApply
.\gradlew.bat :engine-assets:test --tests "com.samo.engine.assets.internal.AssimpGltfMeshImporterTest" --tests "com.samo.engine.assets.internal.AssetCookerTest" --rerun-tasks
.\gradlew.bat spotlessCheck
.\gradlew.bat check
.\gradlew.bat resolveAndLockAllDependencies
```

Focused acceptance uses committed self-contained glTF fixtures with independently authored inputs and independently derived Assimp import-basis expectations. The reference triangle's authored indices `[2,0,1]` imply Assimp first-use remapping `[2,0,1]`; expected imported positions are `(7,-8,9), (1,2,3), (-4,5,6)`, normals remain +Z, tangent xyz remains +X, UV0 becomes `(1,1), (0.25,0.25), (0.5,0.875)` after Assimp's intrinsic `v = 1 - v` normalization, mesh name remains `ReferenceTriangle`, and imported indices are `[0,1,2]`. Additional coverage verifies optional normals/tangents/UV0 remain absent rather than generated, unsupported MESH extensions fail before output creation, malformed/no-mesh input is path-diagnostic, non-triangle topology is rejected without triangulation, non-MESH P6-T03 pass-through remains valid, and successful cooker output still writes the original source bytes until P6-T07.

Dependency review must confirm only existing LWJGL 3.4.3 core/Assimp modules and Windows native artifacts are added to `engine-assets` lock configurations, with no new module edge or unrelated version drift. Spatial review must confirm `aiImportFile(..., 0)` remains free of post-process flags, only the documented intrinsic glTF vertex-remap/UV-origin normalization is accepted, and no P6-T05 spatial coordinate/unit conversion is present.

Because P6-T04 adds native importer execution, dependency/lock changes, Java/tests/resources, and a durable source-basis boundary, the final candidate requires the normal exact-head five-job PR matrix. After merge, exact merged `master` requires Lightweight verification before Issue #371 closes.

Connector execution note: this GitHub-only environment cannot run the Gradle wrapper directly; focused commands are not locally claimed as passing unless repository CI executes equivalent coverage. Exact final-candidate CI remains mandatory.


Accepted P6-T04 verification evidence: after earlier candidate corrections for Assimp import-basis expectations, transitive dependency locks, and Spotless formatting, final PR #372 head `1d3ac786664f8e4ea1f892f600e4ed42326febb3` passed Build and quality gates, Unit tests, Architecture tests, JaCoCo coverage reports, and Windows native smoke in run #531 / `35712370651`. The passing Build job verified the reconciled dependency locks, and the passing Unit/native jobs exercised the LWJGL Assimp path and committed glTF/GLB fixtures. PR #372 merged as `cf034184495f0e914032d330eca85dd869694d71`; exact-merge Lightweight master verification passed in run #532 / `35713724011`, including committed dependency locks, the headless-server runtime boundary, and exact-merge client/server version reporting. P6-T04 acceptance is therefore complete. The connector environment did not separately execute the documented focused Gradle commands locally; the accepted repository CI is the execution evidence.


## P6-T05 mesh coordinate conversion verification

Issue #374 defines the exactly-once Assimp-import-basis to D-041 engine-space mesh conversion.

Focused Windows verification:

```powershell
.\gradlew.bat spotlessApply
.\gradlew.bat :engine-assets:test --tests "com.samo.engine.assets.internal.MeshCoordinateConverterTest" --tests "com.samo.engine.assets.internal.AssetCookerTest" --rerun-tasks
.\gradlew.bat spotlessCheck
.\gradlew.bat check
.\gradlew.bat resolveAndLockAllDependencies
```

Focused acceptance hard-codes the P6-T04 Assimp import-basis reference triangle and verifies engine positions `(-7,-8,-9), (-1,2,-3), (4,5,-6)`, -Z normals, -X tangent xyz, unchanged accepted UV0, and unchanged indices `[0,1,2]`. A committed identity-node one-meter glTF cube is imported through Assimp and converted, then test-only AABB calculation must measure exactly 1.0 meter on all three axes. Additional tests cover missing optional attributes, malformed attribute lengths, null misuse, and non-finite positions/normals/tangents.

Complete dependency-lock diff must remain empty. The final implementation must contain no tangent generation/required-UV policy, persisted mesh schema, runtime resource loader, renderer/world integration, public API, or new dependency/module edge.

Because P6-T05 changes durable spatial semantics and production cooker Java code/tests/resources, the final candidate requires the normal exact-head five-job PR matrix. After merge, exact merged `master` requires Lightweight verification before Issue #374 closes.

Connector execution note: this GitHub-only environment cannot run the Gradle wrapper directly; focused commands are not locally claimed as passing unless repository CI executes equivalent coverage. Exact final-candidate CI remains mandatory.


Accepted P6-T05 verification evidence: an earlier candidate exposed two bounded corrections before acceptance — IEEE signed-zero normalization in rotated zero components and repository Spotless formatting. Final PR #375 head `f9291a8143b514fe3ad4e3954dd664b3a012ed5e` passed Build and quality gates, Unit tests, Architecture tests, JaCoCo coverage reports, and Windows native smoke in run #536 / `35715570825`. That run includes the committed one-meter cube importer/converter test, exact reference-triangle engine-basis assertions, malformed/non-finite conversion tests, no-lock-drift build validation, and retained Windows native regression coverage. PR #375 merged as `019bbaec98b37d92791c375bc8d553c06257b1ee`; exact-merge Lightweight master verification passed in run #537 / `35716215981`, including committed dependency locks, the headless-server runtime boundary, and exact-merge client/server version reporting. P6-T05 acceptance is therefore complete. The connector environment did not separately execute the documented focused Gradle commands locally; the accepted repository CI is the execution evidence.


## P6-T06 tangent-space generation and required-UV verification

Issue #377 enables exactly one new Assimp post-process flag for MESH import: `aiProcess_CalcTangentSpace`.

Focused Windows verification:

```powershell
.\gradlew.bat spotlessApply
.\gradlew.bat :engine-assets:test --tests "com.samo.engine.assets.internal.AssimpGltfMeshImporterTest" --tests "com.samo.engine.assets.internal.MeshCoordinateConverterTest" --tests "com.samo.engine.assets.internal.AssetCookerTest" --rerun-tasks
.\gradlew.bat spotlessCheck
.\gradlew.bat check
.\gradlew.bat resolveAndLockAllDependencies
```

Focused acceptance covers a self-contained triangle with normals + UV0 but no authored tangent, proving Assimp-generated tangent/bitangent data is copied, tangent xyz reaches D-041 engine basis, and per-vertex handedness signs are retained as ±1. A valid glTF normal-mapped material using UV0 succeeds. Normal-mapped fixtures missing UV0 or normals fail with exact source path plus mesh index/name, and cooker coverage proves required-UV failure happens before output-cache creation. Existing no-UV non-normal-mapped mesh import remains accepted.

Review must confirm the importer uses only `aiProcess_CalcTangentSpace`, no dependency/module-edge/metadata-schema changes exist, P6-T03 persisted manifest/temp payload remains unchanged, and no P6-T07+ serialization/runtime work is pulled forward.

Because P6-T06 changes native importer behavior, internal mesh values/tests/resources, and durable tangent-space semantics, the final candidate requires the normal exact-head five-job PR matrix. After merge, exact merged `master` requires Lightweight verification before Issue #377 closes.

Connector execution note: this GitHub-only environment cannot run the Gradle wrapper directly; focused commands are not locally claimed as passing unless repository CI executes equivalent coverage. Exact final-candidate CI remains mandatory.


Accepted P6-T06 verification evidence: initial exact-candidate run #538 / `35719047919` executed the new Assimp tangent path and exposed only an incorrect pre-execution `+1` tangent-sign oracle for the generated fixture plus one Spotless method-wrapping difference. The active Issue was refined to preserve the executed Assimp handedness (`-1` for that fixture) rather than artificially flipping it, and the formatter diff was applied. Final PR #378 head `3e531c87e5c6438a722d8b00db59a9259ba2f542` passed Build and quality gates, Unit tests, Architecture tests, JaCoCo coverage reports, and Windows native smoke in run #540 / `35719320776`. The passing tests exercise missing-tangent generation, valid normal-mapped UV0 use, missing UV0/normals diagnostics, tangent-sign preservation through P6-T05, and pre-output cooker failure; Build verified dependency locks without drift. PR #378 merged as `7b0a59a1518dd58804e81c6bae5b83cc9095cec8`; exact-merge Lightweight master verification passed in run #541 / `35719896845`, including committed dependency locks, headless-server runtime boundary, and exact-merge client/server version reporting. P6-T06 acceptance is therefore complete. The connector environment did not separately execute the focused Gradle commands locally; accepted repository CI is the execution evidence.


## P6-T07 cooked mesh binary verification

Issue #380 defines persisted MESH `SMES` schema v1 and the package-private encode/decode validation boundary.

Focused Windows verification:

```powershell
.\gradlew.bat spotlessApply
.\gradlew.bat :engine-assets:test --tests "com.samo.engine.assets.internal.CookedMeshBinaryTest" --tests "com.samo.engine.assets.internal.AssetCookerTest" --tests "com.samo.engine.assets.internal.MeshCoordinateConverterTest" --tests "com.samo.engine.assets.internal.AssimpGltfMeshImporterTest" --rerun-tasks
.\gradlew.bat spotlessCheck
.\gradlew.bat check
.\gradlew.bat resolveAndLockAllDependencies
```

Focused acceptance independently inspects the encoded little-endian header/record layout, magic `SMES`, schema version, mesh/body counts, CRC32C, attribute flags/stride, exact engine-space bounds, strict UTF-8 name, deterministic bytes, optional attribute combinations, and multi-mesh order. Round-trip tests verify positions, normals, tangent xyz, P6-T06 tangent signs, UV0, and int32 indices. Corruption coverage flips body bytes without updating CRC, mutates semantic fields with a recomputed valid checksum, and exercises bad magic/version/truncation/trailing data. Cooker tests verify MESH output is SMES rather than source glTF bytes, manifest byteSize equals the actual cooked file, non-MESH pass-through remains unchanged, and binary-write failure preserves P6-T03 cleanup behavior.

Complete dependency-lock diff must remain empty. No runtime manifest/resource loader, public mesh API, GPU upload, renderer/world integration, compression, metadata/manifest schema change, or P6-T08+ implementation is allowed.

Because P6-T07 introduces a persisted binary format and production cooker Java/tests/docs, the final candidate requires the normal exact-head five-job PR matrix. After merge, exact merged `master` requires Lightweight verification before Issue #380 closes.

Connector execution note: this GitHub-only environment cannot run the Gradle wrapper directly; focused commands are not locally claimed as passing unless repository CI executes equivalent coverage. Exact final-candidate CI remains mandatory.


Accepted P6-T07 verification evidence: initial exact-candidate run #542 / `35722147969` compiled the new codec/cooker path and exposed only Spotless formatting differences; Unit tests, Architecture tests, and JaCoCo passed on that obsolete candidate. The formatter shape was corrected without changing behavior. Final PR #381 head `57b6926e9626cc96bee47453e6d249057a064972` passed Build and quality gates, Unit tests, Architecture tests, JaCoCo coverage reports, and Windows native smoke in run #544 / `35722428096`. The passing tests verify exact SMES header/record layout, little-endian interpretation, CRC32C, deterministic encoding, optional attributes, strict UTF-8, D-041 AABB persistence, tangent handedness persistence, checksum-first corruption rejection, semantic corruption with recomputed checksum, cooker MESH binary output/manifest byteSize, non-MESH pass-through, and cleanup on binary-write failure. Build verified committed dependency locks without drift. PR #381 merged as `c43e703e54ee325b0be5287a4520a38fc05da939`; exact-merge Lightweight master verification passed in run #545 / `35723120057`, including committed dependency locks, headless-server runtime boundary, and exact-merge client/server version reporting. P6-T07 acceptance is therefore complete. The connector environment did not separately execute the focused Gradle commands locally; accepted repository CI is the execution evidence.


## P6-T08 texture cooking verification

Issue #383 introduces offline PNG/JPEG decoding, deterministic mip generation, and persisted `STEX` schema v1 in `engine-assets`.

Focused Windows verification:

```powershell
.\gradlew.bat spotlessApply
.\gradlew.bat :engine-assets:test --tests "com.samo.engine.assets.internal.StbTextureImporterTest" --tests "com.samo.engine.assets.internal.TextureMipChainTest" --tests "com.samo.engine.assets.internal.CookedTextureBinaryTest" --tests "com.samo.engine.assets.internal.AssetCookerTest" --rerun-tasks
.\gradlew.bat spotlessCheck
.\gradlew.bat check
.\gradlew.bat resolveAndLockAllDependencies
```

Focused acceptance covers PNG and JPEG decode through stb, unflipped RGBA8 sample preservation, mixed-case supported extensions, unsupported/malformed source diagnostics, independently calculated odd-size mip dimensions and byte averages, deterministic `STEX` bytes, header/format/count/length/CRC32C validation, checksum and semantic corruption, truncation/trailing data, cooker integration/manifest byte size, and retained MESH/non-TEXTURE behavior.

Dependency review must confirm that only the existing LWJGL 3.4.3 family gains `lwjgl-stb` in `engine-assets`, with no unrelated version drift. P6-T08 changes executable Java/build behavior and a persisted texture format, so the final candidate requires the normal exact-head five-job PR matrix. After merge, exact merged `master` requires Lightweight master verification before Issue #383 closes.

Connector execution note: the GitHub connector environment cannot execute the Gradle wrapper directly. Focused commands are therefore not locally claimed as passing unless repository CI executes equivalent coverage; exact final-candidate CI remains mandatory.


Accepted P6-T08 verification evidence: initial candidate run #546 / `35725924137` exposed incomplete transitive `lwjgl-stb` dependency-lock state after `engine-assets` itself compiled and its tests executed; the lock graph was reconciled across affected runtime/test consumers. Subsequent candidates exposed repository Spotless-only differences, which were corrected without changing behavior. Final PR #384 head `29e65306385babf2a1e9c10c7b842a49e084acfa` passed Build and quality gates, Unit tests, Architecture tests, JaCoCo coverage reports, and Windows native smoke in run #553 / `35726845758`. The passing Build job resolved committed dependency locks successfully and enforced the pinned formatter; Unit/JaCoCo exercised PNG/JPEG decode, deterministic mip generation, `STEX` encoding/decoding/corruption handling, and cooker integration. PR #384 merged as `51142468b8186a06b0fe73bfaac117843c065ff3`; exact-merge Lightweight master verification passed in run #554 / `35727487378`, including committed dependency locks, the headless-server runtime boundary, and exact-merge client/server version reporting. P6-T08 acceptance is therefore complete. The connector environment did not separately execute the focused Gradle command sequence locally; accepted repository CI is the execution evidence.


## P6-T09 Ogg Vorbis audio cooking verification

Issue #386 introduces offline Ogg Vorbis validation plus persisted `SAUD` schema v1 in `engine-assets`.

Focused Windows verification:

```powershell
.\gradlew.bat spotlessApply
.\gradlew.bat :engine-assets:test --tests "com.samo.engine.assets.internal.StbVorbisAudioImporterTest" --tests "com.samo.engine.assets.internal.CookedAudioBinaryTest" --tests "com.samo.engine.assets.internal.AssetCookerTest" --rerun-tasks
.\gradlew.bat spotlessCheck
.\gradlew.bat check
.\gradlew.bat resolveAndLockAllDependencies
```

Focused acceptance covers committed Ogg Vorbis fixtures whose metadata oracles are independently authored from their generation parameters: Base64-backed mono 22050 Hz, binary stereo 44100 Hz, and Base64-backed unsupported three-channel 32000 Hz. Tests verify case-insensitive `.ogg`, exact source-payload preservation, explicit whole-memory stb_vorbis open/info/sample-drain/error/close validation, path-diagnostic unsupported/malformed/truncated/multichannel failures, deterministic `SAUD` bytes, explicit little-endian header fields, mono/stereo/sample-rate validation, CRC32C, semantic metadata/length corruption, truncation/trailing data, cooker manifest byte size, and cleanup on cooked-audio write failure while retaining MESH/TEXTURE/non-AUDIO regressions.

Dependency review must confirm no new dependency family, module edge, or unrelated lock drift; P6-T09 reuses the `lwjgl-stb` 3.4.3 dependency already present from P6-T08. The final candidate requires the normal exact-head five-job PR matrix. After merge, exact merged `master` requires Lightweight master verification before Issue #386 closes.

Connector execution note: this GitHub-only environment cannot execute the Gradle wrapper directly. Focused commands are therefore not locally claimed as passing unless repository CI executes equivalent coverage; exact final-candidate CI remains mandatory.


Accepted P6-T09 verification evidence: final PR #388 head `7e6aabbfb58bddc84abd3a10c674c2a003d67221` passed Build and quality gates, Unit tests, Architecture tests, JaCoCo coverage reports, and Windows native smoke in run #579 / `35781036481`. Earlier candidate runs exposed fixture compatibility and documentation-consistency issues; the final accepted implementation uses the explicit stb_vorbis open/info/sample-drain/error/close lifecycle with committed mono/stereo/multichannel fixtures and synchronized durable documentation. PR #388 merged as `4de4f3a9e9ca67857f9fe6f3b0bb159e4b6749d3`; exact-merge Lightweight master verification passed in run #580 / `35783262964`, including committed dependency locks, the headless-server runtime boundary, and exact-merge client/server version reporting. P6-T09 acceptance is complete.


## P6-T10 asset dependency graph verification

Issue #390 introduces strict optional `<source>.deps.json` authoring sidecars, package-private dependency graph/invalidation logic, and deterministic persisted `dependencies.json` schema v1 in `engine-assets`.

Focused Windows verification:

```powershell
.\gradlew.bat spotlessApply
.\gradlew.bat :engine-assets:test --tests "com.samo.engine.assets.internal.SourceAssetDependenciesTest" --tests "com.samo.engine.assets.internal.AssetDependencyGraphTest" --tests "com.samo.engine.assets.internal.CookedDependencyGraphTest" --tests "com.samo.engine.assets.internal.AssetCookerTest" --rerun-tasks
.\gradlew.bat spotlessCheck
.\gradlew.bat check
.\gradlew.bat resolveAndLockAllDependencies
```

Focused acceptance covers strict sidecar fields/version/canonical AssetIds, canonical logical shader keys, duplicate/self/missing/type-invalid references, unsupported/orphan sidecars, cycle rejection, exact reverse-transitive invalidation including diamond deduplication and unrelated-branch exclusion, deterministic persisted ordering/bytes, malformed persisted graph rejection, cooker integration, and owned-output cleanup when `dependencies.json` writing fails. Expected invalidation sets are authored independently from production traversal.

Dependency-lock diff must remain empty. No `AssetType`/source metadata v1/manifest schema/public API/module-edge/dependency change is permitted. The final candidate requires the normal exact-head five-job PR matrix. After merge, exact merged `master` requires Lightweight master verification before Issue #390 closes.

Connector execution note: this GitHub-only environment cannot execute the Gradle wrapper directly. Focused commands are not locally claimed as passing; exact final-candidate repository CI remains mandatory.


Accepted P6-T10 verification evidence: final PR #391 head `f831c9eb548b049ae284b353a9b308679914f1b1` passed Build and quality gates, Unit tests, Architecture tests, JaCoCo coverage reports, and Windows native smoke in run #586 / `35786375612`. Earlier candidates exposed only test-fixture ordering/context mistakes and repository Spotless shape differences; production dependency semantics remained within Issue #390. PR #391 merged as `7b5843968b58193f249f54f6d5665e7435f8fd37`; exact-merge Lightweight master verification passed in run #587 / `35786970766`, including committed dependency locks, the headless-server runtime boundary, and exact-merge client/server version reporting. P6-T10 acceptance is complete.


## P6-T11 resource handle verification

Issue #393 introduces the first public typed runtime resource-handle lifecycle boundary in `engine-assets`.

Focused Windows verification:

```powershell
.\gradlew.bat spotlessApply
.\gradlew.bat :engine-assets:test --tests "com.samo.engine.assets.api.ResourceHandleTest" --tests "com.samo.engine.assets.internal.ResourceHandleCellTest" --rerun-tasks
.\gradlew.bat spotlessCheck
.\gradlew.bat check
.\gradlew.bat resolveAndLockAllDependencies
```

Focused acceptance covers exact public states, no producer/native public surface, non-null AssetId construction, initial LOADING, READY identity preservation, FAILED completion, AssetId/state diagnostics from non-ready required access, null/duplicate/terminal completion rejection without mutation, idempotent release from LOADING/READY/FAILED, clearing the retained READY value on release, terminal RELEASED behavior, and synchronized concurrent observation/completion/release without stable impossible state/value combinations.

Dependency-lock diff must remain empty. No module edge/dependency change, runtime loader/cache/reference counting, fallback asset, async I/O, GPU/OpenAL upload, renderer/world integration, or P6-T12+ behavior is permitted. Because P6-T11 adds public API and lifecycle semantics, the final candidate requires the normal exact-head five-job PR matrix. After merge, exact merged `master` requires Lightweight master verification before Issue #393 closes.

Connector execution note: this GitHub-only environment cannot execute the Gradle wrapper directly. Focused commands are not locally claimed as passing; exact final-candidate repository CI remains mandatory.


Accepted P6-T11 verification evidence: initial candidate run #588 / `35849147685` exposed only repository Spotless formatting differences and one Checkstyle empty-catch violation in the concurrency test; both were corrected without changing the public lifecycle contract. Final PR #394 head `b22258a8e9b7102de4c5ec14af987b751ca009ce` passed Build and quality gates, Unit tests, Architecture tests, JaCoCo coverage reports, and Windows native smoke in run #590 / `35849372952`. The passing tests cover the exact public state set, no producer/native public surface, READY/FAILED completion, non-ready diagnostics, terminal/idempotent release, clearing retained READY values, illegal completion rejection, and synchronized concurrent observation/completion/release. PR #394 merged as `b761d15f07eaeeb2d082fc8e8dea53bfb641cefd`; exact-merge Lightweight master verification passed in run #591 / `35849980110`, including committed dependency locks, the headless-server runtime boundary, and exact-merge client/server version reporting. P6-T11 acceptance is complete. The connector environment did not separately execute the focused Gradle command sequence locally; accepted repository CI is the execution evidence.


## P6-T12 fallback asset verification

Issue #396 introduces deterministic missing-content fallback values plus public structured `AssetLoadError` diagnostics in `engine-assets`.

Focused Windows verification:

```powershell
.\gradlew.bat spotlessApply
.\gradlew.bat :engine-assets:test --tests "com.samo.engine.assets.api.AssetLoadErrorTest" --tests "com.samo.engine.assets.internal.FallbackAssetResolverTest" --rerun-tasks
.\gradlew.bat spotlessCheck
.\gradlew.bat check
.\gradlew.bat resolveAndLockAllDependencies
```

Focused acceptance independently verifies a centered closed one-meter D-041 cube, exact 2x2 magenta/black RGBA8 checker and 1x1 mip, finite opaque-magenta material components, deterministic non-silent mono PCM16 22050 Hz sound with repeated sign crossings, READY fallback handles preserving requested AssetIds, structured MISSING_CONTENT diagnostics, deterministic repeated resolution with independent handle lifetime, null rejection, and absence of native/backend types from the public diagnostic API.

Dependency-lock diff must remain empty. D-041 mesh basis/units, D-074 texture color semantics, D-075 SAUD persistence, P6-T11 handle API, and all module edges remain unchanged. No runtime manifest/file reading, cache/reference counting, async execution, native allocation/upload/playback, renderer/world integration, or P6-T13+ implementation is permitted.

Because P6-T12 adds public structured diagnostic API and runtime fallback lifecycle behavior, the final candidate requires the normal exact-head five-job PR matrix. After merge, exact merged `master` requires Lightweight master verification before Issue #396 closes.

Connector execution note: this GitHub-only environment cannot execute the Gradle wrapper directly. Focused commands are not locally claimed as passing; exact final-candidate repository CI remains mandatory.


Accepted P6-T12 verification evidence: candidate runs #592 / `35905334367` and #593 / `35905599395` exposed only repository Spotless formatter differences and were obsolete after formatter-only corrections. Final PR #397 head `1ceecab7225b863302231bb9243631d7dcaac971` passed Build and quality gates, Unit tests, Architecture tests, JaCoCo coverage reports, and Windows native smoke in run #594 / `35905886197`. The passing tests independently verify the closed one-meter D-041 fallback cube topology/bounds, exact magenta/black RGBA8 checker plus 1x1 mip, opaque-magenta material components, deterministic non-silent mono PCM16 fallback with repeated sign crossings, READY handle/error pairing, deterministic repeated resolution, independent handle lifetime, public error validation, and absence of native/backend public diagnostic types. PR #397 merged as `f2919bb93c34b410e9a3dbf7707e5ecce850766f`; exact-merge Lightweight master verification passed in run #595 / `35906606650`, including committed dependency locks, the headless-server runtime boundary, and exact-merge client/server version reporting. P6-T12 acceptance is complete. The connector environment did not separately execute the focused Gradle command sequence locally; accepted repository CI is the execution evidence.


## P6-T13 asynchronous mesh loading verification

Issue #399 adds strict manifest-backed asynchronous runtime MESH loading plus owner-thread-only OpenGL buffer upload.

Focused Windows verification:

```powershell
.\gradlew.bat spotlessApply
.\gradlew.bat :engine-assets:test --tests "com.samo.engine.assets.internal.RuntimeAssetManifestTest" --tests "com.samo.engine.assets.internal.AsyncAssetLoaderTest" --rerun-tasks
.\gradlew.bat :engine-render-opengl:test --tests "com.samo.engine.render.opengl.internal.RuntimeMeshGpuUploaderTest" --rerun-tasks
.\gradlew.bat spotlessCheck
.\gradlew.bat check
.\gradlew.bat resolveAndLockAllDependencies
```

Focused acceptance verifies strict manifest v1 parsing/path containment, LOADING-before-controlled-worker execution, off-caller-thread file read/SMES decode, exact decoded mesh values, P6-T12 fallback reuse for missing identity/file, READ_FAILED versus INVALID_CONTENT diagnostics, wrong-type synchronous rejection, release-during-load non-resurrection, ordered error draining, and loader-close submission rejection. Renderer acceptance constructs an independent valid >1 MiB SMES fixture, loads it through the public production `AssetLoaders.open` virtual-thread path, proves no resource backend call occurs before explicit owner-thread upload, checks exact buffer sizes and representative values, rejects non-owner upload before backend mutation, verifies partial-upload rollback, and verifies idempotent owner-thread close.

Dependency-lock diff must remain empty. No new module edge, dependency, workflow, persisted schema, shader, texture/audio/material loader, cache/reference counting, arbitrary draw submission, world/game/sandbox source, or P6-T14 behavior is permitted. `engine-assets` must not import renderer or OpenGL packages. D-041 positions remain already-converted engine-space meters and are not converted again.

Because P6-T13 adds public asset API, asynchronous lifecycle behavior, and explicit GPU ownership behavior, the final candidate requires the normal exact-head five-job PR matrix. After merge, exact merged `master` requires Lightweight master verification before Issue #399 closes.

Connector execution note: this GitHub-only environment cannot execute the Gradle wrapper directly. Focused commands are not locally claimed as passing; exact final-candidate repository CI remains mandatory.
