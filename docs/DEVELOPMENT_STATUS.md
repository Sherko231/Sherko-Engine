# Sherko Engine — Development Status

> Commit-contained handoff checkpoint. Live GitHub Issues/PRs/workflows may be newer than this file; a fresh agent must inspect them before continuing.

## Checkpoint identity

| Field | Value |
| --- | --- |
| Active phase | Phase 4 — Math and spatial conventions |
| Last accepted Phase 3 task | P3-T10 / Issue #93 / PR #160 |
| Accepted P4-T01 | Issue #94 / PR #161 — D-041 canonical spatial convention |
| Accepted P4-T02 | Issue #95 / PR #170 — JOML 1.10.9 hot-loop allocation evidence |
| Accepted P4-T03 | Issue #96 / PR #171 — D-042 public JOML spatial math + cached hierarchical `Transform` |
| Accepted P4-T04 | Issue #97 / PR #172 — D-043 atomic transform-parent cycle rejection |
| Accepted P4-T05 | Issue #98 / PR #173 — descendant-only transform dirty propagation |
| P4-T05 final candidate | `47773ab3a48f2326d86d95488ae9ba5feeb433c0` |
| P4-T05 heavy verification | run `35129645915` (#326), all five required jobs passed |
| P4-T05 merged `master` | `658bfb9b46ac6e97ff29acddd20e50eb3fb58855` |
| P4-T05 exact-merge verification | run `35130221168` (#327), Lightweight master verification passed |
| Active executable task | P4-T06 / Issue #99 — ray/plane/sphere/AABB/frustum primitives |
| P4-T06 activation baseline | `658bfb9b46ac6e97ff29acddd20e50eb3fb58855` |
| Next planned after P4-T06 acceptance | P4-T07 / Issue #100 — screen-to-world ray construction, dependent on later projection/depth contract review |
| Accepted sandbox maintenance | Issue #165 / PR #166 — persistent cumulative owner playground |
| Milestone | M1 — Engine Foundation remains in progress through P1-P4 |
| Independent feasibility follow-ups | P0-T09A / #42, P0-T13 / #43, P0-T14 / #44 |

## Phase 3 accepted

Phase 3 is complete through P3-T10. The platform/input foundation includes production GLFW/OpenGL window ownership, renderer-frame hardware/action state, deterministic input response, tick-aligned `PlayerInputCommand`, fixed 126-byte replay/storage codec, and deterministic headless input replay. `game-server` remains independent of `engine-platform-lwjgl`.

## Persistent sandbox accepted

`game-sandbox` is the canonical cumulative owner-facing playground. It remains visually empty until production renderer/world presentation exists. Pure math/cache capabilities with no meaningful public visual presentation record `Sandbox impact: none` instead of adding diagnostic-only showcase code.

## Phase 4 accepted foundation through P4-T05

D-041 fixes the canonical right-handed world convention: +X right, +Y up, -Z forward, meters, radians internally, right-hand positive rotation, and dimensionless scale.

P4-T02 pins JOML 1.10.9 and records bounded hot-loop allocation evidence. D-042/P4-T03 provides public hierarchical `Transform` with local `T * R * S`, world `parentWorld * local`, copied JOML inputs/destinations, and cached world matrices. D-043/P4-T04 rejects self/indirect parent cycles atomically.

P4-T05 replaced the temporary parent-revision cache fallback with explicit descendant-only dirty propagation. Successful local/reparent mutations dirty only the affected subtree; unrelated ancestors/sibling branches remain cached. Private child membership is an implementation detail and no public child-enumeration API was added.

P4-T05 final candidate `47773ab3a48f2326d86d95488ae9ba5feeb433c0` passed all five heavy jobs in run `35129645915`. PR #173 merged as `658bfb9b46ac6e97ff29acddd20e50eb3fb58855`; exact merged master passed run `35130221168` Lightweight master verification. Issue #98 is closed completed.

## P4-T06 executable checkpoint — spatial primitives

Issue #99 is activated from exact accepted master `658bfb9b46ac6e97ff29acddd20e50eb3fb58855` after confirming #98 completion, no open PR conflict, the D-041 spatial contract, and the separation from P4-T07/P4-T08.

The bounded implementation adds immutable public `engine-core` primitives:

- `Ray3f` with normalized direction, point evaluation, and plane/sphere/AABB forward intersections;
- normalized `Plane3f` using `normal dot point + offset = 0`;
- `Sphere3f` with inclusive point/sphere/AABB tests;
- `Aabb3f` with inclusive point/AABB/sphere tests;
- `Frustum3f` built from six inward-facing planes with inclusive point/sphere/AABB classification.

Production geometry comparisons are exact and boundary-inclusive with no hidden epsilon. Ray misses return `Float.NaN`; origin-on/inside returns `0`. Inputs are finite-validated, degenerate directions/normals and inverted AABBs fail before construction, and JOML inputs are copied.

P4-T06 deliberately does not add screen-to-world unprojection, view/projection construction, clip/NDC/depth policy, frustum extraction from matrices, renderer culling, physics/Jolt query adapters, serialization, or broad-phase structures. D-044 records the durable public geometry semantics.

Independent review: required for the new public API. If no separate reviewer identity is available, final PR/handoff records `not performed` and residual risk; CI is not a substitute.

Sandbox impact: none — these are world-geometry query primitives without a world/render presentation surface.

## Open gates and blockers

| Gate | Blocks | Current evidence gap |
| --- | --- | --- |
| P0-T09A / #42 | Production Steam transport work in P10/P13 | End-to-end two-process SteamNetworkingSockets lifecycle |
| P0-T13 / #43 | Claims of sustained native stability | 15-minute combined native run with retained evidence |
| P0-T14 / #44 | Claims of repeatable native lifecycle safety | 100 supported lifecycle cycles or explicit process-global limits |

None blocks P4-T06 pure Java spatial primitive work.

## Exact next action

Finish P4-T06 code/tests/docs/wiki/self-review and consistency audit on `p4-t06-spatial-primitives`, open one final non-draft PR for Issue #99, require all five heavy jobs on the exact final head, merge only if that tested head/base remain current, require exact merged `master` Lightweight master verification, then close #99. Freshly audit P4-T07/#100 and P4-T08/#101 dependency ordering before later implementation.
