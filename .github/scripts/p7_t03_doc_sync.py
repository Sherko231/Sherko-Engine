from pathlib import Path
import base64
import gzip


def replace_once(path: str, old: str, new: str) -> None:
    file_path = Path(path)
    text = file_path.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{path}: expected exactly one match, found {count}: {old[:140]!r}")
    file_path.write_text(text.replace(old, new, 1), encoding="utf-8")


replace_once(
    "docs/DEVELOPMENT_STATUS.md",
    "| Active phase | Phase 7 active — P7-T01 / Issue #414 is accepted; P7-T02 / Issue #416 is the current bounded task; Phase 6, including the accepted sandbox Asset Lab and visual diagnostics polish, remains complete |",
    "| Active phase | Phase 7 active — P7-T01 / Issue #414 and P7-T02 / Issue #416 are accepted; P7-T03 / Issue #418 is the current bounded task; Phase 6, including the accepted sandbox Asset Lab and visual diagnostics polish, remains complete |",
)
replace_once(
    "docs/DEVELOPMENT_STATUS.md",
    "| Active executable task | P7-T02 / Issue #416 — package-private packed component store keyed sparsely by entity index; implementation is on `p7-t02-packed-component-store`; final PR/CI is not yet accepted |",
    "| Active executable task | P7-T03 / Issue #418 — package-private deferred entity/component structural commands with explicit post-iteration flush; implementation is on `p7-t03-deferred-structural-commands`; final PR/CI is not yet accepted |",
)
replace_once(
    "docs/DEVELOPMENT_STATUS.md",
    "Complete **P7-T02 / Issue #416** only. Finish the bounded package-private packed component store and its stale-ID/add/get/remove/iteration tests, reconcile architecture/status/orientation documentation, complete the final diff/consistency audit, then require exact-head five-job PR CI before merge. Do not materialize or implement P7-T03 until P7-T02 is accepted.",
    "Complete **P7-T03 / Issue #418** only. Finish the bounded package-private deferred structural-command buffer and its iteration/deferred-visibility/FIFO/failure tests, reconcile architecture/status/orientation documentation, remove temporary preflight tooling, complete the final diff/consistency audit, then require exact-head five-job PR CI before merge. Do not materialize or implement P7-T04 until P7-T03 is accepted.",
)
replace_once(
    "docs/DEVELOPMENT_STATUS.md",
    "The Phase 7 readiness review found no prerequisite conflict. P7-T01 / Issue #414 is accepted: final candidate `182ff9ca4ed8ef915fac94e05427d6328f41c78f` passed the required five-job matrix in run #641 / `36872723180`, PR #415 merged as `e25458acaaa68bcd5f9dc5ab201cfd55f4bfb359`, and exact merged master passed Lightweight verification in run #642 / `36878894604`. P7-T02 / Issue #416 is now the only materialized Phase 7 implementation task; later P7 tasks remain unmaterialized.",
    "The Phase 7 readiness review found no prerequisite conflict. P7-T01 / Issue #414 is accepted: final candidate `182ff9ca4ed8ef915fac94e05427d6328f41c78f` passed the required five-job matrix in run #641 / `36872723180`, PR #415 merged as `e25458acaaa68bcd5f9dc5ab201cfd55f4bfb359`, and exact merged master passed Lightweight verification in run #642 / `36878894604`. P7-T02 / Issue #416 is also accepted: final candidate `7968e6ececf4a2b5cdd614c4e6848cf703ddb6f1` passed the required five-job matrix in run #645 / `36986058671`, PR #417 merged as `1b10c7683a231afec49e3fecd085314834ab2a96`, and exact merged master passed Lightweight verification in run #646 / `36986734070`. P7-T03 / Issue #418 is now the only materialized Phase 7 implementation task; later P7 tasks remain unmaterialized.",
)
replace_once(
    "docs/DEVELOPMENT_STATUS.md",
    "Phase 7 has accepted P7-T01 and is active on P7-T02. The sandbox remains intentionally unchanged because P7-T02 adds only package-private component storage and there is still no public world/entity/component lifecycle that can be exercised honestly without pulling later world/ECS work forward.",
    "Phase 7 has accepted P7-T01 and P7-T02 and is active on P7-T03. The sandbox remains intentionally unchanged because the accepted/current Phase 7 work is internal identity/storage/structural-command infrastructure and there is still no public world/entity/component lifecycle that can be exercised honestly without pulling P7-T04+ work forward.",
)

