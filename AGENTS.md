# Sherko Engine — AI Agent Contract

This is the mandatory starting point for any AI coding agent working in this repository. Follow it before editing code, documentation, Issues, or build configuration.

## Required read order

1. `AGENTS.md` — operating rules and truth hierarchy.
2. `ENGINE_SCOPE.md` — locked product and architecture boundaries.
3. `docs/DEVELOPMENT_STATUS.md` — checkpoint represented by the current repository commit.
4. `ROADMAP.md` — milestone order and outcomes.
5. The active GitHub Issue — the executable implementation contract.
6. The relevant phase/task in `docs/roadmap/TECHNICAL_BACKLOG.md`.
7. Relevant sections of `docs/ARCHITECTURE.md`, `docs/DECISIONS.md`, `docs/BUILD_AND_VERIFY.md`, and feasibility evidence.
8. `docs/SPATIAL_CONVENTIONS.md` before any task that defines, consumes, converts, serializes, tests, or documents math/spatial behavior, transforms, camera/view/projection behavior, renderer world-space assumptions, physics coordinates, asset-space conversion, spatial audio, or network-spatial/quantization behavior.
9. Relevant pages under `wiki/` when the task adds, removes, renames, behaviorally changes, or changes the intended consumption of a public engine API.
10. `game-sandbox/README.md` when the task adds or materially changes an engine capability that may be human-observable through production public APIs.

Do not read the entire backlog or the spatial-conventions document as permission to implement future work. One active Issue defines one bounded change.

## Truth hierarchy

Use the highest applicable source when information conflicts:

1. `ENGINE_SCOPE.md` for product boundaries and locked technology choices.
2. `docs/DECISIONS.md` for durable architecture decisions within that scope.
3. `docs/SPATIAL_CONVENTIONS.md` for the accepted canonical engine world-space, axis, linear/angular-unit, rotation-sign, scale, and adapter-boundary contract when the question is spatial; it elaborates D-041 and may not override `ENGINE_SCOPE.md` or accepted decisions.
4. The active GitHub Issue for the exact task contract and allowed change.
5. Code, tests, Gradle files, workflow configuration, and generated verification evidence for implemented behavior.
6. `docs/DEVELOPMENT_STATUS.md` for the repository-commit checkpoint and handoff summary.
7. GitHub Issues/Project for live workflow state that may have changed after the checked-out commit.
8. `ROADMAP.md` and `docs/roadmap/TECHNICAL_BACKLOG.md` for planned outcomes and future task definitions.
9. `wiki/` for human/AI consumer guidance and practical usage of already implemented public engine APIs.
10. `game-sandbox/` for owner-facing demonstration of already implemented public behavior only; it never overrides production code/tests or the sources above.

If a lower source conflicts with a higher source, stop and report the conflict. Do not silently choose one. The wiki and sandbox never override scope, accepted decisions, the canonical spatial contract when applicable, an active Issue, code/tests/evidence, status, or roadmap/backlog; correct the lower source instead.

## Freshness and repository state

- The current Git commit is the identity of the checked-out snapshot. Run `git status --short --branch` and `git rev-parse HEAD` before work.
- `docs/DEVELOPMENT_STATUS.md` describes the commit that contains it. Its recorded baseline commit is the last known merge before the checkpoint edit; a document cannot contain the hash of the commit that creates itself.
- Before implementing, fetch the remote and confirm the task branch starts from current `master`.
- Inspect uncommitted and untracked files. They belong to the user unless proven otherwise; preserve them.
- Check the active Issue and open pull requests for newer live state. Markdown alone cannot represent uncommitted work or GitHub activity after the commit.

## Consistency audit

Run this audit before implementation and again before opening the final pull request:

