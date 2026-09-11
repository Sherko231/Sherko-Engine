# Wiki maintenance contract

The `wiki/` directory is the in-repository **engine consumer/API guide**. It must evolve with production engine usage.

## When an implementation task must update the wiki

A task must update the relevant wiki page(s) in the same PR when it:

- adds a public engine API;
- removes or renames a public engine API;
- changes a public method/constructor signature;
- changes lifecycle ordering, ownership, cleanup, thread-affinity, or failure semantics visible to callers;
- changes configuration keys/defaults/ranges or how configuration is consumed;
- changes how a consumer initializes, runs, stops, closes, or composes an engine feature;
- turns previously unavailable roadmap behavior into a usable production capability;
- invalidates an existing wiki example or limitation statement.

## When a wiki edit is not required

If a task has no user-facing/public-API usage impact, it may record:

```text
Wiki impact: none — <reason>
```

Examples include an internal refactor with identical public behavior, historical evidence-only edits, or a test-only change that does not alter the engine consumer contract.

Do not make meaningless wiki churn merely to satisfy a checkbox.

## Required content quality

When a page is updated:

1. describe only behavior implemented in production source;
2. use current public type/method names;
3. include a practical example when it materially helps usage;
4. state ownership/lifecycle/threading/failure constraints that a caller could get wrong;
5. state important current limitations instead of implying future roadmap functionality already exists;
6. keep links relative and valid;
7. reconcile the API index when public surface area changes.

## Authority rule

The wiki explains usage; it does not authorize implementation or redefine architecture.

If the wiki conflicts with repository authority, correct the wiki. Use the truth hierarchy in [`../AGENTS.md`](../AGENTS.md):

1. `ENGINE_SCOPE.md`;
2. accepted `docs/DECISIONS.md`;
3. active executable Issue;
4. code/tests/evidence;
5. `docs/DEVELOPMENT_STATUS.md`;
6. live GitHub workflow state;
7. roadmap/backlog.

## Agent handoff check

Before an AI agent finishes a task, it must explicitly answer:

```text
Wiki impact: updated <pages>
```

or

```text
Wiki impact: none — <reason>
```

For public API or consumer-behavior changes, `none` is invalid unless the agent can demonstrate that the existing wiki already describes the new contract exactly.