status_path = Path("docs/DEVELOPMENT_STATUS.md")
status_text = status_path.read_text(encoding="utf-8")
marker = "## Phase 7 packed component storage — P7-T02 / Issue #416\n"
index = status_text.find(marker)
if index < 0:
    raise SystemExit("docs/DEVELOPMENT_STATUS.md: P7-T02 section marker not found")
new_tail = """## Phase 7 packed component storage — P7-T02 / Issue #416

P7-T02 is accepted from the P7-T01 merge baseline `e25458acaaa68bcd5f9dc5ab201cfd55f4bfb359`. Final candidate `7968e6ececf4a2b5cdd614c4e6848cf703ddb6f1` passed Build and quality gates, Unit tests, Architecture tests, JaCoCo coverage reports, and Windows native smoke in run #645 / `36986058671`. PR #417 merged as `1b10c7683a231afec49e3fecd085314834ab2a96`, and exact merged master passed Lightweight verification in run #646 / `36986734070`. Issue #416 is closed completed.

The accepted implementation adds package-private `PackedComponentStore<T>` plus focused internal tests only in `engine-world`. The store uses sparse entity-index lookup into dense exact-`EntityId`/component storage, delegates liveness and generation validity to the accepted P7-T01 allocator, swap-compacts removals, excludes stale entries from size/iteration, and rejects structural add/remove through the same store during iteration so P7-T03 retains deferred-command ownership. No public `World` or component-store API, deferred command queue, fixed update phases, concrete components, prefab/scene behavior, dependency/module edge, persisted/protocol/network identity, renderer/physics/audio/assets/game behavior, Gradle change, or lockfile change was introduced.

Wiki impact: none — P7-T02 adds no supported public engine API or consumer-visible behavior.
Sandbox impact: none — the store is package-private and there is still no public world/entity/component lifecycle to exercise without pulling later Phase 7 work forward.
Independent review: not performed in the connected authoring session; no independent approval is inferred.

## Phase 7 deferred structural commands — P7-T03 / Issue #418

P7-T03 is active from accepted P7-T02 merge baseline `1b10c7683a231afec49e3fecd085314834ab2a96` on branch `p7-t03-deferred-structural-commands`. The bounded candidate adds package-private `DeferredStructuralCommandBuffer` plus focused internal tests only in `engine-world`. Entity creation/destruction and component add/removal requests are recorded without immediate structural mutation and are applied only by an explicit FIFO `flush()` after iteration. A pending internal creation result resolves only when its creation command executes. P7-T03 reuses the accepted P7-T01 allocator and P7-T02 store semantics rather than duplicating liveness, generation, sparse/dense, or removal logic.

The buffer rejects recursive flush and recording during an active flush. Commands are removed from the queue before execution: commands already reached stay applied, a failing command is consumed and propagates its exception, and untouched tail commands remain queued in original order for a later explicit flush. No transactional rollback or automatic flush/update phase is claimed; P7-T04 retains ownership of fixed world update phases and eventual flush scheduling.

Authoring preflight run #3 / `36990597864` on head `254d927326ea9a288324ee2ef93f89ba896907fe` passed Spotless verification, `:engine-world:test --rerun-tasks`, and the architecture boundary test. This temporary branch-only preflight is authoring evidence only and will be removed before the final candidate; final acceptance still requires the exact-head five-job PR matrix and exact-merge Lightweight verification. The connected environment does not execute the Gradle wrapper locally, so no separate local command run is claimed.

Wiki impact: none — P7-T03 adds no supported public engine API or consumer-visible behavior.
Sandbox impact: none — deferred commands remain package-private and there is still no public world/entity/component lifecycle to exercise without pulling P7-T04+ work forward.
Independent review: not performed in this connected session; no independent reviewer/provenance is available.

Exact next action: finish P7-T03 / Issue #418 only, remove temporary preflight tooling, complete the final diff/self-review/consistency audit, open one final non-draft PR, require the five-job exact-head matrix, merge only while tested head/base remain current, then require exact-merge Lightweight verification before closing #418. Do not materialize P7-T04 before P7-T03 is accepted.
"""
status_path.write_text(status_text[:index] + new_tail, encoding="utf-8")

