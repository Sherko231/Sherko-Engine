# GitHub Roadmap Model — Sherko Engine

This document defines the stable GitHub planning model for Sherko Engine. It does not duplicate live board columns. Repository-commit state belongs in `docs/DEVELOPMENT_STATUS.md`; activity after that commit belongs in GitHub.

## Source-of-truth layers

- `AGENTS.md` — mandatory AI read order, truth hierarchy, execution/CI lifecycle, and handoff rules.
- `ENGINE_SCOPE.md` — product and architecture boundaries.
- `docs/DECISIONS.md` — durable architecture decisions.
- `docs/ARCHITECTURE.md` — current module roles, dependencies, and maturity.
- `ROADMAP.md` — milestone/phase-level roadmap and planning rules.
- `docs/roadmap/TECHNICAL_BACKLOG.md` — detailed task catalog and planning acceptance criteria; its checkboxes do not track completion.
- `docs/BUILD_AND_VERIFY.md` — canonical commands and evidence expectations.
- `docs/DEVELOPMENT_STATUS.md` — checkpoint represented by the containing commit.
- `wiki/` — lower-authority human/AI consumer guide for how to use implemented public engine APIs; it must stay synchronized with public API/usage changes.
- GitHub Milestones — milestone progress over executable Issues.
- GitHub Labels — durable task type/subsystem classification.
- GitHub Issues — executable work and task-level execution contracts.
- GitHub Project — live roadmap/board layer and workflow status after the commit.

## Milestones

Repository milestones:

- `M0 — Feasibility` — P0
- `M1 — Engine Foundation` — P1-P4
- `M2 — Local Playable Runtime` — P5, P5R, P6-P9
- `M3 — Multiplayer Core` — P10-P13
- `M4 — Genre Systems` — P14
- `M5 — Tools` — P15
- `M6 — Production Base` — P16

Do not assign arbitrary due dates. Add dates only when they represent a real planning commitment.

## Issue model

Issues represent executable tasks only.

- Materialize tasks as Issues only when their phase is near execution.
- Treat inserted phases such as `P5R` as first-class ordered phases; do not activate the following numbered phase until the inserted phase exit gate passes.
- Keep permanent task IDs from `TECHNICAL_BACKLOG.md` in Issue titles.
- The Issue becomes the task's execution contract once work is activated.
- Do not mirror Ready/In Progress/Done state back into roadmap/backlog documents.
- Do not use repository Issues as phase epics when a Project draft item is sufficient.
- For tasks that add/change public engine APIs or consumer-visible usage, include the applicable `wiki/` pages in document impact; otherwise record `Wiki impact: none — <reason>`.
- For tasks that add materially human-observable behavior, evaluate `game-sandbox` impact under `AGENTS.md`.

## Labels

### Type

- `type:spike`
- `type:feature`
- `type:bug`
- `type:docs`
- `type:refactor`
- `type:test`

### Area

- `area:build-ci`
- `area:core`
- `area:platform`
- `area:input`
- `area:math`
- `area:rendering`
- `area:assets`
- `area:world`
- `area:physics`
- `area:gameplay`
- `area:networking`
- `area:steam`
- `area:audio`
- `area:animation`
- `area:ai-nav`
- `area:editor`
- `area:runtime-ui`
- `area:release`
- `area:documentation`

### Decision

- `decision-needed`

Status and priority are Project fields, not labels.

## GitHub Project model

Project name:

`Sherko Engine Development`

### Phase cards

Create the following as **Project draft items**, not Issues:

- P0 — Feasibility gates and irreversible decisions
- P1 — Build, modules, and quality gates
- P2 — Core lifecycle, time, configuration, and native ownership
- P3 — Platform and input
- P4 — Math and spatial conventions
- P5 — Rendering foundation
- P6 — Asset pipeline and resource lifetime
- P7 — World, entities, components, prefabs, and scenes
- P8 — Physics and local interaction
- P9 — Local first-person vertical slice
- P10 — Network transport and protocol
- P11 — Replication and join-in-progress
- P12 — Prediction, reconciliation, interpolation, and physics correction
- P13 — Steam session and production transport
- P14 — Audio, animation, and AI required by the genre
- P15 — Editor and debugging tools
- P16 — Production hardening and release gate

