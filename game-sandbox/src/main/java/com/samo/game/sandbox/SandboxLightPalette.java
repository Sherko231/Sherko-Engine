package com.samo.game.sandbox;

import com.samo.engine.assets.api.MaterialAsset;
import java.util.Objects;

record SandboxLightPalette(float pointRed, float pointGreen, float pointBlue, float spotRed, float spotGreen, float spotBlue) {
    static SandboxLightPalette from(MaterialAsset material) {

        MaterialAsset value = Objects.requireNonNull(material, "material");
        return new SandboxLightPalette(value.redMultiplier(), value.greenMultiplier(), value.blueMultiplier(), value.blueMultiplier(), value.redMultiplier(),
            value.greenMultiplier());

    }
}