replace_once(
    "docs/ARCHITECTURE.md",
    "| `engine-world` | Scene/world/component/prefab runtime | P7-T01 public generational `EntityId` plus package-private allocation/reuse/liveness bookkeeping; P7-T02 adds package-private packed one-type component storage keyed sparsely by entity index with exact-ID stale safety; public world lifecycle, deferred structural commands, update phases, and prefab/scene behavior remain planned | `engine-core`, `engine-assets` |",
    "| `engine-world` | Scene/world/component/prefab runtime | P7-T01 public generational `EntityId` plus package-private allocation/reuse/liveness bookkeeping; P7-T02 adds package-private packed one-type component storage keyed sparsely by entity index with exact-ID stale safety; P7-T03 adds package-private FIFO deferred entity/component structural commands with explicit flush; public world lifecycle, fixed update phases, concrete components, and prefab/scene behavior remain planned | `engine-core`, `engine-assets` |",
)
architecture_path = Path("docs/ARCHITECTURE.md")
architecture_text = architecture_path.read_text(encoding="utf-8")
append_text = """

## Phase 7 deferred structural commands — P7-T03 / Issue #418

`engine-world` adds package-private `DeferredStructuralCommandBuffer` above the accepted P7-T01 identity allocator and P7-T02 packed component store. Internal callers record entity creation/destruction and typed component add/removal requests while packed-store iteration is active; recording itself does not mutate allocator or store structure. An owner later invokes `flush()` explicitly, and queued commands execute in FIFO record order.

Deferred entity creation uses a package-private pending result whose `EntityId` is unavailable until the queued creation command executes. Destruction delegates to `EntityIdAllocator.destroy(...)`, preserving exact generation/liveness safety. Component commands delegate to `PackedComponentStore.add/remove(...)`, preserving exact-ID lookup, stale-generation isolation, swap-compaction, and the existing prohibition on direct structural mutation during a store's own iteration. P7-T03 adds no alternate generation or sparse/dense bookkeeping.

`flush()` is deliberately non-transactional. Each command is removed from the queue before it executes: earlier successful commands remain applied, a failing command propagates and is consumed, and commands not yet reached remain queued in original order for a later explicit flush. Recursive flush and recording during an active flush are rejected deterministically. Access remains externally serialized; no concurrency guarantee is added.

P7-T03 does not define automatic flush scheduling or a world update phase. P7-T04 remains the owner of fixed update phases and eventual structural-command visibility timing. No public `World`, public command buffer/component store, concrete component set, query/event scheduler, prefab/scene behavior, persistence/protocol/network identity, renderer/physics/audio/assets/game behavior, project edge, or production dependency is introduced.

Durable decision impact: none — the active Issue bounds this internal FIFO/explicit-flush mechanism without establishing a broader public or cross-phase contract.
Wiki impact: none — no supported public engine API or consumer-visible usage changes.
Sandbox impact: none — there is still no public world/entity/component lifecycle to exercise without pulling P7-T04+ work forward.
"""
if "## Phase 7 deferred structural commands — P7-T03 / Issue #418" in architecture_text:
    raise SystemExit("docs/ARCHITECTURE.md: P7-T03 section already exists")
architecture_path.write_text(architecture_text.rstrip() + append_text + "\n", encoding="utf-8")

for path in ("docs/DEVELOPMENT_STATUS.md", "docs/ARCHITECTURE.md"):
    data = Path(path).read_bytes()
    compressed = gzip.compress(data, compresslevel=9)
    encoded = base64.b64encode(compressed).decode("ascii")
    print(f"BEGIN_GZIP_BASE64 {path} {len(data)}")
    print(encoded)
    print(f"END_GZIP_BASE64 {path}")
