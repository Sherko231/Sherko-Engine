package com.samo.engine.render.opengl.internal;

import com.samo.engine.core.api.DebugFrame;
import java.util.List;
import java.util.Objects;

final class ReferenceSceneDrawExecutor {
    private static final DirectionalLight REFERENCE_DIRECTIONAL_LIGHT =
            new DirectionalLight(0.0f, -1.0f, -1.0f, 1.0f, 1.0f, 1.0f, 0.8f);

    private final OpenGlDrawBackend drawBackend;
    private final int programHandle;
    private final int vertexArrayHandle;
    private final DebugLineRenderer debugLineRenderer;
    private final ViewModelRenderer viewModelRenderer;
    private final PresentationMode presentationMode;

    ReferenceSceneDrawExecutor(
            OpenGlDrawBackend drawBackend,
            int programHandle,
            int vertexArrayHandle,
            DebugLineRenderer debugLineRenderer,
            ViewModelRenderer viewModelRenderer,
            PresentationMode presentationMode) {
        this.drawBackend = Objects.requireNonNull(drawBackend, "drawBackend");
        this.programHandle = programHandle;
        this.vertexArrayHandle = vertexArrayHandle;
        this.debugLineRenderer = Objects.requireNonNull(debugLineRenderer, "debugLineRenderer");
        this.viewModelRenderer = Objects.requireNonNull(viewModelRenderer, "viewModelRenderer");
        this.presentationMode = Objects.requireNonNull(presentationMode, "presentationMode");
    }

    int execute(
            List<DrawSubmission> orderedSubmissions,
            DebugFrame debugFrame,
            int framebufferWidth,
            int framebufferHeight) {
        List<DrawSubmission> submissions =
                Objects.requireNonNull(orderedSubmissions, "orderedSubmissions");
        DebugFrame diagnostics = Objects.requireNonNull(debugFrame, "debugFrame");

        int submittedDraws = 0;
        drawBackend.setViewport(0, 0, framebufferWidth, framebufferHeight);
        drawBackend.setFramebufferSrgbEnabled(presentationMode.framebufferSrgbEnabled());
        try {
            drawBackend.clearFrame(presentationMode);
            for (DrawSubmission submission : submissions) {
                drawMaterial(
                        submission.material(),
                        submission.viewportX(),
                        submission.viewportY(),
                        submission.viewportWidth(),
                        submission.viewportHeight());
                submittedDraws++;
            }
            debugLineRenderer.render(diagnostics, framebufferWidth, framebufferHeight);
            viewModelRenderer.render(framebufferWidth, framebufferHeight);
            return submittedDraws;
        } finally {
            drawBackend.setViewport(0, 0, framebufferWidth, framebufferHeight);
            drawBackend.setFramebufferSrgbEnabled(false);
        }
    }

    private void drawMaterial(RenderMaterialDescriptor material, int x, int y, int width, int height) {
        int materialProgram = programFor(material.shaderVariant());
        drawBackend.setViewport(x, y, width, height);
        drawBackend.applyMaterialState(material);
        for (MaterialTextureBinding textureBinding : material.textures()) {
            drawBackend.bindTextureAndSampler(
                    textureBinding.unit(),
                    textureBinding.textureHandle(),
                    textureBinding.samplerHandle());
        }
        drawBackend.setMaterialScalars(materialProgram, material.scalars());
        drawBackend.setDirectionalLight(materialProgram, REFERENCE_DIRECTIONAL_LIGHT);
        drawBackend.useProgram(materialProgram);
        drawBackend.bindVertexArray(vertexArrayHandle);
        try {
            drawBackend.drawIndexedTriangles(ReferenceRoomFixture.INDEX_COUNT);
        } finally {
            drawBackend.bindDefaultVertexArray();
            drawBackend.useDefaultProgram();
            for (MaterialTextureBinding textureBinding : material.textures()) {
                drawBackend.bindTextureAndSampler(textureBinding.unit(), 0, 0);
            }
        }
    }

    private int programFor(MaterialShaderVariant shaderVariant) {
        return switch (shaderVariant) {
            case TEXTURED_REFERENCE -> programHandle;
        };
    }
}
