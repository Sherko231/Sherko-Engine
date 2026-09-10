## Roadmap task

**Task ID / maintenance Issue:** P?-T?? / #
**Closes:** #

## What changed

-

## Verification

```text
./gradlew ...
```

List the environment, result, skipped checks/reason, and evidence path where applicable. Configuration review is not execution evidence.

## Acceptance evidence

- [ ] Required tests/checks actually ran and passed.
- [ ] Acceptance criteria in the linked Issue pass.
- [ ] No undeclared modules/interfaces were changed.
- [ ] No unrelated future-roadmap work was implemented.
- [ ] Resource/native cleanup was verified where applicable.
- [ ] Branch is based on current `master`; no unrelated user work was overwritten.

## Review record

- Review kind: independent human / independent agent / self-review only / not performed.
- Reviewer identity or session and exact reviewed commit SHA:
- Coverage: Issue contract, scope/decisions, actual diff, tests, and relevant integration path.
- Findings: contract errors, out-of-scope design, unnecessary complexity, missing failure cases, or tests that could pass while requirements are broken.
- Disposition: fixed with evidence / unresolved with impact / no findings with coverage explained.
- Remaining limitations; if independent review was not performed, explain why and identify the risk.

A checkbox, an author's second pass, or green CI is not independent review evidence. After substantive edits, reassess affected findings and update the reviewed SHA. Honor any explicit review requirement in the Issue or repository settings.

## Phase integration / next-phase review

<!-- Fill for phase-exit or integration work; otherwise mark Not applicable with a reason. -->

- Existing backlog exit gate / acceptance link:
- Integrated scenario, participating systems, exact command or manual steps:
- Tested SHA/environment, observed result against existing thresholds, evidence paths:
- Skipped/unproven portions and blockers:
- At phase exit: next-phase assumptions/dependencies reviewed, necessary backlog refinements, and next bounded Issue candidate:

Passing isolated unit tests does not by itself establish the phase exit outcome. See `docs/BUILD_AND_VERIFY.md` for the evidence procedure.

## Handoff documents

Using the update matrix in `AGENTS.md`:

- [ ] `docs/DEVELOPMENT_STATUS.md` reflects completed work, next action, blockers, and verification where applicable.
- [ ] `docs/ARCHITECTURE.md` reflects module/dependency/maturity changes where applicable.
- [ ] `docs/DECISIONS.md` records durable decisions or supersessions where applicable.
- [ ] `docs/BUILD_AND_VERIFY.md` reflects command/CI/evidence changes where applicable.
- [ ] Scope/roadmap/backlog/feasibility evidence was reconciled where applicable.
- [ ] Every non-applicable document was reviewed and intentionally left unchanged.

## Architecture check

If this PR introduced an architectural decision that was not explicitly permitted by the Issue, explain it here and do not merge until the Issue and decision/scope documents authorize it.
