package com.samo.engine.platform.api;

import java.util.EnumSet;
import java.util.Objects;

/** Immutable hardware input state captured for one caller-defined renderer frame. */
public final class InputSnapshot {
    private final long frameId;
    private final boolean focused;
    private final boolean cursorCaptured;
    private final EnumSet<InputKey> heldKeys;
    private final EnumSet<InputKey> pressedKeys;
    private final EnumSet<InputKey> releasedKeys;
    private final EnumSet<InputMouseButton> heldMouseButtons;
    private final EnumSet<InputMouseButton> pressedMouseButtons;
    private final EnumSet<InputMouseButton> releasedMouseButtons;
    private final double mouseDeltaX;
    private final double mouseDeltaY;

    InputSnapshot(
            long frameId,
            boolean focused,
            boolean cursorCaptured,
            EnumSet<InputKey> heldKeys,
            EnumSet<InputKey> pressedKeys,
            EnumSet<InputKey> releasedKeys,
            EnumSet<InputMouseButton> heldMouseButtons,
            EnumSet<InputMouseButton> pressedMouseButtons,
            EnumSet<InputMouseButton> releasedMouseButtons,
            double mouseDeltaX,
            double mouseDeltaY) {
        if (frameId < 0L) {
            throw new IllegalArgumentException("frameId must be non-negative");
        }
        this.frameId = frameId;
        this.focused = focused;
        this.cursorCaptured = cursorCaptured;
        this.heldKeys = copy(heldKeys, "heldKeys");
        this.pressedKeys = copy(pressedKeys, "pressedKeys");
        this.releasedKeys = copy(releasedKeys, "releasedKeys");
        this.heldMouseButtons = copy(heldMouseButtons, "heldMouseButtons");
        this.pressedMouseButtons = copy(pressedMouseButtons, "pressedMouseButtons");
        this.releasedMouseButtons = copy(releasedMouseButtons, "releasedMouseButtons");
        this.mouseDeltaX = mouseDeltaX;
        this.mouseDeltaY = mouseDeltaY;
    }

    public long frameId() {
        return frameId;
    }

    public boolean focused() {
        return focused;
    }

    public boolean cursorCaptured() {
        return cursorCaptured;
    }

    public boolean keyHeld(InputKey key) {
        return heldKeys.contains(Objects.requireNonNull(key, "key"));
    }

    public boolean keyPressed(InputKey key) {
        return pressedKeys.contains(Objects.requireNonNull(key, "key"));
    }

    public boolean keyReleased(InputKey key) {
        return releasedKeys.contains(Objects.requireNonNull(key, "key"));
    }

    public boolean mouseButtonHeld(InputMouseButton button) {
        return heldMouseButtons.contains(Objects.requireNonNull(button, "button"));
    }

    public boolean mouseButtonPressed(InputMouseButton button) {
        return pressedMouseButtons.contains(Objects.requireNonNull(button, "button"));
    }

    public boolean mouseButtonReleased(InputMouseButton button) {
        return releasedMouseButtons.contains(Objects.requireNonNull(button, "button"));
    }

    public double mouseDeltaX() {
        return mouseDeltaX;
    }

    public double mouseDeltaY() {
        return mouseDeltaY;
    }

    private static <E extends Enum<E>> EnumSet<E> copy(EnumSet<E> values, String name) {
        return EnumSet.copyOf(Objects.requireNonNull(values, name));
    }
}
