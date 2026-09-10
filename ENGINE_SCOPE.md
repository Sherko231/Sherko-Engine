# Sherko Engine Scope

This document is the product and architecture boundary for the roadmap. Changes here are high-impact because they can invalidate networking, rendering, packaging, asset, or tooling decisions.

## Product target

| Field | Locked baseline |
| --- | --- |
| Engine/game runtime source | Java only |
| Native libraries | Allowed through maintained Java bindings / Java native interop |
| Runtime | Java 25 LTS, bundled with the game |
| Initial platform | Windows x64 |
| Store/social layer | Steam |
| Game family | First-person v1; third-person capability after the stable multiplayer vertical slice; 3D, physics-heavy, humorous co-op |
| Runtime session size | 1-4 players supported; intended co-op sessions contain 2-4 players |
| Initial hosting | Listen server; headless server build kept possible |
| Simulation tick | Fixed 60 Hz starting target |
| Snapshot send rate | 20 Hz starting target; profile before changing |
| Renderer | Forward renderer, limited dynamic lights, no ray tracing |
| World size | Small/medium complete-scene levels; no seamless open-world streaming in v1 |
| Authoring interchange | glTF 2.0 |
| Runtime assets | Cooked engine-specific formats |

## Baseline technology choices

| Concern | Baseline | Why |
| --- | --- | --- |
| Build | Gradle multi-project | Keep engine modules isolated and dependency direction testable |
| Build scripts | Gradle Kotlin DSL (`.gradle.kts`) | Build configuration is not shipped engine/game source; retaining it avoids a no-value migration |
| Native API access | LWJGL 3.4.x | Low-level Java access to GLFW/OpenGL/OpenAL/Assimp/stb/shaderc and native APIs |
| Window/input | GLFW through LWJGL | Window lifecycle, cursor locking, keyboard/mouse/controller input |
| Renderer | OpenGL 4.6 Core through LWJGL | Smaller implementation surface than Vulkan for v1 |
| Math | JOML | Graphics-oriented mutable Java math types |
| Physics | Jolt Physics through Jolt JNI | Rigid bodies, constraints, collision queries, character-controller foundation |
| Asset import | Assimp through LWJGL | Offline cooker only; not a runtime asset format |
| Audio | OpenAL Soft through LWJGL + stb_vorbis | Positional audio without another engine-scale framework |
| Runtime game UI | Engine-owned retained UI model with OpenGL renderer adapter | Menus, HUD, prompts, lobby, settings, and controller navigation without coupling game state to OpenGL |
| Debug/editor UI | imgui-java | Internal tools only; never the shipped game UI |
| Scene/prefab authoring | Jackson JSON | Human-readable versioned data; no Java native serialization |
| Network encoding | Explicit ByteBuffer codecs | Bounded, versioned packets without reflection/Java serialization |
| Profiling | Java Flight Recorder + RenderDoc | JVM/CPU/GC and GPU-frame diagnosis |
| Packaging | jlink + jpackage | Self-contained Windows client/server distributions |

## Architectural constraints

- Steam lobbies do **not** solve gameplay transport.
- Java FFM access to `ISteamNetworkingSockets` is proven, but end-to-end connection, callback, message ownership, and cleanup remain a required follow-up gate before production transport work.
- The 15-second integrated native run is a smoke test, not evidence of long-duration stability.
- Jolt JNI native objects require explicit destruction; garbage collection is not native ownership.
- Allocation behavior in simulation/render hot paths must be measured and budgeted.
- Networked rigid-body physics is server-authoritative; matching physics libraries do not imply deterministic lockstep.
- No lower engine module may depend on game-specific modules/classes.
- Runtime game UI is independent of imgui-java and exposes renderer-neutral draw data plus device-neutral navigation actions.
- Keyboard/mouse and controller navigation are supported architectural inputs from the first runtime UI slice.
- Runtime gameplay does not parse authoring formats such as glTF/PNG/JPEG directly.
- Java object serialization is forbidden for disk/network protocols.

## Target module layout

```text
engine-root/
  engine-core/
  engine-platform-lwjgl/
  engine-render-opengl/
  engine-ui/
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

Dependency direction principles:

```text
game rules -> world / UI / network APIs
client composition -> platform / render / audio / IP or Steam adapters
render-opengl -> UI draw data / assets / core / platform
server composition -> world / physics / network APIs and adapters
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

- Third-person camera and character presentation until the first-person multiplayer vertical slice is stable.
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

## Target hardware policy

- Product target: mainstream/mid-range Windows gaming PCs, not only the development machine.
- Initial performance target: 1080p at 60 FPS.
- Exact minimum CPU, GPU, driver, RAM, and VRAM values must be selected by a representative-hardware benchmark before Phase 5 renderer implementation.
- A successful run on a high-end development GPU does not establish the minimum supported hardware.

## Perspective policy

- The v1 vertical slice and first complete multiplayer loop are first-person.
- Core player, interaction, authority, and physics contracts must remain camera-agnostic.
- Third-person orbit/collision, shoulder switching, facing/aim alignment, and presentation acceptance are implemented only after multiplayer stability, without changing canonical collision or authority.

## Scope-change rule

A new feature may enter v1 only when it is required to pass an existing roadmap milestone exit outcome. Otherwise it remains deferred until a real game demonstrates the need.
