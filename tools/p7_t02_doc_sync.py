from pathlib import Path


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected exactly one match, found {count}")
    return text.replace(old, new, 1)


architecture = Path("docs/ARCHITECTURE.md")
text = architecture.read_text(encoding="utf-8")
text = replace_once(
    text,
    "| `engine-world` | Scene/world/component/prefab runtime | P7-T01 public generational `EntityId` plus package-private allocation/reuse/liveness bookkeeping; component storage, public world lifecycle, update phases, and prefab/scene behavior remain planned | `engine-core`, `engine-assets` |",
    "| `engine-world` | Scene/world/component/prefab runtime | P7-T01 public generational `EntityId` plus package-private allocation/reuse/liveness bookkeeping; P7-T02 adds package-private packed one-type component storage keyed sparsely by entity index with exact-ID stale safety; public world lifecycle, deferred structural commands, update phases, and prefab/scene behavior remain planned | `engine-core`, `engine-assets` |",
    "architecture module table",
)
heading = "## Phase 7 packed component storage — P7-T02 / Issue #416"
if heading in text:
    raise SystemExit("architecture P7-T02 section already exists")
text = text.rstrip() + """


## Phase 7 packed component storage — P7-T02 / Issue #416

`engine-world` now adds package-private `PackedComponentStore<T>` as a one-component-type-per-instance storage foundation. A sparse integer lookup maps entity indices to dense positions while dense storage retains the exact `EntityId` beside each component value. `EntityIdAllocator` remains the sole liveness/generation authority, so the store does not duplicate P7-T01 generation bookkeeping.

Add/get/remove accept only the exact currently live identity. Stale, destroyed, generation-mismatched, and unknown IDs cannot observe or remove another generation's value. When an allocator index is reused, a stale dense entry for the previous generation may be discarded before the replacement generation is inserted, without reviving the old identity. Duplicate add for the same live entity fails without replacing the existing component.

Removal uses swap-compaction and repairs the moved entity's sparse position so packed iteration does not scan the full allocated entity-index space. `size()` and iteration exclude stale entries. Iteration order is intentionally not a contract. Structural `add`/`remove` through the same store while its iteration callback is active is rejected deterministically; P7-T03 remains responsible for deferred structural commands rather than P7-T02 inventing mutation-during-iteration semantics.

The store remains internal and externally serialized. P7-T02 introduces no public `World` or component-store API, concrete gameplay component types, query/event/deferred-command system, persistence/serialization/network identity, renderer/physics/audio/game behavior, dependency, or module edge.

Durable decision impact: none — this is the bounded internal implementation of the already planned P7-T02 packed-store contract and consumes P7-T01 identity validity without adding a new cross-task architecture policy.
Wiki impact: none — no supported public engine API or consumer-visible behavior changes.
Sandbox impact: none — there is still no public world/entity/component lifecycle that can be exercised honestly without pulling P7-T03+ work forward.
"""
architecture.write_text(text, encoding="utf-8")

status = Path("docs/DEVELOPMENT_STATUS.md")
text = status.read_text(encoding="utf-8")
text = replace_once(
    text,
    "| Active phase | Phase 7 active — P7-T01 / Issue #414 is the current bounded task; Phase 6, including the accepted sandbox Asset Lab and visual diagnostics polish, remains complete |",
    "| Active phase | Phase 7 active — P7-T01 / Issue #414 is accepted; P7-T02 / Issue #416 is the current bounded task; Phase 6, including the accepted sandbox Asset Lab and visual diagnostics polish, remains complete |",
    "status active phase",
)
text = replace_once(
    text,
    "| Active executable task | P7-T01 / Issue #414 — generational `EntityId` identity and stale-handle safety; implementation is on `p7-t01-generational-entity-id` with draft PR #415 pending final-candidate verification |",
    "| Active executable task | P7-T02 / Issue #416 — package-private packed component store keyed sparsely by entity index; implementation is on `p7-t02-packed-component-store`; final PR/CI is not yet accepted |",
    "status active executable task",
)
text = replace_once(
    text,
    "The Phase 7 readiness review found no prerequisite conflict. Phase 7 is now active through P7-T01 / Issue #414 on branch `p7-t01-generational-entity-id` with draft PR #415; later P7 tasks remain unmaterialized.",
    "The Phase 7 readiness review found no prerequisite conflict. P7-T01 / Issue #414 is accepted: final candidate `182ff9ca4ed8ef915fac94e05427d6328f41c78f` passed the required five-job matrix in run #641 / `36872723180`, PR #415 merged as `e25458acaaa68bcd5f9dc5ab201cfd55f4bfb359`, and exact merged master passed Lightweight verification in run #642 / `36878894604`. P7-T02 / Issue #416 is now the only materialized Phase 7 implementation task; later P7 tasks remain unmaterialized.",
    "status phase 7 readiness paragraph",
)
text = replace_once(
    text,
    "Phase 7 is active through P7-T01 only. The sandbox remains intentionally unchanged because P7-T01 exposes no public entity lifecycle/allocator that can be exercised honestly without pulling later world/ECS work forward.",
    "Phase 7 has accepted P7-T01 and is active on P7-T02. The sandbox remains intentionally unchanged because P7-T02 adds only package-private component storage and there is still no public world/entity/component lifecycle that can be exercised honestly without pulling later world/ECS work forward.",
    "status sandbox paragraph",
)
text = replace_once(
    text,
    "Complete **P7-T01 / Issue #414** only. Finish the bounded generational `EntityId(index,generation)` candidate in PR #415: keep allocator/reuse bookkeeping package-private, preserve stale-handle safety and generation retirement, reconcile documentation/wiki, complete the final diff/consistency audit, then require exact-head five-job PR CI before merge. Do not materialize or implement P7-T02 until P7-T01 is accepted.",
    "Complete **P7-T02 / Issue #416** only. Finish the bounded package-private packed component store and its stale-ID/add/get/remove/iteration tests, reconcile architecture/status/orientation documentation, complete the final diff/consistency audit, then require exact-head five-job PR CI before merge. Do not materialize or implement P7-T03 until P7-T02 is accepted.",
    "status exact next action",
)
marker = "## Phase 7 activation — P7-T01 / Issue #414"
if text.count(marker) != 1:
    raise SystemExit(f"status Phase 7 marker count={text.count(marker)}")
