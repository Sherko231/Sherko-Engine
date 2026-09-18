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
| Active executable task | P5-T00 / Issue #182 — remove obsolete hardware-baseline gate |
| P5-T00 branch | `p5-t00-hardware-baseline` |
| P5-T00 checkpoint | Owner removed mandatory hardware benchmarking from the roadmap; cleanup branch removes the spike/task and unblocks P5-T01 |
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

Issue #182 now owns only the cleanup that removes the obsolete P5-T00 benchmark code/task and stale documentation. Follow-up #201 is closed as not planned. After this cleanup merges, P5-T01 / #183 is the next executable renderer-foundation task.

## Open gates and blockers

| Gate | Blocks | Current evidence gap |
| --- | --- | --- |
| P0-T09A / #42 | Production Steam transport work in P10/P13 | End-to-end two-process SteamNetworkingSockets lifecycle |
| P0-T13 / #43 | Claims of sustained native stability | 15-minute combined native run with retained evidence |
| P0-T14 / #44 | Claims of repeatable native lifecycle safety | 100 supported lifecycle cycles or explicit process-global limits |

The P0 follow-up gates do not block Phase 5 renderer-foundation work, but their unproven claims must remain narrow.

## Exact next action

Merge the P5-T00 removal cleanup after its final verification, close #182 as not planned, then freshly activate P5-T01 / #183 against current `master`. No hardware benchmark or minimum-hardware gate remains in the current Phase 5 roadmap.
