package com.samo.engine.core.api;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/** Immutable device-neutral input command for one simulation tick. */
public final class PlayerInputCommand {
    /** Digital gameplay actions carried by the tick command. */
    public enum DigitalAction {
        JUMP,
        CROUCH,
        SPRINT,
        INTERACT,
        GRAB,
        THROW,
        PRIMARY_USE,
        PAUSE,
        PUSH_TO_TALK
    }

    /** Immutable scalar and transition state for one digital action. */
    public record DigitalState(double value, boolean pressed, boolean held, boolean released) {
        public DigitalState {
            if (!Double.isFinite(value)) {
                throw new IllegalArgumentException("digital value must be finite");
            }
        }
    }

    private final long tickId;
    private final double moveX;
    private final double moveY;
    private final double lookX;
    private final double lookY;
    private final Map<DigitalAction, DigitalState> digitalStates;

    public PlayerInputCommand(
            long tickId,
            double moveX,
            double moveY,
            double lookX,
            double lookY,
            Map<DigitalAction, DigitalState> digitalStates) {
        if (tickId < 0L) {
            throw new IllegalArgumentException("tickId must be non-negative");
        }
        requireFinite("moveX", moveX);
        requireFinite("moveY", moveY);
        requireFinite("lookX", lookX);
        requireFinite("lookY", lookY);
        Objects.requireNonNull(digitalStates, "digitalStates");

        EnumMap<DigitalAction, DigitalState> copy = new EnumMap<>(DigitalAction.class);
        for (DigitalAction action : DigitalAction.values()) {
            copy.put(action, Objects.requireNonNull(digitalStates.get(action), "state for " + action));
        }
        if (digitalStates.size() != DigitalAction.values().length) {
            throw new IllegalArgumentException("digitalStates must contain exactly the supported actions");
        }

        this.tickId = tickId;
        this.moveX = moveX;
        this.moveY = moveY;
        this.lookX = lookX;
        this.lookY = lookY;
        this.digitalStates = Map.copyOf(copy);
    }

    public long tickId() {
        return tickId;
    }

    public double moveX() {
        return moveX;
    }

    public double moveY() {
        return moveY;
    }

    public double lookX() {
        return lookX;
    }

    public double lookY() {
        return lookY;
    }

    public DigitalState digitalState(DigitalAction action) {
        return digitalStates.get(Objects.requireNonNull(action, "action"));
    }

    public Map<DigitalAction, DigitalState> digitalStates() {
        return digitalStates;
    }

    private static void requireFinite(String name, double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite");
        }
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof PlayerInputCommand that)) {
            return false;
        }
        return tickId == that.tickId
                && Double.doubleToLongBits(moveX) == Double.doubleToLongBits(that.moveX)
                && Double.doubleToLongBits(moveY) == Double.doubleToLongBits(that.moveY)
                && Double.doubleToLongBits(lookX) == Double.doubleToLongBits(that.lookX)
                && Double.doubleToLongBits(lookY) == Double.doubleToLongBits(that.lookY)
                && digitalStates.equals(that.digitalStates);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tickId, moveX, moveY, lookX, lookY, digitalStates);
    }

    @Override
    public String toString() {
        return "PlayerInputCommand[tickId=" + tickId
                + ", moveX=" + moveX
                + ", moveY=" + moveY
                + ", lookX=" + lookX
                + ", lookY=" + lookY
                + ", digitalStates=" + digitalStates
                + ']';
    }
}
