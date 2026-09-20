package com.samo.engine.render.opengl.internal;

import com.samo.engine.platform.api.OpenGlThreadGuard;
import java.util.Objects;

final class UniformBlockLayoutVerifier {
    private UniformBlockLayoutVerifier() {
    }

    static void verify(int programHandle, OpenGlThreadGuard threadGuard, OpenGlUniformBlockReflectionBackend backend) {
        OpenGlThreadGuard guard = Objects.requireNonNull(threadGuard, "threadGuard");
        OpenGlUniformBlockReflectionBackend reflection = Objects.requireNonNull(backend, "backend");
        guard.assertOwnerThread();

        verifyBlock(programHandle, CameraMatricesUniformBlock.GLSL_BLOCK_NAME, CameraMatricesUniformBlock.SIZE_BYTES, CameraMatricesUniformBlock.BINDING, reflection);
        verifyBlock(programHandle, FramebufferMetricsUniformBlock.GLSL_BLOCK_NAME, FramebufferMetricsUniformBlock.SIZE_BYTES, FramebufferMetricsUniformBlock.BINDING, reflection);
        verifyBlock(programHandle, LocalLightUniformBlock.GLSL_BLOCK_NAME, LocalLightUniformBlock.SIZE_BYTES, LocalLightUniformBlock.BINDING, reflection);
    }

    static void verifyCameraOnly(int programHandle, OpenGlThreadGuard threadGuard, OpenGlUniformBlockReflectionBackend backend) {
        OpenGlThreadGuard guard = Objects.requireNonNull(threadGuard, "threadGuard");
        OpenGlUniformBlockReflectionBackend reflection = Objects.requireNonNull(backend, "backend");
        guard.assertOwnerThread();
        verifyBlock(programHandle, CameraMatricesUniformBlock.GLSL_BLOCK_NAME, CameraMatricesUniformBlock.SIZE_BYTES, CameraMatricesUniformBlock.BINDING, reflection);
    }

    private static void verifyBlock(int programHandle, String blockName, int expectedSize, int expectedBinding, OpenGlUniformBlockReflectionBackend backend) {
        int blockIndex = backend.uniformBlockIndex(programHandle, blockName);
        if (blockIndex == GL_INVALID_INDEX) {
            throw new IllegalStateException("Missing GLSL uniform block: " + blockName);
        }

        int actualSize = backend.uniformBlockDataSize(programHandle, blockIndex);
        if (actualSize != expectedSize) {
            throw new IllegalStateException("GLSL uniform block size mismatch for " + blockName + ": expected=" + expectedSize + ", actual=" + actualSize);
        }

        int actualBinding = backend.uniformBlockBinding(programHandle, blockIndex);
        if (actualBinding != expectedBinding) {
            throw new IllegalStateException("GLSL uniform block binding mismatch for " + blockName + ": expected=" + expectedBinding + ", actual=" + actualBinding);
        }
    }

    private static final int GL_INVALID_INDEX = -1;
}
