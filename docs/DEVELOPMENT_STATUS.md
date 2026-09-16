# Sherko Engine — Development Status

> Commit-contained handoff checkpoint. Live GitHub Issues/PRs/workflows may be newer than this file; a fresh agent must inspect them before continuing.

## Checkpoint identity

| Field | Value |
| --- | --- |
| Active phase | Phase 4 — Math and spatial conventions |
| Accepted through P4-T06 | Issues #94–#99; P4-T06 merged by PR #174 |
| P4-T08 accepted | Issue #101 / PR #175 |
| P4-T08 final candidate | `2ac964f60f115a2cee58e0d8b34b4d586079f698` |
| P4-T08 heavy verification | run `35137500332` (#330), all five required jobs passed after infrastructure-only retries on the same SHA |
| P4-T08 merged `master` | `58b775fb634a1bb27ae6193194dd1c5d7f670ffa` |
| P4-T08 exact-merge verification | run `35139973161` (#331), Lightweight master verification passed after codeload timeout retries |
| Active executable task | P4-T07 / Issue #100 — screen-to-world ray construction |
| P4-T07 activation baseline | `58b775fb634a1bb27ae6193194dd1c5d7f670ffa` |
| Accepted sandbox maintenance | Issue #165 / PR #166 — persistent cumulative owner playground |
| Milestone | M1 — Engine Foundation remains in progress through P1-P4 |
| Independent feasibility follow-ups | P0-T09A / #42, P0-T13 / #43, P0-T14 / #44 |

## Accepted Phase 4 foundation

D-041 fixes right-handed world space with +X right, +Y up, -Z forward, meters, radians internally, right-hand positive rotation, and dimensionless scale. P4-T02 pins JOML 1.10.9 and records bounded hot-loop allocation evidence. D-042/P4-T03 provides public hierarchical `Transform`; P4-T04 rejects parent cycles atomically; P4-T05 propagates transform dirtiness only through affected descendants.

D-044/P4-T06 adds immutable public `Ray3f`, `Plane3f`, `Sphere3f`, `Aabb3f`, and `Frustum3f` geometry primitives with copied JOML inputs, normalized ray/plane definitions, exact boundary-inclusive tests, and no projection/depth assumption. P4-T06 final candidate `c515a1a1444b7d594eef519f6009e0a53d52b8c6` passed the required five heavy jobs in run #328. PR #174 merged as `19e790bec9ab2dc93d14b19e995bf212e169506c`; exact merged master passed run #329 Lightweight master verification. Issue #99 is closed completed.

D-045/P4-T08 adds public `CameraMatrices` view/perspective construction. View space is right-handed with camera forward on `-Z`; perspective uses vertical FOV radians, positive aspect/near with `far > near`, conventional finite non-reversed depth, and OpenGL NDC `[-1,+1]` with near/far at `-1/+1`. P4-T08 final candidate `2ac964f60f115a2cee58e0d8b34b4d586079f698` passed the five-job heavy matrix in run #330. PR #175 merged as `58b775fb634a1bb27ae6193194dd1c5d7f670ffa`; exact merged master passed run #331 Lightweight master verification. Issue #101 is closed completed.

## P4-T07 executable checkpoint — screen-to-world rays

Issue #100 is activated from exact accepted master `58b775fb634a1bb27ae6193194dd1c5d7f670ffa` now that D-045 supplies its required projection/depth contract.

The bounded implementation adds public `ScreenRays.worldRay(...)` in `engine-core`:

- top-left screen/viewport origin, X right and Y down;
- continuous sample coordinates in the same domain as the viewport rectangle;
- raster pixel centers at `index + 0.5` when pixel coordinates are used;
- closed viewport boundary mapped to NDC `±1`;
- D-045 near/far clip depths `-1/+1`;
- inverse `projection * view` homogeneous unprojection;
- near-plane ray origin and normalized near-to-far direction;
- no implicit logical-window/framebuffer conversion and no OpenGL/GLFW dependency.

D-046 records this durable mapping. Analytical tests cover center/edge samples, Y direction, viewport offsets, rotated cameras, matrix immutability, singular/non-finite matrices, invalid homogeneous division, and interoperability with existing `Ray3f` geometry queries.

No dependency, lockfile, Gradle edge, renderer/platform/world/game source, `CameraMatrices` behavior, Transform behavior, physics query, or sandbox source change is authorized.

Independent review: required for public API/durable architecture work. No separate reviewer identity is available in the connected authoring environment unless one is explicitly provided; final PR must record `not performed` and residual risk. CI is not a substitute.

Sandbox impact: none — there is still no production world/render scene/picking presentation surface where screen rays can be honestly demonstrated without pulling future work forward.

## Open gates and blockers

| Gate | Blocks | Current evidence gap |
| --- | --- | --- |
| P0-T09A / #42 | Production Steam transport work in P10/P13 | End-to-end two-process SteamNetworkingSockets lifecycle |
| P0-T13 / #43 | Claims of sustained native stability | 15-minute combined native run with retained evidence |
| P0-T14 / #44 | Claims of repeatable native lifecycle safety | 100 supported lifecycle cycles or explicit process-global limits |

None blocks P4-T07 pure Java screen/world math.

## Exact next action

Finish P4-T07 implementation/docs/wiki/self-review and consistency audit on `p4-t07-screen-rays`, open one final non-draft PR for #100, require the five-job heavy matrix on the exact final head, merge only if the tested head/base remain current, require exact merged-master Lightweight verification, then close #100. Phase 4 exit remains a separate integration/planning gate.