- Verify every documented dependency or tool major version against the version catalog, relevant Gradle build files, and committed dependency lockfiles. When they disagree, stop and reconcile the active Issue before editing.
- Treat `README.md` as repository orientation, not an independent status authority. If it names the current phase, completed milestone, module count, or next task, reconcile it with `docs/DEVELOPMENT_STATUS.md`, the roadmap, and live GitHub state.
- For any spatially relevant task, compare the active Issue, implementation/tests, architecture text, and consumer guidance against `docs/SPATIAL_CONVENTIONS.md`. Do not let a library default, renderer/physics convention, imported asset basis, screen-space convention, or packet format silently redefine engine world space or units.
- Distinguish a configured CI workflow from a platform-enforced merge requirement. Inspect branch protection or repository rulesets before claiming that CI blocks merging; if no required check exists, state that the agent contract still forbids merging before the required final-candidate verification unless the complete diff qualifies for the Markdown-only exemption below.
- Describe automated architecture and quality gates only to the extent their executable tests actually cover. Record known exclusions or gaps; do not infer comprehensive enforcement from task names or configuration.
- Search repository documentation for stale claims about phase/task state, dependency versions, module counts, runner environment, CI enforcement, and gate coverage. A targeted search supplements reading; it does not replace checking the authoritative sources.
- When public API or consumer-visible usage changed, compare `wiki/API_INDEX.md`, relevant usage/example pages, and `wiki/LIMITATIONS.md` with the actual production signatures and behavior. Remove stale examples and never document planned APIs as implemented.
- When the changed capability is or should be observable in `game-sandbox`, compare the sandbox behavior/instructions with the production public API and the active Issue. Do not leave a stale owner-facing demo silently behind the engine.
- Before opening the final PR, make all expected documentation, wiki, sandbox, review-record, and handoff edits. Do not intentionally leave cosmetic/status cleanup until after a passing heavy PR run, because any later commit invalidates that candidate's CI evidence.

## Work rules

- Use Java 25 for authored engine/game runtime source. Gradle Kotlin DSL is allowed only for build configuration.
- Implement exactly one executable Issue on one dedicated branch.
- Never commit agent-generated work directly to `master`.
- Treat the task branch as the development workspace. Finish implementation, focused verification where available, tests, required documentation, self-review, sandbox/wiki impact, and the consistency audit **before opening the normal non-draft PR**.
- Do not open a non-draft PR as a scratchpad while implementation is still expected to change. A draft PR is allowed only when early human/reviewer visibility is specifically useful; draft state is not the normal heavy-CI trigger and does not replace final-candidate verification.
- Open the final PR linked to the Issue only when the candidate is ready for the expensive repository CI matrix. For non-exempt work use a non-closing reference such as `Refs #123`.
- Merge only after the required final PR-candidate verification passes and the candidate/base are still current. If the candidate changes after a pass, the old run is obsolete. If `master` advances relative to the tested candidate base, refresh the branch/candidate and reverify before merge.
- For ordinary non-exempt work, close the Issue manually only after the lightweight exact-merge `master` verifier passes on the resulting merge commit. A second routine five-job heavy matrix on `master` is intentionally not required.
- A task may require stronger post-merge evidence only when its active Issue explicitly needs exact-merge native, performance, soak, protocol, release, or phase-gate evidence that the lightweight verifier cannot establish. Use `workflow_dispatch` or the task-specific command for that explicit need rather than making every task pay that cost.
- A qualifying Markdown-only pull request may use a closing keyword only after its complete-diff exemption and every other acceptance requirement is confirmed.
- Keep lower engine modules independent of game-specific modules.
- Do not pull deferred features into v1 unless `ENGINE_SCOPE.md` is deliberately changed.
- Do not convert feasibility spikes under the root `src/` tree into production architecture by accident.
- Treat native resources as explicitly owned and closed; garbage collection is not native cleanup.
- Do not use Java object serialization for disk or network protocols.
- Keep `wiki/` synchronized with production consumer behavior: when a task adds/removes/renames a public engine API, changes a public signature, or changes lifecycle/ownership/threading/failure/configuration semantics visible to callers, update the relevant wiki pages in the same PR. If there is no wiki impact, record `Wiki impact: none — <reason>` rather than making meaningless wiki churn.

## Owner-facing sandbox demo

`game-sandbox` is the canonical manual demo used by the owner to observe the engine's current behavior. It supplements automated verification; it is not test or benchmark authority.

