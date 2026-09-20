package com.samo.engine.render.opengl.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class CleanupFailureSuppressionTest {
    @Test
    void successfulCleanupLeavesPrimaryUnchanged() {

        RuntimeException primary = new IllegalStateException("primary");
        AtomicBoolean cleaned = new AtomicBoolean();

        CleanupFailureSuppression.runAndSuppress(primary, () -> cleaned.set(true));

        assertEquals(true, cleaned.get());
        assertEquals(0, primary.getSuppressed().length);

    }

    @Test
    void distinctCleanupFailureIsSuppressedOnPrimary() {

        RuntimeException primary = new IllegalStateException("primary");
        RuntimeException cleanupFailure = new IllegalStateException("cleanup");

        CleanupFailureSuppression.runAndSuppress(primary, () -> {
            throw cleanupFailure;
        });

        assertEquals(1, primary.getSuppressed().length);
        assertSame(cleanupFailure, primary.getSuppressed()[0]);

    }

    @Test
    void sameThrowableIsNotSelfSuppressed() {

        RuntimeException primary = new IllegalStateException("shared");

        CleanupFailureSuppression.runAndSuppress(primary, () -> {
            throw primary;
        });

        assertEquals(0, primary.getSuppressed().length);

    }
}
