# Entity identities

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

P7-T06 proves the identity boundary with the internal resolver. P7-T07 now defines schema-v1 scene persistence internally: entity `guid` and nullable `parentGuid` fields use canonical `EntityGuid` text, while runtime `EntityId` values never appear in the persisted document. The codec is still package-private and does not expose public world activation/loading.

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

There is still no public `World`, public entity allocator, public GUID-binding API, component store/query API, public scene codec/activation API, prefab lifecycle, or replication identity. The internal scene schema now records parent GUIDs, but runtime hierarchy activation remains later work.

## Ownership and threading

`EntityGuid` and `EntityId` own no native resources and require no cleanup. They are immutable Java values. Current internal allocation/GUID-resolution work adds no concurrency guarantee; later world owners define synchronization/update-phase rules.
