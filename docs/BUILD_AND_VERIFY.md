# Sherko Engine Build and Verification

This file centralizes repeatable commands and the evidence expected from them. Run Windows commands on Windows x64 when native libraries are involved. CI is the authoritative clean Windows Java 25 environment for ordinary build/test checks.

## Environment baseline

- JDK: Temurin/OpenJDK 25 through the Gradle toolchain.
- Build: repository Gradle Wrapper.
- CI: `windows-latest`.
- Runtime target: Windows x64.

Use `./gradlew` on Unix-like shells for non-native configuration checks and `.\gradlew.bat` on Windows.

## Routine verification matrix

| Purpose | Windows command | Expected evidence |
| --- | --- | --- |
| Show declared projects | `.\gradlew.bat projects` | All 16 subprojects from `settings.gradle.kts` appear. |
| Compile/test every module and run the root quality gate | `.\gradlew.bat buildAllModules` | Root `check` and every subproject `build` complete. |
| Run the root quality gate | `.\gradlew.bat check` | Checkstyle, architecture-boundary tests, Phase 0 exclusion verification, and root tests pass. |
| Run root and subproject tests | `.\gradlew.bat test` | JUnit Platform tasks pass, including the package/module architecture tests and `engine-ui`. |
| Run only the root architecture boundary suite | `.\gradlew.bat :test --tests "com.samo.architecture.ModulePackageBoundaryTest" --rerun-tasks` | The root architecture test executes even when Gradle would otherwise consider `:test` up-to-date. |
| Generate and verify JaCoCo reports | `.\gradlew.bat verifyJacocoReports` | Tests run for all 12 current test-bearing engine modules and each produces XML plus HTML coverage reports. |
| Verify Java selection | `.\gradlew.bat javaToolchains` | Java 25 toolchain is available/selected. |
| Resolve committed locks | `.\gradlew.bat resolveAndLockAllDependencies` | Resolution completes without changing locks in an unchanged dependency graph. |

For P1-T05 / Issue #35, the deliberate negative fixture is opt-in and must fail the root check task:

```powershell
.\gradlew.bat check -PcheckstyleIncludeInvalidFixture=true
```

The fixture intentionally contains a wildcard import, an empty catch block, and a discarded return value from a side-effect-free fully-qualified `java.lang.Math` call. The normal valid command is:

```powershell
.\gradlew.bat check
```

The root Checkstyle scan deliberately excludes `src/main/java/com/samo/spike/**`, which remains Phase 0 experimental evidence. `verifyCheckstyleSourceBoundary` makes that exclusion explicit and fails if excluded spike sources leak into the production scan.

For P1-T06 / Issue #36, run:

```powershell
.\gradlew.bat verifyJacocoReports
```

JaCoCo is configured only for the 12 engine modules that currently contain the shared smoke tests. Each module writes:

- XML: `<module>/build/reports/jacoco/test/jacocoTestReport.xml`
- HTML: `<module>/build/reports/jacoco/test/html/index.html`

`verifyJacocoReports` fails when either format is missing for any configured test-bearing module. Coverage is reported for visibility only; P1-T06 deliberately defines no global or per-module minimum percentage.

For P1-T07 / Issue #37, the valid architecture suite is part of the ordinary root test/check flow. To run only the package-boundary tests locally, target the root `:test` task explicitly and force execution so a changed system property cannot be hidden by Gradle up-to-date state:

```powershell
.\gradlew.bat :test --tests "com.samo.architecture.ModulePackageBoundaryTest" --rerun-tasks
```

The deliberate negative fixture at `config/architecture/fixtures/ForbiddenGameToPlatformShortcut.java` is disabled by default. To prove the representative `game-client -> engine-platform-lwjgl.internal` shortcut is rejected, run:

```powershell
$env:JAVA_TOOL_OPTIONS="-Darchitecture.includeInvalidFixture=true"
.\gradlew.bat :test --tests "com.samo.architecture.ModulePackageBoundaryTest" --rerun-tasks
Remove-Item Env:JAVA_TOOL_OPTIONS
```

That negative run must fail because a cross-module import targets an implementation package instead of the destination module's declared API root. After clearing `JAVA_TOOL_OPTIONS`, rerun the valid architecture command and require success.

The boundary registry is `config/architecture/module-boundaries.properties`. It must contain exactly one `.root`, `.api`, and `.internal` declaration for every one of the 16 Gradle subprojects.

For a general documentation/build-boundary pull request, the minimum clean verification is:

```powershell
.\gradlew.bat projects
.\gradlew.bat buildAllModules
.\gradlew.bat test
```

Do not use Gradle task counts as durable evidence; counts change when modules/plugins/tasks change.

## CI gate

`.github/workflows/java25.yml` runs on pull requests targeting `master` and pushes to `master` using required Windows Java 25 jobs for:

- build and root quality gates via `buildAllModules`;
- unit/root/subproject tests via `test`;
- explicit architecture boundaries via the root `ModulePackageBoundaryTest` command above;
- JaCoCo XML/HTML generation and artifact upload via `verifyJacocoReports`;
- a short Windows native smoke via `runIntegratedNativeSmoke`, with its JFR uploaded when available.

The native CI job is intentionally the existing 15-second P0-T12 smoke classification. It must not be interpreted as P0-T13 sustained-stability evidence, P0-T14 repeated-lifecycle evidence, or P0-T09A end-to-end Steam transport evidence. Steam-dependent checks are not part of unattended hosted CI because they require an authenticated Steam client/account environment.

P1-T08 requires every required job failure to fail the workflow. During implementation, one controlled failure must be demonstrated and then removed before merge.

## Phase 0 feasibility commands

| Task | Command | Classification / output |
| --- | --- | --- |
| P0-T03 OpenGL | `.\gradlew.bat runOpenGL46Spike` | Interactive native feasibility run. |
| P0-T04 Jolt | `.\gradlew.bat runJoltLifecycleSpike` | Native lifecycle/cleanup spike. |
| P0-T05 OpenAL | `.\gradlew.bat runOpenAL3DAudioSpike` | Native audio lifecycle spike. |
| P0-T11 impairment | `.\gradlew.bat runNetworkImpairmentHarness` | Deterministic latency/jitter/loss/duplication/reordering suite. |
| P0-T07 Steam init | `.\gradlew.bat runSteamInitSpike` | Requires running Steam client and native access. |
| P0-T09 FFM access | `.\gradlew.bat runSteamFlatApiFfmSpike` | Proves flat-API access, not end-to-end transport. |
| P0-T12 integrated smoke | `.\gradlew.bat runIntegratedNativeSmoke` | 15 seconds; JFR at `build/spikes/native-evidence/p0-t12-smoke.jfr`. |
| P0-T13 sustained test | `.\gradlew.bat runIntegratedNativeSoak` | 900 seconds; JFR at `build/spikes/native-evidence/p0-t13-soak.jfr`; pending Issue #43. |

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

After an authorized dependency/version change:

```powershell
.\gradlew.bat resolveAndLockAllDependencies --write-locks
.\gradlew.bat buildAllModules
```

Review every changed lockfile. Do not refresh locks during unrelated tasks.
