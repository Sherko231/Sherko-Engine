package com.samo.spike.opengl;

import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MAJOR;
import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MINOR;
import static org.lwjgl.glfw.GLFW.GLFW_FALSE;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_CORE_PROFILE;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_FORWARD_COMPAT;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_PROFILE;
import static org.lwjgl.glfw.GLFW.GLFW_RESIZABLE;
import static org.lwjgl.glfw.GLFW.GLFW_TRUE;
import static org.lwjgl.glfw.GLFW.GLFW_VISIBLE;
import static org.lwjgl.glfw.GLFW.glfwCreateWindow;
import static org.lwjgl.glfw.GLFW.glfwDefaultWindowHints;
import static org.lwjgl.glfw.GLFW.glfwDestroyWindow;
import static org.lwjgl.glfw.GLFW.glfwGetPrimaryMonitor;
import static org.lwjgl.glfw.GLFW.glfwGetVideoMode;
import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;
import static org.lwjgl.glfw.GLFW.glfwSetErrorCallback;
import static org.lwjgl.glfw.GLFW.glfwSwapInterval;
import static org.lwjgl.glfw.GLFW.glfwTerminate;
import static org.lwjgl.glfw.GLFW.glfwWindowHint;
import static org.lwjgl.opengl.GL11.GL_BACK;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_CULL_FACE;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_LINEAR;
import static org.lwjgl.opengl.GL11.GL_RENDERER;
import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11.GL_RGBA8;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_INT;
import static org.lwjgl.opengl.GL11.GL_VENDOR;
import static org.lwjgl.opengl.GL11.GL_VERSION;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glCullFace;
import static org.lwjgl.opengl.GL11.glDeleteTextures;
import static org.lwjgl.opengl.GL11.glDrawElements;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glFinish;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL11.glGetString;
import static org.lwjgl.opengl.GL11.glTexImage2D;
import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_ELEMENT_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL20.GL_COMPILE_STATUS;
import static org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20.GL_LINK_STATUS;
import static org.lwjgl.opengl.GL20.GL_VERTEX_SHADER;
import static org.lwjgl.opengl.GL20.glAttachShader;
import static org.lwjgl.opengl.GL20.glCompileShader;
import static org.lwjgl.opengl.GL20.glCreateProgram;
import static org.lwjgl.opengl.GL20.glCreateShader;
import static org.lwjgl.opengl.GL20.glDeleteProgram;
import static org.lwjgl.opengl.GL20.glDeleteShader;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glGetProgramInfoLog;
import static org.lwjgl.opengl.GL20.glGetProgrami;
import static org.lwjgl.opengl.GL20.glGetShaderInfoLog;
import static org.lwjgl.opengl.GL20.glGetShaderi;
import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import static org.lwjgl.opengl.GL20.glLinkProgram;
import static org.lwjgl.opengl.GL20.glShaderSource;
import static org.lwjgl.opengl.GL20.glUniform3f;
import static org.lwjgl.opengl.GL20.glUseProgram;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.GL_COLOR_ATTACHMENT0;
import static org.lwjgl.opengl.GL30.GL_DEPTH_ATTACHMENT;
import static org.lwjgl.opengl.GL30.GL_DEPTH_COMPONENT24;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER_COMPLETE;
import static org.lwjgl.opengl.GL30.GL_RENDERBUFFER;
import static org.lwjgl.opengl.GL30.glBindFramebuffer;
import static org.lwjgl.opengl.GL30.glBindRenderbuffer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glCheckFramebufferStatus;
import static org.lwjgl.opengl.GL30.glDeleteFramebuffers;
import static org.lwjgl.opengl.GL30.glDeleteRenderbuffers;
import static org.lwjgl.opengl.GL30.glDeleteVertexArrays;
import static org.lwjgl.opengl.GL30.glFramebufferRenderbuffer;
import static org.lwjgl.opengl.GL30.glFramebufferTexture2D;
import static org.lwjgl.opengl.GL30.glGenFramebuffers;
import static org.lwjgl.opengl.GL30.glGenRenderbuffers;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;
import static org.lwjgl.opengl.GL30.glRenderbufferStorage;

