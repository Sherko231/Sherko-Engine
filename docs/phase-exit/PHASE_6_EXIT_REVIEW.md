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

**PASS. Phase 6 is complete and Phase 7 is ready to activate.**

Accepted evidence:

- final candidate `17b57f92963de4e336308c4a1a4315423a1003e9` passed all five heavy jobs in run #606 / `35923546789`;
- the retained `p6-exit-cooked-runtime` artifact reports:
  - `result=PASS`;
  - `source.tree.removed.before.runtime.open=true`;
  - `manifest.present=true`;
  - `runtime.input=cooked-cache-root-only`;
  - MESH handle READY;
  - MATERIAL handle READY;
  - `runtime.error.count=0`;
- the JUnit report contains one test, zero failures, zero errors, and zero skips;
- PR #406 merged as `a8b3211729e75bd0192df1c6d04dc98d79eadd54`;
- exact merged master passed Lightweight verification in run #607 / `35924287931`.

### Merge-tree verification

The heavy PR run checked out GitHub's synthetic merge commit `97ae0cad824ae3da8607d35f9fae9eebc2726dea`, not merely the branch tip. The synthetic merge and final merge have the same two parents:

- base `3a0df9e1c85c40cee2a02aa6810bf8865476014a`;
- candidate `17b57f92963de4e336308c4a1a4315423a1003e9`.

Both commits point to exact tree `c5c5ddc055615896a44127f177ad57598c6927ba`. The five-job run therefore exercised the exact repository contents that reached master; exact-SHA run #607 then verified the final merge commit itself.

### Stronger post-merge dispatch note

Issue #405 originally requested an additional explicit `workflow_dispatch` heavy run after merge. The connected GitHub tool surface does not expose workflow dispatch. A bounded attempt to rerun a heavy job that was skipped on push succeeded at the Actions API request level, but GitHub re-evaluated the job condition for the push event and kept heavy work skipped. No workflow-dispatch result is claimed.

This limitation does not hide a tree gap: the complete heavy matrix already executed on the byte-identical synthetic merge tree with identical parents, and the exact final merge SHA passed the repository's Lightweight verifier. That combined evidence is the accepted Phase 6 phase-gate record.

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

Independent review remains unavailable, but the integrated gate and CI execution are complete. Run #606 supplies full five-job execution evidence on the exact final merge tree; run #607 supplies exact final merge-SHA verification. These automated checks do not masquerade as independent design review.

## Wiki / sandbox

Wiki impact: none — this exit task verifies already-documented public asset behavior and adds no consumer API or semantic change.

Sandbox impact: none — the gate is a nonvisual cooker/runtime integration property. Demonstrating it through the headless integration path avoids internal imports, new public API, or premature Phase 7 world/render submission.
