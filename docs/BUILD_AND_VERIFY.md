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

## CI gate

`.github/workflows/java25.yml` runs on pull requests targeting `master` and pushes to `master`. All five workflow jobs select `[self-hosted, Windows, X64]`. The repository runner must therefore be online before the workflow can execute; an offline, queued, or unstarted job is an execution blocker, not a pass. A failure in any required job fails the workflow. Whether GitHub itself blocks a merge is controlled separately by live branch-protection or ruleset settings; regardless of those settings, `AGENTS.md` forbids agents from merging before a passing exact-head run.

The five jobs cover:

- build and root quality gates via `buildAllModules`, followed by client/server foundation runs, server headless verification, and client/server version-report compatibility;
- root/subproject test aggregation via `test`;
- explicit architecture boundaries via `:test-support:test --tests "com.samo.architecture.ModulePackageBoundaryTest" --rerun-tasks`;
- JaCoCo XML/HTML generation and artifact upload via `verifyJacocoReports`;
- Windows native lifecycle coverage via the preserved root aliases `runWindowsNativeCiSmoke` and `runJoltLifecycleSpike`.

A self-hosted run is not an ephemeral clean VM. `actions/checkout` still checks out the requested commit into the runner work directory, but machine-level installed software and caches can persist across jobs. For this reason the committed Gradle Wrapper, Java 25 setup, dependency locks, explicit task outputs, and repository tests remain the verification contracts; do not infer reproducibility merely from machine state.

Because only one repository runner is currently expected, independent jobs may execute sequentially. This affects wall-clock time only and does not change pass/fail semantics.

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
