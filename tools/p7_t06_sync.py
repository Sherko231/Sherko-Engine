from pathlib import Path


def read(path: str) -> str:
    return Path(path).read_text(encoding="utf-8")


def write(path: str, text: str) -> None:
    Path(path).write_text(text.rstrip() + "\n", encoding="utf-8")


def replace_once(path: str, old: str, new: str) -> None:
    text = read(path)
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{path}: expected one match, found {count}: {old[:100]!r}")
    write(path, text.replace(old, new, 1))


def replace_line_once(path: str, prefix: str, replacement: str) -> None:
    text = read(path)
    lines = text.splitlines()
    matches = [index for index, line in enumerate(lines) if line.startswith(prefix)]
    if len(matches) != 1:
        raise RuntimeError(f"{path}: expected one line starting {prefix!r}, found {len(matches)}")
    lines[matches[0]] = replacement
    write(path, "\n".join(lines))


p7_t05_evidence = (
    "P7-T05 / Issue #422 is accepted through PR #423: final candidate "
    "`4ad2a804c18920212dea6b76f033ef2dd876411a` passed the required five-job matrix in run #651 / "
    "`37004765576`, merged as `8275f5e86647b2629d3cb1b4d3ae973a647043ca`, and exact merged master passed "
    "Lightweight verification in run #652 / `37005330891`."
)

replace_once(
    "README.md",
    "P7-T05 / Issue #422 is the current bounded task and adds only the five public data-oriented world components plus the API metadata needed by their existing `Transform` / `AssetId` signatures; P7-T06 and later Phase 7 work remain separately materialized tasks.",
    p7_t05_evidence
    + " P7-T06 / Issue #424 is the current bounded task for stable `EntityGuid` authoring identity plus internal GUID-to-runtime resolution; P7-T07 scene JSON/schema work and later Phase 7 tasks remain separate.",
)

replace_once(
    "ROADMAP.md",
    "P7-T05 / Issue #422 is the current bounded task for `TransformComponent`, `NameComponent`, `MeshRendererComponent`, `CameraComponent`, and `AudioEmitterComponent`; public world lifecycle, scene JSON/GUID work, renderer/audio bridges, prefabs/scenes, and later Phase 7 work remain separately materialized tasks.",
    p7_t05_evidence
    + " P7-T06 / Issue #424 is the current bounded task separating stable authoring `EntityGuid` values from transient runtime `EntityId` handles; P7-T07 still owns scene JSON/schema/parser behavior, and later Phase 7 work remains separately materialized.",
)

replace_line_once(
    "docs/DEVELOPMENT_STATUS.md",
    "| Active phase |",
    "| Active phase | Phase 7 active — P7-T01 / #414 through P7-T05 / #422 are accepted; P7-T06 / Issue #424 is the current bounded task; Phase 6 remains complete |",
)
replace_line_once(
    "docs/DEVELOPMENT_STATUS.md",
    "| Active executable task |",
    "| Active executable task | P7-T06 / Issue #424 — stable public authoring `EntityGuid` plus package-private live GUID-to-`EntityId` resolution; implementation is on `p7-t06-authoring-entity-guids`; final PR/CI is not yet accepted |",
)

status = read("docs/DEVELOPMENT_STATUS.md")
marker = "## Phase 7 core data-driven world components — P7-T05 / Issue #422\n"
if status.count(marker) != 1:
    raise RuntimeError("docs/DEVELOPMENT_STATUS.md: expected one P7-T05 section marker")
prefix, old_tail = status.split(marker, 1)
if "\n## " in old_tail:
    raise RuntimeError("docs/DEVELOPMENT_STATUS.md: P7-T05 is no longer the final section; refuse tail replacement")
if "Do not materialize P7-T06 before P7-T05 is accepted." not in old_tail:
    raise RuntimeError("docs/DEVELOPMENT_STATUS.md: expected stale P7-T05 next-action guard missing")
