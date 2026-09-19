# Phase 5R Naming and Refactor Standard

Status: P5R-T01 planning standard. This document defines how later Phase 5R Issues evaluate names and responsibility boundaries. It does not authorize a rename, move, split, API change, module-edge change, behavior change, or compatibility break by itself.

## Goals

- Make type and member names communicate the responsibility a reader should expect before opening the implementation.
- Prefer one clear responsibility per type over broad coordinator classes that silently accumulate unrelated work.
- Preserve already-clear names. Refactoring for vocabulary churn alone is explicitly rejected.
- Keep public contracts, native ownership, spatial semantics, persisted/config/wire formats, shader ABI, and accepted runtime behavior stable unless a later bounded Issue explicitly authorizes a change.
- Apply patterns only when they solve an observed responsibility, coupling, ownership, replaceability, or testability problem.

## Decision vocabulary

Every production/runtime type in the P5R inventory is classified with one of these actions:

- `KEEP`: the current name and responsibility are sufficiently clear; later work should not rename it without new evidence.
- `RENAME`: the responsibility is already bounded but the current name does not state it precisely enough.
- `DECOMPOSE`: the current type owns multiple independently meaningful responsibilities. The later Issue must preserve behavior while extracting named collaborators.
- `MOVE`: the responsibility is sound but its package/owner is misleading after stable boundaries are known.
- `REMOVE`: the symbol is obsolete or compatibility-only and may be deleted only after reference/evidence checks required by the later Issue.

A proposal in the inventory is not executable permission. The later active Issue remains the implementation contract.

## Type naming

### Classes

Use a concrete domain noun plus a responsibility-bearing role where the role materially improves understanding.

Preferred role words include:

- `Renderer`: consumes render-facing data and issues/render-composes visual work.
- `Controller`: owns a stateful policy or interaction flow with a clear controlled subject.
- `Coordinator`: orders multiple existing collaborators without becoming their implementation owner.
- `Loader`: acquires an external representation and returns a domain result.
- `Parser`: converts representation syntax into structured values; it does not silently own unrelated semantic policy.
- `Validator`: checks semantic constraints and reports/rejects violations.
- `Selector`: chooses a bounded subset from candidates.
- `Sorter`: establishes a documented ordering.
- `Factory`: creates configured instances when construction itself is a meaningful responsibility.
- `Descriptor`: immutable or effectively immutable declarative input/configuration data.
- `Snapshot`: immutable observation captured at a defined point in time.
- `Renderer`: renderer implementation or renderer-facing composition role, not arbitrary graphics data.
- `Policy`: explicit decision rules with little/no ownership of the operated resources.
- `Adapter`: translates between two existing boundaries/conventions without redefining either.
- `Facade`: intentionally stable entry surface delegating to narrower internal collaborators.

Do not add a suffix merely because it sounds architectural. A short domain name is preferred when the responsibility is already unambiguous, for example `Transform`, `Ray3f`, or `WindowMode`.

### Interfaces

Name interfaces by the capability/contract they expose. Avoid an `I` prefix. A backend/adapter interface may use a suffix such as `Backend` only when it is actually the replaceable boundary exercised by production/tests.

### Records

Name records for the value they represent. Use `Descriptor`, `Snapshot`, `State`, `Counters`, `Source`, or another value noun only when it matches the semantics. Do not use `Data` as a generic escape hatch.

### Enums

Name enums for the dimension being selected, not for implementation mechanics. Enum constants use stable screaming-snake-case names and must not be renamed when their persisted/config/wire meaning would change unless the active Issue explicitly authorizes migration/compatibility work.

## Method naming

- Use verbs for operations: `load`, `parse`, `validate`, `select`, `sort`, `render`, `write`, `sample`, `project`, `close`.
- A method name must reveal side effects that materially affect ownership/lifecycle. Prefer `startAndRollbackOnFailure`-style explicitness over a vague `process` if the shorter name hides the contract.
- Boolean queries use predicates such as `isVisible`, `hasPending...`, or `supports...`.
- Conversion methods state direction when ambiguity is possible.
- Do not use `handle`, `process`, `doWork`, or `update` as catch-all names when a more specific verb exists.
- Existing concise names remain valid when the enclosing type already supplies the missing context.

## Field and parameter naming

- Name values by domain meaning and units where unit ambiguity is realistic: `rangeMeters`, `innerConeRadians`, `framebufferWidth`.
- Use `source`/`destination`, `submitted`/`selected`, `previous`/`current` when direction or temporal meaning matters.
- Do not encode Java types in names.
- Avoid abbreviations except established domain terms such as GL, GLFW, CPU, GPU, RGB, sRGB, AABB, FOV, or ABI.
- Native handles should state the owned resource where possible; ownership must remain explicit in lifecycle code rather than being implied only by a name.

## Package naming

- Public consumer APIs remain under the accepted public package boundary unless a later Issue explicitly authorizes a public move.
- Internal packages group stable domain responsibilities, not temporary implementation chronology.
- A package move must not create/reverse a Gradle module edge, expose an internal implementation merely for convenience, or make lower engine modules depend on game-specific modules.
- Package names should remain lowercase and domain-oriented.
- P5R-T23 is the normal point for internal package reorganization after decompositions stabilize. Earlier tasks should move a type only when their active Issue requires it.

