package com.samo.engine.render.opengl.internal;

interface OpenGlResourceBackend {
    int createBuffer();

    void deleteBuffer(int handle);

    int createVertexArray();

    void deleteVertexArray(int handle);

    int createTexture();

    void deleteTexture(int handle);

    int createSampler();

    void deleteSampler(int handle);

    int createFramebuffer();

    void deleteFramebuffer(int handle);

    int createShader(int shaderType);

    void shaderSource(int shader, String source);

    void compileShader(int shader);

    boolean shaderCompileSucceeded(int shader);

    String shaderInfoLog(int shader);

    void deleteShader(int shader);

    int createProgram();

    void attachShader(int program, int shader);

    void linkProgram(int program);

    boolean programLinkSucceeded(int program);

    String programInfoLog(int program);

    void detachShader(int program, int shader);

    void deleteProgram(int program);
}
