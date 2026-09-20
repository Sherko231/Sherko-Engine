package com.samo.engine.platform.api;

import java.util.Arrays;
import java.util.EnumSet;
import org.lwjgl.glfw.GLFW;

/** Owns GLFW keyboard/mouse held state, frame edges, and focus-loss input safety. */
final class GlfwInputState {
    private final boolean[] heldKeys = new boolean[GLFW.GLFW_KEY_LAST + 1];
    private final boolean[] heldMouseButtons = new boolean[GLFW.GLFW_MOUSE_BUTTON_LAST + 1];
    private final boolean[] pendingPressedKeys = new boolean[InputKey.values().length];
    private final boolean[] pendingReleasedKeys = new boolean[InputKey.values().length];
    private final boolean[] pendingPressedMouseButtons = new boolean[InputMouseButton.values().length];
    private final boolean[] pendingReleasedMouseButtons = new boolean[InputMouseButton.values().length];

    private boolean focused;

    boolean focused() {
        return focused;
    }

    void onFocusChanged(boolean focused) {
        this.focused = focused;
        if (!focused) {
            clearHeldInputForFocusLoss();
        }
    }

    void onKeyChanged(int key, int action) {
        if (!focused || key < 0 || key >= heldKeys.length) {
            return;
        }
        InputKey inputKey = inputKey(key);
        if (action == GLFW.GLFW_PRESS) {
            heldKeys[key] = true;
            if (inputKey != null) {
                pendingPressedKeys[inputKey.ordinal()] = true;
            }
        } else if (action == GLFW.GLFW_REPEAT) {
            heldKeys[key] = true;
        } else if (action == GLFW.GLFW_RELEASE) {
            heldKeys[key] = false;
            if (inputKey != null) {
                pendingReleasedKeys[inputKey.ordinal()] = true;
            }
        }
    }

    void onMouseButtonChanged(int button, int action) {
        if (!focused || button < 0 || button >= heldMouseButtons.length) {
            return;
        }
        InputMouseButton inputButton = inputMouseButton(button);
        if (action == GLFW.GLFW_PRESS) {
            heldMouseButtons[button] = true;
            if (inputButton != null) {
                pendingPressedMouseButtons[inputButton.ordinal()] = true;
            }
        } else if (action == GLFW.GLFW_RELEASE) {
            heldMouseButtons[button] = false;
            if (inputButton != null) {
                pendingReleasedMouseButtons[inputButton.ordinal()] = true;
            }
        }
    }

    InputSnapshot captureSnapshot(
            long frameId,
            boolean cursorCaptured,
            double accumulatedMouseDeltaX,
            double accumulatedMouseDeltaY) {
        if (frameId < 0L) {
            throw new IllegalArgumentException("frameId must be non-negative");
        }

        EnumSet<InputKey> heldKeySet = EnumSet.noneOf(InputKey.class);
        EnumSet<InputKey> pressedKeySet = EnumSet.noneOf(InputKey.class);
        EnumSet<InputKey> releasedKeySet = EnumSet.noneOf(InputKey.class);
        for (InputKey key : InputKey.values()) {
            if (heldKeys[glfwKey(key)]) {
                heldKeySet.add(key);
            }
            if (pendingPressedKeys[key.ordinal()]) {
                pressedKeySet.add(key);
            }
            if (pendingReleasedKeys[key.ordinal()]) {
                releasedKeySet.add(key);
            }
        }

        EnumSet<InputMouseButton> heldButtonSet = EnumSet.noneOf(InputMouseButton.class);
        EnumSet<InputMouseButton> pressedButtonSet = EnumSet.noneOf(InputMouseButton.class);
        EnumSet<InputMouseButton> releasedButtonSet = EnumSet.noneOf(InputMouseButton.class);
        for (InputMouseButton button : InputMouseButton.values()) {
            if (heldMouseButtons[glfwMouseButton(button)]) {
                heldButtonSet.add(button);
            }
            if (pendingPressedMouseButtons[button.ordinal()]) {
                pressedButtonSet.add(button);
            }
            if (pendingReleasedMouseButtons[button.ordinal()]) {
                releasedButtonSet.add(button);
            }
        }

        InputSnapshot snapshot = new InputSnapshot(
                frameId,
                focused,
                cursorCaptured,
                heldKeySet,
                pressedKeySet,
                releasedKeySet,
                heldButtonSet,
                pressedButtonSet,
                releasedButtonSet,
                accumulatedMouseDeltaX,
                accumulatedMouseDeltaY);
        clearPendingInputEdges();
        return snapshot;
    }

    boolean isKeyHeld(int key) {
        return key >= 0 && key < heldKeys.length && heldKeys[key];
    }

    boolean isMouseButtonHeld(int button) {
        return button >= 0 && button < heldMouseButtons.length && heldMouseButtons[button];
    }

    void clearForLifecycle() {
        clearHeldInput();
        clearPendingInputEdges();
    }

    private void clearHeldInput() {
        Arrays.fill(heldKeys, false);
        Arrays.fill(heldMouseButtons, false);
    }

