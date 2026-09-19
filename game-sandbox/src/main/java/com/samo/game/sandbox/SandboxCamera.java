package com.samo.game.sandbox;

import com.samo.engine.core.api.CameraMatrices;
import com.samo.engine.core.api.PlayerInputCommand;
import org.joml.Matrix4f;
import org.joml.Vector3f;

final class SandboxCamera {
    static final float FIXED_STEP_SECONDS = 1.0f / 60.0f;
    static final float MOVE_SPEED_METERS_PER_SECOND = 2.0f;
    static final float LOOK_RADIANS_PER_UNIT = 0.0025f;
    static final float MAX_PITCH_RADIANS = (float) Math.toRadians(89.0);

    private final Vector3f position = new Vector3f(0.0f, 0.0f, 2.0f);
    private float yawRadians;
    private float pitchRadians;

    void apply(PlayerInputCommand command) {
        double moveX = command.moveX();
        double moveY = command.moveY();

        float forwardX = (float) Math.sin(yawRadians);
        float forwardZ = (float) -Math.cos(yawRadians);
        float rightX = (float) Math.cos(yawRadians);
        float rightZ = (float) Math.sin(yawRadians);

        double lengthSquared = moveX * moveX + moveY * moveY;
        double movementScale = lengthSquared > 1.0 ? 1.0 / Math.sqrt(lengthSquared) : 1.0;
        float step = MOVE_SPEED_METERS_PER_SECOND * FIXED_STEP_SECONDS;
        position.x += (float) ((rightX * moveX + forwardX * moveY) * movementScale * step);
        position.z += (float) ((rightZ * moveX + forwardZ * moveY) * movementScale * step);

        yawRadians = wrapYaw(yawRadians + (float) command.lookX() * LOOK_RADIANS_PER_UNIT);
        pitchRadians = clampPitch(
                pitchRadians - (float) command.lookY() * LOOK_RADIANS_PER_UNIT);
    }

    Matrix4f view(Matrix4f destination) {
        float cosPitch = (float) Math.cos(pitchRadians);
        Vector3f forward = new Vector3f(
                (float) Math.sin(yawRadians) * cosPitch,
                (float) Math.sin(pitchRadians),
                (float) -Math.cos(yawRadians) * cosPitch);
        return CameraMatrices.view(
                position,
                forward,
                new Vector3f(0.0f, 1.0f, 0.0f),
                destination);
    }

    Vector3f position(Vector3f destination) {
        return destination.set(position);
    }

    float yawRadians() {
        return yawRadians;
    }

    float pitchRadians() {
        return pitchRadians;
    }

    private static float clampPitch(float pitch) {
        return Math.max(-MAX_PITCH_RADIANS, Math.min(MAX_PITCH_RADIANS, pitch));
    }

    private static float wrapYaw(float yaw) {
        float twoPi = (float) (Math.PI * 2.0);
        float wrapped = yaw % twoPi;
        if (wrapped > Math.PI) {
            wrapped -= twoPi;
        } else if (wrapped < -Math.PI) {
            wrapped += twoPi;
        }
        return wrapped;
    }
}
