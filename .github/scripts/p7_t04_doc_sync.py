from pathlib import Path


def replace_once(path: str, old: str, new: str) -> None:
    file = Path(path)
    text = file.read_text(encoding="utf-8")
    if new in text:
        return
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{path}: expected exactly one match, found {count}")
    file.write_text(text.replace(old, new, 1), encoding="utf-8", newline="\n")


def append_before(path: str, marker: str, insertion: str, guard: str) -> None:
    file = Path(path)
    text = file.read_text(encoding="utf-8")
    if guard in text:
        return
    count = text.count(marker)
    if count != 1:
        raise RuntimeError(f"{path}: expected exactly one marker, found {count}")
    file.write_text(text.replace(marker, insertion + marker, 1), encoding="utf-8", newline="\n")


p7_t03_evidence = (
    "P7-T03 / Issue #418 is accepted through PR #419: final candidate "
    "`c02c4715a77eced7feeb8055d0c7dd01ae9d31a4` passed the required five-job matrix in run #647 / "
    "`36993124976`, merged as `e97dceb477d9492d28aa2a924e5c5adc93628ba8`, and exact merged master passed "
    "Lightweight verification in run #648 / `36994718637`."
)

replace_once(
    "README.md",
    "P7-T03 / Issue #418 is the current bounded task and adds only package-private deferred entity/component structural commands with explicit post-iteration flush; P7-T04 update phases and later Phase 7 work remain separately materialized tasks.",
    p7_t03_evidence + " P7-T04 / Issue #420 is the current bounded task and defines only package-private fixed world update phases plus successful-phase structural-command flush boundaries; P7-T05 and later Phase 7 work remain separately materialized tasks.",
)

replace_once(
    "ROADMAP.md",
    "P7-T03 / Issue #418 is the current bounded task and adds only package-private deferred structural commands with explicit post-iteration flush; public world lifecycle, P7-T04 fixed update phases, concrete components, prefabs/scenes, and later Phase 7 work remain separately materialized tasks.",
    p7_t03_evidence + " P7-T04 / Issue #420 is the current bounded task and defines only the package-private fixed phase order plus structural-command flush boundaries; public world lifecycle, P7-T05 concrete components, prefabs/scenes, and later Phase 7 work remain separately materialized tasks.",
)

replace_once(
    "docs/DEVELOPMENT_STATUS.md",
    "| Active phase | Phase 7 active — P7-T01 / Issue #414 and P7-T02 / Issue #416 are accepted; P7-T03 / Issue #418 is the current bounded task; Phase 6, including the accepted sandbox Asset Lab and visual diagnostics polish, remains complete |",
    "| Active phase | Phase 7 active — P7-T01 / Issue #414, P7-T02 / Issue #416, and P7-T03 / Issue #418 are accepted; P7-T04 / Issue #420 is the current bounded task; Phase 6, including the accepted sandbox Asset Lab and visual diagnostics polish, remains complete |",
)
replace_once(
    "docs/DEVELOPMENT_STATUS.md",
    "| Active executable task | P7-T03 / Issue #418 — package-private deferred entity/component structural commands with explicit post-iteration flush; implementation is on `p7-t03-deferred-structural-commands`; final PR/CI is not yet accepted |",
    "| Active executable task | P7-T04 / Issue #420 — package-private fixed world update phases with a P7-T03 structural-command flush after each successfully completed phase; implementation is on `p7-t04-fixed-world-update-phases`; final PR/CI is not yet accepted |",
)

