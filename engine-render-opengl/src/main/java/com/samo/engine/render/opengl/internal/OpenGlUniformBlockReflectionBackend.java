package com.samo.engine.render.opengl.internal;

interface OpenGlUniformBlockReflectionBackend {
    int uniformBlockIndex(int programHandle, String blockName);

    int uniformBlockDataSize(int programHandle, int blockIndex);

    int uniformBlockBinding(int programHandle, int blockIndex);
}
