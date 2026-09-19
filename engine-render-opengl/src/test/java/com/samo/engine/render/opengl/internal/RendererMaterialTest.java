package com.samo.engine.render.opengl.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class RendererMaterialTest {
    @Test
    void retainsCompleteImmutableMaterialValueWithoutOwningResources() {
        MaterialTextureBinding binding = new MaterialTextureBinding(0, 31, 41);
        MaterialScalars scalars = new MaterialScalars(1.0f, 0.5f, 0.25f, 0.75f);
        RendererMaterial material = new RendererMaterial(
                MaterialShaderVariant.TEXTURED_REFERENCE,
                List.of(binding),
                scalars,
                MaterialBlendMode.ALPHA_BLEND,
                MaterialDepthMode.TEST_NO_WRITE,
                MaterialCullMode.NONE);

        assertEquals(MaterialShaderVariant.TEXTURED_REFERENCE, material.shaderVariant());
        assertEquals(List.of(binding), material.textures());
        assertEquals(scalars, material.scalars());
        assertEquals(MaterialBlendMode.ALPHA_BLEND, material.blendMode());
        assertEquals(MaterialDepthMode.TEST_NO_WRITE, material.depthMode());
        assertEquals(MaterialCullMode.NONE, material.cullMode());
    }

    @Test
    void rejectsInvalidBindingsBeforeDrawStateCanExist() {
        assertThrows(IllegalArgumentException.class, () -> new MaterialTextureBinding(-1, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> new MaterialTextureBinding(0, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> new MaterialTextureBinding(0, 1, 0));
        assertThrows(
                IllegalArgumentException.class,
                () -> new RendererMaterial(
                        MaterialShaderVariant.TEXTURED_REFERENCE,
                        List.of(
                                new MaterialTextureBinding(0, 1, 1),
                                new MaterialTextureBinding(1, 2, 2)),
                        MaterialScalars.identity(),
                        MaterialBlendMode.OPAQUE,
                        MaterialDepthMode.TEST_WRITE,
                        MaterialCullMode.BACK));
    }

    @Test
    void rejectsNonFiniteOrOutOfRangeScalarParameters() {
        assertThrows(IllegalArgumentException.class, () -> new MaterialScalars(Float.NaN, 1.0f, 1.0f, 1.0f));
        assertThrows(IllegalArgumentException.class, () -> new MaterialScalars(1.1f, 1.0f, 1.0f, 1.0f));
        assertThrows(IllegalArgumentException.class, () -> new MaterialScalars(1.0f, -0.1f, 1.0f, 1.0f));
        assertThrows(
                IllegalArgumentException.class,
                () -> new MaterialScalars(1.0f, 1.0f, Float.POSITIVE_INFINITY, 1.0f));
    }
}
