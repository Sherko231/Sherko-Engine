from pathlib import Path


def read(path: str) -> str:
    return Path(path).read_text(encoding="utf-8")


def write(path: str, text: str) -> None:
    Path(path).write_text(text, encoding="utf-8")


def replace_once(path: str, old: str, new: str) -> None:
    text = read(path)
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{path}: expected one replacement, found {count}: {old[:120]!r}")
    write(path, text.replace(old, new, 1))


def replace_section(path: str, start_marker: str, end_marker: str, replacement: str) -> None:
    text = read(path)
    if text.count(start_marker) != 1 or text.count(end_marker) != 1:
        raise RuntimeError(f"{path}: section markers are not unique")
    start = text.index(start_marker)
    end = text.index(end_marker, start)
    write(path, text[:start] + replacement + text[end:])


# README / roadmap orientation: accepted P7-T04, current P7-T05.
replace_once(
    "README.md",
    "P7-T04 / Issue #420 is the current bounded task and defines only package-private fixed world update phases plus successful-phase structural-command flush boundaries; P7-T05 and later Phase 7 work remain separately materialized tasks.",
    "P7-T04 / Issue #420 is accepted through PR #421: final candidate `7bb6f7f69f8979b4f803dda9ea33d7490f55c616` passed the required five-job matrix in run #649 / `36997114403`, merged as `2098866d1ae01d6d80e66777e1830cb08b6c0bdd`, and exact merged master passed Lightweight verification in run #650 / `36997764861`. P7-T05 / Issue #422 is the current bounded task and adds only the five public data-oriented world components plus the API metadata needed by their existing `Transform` / `AssetId` signatures; P7-T06 and later Phase 7 work remain separately materialized tasks.",
)

replace_once(
    "ROADMAP.md",
    "P7-T04 / Issue #420 is the current bounded task and defines only the package-private fixed phase order plus structural-command flush boundaries; public world lifecycle, P7-T05 concrete components, prefabs/scenes, and later Phase 7 work remain separately materialized tasks.",
    "P7-T04 / Issue #420 is accepted through PR #421: final candidate `7bb6f7f69f8979b4f803dda9ea33d7490f55c616` passed run #649 / `36997114403`, merged as `2098866d1ae01d6d80e66777e1830cb08b6c0bdd`, and exact merged master passed Lightweight verification in run #650 / `36997764861`. P7-T05 / Issue #422 is the current bounded task for `TransformComponent`, `NameComponent`, `MeshRendererComponent`, `CameraComponent`, and `AudioEmitterComponent`; public world lifecycle, scene JSON/GUID work, renderer/audio bridges, prefabs/scenes, and later Phase 7 work remain separately materialized tasks.",
)

# Development status checkpoint and detailed Phase 7 handoff.
replace_once(
    "docs/DEVELOPMENT_STATUS.md",
    "| Active phase | Phase 7 active — P7-T01 / Issue #414, P7-T02 / Issue #416, and P7-T03 / Issue #418 are accepted; P7-T04 / Issue #420 is the current bounded task; Phase 6, including the accepted sandbox Asset Lab and visual diagnostics polish, remains complete |",
    "| Active phase | Phase 7 active — P7-T01 / #414 through P7-T04 / #420 are accepted; P7-T05 / Issue #422 is the current bounded task; Phase 6, including the accepted sandbox Asset Lab and visual diagnostics polish, remains complete |",
)
replace_once(
    "docs/DEVELOPMENT_STATUS.md",
    "| Active executable task | P7-T04 / Issue #420 — package-private fixed world update phases with a P7-T03 structural-command flush after each successfully completed phase; implementation is on `p7-t04-fixed-world-update-phases`; final PR/CI is not yet accepted |",
    "| Active executable task | P7-T05 / Issue #422 — five public data-oriented world components with stable `AssetId` references and canonical `Transform` reuse; implementation is on `p7-t05-core-world-components`; final PR/CI is not yet accepted |",
)

