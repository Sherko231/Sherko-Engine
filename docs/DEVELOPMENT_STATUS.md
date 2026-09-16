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
| P4-T03 final candidate | `046d46d6cc7663260f8ac50ecbb209a86b07fe94` |
| P4-T03 heavy verification | run `35125069380` (#322), all five required jobs passed |
| P4-T03 merged `master` | `fdc7c64488bcd78928f5c95759e616e89fd8b3bd` |
| P4-T03 exact-merge verification | run `35125956410` (#323), Lightweight master verification passed |
| Active executable task | P4-T04 / Issue #97 — reject transform parent cycles |
| P4-T04 activation baseline | `fdc7c64488bcd78928f5c95759e616e89fd8b3bd` |
| Next planned after P4-T04 acceptance | P4-T05 / Issue #98 — descendant dirty propagation |
| Accepted sandbox maintenance | Issue #165 / PR #166 — persistent cumulative owner playground |
| Milestone | M1 — Engine Foundation remains in progress through P1-P4 |
| Independent feasibility follow-ups | P0-T09A / #42, P0-T13 / #43, P0-T14 / #44 |

## Phase 3 accepted

Phase 3 is complete through P3-T10. The accepted platform/input foundation includes production GLFW/OpenGL window ownership, logical/framebuffer sizing, display modes, focus/cursor and relative mouse behavior, immutable renderer-frame hardware snapshots, strict data-driven action bindings, deterministic action evaluation, tick-aligned `PlayerInputCommand` plus fixed 126-byte replay/storage codec, deterministic headless replay, and deterministic `InputResponseSettings`.

`game-server` remains independent of `engine-platform-lwjgl`.

## Persistent sandbox accepted

`game-sandbox` is the canonical persistent cumulative owner-facing playground. It keeps implemented public timing/window/input/action/tick-command/response behavior active together until explicit `Ctrl+Q` exit. It remains visually empty until production renderer/world presentation exists.

A task extends the sandbox only when a capability is meaningfully usable through already-authorized public APIs. Otherwise it records `Sandbox impact: none — <reason>` rather than adding internals, direct native calls, or future-roadmap APIs merely for demonstration.

## P4-T01 accepted — canonical spatial convention

D-041 and `docs/SPATIAL_CONVENTIONS.md` define one right-handed world convention: +X right, +Y up, -Z forward, meters for linear world quantities, radians internally, right-hand positive rotation, and dimensionless transform scale. External systems convert at their adapter/import/export boundaries rather than redefining engine world space.

## P4-T02 accepted — JOML allocation baseline

P4-T02 / Issue #95 pinned JOML 1.10.9 in `engine-core` and added deterministic zero-byte post-warm-up current-thread allocation evidence for a representative preallocated mutable JOML workload. It also verified the D-041 +90° +Y mapping from forward `(0,0,-1)` to left `(-1,0,0)`.

Final candidate `55d677d3bb83b91a1ef23e31bf8b08ebc9d1e557` passed all five heavy jobs in run `35120185073`, merged as `58131ddbb72876e4ec5a722bc25b5f17d777b854`, and exact merged master passed lightweight run `35121240096`.

## P4-T03 accepted — cached hierarchical Transform

P4-T03 / Issue #96 introduced public `com.samo.engine.core.api.Transform` with local position, normalized quaternion rotation, local scale, optional parent identity, local `T * R * S`, world `parentWorld * local`, reusable matrix storage, cached world matrix, and lazy parent-world revision validation.

D-042 makes JOML the public math type family for `engine-core` spatial APIs. Public JOML values are copied rather than retained/exposed as internal mutable aliases. Zero and negative scale remain allowed for forward composition only; inverse/decomposition policy is still future work.

Final candidate `046d46d6cc7663260f8ac50ecbb209a86b07fe94` passed all five heavy jobs in run `35125069380`. PR #171 merged as `fdc7c64488bcd78928f5c95759e616e89fd8b3bd`; exact merged master passed run `35125956410` Lightweight master verification. Issue #96 is closed completed.

## P4-T04 executable checkpoint — transform parent cycle rejection

Issue #97 is activated from exact accepted master `fdc7c64488bcd78928f5c95759e616e89fd8b3bd` after a fresh audit of D-041/D-042, `Transform`, current Phase 4 backlog state, and live GitHub state.

The bounded implementation changes only `Transform.setParent(...)` failure semantics:

- same-parent assignment remains a no-op;
- `null` still detaches;
- self-parenting and assigning a descendant as the new parent are rejected before mutation;
- rejection uses `IllegalArgumentException` with stable message `parent assignment would create a transform cycle`;
- detection walks the proposed parent's current ancestor chain in O(depth), allocation-free;
- the previous hierarchy/cache/local transform state remains intact after rejection;
- legal unrelated/ancestor reparenting remains allowed.

P4-T04 intentionally does not add child collections or descendant dirty propagation; P4-T05 owns that optimization. It also adds no dependency, project edge, entity/world storage, serialization, renderer/physics integration, inverse/decomposition API, or concurrency contract.

D-043 records the durable cycle-rejection semantics. Consumer guidance is updated in `wiki/CORE/TRANSFORMS.md` and `wiki/LIMITATIONS.md` so callers no longer have to manually enforce acyclicity.

Independent review is required for this caller-visible failure-semantics decision. No separate reviewer identity is available in the connected authoring environment, so final handoff must record `not performed` and residual risk rather than treating CI as a substitute.

Sandbox impact: none — cycle rejection is failure prevention and there is still no meaningful renderer/world presentation path for hierarchy visualization.

## Open gates and blockers

| Gate | Blocks | Current evidence gap |
| --- | --- | --- |
| P0-T09A / #42 | Production Steam transport work in P10/P13 | End-to-end two-process SteamNetworkingSockets lifecycle |
| P0-T13 / #43 | Claims of sustained native stability | 15-minute combined native run with retained evidence |
| P0-T14 / #44 | Claims of repeatable native lifecycle safety | 100 supported lifecycle cycles or explicit process-global limits |

None blocks P4-T04 pure Java transform validation work.

## Exact next action

Finish P4-T04 code/tests/docs/wiki/self-review on its dedicated branch, then open one final non-draft PR for Issue #97. Require all five heavy jobs on the exact final head; merge only if that tested head and base are still current. Require exact merged `master` Lightweight master verification before closing #97. After acceptance, freshly audit P4-T05 / Issue #98 before implementation.
