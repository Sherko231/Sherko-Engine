# Sherko Engine Build and Verification

This file centralizes repeatable commands and the evidence expected from them. Run Windows commands on Windows x64 when native libraries are involved. CI is authoritative only when the configured repository self-hosted Windows x64 runner actually executes the jobs.

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
| Run root and subproject tests | `.\gradlew.bat test` | Aggregate JUnit Platform test tasks pass, including `test-support` architecture tests. |
| Run only the architecture boundary suite | `.\gradlew.bat :test-support:test --tests "com.samo.architecture.ModulePackageBoundaryTest" --rerun-tasks` | The Gradle-derived subproject registry, production package ownership, main/test imports, fully qualified references, and boundary regressions pass. |
| Generate and verify JaCoCo reports | `.\gradlew.bat verifyJacocoReports` | Tests run for all 12 current test-bearing engine modules and each produces XML plus HTML coverage reports. |
| Run client foundation entry point | `.\gradlew.bat :game-client:runClient` | Client foundation process starts and exits cleanly. |
| Run headless server foundation entry point | `.\gradlew.bat :game-server:runServer` | Server foundation process starts in headless mode and exits cleanly. |
| Report client version metadata | `.\gradlew.bat :game-client:runClient --args="--version"` | Client reports executable, engine commit, protocol version, asset version, Java version, and native libraries available on its runtime classpath. |
| Report server version metadata | `.\gradlew.bat :game-server:runServer --args="--version"` | Server reports the same shared identifiers plus its executable-specific native-library list. |
| Verify headless server dependency boundary | `.\gradlew.bat :game-server:verifyHeadlessServerRuntime` | Server runtime classpath contains no platform/render/audio projects or GLFW/OpenGL/OpenAL artifacts. |
| Run CI-native lifecycle smoke locally | `$env:ALSOFT_DRIVERS="null"; .\gradlew.bat runWindowsNativeCiSmoke; .\gradlew.bat runJoltLifecycleSpike -PjoltSpikeCycles=1; Remove-Item Env:ALSOFT_DRIVERS` | Historical root task aliases delegate to `feasibility-spikes`; GLFW/OpenAL/Jolt lifecycle smoke completes. |
| Verify Java selection | `.\gradlew.bat javaToolchains` | Java 25 toolchain is available/selected. |
| Resolve committed locks | `.\gradlew.bat resolveAndLockAllDependencies` | Resolution completes without changing locks in an unchanged dependency graph. |

## P2-T01 lifecycle verification

Issue #64 adds the production `EngineSubsystem` contract in `engine-core`. Run the full routine matrix above; `buildAllModules` includes the root `check` gate. Also run the focused acceptance suite:

```powershell
.\gradlew.bat :engine-core:test --tests "com.samo.engine.core.api.EngineSubsystemTest" --rerun-tasks
```

The JUnit 6 suite exercises successful phase order, invalid calls before hooks, early cleanup, runtime-exception/error propagation, cleanup after failed setup/activation/stopping, repeated or failed close, and reentrant calls. Test counters represent synthetic owned resources, not native leak evidence.

Focused outputs are `engine-core/build/test-results/test/TEST-com.samo.engine.core.api.EngineSubsystemTest.xml` and `engine-core/build/reports/tests/test/index.html`. The unit-test CI job uploads these as `engine-subsystem-tests`; the existing coverage job retains JaCoCo output for all 12 engine modules. The filtered run replaces that job's engine-core test report after the full aggregate suite has run; the full coverage job remains unfiltered.

The build job additionally runs `resolveAndLockAllDependencies` without `--write-locks` and verifies that tracked lockfiles did not change. No lockfile or dependency change is expected for this task. Record exact-head PR and merged-master workflow results in the linked Issue/PR; an unstarted or queued job is not a pass.

## P2-T02 dependency-order verification

Issue #72 adds `SubsystemGraph` without lifecycle orchestration or new dependencies. Run the full routine matrix above and the combined focused suite:

```powershell
.\gradlew.bat :engine-core:test --tests "com.samo.engine.core.api.EngineSubsystemTest" --tests "com.samo.engine.core.api.SubsystemGraphTest" --rerun-tasks
```

The graph suite tests dependency-first ordering, deterministic declaration-order traversal, shared/disconnected prerequisites, null/blank/duplicate/missing inputs, reference identity, defensive copies, repeatability and a 10,000-node chain. A cyclic synthetic caller prints the exact closed cycle diagnostic and proves no lifecycle hook runs, even for an earlier valid disconnected component. A successful synthetic caller exercises returned ordering with real `EngineSubsystem` guards and explicit cleanup; this is not rollback, native-resource evidence or the P2 phase exit.