new_tail = """## Phase 7 core data-driven world components — P7-T05 / Issue #422

P7-T05 is accepted. Final candidate `4ad2a804c18920212dea6b76f033ef2dd876411a` passed all five required PR jobs in run #651 / `37004765576`; PR #423 merged as `8275f5e86647b2629d3cb1b4d3ae973a647043ca`, and exact merged `master` passed Lightweight verification in run #652 / `37005330891`. Issue #422 is closed completed.

The accepted public `engine-world` surface adds exactly `TransformComponent`, `NameComponent`, `MeshRendererComponent`, `CameraComponent`, and `AudioEmitterComponent`. Transform state reuses the canonical `engine-core Transform`; mesh/material/audio references retain only stable `AssetId` values; camera stores projection configuration while pose comes from transform and aspect remains runtime input. D-083 records the policy. There is still no public `World`, scene schema/parser, GUID persistence contract, renderer/audio bridge, or later Phase 7 behavior in P7-T05.

## Phase 7 stable authoring entity GUIDs — P7-T06 / Issue #424

P7-T06 is active from accepted baseline `8275f5e86647b2629d3cb1b4d3ae973a647043ca` on branch `p7-t06-authoring-entity-guids`.

The bounded implementation adds public immutable `EntityGuid(highBits,lowBits)` with JDK-backed generation plus strict canonical lowercase UUID text parsing/formatting. Existing `EntityId(index,generation)` remains the transient runtime handle and its value/reuse semantics are unchanged. A package-private `EntityGuidIndex` owns one-to-one GUID/current-live-`EntityId` bindings against one `EntityIdAllocator`, rejects conflicting bindings, invalidates dead runtime mappings, and never transfers a GUID to a reused slot generation.

Focused tests cover GUID text round trips and malformed input, live forward/reverse binding, stale/unknown runtime IDs, duplicate GUID/entity rejection, destruction/reuse safety, stale unbind safety, null misuse, and a save-like/reload scenario where the stored cross-entity reference is GUID text and resolves to a different runtime ID after reload. Branch preflight run #1 / `37007769079` passed Spotless, `engine-world` tests, and the architecture boundary test on the formatter-applied tree; the formatter result is committed on the task branch. Final candidate verification is still pending.

P7-T06 deliberately adds no scene JSON/schema/parser, Jackson usage, parent GUID field, public world/entity lifecycle, prefab behavior, renderer/audio/physics/network integration, replication identity, dependency/module edge, or lockfile change. P7-T07 retains ownership of the first versioned scene JSON contract.

Wiki impact: yes — the public identity guidance must distinguish stable authoring `EntityGuid` from transient runtime `EntityId` and must not claim scene JSON is implemented.

Sandbox impact: none — public GUID values alone are not an honest runnable save/reload capability because GUID binding/resolution and entity lifecycle remain internal.

Independent review: not performed in this connected session; no independent reviewer/provenance is currently available. Remaining risk is limited to author self-review plus automated verification until an independent reviewer inspects the final diff.

Exact next action: finish P7-T06 / Issue #424 only, complete documentation/wiki/identity-domain audits and full branch verification, remove temporary authoring tooling, open one final non-draft PR, require the five-job exact-head matrix, merge only while tested head/base remain current, then require exact-merge Lightweight verification before closing #424. Do not materialize P7-T07 before P7-T06 is accepted.
"""
write("docs/DEVELOPMENT_STATUS.md", prefix + new_tail)

architecture = read("docs/ARCHITECTURE.md")
heading = "## Phase 7 stable authoring entity GUIDs — P7-T06 / Issue #424"
if heading in architecture:
    raise RuntimeError("docs/ARCHITECTURE.md: P7-T06 section already exists")