## Tests and fixtures

- Test class names identify the production contract or behavior under protection.
- Method names describe observable behavior/failure, not the implementation technique.
- Native tests keep `NativeTest` when native execution is the distinguishing acceptance boundary.
- Fixtures state whether they are consumer, invalid-input, architecture, compatibility, or evidence fixtures when that distinction matters.
- Historical CI/evidence identifiers used by automation are not renamed for cosmetic consistency.
- A production rename should update test vocabulary in P5R-T22 unless the implementing Issue must update a directly coupled test name immediately to keep the repository coherent.

## Facade, adapter, coordinator, controller, and policy rules

- **Facade**: use only for a deliberately stable entry surface that delegates. Phase 5R keeps `GlfwWindow` as the platform facade while extracting internal responsibilities.
- **Adapter**: use when translating an external/native/library boundary into an engine boundary. The adapter must not silently own unrelated domain policy.
- **Coordinator**: use for ordering multiple collaborators, including rollback/cleanup sequencing, when no one collaborator naturally owns that flow.
- **Controller**: use for stateful control of one subject, such as cursor/window-mode state, not as a synonym for manager.
- **Policy**: use for explicit deterministic rules; policy objects should not become resource owners by accident.

## Loader, parser, validator, selector, sorter, factory, descriptor, snapshot, renderer

- **Loader** acquires data from a path/resource/source and may coordinate parser/validator collaborators.
- **Parser** handles syntax/representation conversion.
- **Validator** handles semantic constraints and reports all behavior required by the accepted contract.
- **Selector** chooses candidates without implying sort order unless ordering is part of selection.
- **Sorter** defines ordering and should expose/encode the ordering contract.
- **Factory** centralizes non-trivial construction; do not replace ordinary constructors merely to introduce a pattern.
- **Descriptor** is declarative configuration/input, not a live native/resource owner.
- **Snapshot** is immutable state captured for a specific frame/tick/observation boundary.
- **Renderer** owns render work or renderer composition. Pure packing/selection/descriptor types should use their actual narrower role.

## Names to avoid

Discourage `Manager`, `Helper`, `Util`, and `Data` because they usually conceal responsibility. They are acceptable only when the term is genuinely the established domain noun and a more specific name would be less accurate.

Also avoid:

- generic `Processor` when the operation can be named;
- `Common` or `Shared` packages for unrelated responsibilities;
- suffixing every immutable value with `Dto`;
- naming an internal implementation `Service` when it is actually a loader, selector, renderer, adapter, or coordinator.

## Criteria for retaining an existing name

Choose `KEEP` when all of the following are true:

1. a reader can predict the type's primary responsibility from the name and package;
2. the implementation does not materially exceed that responsibility;
3. the name does not misstate ownership, lifecycle, units, spatial convention, or representation;
4. no accepted roadmap task requires a clearer role name;
5. renaming would mainly create churn across tests/wiki/consumers rather than reduce ambiguity.

A long class can still keep its public facade name while being `DECOMPOSE`d internally.

## Public API rename rules

Public API changes are higher risk than internal vocabulary cleanup.

- Prefer internal decomposition/renaming first.
- P5R-T21 is the deliberate public naming pass after internals stabilize.
- A public rename/signature/package change requires an active Issue that explicitly authorizes it.
- Update all consumers, tests, Javadocs, wiki/API index/limitations/examples, and sandbox usage in the same bounded change.
- Record compatibility intent; do not leave aliases by default. Compatibility shims require an explicit reason and removal plan.
- Preserve lifecycle, ownership, threading, failure, spatial, config, persisted, protocol, and rendering semantics.
- Public API or durable architecture changes require the independent review evidence required by `AGENTS.md`.

## Contract-sensitive naming

Names must not imply a semantic change:

- spatial names remain consistent with D-041/D-045/D-046/D-047;
- codec/quantization names do not alter byte layout or numeric bounds;
- config/input-binding names do not alter schema v1 or accepted failure behavior;
- OpenGL resource names do not alter ownership/thread affinity;
- uniform-block/presentation names do not alter GLSL block names, binding points, byte layout, or sRGB behavior;
- client/server report names do not alter compatibility output consumed by verification.

## No-broken-intermediate-state rule

Every later P5R Issue must leave the repository buildable, testable, runnable where applicable, behaviorally valid, and documentation-consistent when merged. A decomposition may use temporary scaffolding inside its branch only when the active Issue permits it; that scaffolding must be valid and fully reconciled before merge.

## How later Issues consume the inventory

1. Re-read the current source and active Issue; the inventory is a planning snapshot, not permanent truth.
2. Confirm the proposed action is still justified.
3. Refine exact extracted type names from observed responsibilities; do not blindly implement provisional names.
4. Preserve accepted behavior and contracts with focused tests.
5. Update the inventory/status if implementation evidence materially invalidates a proposal.

Wiki impact: none — this document defines internal Phase 5R planning policy and does not change a public API.

Sandbox impact: none — no runtime behavior or owner-facing control changes.