old_status_section = """## Phase 7 fixed world update phases — P7-T04 / Issue #420

P7-T04 is active from accepted P7-T03 merge baseline `e97dceb477d9492d28aa2a924e5c5adc93628ba8` on branch `p7-t04-fixed-world-update-phases`. The bounded candidate adds package-private `WorldUpdatePhase` and `WorldUpdatePipeline` only in `engine-world`. One caller-supplied callback runs exactly once in fixed order: INPUT, PRE_PHYSICS, PHYSICS, POST_PHYSICS, GAMEPLAY, REPLICATION_CAPTURE, PRESENTATION_EXTRACTION.

After each successfully completed phase callback, the pipeline invokes the accepted P7-T03 command buffer `flush()` before the next phase. Structural changes recorded by a phase therefore remain invisible during that callback and become visible at the next successful phase boundary. The final presentation-extraction phase is also followed by a flush. Callback or flush failure propagates and stops later phases; a failed callback is not auto-flushed, and command-buffer partial-failure/tail behavior remains P7-T03-owned. Recursive update is rejected, the guard recovers after failure, and no concurrency guarantee is introduced.

Authoring preflight run #6 / `36996034761` on committed Java head `a8c9d6682db4467aec9f33e798e5090684bf1524` passed `spotlessCheck`, `:engine-world:test --rerun-tasks`, and the architecture boundary test. This branch-only authoring evidence is not final acceptance evidence. The connected environment does not execute the Gradle wrapper locally, so no separate local command run is claimed.

P7-T04 adds no public `World`/phase/system API, timing/fixed-step loop, concrete P7-T05 components, physics stepping/Jolt dependency, replication/network implementation, renderer/presentation implementation, platform input acquisition, event/query scheduler, prefab/scene/GUID behavior, production dependency, module edge, Gradle change, or lockfile change.

Wiki impact: none — all P7-T04 types remain package-private and supported consumer API is unchanged.
Sandbox impact: none — there is still no public world lifecycle through which the sandbox could exercise update phases honestly.
Independent review: not performed in this connected session; no independent reviewer/provenance is available.

Exact next action: finish P7-T04 / Issue #420 only, complete final diff/self-review/consistency audit, open one final non-draft PR, require the five-job exact-head matrix, merge only while tested head/base remain current, then require exact-merge Lightweight verification before closing #420. Do not materialize P7-T05 before P7-T04 is accepted."""
new_status_section = """## Phase 7 fixed world update phases — P7-T04 / Issue #420 — accepted

P7-T04 is accepted. Final candidate `7bb6f7f69f8979b4f803dda9ea33d7490f55c616` passed all five required PR jobs in run #649 / `36997114403`; PR #421 merged as `2098866d1ae01d6d80e66777e1830cb08b6c0bdd`; exact merged `master` then passed Lightweight verification in run #650 / `36997764861`. Issue #420 closed completed after that exact-merge gate.

The accepted package-private `WorldUpdatePhase` / `WorldUpdatePipeline` order is INPUT, PRE_PHYSICS, PHYSICS, POST_PHYSICS, GAMEPLAY, REPLICATION_CAPTURE, PRESENTATION_EXTRACTION. A successful callback is followed by the P7-T03 structural-command flush before the next phase, including the final presentation boundary; callback/flush failure and recovery semantics remain as recorded by D-082.

Wiki impact: none — all P7-T04 phase/pipeline types remain package-private.
Sandbox impact: none — there is still no public world lifecycle through which the sandbox can invoke the pipeline honestly.
Independent review: not performed in the connected authoring session; acceptance relied on the required exact-head repository CI and exact-merge verification.

## Phase 7 core data-driven world components — P7-T05 / Issue #422

P7-T05 is active from accepted P7-T04 merge baseline `2098866d1ae01d6d80e66777e1830cb08b6c0bdd` on branch `p7-t05-core-world-components`. The bounded implementation adds exactly five supported public `engine-world` component types: `TransformComponent`, `NameComponent`, `MeshRendererComponent`, `CameraComponent`, and `AudioEmitterComponent`.

`TransformComponent` creates and owns a fresh canonical `engine-core Transform` from explicit local position/quaternion/scale data and exposes that owned transform for later hierarchy/world integration. `NameComponent` stores one nonblank string without silent normalization. `MeshRendererComponent` stores stable mesh/material `AssetId` values only. `CameraComponent` stores D-045 vertical FOV plus near/far plane data while framebuffer aspect remains runtime input and pose remains transform-owned. `AudioEmitterComponent` stores one stable audio `AssetId` only. No component performs I/O, resource loading, renderer/audio/native lookup, or service discovery.

Because supported public signatures expose existing `Transform` and `AssetId` types, `engine-world` publishes its already-existing `engine-core` and `engine-assets` edges through Gradle `api(...)` metadata. This adds no project edge, dependency family/version, lockfile change, reverse dependency, renderer/audio dependency, or persisted schema.

P7-T07 still owns scene JSON. P7-T05 tests represent scene-like parsed data in memory and construct all five components directly from that data, without introducing a production scene parser/schema, public `World`, GUIDs, entity lifecycle, component store/query API, world-to-renderer extraction, arbitrary mesh/material submission, runtime audio playback, physics integration, serialization, or networking.

Wiki impact: yes — the five new supported component APIs require `wiki/WORLD/COMPONENTS.md`, API index/navigation, and limitation updates.
Sandbox impact: none — public component values alone still cannot be exercised as an integrated world feature without a public world/entity lifecycle plus renderer/audio bridges that belong to later tasks.
Independent review: not performed in this connected session; no independent reviewer/provenance is currently available.

Exact next action: finish P7-T05 / Issue #422 only, complete full verification/documentation/wiki/API-metadata/spatial audits, remove temporary authoring tooling, open one final non-draft PR, require the five-job exact-head matrix, merge only while tested head/base remain current, then require exact-merge Lightweight verification before closing #422. Do not materialize P7-T06 before P7-T05 is accepted."""
replace_once("docs/DEVELOPMENT_STATUS.md", old_status_section, new_status_section)

