package com.samo.game.sandbox.demo;

import com.samo.game.sandbox.SandboxMain;

/** Legacy compatibility entry point. Use {@link SandboxMain} / {@code runSandbox}. */
@Deprecated(forRemoval = false)
public final class EngineDemoMain {
    private EngineDemoMain() {
    }

    public static void main(String[] args) throws InterruptedException {
        SandboxMain.main(args);
    }
}
