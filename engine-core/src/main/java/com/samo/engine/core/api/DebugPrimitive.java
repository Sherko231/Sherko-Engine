package com.samo.engine.core.api;

/** Renderer-neutral per-frame debug geometry primitive. */
public sealed interface DebugPrimitive permits DebugLine, DebugAabb, DebugSphere, DebugRay {
    DebugColor color();
}
