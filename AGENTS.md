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
8. Relevant pages under `wiki/` when the task adds, removes, renames, behaviorally changes, or changes the intended consumption of a public engine API.

Do not read the entire backlog as permission to implement future work. One active Issue defines one bounded change.

## Truth hierarchy

Use the highest applicable source when information conflicts:

1. `ENGINE_SCOPE.md` for product boundaries and locked technology choices.
2. `docs/DECISIONS.md` for durable architecture decisions within that scope.
3. The active GitHub Issue for the exact task contract and allowed change.
4. Code, tests, Gradle files, and generated verification evidence for implemented behavior.
5. `docs/DEVELOPMENT_STATUS.md` for the repository-commit checkpoint and handoff summary.
6. GitHub Issues/Project for live workflow state that may have changed after the checked-out commit.
7. `ROADMAP.md` and `docs/roadmap/TECHNICAL_BACKLOG.md` for planned outcomes and future task definitions.
8. `wiki/` for human/AI consumer guidance and practical usage of already implemented public engine APIs.

If a lower source conflicts with a higher source, stop and report the conflict. Do not silently choose one. The wiki never overrides scope, decisions, an active Issue, code/tests/evidence, status, or roadmap/backlog; correct the wiki instead.

## Freshness and repository state

- The current Git commit is the identity of the checked-out snapshot. Run `git status --short --branch` and `git rev-parse HEAD` before work.
- `docs/DEVELOPMENT_STATUS.md` describes the commit that contains it. Its recorded baseline commit is the last known merge before the checkpoint edit; a document cannot contain the hash of the commit that creates itself.
- Before implementing, fetch the remote and confirm the task branch starts from current `master`.
- Inspect uncommitted and untracked files. They belong to the user unless proven otherwise; preserve them.
- Check the active Issue and open pull requests for newer live state. Markdown alone cannot represent uncommitted work or GitHub activity after the commit.

## Consistency audit

Run this audit before implementation and again before handoff:

- Verify every documented dependency or tool major version against the version catalog, relevant Gradle build files, and committed dependency lockfiles. When they disagree, stop and reconcile the active Issue before editing.
- Treat `README.md` as repository orientation, not an independent status authority. If it names the current phase, completed milestone, module count, or next task, reconcile it with `docs/DEVELOPMENT_STATUS.md`, the roadmap, and live GitHub state.
- Distinguish a configured CI workflow from a platform-enforced merge requirement. Inspect branch protection or repository rulesets before claiming that CI blocks merging; if no required check exists, state that the agent contract still forbids merging before a passing exact-head run unless the complete diff qualifies for the Markdown-only exemption below.
- Describe automated architecture and quality gates only to the extent their executable tests actually cover. Record known exclusions or gaps; do not infer comprehensive enforcement from task names or configuration.
- Search repository documentation for stale claims about phase/task state, dependency versions, module counts, runner environment, CI enforcement, and gate coverage. A targeted search supplements reading; it does not replace checking the authoritative sources.
- When public API or consumer-visible usage changed, compare `wiki/API_INDEX.md`, relevant usage/example pages, and `wiki/LIMITATIONS.md` with the actual production signatures and behavior. Remove stale examples and never document planned APIs as implemented.

## Work rules

- Use Java 25 for authored engine/game runtime source. Gradle Kotlin DSL is allowed only for build configuration.
- Implement exactly one executable Issue on one dedicated branch.
- Never commit agent-generated work directly to `master`.
- Open a pull request linked to the Issue and merge only after required verification and CI pass, except that a qualifying Markdown-only change does not require the build/test CI described below.
- For a non-exempt pull request, use a non-closing Issue reference such as `Refs #123`; close the Issue manually only after the exact merged-`master` push CI passes. A qualifying Markdown-only pull request may use a closing keyword only after its complete-diff exemption and every other acceptance requirement is confirmed.
- Keep lower engine modules independent of game-specific modules.
- Do not pull deferred features into v1 unless `ENGINE_SCOPE.md` is deliberately changed.
- Do not convert feasibility spikes under the root `src/` tree into production architecture by accident.
- Treat native resources as explicitly owned and closed; garbage collection is not native cleanup.
- Do not use Java object serialization for disk or network protocols.
- Keep `wiki/` synchronized with production consumer behavior: when a task adds/removes/renames a public engine API, changes a public signature, or changes lifecycle/ownership/threading/failure/configuration semantics visible to callers, update the relevant wiki pages in the same PR. If there is no wiki impact, record `Wiki impact: none — <reason>` rather than making meaningless wiki churn.

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

