package com.samo.engine.assets.api;

import java.util.Objects;
import java.util.regex.Pattern;

/** Immutable backend-neutral runtime material values used by development loading/reload. */
public record MaterialAsset(String shaderKey, float redMultiplier, float greenMultiplier, float blueMultiplier, float alphaMultiplier) {
    private static final Pattern SHADER_KEY = Pattern.compile("[a-z][a-z0-9._-]{0,127}");

    public MaterialAsset {

        Objects.requireNonNull(shaderKey, "shaderKey");
        if (!SHADER_KEY.matcher(shaderKey).matches()) {
            throw new IllegalArgumentException("shaderKey must be canonical lowercase logical shader key");
        }
        requireUnit("redMultiplier", redMultiplier);
        requireUnit("greenMultiplier", greenMultiplier);
        requireUnit("blueMultiplier", blueMultiplier);
        requireUnit("alphaMultiplier", alphaMultiplier);

    }

    private static void requireUnit(String name, float value) {

        if (!Float.isFinite(value) || value < 0.0f || value > 1.0f) {
            throw new IllegalArgumentException(name + " must be finite and within [0,1]");
        }

    }
}
