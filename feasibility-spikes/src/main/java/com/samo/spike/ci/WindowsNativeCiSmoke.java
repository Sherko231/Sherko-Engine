package com.samo.spike.ci;

import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALCCapabilities;
import org.lwjgl.openal.ALCapabilities;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.openal.ALC10.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public final class WindowsNativeCiSmoke {
    private WindowsNativeCiSmoke() {
    }

    public static void main(String[] args) {
        verifyGlfwLifecycle();
        verifyOpenAlLifecycle();
        System.out.println("P1-T08 passed: hosted Windows GLFW and OpenAL native lifecycle smoke completed.");
    }

    private static void verifyGlfwLifecycle() {
        GLFWErrorCallback errorCallback = GLFWErrorCallback.createPrint(System.err);
        glfwSetErrorCallback(errorCallback);
        try {
            if (!glfwInit()) {
                throw new IllegalStateException("Failed to initialize GLFW on hosted Windows CI");
            }
            System.out.println("GLFW version    : " + glfwGetVersionString());
        } finally {
            glfwTerminate();
            glfwSetErrorCallback(null);
            errorCallback.free();
        }
    }

    private static void verifyOpenAlLifecycle() {
        long device = NULL;
        long context = NULL;
        try {
            device = alcOpenDevice((String) null);
            if (device == NULL) {
                throw new IllegalStateException("Failed to open OpenAL device on hosted Windows CI");
            }

            ALCCapabilities alcCapabilities = ALC.createCapabilities(device);
            context = alcCreateContext(device, (int[]) null);
            if (context == NULL) {
                throw new IllegalStateException("Failed to create OpenAL context on hosted Windows CI");
            }
            if (!alcMakeContextCurrent(context)) {
                throw new IllegalStateException("Failed to make OpenAL context current on hosted Windows CI");
            }

            ALCapabilities alCapabilities = AL.createCapabilities(alcCapabilities);
            if (!alCapabilities.OpenAL10) {
                throw new IllegalStateException("OpenAL 1.0 capability unavailable on hosted Windows CI");
            }

            System.out.println("OpenAL version  : " + alGetString(AL_VERSION));
            int error = alGetError();
            if (error != AL_NO_ERROR) {
                throw new IllegalStateException(
                        "OpenAL smoke reported error 0x" + Integer.toHexString(error)
                );
            }
        } finally {
            if (context != NULL) {
                alcMakeContextCurrent(NULL);
                alcDestroyContext(context);
            }
            if (device != NULL && !alcCloseDevice(device)) {
                throw new IllegalStateException("Failed to close OpenAL device on hosted Windows CI");
            }
        }
    }
}