For every task that adds or materially changes an engine capability, explicitly evaluate sandbox impact before handoff:

- If the capability can be demonstrated through already-authorized **public production APIs** without implementing future roadmap work, update the relevant `game-sandbox` demo in the same PR and keep `game-sandbox/README.md` current.
- If the capability cannot yet be demonstrated meaningfully because the required public API/presentation layer does not exist, record `Sandbox impact: none — <reason>` in the PR/handoff. Do not expose a public API solely for the demo, import engine implementation/internal packages, call LWJGL/native APIs directly from the sandbox, or implement a later task to make the demo richer.
- Prefer evolving the existing sandbox experience over creating disconnected throwaway demos. Add a separate subsystem-specific entry point only when combining it into the existing demo would be materially confusing or impractical.
- Human-observable sandbox output may include clearly labeled diagnostics, but do not call a loop rate `FPS`, a benchmark, soak evidence, leak proof, or performance acceptance unless the active Issue actually establishes that measurement contract.
- Sandbox execution never replaces unit tests, native acceptance, integration evidence, final-candidate PR CI, the lightweight exact-merge verifier, P0 feasibility gates, or any stronger task-specific acceptance requirement.

The active Issue must authorize any sandbox source/module/dependency changes needed by that task. If sandbox maintenance would require an undeclared module edge or other stop-condition change, refine the Issue before editing.

## Task contracts and test intent

Before implementing a task that adds or changes a public API or durable architecture decision, put the following in its active Issue:

- one realistic caller/use case with inputs, operation sequence, and observable expected result;
- relevant failure cases, including invalid input/order, partial failure, ownership, and cleanup where applicable;
- why this capability or abstraction is needed by the current task, and the simpler alternative considered.

Resolve a missing or contradictory contract before coding; an implementation choice must not silently redefine acceptance. Documentation-only tasks can mark these fields not applicable with a reason.

Derive tests from the Issue's observable requirements. For each behavior or parameterized family, identify the realistic fault it would catch and how the expected result is determined independently of the implementation. Do not use production output as its own expected value or treat a mock-only reproduction as proof that production behavior works. This does not require one document row per test or a new testing framework.

## Review evidence

For public API or durable architecture changes, seek review from a person or a separate agent that did not author the change. The reviewer must inspect the Issue, applicable scope/decisions, actual diff, tests, and the relevant wiki/API usage guidance when consumer behavior changed, rather than relying only on the author's summary.

Use the PR review record to identify the reviewer/type, reviewed commit SHA, findings, their disposition, and remaining uncertainty. Review must question scope, unnecessary complexity, whether the contract itself is correct, whether tests could pass despite a violated requirement, and whether wiki examples or limitations misrepresent the implemented public API. No findings is a valid result only with stated review coverage.

The author's second pass is self-review, not independent review. If independent review is unavailable, record `not performed`, the reason, and the remaining risk; never invent approval or imply that CI/checklists substitute for review. Any explicit review gate in the active Issue or repository settings still applies. Reassess affected findings after substantive changes and record the final reviewed SHA.

## Phase integration and planning review

Task completion does not establish phase completion. Follow the phase verification procedure in `docs/BUILD_AND_VERIFY.md`: demonstrate the existing backlog exit gate through the relevant integrated runtime/test path, record evidence, and review the next phase before materializing its executable Issues. Use a small integration scenario within an authorized task when its behavior becomes testable; do not add future systems merely to create a demo. Keep the backlog's numeric thresholds, scope, and native evidence limits unchanged unless a separate Issue explicitly authorizes changing them.

A phase exit, release gate, native soak, protocol evidence task, or other Issue may explicitly require stronger exact-merge verification than the ordinary lightweight `master` verifier. That stronger requirement remains authoritative for that specific task and should be invoked deliberately, usually through the task-specific command or `workflow_dispatch` full CI.

## Stop conditions

Stop implementation and report the conflict when the task would require any of the following without explicit authorization in the Issue:

- changing `ENGINE_SCOPE.md`;
- introducing or reversing a module dependency;
- changing a public engine API, persisted format, packet layout, authority rule, coordinate convention, or ownership model;
- selecting a new production dependency or native binding;
- treating an unproven feasibility conclusion as production-ready;
- modifying unrelated user work;
- implementing work from another roadmap task.

Record an approved durable architecture change in `docs/DECISIONS.md` before or with its implementation.

## Verification rule

Use `docs/BUILD_AND_VERIFY.md` to choose the required commands. A handoff must state:

- commands executed;
- environment used;
- pass/fail result;
- skipped checks and the reason;
- artifact/evidence path for native, performance, or protocol work.

Never claim a check passed because configuration appears correct. Record actual execution or say it was not run. For a qualifying Markdown-only change, explicitly record the complete-diff audit and that build/test CI was not required by policy; do not describe the absence of a run as a pass.

## CI lifecycle and obsolete-run handling

The normal non-exempt CI lifecycle is intentionally optimized to avoid duplicate runner work while preserving a strong final candidate gate.

### Development before the PR

- Perform ordinary implementation work on the dedicated branch before opening the normal non-draft PR.
- The standard workflow does not run heavy CI for ordinary feature-branch pushes by themselves. Use focused local/task verification during development when available.
- Complete expected code, tests, docs, wiki/sandbox updates, self-review, and consistency reconciliation before opening the final PR.
- A draft PR may be used for early visibility, but the heavy five-job matrix is not the normal draft-development loop. Mark/open the PR non-draft only when the candidate is ready to be judged.

### Final PR candidate

A pull request qualifies for the **Markdown-only CI exemption** only when its complete changed-file set is non-empty and every changed path ends in `.md`. The agent must inspect the complete PR diff/file list before relying on this exemption. If any changed path has any other extension or file type — including `.java`, `.gradle.kts`, `.yml`, `.yaml`, `.properties`, lockfiles, configuration, scripts, resources, or binaries — the exemption does not apply and the normal CI rules below apply in full. If a later commit adds any non-Markdown path, the PR immediately becomes non-exempt and final-candidate CI is required.

For a qualifying Markdown-only pull request:

- heavy PR build/test CI is not required before merge;
- the lightweight `master` build/runtime verifier is not required after merge because Markdown paths are ignored by that workflow;
- the absence of those workflow runs is expected and is not a skipped failure;
- this exemption affects build/runtime execution verification only. It does not waive the active Issue, truth hierarchy, branch/PR discipline, documentation consistency, review requirements, architecture/decision rules, wiki synchronization requirements, sandbox-impact evaluation, or any explicit manual verification required by the Issue.

For every non-exempt final PR candidate:

- Require the configured heavy five-job matrix: `Build and quality gates`, `Unit tests`, `Architecture tests`, `JaCoCo coverage reports`, and `Windows native smoke`.
- Determine the PR's current head SHA and ensure the accepted workflow run belongs to that current candidate. A run for an older candidate is obsolete evidence.
- Repository workflow concurrency may cancel superseded PR runs automatically. An obsolete cancelled run is neither a pass nor a failure of the current candidate.
- Never cancel the current final-candidate run merely to reduce runner usage.
- A correction after a failed CI run is legitimate new candidate work; rerunning CI is required because the code changed.
- Do not append cosmetic or status commits after a successful final-candidate run. If any commit changes the candidate, require a new heavy run.
- Immediately before merge, confirm the PR head is still the tested head and `master`/the PR base has not advanced relative to the candidate that was verified. If it advanced, refresh the branch/candidate and run heavy CI again. Do not assume an old green run proves a new base combination.

### After merge

For ordinary non-exempt tasks:

- The `push` workflow on `master` runs one **Lightweight master verification** job on the exact merge SHA rather than repeating the five heavy jobs.
- That verifier must resolve committed dependency locks without drift, verify the headless server runtime boundary, run client/server version reporting, require shared compatibility identifiers to match, and require reported `engineCommit == github.sha`.
- Require that lightweight job to pass on the exact merge commit before closing the Issue.
- Do not rerun unit/coverage/architecture/native smoke automatically on `master`; those were already required on the final candidate.
- If the active Issue explicitly requires exact-merge native/performance/soak/protocol/release/phase-gate evidence, run that stronger evidence separately. `workflow_dispatch` remains available to run the full five-job matrix intentionally when it is genuinely required.