status = Path("docs/DEVELOPMENT_STATUS.md")
status_text = status.read_text(encoding="utf-8")
start = "## Phase 7 deferred structural commands — P7-T03 / Issue #418\n"
if "## Phase 7 fixed world update phases — P7-T04 / Issue #420" not in status_text:
    index = status_text.find(start)
    if index < 0:
        raise RuntimeError("docs/DEVELOPMENT_STATUS.md: P7-T03 section not found")
    replacement = """## Phase 7 deferred structural commands — P7-T03 / Issue #418

P7-T03 is accepted from the P7-T02 merge baseline `1b10c7683a231afec49e3fecd085314834ab2a96`. Final candidate `c02c4715a77eced7feeb8055d0c7dd01ae9d31a4` passed Build and quality gates, Unit tests, Architecture tests, JaCoCo coverage reports, and Windows native smoke in run #647 / `36993124976`. PR #419 merged as `e97dceb477d9492d28aa2a924e5c5adc93628ba8`, and exact merged master passed Lightweight verification in run #648 / `36994718637`. Issue #418 is closed completed.

The accepted implementation adds package-private `DeferredStructuralCommandBuffer` above the accepted P7-T01 allocator and P7-T02 packed store. Entity creation/destruction and component add/removal are recorded without immediate structural mutation and execute only through explicit FIFO `flush()`. Commands are consumed before execution, so reached work stays applied, a failing command is consumed and propagates, and untouched tail commands remain queued for a later explicit flush. Recursive flush and recording during active flush are rejected. P7-T03 intentionally defines no automatic update-phase scheduling; P7-T04 owns phase boundaries.

Wiki impact: none — P7-T03 adds no supported public engine API or consumer-visible behavior.
Sandbox impact: none — deferred commands remain package-private and there is still no public world lifecycle to exercise honestly.
Independent review: not performed in the connected authoring session; no independent reviewer/provenance was available.

## Phase 7 fixed world update phases — P7-T04 / Issue #420

P7-T04 is active from accepted P7-T03 merge baseline `e97dceb477d9492d28aa2a924e5c5adc93628ba8` on branch `p7-t04-fixed-world-update-phases`. The bounded candidate adds package-private `WorldUpdatePhase` and `WorldUpdatePipeline` only in `engine-world`. One caller-supplied callback runs exactly once in fixed order: INPUT, PRE_PHYSICS, PHYSICS, POST_PHYSICS, GAMEPLAY, REPLICATION_CAPTURE, PRESENTATION_EXTRACTION.

After each successfully completed phase callback, the pipeline invokes the accepted P7-T03 command buffer `flush()` before the next phase. Structural changes recorded by a phase therefore remain invisible during that callback and become visible at the next successful phase boundary. The final presentation-extraction phase is also followed by a flush. Callback or flush failure propagates and stops later phases; a failed callback is not auto-flushed, and command-buffer partial-failure/tail behavior remains P7-T03-owned. Recursive update is rejected, the guard recovers after failure, and no concurrency guarantee is introduced.

Authoring preflight run #6 / `36996034761` on committed Java head `a8c9d6682db4467aec9f33e798e5090684bf1524` passed `spotlessCheck`, `:engine-world:test --rerun-tasks`, and the architecture boundary test. This branch-only authoring evidence is not final acceptance evidence. The connected environment does not execute the Gradle wrapper locally, so no separate local command run is claimed.

P7-T04 adds no public `World`/phase/system API, timing/fixed-step loop, concrete P7-T05 components, physics stepping/Jolt dependency, replication/network implementation, renderer/presentation implementation, platform input acquisition, event/query scheduler, prefab/scene/GUID behavior, production dependency, module edge, Gradle change, or lockfile change.

Wiki impact: none — all P7-T04 types remain package-private and supported consumer API is unchanged.
Sandbox impact: none — there is still no public world lifecycle through which the sandbox could exercise update phases honestly.
Independent review: not performed in this connected session; no independent reviewer/provenance is available.

Exact next action: finish P7-T04 / Issue #420 only, complete final diff/self-review/consistency audit, open one final non-draft PR, require the five-job exact-head matrix, merge only while tested head/base remain current, then require exact-merge Lightweight verification before closing #420. Do not materialize P7-T05 before P7-T04 is accepted.
"""
    status.write_text(status_text[:index] + replacement, encoding="utf-8", newline="\n")