# Architecture: append the bounded public component contract after accepted P7-T04.
architecture = read("docs/ARCHITECTURE.md")
heading = "## Phase 7 core data-driven world components — P7-T05 / Issue #422"
if heading in architecture:
    raise RuntimeError("docs/ARCHITECTURE.md: P7-T05 section already present")
architecture_addition = """


## Phase 7 core data-driven world components — P7-T05 / Issue #422

`engine-world` now adds exactly five supported public data-oriented component types without exposing a public world/container API. Each component can be created from explicit Java data before any renderer, audio backend, native resource, loader, or scene parser exists.

`TransformComponent` owns one fresh canonical `engine-core Transform`. Its constructor accepts local position XYZ in meters, quaternion XYZW, and dimensionless scale XYZ, delegates validation/normalization to the accepted D-041 `Transform`, and starts unparented. Returning the owned mutable `Transform` lets later bounded hierarchy/world work reuse the existing transform implementation rather than creating a second spatial-state/math contract.

`NameComponent` is an immutable non-null/nonblank string and preserves accepted text exactly. `MeshRendererComponent` is an immutable pair of mesh/material `AssetId` values. `AudioEmitterComponent` is an immutable audio `AssetId` reference. Those asset-facing components retain stable authored identities only: they do not retain `ResourceHandle`, decoded resource values, renderer/audio objects, GPU/OpenAL/native IDs, or paths, and construction performs no asset-type lookup or I/O.

`CameraComponent` is immutable projection configuration containing finite vertical FOV radians, positive near plane meters, and a farther finite far plane, matching the D-045 perspective domain that is independent of framebuffer aspect. Aspect remains runtime/framebuffer input. Camera pose comes from the same entity's `TransformComponent`; no duplicate camera position/orientation, matrix state, active-camera registry, or viewport policy is introduced.

Because the supported `TransformComponent.transform()` signature exposes `engine-core Transform` and mesh/material/audio components expose `engine-assets AssetId`, `engine-world` publishes its already-existing `engine-core` and `engine-assets` project dependencies through Gradle `api(...)` metadata. This changes consumer compile metadata only: no project edge, dependency version/family, lockfile, ownership direction, or native dependency is added.

D-083 records the stable component-data/reference policy. P7-T05 deliberately defines no public `World`, allocator/store/query/structural-command/update-phase API, GUIDs, scene JSON/schema/parser, persisted component format, prefab behavior, world activation, renderer extraction/submission, runtime audio loading/playback/spatialization, physics/Jolt integration, networking identity/protocol, concurrency, or later P7-T06+ behavior.

Wiki impact: yes — the supported component APIs and their current integration limits are documented under `wiki/WORLD/COMPONENTS.md`, the API index, navigation, and limitations guide.
Sandbox impact: none — the public values cannot yet drive an honest sandbox world feature without later public world lifecycle and renderer/audio integration boundaries.
"""
write("docs/ARCHITECTURE.md", architecture.rstrip() + architecture_addition + "\n")

