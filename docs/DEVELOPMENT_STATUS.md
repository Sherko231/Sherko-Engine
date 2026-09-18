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
| Active executable task | P5-T00 / Issue #182 — representative Windows hardware baseline |
| P5-T00 branch | `p5-t00-hardware-baseline` |
| P5-T00 checkpoint | Exact-head 1080p60 fixture and CI evidence passed on the available development/reference machine; representative mainstream minimum is deferred to Phase 5 exit / #201 |
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

One entry requirement remains intentionally first in Phase 5: `ENGINE_SCOPE.md` requires exact minimum/reference Windows CPU, GPU, driver floor, RAM, and VRAM values selected from a representative benchmark **before Phase 5 renderer implementation**. P5-T00 owns that work.

Phase 5 tasks are materialized as Issues #182–#199, but P5-T01 through P5-T17 remain blocked on accepted P5-T00 evidence. Later Phase 6+ work remains backlog-only.

## P5-T00 implementation checkpoint

The activation audit found no existing repository benchmark capable of supporting a 1080p60 minimum/reference hardware claim. The old P0 OpenGL spike only clears a framebuffer and cannot be used as a representative renderer workload.

Issue #182 was therefore refined before implementation to authorize one disposable measurement path under `feasibility-spikes`, using only its existing LWJGL dependencies and no production renderer code.

The branch now contains `P5HardwareBaselineSpike` plus `:feasibility-spikes:runP5HardwareBaseline`. The frozen fixture uses a hidden OpenGL 4.6 Core context with an explicit offscreen 1920x1080 RGBA8 + depth24 framebuffer, 300 warm-up frames, 600 measured frames, 1000 individual indexed textured cube draws per frame, depth testing, back-face culling, one fixed directional-light calculation, disabled vsync, no presentation swap, and `glFinish` before each measured frame sample completes. Candidate acceptance is p95 synchronized frame time `<= 16.667 ms`.

The benchmark requires a clean Git checkout and an independently verified exact VRAM value. The report records repository SHA, clean-checkout status, Java version, Windows caption/version/build, CPU/RAM, Windows video-controller vendor/driver/PNP metadata, supplied exact VRAM, primary display mode, OpenGL vendor/version/renderer, fixed workload constants, mean/median/p95/p99/max synchronized frame time, and derived mean/p95 FPS. The report path is `build/spikes/p5-hardware-baseline/p5-t00-hardware-baseline.txt`.

P5-T00 exact-head run #342 passed the five ordinary CI jobs and the fixed benchmark fixture on the currently available development/reference machine. The physical machine is Intel Core Ultra 9 275HX with NVIDIA GeForce RTX 5090 Laptop GPU, 24463 MiB VRAM, 64 GiB-class RAM, Windows 11 x64, and NVIDIA OpenGL 4.6 driver 592.02; observed p95 synchronized frame time was approximately 0.82–0.90 ms across logical runner registrations. The owner confirmed all current runners map to this same physical machine, so these results are retained as development/reference evidence only. Issue #201 now owns the exact mainstream/minimum CPU/GPU/driver/RAM/VRAM proof and blocks Phase 5 exit rather than P5-T01. `ENGINE_SCOPE.md` records this distinction.

## Open gates and blockers

| Gate | Blocks | Current evidence gap |
| --- | --- | --- |
| P5-EXIT / #201 | Phase 5 completion and minimum-hardware support claims | Real representative mainstream/mid-range Windows benchmark establishing exact CPU/GPU/driver/RAM/VRAM floor at 1080p60 |
| P0-T09A / #42 | Production Steam transport work in P10/P13 | End-to-end two-process SteamNetworkingSockets lifecycle |
| P0-T13 / #43 | Claims of sustained native stability | 15-minute combined native run with retained evidence |
| P0-T14 / #44 | Claims of repeatable native lifecycle safety | 100 supported lifecycle cycles or explicit process-global limits |

The P0 follow-up gates do not block Phase 5 renderer-foundation work, but their unproven claims must remain narrow.

## Exact next action

Finalize P5-T00 as a development/reference calibration task: remove its temporary branch-specific evidence workflow, run the exact-final-head five-job heavy CI matrix, merge, run exact-merge Lightweight verification, and close #182. Then freshly activate P5-T01 / #183 against current `master`. Issue #201 remains open and blocks Phase 5 exit until real representative mainstream/mid-range hardware is available and proves the exact supported/minimum floor.
