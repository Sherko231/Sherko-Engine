package com.samo.engine.render.api;

/**
 * Immutable unshadowed point-light submission in canonical D-041 world space.
 *
 * @param positionX world-space X position in meters
 * @param positionY world-space Y position in meters
 * @param positionZ world-space Z position in meters
 * @param red linear red component in [0,1]
 * @param green linear green component in [0,1]
 * @param blue linear blue component in [0,1]
 * @param intensity bounded SDR intensity in [0,1]
 * @param rangeMeters strictly positive attenuation range in meters
 */
public record RenderPointLight(
        float positionX,
        float positionY,
        float positionZ,
        float red,
        float green,
        float blue,
        float intensity,
        float rangeMeters) implements RenderLocalLight {

    public RenderPointLight {
        requireFinite("positionX", positionX);
        requireFinite("positionY", positionY);
        requireFinite("positionZ", positionZ);
        requireUnit("red", red);
        requireUnit("green", green);
        requireUnit("blue", blue);
        requireUnit("intensity", intensity);
        requirePositiveFinite("rangeMeters", rangeMeters);
    }

    static void requireFinite(String name, float value) {
        if (!Float.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite");
        }
    }

    static void requireUnit(String name, float value) {
        if (!Float.isFinite(value) || value < 0.0f || value > 1.0f) {
            throw new IllegalArgumentException(name + " must be finite and within [0,1]");
        }
    }

    static void requirePositiveFinite(String name, float value) {
        if (!Float.isFinite(value) || !(value > 0.0f)) {
            throw new IllegalArgumentException(name + " must be finite and positive");
        }
    }
}
