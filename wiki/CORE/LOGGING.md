# Structured logging

`EngineLogger` is the engine's synchronous structured logging boundary.

## Create a logger

The caller owns the sink:

```java
import com.samo.engine.core.api.EngineLogger;

EngineLogger logger = new EngineLogger(event -> {
    System.out.printf(
            "[%s] [%s] %s%n",
            event.level(),
            event.context().subsystem(),
            event.message());
});
```

The logger owns no background thread, persistence format, retry policy, or sink lifetime.

## Emit an event

```java
EngineLogger.Context context = new EngineLogger.Context(
        null,       // frame
        120L,       // simulation tick
        "gameplay",// subsystem
        null,       // connection
        "player-1" // entity
);

logger.log(
        EngineLogger.Level.INFO,
        "Player entered the room",
        context);
```

Available levels:

```text
DEBUG
INFO
WARN
ERROR
FATAL
```

The logger does not perform threshold filtering.

## Empty context

```java
logger.log(
        EngineLogger.Level.INFO,
        "Engine initialized",
        EngineLogger.Context.empty());
```

## Context fields

All context fields are optional. `null` means absent.

- `frame`
- `simulationTick`
- `subsystem`
- `connection`
- `entity`

Frame/tick values must be non-negative when present. Present strings are stripped and may not be blank.

## Event metadata

`EngineLogger` captures these automatically:

- timestamp;
- caller thread ID;
- caller thread name.

Sink callbacks are serialized across concurrent callers.

## Flush

```java
logger.flush();
```

`flush()` synchronously delegates to the caller-owned sink. The default `Sink.flush()` implementation does nothing unless your sink overrides it.
