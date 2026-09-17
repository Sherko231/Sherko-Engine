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
| P4-T09 active | Issue #102 — bounded transform position/quaternion quantization helpers |
| P4-T09 activation baseline | `6eb3f71b847fa4b75a9e63030594e464b0c9aff2` after Markdown-only PR #178 reconciled Phase 4 handoff docs |
| P4-T09 branch | `p4-t09-transform-quantization` |
| Accepted sandbox maintenance | Issue #165 / PR #166 — persistent cumulative owner playground |
| Milestone | M1 — Engine Foundation remains in progress through P1-P4 |
| Independent feasibility follow-ups | P0-T09A / #42, P0-T13 / #43, P0-T14 / #44 |

## Accepted Phase 4 foundation

D-041 fixes right-handed world space with +X right, +Y up, -Z forward, meters, radians internally, right-hand positive rotation, and dimensionless scale. P4-T02 pins JOML 1.10.9 and records bounded hot-loop allocation evidence. D-042/P4-T03 provides public hierarchical `Transform`; P4-T04 rejects parent cycles atomically; P4-T05 propagates transform dirtiness only through affected descendants.

D-044/P4-T06 adds immutable public `Ray3f`, `Plane3f`, `Sphere3f`, `Aabb3f`, and `Frustum3f` geometry primitives with copied JOML inputs, normalized ray/plane definitions, exact boundary-inclusive tests, and no projection/depth assumption. P4-T06 final candidate `c515a1a1444b7d594eef519f6009e0a53d52b8c6` passed the required five heavy jobs in run #328. PR #174 merged as `19e790bec9ab2dc93d14b19e995bf212e169506c`; exact merged master passed run #329 Lightweight master verification. Issue #99 is closed completed.

D-045/P4-T08 adds public `CameraMatrices` view/perspective construction. View space is right-handed with camera forward on `-Z`; perspective uses vertical FOV radians, positive aspect/near with `far > near`, conventional finite non-reversed depth, and OpenGL NDC `[-1,+1]` with near/far at `-1/+1`. P4-T08 final candidate `2ac964f60f115a2cee58e0d8b34b4d586079f698` passed the five-job heavy matrix in run #330. PR #175 merged as `58b775fb634a1bb27ae6193194dd1c5d7f670ffa`; exact merged master passed run #331 Lightweight master verification. Issue #101 is closed completed.

D-046/P4-T07 adds public `ScreenRays.worldRay(...)` using top-left/Y-down continuous viewport samples, same-domain sample/viewport coordinates, raster pixel centers at `index + 0.5`, closed viewport boundaries, D-045 NDC depth `[-1,+1]`, inverse `projection * view` homogeneous unprojection, a near-plane ray origin, and normalized near-to-far direction. Final candidate `e065eedae8c26dd799f6d8c1c89bfa8928a9ae31` passed the required five-job matrix in run #332; PR #176 merged as `35e9ad2fe1802af8d1d71f564047e5c7b32ab85f`; exact merged master passed Lightweight run #333. Issue #100 is closed completed.

## P4-T09 implementation checkpoint — transform quantization

Issue #102 is active from exact accepted baseline `6eb3f71b847fa4b75a9e63030594e464b0c9aff2`.

The bounded implementation adds public `TransformQuantization` in `engine-core`:

- position quantization is independent per axis at exactly `1/64 m` into the full signed-short domain;
- valid position range is `[-512.0, 511.984375] m` with maximum `1/128 m` per-axis round-trip error and no saturation;
- quaternion quantization uses deterministic smallest-three encoding with normalization, lowest-index largest-component tie breaking, and sign canonicalization so `q` and `-q` encode identically;
- stored quaternion components use `[-32767,+32767]` with `Short.MIN_VALUE` reserved malformed;
- malformed decode checks reject records whose stored squared magnitude leaves no positive omitted component before mutating the caller destination;
- valid quaternion round-trip angular error is bounded by `0.0002 rad`;
- quantization returns immutable records, while dequantization writes into caller-owned JOML destinations and retains no caller objects.

D-047 records these value-level semantics. They do not define byte order, a production packet layout/version, entity/tick association, replication authority, transport, origin rebasing, delta compression, scale quantization, or a persisted transform format.

Deterministic `TransformQuantizationTest` covers exact position grid/endpoints, rounding boundaries, range/non-finite rejection, quaternion identity and axis half-turns, each omitted component, tie breaking, known component/code ordering, normalization, `q`/`-q` equivalence, fixed interior round trips, unit output, malformed records, and destination atomicity.

No dependency, lockfile, project edge, workflow, `Transform` behavior, networking module, ByteBuffer codec, renderer/platform/world/physics/game source, or sandbox source change is authorized or expected.

Independent review is required because this adds public API and D-047. No separate reviewer identity is available in the connected authoring environment at this checkpoint; final PR must record `not performed`, reason, reviewed/self-reviewed SHA, and residual range/rounding/quaternion-sign/error-bound risk. CI is not a substitute.

Wiki impact: updated API index, spatial conventions/usage, and limitations for the new public value API.

Sandbox impact: none — pure quantization has no meaningful interactive owner-facing path without inventing console-only diagnostics.

## Open gates and blockers

| Gate | Blocks | Current evidence gap |
| --- | --- | --- |
| P0-T09A / #42 | Production Steam transport work in P10/P13 | End-to-end two-process SteamNetworkingSockets lifecycle |
| P0-T13 / #43 | Claims of sustained native stability | 15-minute combined native run with retained evidence |
| P0-T14 / #44 | Claims of repeatable native lifecycle safety | 100 supported lifecycle cycles or explicit process-global limits |

None blocks bounded pure-Java P4-T09 quantization helpers. These feasibility gates remain independent and must not be represented as satisfied by Phase 4 math tests.

## Exact next action

Complete P4-T09 branch consistency/diff review, run the focused `TransformQuantizationTest` where Java 25/Gradle is available, open one final non-draft PR linked to #102, require the heavy five-job CI matrix on the exact final candidate, merge only while tested head/base remain current, then require the exact merged-master Lightweight verifier before closing #102. After P4-T09 is accepted, execute the separate Phase 4 exit-gate procedure from `docs/BUILD_AND_VERIFY.md`; do not claim Phase 4/M1 complete or activate Phase 5 implementation solely from P4-T09 task acceptance.
