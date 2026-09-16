# Sherko Engine — Development Status

> Commit-contained handoff checkpoint. Live GitHub Issues/PRs/workflows may be newer than this file; a fresh agent must inspect them before continuing.

## Checkpoint identity

| Field | Value |
| --- | --- |
| Active phase | Phase 4 — Math and spatial conventions |
| Last accepted Phase 3 task | P3-T10 / Issue #93 / PR #160 |
| Accepted P4-T01 | Issue #94 / PR #161 — D-041 canonical spatial convention |
| Accepted P4-T02 | Issue #95 / PR #170 — JOML 1.10.9 hot-loop allocation evidence |
| P4-T02 final candidate | `55d677d3bb83b91a1ef23e31bf8b08ebc9d1e557` |
| P4-T02 heavy verification | run `35120185073` (#319), all five required jobs passed |
| P4-T02 merged `master` | `58131ddbb72876e4ec5a722bc25b5f17d777b854` |
| P4-T02 exact-merge verification | run `35121240096` (#320), lightweight master verification passed |
| Active executable task | P4-T03 / Issue #96 — cached hierarchical `Transform` |
| P4-T03 activation baseline | `58131ddbb72876e4ec5a722bc25b5f17d777b854` |
| Next planned after P4-T03 acceptance | P4-T04 / Issue #97 — transform cycle rejection |
| Accepted sandbox maintenance | Issue #165 / PR #166 — persistent cumulative owner playground |
| Milestone | M1 — Engine Foundation remains in progress through P1-P4 |
| Independent feasibility follow-ups | P0-T09A / #42, P0-T13 / #43, P0-T14 / #44 |

## Phase 3 accepted

Phase 3 is complete through P3-T10. The accepted platform/input foundation includes production GLFW/OpenGL window ownership, logical/framebuffer sizing, display modes, focus/cursor and relative mouse behavior, immutable renderer-frame hardware snapshots, strict data-driven action bindings, deterministic action evaluation, tick-aligned `PlayerInputCommand` plus fixed 126-byte replay/storage codec, deterministic headless replay, and deterministic `InputResponseSettings`.

`game-server` remains independent of `engine-platform-lwjgl`.

## Persistent sandbox accepted

`game-sandbox` is the canonical persistent cumulative owner-facing playground. It keeps already-implemented public timing/window/input/action/tick-command/response behavior active together until explicit `Ctrl+Q` exit. Existing owner controls include window-mode cycling, cursor capture, mouse sensitivity, and mouse-Y inversion. The playground remains visually empty until production renderer/world presentation exists.

Future tasks extend this same playground only when a capability is meaningfully usable through already-authorized public APIs. Otherwise the task records `Sandbox impact: none — <reason>` rather than adding internals, direct native calls, or future-roadmap APIs merely for demonstration.

## P4-T01 accepted — canonical spatial convention

D-041 and `docs/SPATIAL_CONVENTIONS.md` define:

- right-handed Cartesian world;
- +X right;
- +Y up;
- -Z forward (+Z backward);
- meters for linear world quantities;
- radians internally;
- positive rotation by the right-hand rule;
- dimensionless scale, `(1,1,1)` identity.

External libraries/formats convert at adapter/import/export boundaries instead of redefining engine world space. Projection/NDC depth policy, external-format/Jolt/OpenAL adapter details, and network quantization remain later bounded tasks.

## P4-T02 accepted — JOML allocation baseline

P4-T02 / Issue #95 introduced `org.joml:joml:1.10.9` into `engine-core` and added deterministic test evidence for the existing mutable/preallocated hot-loop policy. The acceptance workload exercises `Vector3f`, `Quaternionf`, and `Matrix4f` with caller-owned destinations, warms the exact workload, then uses Java 25 `com.sun.management.ThreadMXBean` current-thread allocation accounting. Every representative measured math pass must report zero heap bytes while an escaping allocating control must report positive allocation.

P2-T11 sampled JFR allocation evidence remains complementary and is not treated as proof of mathematical zero allocation. P4-T02 also verified D-041's +90° +Y mapping from canonical forward `(0,0,-1)` to left `(-1,0,0)`.

PR #170 final head `55d677d3bb83b91a1ef23e31bf8b08ebc9d1e557` passed all five heavy jobs in run `35120185073`. It merged as `58131ddbb72876e4ec5a722bc25b5f17d777b854`, and exact merged master passed lightweight run `35121240096`. Issue #95 is closed completed.

## P4-T03 executable checkpoint — cached hierarchical Transform

Issue #96 was activated from exact accepted master `58131ddbb72876e4ec5a722bc25b5f17d777b854` after a fresh audit of D-041, P4-T02/JOML ownership, current architecture, and the Phase 4 backlog.

The implementation checkpoint in this branch introduces public `com.samo.engine.core.api.Transform` with:

- local position, normalized quaternion rotation, and local scale;
- optional parent identity;
- local `T * R * S` composition;
- world `parentWorld * local` composition;
- cached world matrix plus local dirty state;
- lazy parent-world revision validation so a child read observes parent mutation without implementing P4-T05 descendant traversal;
- caller-owned JOML values/destinations copied rather than retained as mutable aliases;
- finite-value validation and atomic rejection of invalid rotation/position/scale input;
- zero and negative scale allowed for forward composition only.

D-042 records the public math ownership decision: beginning with `Transform`, JOML is the public spatial math type family for `engine-core`. `engine-core` therefore exposes JOML through its Gradle `api` dependency; mechanically affected dependency locks are reconciled without changing project dependency edges.

P4-T03 intentionally does not implement P4-T04 general parent-cycle rejection. Parent graphs must remain acyclic until #97 defines rejection behavior. It also does not implement P4-T05 child collections/descendant dirty propagation, inverse/decomposition APIs, Euler APIs, transform serialization/quantization, world/entity storage, renderer integration, or physics adapters.

Consumer documentation is synchronized in `wiki/CORE/TRANSFORMS.md`, `wiki/API_INDEX.md`, and `wiki/LIMITATIONS.md`.

Acceptance is not asserted by this commit-contained checkpoint. The exact final non-draft PR candidate must pass the five-job heavy matrix; exact merged master must then pass the lightweight verifier before Issue #96 closes.

Independent review: required by the public API/durable architecture contract. No separate reviewer identity is available in the connected authoring environment, so it must be recorded as not performed with residual risk rather than treated as satisfied by CI.

Sandbox impact: none — without renderer/world visualization, printing arbitrary transform matrices is not a meaningful owner-facing capability and would add diagnostic clutter while pulling no useful interactive behavior forward.

## Open gates and blockers

| Gate | Blocks | Current evidence gap |
| --- | --- | --- |
| P0-T09A / #42 | Production Steam transport work in P10/P13 | End-to-end two-process SteamNetworkingSockets lifecycle |
| P0-T13 / #43 | Claims of sustained native stability | 15-minute combined native run with retained evidence |
| P0-T14 / #44 | Claims of repeatable native lifecycle safety | 100 supported lifecycle cycles or explicit process-global limits |

None blocks P4-T03 pure Java/JOML transform work.

## Exact next action

Finish P4-T03 self-review and consistency audit, open one final non-draft PR for Issue #96, require the heavy five-job matrix on the exact final head, merge only that tested candidate if master remains current, require the exact-merge lightweight master verifier, then close #96. After acceptance, freshly audit P4-T04 / Issue #97 before implementation.