    private void clearHeldInputForFocusLoss() {
        Arrays.fill(pendingPressedKeys, false);
        Arrays.fill(pendingPressedMouseButtons, false);
        for (InputKey key : InputKey.values()) {
            int nativeKey = glfwKey(key);
            if (heldKeys[nativeKey]) {
                pendingReleasedKeys[key.ordinal()] = true;
            }
        }
        for (InputMouseButton button : InputMouseButton.values()) {
            int nativeButton = glfwMouseButton(button);
            if (heldMouseButtons[nativeButton]) {
                pendingReleasedMouseButtons[button.ordinal()] = true;
            }
        }
        clearHeldInput();
    }

    private void clearPendingInputEdges() {
        Arrays.fill(pendingPressedKeys, false);
        Arrays.fill(pendingReleasedKeys, false);
        Arrays.fill(pendingPressedMouseButtons, false);
        Arrays.fill(pendingReleasedMouseButtons, false);
    }

    private static InputKey inputKey(int glfwKey) {
        return switch (glfwKey) {
            case GLFW.GLFW_KEY_W -> InputKey.W;
            case GLFW.GLFW_KEY_A -> InputKey.A;
            case GLFW.GLFW_KEY_S -> InputKey.S;
            case GLFW.GLFW_KEY_D -> InputKey.D;
            case GLFW.GLFW_KEY_SPACE -> InputKey.SPACE;
            case GLFW.GLFW_KEY_LEFT_SHIFT -> InputKey.LEFT_SHIFT;
            case GLFW.GLFW_KEY_RIGHT_SHIFT -> InputKey.RIGHT_SHIFT;
            case GLFW.GLFW_KEY_LEFT_CONTROL -> InputKey.LEFT_CONTROL;
            case GLFW.GLFW_KEY_RIGHT_CONTROL -> InputKey.RIGHT_CONTROL;
            case GLFW.GLFW_KEY_LEFT_ALT -> InputKey.LEFT_ALT;
            case GLFW.GLFW_KEY_RIGHT_ALT -> InputKey.RIGHT_ALT;
            case GLFW.GLFW_KEY_ESCAPE -> InputKey.ESCAPE;
            case GLFW.GLFW_KEY_E -> InputKey.E;
            case GLFW.GLFW_KEY_Q -> InputKey.Q;
            case GLFW.GLFW_KEY_R -> InputKey.R;
            case GLFW.GLFW_KEY_F -> InputKey.F;
            default -> null;
        };
    }

    private static int glfwKey(InputKey key) {
        return switch (key) {
            case W -> GLFW.GLFW_KEY_W;
            case A -> GLFW.GLFW_KEY_A;
            case S -> GLFW.GLFW_KEY_S;
            case D -> GLFW.GLFW_KEY_D;
            case SPACE -> GLFW.GLFW_KEY_SPACE;
            case LEFT_SHIFT -> GLFW.GLFW_KEY_LEFT_SHIFT;
            case RIGHT_SHIFT -> GLFW.GLFW_KEY_RIGHT_SHIFT;
            case LEFT_CONTROL -> GLFW.GLFW_KEY_LEFT_CONTROL;
            case RIGHT_CONTROL -> GLFW.GLFW_KEY_RIGHT_CONTROL;
            case LEFT_ALT -> GLFW.GLFW_KEY_LEFT_ALT;
            case RIGHT_ALT -> GLFW.GLFW_KEY_RIGHT_ALT;
            case ESCAPE -> GLFW.GLFW_KEY_ESCAPE;
            case E -> GLFW.GLFW_KEY_E;
            case Q -> GLFW.GLFW_KEY_Q;
            case R -> GLFW.GLFW_KEY_R;
            case F -> GLFW.GLFW_KEY_F;
        };
    }

    private static InputMouseButton inputMouseButton(int glfwButton) {
        return switch (glfwButton) {
            case GLFW.GLFW_MOUSE_BUTTON_LEFT -> InputMouseButton.LEFT;
            case GLFW.GLFW_MOUSE_BUTTON_RIGHT -> InputMouseButton.RIGHT;
            case GLFW.GLFW_MOUSE_BUTTON_MIDDLE -> InputMouseButton.MIDDLE;
            case GLFW.GLFW_MOUSE_BUTTON_4 -> InputMouseButton.BUTTON_4;
            case GLFW.GLFW_MOUSE_BUTTON_5 -> InputMouseButton.BUTTON_5;
            default -> null;
        };
    }

    private static int glfwMouseButton(InputMouseButton button) {
        return switch (button) {
            case LEFT -> GLFW.GLFW_MOUSE_BUTTON_LEFT;
            case RIGHT -> GLFW.GLFW_MOUSE_BUTTON_RIGHT;
            case MIDDLE -> GLFW.GLFW_MOUSE_BUTTON_MIDDLE;
            case BUTTON_4 -> GLFW.GLFW_MOUSE_BUTTON_4;
            case BUTTON_5 -> GLFW.GLFW_MOUSE_BUTTON_5;
        };
    }
}
