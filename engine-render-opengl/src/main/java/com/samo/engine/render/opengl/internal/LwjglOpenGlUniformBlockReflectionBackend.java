package com.samo.engine.render.opengl.internal;

import org.lwjgl.opengl.GL31;

final class LwjglOpenGlUniformBlockReflectionBackend implements OpenGlUniformBlockReflectionBackend {
    @Override
    public int uniformBlockIndex(int programHandle, String blockName) {
        return GL31.glGetUniformBlockIndex(programHandle, blockName);
    }

    @Override
    public int uniformBlockDataSize(int programHandle, int blockIndex) {
        return GL31.glGetActiveUniformBlocki(programHandle, blockIndex, GL31.GL_UNIFORM_BLOCK_DATA_SIZE);
    }

    @Override
    public int uniformBlockBinding(int programHandle, int blockIndex) {
        return GL31.glGetActiveUniformBlocki(programHandle, blockIndex, GL31.GL_UNIFORM_BLOCK_BINDING);
    }
}