Graph XML: `engine-core/build/test-results/test/TEST-com.samo.engine.core.api.SubsystemGraphTest.xml`. HTML: `engine-core/build/reports/tests/test/index.html`. The existing `engine-subsystem-tests` artifact now includes both lifecycle and graph XML/HTML, and `jacoco-reports` retains unfiltered coverage. The combined filtered command supersedes the lifecycle-only CI rerun so both focused reports survive together; the lifecycle-only command above remains usable on its own.

No lockfile change is expected. Inspect actual exact-head PR and merged-master push runs; local download/toolchain failures or queued jobs cannot be reported as passes.

## P2-T03 startup rollback verification

Issue #73 adds stateless `SubsystemStartup` coordination for an already resolved dependency-first order. Run the full routine matrix above and the combined focused suite:

```powershell
.\gradlew.bat :engine-core:test --tests "com.samo.engine.core.api.EngineSubsystemTest" --tests "com.samo.engine.core.api.SubsystemGraphTest" --tests "com.samo.engine.core.api.SubsystemStartupTest" --rerun-tasks
```

The rollback suite uses handwritten global hook traces to verify per-subsystem initialize/start activation, close of the currently failing subsystem, reverse stop/close of previously started subsystems, first-element isolation, caller-owned successful shutdown, input snapshotting, original throwable identity, continued cleanup after rollback failures, deterministic suppressed-failure order, and self-suppression avoidance.

Startup XML: `engine-core/build/test-results/test/TEST-com.samo.engine.core.api.SubsystemStartupTest.xml`. HTML remains `engine-core/build/reports/tests/test/index.html`. The `engine-subsystem-tests` artifact includes lifecycle, graph, and startup XML/HTML; `jacoco-reports` remains unfiltered.

These tests use synthetic Java subsystems. They do not establish native restartability, native leak freedom, sustained stability, or the P2 ten-minute integrated headless exit gate. No dependency or lockfile change is expected.

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

These tests establish deterministic elapsed-time semantics only. They do not implement or prove the P2-T05 fixed-step accumulator, P2-T06 catch-up limits, P2-T07 interpolation, frame pacing, concurrency, native timing behavior, or the P2 ten-minute integrated headless-loop exit gate. No dependency or lockfile change is expected.

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

JaCoCo is configured only for the 12 engine modules that currently contain the shared smoke tests. Each module writes:

- XML: `<module>/build/reports/jacoco/test/jacocoTestReport.xml`
- HTML: `<module>/build/reports/jacoco/test/html/index.html`

`verifyJacocoReports` fails when either format is missing for any configured test-bearing module. Coverage is reported for visibility only; no global or per-module minimum percentage is defined yet.

## Architecture boundaries

The D-016 suite now lives in `test-support` so the root can remain source-free. Run:

```powershell
.\gradlew.bat :test-support:test --tests "com.samo.architecture.ModulePackageBoundaryTest" --rerun-tasks
```

The `test-support` test task supplies `repository.root` and the sorted names from `rootProject.subprojects`. The suite requires an exact registry triple for every supplied project, requires each production Java source to declare an owned package, and parses imports plus fully qualified references from both main and test Java trees. Test source packages themselves are not ownership-checked because the shared Phase 1 smoke tests intentionally use `com.samo.testing`; their references are still checked.

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

The current entry points intentionally do not initialize later production subsystems. The headless verification inspects the resolved `game-server` runtime classpath and fails if `engine-platform-lwjgl`, `engine-render-opengl`, `engine-audio-openal`, `lwjgl-glfw`, `lwjgl-opengl`, or `lwjgl-openal` appears.

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

For a general documentation/build-boundary pull request, the minimum clean verification is:

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

For the current P2 phase, the existing gate is a headless loop running deterministic fixed ticks for ten minutes with bounded catch-up and verified cleanup. The eventual gate evidence must show those properties together; P2-T01/P2-T02/P2-T03/P2-T04 isolated suites do not satisfy that gate. The integrated loop and its command are not implemented yet.

For comparison, P3 requires replaying an identical input sequence into headless simulation, while P4 requires spatial tests independent of OpenGL/Jolt. Use those actual gate forms rather than requiring a rendered demo for every phase. Later phases retain their own scene, multiplayer, tooling, and release criteria from the backlog.

