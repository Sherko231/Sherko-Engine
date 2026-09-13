package com.samo.engine.core.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import org.junit.jupiter.api.Test;

class PlayerInputCommandReplayTest {
    @Test
    void encodedRecordedSequenceReplaysToIndependentExpectedHeadlessState() {
        List<PlayerInputCommand> recorded = List.of(
                command(0L, 1.0d, 0.0d, 2.0d, 0.0d, true, true, false),
                command(1L, 1.0d, 0.5d, 0.0d, -1.0d, false, true, false),
                command(2L, 0.0d, 0.5d, -0.5d, 0.0d, false, false, true));

        ByteBuffer bytes = ByteBuffer.allocate(recorded.size() * PlayerInputCommandCodec.ENCODED_SIZE);
        for (PlayerInputCommand command : recorded) {
            PlayerInputCommandCodec.encode(command, bytes);
        }
        bytes.flip();

        List<PlayerInputCommand> decoded = new ArrayList<>();
        while (bytes.hasRemaining()) {
            decoded.add(PlayerInputCommandCodec.decode(bytes));
        }

        HeadlessState original = replay(recorded);
        HeadlessState replayed = replay(decoded);

        assertThat(decoded).containsExactlyElementsOf(recorded);
        assertThat(replayed).isEqualTo(original);
        assertThat(replayed).isEqualTo(new HeadlessState(2.0d, 1.0d, 1.5d, -1.0d, 1, 1, 2L));
    }

    private static HeadlessState replay(List<PlayerInputCommand> commands) {
        double x = 0.0d;
        double y = 0.0d;
        double yaw = 0.0d;
        double pitch = 0.0d;
        int jumpPresses = 0;
        int jumpReleases = 0;
        long lastTick = -1L;

        for (PlayerInputCommand command : commands) {
            x += command.moveX();
            y += command.moveY();
            yaw += command.lookX();
            pitch += command.lookY();
            PlayerInputCommand.DigitalState jump = command.digitalState(PlayerInputCommand.DigitalAction.JUMP);
            if (jump.pressed()) {
                jumpPresses++;
            }
            if (jump.released()) {
                jumpReleases++;
            }
            lastTick = command.tickId();
        }
        return new HeadlessState(x, y, yaw, pitch, jumpPresses, jumpReleases, lastTick);
    }

    private static PlayerInputCommand command(
            long tickId,
            double moveX,
            double moveY,
            double lookX,
            double lookY,
            boolean jumpPressed,
            boolean jumpHeld,
            boolean jumpReleased) {
        EnumMap<PlayerInputCommand.DigitalAction, PlayerInputCommand.DigitalState> states =
                new EnumMap<>(PlayerInputCommand.DigitalAction.class);
        for (PlayerInputCommand.DigitalAction action : PlayerInputCommand.DigitalAction.values()) {
            if (action == PlayerInputCommand.DigitalAction.JUMP) {
                states.put(action, new PlayerInputCommand.DigitalState(jumpHeld ? 1.0d : 0.0d, jumpPressed, jumpHeld, jumpReleased));
            } else {
                states.put(action, new PlayerInputCommand.DigitalState(0.0d, false, false, false));
            }
        }
        return new PlayerInputCommand(tickId, moveX, moveY, lookX, lookY, states);
    }

    private record HeadlessState(
            double x,
            double y,
            double yaw,
            double pitch,
            int jumpPresses,
            int jumpReleases,
            long lastTick) {
    }
}