public final class P5HardwareBaselineSpike {
    private static final int WIDTH = 1920;
    private static final int HEIGHT = 1080;
    private static final int WARMUP_FRAMES = 300;
    private static final int MEASUREMENT_FRAMES = 600;
    private static final int DRAWS_PER_FRAME = 1000;
    private static final int FLOATS_PER_VERTEX = 8;
    private static final int INDEX_COUNT = 36;
    private static final double FRAME_BUDGET_MILLIS = 16.667;

    private static final String VERTEX_SHADER = """
        #version 460 core
        layout(location = 0) in vec3 aPosition;
        layout(location = 1) in vec3 aNormal;
        layout(location = 2) in vec2 aUv;
        uniform vec3 uOffset;
        out vec3 vNormal;
        out vec2 vUv;
        void main() {
            vec3 position = aPosition * 0.018 + uOffset;
            gl_Position = vec4(position, 1.0);
            vNormal = aNormal;
            vUv = aUv;
        }
        """;

    private static final String FRAGMENT_SHADER = """
        #version 460 core
        in vec3 vNormal;
        in vec2 vUv;
        layout(location = 0) out vec4 outColor;
        uniform sampler2D uTexture;
        void main() {
            vec3 lightDirection = normalize(vec3(0.35, 0.8, 0.45));
            float diffuse = max(dot(normalize(vNormal), lightDirection), 0.15);
            vec3 albedo = texture(uTexture, vUv).rgb;
            outColor = vec4(albedo * diffuse, 1.0);
        }
        """;

    private P5HardwareBaselineSpike() {
    }

    public static void main(String[] args) throws Exception {
        requireWindowsX64();
        int vramMiB = requiredPositiveIntProperty("spike.vramMiB");
        Path reportPath = Path.of(requiredProperty("spike.reportPath"));
        HardwareInfo hardware = collectHardwareInfo(vramMiB);
        BenchmarkResult result = runBenchmark();
        writeReport(reportPath, hardware, result);
        System.out.println("P5-T00 report: " + reportPath.toAbsolutePath());
        System.out.printf(Locale.ROOT, "P5-T00 p95 synchronized frame time: %.3f ms (%s)%n", result.p95Millis(), result.passed() ? "PASS" : "FAIL");
        if (!result.passed()) {
            throw new IllegalStateException("Candidate hardware does not satisfy the P5-T00 1080p60 p95 frame-time threshold");
        }
    }

    private static BenchmarkResult runBenchmark() {
        GLFWErrorCallback errorCallback = GLFWErrorCallback.createPrint(System.err);
        glfwSetErrorCallback(errorCallback);
        long window = MemoryUtil.NULL;
        int vao = 0;
        int vertexBuffer = 0;
        int indexBuffer = 0;
        int checkerTexture = 0;
        int colorTexture = 0;
        int depthRenderbuffer = 0;
        int framebuffer = 0;
        int program = 0;
        try {
            if (!glfwInit()) {
                throw new IllegalStateException("Failed to initialize GLFW");
            }

            glfwDefaultWindowHints();
            glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
            glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 6);
            glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
            glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
            glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
            glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);

            window = glfwCreateWindow(64, 64, "Sherko Engine - P5-T00 Hardware Baseline", MemoryUtil.NULL, MemoryUtil.NULL);
            if (window == MemoryUtil.NULL) {
                throw new IllegalStateException("Failed to create hidden OpenGL 4.6 Core measurement context");
            }

            glfwMakeContextCurrent(window);
            glfwSwapInterval(0);
            GL.createCapabilities();

            String openGlVersion = safeGlString(GL_VERSION);
            String openGlVendor = safeGlString(GL_VENDOR);
            String openGlRenderer = safeGlString(GL_RENDERER);
            String displayMode = currentDisplayMode();

            framebuffer = glGenFramebuffers();
            glBindFramebuffer(GL_FRAMEBUFFER, framebuffer);

