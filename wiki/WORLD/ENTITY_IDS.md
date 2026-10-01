# Entity identities

`com.samo.engine.world.api.EntityId` is the public immutable identity value for Phase 7 world entities.

## Value shape

An `EntityId` contains two non-negative integers:

- `index` — the reusable slot index;
- `generation` — the version of that slot.

Both values are part of the identity. Do not keep or compare only the index.

```java
EntityId id = new EntityId(12, 3);
int index = id.index();
int generation = id.generation();
```

Constructing an ID with a negative index or generation fails with `IllegalArgumentException`.

## Stale-ID rule

The engine-world allocator may reuse an index after its current entity is destroyed. Any later reuse returns that index with the previous generation plus one. Therefore an old `(index, generation)` pair does not become valid merely because the same index is occupied again.

The generation never wraps. When an internal slot reaches `Integer.MAX_VALUE` and is destroyed, that index is retired permanently rather than returning to generation zero and risking stale-ID resurrection.

## Current public surface

P7-T01 intentionally exposes only the immutable `EntityId` value. Entity allocation, destruction, and liveness bookkeeping remain internal to `engine-world` for now.

There is currently no public `World`, public entity allocator, component store, deferred structural-command API, prefab/scene entity lifecycle, replication identity, or entity serialization contract. Those remain later bounded Phase 7/network tasks.

Do not invent entity IDs by index alone in gameplay code or treat `EntityId` as a network or persisted identifier. Later APIs that create/own entities will return and validate these values.

## Ownership and threading

`EntityId` owns no native resources and requires no cleanup. It is an immutable Java value. P7-T01 introduces no concurrency guarantee for internal world allocation; later world owners define their own synchronization/update-phase rules.
