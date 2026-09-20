# Phase 5R Exit Review — P5R-T26

Baseline: `master` `69c860f02aa54bdb8e8d32055f9e582dc19942b9`.

Issue: #306 — execute the mandatory Phase 5R Architecture & Refactor Hardening exit review before Phase 6 may be activated.

## Review state

**PASS. Phase 5R exit gate is satisfied.**

Final candidate `40f2bb91ae3afeeee07d7f8be92dd7b088419fa0` passed all five required jobs in PR run #495 / `35537699489`. PR #357 merged as `5e0cac4e7748a66b7c2d19e0cf444eaca0a49fce`, and exact-merge Lightweight master verification passed in run #496 / `35537989459`.

## Exit contract

The technical backlog requires all of the following:

1. required P5R tasks are accepted;
2. full automated verification is green on the final candidate/merge path;
3. the persistent sandbox and standalone renderer visual demo preserve their accepted behavior;
4. public/internal/module/ownership/spatial/persisted/protocol contracts are reconciled;
5. no unresolved stale names/docs remain;
6. this review explicitly records PASS before P6-T01 may be materialized.

## P5R task completion

P5R-T01 through P5R-T25 are accepted in the current technical backlog.

The sequence established and then reconciled:

- a repository naming/refactor standard and symbol inventory;
- public/internal/package boundary inventories;
- bounded GLFW and renderer responsibility decomposition;
- input-binding loader/parser/validator separation;
- lifecycle public-name correction under D-066;
- core/spatial KEEP audits;
- sandbox and renderer visual-demo decomposition;
- client/server and feasibility-spike naming/isolation cleanup;
- deliberate public-API naming KEEP review;
- test/fixture/evidence naming synchronization;
- internal-package cohesion KEEP review;
- renderer adapter-composition hardening;
- final stale-name/dead-code/compatibility cleanup audit.

No accepted P5R task relies on a later task to restore compilation, runtime behavior, ownership, public contracts, or documentation consistency.

## Owner-facing regression review

### Persistent sandbox

Canonical entry point remains:

```powershell
.\gradlew.bat :game-sandbox:runSandbox
```

Current Gradle wiring still targets `com.samo.game.sandbox.SandboxMain`. The current sandbox README retains the same command and owner-controlled exit model.

Accepted behavior contracts rechecked against source/docs:

- `SandboxMain` remains bootstrap/lifecycle owner;
- extracted T16 collaborators retain frame-loop/control/scene/diagnostic/framebuffer responsibilities;
- T17 removed only the obsolete `EngineDemoMain` / `runEngineDemo` compatibility surface;
- camera/input flow remains through public production APIs;
- D-041/D-045 spatial conventions and accepted 70° / 0.1 m / 100 m sandbox projection semantics remain documented;
- no P5R task changed sandbox resources, controls, diagnostic meaning, render/present ordering, or shutdown ownership.

The heavy candidate workflow executes the ordinary sandbox unit/classpath/build coverage through the repository test/build matrix and retains the P5-T18 Windows native integration regression for the renderer/camera assumptions. No interactive sandbox run is claimed by this review environment.

### Standalone renderer visual demo

Canonical entry point remains:

```powershell
.\gradlew.bat :engine-render-opengl:runRendererVisualDemo
```

Current Gradle wiring still targets `com.samo.engine.render.opengl.internal.RendererVisualDemo` in the dedicated `visualDemo` source set.

Accepted T18 behavior contracts remain:

- entry point is launch-only;
- application lifecycle, frame loop, framebuffer size, animated lighting, and material overlay remain separated into package-private collaborators;
- material/shader values, two-light trajectories, debug primitives, renderer -> overlay -> present ordering, and GL cleanup order remain unchanged;
- production renderer API/source-set boundary remains unchanged.

The repository `check` task compiles the visual-demo source set, and the ordinary renderer tests include the deterministic T18 collaborator regressions. The Windows native job retains the Phase 5 integrated renderer acceptance. No interactive visual-demo run is claimed by this review environment.

## Contract reconciliation

### Public API

P5R-T21 reviewed all supported P1-P5 public top-level/nested types and member vocabulary and selected KEEP for the current supported consumer roots:

- `com.samo.engine.core.api`;
- `com.samo.engine.platform.api`;
- `com.samo.engine.render.api`.

D-066 remains the only Phase 5R supported public-name correction. No deprecated compatibility alias is retained.

### Internal/package boundaries

P5R-T23 confirmed the current renderer/platform/client/server internal package arrangement is the stronger encapsulation boundary. No implementation type was promoted merely to cross a package boundary.

The renderer API artifact still excludes `com.samo.engine.render.opengl.internal`, while the runtime artifact retains the internal renderer implementation/resources.

### Module direction

The declared Gradle project list and `config/architecture/module-boundaries.properties` remain aligned. Architecture tests remain responsible for rejecting cross-module implementation imports, and the feasibility-spike isolation check remains part of the experimental module's `check`.

