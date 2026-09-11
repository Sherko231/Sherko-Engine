# Engineering References

These references are the source map used to construct the technical roadmap. They provide architecture/background; the repository's locked scope and task acceptance criteria remain authoritative for this project.

## 6. Source map

### Book

Jason Gregory, *Game Engine Architecture, Third Edition*:

- Runtime layering and dependency direction: Section 1.6.
- Tools and asset pipeline: Section 1.7.
- data/code/memory layout and hardware: Chapter 3.
- concurrency: Chapter 4, deliberately deferred here.
- 3D math: Chapter 5.
- subsystem lifecycle and core support: Chapter 6.
- files/resources: Chapter 7.
- fixed real-time game loop and timelines: Chapter 8.
- input devices: Chapter 9.
- development/debug tools: Chapter 10.
- rendering: Chapter 11.
- animation: Chapter 12.
- collision and rigid-body integration: Chapter 13.
- audio: Chapter 14.
- data-driven worlds, object models, events, and game flow: Chapters 15-16.

Java adaptation: the book primarily assumes C++. Its architectural boundaries remain useful, but custom allocators, pointer ownership, containers, and native lifecycle cannot be copied literally into a Java engine.

### Online primary/project sources

- LWJGL project: https://github.com/LWJGL/lwjgl3
- Oracle Java support roadmap: https://www.oracle.com/java/technologies/java-se-support-roadmap.html
- Gradle multi-project builds: https://docs.gradle.org/current/userguide/multi_project_builds.html
- JOML project: https://github.com/JOML-CI/JOML
- Jolt JNI project: https://github.com/stephengold/jolt-jni
- Jolt Physics architecture: https://jrouwe.github.io/JoltPhysics/
- Steamworks4j limitations: https://code-disaster.github.io/steamworks4j/
- Steam lobbies: https://partner.steamgames.com/doc/features/multiplayer/matchmaking
- SteamNetworkingSockets API: https://partner.steamgames.com/doc/api/ISteamNetworkingSockets
- Epic client/server and replication overview: https://dev.epicgames.com/documentation/en-us/unreal-engine/networking-overview-for-unreal-engine
- Fixed timestep: https://gafferongames.com/post/fix_your_timestep/
- Snapshot interpolation: https://gafferongames.com/post/snapshot_interpolation/
- imgui-java: https://github.com/SpaiR/imgui-java
- Recast4j: https://github.com/recast4j/recast4j
- Java packaging: https://docs.oracle.com/en/java/javase/25/docs/specs/man/jpackage.html

## Wiki synchronization

These references provide background rather than consumer API truth. If a future task uses or changes a referenced technology in a way that changes the implemented public engine API or how callers use it, update the relevant [`../wiki/`](../wiki/README.md) pages in the same PR. Otherwise record `Wiki impact: none — <reason>`.
