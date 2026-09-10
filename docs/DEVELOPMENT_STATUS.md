# Sherko Engine — Development Status

> Living snapshot for the current repository state. GitHub Issues remain the authoritative source for exact live task status.

## Current position

- **Milestone:** M1 — Engine Foundation
- **Phase:** P1 — Build, modules, and quality gates
- **Completed:** P1-T01 — initial four-module Gradle build
- **Completed:** P1-T02 — full target module tree
- **Completed:** P1-T03 — centralized dependency versions and dependency locking
- **Current task:** P1-T04 — Add shared JUnit 5 and AssertJ test support (Issue #34)
- **P1-T04 state:** root test verification passed; refreshed dependency lock files still need commit/push
- **Phase 0:** complete; feasibility spikes remain disposable evidence, not production architecture

## Proven feasibility baseline

- Java 25 toolchain and CI baseline work.
- GLFW/OpenGL 4.6, Jolt JNI, OpenAL, and localhost UDP were proven independently.
- The integrated P0-T12 run exercised graphics, physics, audio, and UDP together under JFR and shut down cleanly.
- Steamworks4j is sufficient for basic Steam initialization/callbacks but not the required modern `ISteamNetworkingSockets` surface.
- Java 25 FFM can reach the official Steam flat API / `ISteamNetworkingSockets` path without authored C/C++ glue.
- Development network impairment injection for latency, jitter, loss, duplication, and reordering is proven.

Detailed feasibility notes live under `docs/feasibility/`.

## Phase 1 progress

| Task | State | Result |
| --- | --- | --- |
| P1-T01 | Complete | `engine-core`, `test-support`, `game-client`, and `game-server` are declared Gradle subprojects; `gradlew projects` and `gradlew buildAllModules` were verified successfully. |
| P1-T02 | Complete | The complete target module tree from `ENGINE_SCOPE.md` is declared. Local verification showed all 15 modules in `gradlew projects`, `gradlew buildAllModules` succeeded, and `:engine-network-ip:test` also completed successfully. The project dependency graph remains one-way with no circular project dependency observed. |
| P1-T03 | Complete | Shared external dependency versions are centralized in `gradle/libs.versions.toml`; dependency locking is enabled for all projects; generated lock state is committed; and two repeated dependency resolutions completed successfully using the locked graph. |
| P1-T04 | In progress | `test-support` exports JUnit and AssertJ, all Java test tasks use JUnit Platform, and every engine module has a minimal smoke test using the shared setup. After adding the missing JUnit Platform launcher runtime dependency, `gradlew test` completed successfully with 35 actionable tasks. Completion now only requires committing/pushing the refreshed dependency lock files. |

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

The root test command has now been verified successfully. P1-T04 completes once the refreshed lock files produced by the successful `--write-locks` run are committed to `master`.

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

## What happens next

Commit and push the refreshed dependency lock files from the successful P1-T04 verification. Then close Issue #34 and advance to **P1-T05 — Add Checkstyle quality rules** (Issue #35).

## Working convention

Current owner-directed work is performed directly on `master`. Do not create a pull request unless the repository owner explicitly asks for one.

## Documentation rule

After meaningful repository changes, review and update documentation whose status, commands, architecture, decisions, or scope changed. At minimum, keep this file synchronized with current implementation and verification reality.