# Durable decision D-083.
decision_marker = "\n## Adding or changing a decision\n"
d083 = """
| D-083 | Accepted | P7-T05 / Issue #422 defines exactly five supported public `engine-world` data components. `TransformComponent` constructs and owns one canonical mutable `engine-core Transform` from explicit local position/quaternion/scale data; `NameComponent` preserves one nonblank name; `MeshRendererComponent` retains stable mesh/material `AssetId` values; `CameraComponent` retains D-045 vertical-FOV/near/far configuration while pose comes from the entity transform and aspect remains runtime input; `AudioEmitterComponent` retains one stable audio `AssetId`. Asset-facing components never retain `ResourceHandle`, loaded/backend/native resource objects, or paths and perform no I/O/type lookup. The existing `engine-world -> engine-core` and `engine-world -> engine-assets` edges are published as Gradle API metadata because those types appear in supported public signatures. P7-T05 defines no public `World`, scene schema/parser, GUID/persistence contract, renderer/audio/physics bridge, or runtime resource policy. | Phase 7 needs authored component data that can exist before scene loading and subsystem adapters while remaining compatible with the stable Phase 4 spatial and Phase 6 asset-identity boundaries. Reusing the canonical Transform avoids divergent transform math/hierarchy semantics; storing AssetIds instead of live handles keeps authored world data backend-neutral and relocatable; leaving aspect/runtime resource resolution to later adapters avoids freezing presentation/audio or P7-T07 serialization policy prematurely. |
"""
decisions = read("docs/DECISIONS.md")
if "| D-083 |" in decisions:
    raise RuntimeError("docs/DECISIONS.md: D-083 already present")
replace_once("docs/DECISIONS.md", decision_marker, "\n" + d083 + decision_marker)

