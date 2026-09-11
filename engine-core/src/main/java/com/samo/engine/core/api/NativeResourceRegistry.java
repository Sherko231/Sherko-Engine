package com.samo.engine.core.api;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Tracks explicitly owned native handles for deterministic shutdown diagnostics.
 *
 * <p>The registry is intentionally not thread-safe. Callers must externally
 * serialize registration, close, and verification calls on the appropriate
 * lifecycle/native-affinity thread.
 */
public final class NativeResourceRegistry {
    private final Map<ResourceKey, Registration> registrations = new LinkedHashMap<>();

    /** Creates an empty registry. */
    public NativeResourceRegistry() {
    }

    /**
     * Registers one explicitly owned native handle.
     *
     * @param resourceType diagnostic resource type; surrounding whitespace is removed
     * @param handle opaque nonzero native handle value
     * @param closer native release action to run synchronously on close
     * @return the sole supported close capability for this tracked ownership
     * @throws NullPointerException if {@code resourceType} or {@code closer} is null
     * @throws IllegalArgumentException if the normalized resource type is blank or the handle is zero
     * @throws IllegalStateException if the same live resource type/handle pair is already tracked
     */
    public Registration register(String resourceType, long handle, Runnable closer) {
        String normalizedType = Objects.requireNonNull(resourceType, "resourceType").strip();
        if (normalizedType.isEmpty()) {
            throw new IllegalArgumentException("resourceType must not be blank");
        }
        if (handle == 0L) {
            throw new IllegalArgumentException("handle must be nonzero");
        }
        Runnable releaseAction = Objects.requireNonNull(closer, "closer");
        ResourceKey key = new ResourceKey(normalizedType, handle);
        if (registrations.containsKey(key)) {
            throw new IllegalStateException(
                    "Native resource already registered: type=" + normalizedType + ", handle=" + handle);
        }

        Registration registration = new Registration(
                this,
                key,
                releaseAction,
                captureAllocationSite());
        registrations.put(key, registration);
        return registration;
    }

    /**
     * Fails when any native ownership remains tracked.
     *
     * <p>This check is diagnostic only: it never invokes closers or mutates the
     * registry.
     *
     * @throws IllegalStateException when at least one resource is still tracked
     */
    public void assertNoOpenResources() {
        if (registrations.isEmpty()) {
            return;
        }

        StringBuilder message = new StringBuilder()
                .append("Native resources still tracked: ")
                .append(registrations.size());
        int index = 1;
        for (Registration registration : registrations.values()) {
            message.append(System.lineSeparator())
                    .append(index++)
                    .append(". type=")
                    .append(registration.key.resourceType())
                    .append(", handle=")
                    .append(registration.key.handle())
                    .append(", state=")
                    .append(registration.state)
                    .append(", allocatedAt=")
                    .append(registration.allocationSite);
        }
        throw new IllegalStateException(message.toString());
    }

    private void removeSuccessful(Registration registration) {
        Registration removed = registrations.remove(registration.key);
        if (removed != registration) {
            throw new IllegalStateException("Native resource registry ownership mismatch");
        }
    }

    private static StackTraceElement captureAllocationSite() {
        String registryClass = NativeResourceRegistry.class.getName();
        return StackWalker.getInstance().walk(frames -> frames
                .filter(frame -> {
                    String className = frame.getClassName();
                    return !className.equals(registryClass)
                            && !className.startsWith(registryClass + "$");
                })
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Unable to capture native resource allocation site"))
                .toStackTraceElement());
    }

    private record ResourceKey(String resourceType, long handle) {
    }

    /** Represents one registered native ownership and releases it at most once. */
    public static final class Registration implements AutoCloseable {
        private final NativeResourceRegistry owner;
        private final ResourceKey key;
        private final Runnable closer;
        private final StackTraceElement allocationSite;
        private State state = State.OPEN;

        private Registration(
                NativeResourceRegistry owner,
                ResourceKey key,
                Runnable closer,
                StackTraceElement allocationSite) {
            this.owner = owner;
            this.key = key;
            this.closer = closer;
            this.allocationSite = allocationSite;
        }

        /**
         * Attempts the native closer once. Successful close unregisters the resource;
         * failed close remains tracked and is never retried automatically.
         */
        @Override
        public void close() {
            if (state == State.CLOSED || state == State.CLOSE_FAILED) {
                return;
            }
            if (state == State.CLOSING) {
                throw new IllegalStateException("Native resource close is already in progress");
            }

            state = State.CLOSING;
            try {
                closer.run();
                owner.removeSuccessful(this);
                state = State.CLOSED;
            } catch (RuntimeException | Error failure) {
                state = State.CLOSE_FAILED;
                throw failure;
            }
        }
    }

    private enum State {
        OPEN,
        CLOSING,
        CLOSED,
        CLOSE_FAILED
    }
}
