package com.samo.engine.platform.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.samo.engine.core.api.PlayerInputCommand;
import java.util.EnumMap;
import org.junit.jupiter.api.Test;

class PlayerInputCommandSamplerTest {
    @Test
    void retainsPendingEdgesAndLookUntilNextTickThenConsumesThemOnce() {
        PlayerInputCommandSampler sampler = new PlayerInputCommandSampler();

        sampler.submit(snapshot(10L, 0.0d, 0.5d, 2.0d, 0.0d, true, false, true, 0.0d));
        sampler.submit(snapshot(11L, 0.0d, 1.0d, 1.5d, -2.0d, false, false, false, 0.0d));

        PlayerInputCommand first = sampler.nextCommand(100L);
        PlayerInputCommand second = sampler.nextCommand(101L);

        assertThat(first.moveY()).isEqualTo(1.0d);
        assertThat(first.lookX()).isEqualTo(3.5d);
        assertThat(first.lookY()).isEqualTo(-2.0d);
        assertThat(first.digitalState(PlayerInputCommand.DigitalAction.JUMP).pressed()).isTrue();
        assertThat(first.digitalState(PlayerInputCommand.DigitalAction.JUMP).held()).isFalse();
        assertThat(first.digitalState(PlayerInputCommand.DigitalAction.JUMP).released()).isTrue();

        assertThat(second.moveY()).isEqualTo(1.0d);
        assertThat(second.lookX()).isZero();
        assertThat(second.lookY()).isZero();
        assertThat(second.digitalState(PlayerInputCommand.DigitalAction.JUMP).pressed()).isFalse();
        assertThat(second.digitalState(PlayerInputCommand.DigitalAction.JUMP).released()).isFalse();
    }

    @Test
    void repeatsLatestHeldAndScalarStateAcrossTicks() {
        PlayerInputCommandSampler sampler = new PlayerInputCommandSampler();
        sampler.submit(snapshot(1L, 0.25d, -0.75d, 0.0d, 0.0d, true, true, false, 2.0d));

        PlayerInputCommand first = sampler.nextCommand(7L);
        PlayerInputCommand second = sampler.nextCommand(8L);

        assertThat(first.moveX()).isEqualTo(0.25d);
        assertThat(second.moveX()).isEqualTo(0.25d);
        assertThat(first.digitalState(PlayerInputCommand.DigitalAction.JUMP).value()).isEqualTo(2.0d);
        assertThat(second.digitalState(PlayerInputCommand.DigitalAction.JUMP).value()).isEqualTo(2.0d);
        assertThat(first.digitalState(PlayerInputCommand.DigitalAction.JUMP).held()).isTrue();
        assertThat(second.digitalState(PlayerInputCommand.DigitalAction.JUMP).held()).isTrue();
        assertThat(first.digitalState(PlayerInputCommand.DigitalAction.JUMP).pressed()).isTrue();
        assertThat(second.digitalState(PlayerInputCommand.DigitalAction.JUMP).pressed()).isFalse();
    }

    @Test
    void rejectsOutOfOrderFrameAndTickWithoutConsumingPendingState() {
        PlayerInputCommandSampler sampler = new PlayerInputCommandSampler();
        sampler.submit(snapshot(5L, 0.0d, 0.0d, 4.0d, 0.0d, true, false, true, 0.0d));

        assertThatThrownBy(() -> sampler.submit(snapshot(5L, 0, 0, 1, 0, false, false, false, 0)))
                .isInstanceOf(IllegalArgumentException.class);

        PlayerInputCommand command = sampler.nextCommand(9L);
        assertThat(command.lookX()).isEqualTo(4.0d);
        assertThat(command.digitalState(PlayerInputCommand.DigitalAction.JUMP).pressed()).isTrue();
        assertThat(command.digitalState(PlayerInputCommand.DigitalAction.JUMP).released()).isTrue();

        assertThatThrownBy(() -> sampler.nextCommand(9L)).isInstanceOf(IllegalArgumentException.class);

        sampler.submit(snapshot(6L, 0, 0, 2.0d, 0, false, false, false, 0));
        PlayerInputCommand after = sampler.nextCommand(10L);
        assertThat(after.lookX()).isEqualTo(2.0d);
    }

    @Test
    void requiresAFrameBeforeFirstCommand() {
        PlayerInputCommandSampler sampler = new PlayerInputCommandSampler();
        assertThatThrownBy(() -> sampler.nextCommand(0L)).isInstanceOf(IllegalStateException.class);
    }

    private static InputActionSnapshot snapshot(
            long frameId,
            double moveX,
            double moveY,
            double lookX,
            double lookY,
            boolean jumpPressed,
            boolean jumpHeld,
            boolean jumpReleased,
            double jumpValue) {
        EnumMap<InputAction, InputActionState> states = new EnumMap<>(InputAction.class);
        for (InputAction action : InputAction.values()) {
            if (action == InputAction.MOVE) {
                states.put(action, new InputActionState(action, false, moveX != 0.0d || moveY != 0.0d, false, 0.0d, moveX, moveY));
            } else if (action == InputAction.LOOK) {
                states.put(action, new InputActionState(action, false, lookX != 0.0d || lookY != 0.0d, false, 0.0d, lookX, lookY));
            } else if (action == InputAction.JUMP) {
                states.put(action, new InputActionState(action, jumpPressed, jumpHeld, jumpReleased, jumpValue, 0.0d, 0.0d));
            } else {
                states.put(action, new InputActionState(action, false, false, false, 0.0d, 0.0d, 0.0d));
            }
        }
        return new InputActionSnapshot(frameId, states);
    }
}
