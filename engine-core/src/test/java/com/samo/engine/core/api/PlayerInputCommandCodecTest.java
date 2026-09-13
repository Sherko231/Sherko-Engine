package com.samo.engine.core.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.EnumMap;
import java.util.Map;
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

        buffer.position(start);
        PlayerInputCommand decoded = PlayerInputCommandCodec.decode(buffer);
        assertThat(decoded).isEqualTo(command);
        assertThat(buffer.position()).isEqualTo(start + 126);
        assertThat(buffer.order()).isEqualTo(ByteOrder.LITTLE_ENDIAN);
    }

    @Test
    void commandDefensivelyOwnsCompleteDigitalState() {
        EnumMap<PlayerInputCommand.DigitalAction, PlayerInputCommand.DigitalState> states = states();
        PlayerInputCommand command = new PlayerInputCommand(1L, 0.0d, 0.0d, 0.0d, 0.0d, states);
        states.put(PlayerInputCommand.DigitalAction.JUMP, new PlayerInputCommand.DigitalState(9.0d, false, false, false));

        assertThat(command.digitalStates()).hasSize(9);
        assertThat(command.digitalState(PlayerInputCommand.DigitalAction.JUMP).value()).isEqualTo(1.0d);
        assertThatThrownBy(() -> command.digitalStates().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> new PlayerInputCommand(1L, Double.NaN, 0, 0, 0, states()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void decodeRejectsMalformedValuesWithoutAdvancingSource() {
        assertMalformed(0, view -> view.putInt(0, 0));
        assertMalformed(4, view -> view.putShort(4, (short) 2));
        assertMalformed(6, view -> view.putShort(6, (short) 1));
        assertMalformed(8, view -> view.putLong(8, -1L));
        assertMalformed(16, view -> view.putLong(16, Double.doubleToRawLongBits(Double.NaN)));
        assertMalformed(120, view -> view.putShort(120, (short) (1 << 9)));

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

    private static void assertMalformed(int expectedPosition, java.util.function.Consumer<ByteBuffer> mutation) {
        ByteBuffer buffer = ByteBuffer.allocate(PlayerInputCommandCodec.ENCODED_SIZE);
        PlayerInputCommandCodec.encode(fixture(42L), buffer);
        buffer.flip();
        ByteBuffer view = buffer.duplicate().order(ByteOrder.BIG_ENDIAN);
        mutation.accept(view);

        assertThatThrownBy(() -> PlayerInputCommandCodec.decode(buffer))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(buffer.position()).isZero();
        assertThat(expectedPosition).isGreaterThanOrEqualTo(0);
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
