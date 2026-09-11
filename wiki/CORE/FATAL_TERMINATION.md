# Fatal termination

`FatalTermination` coordinates one synchronous orderly fatal shutdown before process termination.

## Create a coordinator

```java
import com.samo.engine.core.api.FatalTermination;

FatalTermination fatalTermination = new FatalTermination(logger);
```

The public production constructor uses process exit status `1`.

## Invoke fatal termination

```java
fatalTermination.terminate(
        "Fatal engine failure",
        EngineLogger.Context.empty(),
        initializationOrder,
        nativeResources);
```

`initializationOrder` must be the dependency-first list of subsystems that successfully completed startup. Invoke the coordinator on the lifecycle/native-affinity thread that owns those subsystems and the supplied resource registry.

## Order of operations

A claimed fatal termination performs these steps synchronously:

1. emit one `FATAL` structured log event;
2. visit supplied subsystems in strict reverse order;
3. attempt `stop()` then `close()` for each subsystem;
4. call `NativeResourceRegistry.assertNoOpenResources()` once;
5. emit best-effort `ERROR` events for cleanup/verification failures collected before reporting;
6. flush the logger once;
7. terminate the process with status `1`.

Failures from logging, cleanup, registry verification, or flush are captured so later cleanup/termination is still attempted.

## One-shot contract

A `FatalTermination` instance is one-shot. Reentrant, concurrent, or later `terminate(...)` calls are rejected.

## Important warning

Do not call `terminate(...)` as a normal error-return mechanism. Its public production path is expected to terminate the JVM. Unit tests use an internal test seam; normal engine consumers do not receive that seam.

This API proves Java shutdown orchestration. It does not itself prove that every native backend is restartable or long-duration leak-free.
