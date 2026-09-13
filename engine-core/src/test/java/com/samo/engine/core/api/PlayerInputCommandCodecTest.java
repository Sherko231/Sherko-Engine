package com.samo.engine.core.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.EnumMap;
import org.junit.jupiter.api.Test;

class PlayerInputCommandCodecTest {
    @Test
    void encodesFixedBigEndianLayoutAndRoundTripsWithoutChangingCallerByteOrder() {
        PlayerInputCommand command = fixture(42L);
        ByteBuffer buffer = ByteBuffer.allocate(160).order(ByteOrder.LITTLE_ENDIAN);
        buffer.position(7);
        int start = buffer.position();

        PlayerInputCommandCodec.encode(command, buffer);

        assertThat(PlayerInputCommandCodec.ENCODED_SIZE).isEqualTo(126);
        assertThat(buffer.position()).isEqualTo(start + 126);
        assertThat(buffer.order()).isEqualTo(ByteOrder.LITTLE_ENDIAN);

        ByteBuffer layout = buffer.duplicate().order(ByteOrder.BIG_ENDIAN);
        layout.position(start);
        assertThat(layout.getInt()).isEqualTo(0x53504943);
        assertThat(Short.toUnsignedInt(layout.getShort())).isEqualTo(1);
        assertThat(Short.toUnsignedInt(layout.getShort())).isZero();
        assertThat(layout.getLong()).isEqualTo(42L);
        assertThat(layout.getDouble()).isEqualTo(0.25d);
        assertThat(layout.getDouble()).isEqualTo(-0.5d);
        assertThat(layout.getDouble()).isEqualTo(3.0d);
        assertThat(layout.getDouble()).isEqualTo(-4.0d);
        for (int ordinal = 0; ordinal < PlayerInputCommand.DigitalAction.values().length; ordinal++) {
            assertThat(layout.getDouble()).isEqualTo(ordinal + 1.0d);
        }
        assertThat(Short.toUnsignedInt(layout.getShort())).isEqualTo(1);
        assertThat(Short.toUnsignedInt(layout.getShort())).isEqualTo(1 << 1);
        assertThat(Short.toUnsignedInt(layout.getShort())).isEqualTo(1);
        assertThat(layout.position()).isEqualTo(start + PlayerInputCommandCodec.ENCODED_SIZE);

        buffer.position(start);
        PlayerInputCommand decoded = PlayerInputCommandCodec.decode(buffer);
        assertThat(decoded).isEqualTo(command);
        assertThat(buffer.position()).isEqualTo(start + 126);
        assertThat(buffer.order()).isEqualTo(ByteOrder.LITTLE_ENDIAN);
    }

    @Test
    void commandDefensivelyOwnsCompleteDigitalStateAndRejectsInvalidConstruction() {
        EnumMap<PlayerInputCommand.DigitalAction, PlayerInputCommand.DigitalState> states = states();
        PlayerInputCommand command = new PlayerInputCommand(1L, 0.0d, 0.0d, 0.0d, 0.0d, states);
        states.put(
                PlayerInputCommand.DigitalAction.JUMP,
                new PlayerInputCommand.DigitalState(9.0d, false, false, false));

        assertThat(command.digitalStates()).hasSize(PlayerInputCommand.DigitalAction.values().length);
        assertThat(command.digitalState(PlayerInputCommand.DigitalAction.JUMP).value()).isEqualTo(1.0d);
        assertThatThrownBy(() -> command.digitalStates().clear())
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> new PlayerInputCommand(-1L, 0, 0, 0, 0, states()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PlayerInputCommand(1L, Double.NaN, 0, 0, 0, states()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PlayerInputCommand.DigitalState(
                        Double.POSITIVE_INFINITY,
                        false,
                        false,
                        false))
                .isInstanceOf(IllegalArgumentException.class);

        EnumMap<PlayerInputCommand.DigitalAction, PlayerInputCommand.DigitalState> incomplete = states();
        incomplete.remove(PlayerInputCommand.DigitalAction.PAUSE);
        assertThatThrownBy(() -> new PlayerInputCommand(1L, 0, 0, 0, 0, incomplete))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void decodeRejectsMalformedValuesWithoutAdvancingSource() {
        assertMalformed(view -> view.putInt(0, 0));
        assertMalformed(view -> view.putShort(4, (short) 2));
        assertMalformed(view -> view.putShort(6, (short) 1));
        assertMalformed(view -> view.putLong(8, -1L));
        assertMalformed(view -> view.putLong(16, Double.doubleToRawLongBits(Double.NaN)));
        assertMalformed(view -> view.putLong(48, Double.doubleToRawLongBits(Double.NaN)));
        assertMalformed(view -> view.putShort(120, (short) (1 << 9)));
        assertMalformed(view -> view.putShort(122, (short) (1 << 9)));
        assertMalformed(view -> view.putShort(124, (short) (1 << 9)));

        ByteBuffer shortBuffer = ByteBuffer.allocate(PlayerInputCommandCodec.ENCODED_SIZE - 1);
        assertThatThrownBy(() -> PlayerInputCommandCodec.decode(shortBuffer))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(shortBuffer.position()).isZero();
    }

    @Test
    void encodeRejectsShortDestinationWithoutAdvancingIt() {
        ByteBuffer destination = ByteBuffer.allocate(PlayerInputCommandCodec.ENCODED_SIZE - 1);
        assertThatThrownBy(() -> PlayerInputCommandCodec.encode(fixture(1L), destination))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(destination.position()).isZero();
    }

    private static void assertMalformed(java.util.function.Consumer<ByteBuffer> mutation) {
        ByteBuffer buffer = ByteBuffer.allocate(PlayerInputCommandCodec.ENCODED_SIZE);
        PlayerInputCommandCodec.encode(fixture(42L), buffer);
        buffer.flip();
        ByteBuffer view = buffer.duplicate().order(ByteOrder.BIG_ENDIAN);
        mutation.accept(view);

        assertThatThrownBy(() -> PlayerInputCommandCodec.decode(buffer))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(buffer.position()).isZero();
    }

    private static PlayerInputCommand fixture(long tickId) {
        return new PlayerInputCommand(tickId, 0.25d, -0.5d, 3.0d, -4.0d, states());
    }

    private static EnumMap<PlayerInputCommand.DigitalAction, PlayerInputCommand.DigitalState> states() {
        EnumMap<PlayerInputCommand.DigitalAction, PlayerInputCommand.DigitalState> states =
                new EnumMap<>(PlayerInputCommand.DigitalAction.class);
        for (PlayerInputCommand.DigitalAction action : PlayerInputCommand.DigitalAction.values()) {
            int ordinal = action.ordinal();
            states.put(
                    action,
                    new PlayerInputCommand.DigitalState(
                            ordinal + 1.0d,
                            action == PlayerInputCommand.DigitalAction.JUMP,
                            action == PlayerInputCommand.DigitalAction.CROUCH,
                            action == PlayerInputCommand.DigitalAction.JUMP));
        }
        return states;
    }
}
