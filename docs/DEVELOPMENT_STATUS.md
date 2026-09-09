# Sherko Engine — Development Status

> This document is the **living development snapshot** for humans and AI agents working on Sherko Engine.
> It should explain what is currently happening, what has already been proven, what is temporary, and what should happen next.
>
> **GitHub Issues / Project remain the authoritative source for exact live task state.** This file is the fast orientation layer that keeps the repository understandable without reconstructing project history from commits and issues.

## Current position

- **Milestone:** M1 — Engine Foundation
- **Phase:** P1 — Build, modules, and quality gates
- **Current work package:** Multi-project Gradle foundation
- **M0 / Phase 0:** complete; exit gate passed
- **Current executable task:** P1-T01 — Initialize multi-project Gradle build (Issue #31)
- **Production engine architecture:** beginning with build/module boundaries; Phase 0 spike code remains temporary feasibility evidence

Phase 0 successfully discharged the major native, Steam, and networking feasibility risks that blocked foundation work. Phase 1 now establishes the project structure and quality boundaries that later engine code will depend on.

## Proven so far

| Task | Result | Durable conclusion |
| --- | --- | --- |
| P0-T01 | Complete | The v1 engine/product scope is explicitly bounded in `ENGINE_SCOPE.md`. |
| P0-T02 | Complete | Java 25 is pinned through Gradle toolchains and used by CI. |
| P0-T03 | Complete | LWJGL + GLFW can create the required OpenGL 4.6 Core context on the target development environment. |
| P0-T04 | Complete | Jolt JNI 6.0.0 loads and simulates correctly on the target Windows/Java environment; a dynamic rigid body falls onto a static floor, repeated start/stop succeeds, and the debug native-allocation balance returns to zero after cleanup. |
| P0-T05 | Complete | LWJGL OpenAL can open the default device/context, play a generated mono source with audible left/right 3D positioning, and explicitly delete the source/buffer before destroying the context/device. |
| P0-T06 | Complete | Two Java JVM processes can exchange numbered UDP datagrams over localhost using `DatagramChannel`; runtime verification received 8/8 replies and logged an average RTT of 0.459 ms on the development machine. |
| P0-T07 | Complete | Steamworks4j 1.10.0 successfully initializes Steam from Java when the Steam desktop client is running and logged in, receives asynchronous callbacks, and shuts down without a native crash. |
| P0-T08 | Complete — gate failed | Steamworks4j 1.10.0 does **not** expose the required `ISteamNetworkingSockets` listen-socket, connection, message, and connection-status APIs; its older P2P wrapper is not sufficient as the production transport binding. |
| P0-T09 | Complete | Java 25 FFM successfully loaded the official `steam_api64.dll`, initialized Steam through `SteamAPI_InitFlat`, obtained a non-null `ISteamNetworkingSockets` pointer, called the flat networking API, and received a valid result without authored C/C++ glue. |
| P0-T10 | Not required | The dedicated-server fallback trigger did not fire because P0-T09 established a viable Java-to-Steam flat-API path. |
| P0-T11 | Complete | The localhost UDP impairment harness successfully injected and independently observed latency, jitter, packet loss, duplication, and reordering. |
| P0-T12 | Complete | The owner-approved 15-second integrated JFR soak initialized GLFW/OpenGL, OpenAL, Jolt, and UDP together; UDP completed 58/58 echoes; Jolt allocation balance changed from 1 to 0; and shutdown completed cleanly. |

The M0 exit gate is therefore satisfied: a concrete Java production-networking path exists, and the selected native stack can run together cleanly in one process.

Detailed feasibility notes:

- `docs/feasibility/P0-T08_STEAM_NETWORKING_SOCKETS_COVERAGE.md`
- `docs/feasibility/P0-T11_NETWORK_IMPAIRMENT_HARNESS.md`
- `docs/feasibility/P0-T12_INTEGRATED_NATIVE_SOAK.md`

## Current experimental code

Phase 0 spikes are **feasibility probes, not production subsystem architecture**.

Current examples include:

- `src/main/java/com/samo/spike/opengl/OpenGL46Spike.java`
- `src/main/java/com/samo/spike/physics/JoltLifecycleSpike.java`
- `src/main/java/com/samo/spike/audio/OpenAL3DAudioSpike.java`
- `src/main/java/com/samo/spike/network/LocalhostUdpSpike.java`
- `src/main/java/com/samo/spike/network/NetworkImpairmentHarness.java`
- `src/main/java/com/samo/spike/steam/SteamInitSpike.java`
- `src/main/java/com/samo/spike/steam/SteamFlatApiFfmSpike.java`
- `src/main/java/com/samo/spike/integration/IntegratedNativeSoakSpike.java`

P1-T01 must preserve access to this evidence while introducing the initial multi-project build. Do not silently delete or reinterpret spike code as permanent engine APIs.

> **Spikes are disposable. Conclusions are durable.**

## Current technical baseline

The proven baseline entering Phase 1 is:

- Java 25 via Gradle toolchains
- Windows x64 as the initial platform target
- LWJGL 3.4.x family for native Java bindings
- GLFW + OpenGL 4.6 Core for rendering feasibility
- Jolt Physics through Jolt JNI for rigid-body physics feasibility
- OpenAL through LWJGL for 3D-audio feasibility
- Java NIO `DatagramChannel` for development UDP testing
- Steamworks4j 1.10.0 for basic Steam client initialization/callbacks only
- Java 25 FFM for direct access to the official Steam flat API / `ISteamNetworkingSockets` path
- explicit native-resource cleanup as a required ownership discipline
- development-only network impairment infrastructure for latency, jitter, loss, duplication, and reordering
- JFR available for runtime/native integration profiling

The production transport wrapper/design is still a later implementation task. P0 proved reachability and feasibility, not the final networking abstraction.

## What happens next

The immediate task is **P1-T01 — Initialize multi-project Gradle build** (Issue #31).

P1-T01 must establish exactly these initial modules:

- `engine-core`
- `test-support`
- `game-client`
- `game-server`

The Gradle Wrapper remains the project entry point. One root command must compile and test all four modules successfully. Existing Phase 0 spike code must remain accessible during the transition.

P1-T02 will add the remaining planned modules only after this initial structure is proven.

## Documentation maintenance policy

Repository documentation is part of the implementation, not cleanup work to postpone until the end of the project.

### Required rule

After every **meaningful repository development**, review the Markdown documentation and update every document whose claims, status, decisions, commands, architecture, or scope were changed by that development.

At minimum, `docs/DEVELOPMENT_STATUS.md` must be reviewed and updated whenever a task is completed, a new task becomes current, a feasibility result changes a technical assumption, a permanent architectural decision is made, or an important blocker/failure changes the project's direction.

Do **not** create documentation churn for trivial formatting-only changes or implementation details that do not change the repository's useful state or understanding.

### Which document to update

| Document | Update when... |
| --- | --- |
| `docs/DEVELOPMENT_STATUS.md` | meaningful development changes current progress, proven conclusions, blockers, current/next work, or important implementation reality |
| `README.md` | the project identity, primary entry points, setup/onboarding, or high-level current phase changes |
| `ENGINE_SCOPE.md` | a product boundary, platform target, architecture boundary, or explicitly deferred capability changes |
| `ROADMAP.md` | milestone ordering, phase outcomes, execution policy, or roadmap-level process changes |
| `docs/roadmap/TECHNICAL_BACKLOG.md` | stable task definitions, dependencies, acceptance criteria, or task sequencing change |
| `docs/REFERENCES.md` | an engineering decision starts depending on a new important external reference, or an existing reference becomes obsolete |

### What to record from spikes

For experimental Phase 0 work, document the **result and decision**, not a tutorial of the temporary implementation.

Good documentation records the tested dependency/version or capability, whether the feasibility gate passed or failed, important compatibility/lifecycle constraints discovered, and decisions that future production code must preserve.

Avoid documenting temporary class structure, helper methods, or one-off spike code as if they were engine architecture.

## AI / contributor handoff protocol

Before making a non-trivial change, an AI agent or contributor should read, in this order when relevant:

1. `README.md`
2. `docs/DEVELOPMENT_STATUS.md`
3. `ENGINE_SCOPE.md`
4. `ROADMAP.md`
5. the active GitHub Issue
6. the relevant section of `docs/roadmap/TECHNICAL_BACKLOG.md`

After the change:

1. verify the task's acceptance criteria with real evidence rather than assumptions;
2. update the relevant Markdown documents under the documentation-maintenance policy above;
3. keep temporary spike conclusions separate from permanent architecture;
4. keep the active GitHub Issue state consistent with actual verification results.

## Working repository convention

Current owner-directed development is performed directly on the explicitly selected branch. At the time of this snapshot, that branch is `master`.

Do not create a pull request unless the repository owner explicitly asks for one. Older documentation or issue text that assumes every change must go through a PR does not override this working convention.

## Status-file design rule

Keep this file concise enough to scan quickly. It should answer these questions without requiring commit-history archaeology:

- What phase are we in?
- What has actually been proven?
- What is the current/next task?
- What code is temporary?
- What architectural conclusions are durable?
- What should the next AI or developer read before changing the repository?
