package com.samo.engine.render.api;

/**
 * Immutable renderer-facing local-light submission data for one frame.
 *
 * <p>
 * Implementations use canonical D-041 world-space meters and linear bounded color/intensity
 * values. They own no native resources and carry no world/entity identity.
 */
public sealed interface RenderLocalLight permits RenderPointLight, RenderSpotLight {
    float positionX();

    float positionY();

    float positionZ();

    float red();

    float green();

    float blue();

    float intensity();

    float rangeMeters();
}