# Wiki API index: replace only the engine-world block.
world_api_section = """## `engine-world` — `com.samo.engine.world.api`

| Type | Purpose |
| --- | --- |
| `EntityId` | Immutable generational world-entity identity containing a non-negative reusable index plus non-negative generation. |
| `TransformComponent` | Mutable spatial component that owns one canonical `engine-core Transform` created from explicit local position/quaternion/scale data. |
| `NameComponent` | Immutable nonblank human-readable entity name preserving accepted text exactly. |
| `MeshRendererComponent` | Immutable stable mesh/material `AssetId` references; it does not load or submit renderer resources. |
| `CameraComponent` | Immutable vertical-FOV/near/far perspective configuration; pose comes from transform and framebuffer aspect stays runtime-owned. |
| `AudioEmitterComponent` | Immutable stable audio `AssetId` reference; it does not load, play, or own backend audio state. |

`EntityId` is the complete identity pair; callers must not compare or retain only the index. P7-T01 keeps allocation, destruction, liveness tracking, index reuse, and generation retirement inside `engine-world` rather than freezing a public world-management API early. Any later reuse of an internal slot returns its index with the previous generation plus one, and generation wraparound is forbidden: a slot destroyed at `Integer.MAX_VALUE` generation is retired permanently.

P7-T05 component construction is data-only. `TransformComponent` creates and owns a fresh canonical `Transform`; mesh/material/audio references are stable path-independent `AssetId` values rather than loaded `ResourceHandle` or backend/native identifiers. `CameraComponent` stores only projection values independent of framebuffer aspect. These component values do not perform I/O and do not imply that world-to-renderer or runtime-audio integration exists.

There is still no public `World`, entity allocator, component store/query API, deferred structural-command API, scene JSON/schema/parser, prefab lifecycle, GUID/persistence contract, renderer extraction bridge, audio playback bridge, replication identity, or entity serialization contract. P7-T07 still owns the first planned scene JSON format.

Usage: [Entity identities](WORLD/ENTITY_IDS.md) and [World components](WORLD/COMPONENTS.md).

"""
replace_section("wiki/API_INDEX.md", "## `engine-world` — `com.samo.engine.world.api`\n", "## `engine-render-opengl` — `com.samo.engine.render.api`\n", world_api_section)

# Wiki limitations: update camera/component and world availability claims.
replace_once(
    "wiki/LIMITATIONS.md",
    "- no public camera component/object or Transform-to-camera decomposition API exists;",
    "- public `engine-world CameraComponent` now stores vertical-FOV/near/far data, but there is still no active-camera selection, viewport/aspect ownership, Transform-to-view decomposition helper, or world-to-renderer camera extraction API;",
)
replace_once(
    "wiki/LIMITATIONS.md",
    "`engine-world` now exposes P7-T01 immutable `EntityId(index,generation)` values as the complete public world-entity identity pair. Allocation, destruction, liveness checks, index reuse bookkeeping, and generation retirement remain package-private; there is still no public `World`, public entity allocator, component store, structural-command API, prefab/scene entity lifecycle, entity persistence/serialization format, replication/network identity, or concurrency contract.",
    "`engine-world` exposes P7-T01 immutable `EntityId(index,generation)` values plus the five P7-T05 public data-oriented components. `TransformComponent` owns a canonical mutable `engine-core Transform`; `NameComponent` holds a nonblank name; `MeshRendererComponent` holds stable mesh/material `AssetId` values; `CameraComponent` holds vertical-FOV/near/far projection data; and `AudioEmitterComponent` holds one stable audio `AssetId`. These components perform no I/O/resource lookup and expose no backend/native handles. Allocation, destruction, liveness checks, index reuse bookkeeping, and generation retirement remain package-private; there is still no public `World`, entity allocator, component store/query API, structural-command API, scene JSON/schema/parser, prefab/scene entity lifecycle, GUID/persistence format, world-to-renderer extraction/submission, runtime audio loading/playback bridge, replication/network identity, or concurrency contract.",
)

# Wiki navigation/questions.
replace_once(
    "wiki/README.md",
    "- Which APIs are production-ready today, and which features are still planned?",
    "- How do I construct the core world components entirely from data?\n- Which APIs are production-ready today, and which features are still planned?",
)
replace_once(
    "wiki/README.md",
    "21. [Entity identities](WORLD/ENTITY_IDS.md)\n22. [Current limitations](LIMITATIONS.md)\n23. [How this wiki must be maintained](MAINTENANCE.md)",
    "21. [Entity identities](WORLD/ENTITY_IDS.md)\n22. [World components](WORLD/COMPONENTS.md)\n23. [Current limitations](LIMITATIONS.md)\n24. [How this wiki must be maintained](MAINTENANCE.md)",
)