architecture_addition = """

## Phase 7 stable authoring entity GUIDs — P7-T06 / Issue #424

`engine-world` exposes immutable `com.samo.engine.world.api.EntityGuid` as the stable 128-bit identity of one authored logical entity. Its two `long` components are the complete value; `generate()` uses the JDK UUID generator, `parse(String)` accepts only canonical lowercase 8-4-4-4-12 hexadecimal UUID text, and `toString()` emits that canonical form. All bit patterns are valid and there is no sentinel GUID.

`EntityGuid` and `EntityId` have deliberately different lifetimes. `EntityGuid` is stable authoring/persistence identity. `EntityId(index,generation)` remains a transient runtime handle whose slot may be reused only under P7-T01 generation safety; it is not persisted and is not network/replication identity.

Package-private `EntityGuidIndex` is the bounded runtime resolution boundary for the current internal world foundation. One index is tied to one `EntityIdAllocator` and keeps a one-to-one mapping between stable GUIDs and currently live exact runtime IDs. Binding requires a live ID; duplicate GUID or runtime-ID bindings fail without replacing the existing pair. Resolve/reverse lookup validate allocator liveness, dead mappings are removed safely, and exact stale unbind cannot remove a replacement generation's binding.

The acceptance test uses an in-memory save-like record containing canonical GUID text only. It destroys the first runtime entities, reloads them in an order that changes their generational runtime IDs, rebinds the parsed stable GUIDs, and proves the saved cross-entity GUID resolves to the new target runtime ID rather than the old handle. This establishes the P7-T06 identity boundary without defining a production persistence document.

D-084 records the durable identity policy. P7-T07 still owns schema versioning, scene JSON fields, entity/parent GUID representation, component objects, parsing, saving, and semantic load-save-load behavior. P7-T06 adds no public `World`, allocator/store/query API, prefab behavior, replication identity, renderer/audio/physics integration, project edge, production dependency, or lockfile change.

Wiki impact: yes — public identity guidance distinguishes stable authored `EntityGuid` values from transient runtime `EntityId` values.
Sandbox impact: none — there is no public entity lifecycle or public GUID binding/resolution boundary to exercise without pulling P7-T07+ work forward.
"""
write("docs/ARCHITECTURE.md", architecture + architecture_addition)

decisions = read("docs/DECISIONS.md")
anchor = "\nAn Issue must explicitly authorize a durable architecture change."
if decisions.count(anchor) != 1:
    raise RuntimeError("docs/DECISIONS.md: decision footer anchor is not unique")
d084 = (
    "\n| D-084 | Accepted | P7-T06 / Issue #424 separates stable authored entity identity from transient runtime identity. Public immutable `engine-world` `EntityGuid(highBits,lowBits)` is the stable 128-bit authoring/persistence value with JDK UUID generation and strict canonical lowercase UUID text. `EntityId(index,generation)` remains runtime-only. Package-private `EntityGuidIndex` maps one stable GUID to one currently live exact `EntityId` for one allocator context, rejects conflicting bindings, removes dead mappings safely, and never transfers identity across slot reuse. Authored cross-entity references retain GUIDs and resolve them to current runtime IDs at load/runtime binding boundaries. P7-T06 defines no scene JSON fields/schema/version, parent representation, public `World`, or replication/network identity. | Persisting generational runtime handles would couple authored references to allocator history and slot reuse. A typed stable GUID prevents identity-domain mixups and gives later P7-T07 scene loading a deterministic resolution key without freezing its JSON schema or a public world lifecycle prematurely. |\n"
)
write("docs/DECISIONS.md", decisions.replace(anchor, d084 + anchor, 1))

replace_once(
    "wiki/API_INDEX.md",
    "| `EntityId` | Immutable generational world-entity identity containing a non-negative reusable index plus non-negative generation. |",
    "| `EntityId` | Immutable transient runtime world-entity handle containing a non-negative reusable index plus non-negative generation; never persist it. |\n| `EntityGuid` | Stable 128-bit authoring/persistence identity with canonical lowercase UUID text, deliberately separate from runtime `EntityId`. |",
)
replace_once(
    "wiki/API_INDEX.md",
    "There is still no public `World`, entity allocator, component store/query API, deferred structural-command API, scene JSON/schema/parser, prefab lifecycle, GUID/persistence contract, renderer extraction bridge, audio playback bridge, replication identity, or entity serialization contract. P7-T07 still owns the first planned scene JSON format.",
    "There is still no public `World`, entity allocator, component store/query API, deferred structural-command API, scene JSON/schema/parser, prefab lifecycle, public GUID-binding/resolution API, renderer extraction bridge, audio playback bridge, replication identity, or entity serialization format. `EntityGuid` now supplies stable authoring identity and canonical text only; P7-T07 still owns the first planned scene JSON format and parent/component representation.",
)

