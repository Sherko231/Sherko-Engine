package com.samo.engine.platform.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import org.junit.jupiter.api.Test;

class InputSnapshotTest {
    @Test
    void snapshotCopiesAllSourceSetsAndRemainsStable() {
        EnumSet<InputKey> heldKeys = EnumSet.of(InputKey.W);
        EnumSet<InputKey> pressedKeys = EnumSet.of(InputKey.A);
        EnumSet<InputKey> releasedKeys = EnumSet.of(InputKey.S);
        EnumSet<InputMouseButton> heldButtons = EnumSet.of(InputMouseButton.LEFT);
        EnumSet<InputMouseButton> pressedButtons = EnumSet.of(InputMouseButton.RIGHT);
        EnumSet<InputMouseButton> releasedButtons = EnumSet.of(InputMouseButton.MIDDLE);

        InputSnapshot snapshot = new InputSnapshot(42L, true, true, heldKeys, pressedKeys, releasedKeys, heldButtons, pressedButtons, releasedButtons, 3.5, -2.25);

        heldKeys.clear();
        pressedKeys.clear();
        releasedKeys.clear();
        heldButtons.clear();
        pressedButtons.clear();
        releasedButtons.clear();

        assertEquals(42L, snapshot.frameId());
        assertTrue(snapshot.focused());
        assertTrue(snapshot.cursorCaptured());
        assertTrue(snapshot.keyHeld(InputKey.W));
        assertTrue(snapshot.keyPressed(InputKey.A));
        assertTrue(snapshot.keyReleased(InputKey.S));
        assertTrue(snapshot.mouseButtonHeld(InputMouseButton.LEFT));
        assertTrue(snapshot.mouseButtonPressed(InputMouseButton.RIGHT));
        assertTrue(snapshot.mouseButtonReleased(InputMouseButton.MIDDLE));
        assertEquals(3.5, snapshot.mouseDeltaX(), 0.0);
        assertEquals(-2.25, snapshot.mouseDeltaY(), 0.0);
    }

    @Test
    void queriesRejectNullWithoutMutatingSnapshot() {
        InputSnapshot snapshot = emptySnapshot();

        assertThrows(NullPointerException.class, () -> snapshot.keyHeld(null));
        assertThrows(NullPointerException.class, () -> snapshot.keyPressed(null));
        assertThrows(NullPointerException.class, () -> snapshot.keyReleased(null));
        assertThrows(NullPointerException.class, () -> snapshot.mouseButtonHeld(null));
        assertThrows(NullPointerException.class, () -> snapshot.mouseButtonPressed(null));
        assertThrows(NullPointerException.class, () -> snapshot.mouseButtonReleased(null));

        assertFalse(snapshot.keyHeld(InputKey.W));
        assertFalse(snapshot.mouseButtonHeld(InputMouseButton.LEFT));
    }

    @Test
    void negativeFrameIdentityIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new InputSnapshot(-1L, true, false, EnumSet.noneOf(InputKey.class), EnumSet.noneOf(InputKey.class),
            EnumSet.noneOf(InputKey.class), EnumSet.noneOf(InputMouseButton.class), EnumSet.noneOf(InputMouseButton.class), EnumSet.noneOf(InputMouseButton.class), 0.0, 0.0));
    }

    private static InputSnapshot emptySnapshot() {
        return new InputSnapshot(0L, false, false, EnumSet.noneOf(InputKey.class), EnumSet.noneOf(InputKey.class), EnumSet.noneOf(InputKey.class),
            EnumSet.noneOf(InputMouseButton.class), EnumSet.noneOf(InputMouseButton.class), EnumSet.noneOf(InputMouseButton.class), 0.0, 0.0);
    }
}
