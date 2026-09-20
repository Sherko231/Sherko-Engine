package com.samo.engine.render.opengl.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class DrawSubmissionSorterTest {
    private final DrawSubmissionSorter sorter = new DrawSubmissionSorter();

    @Test
    void groupsOpaqueByProgramMaterialMeshBeforeTransparent() {
        RenderMaterialDescriptor opaque = material(MaterialBlendMode.OPAQUE);
        RenderMaterialDescriptor transparent = material(MaterialBlendMode.ALPHA_BLEND);

        DrawSubmission opaqueLaterKey = submission(opaque, 2, 1, 1, 1.0f, 3);
        DrawSubmission transparentFar = submission(transparent, 0, 0, 0, 8.0f, 4);
        DrawSubmission opaqueFirstKey = submission(opaque, 1, 4, 9, 3.0f, 2);
        DrawSubmission opaqueMiddleKey = submission(opaque, 2, 0, 5, 2.0f, 1);

        List<DrawSubmission> ordered = sorter.sort(List.of(
                transparentFar,
                opaqueLaterKey,
                opaqueFirstKey,
                opaqueMiddleKey));

        assertEquals(
                List.of(opaqueFirstKey, opaqueMiddleKey, opaqueLaterKey, transparentFar),
                ordered);
    }

    @Test
    void sortsTransparentBackToFrontThenUsesStableKeysAndSequence() {
        RenderMaterialDescriptor transparent = material(MaterialBlendMode.ALPHA_BLEND);

        DrawSubmission near = submission(transparent, 0, 0, 0, 2.0f, 5);
        DrawSubmission far = submission(transparent, 9, 9, 9, 10.0f, 4);
        DrawSubmission tieHigherMaterial = submission(transparent, 0, 2, 0, 5.0f, 3);
        DrawSubmission tieLowerMaterialLaterSequence = submission(transparent, 0, 1, 0, 5.0f, 8);
        DrawSubmission tieLowerMaterialEarlierSequence = submission(transparent, 0, 1, 0, 5.0f, 1);

        List<DrawSubmission> ordered = sorter.sort(List.of(
                near,
                tieLowerMaterialLaterSequence,
                far,
                tieHigherMaterial,
                tieLowerMaterialEarlierSequence));

        assertEquals(
                List.of(
                        far,
                        tieLowerMaterialEarlierSequence,
                        tieLowerMaterialLaterSequence,
                        tieHigherMaterial,
                        near),
                ordered);
    }

    @Test
    void doesNotMutateSourceCollectionOrMaterialIdentity() {
        RenderMaterialDescriptor opaque = material(MaterialBlendMode.OPAQUE);
        DrawSubmission later = submission(opaque, 2, 0, 0, 1.0f, 1);
        DrawSubmission earlier = submission(opaque, 1, 0, 0, 1.0f, 0);
        ArrayList<DrawSubmission> source = new ArrayList<>(List.of(later, earlier));

        List<DrawSubmission> ordered = sorter.sort(source);

        assertEquals(List.of(later, earlier), source);
        assertEquals(List.of(earlier, later), ordered);
        assertSame(opaque, ordered.getFirst().material());
        assertSame(opaque, ordered.getLast().material());
    }

    @Test
    void validatesOrderingInputsBeforeSorting() {
        RenderMaterialDescriptor opaque = material(MaterialBlendMode.OPAQUE);

        assertThrows(
                IllegalArgumentException.class,
                () -> new DrawSubmission(opaque, -1, 0, 0, 0.0f, 0, 0, 0, 10, 10));
        assertThrows(
                IllegalArgumentException.class,
                () -> new DrawSubmission(opaque, 0, 0, 0, Float.NaN, 0, 0, 0, 10, 10));
        assertThrows(
                IllegalArgumentException.class,
                () -> new DrawSubmission(opaque, 0, 0, 0, 0.0f, -1, 0, 0, 10, 10));
        assertThrows(
                IllegalArgumentException.class,
                () -> new DrawSubmission(opaque, 0, 0, 0, 0.0f, 0, 0, 0, 0, 10));
    }

    @Test
    void emptyAndSingletonInputsRemainDeterministic() {
        RenderMaterialDescriptor opaque = material(MaterialBlendMode.OPAQUE);
        DrawSubmission only = submission(opaque, 0, 0, 0, 0.0f, 0);

        assertEquals(List.of(), sorter.sort(List.of()));
        assertEquals(List.of(only), sorter.sort(List.of(only)));
    }

    private static DrawSubmission submission(
            RenderMaterialDescriptor material,
            int programKey,
            int materialKey,
            int meshKey,
            float cameraDepth,
            int sequence) {
        return new DrawSubmission(
                material,
                programKey,
                materialKey,
                meshKey,
                cameraDepth,
                sequence,
                0,
                0,
                100,
                100);
    }

    private static RenderMaterialDescriptor material(MaterialBlendMode blendMode) {
        return new RenderMaterialDescriptor(
                MaterialShaderVariant.TEXTURED_REFERENCE,
                List.of(new MaterialTextureBinding(0, 31, 41)),
                MaterialScalars.identity(),
                blendMode,
                blendMode == MaterialBlendMode.OPAQUE
                        ? MaterialDepthMode.TEST_WRITE
                        : MaterialDepthMode.TEST_NO_WRITE,
                MaterialCullMode.BACK);
    }
}
