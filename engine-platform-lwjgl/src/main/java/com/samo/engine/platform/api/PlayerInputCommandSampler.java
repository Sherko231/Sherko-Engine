package com.samo.engine.platform.api;

import com.samo.engine.core.api.PlayerInputCommand;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/** Caller-owned stateful bridge from renderer-frame action snapshots to simulation-tick commands. */
public final class PlayerInputCommandSampler {
    private final double[] latestDigitalValues = new double[PlayerInputCommand.DigitalAction.values().length];
    private final boolean[] latestDigitalHeld = new boolean[PlayerInputCommand.DigitalAction.values().length];
    private final boolean[] pendingDigitalPressed = new boolean[PlayerInputCommand.DigitalAction.values().length];
    private final boolean[] pendingDigitalReleased = new boolean[PlayerInputCommand.DigitalAction.values().length];

    private boolean hasFrame;
    private long lastFrameId;
    private boolean hasTick;
    private long lastTickId;
    private double latestMoveX;
    private double latestMoveY;
    private double pendingLookX;
    private double pendingLookY;

    public void submit(InputActionSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        if (hasFrame && snapshot.frameId() <= lastFrameId) {
            throw new IllegalArgumentException("frameId must be strictly increasing");
        }

        InputActionState move = snapshot.state(InputAction.MOVE);
        InputActionState look = snapshot.state(InputAction.LOOK);
        requireFinite("MOVE.x", move.x());
        requireFinite("MOVE.y", move.y());
        requireFinite("LOOK.x", look.x());
        requireFinite("LOOK.y", look.y());

        double nextLookX = finiteSum(pendingLookX, look.x(), "LOOK.x accumulation");
        double nextLookY = finiteSum(pendingLookY, look.y(), "LOOK.y accumulation");
        double[] nextValues = latestDigitalValues.clone();
        boolean[] nextHeld = latestDigitalHeld.clone();
        boolean[] nextPressed = pendingDigitalPressed.clone();
        boolean[] nextReleased = pendingDigitalReleased.clone();

        for (PlayerInputCommand.DigitalAction action : PlayerInputCommand.DigitalAction.values()) {
            InputActionState state = snapshot.state(platformAction(action));
            requireFinite(action + ".value", state.value());
            int index = action.ordinal();
            nextValues[index] = state.value();
            nextHeld[index] = state.held();
            nextPressed[index] |= state.pressed();
            nextReleased[index] |= state.released();
        }

        latestMoveX = move.x();
        latestMoveY = move.y();
        pendingLookX = nextLookX;
        pendingLookY = nextLookY;
        System.arraycopy(nextValues, 0, latestDigitalValues, 0, nextValues.length);
        System.arraycopy(nextHeld, 0, latestDigitalHeld, 0, nextHeld.length);
        System.arraycopy(nextPressed, 0, pendingDigitalPressed, 0, nextPressed.length);
        System.arraycopy(nextReleased, 0, pendingDigitalReleased, 0, nextReleased.length);
        lastFrameId = snapshot.frameId();
        hasFrame = true;
    }

    public PlayerInputCommand nextCommand(long tickId) {
        if (!hasFrame) {
            throw new IllegalStateException("at least one action snapshot must be submitted before a command is emitted");
        }
        if (tickId < 0L) {
            throw new IllegalArgumentException("tickId must be non-negative");
        }
        if (hasTick && tickId <= lastTickId) {
            throw new IllegalArgumentException("tickId must be strictly increasing");
        }

        Map<PlayerInputCommand.DigitalAction, PlayerInputCommand.DigitalState> states = new EnumMap<>(PlayerInputCommand.DigitalAction.class);
        for (PlayerInputCommand.DigitalAction action : PlayerInputCommand.DigitalAction.values()) {
            int index = action.ordinal();
            states.put(action,
                new PlayerInputCommand.DigitalState(latestDigitalValues[index], pendingDigitalPressed[index], latestDigitalHeld[index], pendingDigitalReleased[index]));
        }

        PlayerInputCommand command = new PlayerInputCommand(tickId, latestMoveX, latestMoveY, pendingLookX, pendingLookY, states);

        pendingLookX = 0.0;
        pendingLookY = 0.0;
        for (int index = 0; index < pendingDigitalPressed.length; index++) {
            pendingDigitalPressed[index] = false;
            pendingDigitalReleased[index] = false;
        }
        lastTickId = tickId;
        hasTick = true;
        return command;
    }

    private static InputAction platformAction(PlayerInputCommand.DigitalAction action) {
        return switch (action) {
            case JUMP -> InputAction.JUMP;
            case CROUCH -> InputAction.CROUCH;
            case SPRINT -> InputAction.SPRINT;
            case INTERACT -> InputAction.INTERACT;
            case GRAB -> InputAction.GRAB;
            case THROW -> InputAction.THROW;
            case PRIMARY_USE -> InputAction.PRIMARY_USE;
            case PAUSE -> InputAction.PAUSE;
            case PUSH_TO_TALK -> InputAction.PUSH_TO_TALK;
        };
    }

    private static double finiteSum(double left, double right, String name) {
        double result = left + right;
        requireFinite(name, result);
        return result;
    }

    private static void requireFinite(String name, double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite");
        }
    }
}
