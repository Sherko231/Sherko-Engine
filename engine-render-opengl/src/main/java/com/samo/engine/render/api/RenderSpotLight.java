package com.samo.engine.render.api;

/**
 * Immutable unshadowed spot-light submission in canonical D-041 world space.
 *
 * <p>
 * The stored direction is normalized and describes the direction in which light rays travel
 * away from the light. Cone angles are radians and satisfy
 * {@code 0 <= innerConeRadians < outerConeRadians < PI/2}.
 */
public record RenderSpotLight(float positionX, float positionY, float positionZ, float directionX, float directionY, float directionZ, float red, float green, float blue,
    float intensity, float rangeMeters, float innerConeRadians, float outerConeRadians) implements RenderLocalLight {

    public RenderSpotLight {
        RenderPointLight.requireFinite("positionX", positionX);
        RenderPointLight.requireFinite("positionY", positionY);
        RenderPointLight.requireFinite("positionZ", positionZ);
        RenderPointLight.requireFinite("directionX", directionX);
        RenderPointLight.requireFinite("directionY", directionY);
        RenderPointLight.requireFinite("directionZ", directionZ);

        float lengthSquared = directionX * directionX + directionY * directionY + directionZ * directionZ;
        if (!(lengthSquared > 0.0f) || !Float.isFinite(lengthSquared)) {
            throw new IllegalArgumentException("direction must be finite and non-zero");
        }
        float inverseLength = 1.0f / (float) Math.sqrt(lengthSquared);
        directionX *= inverseLength;
        directionY *= inverseLength;
        directionZ *= inverseLength;

        RenderPointLight.requireUnit("red", red);
        RenderPointLight.requireUnit("green", green);
        RenderPointLight.requireUnit("blue", blue);
        RenderPointLight.requireUnit("intensity", intensity);
        RenderPointLight.requirePositiveFinite("rangeMeters", rangeMeters);

        RenderPointLight.requireFinite("innerConeRadians", innerConeRadians);
        RenderPointLight.requireFinite("outerConeRadians", outerConeRadians);
        float halfPi = (float) (Math.PI * 0.5);
        if (innerConeRadians < 0.0f || !(innerConeRadians < outerConeRadians) || !(outerConeRadians < halfPi)) {
            throw new IllegalArgumentException("cone angles must satisfy 0 <= innerConeRadians < outerConeRadians < PI/2");
        }
    }
}