### Project fields

- `Status`: Backlog / Ready / In Progress / In Review / Blocked / Done
- `Phase`: P0-P16 plus inserted `P5R`
- `Priority`: P0 Critical / P1 High / P2 Normal / P3 Low
- `Subsystem`: Build / CI, Core, Platform, Input, Math, Rendering, Runtime UI, Assets, World, Physics, Gameplay, Networking, Steam, Audio, Animation, AI / Navigation, Editor / Tools, Release, Documentation
- `Effort`: XS / S / M / L
- `Roadmap Level`: Phase / Task
- `Task ID`: text
- `Start Date`: date
- `Target Date`: date

No `XL`: split work that exceeds L.

### Views

1. `Roadmap` — Roadmap layout, phase draft items only.
2. `Board` — Board layout, executable task Issues grouped by Status.
3. `Backlog` — Table layout, open executable tasks.
4. `Current Phase` — Table layout filtered to the active phase.

## Execution rule

1. Pick one executable Issue and move it to `In Progress`.
2. Create a dedicated branch from current verified `master` and give the coding agent that task only.
3. Implement the bounded change **without opening a PR as a development scratchpad**. Complete focused/local verification available in the environment, required docs/wiki/sandbox work, self-review, and consistency audit first.
4. Open a linked **final non-draft PR** only when the branch is intended to be the merge candidate. Use `Refs #...` for non-exempt work; do not auto-close the Issue before post-merge evidence. A draft PR is optional only when early human/reviewer visibility is genuinely useful; heavy CI is not the purpose of a draft.
5. For a non-exempt final candidate, require the heavy five-job PR matrix to pass on the exact current PR candidate. A new substantive or required documentation commit invalidates older candidate evidence and requires a new exact-head pass.
6. Before merging, confirm `master` has not advanced relative to the candidate's tested base. If it has, refresh/rebase/merge-base as appropriate and re-run the heavy candidate CI rather than relying on stale evidence.
7. Merge only after the required final-candidate CI/verification passes, except for the documented Markdown-only build/test exemption.
8. For ordinary non-exempt work, require the lightweight exact-merge `master` verifier on the resulting merge SHA, then close the Issue. Do **not** routinely repeat the entire five-job matrix on `master`.
9. Require a stronger/full post-merge run only when the active Issue explicitly needs exact-merge native/performance/integration evidence that the lightweight verifier cannot establish. `workflow_dispatch` is the explicit escape hatch for such cases and investigations.
10. Keep relevant `wiki/` pages synchronized when public API/consumer usage changes and keep `game-sandbox` synchronized when appropriate under `AGENTS.md`.
11. Mark Done only after the applicable merge/acceptance/closure sequence is complete.

Corrections after a failed candidate CI are legitimate new candidate work, not waste. Avoidable cosmetic/status commits after a passing candidate are waste: finalize handoff/status documentation before opening the final PR whenever possible.

Direct commits to `master` are reserved for explicit owner-directed emergencies. AI-generated implementation work always uses the branch/PR flow above.

Never ask a coding agent to implement an entire phase at once.

## CI efficiency model

The default runner budget for an ordinary successful non-Markdown task is intentionally:

```text
branch development:      0 heavy GitHub Actions runs
final PR candidate:      1 heavy five-job run
merged master commit:    1 lightweight verification job
```

Additional heavy runs are expected only when the candidate actually changes after CI, CI exposes a defect that must be fixed, the tested base becomes stale, or the active Issue explicitly calls for additional evidence. Automatic concurrency cancellation may discard superseded queued/in-progress PR runs; cancelled stale work is not evidence.

The repository workflow still accepts manual dispatch for full CI when needed. Markdown-only changes retain their documented exemption after complete-diff audit.

## Wiki synchronization

The wiki is a consumer guide, not a workflow authority. `AGENTS.md` and the active Issue remain authoritative for execution. Future AI agents must update relevant `wiki/` pages/examples when public API or consumer-visible lifecycle/ownership/configuration behavior changes, and must state why no wiki change is needed when impact is none.