During a phase, add a small integration exercise within a task's authorized scope as soon as meaningful behavior is available. If it requires another task's implementation, record the missing dependency and keep the work bounded. Keep findings and review provenance in existing Issues/PRs and the current handoff documents; no parallel management document is required.

## CI gate

`.github/workflows/java25.yml` runs on pull requests targeting `master` and pushes to `master`. All five workflow jobs select `[self-hosted, Windows, X64]`. The repository runner must therefore be online before the workflow can execute; an offline, queued, or unstarted job is an execution blocker, not a pass. A failure in any required job fails the workflow. Whether GitHub itself blocks a merge is controlled separately by live branch-protection or ruleset settings; regardless of those settings, `AGENTS.md` forbids agents from merging before a passing exact-head run.

The workflow uses a top-level concurrency group keyed by workflow name plus PR number for pull requests, or by Git ref for push/manual runs, with `cancel-in-progress: true`. A newer commit on the same PR therefore supersedes older queued/in-progress runs for that PR without grouping it together with other PRs. A push to `master` uses the `master` ref group and is independent from PR groups.

Before interpreting CI evidence:

1. Read the current PR head SHA.
2. Match the candidate workflow run to that exact SHA. Runs for older PR-head SHAs are obsolete evidence.
3. Obsolete queued/in-progress PR runs may be cancelled manually when tooling and permissions allow; automatic concurrency cancellation is also acceptable. Cancellation of stale work is not a pass or failure for the current head.
4. Never cancel the current-head run merely to save runner time.
5. Require the exact current PR-head run to finish successfully before merge.
6. After merge, require a separate successful push workflow on the exact resulting `master` merge commit. Do not classify that merged-master run as an obsolete PR run.
7. If manual cancellation tooling is unavailable, leave stale runs alone and state that fact rather than claiming cancellation.

The five jobs cover:

- build and root quality gates via `buildAllModules`, followed by client/server foundation runs, server headless verification, and client/server version-report compatibility;
- root/subproject test aggregation via `test`, followed by the focused `EngineSubsystemTest`, `SubsystemGraphTest`, `SubsystemStartupTest`, and `EngineClockTest` suites and their XML/HTML evidence upload;
- explicit architecture boundaries via `:test-support:test --tests "com.samo.architecture.ModulePackageBoundaryTest" --rerun-tasks`;
- JaCoCo XML/HTML generation and artifact upload via `verifyJacocoReports`;
- Windows native lifecycle coverage via the preserved root aliases `runWindowsNativeCiSmoke` and `runJoltLifecycleSpike`.

A self-hosted run is not an ephemeral clean VM. `actions/checkout` still checks out the requested commit into the runner work directory, but machine-level installed software and caches can persist across jobs. For this reason the committed Gradle Wrapper, Java 25 setup, dependency locks, explicit task outputs, and repository tests remain the verification contracts; do not infer reproducibility merely from machine state.

Because independent jobs may execute sequentially when fewer matching runners are available, runner count affects wall-clock time only and does not change pass/fail semantics.

Because `game-server:check` also depends on `verifyHeadlessServerRuntime`, the ordinary all-module build enforces the server headless dependency boundary before the explicit runtime smoke steps.

The self-hosted Windows runner may have graphics capabilities that GitHub-hosted runners did not, but the required CI workflow still runs only the bounded native lifecycle smoke defined above. Do not upgrade this into P0-T12/P0-T13/P0-T14 evidence unless the corresponding explicit feasibility Issue is executed and documented.

This native CI gate must not be interpreted as P0-T13 sustained-stability evidence, P0-T14 repeated-lifecycle evidence, or P0-T09A end-to-end Steam transport evidence. Steam-dependent checks are not part of unattended CI because they require an authenticated Steam client/account environment.

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

For every pull request, list each executed command and result. For native/performance/protocol work also record:

- OS, JDK, relevant GPU/driver/native-library versions;
- configured duration, iteration count, or impairment parameters;
- pass/fail criteria and observed result;
- JFR/log/capture path;
- cleanup/leak observations;
- checks skipped because the environment could not support them.

Configuration review is not runtime evidence. If a command was not run, write `not run` and why.

## Dependency changes

After an authorized dependency/version or dependency-ownership change:

```powershell
.\gradlew.bat resolveAndLockAllDependencies --write-locks
.\gradlew.bat buildAllModules
```

Review every changed lockfile. P1-T10A relocates existing dependencies without changing their selected versions, so only ownership-related lock changes are expected.