## CI run authority and obsolete-run handling

Before interpreting pull-request CI, determine the pull request's current head SHA and compare each workflow run against it.

A pull request qualifies for the **Markdown-only CI exemption** only when its complete changed-file set is non-empty and every changed path ends in `.md`. The agent must inspect the complete PR diff/file list before relying on this exemption. If any changed path has any other extension or file type — including `.java`, `.gradle.kts`, `.yml`, `.yaml`, `.properties`, lockfiles, configuration, scripts, resources, or binaries — the exemption does not apply and the normal CI rules below apply in full. If a later commit adds any non-Markdown path, the PR immediately becomes non-exempt and exact-head CI is required.

For a qualifying Markdown-only pull request:

- exact-head build/test CI is not required before merge;
- merged-`master` build/test CI is not required after merge;
- the absence of those workflow runs is expected and is not a skipped failure;
- this exemption affects build/runtime execution verification only. It does not waive the active Issue, truth hierarchy, branch/PR discipline, documentation consistency, review requirements, architecture/decision rules, wiki synchronization requirements, or any explicit manual verification required by the Issue.

For every non-exempt pull request:

- A pull-request workflow run whose head SHA is older than the current PR head is obsolete verification evidence for that PR.
- Obsolete queued or in-progress PR runs may be cancelled when the available tooling and permissions support cancellation. Cancelling stale work is an efficiency measure, not a verification result.
- Never cancel the workflow run for the current PR head merely to reduce runner usage.
- A cancelled obsolete run is neither a pass nor a failure of the current PR head. Do not use it to satisfy or defeat acceptance.
- Only a completed passing workflow for the exact current PR head satisfies the pre-merge CI requirement.
- After merge, the push workflow on the resulting `master` merge commit is separate required evidence. Do not cancel or ignore it as though it were an obsolete PR run.
- If cancellation tooling is unavailable, leave obsolete runs alone and state that they remain stale; never claim that they were cancelled.
- Repository workflow concurrency may cancel superseded runs automatically. Still inspect the exact current PR head and the final `master` merge commit before recording verification.

Manual `workflow_dispatch` remains available regardless of file type.

## Documentation update matrix

| Change type | Required documentation |
| --- | --- |
| Product target, platform, v1 boundary, locked technology | `ENGINE_SCOPE.md`, then reconcile `ROADMAP.md` and decisions |
| Durable architectural choice or superseded choice | `docs/DECISIONS.md` and `docs/ARCHITECTURE.md` |
| Module role/dependency/status change | `docs/ARCHITECTURE.md` |
| Build command, CI gate, or evidence command change | `docs/BUILD_AND_VERIFY.md` |
| Completed task, next action, blocker, or verified conclusion | `docs/DEVELOPMENT_STATUS.md` |
| Repository orientation or current-state summary | `README.md` plus `docs/DEVELOPMENT_STATUS.md`; the README must not contradict the checkpoint or live-state instructions |
| Milestone ordering/outcome change | `ROADMAP.md` |
| Planned task definition or acceptance change | `docs/roadmap/TECHNICAL_BACKLOG.md`; update an existing executable Issue too |
| Feasibility run/result change | matching file under `docs/feasibility/` plus status if the conclusion is durable |
| Public engine API or consumer-visible API usage/lifecycle/ownership/configuration behavior change | relevant `wiki/` pages, including `wiki/API_INDEX.md` and `wiki/LIMITATIONS.md` when public surface/availability changes |

Update only the rows that apply. Do not copy volatile live status into every document. The wiki is consumer guidance and must not become a competing status/architecture authority.

## Handoff checklist

Before yielding to another agent:

1. Confirm `git status`, branch, and HEAD.
2. Confirm the active Issue and pull request state.
3. Run applicable verification from `docs/BUILD_AND_VERIFY.md`, or record a complete-diff Markdown-only exemption when it applies.
4. Update the required documents from the matrix.
5. If public API or consumer-visible behavior changed, verify the relevant `wiki/` pages/examples against production signatures and behavior; otherwise record `Wiki impact: none — <reason>` in the PR/handoff.
6. Repeat the consistency audit and resolve every stale or overstated claim in the files affected by the active Issue.
7. Put the exact next action, remaining blockers, and skipped checks in `docs/DEVELOPMENT_STATUS.md` or the pull request, as appropriate.
8. Record review provenance and unresolved findings in the PR; for phase completion, link integration evidence and the next-phase planning review.
9. Ensure all changes are committed and pushed. Uncommitted local state is not transferable through Markdown.