prefix, _ = text.split(marker, 1)
text = prefix.rstrip() + """


## Phase 7 accepted identity foundation — P7-T01 / Issue #414

P7-T01 is accepted from `master` baseline `eb76993758edd567778b59daa3e63a537cf52719`. Final candidate `182ff9ca4ed8ef915fac94e05427d6328f41c78f` passed Build and quality gates, Unit tests, Architecture tests, JaCoCo coverage reports, and Windows native smoke in run #641 / `36872723180`. PR #415 merged as `e25458acaaa68bcd5f9dc5ab201cfd55f4bfb359`, and exact merged master passed Lightweight verification in run #642 / `36878894604`.

The accepted implementation adds public immutable `com.samo.engine.world.api.EntityId(index,generation)` plus only package-private allocation/liveness/reuse bookkeeping. Fresh slots start at generation 0, successful destruction invalidates the exact identity, reuse increments generation, stale/mismatched/unknown destruction cannot affect a different live entity, and destroying generation `Integer.MAX_VALUE` retires the index rather than wrapping. D-081 and the public wiki record the identity semantics. No component storage, public `World`, deferred structural commands, update phases, prefab/scene format, serialization/protocol/network identity, renderer/assets/physics/audio/game behavior, production dependency, or module edge was introduced.


## Phase 7 packed component storage — P7-T02 / Issue #416

P7-T02 is active from accepted P7-T01 merge baseline `e25458acaaa68bcd5f9dc5ab201cfd55f4bfb359` on branch `p7-t02-packed-component-store`. The bounded candidate adds package-private `PackedComponentStore<T>` plus focused internal tests only in `engine-world`. The store uses sparse entity-index lookup into dense exact-`EntityId`/component storage, delegates liveness and generation validity to the accepted P7-T01 allocator, swap-compacts removals, excludes stale entries from size/iteration, and rejects structural add/remove through the same store during iteration so P7-T03 retains deferred-command ownership.

Current authoring verification state: the connected environment cannot execute the Gradle wrapper locally because direct GitHub network access is unavailable, so no focused/local Gradle command is claimed as passing here. No Gradle/dependency/lockfile change is intended. Before final acceptance, the candidate still requires complete diff/self-review/consistency audit and the exact-head five-job PR matrix; after merge, exact merged master must pass Lightweight verification before Issue #416 closes.

Wiki impact: none — P7-T02 adds no supported public engine API or consumer-visible behavior.
Sandbox impact: none — the store is package-private and there is still no public world/entity/component lifecycle to exercise without pulling P7-T03+ work forward.
Independent review: not performed in this connected session; no approval is inferred.

Exact next action: finish P7-T02 / Issue #416 only, open the final non-draft PR only after the diff/docs are complete, require the five-job exact-head matrix, merge only while tested head/base remain current, then require exact-merge Lightweight verification. Do not materialize P7-T03 before P7-T02 is accepted.
"""
status.write_text(text, encoding="utf-8")

roadmap = Path("ROADMAP.md")
text = roadmap.read_text(encoding="utf-8")
text = replace_once(
    text,
    "## Current focus — M2 / Phase 6 asset pipeline and resource lifetime\n\nPhase 5 and the mandatory Phase 5R hardening gate are complete. Phase 6 is now active through **P6-T01 / Issue #362**, which introduces only the stable path-independent 128-bit asset identity contract before metadata, cooking, manifests, runtime resource lifetime, or import work.",
    "## Current focus — M2 / Phase 7 world, entities, components, prefabs, and scenes\n\nPhase 5, mandatory Phase 5R hardening, and Phase 6 asset-pipeline/resource-lifetime work are complete. Phase 7 is active. P7-T01 / Issue #414 established and accepted generational `EntityId(index,generation)` identity with stale-handle safety. P7-T02 / Issue #416 is the current bounded task and adds only one package-private packed component-store foundation keyed sparsely by entity index; public world lifecycle, deferred structural commands, concrete components, prefabs/scenes, and later Phase 7 work remain separately materialized tasks.",
    "roadmap current focus",
)
roadmap.write_text(text, encoding="utf-8")
