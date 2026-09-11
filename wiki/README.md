# Sherko Engine Wiki

This directory is the human- and AI-readable usage guide for the **implemented** Sherko Engine API.

Use it when you want to answer questions such as:

- How do I create and shut down an engine subsystem?
- How do I order and start multiple dependent subsystems?
- How do I create a production GLFW/OpenGL window?
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
7. [Structured logging](CORE/LOGGING.md)
8. [Native resource ownership](CORE/NATIVE_RESOURCES.md)
9. [Fatal termination](CORE/FATAL_TERMINATION.md)
10. [GLFW/OpenGL window](PLATFORM/GLFW_WINDOW.md)
11. [Create a window example](EXAMPLES/CREATE_A_WINDOW.md)
12. [Current limitations](LIMITATIONS.md)
13. [How this wiki must be maintained](MAINTENANCE.md)

## What this wiki is

The wiki explains **how to consume the engine as a library**. It should favor practical examples, public API names, ownership/lifecycle rules, failure behavior, and current limitations.

## What this wiki is not

The wiki is not the source of truth for product scope, architecture decisions, implementation status, or executable task authorization.

When information conflicts, follow the repository truth hierarchy in [`../AGENTS.md`](../AGENTS.md): scope, accepted decisions, active Issue, code/tests/evidence, development status, live GitHub state, then roadmap/backlog. The wiki must be corrected to match those higher-authority sources; it must never override them.

## Stability rule

Only document APIs and behavior that exist in production source on the current repository state. Planned roadmap APIs may be mentioned only as unavailable/future functionality and must never be shown as usable code.