            colorTexture = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, colorTexture);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, WIDTH, HEIGHT, 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer) null);
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, colorTexture, 0);

            depthRenderbuffer = glGenRenderbuffers();
            glBindRenderbuffer(GL_RENDERBUFFER, depthRenderbuffer);
            glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT24, WIDTH, HEIGHT);
            glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, depthRenderbuffer);

            int framebufferStatus = glCheckFramebufferStatus(GL_FRAMEBUFFER);
            if (framebufferStatus != GL_FRAMEBUFFER_COMPLETE) {
                throw new IllegalStateException(String.format(Locale.ROOT, "P5-T00 framebuffer incomplete: 0x%X", framebufferStatus));
            }
            glViewport(0, 0, WIDTH, HEIGHT);

            program = createProgram();
            vao = glGenVertexArrays();
            vertexBuffer = glGenBuffers();
            indexBuffer = glGenBuffers();
            checkerTexture = createCheckerTexture();
            configureGeometry(vao, vertexBuffer, indexBuffer);

            glEnable(GL_DEPTH_TEST);
            glEnable(GL_CULL_FACE);
            glCullFace(GL_BACK);
            glUseProgram(program);
            glBindVertexArray(vao);
            glBindTexture(GL_TEXTURE_2D, checkerTexture);

            int offsetLocation = glGetUniformLocation(program, "uOffset");
            if (offsetLocation < 0) {
                throw new IllegalStateException("uOffset uniform was not found");
            }

            for (int frame = 0; frame < WARMUP_FRAMES; frame++) {
                renderFrame(offsetLocation);
                glFinish();
            }

            double[] samplesMillis = new double[MEASUREMENT_FRAMES];
            for (int frame = 0; frame < MEASUREMENT_FRAMES; frame++) {
                long startNanos = System.nanoTime();
                renderFrame(offsetLocation);
                glFinish();
                samplesMillis[frame] = (System.nanoTime() - startNanos) / 1_000_000.0;
            }

            return summarize(samplesMillis, displayMode, openGlVersion, openGlVendor, openGlRenderer);
        } finally {
            if (program != 0) {
                glDeleteProgram(program);
            }
            if (checkerTexture != 0) {
                glDeleteTextures(checkerTexture);
            }
            if (indexBuffer != 0) {
                glDeleteBuffers(indexBuffer);
            }
            if (vertexBuffer != 0) {
                glDeleteBuffers(vertexBuffer);
            }
            if (vao != 0) {
                glDeleteVertexArrays(vao);
            }
            if (depthRenderbuffer != 0) {
                glDeleteRenderbuffers(depthRenderbuffer);
            }
            if (colorTexture != 0) {
                glDeleteTextures(colorTexture);
            }
            if (framebuffer != 0) {
                glDeleteFramebuffers(framebuffer);
            }
            if (window != MemoryUtil.NULL) {
                glfwDestroyWindow(window);
            }
            glfwTerminate();
            glfwSetErrorCallback(null);
            errorCallback.free();
        }
    }

    private static String currentDisplayMode() {
        long monitor = glfwGetPrimaryMonitor();
        GLFWVidMode mode = monitor == MemoryUtil.NULL ? null : glfwGetVideoMode(monitor);
        return mode == null ? "unavailable" : mode.width() + "x" + mode.height() + "@" + mode.refreshRate();
    }

    private static void renderFrame(int offsetLocation) {
        glClearColor(0.035f, 0.045f, 0.065f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        for (int draw = 0; draw < DRAWS_PER_FRAME; draw++) {
            int column = draw % 40;
            int row = (draw / 40) % 25;
            float x = -0.9f + column * (1.8f / 39.0f);
            float y = -0.9f + row * (1.8f / 24.0f);
            float z = -0.8f + (draw % 17) * 0.09f;
            glUniform3f(offsetLocation, x, y, z);
            glDrawElements(GL_TRIANGLES, INDEX_COUNT, GL_UNSIGNED_INT, 0L);
        }
    }

    private static void configureGeometry(int vao, int vertexBuffer, int indexBuffer) {
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vertexBuffer);
        glBufferData(GL_ARRAY_BUFFER, cubeVertices(), GL_STATIC_DRAW);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, indexBuffer);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, cubeIndices(), GL_STATIC_DRAW);
        int stride = FLOATS_PER_VERTEX * Float.BYTES;
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0L);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 3, GL_FLOAT, false, stride, 3L * Float.BYTES);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(2, 2, GL_FLOAT, false, stride, 6L * Float.BYTES);
        glEnableVertexAttribArray(2);
    }

    private static int createCheckerTexture() {
        int texture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, texture);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        ByteBuffer pixels = ByteBuffer.allocateDirect(16);
        putRgba(pixels, 230, 230, 230, 255);
        putRgba(pixels, 60, 90, 140, 255);
        putRgba(pixels, 60, 90, 140, 255);
        putRgba(pixels, 230, 230, 230, 255);
        pixels.flip();
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, 2, 2, 0, GL_RGBA, GL_UNSIGNED_BYTE, pixels);
        return texture;
    }

    private static void putRgba(ByteBuffer target, int red, int green, int blue, int alpha) {
        target.put((byte) red).put((byte) green).put((byte) blue).put((byte) alpha);
    }

    private static int createProgram() {
        int vertexShader = compileShader(GL_VERTEX_SHADER, VERTEX_SHADER);
        int fragmentShader = compileShader(GL_FRAGMENT_SHADER, FRAGMENT_SHADER);
        int program = glCreateProgram();
        try {
            glAttachShader(program, vertexShader);
            glAttachShader(program, fragmentShader);
            glLinkProgram(program);
            if (glGetProgrami(program, GL_LINK_STATUS) == 0) {
                throw new IllegalStateException("P5-T00 shader link failed: " + glGetProgramInfoLog(program));
            }
            return program;
        } catch (RuntimeException | Error failure) {
            glDeleteProgram(program);
            throw failure;
        } finally {
            glDeleteShader(vertexShader);
            glDeleteShader(fragmentShader);
        }
    }

    private static int compileShader(int type, String source) {
        int shader = glCreateShader(type);
        glShaderSource(shader, source);
        glCompileShader(shader);
        if (glGetShaderi(shader, GL_COMPILE_STATUS) == 0) {
            String log = glGetShaderInfoLog(shader);
            glDeleteShader(shader);
            throw new IllegalStateException("P5-T00 shader compile failed: " + log);
        }
        return shader;
    }

    private static BenchmarkResult summarize(double[] samplesMillis, String displayMode, String openGlVersion, String openGlVendor, String openGlRenderer) {
        double[] sorted = samplesMillis.clone();
        Arrays.sort(sorted);
        double sum = 0.0;
        double maximum = 0.0;
        for (double sample : samplesMillis) {
            sum += sample;
            maximum = Math.max(maximum, sample);
        }
        double mean = sum / samplesMillis.length;
        double median = percentile(sorted, 0.50);
        double p95 = percentile(sorted, 0.95);
        double p99 = percentile(sorted, 0.99);
        return new BenchmarkResult(mean, median, p95, p99, maximum, p95 <= FRAME_BUDGET_MILLIS, displayMode, openGlVersion, openGlVendor, openGlRenderer);
    }

    private static double percentile(double[] sorted, double percentile) {
        int index = (int) Math.ceil(percentile * sorted.length) - 1;
        return sorted[Math.max(0, Math.min(index, sorted.length - 1))];
    }

    private static HardwareInfo collectHardwareInfo(int vramMiB) throws IOException, InterruptedException {
        assertCleanCheckout();
        Map<String, String> windows = queryWindowsHardware();
        String commit = readCommand(List.of("git", "rev-parse", "HEAD")).strip();
        if (commit.isBlank()) {
            throw new IllegalStateException("Unable to determine exact repository SHA");
        }
        return new HardwareInfo(
                commit,
                windows.getOrDefault("WINDOWS_CAPTION", "unknown"),
                windows.getOrDefault("WINDOWS_VERSION", "unknown"),
                windows.getOrDefault("WINDOWS_BUILD", "unknown"),
                windows.getOrDefault("CPU", "unknown"),
                windows.getOrDefault("RAM_BYTES", "unknown"),
                windows.getOrDefault("VIDEO_CONTROLLERS", "unknown"),
                vramMiB
        );
    }

    private static void assertCleanCheckout() throws IOException, InterruptedException {
        String status = readCommand(List.of("git", "status", "--porcelain")).strip();
        if (!status.isEmpty()) {
            throw new IllegalStateException("P5-T00 benchmark requires a clean Git checkout");
        }
    }

    private static Map<String, String> queryWindowsHardware() throws IOException, InterruptedException {
        String script = "$os=Get-CimInstance Win32_OperatingSystem;"
                + "$cpu=(Get-CimInstance Win32_Processor | Select-Object -First 1 -ExpandProperty Name);"
                + "$ram=(Get-CimInstance Win32_ComputerSystem).TotalPhysicalMemory;"
                + "$video=((Get-CimInstance Win32_VideoController | ForEach-Object {"
                + "$_.Name + '|' + $_.AdapterCompatibility + '|' + $_.DriverVersion + '|' + $_.PNPDeviceID"
                + "}) -join '; ');"
                + "Write-Output ('WINDOWS_CAPTION='+$os.Caption);"
                + "Write-Output ('WINDOWS_VERSION='+$os.Version);"
                + "Write-Output ('WINDOWS_BUILD='+$os.BuildNumber);"
                + "Write-Output ('CPU='+$cpu);"
                + "Write-Output ('RAM_BYTES='+$ram);"
                + "Write-Output ('VIDEO_CONTROLLERS='+$video)";
        String output = readCommand(List.of("powershell.exe", "-NoProfile", "-Command", script));
        Map<String, String> values = new HashMap<>();
        for (String line : output.split("\\R")) {
            int separator = line.indexOf('=');
            if (separator > 0) {
                values.put(line.substring(0, separator).strip(), line.substring(separator + 1).strip());
            }
        }
        return values;
    }

    private static String readCommand(List<String> command) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(new ArrayList<>(command)).redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IllegalStateException("Command failed (" + exitCode + "): " + String.join(" ", command) + "\n" + output);
        }
        return output;
    }

    private static void writeReport(Path reportPath, HardwareInfo hardware, BenchmarkResult result) throws IOException {
        Files.createDirectories(reportPath.toAbsolutePath().getParent());
        List<String> lines = List.of(
                "task=P5-T00",
                "result=" + (result.passed() ? "PASS" : "FAIL"),
                "repositorySha=" + hardware.repositorySha(),
                "repositoryClean=true",
                "javaVersion=" + System.getProperty("java.version"),
                "windowsCaption=" + hardware.windowsCaption(),
                "windowsVersion=" + hardware.windowsVersion(),
                "windowsBuild=" + hardware.windowsBuild(),
                "osArch=" + System.getProperty("os.arch"),
                "cpu=" + hardware.cpu(),
                "ramBytes=" + hardware.ramBytes(),
                "videoControllers=name|vendor|driverVersion|pnpDeviceId:" + hardware.videoControllers(),
                "vramMiB=" + hardware.vramMiB(),
                "displayMode=" + result.displayMode(),
                "openGlVersion=" + result.openGlVersion(),
                "openGlVendor=" + result.openGlVendor(),
                "openGlRenderer=" + result.openGlRenderer(),
                "framebuffer=offscreen-1920x1080-rgba8-depth24",
                "warmupFrames=" + WARMUP_FRAMES,
                "measurementFrames=" + MEASUREMENT_FRAMES,
                "sampleCount=" + MEASUREMENT_FRAMES,
                "drawsPerFrame=" + DRAWS_PER_FRAME,
                "geometry=indexed-cube-position-normal-uv",
                "texture=generated-2x2-checkerboard",
                "depthTest=true",
                "backFaceCulling=true",
                "directionalLight=true",
                "vsync=false",
                "presentationSwap=false",
                "gpuCompletionPerSample=glFinish",
                "frameBudgetMillis=" + FRAME_BUDGET_MILLIS,
                String.format(Locale.ROOT, "meanMillis=%.6f", result.meanMillis()),
                String.format(Locale.ROOT, "medianMillis=%.6f", result.medianMillis()),
                String.format(Locale.ROOT, "p95Millis=%.6f", result.p95Millis()),
                String.format(Locale.ROOT, "p99Millis=%.6f", result.p99Millis()),
                String.format(Locale.ROOT, "maxMillis=%.6f", result.maximumMillis()),
                String.format(Locale.ROOT, "meanFps=%.3f", 1000.0 / result.meanMillis()),
                String.format(Locale.ROOT, "p95Fps=%.3f", 1000.0 / result.p95Millis()),
                "evidenceLimit=fixed disposable Phase 5 baseline workload; not final production performance, soak, leak, or multi-vendor proof"
        );
        Files.write(reportPath, lines, StandardCharsets.UTF_8);
    }

    private static String safeGlString(int name) {
        String value = glGetString(name);
        return value == null ? "unavailable" : value.replace('\n', ' ').replace('\r', ' ');
    }

    private static void requireWindowsX64() {
        String osName = System.getProperty("os.name", "");
        String osArch = System.getProperty("os.arch", "");
        boolean x64 = osArch.equalsIgnoreCase("amd64") || osArch.equalsIgnoreCase("x86_64");
        if (!osName.toLowerCase(Locale.ROOT).contains("windows") || !x64) {
            throw new IllegalStateException("P5-T00 hardware baseline requires Windows x64");
        }
    }

    private static String requiredProperty(String name) {
        String value = System.getProperty(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must be supplied");
        }
        return value;
    }

    private static int requiredPositiveIntProperty(String name) {
        String value = requiredProperty(name);
        try {
            int parsed = Integer.parseInt(value);
            if (parsed <= 0) {
                throw new IllegalArgumentException(name + " must be greater than zero");
            }
            return parsed;
        } catch (NumberFormatException failure) {
            throw new IllegalArgumentException(name + " must be a positive integer", failure);
        }
    }

    private static float[] cubeVertices() {
        return new float[] {
            -1, -1, 1, 0, 0, 1, 0, 0, 1, -1, 1, 0, 0, 1, 1, 0, 1, 1, 1, 0, 0, 1, 1, 1, -1, 1, 1, 0, 0, 1, 0, 1,
            1, -1, -1, 0, 0, -1, 0, 0, -1, -1, -1, 0, 0, -1, 1, 0, -1, 1, -1, 0, 0, -1, 1, 1, 1, 1, -1, 0, 0, -1, 0, 1,
            -1, -1, -1, -1, 0, 0, 0, 0, -1, -1, 1, -1, 0, 0, 1, 0, -1, 1, 1, -1, 0, 0, 1, 1, -1, 1, -1, -1, 0, 0, 0, 1,
            1, -1, 1, 1, 0, 0, 0, 0, 1, -1, -1, 1, 0, 0, 1, 0, 1, 1, -1, 1, 0, 0, 1, 1, 1, 1, 1, 1, 0, 0, 0, 1,
            -1, 1, 1, 0, 1, 0, 0, 0, 1, 1, 1, 0, 1, 0, 1, 0, 1, 1, -1, 0, 1, 0, 1, 1, -1, 1, -1, 0, 1, 0, 0, 1,
            -1, -1, -1, 0, -1, 0, 0, 0, 1, -1, -1, 0, -1, 0, 1, 0, 1, -1, 1, 0, -1, 0, 1, 1, -1, -1, 1, 0, -1, 0, 0, 1
        };
    }

    private static int[] cubeIndices() {
        return new int[] {
            0, 1, 2, 2, 3, 0,
            4, 5, 6, 6, 7, 4,
            8, 9, 10, 10, 11, 8,
            12, 13, 14, 14, 15, 12,
            16, 17, 18, 18, 19, 16,
            20, 21, 22, 22, 23, 20
        };
    }

    private record HardwareInfo(String repositorySha, String windowsCaption, String windowsVersion, String windowsBuild, String cpu, String ramBytes, String videoControllers, int vramMiB) {
    }

    private record BenchmarkResult(double meanMillis, double medianMillis, double p95Millis, double p99Millis, double maximumMillis, boolean passed, String displayMode, String openGlVersion, String openGlVendor, String openGlRenderer) {
    }
}
