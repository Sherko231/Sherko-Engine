package com.samo.engine.platform.api;

/** Non-owning guard for the single thread allowed to enter production OpenGL work. */
public final class OpenGlThreadGuard {
    private Thread ownerThread;

    OpenGlThreadGuard() {

    }

    void bindOwnerThread(Thread thread) {

        if (ownerThread != null) {
            throw new IllegalStateException("OpenGL owner thread is already bound");
        }
        ownerThread = java.util.Objects.requireNonNull(thread, "thread");

    }

    /** Rejects calls made before binding or from a thread other than the OpenGL owner. */
    public void assertOwnerThread() {

        Thread owner = ownerThread;
        if (owner == null) {
            throw new IllegalStateException("OpenGL owner thread is not initialized");
        }
        if (owner != Thread.currentThread()) {
            throw new IllegalStateException("OpenGL calls must run on the initializing thread");
        }

    }
}
