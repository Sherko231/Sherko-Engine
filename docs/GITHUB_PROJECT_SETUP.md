# GitHub Project Setup — Sherko Engine

This file defines the live GitHub planning setup. The repository documentation remains the architectural source of truth; GitHub Projects is the execution/tracking layer.

## Project

Create one GitHub Project named:

`Sherko Engine Development`

## Views

Create these views over the same project items:

### 1. Roadmap
- Layout: Roadmap
- Show: phase epics (`[EPIC][P0]` through `[EPIC][P16]`)
- Use Start Date / Target Date only when real estimates exist.
- Do not invent dates just to fill the timeline.

### 2. Board
- Layout: Board
- Group by: Status
- Status values:
  - Backlog
  - Ready
  - In Progress
  - In Review
  - Blocked
  - Done

### 3. Backlog
- Layout: Table
- Show all open project items.
- Sort by Milestone/Phase, then Priority.

### 4. Current Phase
- Layout: Table
- Initially filter to `Phase = P0`.
- This is the operational work queue.

## Custom fields

Create:

- `Status` — Single select
- `Priority` — Single select: `P0 Critical`, `P1 High`, `P2 Normal`, `P3 Low`
- `Phase` — Single select: `P0` through `P16`
- `Subsystem` — Single select
- `Effort` — Single select: `XS`, `S`, `M`, `L`
- `Task ID` — Text
- `Start Date` — Date
- `Target Date` — Date

Do not create `XL`. Split work that is too large for `L`.

Recommended `Subsystem` values:

- Build / CI
- Core
- Platform
- Input
- Math
- Rendering
- Assets
- World
- Physics
- Gameplay
- Networking
- Steam
- Audio
- Animation
- AI / Navigation
- Editor / Tools
- Release
- Documentation

## Milestones

Create these repository Milestones without arbitrary due dates:

- `M0 — Feasibility` — P0
- `M1 — Engine Foundation` — P1-P4
- `M2 — Local Playable Runtime` — P5-P9
- `M3 — Multiplayer Core` — P10-P13
- `M4 — Genre Systems` — P14
- `M5 — Tools` — P15
- `M6 — Production Base` — P16

## Labels

Labels are for durable classification, not Project status/priority duplication.

### Type
- `type:epic`
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
- `area:release`
- `area:docs`

### State / decision
- `blocked`
- `decision-needed`

Do not create labels such as `status:done` or `priority:high`; those belong in Project fields.

## Issue model

- Phase epics are the roadmap items: Issues #1 through #17.
- Only the active phase is expanded into executable task Issues.
- Phase 0 task Issues are #18 through #29.
- `P0-T01` (#18) is already completed because the engine scope is locked.
- Future technical tasks remain in `docs/roadmap/TECHNICAL_BACKLOG.md` until their phase becomes active.

## Initial project item configuration

### Epics #1-#17
- Add all to the Project.
- Set `type:epic`.
- Set `Phase` to their matching P-number.
- Put only #1 (`P0`) in active/current focus.
- Keep future epics in Backlog; do not assign fake Start/Target dates.

### P0 tasks #18-#29
- Add all to the Project.
- Set `Phase = P0`.
- Set `Task ID` to the title's task ID.
- #18 -> Done.
- #19-#25 and #28-#29 -> Backlog initially; move to Ready only when dependencies permit.
- #26 (`P0-T09`) -> Blocked by outcome of #25 (`P0-T08`).
- #27 (`P0-T10`) -> Blocked by failure of both #25 and #26.

Suggested priorities for P0:

- Critical: #25, #26, #27 (networking feasibility critical path)
- High: #19, #20, #21, #22, #23, #24, #28, #29
- Normal: #18 (already completed scope task)

## Execution rule

For each implementation task:

1. Move exactly one task to `In Progress`.
2. Create a dedicated branch.
3. Give the coding agent that task only.
4. Require acceptance-criteria tests/verification.
5. Open a PR that references/closes the task.
6. CI must pass before merge.
7. Move to Done only after merge/verification.

Do not ask an agent to implement a whole phase or subsystem at once.
