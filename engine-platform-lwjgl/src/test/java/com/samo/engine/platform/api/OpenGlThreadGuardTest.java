package com.samo.engine.platform.api;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class OpenGlThreadGuardTest {
    @Test
    void rejectsBeforeBinding() {

        OpenGlThreadGuard guard = new OpenGlThreadGuard();

        assertThrows(IllegalStateException.class, guard::assertOwnerThread);

    }

    @Test
    void bindsOnceAllowsOwnerAndRejectsWorkerWithoutPoisoningOwner() throws Exception {

        OpenGlThreadGuard guard = new OpenGlThreadGuard();
        Thread owner = Thread.currentThread();
        guard.bindOwnerThread(owner);

        guard.assertOwnerThread();
        assertThrows(IllegalStateException.class, () -> guard.bindOwnerThread(owner));

        AtomicReference<Throwable> result = new AtomicReference<>();
        Thread worker = Thread.ofPlatform().start(() -> {
            try {
                guard.assertOwnerThread();
            } catch (Throwable failure) {
                result.set(failure);
            }
        });
        worker.join(5_000L);

        assertSame(IllegalStateException.class, result.get().getClass());
        guard.assertOwnerThread();

    }

    @Test
    void rejectsNullOwnerBindingWithoutMutatingState() {

        OpenGlThreadGuard guard = new OpenGlThreadGuard();

        assertThrows(NullPointerException.class, () -> guard.bindOwnerThread(null));
        assertThrows(IllegalStateException.class, guard::assertOwnerThread);

    }
}
