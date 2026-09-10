# Sherko Engine — Development Status

> Living snapshot for the current repository state. GitHub Issues remain the authoritative source for exact live task status.

## Current position

- Milestone: M1 — Engine Foundation.
- Phase: P1 — Build, modules, and quality gates.
- P1-T01 through P1-T04 have verified implementation evidence on master.
- Exact current-task state belongs to GitHub Issues/Project and is intentionally not duplicated here.
- Phase 0 native/API smoke evidence remains durable, while P0-T09A, P0-T13, and P0-T14 are explicit follow-up gates before their dependent production claims.

## Proven feasibility baseline

- Java 25 toolchain and CI baseline work.
- GLFW/OpenGL 4.6, Jolt JNI, OpenAL, and localhost UDP were proven independently.
- The integrated P0-T12 smoke run exercised graphics, physics, audio, and UDP together for 15 seconds under JFR and shut down cleanly; it is not long-duration stability evidence.
- Steamworks4j is sufficient for basic Steam initialization/callbacks but not the required modern `ISteamNetworkingSockets` surface.
- Java 25 FFM can reach the official Steam flat API / `ISteamNetworkingSockets` pointer without authored C/C++ glue. End-to-end listen/connect/callback/message ownership remains unproven until P0-T09A passes.
- Development network impairment injection for latency, jitter, loss, duplication, and reordering is proven.

Detailed feasibility notes live under `docs/feasibility/`.

## Phase 1 progress

| Task | State | Result |
| --- | --- | --- |
| P1-T01 | Complete | `engine-core`, `test-support`, `game-client`, and `game-server` are declared Gradle subprojects; `gradlew projects` and `gradlew buildAllModules` were verified successfully. |
| P1-T02 | Complete | The complete target module tree from `ENGINE_SCOPE.md` is declared. Local verification showed all 15 modules in `gradlew projects`, `gradlew buildAllModules` succeeded, and `:engine-network-ip:test` also completed successfully. The project dependency graph remains one-way with no circular project dependency observed. |
| P1-T03 | Complete | Shared external dependency versions are centralized in `gradle/libs.versions.toml`; dependency locking is enabled for all projects; generated lock state is committed; and two repeated dependency resolutions completed successfully using the locked graph. |
| P1-T04 | Complete | `test-support` exports JUnit and AssertJ, all Java test tasks use JUnit Platform, every engine module has a minimal shared-setup smoke test, the root test task passed with 35 actionable tasks, and refreshed dependency lock files are present on `master`. |

## Current module tree

```text
engine-core
engine-platform-lwjgl
engine-render-opengl
engine-assets
engine-world
engine-physics-jolt
engine-audio-openal
engine-network-api
engine-network-ip
engine-steam
engine-editor
game-sandbox
game-client
game-server
test-support
```

The module graph is intentionally one-way. Lower engine modules do not depend on game modules. `game-server` does not depend on rendering, platform-window, or audio modules, preserving the future headless-server path.

`buildAllModules` depends on every declared subproject build.

## Shared test infrastructure

The shared testing foundation lives in `test-support`.

- JUnit is the active test platform for Java modules.
- AssertJ is exported by `test-support` for fluent assertions.
- The JUnit Platform launcher is provided at test runtime through `test-support`.
- Engine modules depend on `test-support` only in `testImplementation`, so test infrastructure does not enter production runtime dependencies.
- Minimal smoke tests exist in each engine module to prove JUnit + AssertJ are available through the common setup.

After adding or changing test dependencies, refresh dependency lock state:

```powershell
.\gradlew.bat resolveAndLockAllDependencies --write-locks
```

Then verify all module sample tests from the repository root:

```powershell
.\gradlew.bat test
```

The root test command was verified successfully and the refreshed lock files are committed. P1-T04 is complete. Identical module smoke tests are temporary proof of shared setup and should be replaced/removed as real module tests arrive.

## Dependency reproducibility

Shared dependency versions are owned by:

```text
gradle/libs.versions.toml
```

Dependency locking is enabled across the build. Current committed lock state includes the root dependency graph in `gradle.lockfile` plus Gradle settings lock state in `settings-gradle.lockfile`.

Refresh lock state only when dependencies intentionally change:

```powershell
.\gradlew.bat resolveAndLockAllDependencies --write-locks
```

Normal dependency resolution should run without `--write-locks` so unexpected graph changes fail instead of silently rewriting lock state.

## Experimental Phase 0 code

The root `src/main/java/com/samo/spike/...` code remains available for feasibility evidence. Do not treat those spike class structures as permanent engine APIs.

> **Spikes are disposable. Conclusions are durable.**

## Execution policy

Implementation work uses one bounded Issue, one dedicated task branch, one pull request, and CI/acceptance verification before merge. Agent-generated changes do not land directly on master.

## Architecture corrections recorded

- Runtime supports 1–4 players; the intended co-op target is 2–4.
- First-person is the v1 vertical slice; third-person follows stable multiplayer.
- Gradle Kotlin DSL is retained; Java-only applies to authored engine/game runtime source.
- A renderer-neutral runtime UI module is required before package boundaries freeze.
- Basic local audio moves before the Phase 8 exit gate; network-aware audio polish remains later.
- Exact target hardware is selected before renderer implementation.
- Phase 0 spikes remain disposable evidence and should eventually move out of the root production project.

## Documentation rule

After meaningful repository changes, review and update documentation whose status, commands, architecture, decisions, or scope changed. At minimum, keep this file synchronized with current implementation and verification reality.
