---
name: Engine roadmap task
about: Implement one executable Sherko Engine roadmap task
title: "[P?-T??] "
labels: ""
assignees: ""
---

## Roadmap task

**Task ID:** P?-T??
**Phase:** Phase ?
**Technical backlog link:** <!-- link to the task in docs/roadmap/TECHNICAL_BACKLOG.md -->

## Goal

<!-- One bounded outcome. Do not paste an entire phase here. -->

## Acceptance criteria

- [ ]

## API / architecture contract

<!-- Required for public API or durable architecture work. Otherwise write Not applicable and why. -->

- Realistic usage example: caller, inputs, operation sequence, observable expected result.
- Failure behavior: invalid use, partial failure, ownership/cleanup where relevant.
- Current need: why this design is needed now; simpler alternative considered.

## Dependencies / blockers

- None / #

## Verification

```text
./gradlew ...
```

For ordinary non-Markdown tasks, follow the CI lifecycle in `AGENTS.md` / `docs/BUILD_AND_VERIFY.md`: implement, verify, document, and self-review on the dedicated branch before opening the final non-draft PR; require the heavy five-job CI on that exact final candidate; after merge require the lightweight exact-merge master verifier. Request a second full post-merge matrix only when this Issue explicitly needs exact-merge native/performance/integration evidence that the lightweight verifier cannot establish.

## Handoff/document impact

<!-- Select every applicable document using the matrix in AGENTS.md; write None only after checking. -->

- [ ] `docs/DEVELOPMENT_STATUS.md`
- [ ] `docs/ARCHITECTURE.md`
- [ ] `docs/DECISIONS.md`
- [ ] `docs/BUILD_AND_VERIFY.md`
- [ ] Scope/roadmap/backlog/evidence document
- [ ] `wiki/` API/usage guide — required when public API or consumer-visible usage changes
- [ ] Wiki impact: none — reason recorded because no consumer/API usage changed
- [ ] `game-sandbox` owner-facing demo/README — update when the capability is meaningfully observable through authorized public production APIs
- [ ] Sandbox impact: none — reason recorded when a demo would require internals or future roadmap work
- [ ] None — no durable repository context changes

<!-- Add the optional sections below only when they materially reduce ambiguity or architectural risk. -->

## Optional: Required tests

<!-- For changed behavior, map each behavior/test family to an acceptance requirement,
     a realistic fault it should catch, and an independently derived expected result.
     Do not repeat the implementation algorithm as the expected-value calculation. -->

- [ ]

## Optional: Phase integration evidence

<!-- For an integration or phase-exit task, link the existing backlog gate and specify
     participating systems, exact scenario/command, unchanged acceptance thresholds,
     environment and evidence paths. Do not invent commands for unimplemented work.
     At phase exit, record the next-phase review described in BUILD_AND_VERIFY.md. -->

## Optional: Non-goals

-

## Optional: Allowed scope / architecture constraints

### Modules / files allowed to change

-

### Interfaces allowed to change

-

### Interfaces that must not change

-

## Optional: Stop condition

Stop implementation and report the architectural conflict if completing this task requires changing an interface, module boundary, protocol/layout, product-scope decision, dependency, or durable decision that is not explicitly allowed by this Issue.