Manual `workflow_dispatch` remains available regardless of file type and runs the heavy matrix by design.

## Documentation update matrix

| Change type | Required documentation |
| --- | --- |
| Product target, platform, v1 boundary, locked technology | `ENGINE_SCOPE.md`, then reconcile `ROADMAP.md` and decisions |
| Durable architectural choice or superseded choice | `docs/DECISIONS.md` and `docs/ARCHITECTURE.md` |
| Canonical world-space handedness/axes, linear or angular units, rotation sign, transform-scale convention, or external spatial conversion boundary | `docs/SPATIAL_CONVENTIONS.md`, `docs/DECISIONS.md`, and `docs/ARCHITECTURE.md`; reconcile affected tests/wiki/adapter docs |
| Module role/dependency/status change | `docs/ARCHITECTURE.md` |
| Build command, CI gate, or evidence command change | `docs/BUILD_AND_VERIFY.md` |
| Completed task, next action, blocker, or verified conclusion | `docs/DEVELOPMENT_STATUS.md` |
| Repository orientation or current-state summary | `README.md` plus `docs/DEVELOPMENT_STATUS.md`; the README must not contradict the checkpoint or live-state instructions |
| Milestone ordering/outcome change | `ROADMAP.md` |
| Planned task definition or acceptance change | `docs/roadmap/TECHNICAL_BACKLOG.md`; update an existing executable Issue too |
| Feasibility run/result change | matching file under `docs/feasibility/` plus status if the conclusion is durable |
| Public engine API or consumer-visible API usage/lifecycle/ownership/configuration behavior change | relevant `wiki/` pages, including `wiki/API_INDEX.md` and `wiki/LIMITATIONS.md` when public surface/availability changes |
| Human-observable engine capability or sandbox maintenance policy change | relevant `game-sandbox` source plus `game-sandbox/README.md`; if no runnable sandbox update is appropriate, record `Sandbox impact: none — <reason>` |

Update only the rows that apply. Do not copy volatile live status into every document. The wiki is consumer guidance and must not become a competing status/architecture authority.

## Handoff checklist

Before yielding to another agent or opening the final PR:

1. Confirm `git status`, branch, and HEAD.
2. Confirm the active Issue and pull request state; normally no non-draft PR exists until the candidate is final.
3. Run applicable focused verification from `docs/BUILD_AND_VERIFY.md`, or record a complete-diff Markdown-only exemption when it applies.
4. Update the required documents from the matrix before final-candidate CI.
5. If the task is spatially relevant, verify the final contract/code/tests and any affected adapter/wiki guidance remain consistent with `docs/SPATIAL_CONVENTIONS.md` and do not silently redefine engine world space or units.
6. If public API or consumer-visible behavior changed, verify the relevant `wiki/` pages/examples against production signatures and behavior; otherwise record `Wiki impact: none — <reason>` in the PR/handoff.
7. Evaluate `game-sandbox` impact. Update the demo/README through production public APIs when appropriate; otherwise record `Sandbox impact: none — <reason>` without bypassing boundaries or pulling future tasks forward.
8. Repeat the consistency audit and resolve every stale or overstated claim in the files affected by the active Issue.
9. Put the exact next action, remaining blockers, and skipped checks in `docs/DEVELOPMENT_STATUS.md` or the pull request, as appropriate.
10. Record review provenance and unresolved findings in the PR; for phase completion, link integration evidence and the next-phase planning review.
11. Ensure all intended changes are committed and pushed **before** opening/marking the final PR ready for heavy CI. Uncommitted local state is not transferable through Markdown.
12. After the final PR run passes, avoid unnecessary candidate changes; merge only while the tested head/base remain current, then require the lightweight exact-merge verifier before task closure unless the Issue explicitly requires stronger post-merge evidence.