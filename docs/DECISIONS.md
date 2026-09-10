# Sherko Engine Decision Log

This is the durable, append-only summary of accepted engineering decisions. It records why the repository is shaped this way without mixing in live task status. New entries receive the next ID; superseded entries remain and link to the replacement.

## Status values

- `Accepted` — binding unless deliberately superseded.
- `Provisional` — usable for current work but requires a named validation gate.
- `Superseded` — historical; a later decision controls.

## Decisions

| ID | Status | Decision | Rationale / consequence |
| --- | --- | --- | --- |
| D-001 | Accepted | Authored engine and game runtime source is Java 25 only; Gradle Kotlin DSL is allowed for build configuration. | Preserves the Java-only product constraint without a no-value build-script migration. |
| D-002 | Accepted | Windows x64 and Steam are the initial release platform/social layer. | Keeps native verification and packaging bounded for v1. |
| D-003 | Accepted | OpenGL 4.6 Core with a forward renderer is the first renderer. | Smaller v1 implementation surface than Vulkan; multiple backends remain deferred. |
| D-004 | Accepted | Jolt through Jolt JNI supplies rigid-body physics; native objects use explicit ownership/cleanup. | Avoids building a solver and prevents treating GC as native-resource management. |
| D-005 | Accepted | Multiplayer uses server-authoritative simulation, not deterministic physics lockstep. | Physics-heavy shared objects are corrected/replicated from the server; clients do not own arbitrary authoritative transforms. |
| D-006 | Accepted | Production networking uses transport-neutral APIs with direct-IP and Steam adapters; no home-grown reliable UDP transport. | Keeps gameplay independent of transport and limits custom protocol risk. |
| D-007 | Provisional | Java FFM may call the Steam flat API without authored C/C++ glue. | API/pointer access is proven; P0-T09A / Issue #42 must prove end-to-end connection, callbacks, message release, and cleanup before production use. |
| D-008 | Accepted | Runtime game UI is an engine-owned retained, renderer-neutral model; imgui-java is editor/debug only. | Prevents shipped UI/game state from coupling to OpenGL or internal tooling and includes keyboard/controller navigation. |
| D-009 | Accepted | glTF 2.0 is authoring interchange; gameplay loads cooked engine formats. | Separates flexible import from bounded/versioned runtime data. |
| D-010 | Accepted | Disk/network formats use explicit versioned codecs; Java object serialization is forbidden. | Provides bounded compatibility and avoids unsafe, implicit layouts. |
| D-011 | Accepted | v1 gameplay is first-person; third-person presentation follows a stable multiplayer vertical slice. | Core player/authority contracts stay camera-agnostic without blocking the first playable loop. |
| D-012 | Accepted | Target is 1–4 supported players, with intended co-op sessions of 2–4, in small/medium complete-scene levels. | Bounds replication, hosting, content, and performance work; open-world streaming is deferred. |
| D-013 | Accepted | Agent-authored changes use one Issue, one dedicated branch, one pull request, and CI before merge. | Makes handoff state reviewable and prevents undocumented direct changes to `master`. |
| D-014 | Accepted | Commit-contained status and live workflow status are separate. | `docs/DEVELOPMENT_STATUS.md` describes its containing commit; GitHub Issues/Project describes activity after that commit. |
| D-015 | Accepted | The 15-second P0-T12 combined run is a smoke test, not a soak test. | P0-T13 / Issue #43 owns the 15-minute sustained run; P0-T14 / Issue #44 owns repeated lifecycle evidence. |

## Adding or changing a decision

An Issue must explicitly authorize a durable architecture change. Add a new row with its evidence/gate, update `docs/ARCHITECTURE.md` where structure changes, and reconcile `ENGINE_SCOPE.md` when the product boundary changes. Never rewrite an old decision to hide history; mark it `Superseded` and add the replacement.
