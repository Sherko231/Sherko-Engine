# GitHub Roadmap Model — Sherko Engine

This document defines the stable GitHub planning model for Sherko Engine. It does not duplicate live board columns. Repository-commit state belongs in `docs/DEVELOPMENT_STATUS.md`; activity after that commit belongs in GitHub.

## Source-of-truth layers

- `AGENTS.md` — mandatory AI read order, truth hierarchy, and handoff rules.
- `ENGINE_SCOPE.md` — product and architecture boundaries.
- `docs/DECISIONS.md` — durable architecture decisions.
- `docs/ARCHITECTURE.md` — current module roles, dependencies, and maturity.
- `ROADMAP.md` — milestone/phase-level roadmap and planning rules.
- `docs/roadmap/TECHNICAL_BACKLOG.md` — detailed task catalog and planning acceptance criteria; its checkboxes do not track completion.
- `docs/BUILD_AND_VERIFY.md` — canonical commands and evidence expectations.
- `docs/DEVELOPMENT_STATUS.md` — checkpoint represented by the containing commit.
- GitHub Milestones — milestone progress over executable Issues.
- GitHub Labels — durable task type/subsystem classification.
- GitHub Issues — executable work and task-level execution contracts.
- GitHub Project — live roadmap/board layer and workflow status after the commit.

## Milestones

Repository milestones:

- `M0 — Feasibility` — P0
- `M1 — Engine Foundation` — P1-P4
- `M2 — Local Playable Runtime` — P5-P9
- `M3 — Multiplayer Core` — P10-P13
- `M4 — Genre Systems` — P14
- `M5 — Tools` — P15
- `M6 — Production Base` — P16

Do not assign arbitrary due dates. Add dates only when they represent a real planning commitment.

## Issue model

Issues represent executable tasks only.

- Materialize tasks as Issues only when their phase is near execution.
- Keep permanent task IDs from `TECHNICAL_BACKLOG.md` in Issue titles.
- The Issue becomes the task's execution contract once work is activated.
- Do not mirror Ready/In Progress/Done state back into roadmap/backlog documents.
- Do not use repository Issues as phase epics when a Project draft item is sufficient.

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
- `Phase`: P0-P16
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

1. Pick one executable Issue.
2. Move it to `In Progress`.
3. Create a dedicated branch.
4. Give the coding agent that task only.
5. Require the Issue's acceptance criteria and appropriate verification.
6. Open a PR that closes the Issue.
7. Merge only after CI/verification passes.
8. Mark Done only after merge and acceptance verification.

Direct commits to `master` are reserved for explicit owner-directed emergencies. AI-generated implementation work always uses the branch/PR flow above.

Never ask a coding agent to implement an entire phase at once.
