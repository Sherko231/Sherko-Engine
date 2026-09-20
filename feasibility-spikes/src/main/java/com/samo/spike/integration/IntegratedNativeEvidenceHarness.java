package com.samo.spike.integration;

import com.github.stephengold.joltjni.BodyCreationSettings;
import com.github.stephengold.joltjni.BodyInterface;
import com.github.stephengold.joltjni.BoxShape;
import com.github.stephengold.joltjni.BroadPhaseLayerInterfaceTable;
import com.github.stephengold.joltjni.JobSystemThreadPool;
import com.github.stephengold.joltjni.Jolt;
import com.github.stephengold.joltjni.ObjVsBpFilter;
import com.github.stephengold.joltjni.ObjectLayerPairFilterTable;
import com.github.stephengold.joltjni.PhysicsSystem;
import com.github.stephengold.joltjni.Quat;
import com.github.stephengold.joltjni.RVec3;
import com.github.stephengold.joltjni.TempAllocatorMalloc;
import com.github.stephengold.joltjni.Vec3;
import com.github.stephengold.joltjni.enumerate.EActivation;
import com.github.stephengold.joltjni.enumerate.EMotionType;
import electrostatic4j.snaploader.LibraryInfo;
import electrostatic4j.snaploader.LoadingCriterion;
import electrostatic4j.snaploader.NativeBinaryLoader;
import electrostatic4j.snaploader.filesystem.DirectoryPath;
import electrostatic4j.snaploader.platform.NativeDynamicLibrary;
import electrostatic4j.snaploader.platform.util.PlatformPredicate;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALCCapabilities;
import org.lwjgl.openal.ALCapabilities;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLDebugMessageCallback;
import org.lwjgl.system.MemoryUtil;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.nio.channels.DatagramChannel;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.openal.ALC10.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL43.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public final class IntegratedNativeEvidenceHarness {
    private static final int DEFAULT_DURATION_SECONDS = 15;
    private static final int UDP_PORT = 42_120;
    private static final int PACKET_BYTES = Integer.BYTES + Long.BYTES;
    private static final float FIXED_DT = 1.0f / 60.0f;

    private static final int OBJ_LAYER_NON_MOVING = 0;
    private static final int OBJ_LAYER_MOVING = 1;
    private static final int BP_LAYER_NON_MOVING = 0;
    private static final int BP_LAYER_MOVING = 1;

    private IntegratedNativeEvidenceHarness() {
    }

    public static void main(String[] args) throws Exception {
        int durationSeconds = Integer.getInteger("spike.durationSeconds", DEFAULT_DURATION_SECONDS);
        if (durationSeconds <= 0) {
            throw new IllegalArgumentException("spike.durationSeconds must be > 0");
        }

        String evidenceTask = System.getProperty("spike.evidenceTask", "P0-T12");
        String runKind = durationSeconds >= 900 ? "sustained test" : "smoke test";
        System.out.printf("%s integrated native %s: %d seconds%n", evidenceTask, runKind, durationSeconds);

        GLFWErrorCallback glfwError = GLFWErrorCallback.createPrint(System.err);
        glfwSetErrorCallback(glfwError);

        long window = NULL;
        GLDebugMessageCallback glDebug = null;
        AtomicBoolean highSeverityGlError = new AtomicBoolean(false);

        long audioDevice = NULL;
        long audioContext = NULL;
        int audioBuffer = 0;
        int audioSource = 0;

        PhysicsSystem physicsSystem = null;
        BroadPhaseLayerInterfaceTable layerMap = null;
        ObjectLayerPairFilterTable objectLayerFilter = null;
        ObjVsBpFilter broadPhaseFilter = null;
        TempAllocatorMalloc tempAllocator = null;
        JobSystemThreadPool jobSystem = null;
        BoxShape floorShape = null;
        BoxShape boxShape = null;
        BodyCreationSettings floorSettings = null;
        BodyCreationSettings boxSettings = null;
        BodyInterface bodies = null;
        int floorId = -1;
        int boxId = -1;
        boolean joltFactoryCreated = false;

        DatagramChannel udpServer = null;
        DatagramChannel udpClient = null;

        int sentPackets = 0;
        int echoedPackets = 0;
        long initialJoltBalance = 0L;
        boolean joltDebugBuild = false;

        try {
            // GLFW + OpenGL
            if (!glfwInit()) {
                throw new IllegalStateException("Failed to initialize GLFW");
            }
            glfwDefaultWindowHints();
            glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
            glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 6);
            glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
            glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
            glfwWindowHint(GLFW_OPENGL_DEBUG_CONTEXT, GLFW_TRUE);
            glfwWindowHint(GLFW_VISIBLE, GLFW_TRUE);
            glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);

            window = glfwCreateWindow(960, 540, "Sherko Engine - " + evidenceTask + " Integrated Native " + runKind, NULL, NULL);
            if (window == NULL) {
                throw new IllegalStateException("Failed to create OpenGL 4.6 window");
            }
            glfwMakeContextCurrent(window);
            glfwSwapInterval(1);
            GL.createCapabilities();

            glDebug = GLDebugMessageCallback.create((source, type, id, severity, length, message, userParam) -> {
                if (severity == GL_DEBUG_SEVERITY_HIGH) {
                    highSeverityGlError.set(true);
                }
            });
            glEnable(GL_DEBUG_OUTPUT);
            glDebugMessageCallback(glDebug, NULL);
            System.out.println("OpenGL initialized : " + glGetString(GL_VERSION));

            // OpenAL
            audioDevice = alcOpenDevice((String) null);
            if (audioDevice == NULL) {
                throw new IllegalStateException("Failed to open OpenAL device");
            }
            ALCCapabilities alcCapabilities = ALC.createCapabilities(audioDevice);
            audioContext = alcCreateContext(audioDevice, (int[]) null);
            if (audioContext == NULL || !alcMakeContextCurrent(audioContext)) {
                throw new IllegalStateException("Failed to create/make current OpenAL context");
            }
            ALCapabilities alCapabilities = AL.createCapabilities(alcCapabilities);
            if (!alCapabilities.OpenAL10) {
                throw new IllegalStateException("OpenAL 1.0 unavailable");
            }

            ShortBuffer pcm = generateTone();
            audioBuffer = alGenBuffers();
            alBufferData(audioBuffer, AL_FORMAT_MONO16, pcm, 48_000);
            audioSource = alGenSources();
            alSourcei(audioSource, AL_BUFFER, audioBuffer);
            alSourcei(audioSource, AL_LOOPING, AL_TRUE);
            alSourcef(audioSource, AL_GAIN, 0.08f);
            alSourcePlay(audioSource);
            checkAl("OpenAL initialization");
            System.out.println("OpenAL initialized : " + alGetString(AL_VERSION));

            // Jolt JNI
            loadJoltNativeLibrary();
            Jolt.registerDefaultAllocator();
            Jolt.installDefaultTraceCallback();
            Jolt.installDefaultAssertCallback();
            if (!Jolt.newFactory()) {
                throw new IllegalStateException("Failed to create Jolt factory");
            }
            joltFactoryCreated = true;
            Jolt.registerTypes();
            joltDebugBuild = "Debug".equalsIgnoreCase(Jolt.buildType());
            if (joltDebugBuild) {
                initialJoltBalance = allocationBalance();
            }

            objectLayerFilter = new ObjectLayerPairFilterTable(2);
            objectLayerFilter.enableCollision(OBJ_LAYER_MOVING, OBJ_LAYER_MOVING);
            objectLayerFilter.enableCollision(OBJ_LAYER_MOVING, OBJ_LAYER_NON_MOVING);

            layerMap = new BroadPhaseLayerInterfaceTable(2, 2);
            layerMap.mapObjectToBroadPhaseLayer(OBJ_LAYER_NON_MOVING, BP_LAYER_NON_MOVING);
            layerMap.mapObjectToBroadPhaseLayer(OBJ_LAYER_MOVING, BP_LAYER_MOVING);

            broadPhaseFilter = new ObjVsBpFilter(2, 2);
            broadPhaseFilter.disablePair(OBJ_LAYER_NON_MOVING, BP_LAYER_NON_MOVING);

            physicsSystem = new PhysicsSystem();
            physicsSystem.init(1_024, 0, 1_024, 1_024, layerMap, broadPhaseFilter, objectLayerFilter);
            tempAllocator = new TempAllocatorMalloc();
            jobSystem = new JobSystemThreadPool(Jolt.cMaxPhysicsJobs, Jolt.cMaxPhysicsBarriers, Math.max(1, Runtime.getRuntime().availableProcessors() - 1));
            bodies = physicsSystem.getBodyInterface();

            floorShape = new BoxShape(new Vec3(10f, 0.5f, 10f));
            floorSettings = new BodyCreationSettings(floorShape, new RVec3(0.0, -0.5, 0.0), new Quat(), EMotionType.Static, OBJ_LAYER_NON_MOVING);
            floorId = bodies.createAndAddBody(floorSettings, EActivation.DontActivate);

            boxShape = new BoxShape(new Vec3(0.5f, 0.5f, 0.5f));
            boxSettings = new BodyCreationSettings(boxShape, new RVec3(0.0, 5.0, 0.0), new Quat(), EMotionType.Dynamic, OBJ_LAYER_MOVING);
            boxId = bodies.createAndAddBody(boxSettings, EActivation.Activate);
            physicsSystem.optimizeBroadPhase();
            System.out.println("Jolt initialized   : " + Jolt.versionString());

            // UDP loopback
            udpServer = DatagramChannel.open();
            udpServer.bind(new InetSocketAddress("127.0.0.1", UDP_PORT));
            udpServer.configureBlocking(false);

            udpClient = DatagramChannel.open();
            udpClient.bind(new InetSocketAddress("127.0.0.1", 0));
            udpClient.connect(new InetSocketAddress("127.0.0.1", UDP_PORT));
            udpClient.configureBlocking(false);
            System.out.println("UDP initialized    : 127.0.0.1:" + UDP_PORT);

            ByteBuffer clientPacket = packetBuffer();
            ByteBuffer serverPacket = packetBuffer();
            long startNanos = System.nanoTime();
            long endNanos = startNanos + durationSeconds * 1_000_000_000L;
            long nextUdpSendNanos = startNanos;
            int sequence = 1;

            while (System.nanoTime() < endNanos && !glfwWindowShouldClose(window)) {
                physicsSystem.update(FIXED_DT, 1, tempAllocator, jobSystem);

                double elapsedSeconds = (System.nanoTime() - startNanos) / 1_000_000_000.0;
                float x = (float) Math.sin(elapsedSeconds * 1.5) * 3.0f;
                alSource3f(audioSource, AL_POSITION, x, 0f, -1f);
                checkAl("audio movement");

                long now = System.nanoTime();
                if (now >= nextUdpSendNanos) {
                    clientPacket.clear();
                    clientPacket.putInt(sequence++);
                    clientPacket.putLong(now);
                    clientPacket.flip();
                    while (clientPacket.hasRemaining()) {
                        udpClient.write(clientPacket);
                    }
                    sentPackets++;
                    nextUdpSendNanos = now + 250_000_000L;
                }

                serverPacket.clear();
                SocketAddress sender = udpServer.receive(serverPacket);
                if (sender != null) {
                    serverPacket.flip();
                    udpServer.send(serverPacket, sender);
                }

                clientPacket.clear();
                if (udpClient.receive(clientPacket) != null) {
                    echoedPackets++;
                }

                glClearColor(0.05f, 0.08f, 0.12f, 1.0f);
                glClear(GL_COLOR_BUFFER_BIT);
                glfwSwapBuffers(window);
                glfwPollEvents();
            }

            if (glfwWindowShouldClose(window)) {
                throw new IllegalStateException("Window was closed before the 15-second soak completed");
            }
            if (highSeverityGlError.get()) {
                throw new IllegalStateException("High-severity OpenGL debug message observed");
            }
            if (sentPackets < 2 || echoedPackets < 2) {
                throw new IllegalStateException("UDP traffic insufficient: sent=" + sentPackets + ", echoed=" + echoedPackets);
            }
            if (bodies.getPosition(boxId).y() > 1.0) {
                throw new IllegalStateException("Jolt dynamic body did not settle as expected");
            }

            System.out.printf("Runtime complete    : UDP sent=%d echoed=%d%n", sentPackets, echoedPackets);
        } finally {
            if (udpClient != null) {
                udpClient.close();
            }
            if (udpServer != null) {
                udpServer.close();
            }

            if (bodies != null) {
                if (boxId >= 0) {
                    bodies.removeBody(boxId);
                    bodies.destroyBody(boxId);
                }
                if (floorId >= 0) {
                    bodies.removeBody(floorId);
                    bodies.destroyBody(floorId);
                }
            }

            close(boxSettings);
            close(floorSettings);
            close(boxShape);
            close(floorShape);
            close(jobSystem);
            close(tempAllocator);
            if (physicsSystem != null) {
                physicsSystem.forgetMe();
            }
            close(physicsSystem);
            close(broadPhaseFilter);
            close(objectLayerFilter);
            close(layerMap);

            if (joltFactoryCreated) {
                Jolt.unregisterTypes();
                Jolt.destroyFactory();
            }

            if (joltDebugBuild) {
                long finalBalance = allocationBalance();
                System.out.printf("Jolt allocation balance: initial=%d final=%d delta=%+d%n", initialJoltBalance, finalBalance, finalBalance - initialJoltBalance);
                if (finalBalance > initialJoltBalance) {
                    throw new IllegalStateException("Jolt native allocation balance grew during soak");
                }
            }

            if (audioSource != 0) {
                alSourceStop(audioSource);
                alDeleteSources(audioSource);
            }
            if (audioBuffer != 0) {
                alDeleteBuffers(audioBuffer);
            }
            if (audioContext != NULL) {
                alcMakeContextCurrent(NULL);
                alcDestroyContext(audioContext);
            }
            if (audioDevice != NULL && !alcCloseDevice(audioDevice)) {
                throw new IllegalStateException("Failed to close OpenAL device");
            }

            if (glDebug != null) {
                glDebug.free();
            }
            if (window != NULL) {
                glfwDestroyWindow(window);
            }
            glfwTerminate();
            glfwSetErrorCallback(null);
            glfwError.free();
        }

        System.out.printf("%s passed: integrated native %s completed and all subsystems shut down cleanly.%n", evidenceTask, runKind);
    }

    private static ByteBuffer packetBuffer() {
        return ByteBuffer.allocateDirect(PACKET_BYTES).order(ByteOrder.BIG_ENDIAN);
    }

    private static ShortBuffer generateTone() {
        int sampleRate = 48_000;
        ShortBuffer samples = BufferUtils.createShortBuffer(sampleRate);
        double angularStep = 2.0 * Math.PI * 440.0 / sampleRate;
        for (int i = 0; i < sampleRate; i++) {
            samples.put((short) (Math.sin(i * angularStep) * Short.MAX_VALUE * 0.15));
        }
        samples.flip();
        return samples;
    }

    private static void checkAl(String operation) {
        int error = alGetError();
        if (error != AL_NO_ERROR) {
            throw new IllegalStateException(operation + " failed with OpenAL error 0x" + Integer.toHexString(error));
        }
    }

    private static long allocationBalance() {
        return (long) Jolt.countNews() - Jolt.countDeletes();
    }

    private static void close(AutoCloseable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (Exception exception) {
            throw new RuntimeException("Failed to release native object", exception);
        }
    }

    private static void loadJoltNativeLibrary() {
        LibraryInfo info = new LibraryInfo(null, "joltjni", DirectoryPath.USER_DIR);
        NativeBinaryLoader loader = new NativeBinaryLoader(info);
        loader.registerNativeLibraries(new NativeDynamicLibrary[]{new NativeDynamicLibrary("windows/x86-64/com/github/stephengold", PlatformPredicate.WIN_X86_64)})
            .initPlatformLibrary();
        try {
            loader.loadLibrary(LoadingCriterion.CLEAN_EXTRACTION);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to load Jolt JNI native library", exception);
        }
    }
}
