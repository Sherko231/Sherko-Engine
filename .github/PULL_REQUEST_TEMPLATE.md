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
