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
| P4-T04 final candidate | `fb65fd1e4303fe1cacb1319866d2df198e6c9e7e` |
| P4-T04 heavy verification | run `35127781471` (#324), all five required jobs passed |
| P4-T04 merged `master` | `908000921ed93dfec242ca9719d2f53f03b75536` |
| P4-T04 exact-merge verification | run `35128432521` (#325), Lightweight master verification passed |
| Active executable task | P4-T05 / Issue #98 — descendant-only transform dirty propagation |
| P4-T05 activation baseline | `908000921ed93dfec242ca9719d2f53f03b75536` |
| Next planned after P4-T05 acceptance | P4-T06 / Issue #99 — audit live contract before activation |
| Accepted sandbox maintenance | Issue #165 / PR #166 — persistent cumulative owner playground |
| Milestone | M1 — Engine Foundation remains in progress through P1-P4 |
| Independent feasibility follow-ups | P0-T09A / #42, P0-T13 / #43, P0-T14 / #44 |

## Phase 3 accepted

Phase 3 is complete through P3-T10. The accepted platform/input foundation includes production GLFW/OpenGL window ownership, logical/framebuffer sizing, display modes, focus/cursor and relative mouse behavior, immutable renderer-frame hardware snapshots, strict data-driven action bindings, deterministic action evaluation, tick-aligned `PlayerInputCommand` plus fixed 126-byte replay/storage codec, deterministic headless replay, and deterministic `InputResponseSettings`.

`game-server` remains independent of `engine-platform-lwjgl`.

## Persistent sandbox accepted

`game-sandbox` is the canonical persistent cumulative owner-facing playground. It keeps implemented public timing/window/input/action/tick-command/response behavior active together until explicit `Ctrl+Q` exit. It remains visually empty until production renderer/world presentation exists.

A task extends the sandbox only when a capability is meaningfully usable through already-authorized public APIs. Otherwise it records `Sandbox impact: none — <reason>` rather than adding internals, direct native calls, or future-roadmap APIs merely for demonstration.

## Phase 4 accepted foundation through P4-T04

D-041 fixes the canonical right-handed world convention. P4-T02 pins JOML 1.10.9 and records bounded hot-loop allocation evidence. D-042/P4-T03 provides public hierarchical `Transform` with local `T * R * S`, world `parentWorld * local`, copied JOML inputs/destinations, and cached world matrices. D-043/P4-T04 makes `setParent(...)` reject self/indirect cycles atomically before hierarchy mutation.

P4-T04 final candidate `fb65fd1e4303fe1cacb1319866d2df198e6c9e7e` passed all five heavy jobs in run `35127781471`. PR #172 merged as `908000921ed93dfec242ca9719d2f53f03b75536`; exact merged master passed run `35128432521` Lightweight master verification. Issue #97 is closed completed.

## P4-T05 executable checkpoint — descendant-only dirty propagation

Issue #98 is activated from exact accepted master `908000921ed93dfec242ca9719d2f53f03b75536` after confirming #97 completion, no open PR conflict, and current `Transform` semantics.

The bounded implementation replaces P4-T03's temporary parent-world revision fallback with explicit subtree invalidation:

- `Transform` privately tracks child membership; no public child enumeration API is added;
- successful local position/rotation/scale mutation marks only that transform and its current descendants dirty;
- successful reparent/detach updates old/new private child membership and invalidates only the moved subtree;
- same-parent assignment remains a true no-op;
- P4-T04 cycle validation still occurs before any hierarchy membership mutation;
- world reads bring the requested parent current, then recompute only explicit dirty nodes;
- unrelated ancestors/sibling branches remain cached when a descendant changes;
- the previous `cachedParentWorldRevision` fallback is removed.

Tests use the existing private `worldRevision` field reflectively rather than introducing production diagnostics. Acceptance cases cover leaf-only invalidation, ancestor/middle subtree propagation, independent branch isolation, reparent, detach, same-parent no-op, and rejected-cycle integrity with hand-specified world positions.

D-044 records private child membership and descendant-only invalidation as the durable cache architecture. No dependency, lockfile, project edge, renderer/world/physics/game production source, serialization format, or sandbox source changes are part of P4-T05.

Independent review: required by the repository architecture-work contract. No separate reviewer identity is available in the connected authoring environment, so final PR/handoff must record `not performed` plus residual risk; CI is not a substitute.

Sandbox impact: none — descendant invalidation changes transform cache behavior but has no meaningful visual presentation until world/render integration exists.

## Open gates and blockers

| Gate | Blocks | Current evidence gap |
| --- | --- | --- |
| P0-T09A / #42 | Production Steam transport work in P10/P13 | End-to-end two-process SteamNetworkingSockets lifecycle |
| P0-T13 / #43 | Claims of sustained native stability | 15-minute combined native run with retained evidence |
| P0-T14 / #44 | Claims of repeatable native lifecycle safety | 100 supported lifecycle cycles or explicit process-global limits |

None blocks P4-T05 pure Java transform cache/hierarchy work.

## Exact next action

Finish P4-T05 docs/wiki/self-review and consistency audit, open one final non-draft PR for Issue #98, require the five-job heavy matrix on the exact final head, merge only that tested candidate if `master` remains current, require exact merged `master` Lightweight master verification, then close #98. Freshly audit P4-T06 / Issue #99 before any later implementation.