# New focused component consumer page.
components_path = Path("wiki/WORLD/COMPONENTS.md")
if components_path.exists():
    raise RuntimeError("wiki/WORLD/COMPONENTS.md already exists")
components_path.write_text(
    """# World components

P7-T05 adds five public data-oriented component values under `com.samo.engine.world.api`. They can be constructed before a public `World`, scene JSON parser, renderer extraction bridge, or runtime audio system exists.

## Construct components from data

```java
AssetId meshId = new AssetId(1L, 2L);
AssetId materialId = new AssetId(3L, 4L);
AssetId audioId = new AssetId(5L, 6L);

TransformComponent transform = new TransformComponent(
    1.0f, 2.0f, -3.0f,   // local position, meters
    0.0f, 0.0f, 0.0f, 1.0f, // quaternion XYZW
    1.0f, 1.0f, 1.0f);   // dimensionless scale
NameComponent name = new NameComponent("Camera Rig");
MeshRendererComponent mesh = new MeshRendererComponent(meshId, materialId);
CameraComponent camera = new CameraComponent((float) Math.toRadians(60.0), 0.1f, 250.0f);
AudioEmitterComponent audio = new AudioEmitterComponent(audioId);
```

No constructor above performs file I/O, resource loading, renderer/audio lookup, or native allocation.

## Transform component

`TransformComponent` creates and owns a fresh `engine-core Transform` from the supplied local values. Position uses the canonical D-041 meter-based right-handed world (+X right, +Y up, -Z forward). Quaternion input is validated and normalized by the existing `Transform`; scale is dimensionless and follows the existing transform contract.

Call `transform()` to access the component-owned mutable `Transform` for local changes or later hierarchy integration. Construction starts unparented. The component does not accept a caller-owned `Transform`, so it cannot accidentally alias a mutable transform owned elsewhere.

P7-T05 does not define entity/transform parenting from scene data. Stable GUIDs and scene parent resolution remain later Phase 7 work.

## Name component

`NameComponent` is immutable. Names must be non-null and nonblank. Accepted text is preserved exactly; leading/trailing whitespace is not silently trimmed.

## Mesh renderer component

`MeshRendererComponent` stores exactly two stable authored identities: `meshAssetId` and `materialAssetId`.

Those values are references only. The component does not call `AssetLoader`, retain `ResourceHandle`, validate an asset's runtime type, upload GPU resources, or submit a draw. Storing these AssetIds therefore does **not** mean arbitrary world mesh/material rendering is implemented yet.

## Camera component

`CameraComponent` stores only perspective values that are stable scene data:

- vertical FOV in radians, strictly between 0 and PI;
- positive near plane in meters;
- finite far plane greater than near.

Camera pose comes from the same entity's `TransformComponent`. Framebuffer aspect is deliberately not stored because it depends on the active runtime viewport/framebuffer. P7-T05 adds no active-camera registry, viewport policy, matrix cache, or world-to-renderer camera extraction.

## Audio emitter component

`AudioEmitterComponent` stores one stable audio `AssetId` only. It does not load/decode audio, create an OpenAL source, start playback, store gain/loop/attenuation settings, or own native state. Runtime audio loading/playback/spatialization remain later bounded tasks.

## What is still missing

These public component values are not a public ECS/world lifecycle by themselves. There is currently no public `World`, entity allocator, component store/query API, scene JSON/schema/parser, stable authoring GUID resolution, prefab lifecycle, world-to-renderer extraction, or runtime audio bridge.

P7-T07 is the planned owner of versioned scene JSON. Until those later boundaries exist, construct/use these values directly only where your own code already owns their lifecycle; do not invent a persistence or renderer/audio integration contract around them.
""",
    encoding="utf-8",
)

print("P7-T05 documentation/wiki synchronization completed")
