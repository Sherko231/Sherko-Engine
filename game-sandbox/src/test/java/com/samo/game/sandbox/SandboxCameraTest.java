package com.samo.game.sandbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.samo.engine.core.api.PlayerInputCommand;
import java.util.EnumMap;
import java.util.Map;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class SandboxCameraTest {
    private static final float EPSILON = 1.0e-5f;

    @Test
    void forwardMovementUsesCanonicalNegativeZAtZeroYaw() {
        SandboxCamera camera = new SandboxCamera();

        camera.apply(command(1L, 0.0, 1.0, 0.0, 0.0));

        Vector3f position = camera.position(new Vector3f());
        assertEquals(0.0f, position.x, EPSILON);
        assertEquals(0.0f, position.y, EPSILON);
        assertEquals(2.0f - SandboxCamera.MOVE_SPEED_METERS_PER_SECOND * SandboxCamera.FIXED_STEP_SECONDS, position.z, EPSILON);
    }

    @Test
    void yawRotatesForwardMovementTowardPositiveX() {
        SandboxCamera camera = new SandboxCamera();
        double lookUnits = (Math.PI * 0.5) / SandboxCamera.LOOK_RADIANS_PER_UNIT;
        camera.apply(command(1L, 0.0, 0.0, lookUnits, 0.0));

        camera.apply(command(2L, 0.0, 1.0, 0.0, 0.0));

        Vector3f position = camera.position(new Vector3f());
        assertTrue(position.x > 0.03f);
        assertEquals(2.0f, position.z, 2.0e-4f);
    }

    @Test
    void pitchIsBoundedAndChangesSubmittedView() {
        SandboxCamera camera = new SandboxCamera();
        Matrix4f before = camera.view(new Matrix4f());

        camera.apply(command(1L, 0.0, 0.0, 0.0, -1_000_000.0));
        Matrix4f after = camera.view(new Matrix4f());

        assertEquals(SandboxCamera.MAX_PITCH_RADIANS, camera.pitchRadians(), EPSILON);
        assertTrue(Math.abs(before.m12() - after.m12()) > 0.5f);
    }

    @Test
    void diagonalMovementIsNormalized() {
        SandboxCamera camera = new SandboxCamera();

        camera.apply(command(1L, 1.0, 1.0, 0.0, 0.0));

        Vector3f position = camera.position(new Vector3f());
        float dx = position.x;
        float dz = position.z - 2.0f;
        float distance = (float) Math.sqrt(dx * dx + dz * dz);
        assertEquals(SandboxCamera.MOVE_SPEED_METERS_PER_SECOND * SandboxCamera.FIXED_STEP_SECONDS, distance, EPSILON);
    }

    private static PlayerInputCommand command(long tick, double moveX, double moveY, double lookX, double lookY) {
        Map<PlayerInputCommand.DigitalAction, PlayerInputCommand.DigitalState> states = new EnumMap<>(PlayerInputCommand.DigitalAction.class);
        for (PlayerInputCommand.DigitalAction action : PlayerInputCommand.DigitalAction.values()) {
            states.put(action, new PlayerInputCommand.DigitalState(0.0, false, false, false));
        }
        return new PlayerInputCommand(tick, moveX, moveY, lookX, lookY, states);
    }
}