replace_once(
    "docs/ARCHITECTURE.md",
    "P7-T03 adds package-private FIFO deferred entity/component structural commands with explicit flush; public world lifecycle, fixed update phases, concrete components, and prefab/scene behavior remain planned",
    "P7-T03 adds package-private FIFO deferred entity/component structural commands with explicit flush; P7-T04 adds a package-private fixed seven-phase update pipeline with a structural-command flush after each successfully completed phase; public world lifecycle, concrete components, and prefab/scene behavior remain planned",
)

arch = Path("docs/ARCHITECTURE.md")
arch_text = arch.read_text(encoding="utf-8")
if "## Phase 7 fixed world update phases — P7-T04 / Issue #420" not in arch_text:
    arch_text = arch_text.rstrip() + """


## Phase 7 fixed world update phases — P7-T04 / Issue #420

`engine-world` adds package-private `WorldUpdatePhase` and `WorldUpdatePipeline` above the accepted P7-T03 deferred-command boundary. One already-scheduled world update invokes exactly one internal callback for each phase in fixed order: INPUT -> PRE_PHYSICS -> PHYSICS -> POST_PHYSICS -> GAMEPLAY -> REPLICATION_CAPTURE -> PRESENTATION_EXTRACTION. The pipeline is ordering/visibility infrastructure only; it owns no wall-clock timing, fixed-step accumulation, sleeping, render loop, physics step, replication codec, presentation extractor, system registry, or parallel job scheduling.

After a phase callback returns successfully, the pipeline invokes the existing `DeferredStructuralCommandBuffer.flush()` before entering the next phase. Structural commands recorded in PRE_PHYSICS are therefore invisible within PRE_PHYSICS but visible to PHYSICS after the successful boundary; the same rule applies at every boundary, including a final flush after PRESENTATION_EXTRACTION. P7-T04 does not duplicate allocator, packed-store, or command-queue semantics.

If a phase callback throws, the exception propagates, later phases do not run, and commands recorded by the failed callback are not auto-flushed. If a boundary flush throws, the exception propagates and later phases do not run; already-applied and still-queued work retains the accepted P7-T03 partial-failure semantics. Recursive update is rejected and the update guard is restored in `finally`. Access remains externally serialized and no concurrency guarantee is added.

D-082 records the fixed order and successful-phase structural visibility boundary. No public `World`, public phase/system scheduler, concrete component set, physics/network/renderer/platform dependency, persistence/protocol behavior, project edge, or production dependency is introduced.

Wiki impact: none — the phase vocabulary and pipeline are package-private implementation boundaries.
Sandbox impact: none — there is no public world lifecycle through which the persistent sandbox could invoke the pipeline without pulling later Phase 7 work forward.
"""
    arch.write_text(arch_text.rstrip() + "\n", encoding="utf-8", newline="\n")

append_before(
    "docs/DECISIONS.md",
    "\n## Adding or changing a decision\n",
    "\n| D-082 | Accepted | P7-T04 / Issue #420 fixes the package-private `engine-world` update order as INPUT -> PRE_PHYSICS -> PHYSICS -> POST_PHYSICS -> GAMEPLAY -> REPLICATION_CAPTURE -> PRESENTATION_EXTRACTION. One update is already scheduled externally. After each phase callback returns successfully, the accepted P7-T03 structural command buffer flushes before the next phase, so structural changes become visible only at successful phase boundaries. Callback or flush failure propagates and stops later phases; a failed callback is not auto-flushed. The pipeline adds no public `World`/system scheduler, timing/fixed-step ownership, physics/network/render implementation, parallelism, or concurrency guarantee. | Physics, gameplay, replication capture, and presentation extraction need one stable ordering/visibility contract before their implementations exist, otherwise later systems could observe mixed structural state. Reusing P7-T03 at explicit successful phase boundaries fixes that cross-task rule without duplicating queue semantics or pulling later subsystem implementations forward. |\n",
    "| D-082 | Accepted |",
)
