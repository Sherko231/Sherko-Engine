package com.samo.engine.render.opengl.internal;

final class CleanupFailures {
    private CleanupFailures() {
    }

    static void addSuppressedUnlessSame(Throwable primary, Throwable cleanupFailure) {
        if (cleanupFailure != primary) {
            primary.addSuppressed(cleanupFailure);
        }
    }
}