replace_line_once(
    "wiki/LIMITATIONS.md",
    "`engine-world` exposes P7-T01 immutable `EntityId(index,generation)` values plus the five P7-T05 public data-oriented components.",
    "`engine-world` exposes P7-T01 immutable transient `EntityId(index,generation)` runtime handles, P7-T06 stable public `EntityGuid` authoring identities, plus the five P7-T05 public data-oriented components. `EntityGuid` provides canonical stable identity text but not a scene serialization format or public binding lifecycle. Allocation, destruction, liveness checks, GUID-to-runtime resolution, index reuse bookkeeping, and generation retirement remain package-private; there is still no public `World`, entity allocator, component store/query API, structural-command API, scene JSON/schema/parser, parent-GUID/component persistence format, prefab/scene entity lifecycle, world-to-renderer extraction/submission, runtime audio loading/playback bridge, replication/network identity, or concurrency contract.",
)

entity_ids = """# Entity identities

`engine-world` now exposes two deliberately different public identity values:

- `com.samo.engine.world.api.EntityGuid` — stable authoring/persistence identity for one logical entity;
- `com.samo.engine.world.api.EntityId` — transient runtime handle for one currently allocated entity slot/generation.

Do not substitute one for the other.

## Stable `EntityGuid`

An `EntityGuid` is an immutable 128-bit value represented by `highBits` and `lowBits`. The complete bit pair is the identity.

```java
EntityGuid guid = EntityGuid.generate();
String savedText = guid.toString();
EntityGuid loaded = EntityGuid.parse(savedText);
```

`toString()` uses canonical lowercase UUID text in `8-4-4-4-12` hexadecimal form. `parse(String)` accepts only that exact lowercase canonical form. Null input throws `NullPointerException`; malformed, uppercase, truncated, or otherwise non-canonical input throws `IllegalArgumentException`.

All 128-bit values are valid; there is no reserved zero/sentinel GUID. `generate()` uses the JDK UUID generator for newly-authored identities.

An authored cross-entity reference should retain the target `EntityGuid`, not a runtime `EntityId`. During loading/runtime binding, the world layer resolves that stable GUID to whatever runtime ID represents the entity in the current world instance.

P7-T06 proves this boundary with an internal resolver and save-like in-memory test data. It does **not** define the production scene JSON representation. P7-T07 owns schema versioning, entity/parent GUID fields, component objects, parsing, and save/load document behavior.

## Transient `EntityId`

An `EntityId` contains two non-negative integers:

- `index` — the reusable runtime slot index;
- `generation` — the version of that slot.

Both values are part of the runtime handle. Do not keep or compare only the index.

```java
EntityId id = new EntityId(12, 3);
int index = id.index();
int generation = id.generation();
```

Constructing an ID with a negative index or generation fails with `IllegalArgumentException`.

The internal allocator may reuse an index after its current entity is destroyed. Any later reuse returns that index with the previous generation plus one. Therefore an old `(index, generation)` pair does not become valid merely because the same index is occupied again.

The generation never wraps. When an internal slot reaches `Integer.MAX_VALUE` and is destroyed, that index is retired permanently rather than returning to generation zero and risking stale-ID resurrection.

Never persist or network an `EntityId`. Save-like authored references use `EntityGuid`; future replication/network identity remains a separate later contract.

## Current resolution boundary

P7-T06 keeps GUID-to-runtime resolution internal. The package-private resolver binds each stable GUID to one currently live exact `EntityId`, rejects conflicting GUID/entity bindings, validates allocator liveness, and removes dead mappings so index reuse cannot inherit an old entity's authored identity.

There is still no public `World`, public entity allocator, public GUID-binding API, component store/query API, scene JSON/schema/parser, prefab lifecycle, parent GUID contract, or replication identity.

## Ownership and threading

`EntityGuid` and `EntityId` own no native resources and require no cleanup. They are immutable Java values. Current internal allocation/GUID-resolution work adds no concurrency guarantee; later world owners define synchronization/update-phase rules.
"""
write("wiki/WORLD/ENTITY_IDS.md", entity_ids)
