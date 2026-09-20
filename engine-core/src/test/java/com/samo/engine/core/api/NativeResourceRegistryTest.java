package com.samo.engine.core.api;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class NativeResourceRegistryTest {
    @Test
    void emptyRegistryPassesVerification() {

        NativeResourceRegistry registry = new NativeResourceRegistry();

        assertDoesNotThrow(registry::assertNoOpenResources);

    }

    @Test
    void successfulCloseRunsCloserOnceAndUnregistersResource() {

        NativeResourceRegistry registry = new NativeResourceRegistry();
        AtomicInteger closes = new AtomicInteger();
        NativeResourceRegistry.Registration registration = registry.register("OpenGL buffer", 42L, closes::incrementAndGet);

        registration.close();
        registration.close();

        assertEquals(1, closes.get());
        assertDoesNotThrow(registry::assertNoOpenResources);

    }

    @Test
    void leakedResourceFailsVerificationWithoutClosingIt() {

        NativeResourceRegistry registry = new NativeResourceRegistry();
        AtomicInteger closes = new AtomicInteger();
        registerLeakFromNamedMethod(registry, closes);

        IllegalStateException first = assertThrows(IllegalStateException.class, registry::assertNoOpenResources);
        IllegalStateException second = assertThrows(IllegalStateException.class, registry::assertNoOpenResources);

        assertEquals(0, closes.get());
        assertTrue(first.getMessage().contains("Native resources still tracked: 1"));
        assertTrue(first.getMessage().contains("type=OpenGL buffer"));
        assertTrue(first.getMessage().contains("handle=42"));
        assertTrue(first.getMessage().contains("state=OPEN"));
        assertTrue(first.getMessage().contains("registerLeakFromNamedMethod"));
        assertEquals(first.getMessage(), second.getMessage());

    }

    @Test
    void diagnosticsPreserveRegistrationOrder() {

        NativeResourceRegistry registry = new NativeResourceRegistry();
        registry.register("first", 10L, () -> {
        });
        registry.register("second", 20L, () -> {
        });

        String message = assertThrows(IllegalStateException.class, registry::assertNoOpenResources).getMessage();

        assertTrue(message.indexOf("type=first") < message.indexOf("type=second"));

    }

    @Test
    void duplicateLiveIdentityIsRejectedWithoutAffectingOriginal() {

        NativeResourceRegistry registry = new NativeResourceRegistry();
        AtomicInteger originalCloses = new AtomicInteger();
        AtomicInteger duplicateCloses = new AtomicInteger();
        NativeResourceRegistry.Registration original = registry.register("  OpenGL buffer  ", 42L, originalCloses::incrementAndGet);

        assertThrows(IllegalStateException.class, () -> registry.register("OpenGL buffer", 42L, duplicateCloses::incrementAndGet));
        assertEquals(0, duplicateCloses.get());
        assertThrows(IllegalStateException.class, registry::assertNoOpenResources);

        original.close();
        assertEquals(1, originalCloses.get());
        assertDoesNotThrow(registry::assertNoOpenResources);

    }

    @Test
    void sameNumericHandleCanExistUnderDifferentTypes() {

        NativeResourceRegistry registry = new NativeResourceRegistry();
        NativeResourceRegistry.Registration first = registry.register("buffer", 7L, () -> {
        });
        NativeResourceRegistry.Registration second = registry.register("texture", 7L, () -> {
        });

        assertThrows(IllegalStateException.class, registry::assertNoOpenResources);
        first.close();
        assertThrows(IllegalStateException.class, registry::assertNoOpenResources);
        second.close();
        assertDoesNotThrow(registry::assertNoOpenResources);

    }

    @Test
    void identityCanBeReusedAfterSuccessfulClose() {

        NativeResourceRegistry registry = new NativeResourceRegistry();
        NativeResourceRegistry.Registration first = registry.register("buffer", 99L, () -> {
        });
        first.close();

        NativeResourceRegistry.Registration second = registry.register("buffer", 99L, () -> {
        });
        second.close();

        assertDoesNotThrow(registry::assertNoOpenResources);

    }

    @Test
    void rejectsInvalidProgrammerContracts() {

        NativeResourceRegistry registry = new NativeResourceRegistry();

        assertThrows(NullPointerException.class, () -> registry.register(null, 1L, () -> {
        }));
        assertThrows(IllegalArgumentException.class, () -> registry.register("   ", 1L, () -> {
        }));
        assertThrows(IllegalArgumentException.class, () -> registry.register("buffer", 0L, () -> {
        }));
        assertThrows(NullPointerException.class, () -> registry.register("buffer", 1L, null));
        assertDoesNotThrow(registry::assertNoOpenResources);

    }

    @Test
    void acceptsNonzeroNegativeOpaqueHandle() {

        NativeResourceRegistry registry = new NativeResourceRegistry();
        NativeResourceRegistry.Registration registration = registry.register("native pointer", -1L, () -> {
        });

        String message = assertThrows(IllegalStateException.class, registry::assertNoOpenResources).getMessage();
        assertTrue(message.contains("handle=-1"));

        registration.close();
        assertDoesNotThrow(registry::assertNoOpenResources);

    }

    @Test
    void runtimeCloserFailurePropagatesByIdentityAndRemainsTrackedWithoutRetry() {

        NativeResourceRegistry registry = new NativeResourceRegistry();
        AtomicInteger attempts = new AtomicInteger();
        RuntimeException failure = new IllegalStateException("release failed");
        NativeResourceRegistry.Registration registration = registry.register("buffer", 5L, () -> {
            attempts.incrementAndGet();
            throw failure;
        });

        assertSame(failure, assertThrows(RuntimeException.class, registration::close));
        assertDoesNotThrow(registration::close);
        assertEquals(1, attempts.get());

        String message = assertThrows(IllegalStateException.class, registry::assertNoOpenResources).getMessage();
        assertTrue(message.contains("state=CLOSE_FAILED"));

    }

    @Test
    void errorCloserFailurePropagatesByIdentityAndRemainsTrackedWithoutRetry() {

        NativeResourceRegistry registry = new NativeResourceRegistry();
        AtomicInteger attempts = new AtomicInteger();
        AssertionError failure = new AssertionError("release failed");
        NativeResourceRegistry.Registration registration = registry.register("buffer", 6L, () -> {
            attempts.incrementAndGet();
            throw failure;
        });

        assertSame(failure, assertThrows(AssertionError.class, registration::close));
        assertDoesNotThrow(registration::close);
        assertEquals(1, attempts.get());

        String message = assertThrows(IllegalStateException.class, registry::assertNoOpenResources).getMessage();
        assertTrue(message.contains("state=CLOSE_FAILED"));

    }

    @Test
    void reentrantCloseFailsOuterAttemptAndRemainsTracked() {

        NativeResourceRegistry registry = new NativeResourceRegistry();
        NativeResourceRegistry.Registration[] holder = new NativeResourceRegistry.Registration[1];
        holder[0] = registry.register("buffer", 11L, () -> holder[0].close());

        IllegalStateException failure = assertThrows(IllegalStateException.class, holder[0]::close);
        assertTrue(failure.getMessage().contains("already in progress"));
        assertDoesNotThrow(holder[0]::close);

        String message = assertThrows(IllegalStateException.class, registry::assertNoOpenResources).getMessage();
        assertTrue(message.contains("state=CLOSE_FAILED"));

    }

    private static void registerLeakFromNamedMethod(NativeResourceRegistry registry, AtomicInteger closes) {

        registry.register("  OpenGL buffer  ", 42L, closes::incrementAndGet);

    }
}
