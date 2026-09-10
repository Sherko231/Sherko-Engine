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

If a lower source conflicts with a higher source, stop and report the conflict. Do not silently choose one.

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
- Distinguish a configured CI workflow from a platform-enforced merge requirement. Inspect branch protection or repository rulesets before claiming that CI blocks merging; if no required check exists, state that the agent contract still forbids merging before a passing exact-head run.
- Describe automated architecture and quality gates only to the extent their executable tests actually cover. Record known exclusions or gaps; do not infer comprehensive enforcement from task names or configuration.
- Search repository documentation for stale claims about phase/task state, dependency versions, module counts, runner environment, CI enforcement, and gate coverage. A targeted search supplements reading; it does not replace checking the authoritative sources.

## Work rules

- Use Java 25 for authored engine/game runtime source. Gradle Kotlin DSL is allowed only for build configuration.
- Implement exactly one executable Issue on one dedicated branch.
- Never commit agent-generated work directly to `master`.
- Open a pull request linked to the Issue and merge only after required verification and CI pass.
- Keep lower engine modules independent of game-specific modules.
- Do not pull deferred features into v1 unless `ENGINE_SCOPE.md` is deliberately changed.
- Do not convert feasibility spikes under the root `src/` tree into production architecture by accident.
- Treat native resources as explicitly owned and closed; garbage collection is not native cleanup.
- Do not use Java object serialization for disk or network protocols.

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

Never claim a check passed because configuration appears correct. Record actual execution or say it was not run.

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

Update only the rows that apply. Do not copy volatile live status into every document.

## Handoff checklist

Before yielding to another agent:

1. Confirm `git status`, branch, and HEAD.
2. Confirm the active Issue and pull request state.
3. Run applicable verification from `docs/BUILD_AND_VERIFY.md`.
4. Update the required documents from the matrix.
5. Repeat the consistency audit and resolve every stale or overstated claim in the files affected by the active Issue.
6. Put the exact next action, remaining blockers, and skipped checks in `docs/DEVELOPMENT_STATUS.md` or the pull request, as appropriate.
7. Ensure all changes are committed and pushed. Uncommitted local state is not transferable through Markdown.
