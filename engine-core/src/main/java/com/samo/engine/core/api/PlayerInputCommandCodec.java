package com.samo.engine.core.api;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/** Fixed-size explicit binary codec for replayable player input commands. */
public final class PlayerInputCommandCodec {
    public static final int MAGIC = 0x53504943;
    public static final int VERSION = 1;
    public static final int ENCODED_SIZE = 126;

    private static final int VALID_DIGITAL_MASK = (1 << PlayerInputCommand.DigitalAction.values().length) - 1;

    private PlayerInputCommandCodec() {
    }

    public static void encode(PlayerInputCommand command, ByteBuffer destination) {
        Objects.requireNonNull(command, "command");
        Objects.requireNonNull(destination, "destination");
        if (destination.remaining() < ENCODED_SIZE) {
            throw new IllegalArgumentException("destination requires " + ENCODED_SIZE + " remaining bytes");
        }

        ByteBuffer view = destination.duplicate().order(ByteOrder.BIG_ENDIAN);
        int pressedMask = 0;
        int heldMask = 0;
        int releasedMask = 0;

        view.putInt(MAGIC);
        view.putShort((short) VERSION);
        view.putShort((short) 0);
        view.putLong(command.tickId());
        view.putDouble(command.moveX());
        view.putDouble(command.moveY());
        view.putDouble(command.lookX());
        view.putDouble(command.lookY());

        PlayerInputCommand.DigitalAction[] actions = PlayerInputCommand.DigitalAction.values();
        for (int index = 0; index < actions.length; index++) {
            PlayerInputCommand.DigitalState state = command.digitalState(actions[index]);
            view.putDouble(state.value());
            int bit = 1 << index;
            if (state.pressed()) {
                pressedMask |= bit;
            }
            if (state.held()) {
                heldMask |= bit;
            }
            if (state.released()) {
                releasedMask |= bit;
            }
        }

        view.putShort((short) pressedMask);
        view.putShort((short) heldMask);
        view.putShort((short) releasedMask);
        destination.position(destination.position() + ENCODED_SIZE);
    }

    public static PlayerInputCommand decode(ByteBuffer source) {
        Objects.requireNonNull(source, "source");
        if (source.remaining() < ENCODED_SIZE) {
            throw new IllegalArgumentException("source requires " + ENCODED_SIZE + " remaining bytes");
        }

        ByteBuffer view = source.duplicate().order(ByteOrder.BIG_ENDIAN);
        int magic = view.getInt();
        int version = Short.toUnsignedInt(view.getShort());
        int reserved = Short.toUnsignedInt(view.getShort());
        long tickId = view.getLong();
        double moveX = view.getDouble();
        double moveY = view.getDouble();
        double lookX = view.getDouble();
        double lookY = view.getDouble();

        PlayerInputCommand.DigitalAction[] actions = PlayerInputCommand.DigitalAction.values();
        double[] values = new double[actions.length];
        for (int index = 0; index < actions.length; index++) {
            values[index] = view.getDouble();
        }

        int pressedMask = Short.toUnsignedInt(view.getShort());
        int heldMask = Short.toUnsignedInt(view.getShort());
        int releasedMask = Short.toUnsignedInt(view.getShort());

        validateHeader(magic, version, reserved);
        if (tickId < 0L) {
            throw new IllegalArgumentException("tickId must be non-negative");
        }
        requireFinite("moveX", moveX);
        requireFinite("moveY", moveY);
        requireFinite("lookX", lookX);
        requireFinite("lookY", lookY);
        validateMask("pressed", pressedMask);
        validateMask("held", heldMask);
        validateMask("released", releasedMask);

        Map<PlayerInputCommand.DigitalAction, PlayerInputCommand.DigitalState> states = new EnumMap<>(PlayerInputCommand.DigitalAction.class);
        for (int index = 0; index < actions.length; index++) {
            requireFinite("digital value for " + actions[index], values[index]);
            int bit = 1 << index;
            states.put(actions[index], new PlayerInputCommand.DigitalState(values[index], (pressedMask & bit) != 0, (heldMask & bit) != 0, (releasedMask & bit) != 0));
        }

        PlayerInputCommand result = new PlayerInputCommand(tickId, moveX, moveY, lookX, lookY, states);
        source.position(source.position() + ENCODED_SIZE);
        return result;
    }

    private static void validateHeader(int magic, int version, int reserved) {
        if (magic != MAGIC) {
            throw new IllegalArgumentException("invalid PlayerInputCommand magic");
        }
        if (version != VERSION) {
            throw new IllegalArgumentException("unsupported PlayerInputCommand version: " + version);
        }
        if (reserved != 0) {
            throw new IllegalArgumentException("reserved flags must be zero");
        }
    }

    private static void validateMask(String name, int mask) {
        if ((mask & ~VALID_DIGITAL_MASK) != 0) {
            throw new IllegalArgumentException(name + " mask contains unsupported bits");
        }
    }

    private static void requireFinite(String name, double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite");
        }
    }
}
