# Subsystem composition and startup

`SubsystemGraph` and `SubsystemStartupCoordinator` help a composition owner start multiple `EngineSubsystem` instances in dependency-safe order without turning `engine-core` into a general dependency-injection or lifecycle framework.

## Declare dependencies

Each subsystem is registered with an exact case-sensitive ID, the subsystem instance, and the IDs of its prerequisites:

```java
import com.samo.engine.core.api.EngineSubsystem;
import com.samo.engine.core.api.SubsystemGraph;
import java.util.List;

SubsystemGraph graph = new SubsystemGraph(List.of(
        new SubsystemGraph.Registration(
                "platform",
                platformSubsystem,
                List.of()),
        new SubsystemGraph.Registration(
                "renderer",
                rendererSubsystem,
                List.of("platform"))));

List<EngineSubsystem> order = graph.initializationOrder();
```

For this example, `platformSubsystem` appears before `rendererSubsystem`.

## Validation performed by `SubsystemGraph`

Construction rejects:

- duplicate subsystem IDs;
- the same subsystem instance registered under multiple IDs;
- missing dependency IDs;
- invalid registration input.

`initializationOrder()` rejects dependency cycles and returns no partial order. The cycle diagnostic contains the closed path, for example:

```text
Subsystem dependency cycle: A -> B -> C -> A
```

The graph itself never calls lifecycle methods, owns resources, or performs cleanup.

## Start the resolved order

```java
import com.samo.engine.core.api.SubsystemStartupCoordinator;

SubsystemStartupCoordinator.start(order);
```

The utility snapshots the order, then performs:

```text
subsystem 1: initialize -> start
subsystem 2: initialize -> start
...
```

## What happens on startup failure

If a subsystem fails during `initialize()` or `start()`:

1. the original unchecked failure remains primary;
2. the currently failing subsystem receives a `close()` attempt;
3. previously started subsystems are visited in reverse order;
4. each receives `stop()` and then `close()`;
5. rollback failures are added as suppressed exceptions to the original failure;
6. cleanup continues even when a rollback action fails.

## Successful startup ownership

`SubsystemStartupCoordinator.start(...)` does **not** become the normal shutdown owner. After successful startup, the composition owner still owns orderly reverse shutdown.

A typical owner should retain the dependency-first list and, during normal shutdown, visit it in reverse order and call `stop()` then `close()` according to each subsystem's lifecycle state/contract.

## Important limits

- No dependency injection/service locator is provided.
- No normal-shutdown manager is provided.
- No restart contract is implied.
- The graph is structurally immutable, but referenced subsystem objects are not made immutable.
- Native thread-affinity rules still belong to the concrete subsystem.

See [Lifecycle and subsystem ownership](LIFECYCLE.md).
