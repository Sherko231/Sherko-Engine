# Phase 6 Exit Gate and Phase 7 Readiness Review

Issue: #405 — prove the Phase 6 cooked-runtime exit gate and review Phase 7 before any Phase 7 executable Issue is materialized.

Baseline: `3a0df9e1c85c40cee2a02aa6810bf8865476014a`.

## Existing Phase 6 gate

The accepted backlog gate is unchanged:

> the runtime starts using only a cooked asset directory and manifest.

Task completion alone does not satisfy this gate. The exit candidate adds one production-boundary integration scenario that cooks authoring assets, removes the complete source tree, opens the runtime from the cooked cache root only, then loads typed MESH and MATERIAL handles through `AssetLoaders.open`.

## Integrated scenario

`Phase6CookedRuntimeIntegrationTest`:

1. creates one valid glTF MESH source and one strict MATERIAL schema-v1 source with canonical metadata;
2. runs the production `AssetCooker` into a fresh output;
3. confirms `manifest.json` and both AssetId-addressed cooked payloads exist;
4. deletes the complete authoring source tree;
5. opens only the cooked cache through public `AssetLoaders.open(cookedRoot)`;
6. loads the exact MESH and MATERIAL AssetIds through public `AssetLoader`;
7. waits for READY and checks independently expected mesh positions/indices plus material shader/scalars;
8. verifies the runtime diagnostic drain is empty;
9. closes the handles and loader.

The retained report is `engine-assets/build/reports/p6/p6-exit-cooked-runtime.txt`. CI also retains the corresponding JUnit XML in artifact `p6-exit-cooked-runtime`.

The scenario intentionally does not copy authoring content beside runtime input and cannot fall back to the deleted source tree. It therefore catches accidental source-path/runtime coupling in the cooker-to-runtime handoff.

## Gate status

**PENDING EXECUTION on the exact final candidate.**

Phase 6 must not be called complete until:

- the exact final PR candidate passes all five required jobs;
- the retained Phase 6 integration artifact reports `result=PASS`;
- the PR merges without a stale base/candidate;
- ordinary exact-merge Lightweight verification passes;
- the explicit exact-merge `workflow_dispatch` heavy phase-gate run succeeds on merged `master`.

No isolated historical P6 task run substitutes for this integrated phase evidence.

## Phase 7 readiness review

### Assumptions and dependencies

The current accepted Phase 6 state satisfies the prerequisites needed to begin Phase 7 after the gate passes:

- `AssetId` is a stable public 128-bit identity independent of source paths.
- Runtime MESH and MATERIAL values are typed and backend-neutral; public consumers do not receive OpenGL/native handles.
- Runtime loading begins from a cooked-cache root containing the production manifest and AssetId payloads.
- `engine-world` already depends on `engine-core` and `engine-assets` only. No reverse engine dependency or game-specific dependency is required for the planned ECS/world work.
- The canonical spatial contract is already established in `engine-core` for later TransformComponent/camera/spatial component tasks.
- Phase 7 does not require texture/audio runtime loading to begin P7-T01 through P7-T04; later scene/render/audio component integration must remain bounded by the public capabilities available when those tasks are materialized.

### Backlog review

P7-T01 through P7-T13 remain consistent with current evidence and do not require a scope, dependency, technology, or exit-threshold change before Phase 7 activation.

No executable P7 Issue is created in this exit task.

Potential task-local refinements should be made only when each task is freshly materialized:

- P7-T01 should define exact generation overflow/reuse semantics and stale-ID validation behavior without introducing component storage.
- P7-T02 should explicitly consume P7-T01 validity rules and keep the first packed store bounded to one component type contract rather than prematurely creating a generic ECS framework.
- P7-T05/P7-T07 should use stable `AssetId` values for authored asset references and must not expose renderer/native handles.
- P7-T11 atomic scene activation should remain a later world-lifecycle contract and must not be pulled into earlier entity/store tasks.

These are refinement prompts, not blockers and not changes to the current backlog acceptance wording.

## Next bounded executable candidate

After the Phase 6 gate records PASS, the next executable task is:

**P7-T01 — Implement a generational `EntityId(index,generation)`.**

Acceptance remains: destroying and reusing an index never makes an old ID valid.

P7-T01 should be freshly materialized from the then-current `master`; no Phase 7 implementation is authorized by Issue #405 itself.

## Review provenance

Independent review: not performed in the connected session. No independent reviewer/agent with separate authorship provenance is available through the current connector surface.

Remaining risk before acceptance is therefore concentrated in exact execution of the integrated gate and CI review of the actual candidate. The required five-job PR run plus the stronger exact-merge workflow-dispatch run provide execution evidence but do not masquerade as independent design review.

## Wiki / sandbox

Wiki impact: none — this exit task verifies already-documented public asset behavior and adds no consumer API or semantic change.

Sandbox impact: none — the gate is a nonvisual cooker/runtime integration property. Demonstrating it through the headless integration path avoids internal imports, new public API, or premature Phase 7 world/render submission.
