# Sherko Engine Wiki

This directory is the human- and AI-readable usage guide for the **implemented** Sherko Engine API and accepted consumer-facing engine contracts.

Use it when you want to answer questions such as:

- How do I create and shut down an engine subsystem?
- How do I order and start multiple dependent subsystems?
- How do I create a production GLFW/OpenGL window?
- How do I read one stable renderer-frame keyboard/mouse snapshot without calling GLFW?
- How do I build local/world transforms with the canonical world convention?
- What world axes and units must future spatial APIs use?
- How do I log structured engine events?
- How do I track native resources safely?
- How does the fixed-step timing foundation work?
- How does orderly fatal shutdown work?
- Which APIs are production-ready today, and which features are still planned?

## Start here

1. [Getting started](GETTING_STARTED.md)
2. [Current API index](API_INDEX.md)
3. [Lifecycle and subsystem ownership](CORE/LIFECYCLE.md)
4. [Subsystem composition and startup](CORE/SUBSYSTEM_COMPOSITION.md)
5. [Timing and fixed-step simulation](CORE/TIMING.md)
6. [Configuration](CORE/CONFIGURATION.md)
7. [Spatial conventions](CORE/SPATIAL_CONVENTIONS.md)
8. [Transforms](CORE/TRANSFORMS.md)
9. [Structured logging](CORE/LOGGING.md)
10. [Native resource ownership](CORE/NATIVE_RESOURCES.md)
11. [Fatal termination](CORE/FATAL_TERMINATION.md)
12. [GLFW/OpenGL window](PLATFORM/GLFW_WINDOW.md)
13. [Renderer-frame input snapshots](PLATFORM/INPUT.md)
14. [Create a window example](EXAMPLES/CREATE_A_WINDOW.md)
15. [Current limitations](LIMITATIONS.md)
16. [How this wiki must be maintained](MAINTENANCE.md)

## What this wiki is

The wiki explains **how to consume the engine as a library** and how to interpret accepted consumer-facing contracts. It should favor practical examples, public API names, ownership/lifecycle rules, failure behavior, current limitations, and references to normative architecture documents where no runtime API exists yet.

## What this wiki is not

The wiki is not the source of truth for product scope, architecture decisions, implementation status, or executable task authorization.

When information conflicts, follow the repository truth hierarchy in [`../AGENTS.md`](../AGENTS.md): scope, accepted decisions, active Issue, code/tests/evidence, development status, live GitHub state, then roadmap/backlog. The wiki must be corrected to match those higher-authority sources; it must never override them.

## Stability rule

Only document APIs and behavior that exist in production source on the current repository state. Accepted architecture contracts may be summarized only when they are clearly linked to their normative source and not presented as an implemented Java API. Planned roadmap APIs may be mentioned only as unavailable/future functionality and must never be shown as usable code.
