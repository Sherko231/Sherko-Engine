# Sherko Engine CI Lifecycle

This document defines the current repository CI execution policy. `AGENTS.md` remains the higher-authority agent contract; `docs/BUILD_AND_VERIFY.md` remains the command/evidence catalog. For CI lifecycle questions, this document supersedes the legacy generic `## CI gate` wording in `docs/BUILD_AND_VERIFY.md`. Historical task sections in that file may still describe the five-job merged-`master` workflows that were genuinely required when those tasks completed; those historical facts remain valid evidence but do not redefine the current default policy below.

This split is deliberate: the large build/evidence catalog is preserved without rewriting historical acceptance records, while current CI routing and agent behavior have one unambiguous policy source.

## Goal

Preserve the same practical merge confidence while avoiding duplicate expensive GitHub Actions runs on the self-hosted Windows x64 runner.

For an ordinary successful non-Markdown task, the expected runner pattern is:

```text
branch development:      0 heavy GitHub Actions runs
final PR candidate:      1 heavy five-job run
merged master commit:    1 lightweight verification job
```

Additional heavy runs are justified only when the actual candidate changes, a CI failure is corrected, the tested base becomes stale, or the active Issue explicitly requires stronger exact-merge evidence.

## Branch-development stage

Do not open a pull request merely to obtain a development workspace.

Before opening the final non-draft PR, the agent should complete on the dedicated branch:

- the bounded implementation;
- focused/local verification that the authoring environment can actually execute;
- required tests and evidence preparation;
- required status/architecture/decision/build/wiki/sandbox documentation;
- self-review and consistency audit;
- reconciliation with current `master` and the active Issue.

Feature-branch pushes do not trigger the heavy workflow. This is intentional.

A draft PR may be used when early human/reviewer visibility is specifically useful. Draft PR events may create a GitHub workflow record, but the expensive jobs are gated off while the PR is draft. Drafts are not the normal CI-development loop.

## Final PR candidate

The ordinary heavy gate consists of the existing five jobs:

1. `Build and quality gates`
2. `Unit tests`
3. `Architecture tests`
4. `JaCoCo coverage reports`
5. `Windows native smoke`

These jobs run for a non-draft PR targeting `master` and for explicit `workflow_dispatch`.

Only a passing run for the exact current final PR candidate is authoritative. If a substantive code/config/test change or a required documentation change is committed afterward, the old run is obsolete and the new candidate requires another heavy run.

Avoid cosmetic/status-only commits after a passing candidate by finishing expected handoff documentation before opening the PR.

## Base freshness before merge

The passing PR run proves the tested candidate against the repository state represented by that PR. Before merge, verify the candidate has not become stale relative to `master`.

If `master` advanced in a way that changes the candidate/base relationship, refresh the branch as appropriate and require heavy CI again. Do not merge based on stale candidate evidence merely to save runner time.

## Exact merged-master verifier

Ordinary pushes to `master` no longer repeat the entire five-job matrix.

The lightweight `master` job must run on the exact pushed SHA and verify at least:

- committed dependency locks resolve without tracked lockfile drift;
- `:game-server:verifyHeadlessServerRuntime` still passes;
- client/server `--version` reports contain their required identifiers;
- shared compatibility identifiers match;
- reported `engineCommit` equals the exact `github.sha` of the merged `master` commit.

The linked Issue closes only after this exact-merge lightweight job passes, unless the complete change is Markdown-only exempt.

This verifier is intentionally not a substitute for the heavy PR gate. It checks exact-merge identity and high-value composition invariants that are sensitive to the final merge SHA without rerunning expensive unit/coverage/native work on effectively the same candidate.

## When full post-merge CI is still appropriate

Use `workflow_dispatch` for a complete five-job run after merge only when the active Issue explicitly requires exact-merge evidence that the lightweight verifier cannot establish, for example:

- a phase/release gate requiring retained evidence tied to the final merge SHA;
- a native/performance/protocol acceptance whose report must name the exact merge SHA;
- investigation of a merge-only or environment-specific failure;
- an explicit owner request for a full recheck.

Do not make full post-merge CI routine again by copying historical task wording into new Issues.

## Failed CI and retries

A failed final-candidate run that leads to a correction is not wasted work: the candidate changed and must be verified again.

Do not bypass or ignore a failing required job. Do not cancel the current candidate's run to save time. Superseded queued/in-progress PR runs may be cancelled automatically through workflow concurrency or manually when tooling permits; a cancelled stale run is neither a pass nor a failure for the current candidate.

## Markdown-only exemption

The existing Markdown-only exemption remains unchanged: the complete PR changed-file set must be non-empty and every changed path must end in `.md`. Such changes do not require the heavy PR build/test matrix or the lightweight post-merge runtime verifier unless an active Issue explicitly imposes additional manual/document checks.

## Workflow mapping

`.github/workflows/java25.yml` is expected to implement this split:

- `pull_request` + non-draft candidate -> five heavy jobs;
- `workflow_dispatch` -> five heavy jobs;
- `push` to `master` -> `Lightweight master verification` only;
- Markdown-only pull requests / pushes -> ignored by automatic build/test triggers through `paths-ignore`.

Repository branch protection/rulesets are separate platform configuration. If GitHub does not enforce these checks itself, the agent contract still does. Never claim platform-enforced protection without inspecting live rules.
