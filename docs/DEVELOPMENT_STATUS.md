# Sherko Engine — Development Status

> Commit-contained handoff checkpoint. Live GitHub Issues/PRs/workflows may be newer than this file; a fresh agent must inspect them before continuing.

## Checkpoint identity

| Field | Value |
| --- | --- |
| Active phase | Phase 4 — Math and spatial conventions |
| Accepted through P4-T06 | Issues #94–#99; P4-T06 merged by PR #174 |
| P4-T06 final candidate | `c515a1a1444b7d594eef519f6009e0a53d52b8c6` |
| P4-T06 heavy verification | run `35132115464` (#328), all five required jobs passed after infrastructure-only retries on the same SHA |
| P4-T06 merged `master` | `19e790bec9ab2dc93d14b19e995bf212e169506c` |
| P4-T06 exact-merge verification | run `35135140749` (#329), Lightweight master verification passed |
| Active executable task | P4-T08 / Issue #101 — view and perspective projection matrices |
| P4-T08 activation baseline | `19e790bec9ab2dc93d14b19e995bf212e169506c` |
| Deferred until P4-T08 accepted | P4-T07 / Issue #100 — screen-to-world ray construction |
| Accepted sandbox maintenance | Issue #165 / PR #166 — persistent cumulative owner playground |
| Milestone | M1 — Engine Foundation remains in progress through P1-P4 |
| Independent feasibility follow-ups | P0-T09A / #42, P0-T13 / #43, P0-T14 / #44 |

## Accepted Phase 4 foundation through P4-T06

D-041 fixes right-handed world space with +X right, +Y up, -Z forward, meters, radians internally, right-hand positive rotation, and dimensionless scale. P4-T02 pins JOML 1.10.9 and records bounded hot-loop allocation evidence. D-042/P4-T03 provides public hierarchical `Transform`; P4-T04 rejects parent cycles atomically; P4-T05 propagates transform dirtiness only through affected descendants.

D-044/P4-T06 adds immutable public `Ray3f`, `Plane3f`, `Sphere3f`, `Aabb3f`, and `Frustum3f` geometry primitives with copied JOML inputs, normalized ray/plane definitions, exact boundary-inclusive tests, and no projection/depth assumption. P4-T06 final candidate `c515a1a1444b7d594eef519f6009e0a53d52b8c6` passed the required five heavy jobs in run #328. PR #174 merged as `19e790bec9ab2dc93d14b19e995bf212e169506c`; exact merged master passed run #329 Lightweight master verification. Issue #99 is closed completed.

## P4-T08 executable checkpoint — camera matrices

Fresh dependency review found that P4-T07/#100 explicitly requires the projection/depth convention owned by P4-T08/#101. P4-T08 is therefore executed first rather than silently selecting clip/depth semantics inside the ray task.

Issue #101 is activated from exact accepted master `19e790bec9ab2dc93d14b19e995bf212e169506c`. The bounded implementation adds public `CameraMatrices` in `engine-core`:

- `view(position, forward, up, destination)` constructs a right-handed world-to-view matrix with camera forward mapped to view-space `-Z`;
- `perspective(verticalFovRadians, aspectRatio, nearPlane, farPlane, destination)` constructs conventional finite perspective projection;
- FOV is vertical and in radians;
- near/far are meters with `near > 0` and `far > near`;
- NDC depth is the OpenGL `[-1,+1]` convention, near `-1`, far `+1`;
- reversed-Z is not selected;
- methods validate fully before mutating caller-owned destinations and allocate no temporary JOML objects in the production path.

D-045 records this durable convention. Analytical tests cover identity/translated/rotated views, non-unit/non-orthogonal inputs, invalid atomic failure, near/far NDC mapping, horizontal/vertical frustum edges, and destination reuse.

No dependency, lockfile, Gradle edge, renderer/platform/world/game source, Transform/geometry API, screen-to-world ray API, or sandbox source change is authorized.

Independent review: required for public API/durable architecture work. No separate reviewer identity is available in the connected authoring environment unless one is explicitly provided; final PR must record `not performed` and residual risk. CI is not a substitute.

Sandbox impact: none — camera matrix math has no production world/render presentation surface yet.

## Open gates and blockers

| Gate | Blocks | Current evidence gap |
| --- | --- | --- |
| P0-T09A / #42 | Production Steam transport work in P10/P13 | End-to-end two-process SteamNetworkingSockets lifecycle |
| P0-T13 / #43 | Claims of sustained native stability | 15-minute combined native run with retained evidence |
| P0-T14 / #44 | Claims of repeatable native lifecycle safety | 100 supported lifecycle cycles or explicit process-global limits |

None blocks P4-T08 pure Java camera math.

## Exact next action

Finish P4-T08 docs/wiki/self-review and consistency audit on `p4-t08-camera-matrices`, open one final non-draft PR for #101, require the five-job heavy matrix on the exact final head, merge only if the tested head/base remain current, require exact merged-master Lightweight verification, then close #101. After acceptance, return to P4-T07/#100 using D-045 rather than selecting another projection/depth convention.
