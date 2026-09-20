package com.samo.engine.render.opengl.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

class RenderMaterialDescriptorTest {
    @Test
    void retainsCompleteImmutableMaterialValueWithoutOwningResources() {

        MaterialTextureBinding binding = new MaterialTextureBinding(0, 31, 41);
        MaterialScalars scalars = new MaterialScalars(1.0f, 0.5f, 0.25f, 0.75f);
        RenderMaterialDescriptor material = new RenderMaterialDescriptor(MaterialShaderVariant.TEXTURED_REFERENCE, List.of(binding), scalars, MaterialBlendMode.ALPHA_BLEND,
            MaterialDepthMode.TEST_NO_WRITE, MaterialCullMode.NONE);

        assertEquals(MaterialShaderVariant.TEXTURED_REFERENCE, material.shaderVariant());
        assertEquals(List.of(binding), material.textures());
        assertEquals(scalars, material.scalars());
        assertEquals(MaterialBlendMode.ALPHA_BLEND, material.blendMode());
        assertEquals(MaterialDepthMode.TEST_NO_WRITE, material.depthMode());
        assertEquals(MaterialCullMode.NONE, material.cullMode());

    }

    @Test
    void mapsEveryMaterialStateModeDeterministically() {

        MaterialTextureBinding binding = new MaterialTextureBinding(0, 31, 41);

        RenderMaterialDescriptor opaqueBack = new RenderMaterialDescriptor(MaterialShaderVariant.TEXTURED_REFERENCE, List.of(binding), MaterialScalars.identity(),
            MaterialBlendMode.OPAQUE, MaterialDepthMode.TEST_WRITE, MaterialCullMode.BACK);
        OpenGlMaterialStatePolicy opaqueBackPolicy = OpenGlMaterialStatePolicy.from(opaqueBack);
        assertEquals(false, opaqueBackPolicy.blendEnabled());
        assertEquals(GL14.GL_FUNC_ADD, opaqueBackPolicy.blendEquation());
        assertEquals(GL11.GL_ONE, opaqueBackPolicy.blendSourceFactor());
        assertEquals(GL11.GL_ZERO, opaqueBackPolicy.blendDestinationFactor());
        assertEquals(true, opaqueBackPolicy.depthTestEnabled());
        assertEquals(true, opaqueBackPolicy.depthWriteEnabled());
        assertEquals(GL11.GL_LESS, opaqueBackPolicy.depthFunction());
        assertEquals(true, opaqueBackPolicy.cullEnabled());
        assertEquals(GL11.GL_BACK, opaqueBackPolicy.cullFace());
        assertEquals(GL11.GL_CCW, opaqueBackPolicy.frontFace());

        RenderMaterialDescriptor alphaFront = new RenderMaterialDescriptor(MaterialShaderVariant.TEXTURED_REFERENCE, List.of(binding), MaterialScalars.identity(),
            MaterialBlendMode.ALPHA_BLEND, MaterialDepthMode.TEST_NO_WRITE, MaterialCullMode.FRONT);
        OpenGlMaterialStatePolicy alphaFrontPolicy = OpenGlMaterialStatePolicy.from(alphaFront);
        assertEquals(true, alphaFrontPolicy.blendEnabled());
        assertEquals(GL11.GL_SRC_ALPHA, alphaFrontPolicy.blendSourceFactor());
        assertEquals(GL11.GL_ONE_MINUS_SRC_ALPHA, alphaFrontPolicy.blendDestinationFactor());
        assertEquals(true, alphaFrontPolicy.depthTestEnabled());
        assertEquals(false, alphaFrontPolicy.depthWriteEnabled());
        assertEquals(true, alphaFrontPolicy.cullEnabled());
        assertEquals(GL11.GL_FRONT, alphaFrontPolicy.cullFace());

        RenderMaterialDescriptor disabled = new RenderMaterialDescriptor(MaterialShaderVariant.TEXTURED_REFERENCE, List.of(binding), MaterialScalars.identity(),
            MaterialBlendMode.OPAQUE, MaterialDepthMode.DISABLED, MaterialCullMode.NONE);
        OpenGlMaterialStatePolicy disabledPolicy = OpenGlMaterialStatePolicy.from(disabled);
        assertEquals(false, disabledPolicy.depthTestEnabled());
        assertEquals(false, disabledPolicy.depthWriteEnabled());
        assertEquals(false, disabledPolicy.cullEnabled());

    }

    @Test
    void rejectsInvalidBindingsBeforeDrawStateCanExist() {

        assertThrows(IllegalArgumentException.class, () -> new MaterialTextureBinding(-1, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> new MaterialTextureBinding(0, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> new MaterialTextureBinding(0, 1, 0));
        assertThrows(IllegalArgumentException.class,
            () -> new RenderMaterialDescriptor(MaterialShaderVariant.TEXTURED_REFERENCE, List.of(new MaterialTextureBinding(0, 1, 1), new MaterialTextureBinding(1, 2, 2)),
                MaterialScalars.identity(), MaterialBlendMode.OPAQUE, MaterialDepthMode.TEST_WRITE, MaterialCullMode.BACK));

    }

    @Test
    void rejectsNonFiniteOrOutOfRangeScalarParameters() {

        assertThrows(IllegalArgumentException.class, () -> new MaterialScalars(Float.NaN, 1.0f, 1.0f, 1.0f));
        assertThrows(IllegalArgumentException.class, () -> new MaterialScalars(1.1f, 1.0f, 1.0f, 1.0f));
        assertThrows(IllegalArgumentException.class, () -> new MaterialScalars(1.0f, -0.1f, 1.0f, 1.0f));
        assertThrows(IllegalArgumentException.class, () -> new MaterialScalars(1.0f, 1.0f, Float.POSITIVE_INFINITY, 1.0f));

    }
}