No P5R task added a Phase 6 dependency or reversed an accepted engine-module dependency direction.

### Native ownership/lifetime

P5R-T03 through T05 retained `GlfwWindow` lifecycle/native ownership while extracting non-owning collaborators.

P5R-T10 through T15 retained `ReferenceSceneRenderer` and explicit OpenGL wrappers as the native-resource owners while extracting non-owning frame collaborators and clarifying cleanup helpers.

T24's `OpenGlBackendSet` composes adapters only; it does not own native resources or introduce a locator/global registry.

### Spatial contracts

`docs/SPATIAL_CONVENTIONS.md` remains the canonical contract:

- right-handed world;
- +X right, +Y up, -Z forward;
- meters and radians internally;
- D-045 camera/projection convention;
- D-046 screen-ray mapping;
- D-047 transform value quantization.

P5R-T09 selected KEEP for spatial/math naming and structure. Later renderer/sandbox refactors explicitly retained these semantics. No Phase 5R task changed coordinate basis, units, projection convention, tolerances, allocation semantics, or spatial binary meaning.

### Persisted/config/wire/protocol contracts

The input-binding refactor preserved schema-v1 JSON meaning and failure behavior.

Lifecycle, renderer, sandbox, client/server, test/evidence, package, and pattern tasks did not change persisted/config schemas or packet layouts.

`PlayerInputCommandCodec`, transform quantization semantics, client/server compatibility reporting, and protocol/version boundaries remain outside any P5R semantic change.

### Stale names/docs/compatibility

P5R-T25's final cleanup audit found:

- no live stale renamed implementation symbols;
- no authored-Java `@Deprecated` compatibility shim;
- no orphaned P5R helper;
- no avoidable compatibility alias;
- only intentional historical provenance uses of superseded names.

Wiki and sandbox guidance remain consistent with current supported public usage.

## Phase 6 readiness review

Phase 6 remains the asset-pipeline/resource-lifetime phase. Its existing outcome is still needed: runtime gameplay must stop depending on source-format parsing and raw file paths.

The current first planned task remains:

`P6-T01 Define AssetId as a stable 128-bit identifier independent of file path.`

That task is consistent with the current scope and decisions:

- it does not require changing accepted P1-P5 public behavior;
- it supplies identity needed by later cooked metadata/resource-handle tasks;
- it does not require pulling Assimp cooking, GPU upload, hot reload, world/entity systems, or other later P6 tasks forward;
- the later P6-T05 coordinate/unit conversion continues to consume the canonical spatial convention at the cooker boundary.

No P6 issue is materialized or implemented by T26. P6-T01 remains blocked until this review records final PASS.

## Automated verification required before PASS

The exact final PR head/base candidate must pass:

- Build and quality gates;
- Unit tests;
- Architecture tests;
- JaCoCo coverage reports;
- Windows native smoke.

After merge, the exact merged `master` SHA must pass Lightweight master verification.

The non-Markdown evidence manifest `P5R_EXIT_REVIEW_EVIDENCE.txt` is intentionally included so the phase-gate PR is not suppressed by the repository Markdown-only `paths-ignore`. It changes no executable behavior.

## Current disposition

Static/contract review: **PASS**.

Owner-facing entry-point/source/docs reconciliation: **PASS**, with no interactive-window execution claimed.

Automated final-candidate verification: **PASS** — run #495 / `35537699489`, all five required jobs succeeded on exact head `40f2bb91ae3afeeee07d7f8be92dd7b088419fa0`.

Exact-merge verification: **PASS** — run #496 / `35537989459`, Lightweight master verification succeeded on exact merge `5e0cac4e7748a66b7c2d19e0cf444eaca0a49fce`.

Overall Phase 5R exit: **PASS**.

Wiki impact: none — no supported API/consumer usage change.

Sandbox impact: review only — no sandbox source or owner-facing contract change.

Architecture/decision impact: none — reconciliation found no new durable decision requirement.


## Acceptance record

P5R-T26 / Issue #306 Phase 5R exit review PASSED. Exact final candidate `40f2bb91ae3afeeee07d7f8be92dd7b088419fa0` passed all five required jobs in PR run #495 / `35537699489`. PR #357 merged as `5e0cac4e7748a66b7c2d19e0cf444eaca0a49fce`, and exact-merge Lightweight master verification passed in run #496 / `35537989459`. Static/contract reconciliation found no unresolved public/internal/module/native-ownership/spatial/persisted/config/wire/protocol contradiction, no stale-name/refactor residue, and no Phase 6 readiness blocker. Interactive sandbox/visual-demo windows were not run in this connected review environment and are not claimed as manual visual evidence; their canonical entry wiring, deterministic tests/compilation, and retained native renderer regression coverage remain verified.

Phase 6 may now be activated only by freshly materializing/refining P6-T01 against the then-current `master`. No Phase 6 implementation was performed by T26.
