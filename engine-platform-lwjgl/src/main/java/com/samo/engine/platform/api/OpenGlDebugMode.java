package com.samo.engine.platform.api;

/** Controls production OpenGL debug-context diagnostics for one {@link GlfwWindow}. */
public enum OpenGlDebugMode {
    /** Do not request or install OpenGL debug output. */
    DISABLED,

    /** Request debug output and surface high-severity driver messages at the owner-thread boundary. */
    FAIL_ON_HIGH_SEVERITY
}
