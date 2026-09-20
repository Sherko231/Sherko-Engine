package com.samo.engine.render.opengl.internal;

import java.util.Objects;

final class CleanupFailureSuppression {
    private CleanupFailureSuppression() {

    }

    static void addSuppressedUnlessSame(Throwable primary, Throwable cleanupFailure) {

        if (cleanupFailure != primary) {
            primary.addSuppressed(cleanupFailure);
        }

    }

    static void runAndSuppress(Throwable primary, Runnable cleanup) {

        Throwable original = Objects.requireNonNull(primary, "primary");
        Runnable action = Objects.requireNonNull(cleanup, "cleanup");
        try {
            action.run();
        } catch (RuntimeException | Error cleanupFailure) {
            addSuppressedUnlessSame(original, cleanupFailure);
        }

    }
}
