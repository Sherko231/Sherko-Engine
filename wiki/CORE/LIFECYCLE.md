# Lifecycle and subsystem ownership

`EngineSubsystem` is the base lifecycle contract for engine subsystems.

## Normal lifecycle

```java
subsystem.initialize();
subsystem.start();

// subsystem is running

subsystem.stop();
subsystem.close();
```

The public lifecycle methods are final. Subclasses implement protected hooks:

```text
initialize() -> onInitialize()
start()      -> onStart()
stop()       -> onStop()
close()      -> onClose()
```

## Lifecycle meaning

- `initialize()` acquires resources.
- `start()` activates initialized resources/work.
- `stop()` quiesces active work without releasing resources.
- `close()` performs terminal resource release.

## Important rules

- The owner must serialize lifecycle calls.
- `EngineSubsystem` is not thread-safe.
- A running subsystem cannot be closed normally; stop it first.
- A subsystem that was initialized but never started may be closed directly.
- If initialize/start/stop throws an unchecked failure, the subsystem enters a failed state and forward lifecycle progress stops, but `close()` can still be attempted.
- `close()` is terminal and is attempted at most once.
- Reentrant lifecycle calls from lifecycle hooks are invalid.
- The contract does not promise restartability or native reinitialization.

## Multiple subsystems

`SubsystemGraph` represents dependency ordering. `SubsystemStartupCoordinator` is the coordination layer for ordered startup and rollback/cleanup across multiple subsystems. Do not make one `EngineSubsystem` directly coordinate another subsystem's lifecycle unless the active architecture contract explicitly requires it.

## Native subsystems

Native wrappers must keep ownership explicit. A failed Java close does not prove the native resource was released. Use `NativeResourceRegistry` where the subsystem owns native handles and preserve any native-thread-affinity requirements.

See [Native resource ownership](NATIVE_RESOURCES.md).
