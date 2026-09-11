# Native resource ownership

`NativeResourceRegistry` provides explicit native-handle ownership diagnostics without depending on a specific native library.

## Register one native handle

```java
import com.samo.engine.core.api.NativeResourceRegistry;

NativeResourceRegistry registry = new NativeResourceRegistry();

long handle = 1234L; // example nonzero native handle

NativeResourceRegistry.Registration registration = registry.register(
        "Example native resource",
        handle,
        () -> releaseNativeHandle(handle));
```

The closer runs synchronously when the registration is closed.

## Release ownership

```java
registration.close();
```

A successful close runs the supplied closer and removes that resource from the registry.

## Verify terminal cleanup

```java
registry.assertNoOpenResources();
```

This is a diagnostic check. It does not close leaked resources for you.

## Important behavior

- The registry is intentionally not thread-safe.
- Serialize register/close/assert operations on the correct lifecycle/native-affinity thread.
- Resource type is stripped and must remain nonblank.
- Handle must be nonzero.
- Registering the same live type/handle pair twice fails.
- A registration attempts its native closer at most once.
- If the closer fails, the registration remains tracked in a failed-close state; the registry does not silently pretend cleanup succeeded.
- Leak diagnostics retain the allocation call site.

## Ownership pattern

A native-owning subsystem should normally:

1. acquire the native handle;
2. register it immediately;
3. retain the returned `Registration` as its close capability;
4. stop native activity before destruction;
5. close the registration during terminal cleanup;
6. verify the owner registry is empty at the outer composition boundary.

`GlfwWindow` follows this pattern for its GLFW window handle.
