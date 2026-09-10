# Sherko Engine — Development Status

> Living snapshot for the current repository state. GitHub Issues remain the authoritative source for exact live task status.

## Current position

- **Milestone:** M1 — Engine Foundation
- **Phase:** P1 — Build, modules, and quality gates
- **Completed:** P1-T01 — initial four-module Gradle build
- **Completed:** P1-T02 — full target module tree
- **Current task:** P1-T03 — Centralize dependency versions and lock resolution (Issue #33)
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

## Experimental Phase 0 code

The root `src/main/java/com/samo/spike/...` code remains available for feasibility evidence. Do not treat those spike class structures as permanent engine APIs.

> **Spikes are disposable. Conclusions are durable.**

## What happens next

The next executable task is **P1-T03 — Centralize dependency versions and lock resolution** (Issue #33).

P1-T03 will move shared dependency/plugin versions into one central Gradle version catalog and enable dependency locking so clean resolutions remain reproducible across machines. It should not opportunistically upgrade dependencies unless compatibility requires it.

## Working convention

Current owner-directed work is performed directly on `master`. Do not create a pull request unless the repository owner explicitly asks for one.

## Documentation rule

After meaningful repository changes, review and update documentation whose status, commands, architecture, decisions, or scope changed. At minimum, keep this file synchronized with current implementation and verification reality.
