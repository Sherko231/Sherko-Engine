package com.samo.engine.render.opengl.internal;

import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL33;
import org.lwjgl.opengl.GL45;

final class LwjglOpenGlResourceBackend implements OpenGlResourceBackend {
    @Override
    public int createBuffer() {
        return GL45.glCreateBuffers();
    }

    @Override
    public void deleteBuffer(int handle) {
        GL15Compat.deleteBuffer(handle);
    }

    @Override
    public int createVertexArray() {
        return GL45.glCreateVertexArrays();
    }

    @Override
    public void deleteVertexArray(int handle) {
        GL30.glDeleteVertexArrays(handle);
    }

    @Override
    public int createTexture() {
        return GL45.glCreateTextures(org.lwjgl.opengl.GL11.GL_TEXTURE_2D);
    }

    @Override
    public void deleteTexture(int handle) {
        org.lwjgl.opengl.GL11.glDeleteTextures(handle);
    }

    @Override
    public int createSampler() {
        return GL33.glGenSamplers();
    }

    @Override
    public void deleteSampler(int handle) {
        GL33.glDeleteSamplers(handle);
    }

    @Override
    public int createFramebuffer() {
        return GL45.glCreateFramebuffers();
    }

    @Override
    public void deleteFramebuffer(int handle) {
        GL30.glDeleteFramebuffers(handle);
    }

    @Override
    public int createShader(int shaderType) {
        return GL20.glCreateShader(shaderType);
    }

    @Override
    public void shaderSource(int shader, String source) {
        GL20.glShaderSource(shader, source);
    }

    @Override
    public void compileShader(int shader) {
        GL20.glCompileShader(shader);
    }

    @Override
    public boolean shaderCompileSucceeded(int shader) {
        return GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == org.lwjgl.opengl.GL11.GL_TRUE;
    }

    @Override
    public String shaderInfoLog(int shader) {
        return GL20.glGetShaderInfoLog(shader);
    }

    @Override
    public void deleteShader(int shader) {
        GL20.glDeleteShader(shader);
    }

    @Override
    public int createProgram() {
        return GL20.glCreateProgram();
    }

    @Override
    public void attachShader(int program, int shader) {
        GL20.glAttachShader(program, shader);
    }

    @Override
    public void linkProgram(int program) {
        GL20.glLinkProgram(program);
    }

    @Override
    public boolean programLinkSucceeded(int program) {
        return GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == org.lwjgl.opengl.GL11.GL_TRUE;
    }

    @Override
    public String programInfoLog(int program) {
        return GL20.glGetProgramInfoLog(program);
    }

    @Override
    public void detachShader(int program, int shader) {
        GL20.glDetachShader(program, shader);
    }

    @Override
    public void deleteProgram(int program) {
        GL20.glDeleteProgram(program);
    }

    private static final class GL15Compat {
        private GL15Compat() {
        }

        static void deleteBuffer(int handle) {
            org.lwjgl.opengl.GL15.glDeleteBuffers(handle);
        }
    }
}
