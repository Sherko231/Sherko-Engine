# Sherko Engine — Development Status

> This document is the **living development snapshot** for humans and AI agents working on Sherko Engine.
> It should explain what is currently happening, what has already been proven, what is temporary, and what should happen next.
>
> **GitHub Issues / Project remain the authoritative source for exact live task state.** This file is the fast orientation layer that keeps the repository understandable without reconstructing project history from commits and issues.

## Current position

- **Milestone:** M0 — Feasibility
- **Phase:** P0 — Feasibility
- **Current work package:** Networking/Steam feasibility
- **Completed executable tasks:** P0-T01 through P0-T06
- **Current executable task:** P0-T07 — Prove Steam initialization and callback pump
- **Production engine architecture:** not started yet; Phase 0 code is primarily feasibility work

Phase 0 exists to disprove risky native, rendering, physics, audio, networking, and Steam assumptions before permanent engine architecture depends on them.

## Proven so far

| Task | Result | Durable conclusion |
| --- | --- | --- |
| P0-T01 | Complete | The v1 engine/product scope is explicitly bounded in `ENGINE_SCOPE.md`. |
| P0-T02 | Complete | Java 25 is pinned through Gradle toolchains and used by CI. |
| P0-T03 | Complete | LWJGL + GLFW can create the required OpenGL 4.6 Core context on the target development environment. |
| P0-T04 | Complete | Jolt JNI 6.0.0 loads and simulates correctly on the target Windows/Java environment; a dynamic rigid body falls onto a static floor, repeated start/stop succeeds, and the debug native-allocation balance returns to zero after cleanup. |
| P0-T05 | Complete | LWJGL OpenAL can open the default device/context, play a generated mono source with audible left/right 3D positioning, and explicitly delete the source/buffer before destroying the context/device. |
| P0-T06 | Complete | Two Java JVM processes can exchange numbered UDP datagrams over localhost using `DatagramChannel`; runtime verification received 8/8 replies and logged an average RTT of 0.459 ms on the development machine. |
| P0-T07 | In progress | A Steamworks4j 1.10.0 spike is implemented to initialize Steam from Java, print the active persona/Steam ID, request an asynchronous callback, pump callbacks for up to 10 seconds, dispose wrapper callbacks, and shut Steam down cleanly. Runtime verification is still required. |

## Current experimental code

Phase 0 spikes are **feasibility probes, not production subsystem architecture**.

Current examples include:

- `src/main/java/com/samo/spike/opengl/OpenGL46Spike.java`
- `src/main/java/com/samo/spike/physics/JoltLifecycleSpike.java`
- `src/main/java/com/samo/spike/audio/OpenAL3DAudioSpike.java`
- `src/main/java/com/samo/spike/network/LocalhostUdpSpike.java`
- `src/main/java/com/samo/spike/steam/SteamInitSpike.java`

Do not infer renderer, physics, audio, networking, Steam integration, scene, resource-management, or gameplay architecture from these files. They may be simplified, rewritten, moved, or deleted after their conclusions have been captured.

> **Spikes are disposable. Conclusions are durable.**

## Current technical baseline

The current proven/selected baseline relevant to work completed so far is:

- Java 25 via Gradle toolchains
- Windows x64 as the initial platform target
- LWJGL 3.4.x family for native Java bindings
- GLFW + OpenGL 4.6 Core for the rendering feasibility path
- Jolt Physics through Jolt JNI for rigid-body physics feasibility
- OpenAL through LWJGL for 3D-audio feasibility
- Java NIO `DatagramChannel` is proven for the basic localhost UDP feasibility path
- Steamworks4j 1.10.0 is the selected disposable Java Steam binding under evaluation in P0-T07/P0-T08; P0-T08 must still prove whether its `ISteamNetworkingSockets` API coverage is sufficient for production gameplay transport
- Explicit native-resource cleanup is required; native ownership must not be left to accidental GC timing

For the complete intended v1 technology and product boundaries, read `ENGINE_SCOPE.md`. A dependency appearing in a Phase 0 spike does not by itself make its spike structure a permanent engine API.

## What happens next

The immediate task is **P0-T07 — Prove Steam initialization and callback pump**.

The current spike uses Steamworks4j 1.10.0 with its LWJGL3 loader. For local development it generates `build/spikes/steam/steam_appid.txt` with Valve's public Spacewar test App ID (`480`); the generated file lives under `build/` and is not a project/product App ID. The runtime test must be performed with the Steam desktop client running and logged in.

Verification must confirm that Steam initializes, the active persona/Steam ID are printed, at least one asynchronous callback is received through `SteamAPI.runCallbacks()`, and shutdown completes without a native crash. The spike deliberately does not implement lobbies, invites, gameplay transport, or replication.

After P0-T07, **P0-T08** must inspect the selected binding's exact `ISteamNetworkingSockets` coverage before the engine commits to it as a production transport path.

The production networking path remains a milestone gate: Phase 1 must not begin until that path is concrete.

See `ROADMAP.md` and `docs/roadmap/TECHNICAL_BACKLOG.md` for ordering and acceptance criteria.

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

Good documentation:

- the tested dependency/version or capability;
- whether the feasibility gate passed or failed;
- important compatibility or lifecycle constraints discovered;
- decisions that future production code must preserve.

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

Do not create a pull request unless the repository owner explicitly asks for one. Older documentation or issue text that assumes every change must go through a PR should not override this current working convention.

## Status-file design rule

Keep this file concise enough to scan quickly. It should answer these questions without requiring commit-history archaeology:

- What phase are we in?
- What has actually been proven?
- What is the current/next task?
- What code is temporary?
- What architectural conclusions are durable?
- What should the next AI or developer read before changing the repository?
