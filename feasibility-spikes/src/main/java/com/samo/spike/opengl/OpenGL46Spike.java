package com.samo.spike.opengl;

import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLDebugMessageCallback;
import org.lwjgl.system.MemoryUtil;

import java.nio.IntBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.GL_CONTEXT_FLAGS;
import static org.lwjgl.opengl.GL43.*;
import static org.lwjgl.system.MemoryStack.stackPush;

public final class OpenGL46Spike {
    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;
    private static final long DEFAULT_DURATION_SECONDS = 600;

    private OpenGL46Spike() {
    }

    public static void main(String[] args) {
        long durationSeconds = Long.getLong("spike.durationSeconds", DEFAULT_DURATION_SECONDS);
        if (durationSeconds <= 0) {
            throw new IllegalArgumentException("spike.durationSeconds must be greater than zero");
        }

        GLFWErrorCallback errorCallback = GLFWErrorCallback.createPrint(System.err);
        glfwSetErrorCallback(errorCallback);

        long window = MemoryUtil.NULL;
        GLDebugMessageCallback debugCallback = null;
        AtomicBoolean highSeveritySeen = new AtomicBoolean(false);

        try {
            if (!glfwInit()) {
                throw new IllegalStateException("Failed to initialize GLFW");
            }

            glfwDefaultWindowHints();
            glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
            glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 6);
            glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
            glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
            glfwWindowHint(GLFW_OPENGL_DEBUG_CONTEXT, GLFW_TRUE);
            glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
            glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);

            window = glfwCreateWindow(WIDTH, HEIGHT, "Sherko Engine - P0-T03 OpenGL 4.6 Spike", MemoryUtil.NULL, MemoryUtil.NULL);
            if (window == MemoryUtil.NULL) {
                throw new IllegalStateException("Failed to create GLFW window with an OpenGL 4.6 Core context");
            }

            centerWindow(window);
            glfwMakeContextCurrent(window);
            glfwSwapInterval(1);
            GL.createCapabilities();

            String version = glGetString(GL_VERSION);
            String renderer = glGetString(GL_RENDERER);
            String vendor = glGetString(GL_VENDOR);

            System.out.println("OpenGL version : " + version);
            System.out.println("Renderer       : " + renderer);
            System.out.println("Vendor         : " + vendor);
            System.out.printf("Requested run  : %d seconds%n", durationSeconds);

            int contextFlags = glGetInteger(GL_CONTEXT_FLAGS);
            if ((contextFlags & GL_CONTEXT_FLAG_DEBUG_BIT) == 0) {
                throw new IllegalStateException("OpenGL debug context was requested but not created");
            }

            debugCallback = GLDebugMessageCallback.create((source, type, id, severity, length, message, userParam) -> {
                String text = GLDebugMessageCallback.getMessage(length, message);
                System.err.printf("[GL DEBUG] severity=0x%X type=0x%X id=%d message=%s%n", severity, type, id, text);
                if (severity == GL_DEBUG_SEVERITY_HIGH) {
                    highSeveritySeen.set(true);
                }
            });

            glEnable(GL_DEBUG_OUTPUT);
            glEnable(GL_DEBUG_OUTPUT_SYNCHRONOUS);
            glDebugMessageCallback(debugCallback, MemoryUtil.NULL);

            glfwShowWindow(window);

            long startNanos = System.nanoTime();
            long durationNanos = durationSeconds * 1_000_000_000L;

            while (!glfwWindowShouldClose(window)) {
                long elapsed = System.nanoTime() - startNanos;
                if (elapsed >= durationNanos) {
                    break;
                }

                glClearColor(0.05f, 0.08f, 0.12f, 1.0f);
                glClear(GL_COLOR_BUFFER_BIT);

                glfwSwapBuffers(window);
                glfwPollEvents();
            }

            long elapsedNanos = System.nanoTime() - startNanos;
            if (elapsedNanos < durationNanos) {
                throw new IllegalStateException("Spike ended before the requested duration completed");
            }

            if (highSeveritySeen.get()) {
                throw new IllegalStateException("OpenGL high-severity debug messages were reported");
            }

            System.out.println("P0-T03 smoke run completed without high-severity OpenGL debug messages.");
        } finally {
            if (debugCallback != null) {
                debugCallback.free();
            }

            if (window != MemoryUtil.NULL) {
                glfwDestroyWindow(window);
            }

            glfwTerminate();
            glfwSetErrorCallback(null);
            errorCallback.free();
        }
    }

    private static void centerWindow(long window) {
        long monitor = glfwGetPrimaryMonitor();
        if (monitor == MemoryUtil.NULL) {
            return;
        }

        var videoMode = glfwGetVideoMode(monitor);
        if (videoMode == null) {
            return;
        }

        try (var stack = stackPush()) {
            IntBuffer width = stack.mallocInt(1);
            IntBuffer height = stack.mallocInt(1);
            glfwGetWindowSize(window, width, height);

            int x = (videoMode.width() - width.get(0)) / 2;
            int y = (videoMode.height() - height.get(0)) / 2;
            glfwSetWindowPos(window, Math.max(x, 0), Math.max(y, 0));
        }
    }
}
