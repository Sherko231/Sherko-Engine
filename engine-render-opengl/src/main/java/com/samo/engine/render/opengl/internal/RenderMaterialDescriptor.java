package com.samo.engine.render.opengl.internal;

import java.util.List;
import java.util.Objects;

record RenderMaterialDescriptor(MaterialShaderVariant shaderVariant, List<MaterialTextureBinding> textures, MaterialScalars scalars, MaterialBlendMode blendMode,
    MaterialDepthMode depthMode, MaterialCullMode cullMode) {
    RenderMaterialDescriptor {

        Objects.requireNonNull(shaderVariant, "shaderVariant");
        Objects.requireNonNull(textures, "textures");
        Objects.requireNonNull(scalars, "scalars");
        Objects.requireNonNull(blendMode, "blendMode");
        Objects.requireNonNull(depthMode, "depthMode");
        Objects.requireNonNull(cullMode, "cullMode");
        textures = List.copyOf(textures);
        if (textures.size() != 1 || textures.getFirst().unit() != 0) {
            throw new IllegalArgumentException("TEXTURED_REFERENCE requires exactly one texture/sampler binding at unit 0");
        }

    }
}

enum MaterialShaderVariant {
    TEXTURED_REFERENCE
}

enum MaterialBlendMode {
    OPAQUE, ALPHA_BLEND
}

enum MaterialDepthMode {
    TEST_WRITE, TEST_NO_WRITE, DISABLED
}

enum MaterialCullMode {
    BACK, FRONT, NONE
}

record MaterialTextureBinding(int unit, int textureHandle, int samplerHandle) {
    MaterialTextureBinding {

        if (unit < 0) {
            throw new IllegalArgumentException("texture unit must be non-negative");
        }
        if (textureHandle <= 0) {
            throw new IllegalArgumentException("textureHandle must be positive");
        }
        if (samplerHandle <= 0) {
            throw new IllegalArgumentException("samplerHandle must be positive");
        }

    }
}

record MaterialScalars(float redMultiplier, float greenMultiplier, float blueMultiplier, float alphaMultiplier) {
    MaterialScalars {

        requireUnit("redMultiplier", redMultiplier);
        requireUnit("greenMultiplier", greenMultiplier);
        requireUnit("blueMultiplier", blueMultiplier);
        requireUnit("alphaMultiplier", alphaMultiplier);

    }

    static MaterialScalars identity() {

        return new MaterialScalars(1.0f, 1.0f, 1.0f, 1.0f);

    }

    private static void requireUnit(String name, float value) {

        if (!Float.isFinite(value) || value < 0.0f || value > 1.0f) {
            throw new IllegalArgumentException(name + " must be finite and within [0,1]");
        }

    }
}
