package com.samo.engine.render.opengl.internal;

import com.samo.engine.core.api.NativeResourceRegistry;
import com.samo.engine.platform.api.OpenGlThreadGuard;
import java.util.Objects;
import java.util.function.IntConsumer;

final class OwnedOpenGlHandle implements AutoCloseable {
    private final int handle;
    private final OpenGlThreadGuard threadGuard;
    private final NativeResourceRegistry.Registration registration;
    private boolean closeAttempted;

    private OwnedOpenGlHandle(int handle, OpenGlThreadGuard threadGuard, NativeResourceRegistry.Registration registration) {

        this.handle = handle;
        this.threadGuard = threadGuard;
        this.registration = registration;

    }

    static OwnedOpenGlHandle register(String resourceType, int handle, OpenGlThreadGuard threadGuard, NativeResourceRegistry registry, IntConsumer deleter) {

        OpenGlThreadGuard guard = Objects.requireNonNull(threadGuard, "threadGuard");
        NativeResourceRegistry resources = Objects.requireNonNull(registry, "registry");
        IntConsumer nativeDeleter = Objects.requireNonNull(deleter, "deleter");

        guard.assertOwnerThread();
        if (handle == 0) {
            throw new IllegalStateException(resourceType + " creation returned handle 0");
        }

        NativeResourceRegistry.Registration registration;
        try {
            registration = resources.register(resourceType, Integer.toUnsignedLong(handle), () -> {
                guard.assertOwnerThread();
                nativeDeleter.accept(handle);
            });
        } catch (RuntimeException | Error failure) {
            CleanupFailureSuppression.runAndSuppress(failure, () -> nativeDeleter.accept(handle));
            throw failure;
        }
        return new OwnedOpenGlHandle(handle, guard, registration);

    }

    int handle() {

        return handle;

    }

    @Override
    public void close() {

        if (closeAttempted) {
            return;
        }
        threadGuard.assertOwnerThread();
        closeAttempted = true;
        registration.close();

    }
}
