# Sherko Engine Scope

This document is the product and architecture boundary for the roadmap. Changes here are high-impact because they can invalidate networking, rendering, packaging, asset, or tooling decisions.

## Product target

| Field | Locked baseline |
| --- | --- |
| Engine source language | Java only |
| Native libraries | Allowed through maintained Java bindings / Java native interop |
| Runtime | Java 25 LTS, bundled with the game |
| Initial platform | Windows x64 |
| Store/social layer | Steam |
| Game family | First- or third-person 3D, physics-heavy, humorous multiplayer co-op |
| Initial session size | 2-4 players |
| Initial hosting | Listen server; headless server build kept possible |
| Simulation tick | Fixed 60 Hz starting target |
| Snapshot send rate | 20 Hz starting target; profile before changing |
| Renderer | Forward renderer, limited dynamic lights, no ray tracing |
| World size | Small/medium complete-scene levels; no seamless open-world streaming in v1 |
| Authoring interchange | glTF 2.0 |
| Runtime assets | Cooked engine-specific formats |
| Minimum GPU | **UNLOCKED — must be chosen in P0-T01 before Phase 1** |

The minimum GPU is intentionally not invented here. The source roadmap requires one, but did not actually specify it. `P0-T01` is therefore not complete until a concrete minimum GPU is selected.

## Baseline technology choices

| Concern | Baseline | Why |
| --- | --- | --- |
| Build | Gradle multi-project | Keep engine modules isolated and dependency direction testable |
| Build-script target | Groovy DSL | Avoid adding Kotlin source/script as a project language; current repository still uses Kotlin DSL and must be migrated deliberately |
| Native API access | LWJGL 3.4.x | Low-level Java access to GLFW/OpenGL/OpenAL/Assimp/stb/shaderc and native APIs |
| Window/input | GLFW through LWJGL | Window lifecycle, cursor locking, keyboard/mouse/controller input |
| Renderer | OpenGL 4.6 Core through LWJGL | Smaller implementation surface than Vulkan for v1 |
| Math | JOML | Graphics-oriented mutable Java math types |
| Physics | Jolt Physics through Jolt JNI | Rigid bodies, constraints, collision queries, character-controller foundation |
| Asset import | Assimp through LWJGL | Offline cooker only; not a runtime asset format |
| Audio | OpenAL Soft through LWJGL + stb_vorbis | Positional audio without another engine-scale framework |
| Debug/editor UI | imgui-java | Internal tools |
| Scene/prefab authoring | Jackson JSON | Human-readable versioned data; no Java native serialization |
| Network encoding | Explicit ByteBuffer codecs | Bounded, versioned packets without reflection/Java serialization |
| Profiling | Java Flight Recorder + RenderDoc | JVM/CPU/GC and GPU-frame diagnosis |
| Packaging | jlink + jpackage | Self-contained Windows client/server distributions |

## Architectural constraints

- Steam lobbies do **not** solve gameplay transport.
- SteamNetworkingSockets Java feasibility is a Phase 0 gate, not an assumed fact.
- Jolt JNI native objects require explicit destruction; garbage collection is not native ownership.
- Allocation behavior in simulation/render hot paths must be measured and budgeted.
- Networked rigid-body physics is server-authoritative; matching physics libraries do not imply deterministic lockstep.
- No lower engine module may depend on game-specific modules/classes.
- Runtime gameplay does not parse authoring formats such as glTF/PNG/JPEG directly.
- Java object serialization is forbidden for disk/network protocols.

## Target module layout

```text
engine-root/
  engine-core/
  engine-platform-lwjgl/
  engine-render-opengl/
  engine-assets/
  engine-world/
  engine-physics-jolt/
  engine-audio-openal/
  engine-network-api/
  engine-network-ip/
  engine-steam/
  engine-editor/
  game-sandbox/
  game-client/
  game-server/
  test-support/
```

Dependency direction:

```text
game -> world/gameplay -> render/physics/audio/network -> assets/core -> platform
```

## Explicitly rejected for v1

- Vulkan as the first renderer.
- A custom rigid-body solver.
- Java object serialization on disk/network.
- Deterministic lockstep for the physics-heavy multiplayer game.
- Peer authority over arbitrary physics objects.
- A home-grown reliable-UDP production transport.
- A full editor before the vertical slice.
- A feature-maximal general-purpose ECS.
- A broad job system in the first playable build.

## Deferred until a real game proves the need

- Multiple rendering backends / Vulkan.
- Open world and seamless chunk streaming.
- General scripting language.
- General visual scripting.
- Render graph.
- Clustered / Forward+ lighting.
- Broad job system and multithreaded ECS scheduling.
- Host migration.
- Cross-platform accounts and cross-play.
- Mod SDK.
- Ray tracing.
- Destruction beyond authored breakable constraints.
- Full deterministic replay of native physics.
- Custom allocator replacing the JVM allocator.

## Scope-change rule

A new feature may enter v1 only when it is required to pass an existing roadmap milestone exit outcome. Otherwise it remains deferred until a real game demonstrates the need.
