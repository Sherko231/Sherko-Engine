# Sherko Engine — Development Status

> Commit-contained handoff checkpoint. Live GitHub Issues/PRs/workflows may be newer than this file; a fresh agent must inspect them before continuing.

## Checkpoint identity

| Field | Value |
| --- | --- |
| Active phase | Phase 5 — Rendering foundation |
| Completed milestone | M1 — Engine Foundation (Phases 1–4) |
| P4-T08 accepted | Issue #101 / PR #175; intentionally completed before P4-T07 |
| P4-T07 accepted | Issue #100 / PR #176 |
| P4-T09 accepted | Issue #102 / PR #179 |
| P4-T09 final candidate | `a07a0e8ee9dd0e062a557cebf0259fcfb9b8706c` |
| P4-T09 heavy verification | run `35246722603` (#334), all five required jobs passed after an infrastructure-only retry of the unchanged Windows native job |
| P4-T09 merged `master` | `66a81a418e0c953b8f00e54226265aa7cd226749` |
| P4-T09 exact-merge verification | run `35247709901` (#335), Lightweight master verification passed on the exact merge SHA |
| Phase 4 exit gate | Passed — spatial tests execute in `engine-core` independently of OpenGL and Jolt |
| Phase 4 exit/readiness record | Issue #180 / Markdown-only PR #181 |
| Phase 5 activation baseline | `41988dc60b3f36ea64687e733a9c2d5a594e50e3` |
| Active executable task | P5-T03 / Issue #185 — explicit owned OpenGL resource wrappers |
| Independent feasibility follow-ups | P0-T09A / #42, P0-T13 / #43, P0-T14 / #44 |

## Phase 4 completion

P4-T01 through P4-T09 are accepted. D-041 fixes right-handed world space with +X right, +Y up, -Z forward, meters, radians internally, right-hand positive rotation, and dimensionless scale. P4-T02 establishes the JOML mutable/preallocated hot-loop policy. D-042 through D-047 cover hierarchical transforms, cycle rejection, geometry primitives, view/projection semantics, screen-to-world mapping, and bounded transform position/quaternion quantization.

P4-T08 / Issue #101 was deliberately completed before P4-T07 so the screen-to-world implementation could consume an accepted projection/depth convention instead of guessing one.

P4-T09 / Issue #102 completed through PR #179. Final candidate `a07a0e8ee9dd0e062a557cebf0259fcfb9b8706c` passed the required five-job heavy workflow in run #334 / `35246722603`; the Windows native job was retried without changing the candidate after its first attempt received an external cancelled conclusion despite successful steps. The final workflow result is `success`. PR #179 merged as `66a81a418e0c953b8f00e54226265aa7cd226749`, and exact merged master passed Lightweight verification in run #335 / `35247709901`.

### Phase 4 exit evidence

The backlog exit gate is: **spatial tests pass independently of OpenGL and Jolt**.

The accepted P4-T09 candidate and merged master share repository tree `c2e290e844cbd4ae0f08f40796b19967cc254b84`. On that tree, heavy run #334's `Unit tests` job passed `Run root and subproject tests`. `engine-core/build.gradle.kts` declares only the scope-selected pure-Java JOML dependency (`api(libs.joml)`), has no project dependency, and targeted source search finds no LWJGL/OpenGL or Jolt usage in `engine-core` tests. The spatial test surface therefore executes independently of those native systems. Issue #180 records the gate and Phase 5 readiness review.

Conclusion: Phase 4 is complete and M1 — Engine Foundation is complete.

## Phase 5 readiness

Phase 5 is the current M2 focus. Its goal is a stable, inspectable 3D room without gameplay or physics dependencies.

Accepted prerequisites already exist:

- production GLFW/OpenGL 4.6 window/context lifecycle from Phase 3;
- canonical D-041 world-space contract;
- D-045 view/perspective convention and D-046 screen-to-world mapping;
- hierarchical transforms and geometry primitives in `engine-core`;
- existing `engine-render-opengl` module boundary;
- Java 25 / Windows x64 / OpenGL 4.6 Core / forward-renderer technology choices in `ENGINE_SCOPE.md`.

The owner removed mandatory hardware-baseline benchmarking from the roadmap on 2026-09-18. The 1080p60 target remains a development performance target, but exact minimum CPU/GPU/driver/RAM/VRAM qualification is not a current Phase 5 task or exit gate and must not be claimed without separate future evidence.

Issue #182 and follow-up #201 are closed as not planned after the owner removed mandatory hardware benchmarking from the roadmap. P5-T01 / #183 is accepted; P5-T02 / #184 is the active executable renderer-foundation task.

P5-T01 / #183 is accepted. PR #204 final head `8848b7fe7a9d2f55b14a294e4bdc4c6a3d8cd0d3` passed all five heavy CI jobs in run #352 / `35330036806`, including the Windows native OpenGL debug acceptance. It merged as `754f3ea5f1183c7a719de04776c503a0d00153cf`, and exact-merge Lightweight verification passed in run #353 / `35330526069`.

P5-T02 / #184 is accepted. PR #205 final head `7222a9a40279e65b40d70a36adf86d39165f7409` passed all five heavy CI jobs in run #354 / `35331271228`, merged as `be8263f728ddddcab609b3ad32111f71c3a9db75`, and exact-merge Lightweight verification passed in run #355 / `35331779335`.

P5-T03 / #185 is active on branch `p5-t03-opengl-resource-wrappers`. It adds internal owned wrappers for buffer, vertex array, texture, sampler, shader, program, and framebuffer resources; consumes the accepted thread guard; reuses the selected LWJGL OpenGL binding directly in the renderer module; and adds focused failure-injection plus bounded Windows native lifecycle evidence. It adds no draw submission, upload policy, material/asset behavior, cache/resource manager, or public renderer-resource API.

## Open gates and blockers

| Gate | Blocks | Current evidence gap |
| --- | --- | --- |
| P0-T09A / #42 | Production Steam transport work in P10/P13 | End-to-end two-process SteamNetworkingSockets lifecycle |
| P0-T13 / #43 | Claims of sustained native stability | 15-minute combined native run with retained evidence |
| P0-T14 / #44 | Claims of repeatable native lifecycle safety | 100 supported lifecycle cycles or explicit process-global limits |

The P0 follow-up gates do not block Phase 5 renderer-foundation work, but their unproven claims must remain narrow.

## Exact next action

Finalize P5-T03 / #185 on branch `p5-t03-opengl-resource-wrappers`: audit the complete diff, run the exact-final-head five-job CI matrix including the bounded P5-T03 Windows native acceptance, merge only the tested candidate, then require exact-merge Lightweight verification before closing #185. Do not pull P5-T04 upload policy, draw submission, materials, assets, or resource caching forward.
