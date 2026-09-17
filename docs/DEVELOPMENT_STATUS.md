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
| P4-T07 accepted | Issue #100 / PR #176 |
| P4-T07 final candidate | `e065eedae8c26dd799f6d8c1c89bfa8928a9ae31` |
| P4-T07 heavy verification | run `35148006238` (#332), all five required jobs passed after infrastructure-only retry on unchanged candidate |
| P4-T07 merged `master` | `35e9ad2fe1802af8d1d71f564047e5c7b32ab85f` |
| P4-T07 exact-merge verification | run `35149613106` (#333), Lightweight master verification passed on exact merged SHA |
| Next executable task | P4-T09 / Issue #102 — bounded transform position/quaternion quantization helpers |
| P4-T09 activation prerequisite | merge this Markdown-only handoff reconciliation, then activate #102 against the resulting current `master` SHA |
| Accepted sandbox maintenance | Issue #165 / PR #166 — persistent cumulative owner playground |
| Milestone | M1 — Engine Foundation remains in progress through P1-P4 |
| Independent feasibility follow-ups | P0-T09A / #42, P0-T13 / #43, P0-T14 / #44 |

## Accepted Phase 4 foundation

D-041 fixes right-handed world space with +X right, +Y up, -Z forward, meters, radians internally, right-hand positive rotation, and dimensionless scale. P4-T02 pins JOML 1.10.9 and records bounded hot-loop allocation evidence. D-042/P4-T03 provides public hierarchical `Transform`; P4-T04 rejects parent cycles atomically; P4-T05 propagates transform dirtiness only through affected descendants.

D-044/P4-T06 adds immutable public `Ray3f`, `Plane3f`, `Sphere3f`, `Aabb3f`, and `Frustum3f` geometry primitives with copied JOML inputs, normalized ray/plane definitions, exact boundary-inclusive tests, and no projection/depth assumption. P4-T06 final candidate `c515a1a1444b7d594eef519f6009e0a53d52b8c6` passed the required five heavy jobs in run #328. PR #174 merged as `19e790bec9ab2dc93d14b19e995bf212e169506c`; exact merged master passed run #329 Lightweight master verification. Issue #99 is closed completed.

D-045/P4-T08 adds public `CameraMatrices` view/perspective construction. View space is right-handed with camera forward on `-Z`; perspective uses vertical FOV radians, positive aspect/near with `far > near`, conventional finite non-reversed depth, and OpenGL NDC `[-1,+1]` with near/far at `-1/+1`. P4-T08 final candidate `2ac964f60f115a2cee58e0d8b34b4d586079f698` passed the five-job heavy matrix in run #330. PR #175 merged as `58b775fb634a1bb27ae6193194dd1c5d7f670ffa`; exact merged master passed run #331 Lightweight master verification. Issue #101 is closed completed.

D-046/P4-T07 adds public `ScreenRays.worldRay(...)` using top-left/Y-down continuous viewport samples, same-domain sample/viewport coordinates, raster pixel centers at `index + 0.5`, closed viewport boundaries, D-045 NDC depth `[-1,+1]`, inverse `projection * view` homogeneous unprojection, a near-plane ray origin, and normalized near-to-far direction. Final candidate `e065eedae8c26dd799f6d8c1c89bfa8928a9ae31` passed the required five-job matrix in run #332; PR #176 merged as `35e9ad2fe1802af8d1d71f564047e5c7b32ab85f`; exact merged master passed Lightweight run #333. Issue #100 is closed completed.

## P4-T09 readiness — transform quantization

P4-T09 / Issue #102 is the final listed Phase 4 implementation task. Its purpose is to add bounded position/quaternion quantization helpers in `engine-core` without integrating networking and without declaring a production transport packet layout.

Activation must make the Issue executable and resolve the previously open design choices before Java changes begin:

- position representable range and quantization precision/error bound;
- exact packed/value representation exposed by the helper API;
- quaternion normalization, omitted-largest-component representation, and `q`/`-q` sign equivalence/canonicalization;
- invalid, malformed, and out-of-range behavior;
- exact public API, authorized files, deterministic tests, documentation/wiki impact, and focused verification command;
- Phase 4 exit evidence remains separate from isolated P4-T09 unit acceptance.

`docs/SPATIAL_CONVENTIONS.md` remains authoritative for right-handed world space, meters, radians, and boundary conversion. P4-T09 may define its transform quantization semantics under its executable contract but must not silently redefine canonical engine units or freeze a future production network packet.

Independent review will be required if P4-T09 adds public API or a durable serialization/quantization decision. If unavailable, the implementation PR must record `not performed`, reason, and residual risk; CI is not a substitute.

Sandbox impact for activation: none — this reconciliation adds no engine capability. The P4-T09 implementation Issue must separately evaluate whether quantization is meaningfully owner-observable; a console-only diagnostic is not required merely to demonstrate pure encoding math.

## Open gates and blockers

| Gate | Blocks | Current evidence gap |
| --- | --- | --- |
| P0-T09A / #42 | Production Steam transport work in P10/P13 | End-to-end two-process SteamNetworkingSockets lifecycle |
| P0-T13 / #43 | Claims of sustained native stability | 15-minute combined native run with retained evidence |
| P0-T14 / #44 | Claims of repeatable native lifecycle safety | 100 supported lifecycle cycles or explicit process-global limits |

None blocks bounded pure-Java P4-T09 quantization helpers. These feasibility gates remain independent and must not be represented as satisfied by Phase 4 math tests.

## Exact next action

Merge the Markdown-only P4-T09 activation/handoff reconciliation after complete-diff review. Then update Issue #102 against the resulting current `master` SHA to `ACTIVE / executable contract`, with all quantization ranges/representation/failure/API/test/verification decisions fixed before implementation. Create the dedicated implementation branch only from that verified `master`. After P4-T09 implementation/verification/merge, execute the separate Phase 4 exit-gate procedure from `docs/BUILD_AND_VERIFY.md` before claiming Phase 4/M1 complete or activating Phase 5 implementation.