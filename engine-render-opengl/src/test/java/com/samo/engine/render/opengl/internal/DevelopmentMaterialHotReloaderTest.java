package com.samo.engine.render.opengl.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.samo.engine.assets.api.AssetId;
import com.samo.engine.assets.api.MaterialAsset;
import com.samo.engine.assets.api.ResourceHandle;
import com.samo.engine.assets.api.ResourceHandleState;
import com.samo.engine.core.api.EngineLogger;
import com.samo.engine.core.api.NativeResourceRegistry;
import com.samo.engine.platform.api.GlfwWindow;
import com.samo.engine.platform.api.OpenGlThreadGuard;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DevelopmentMaterialHotReloaderTest {
    private static final AssetId MATERIAL_ID = AssetId.parse("01234567-89ab-cdef-fedc-ba9876543210");

    @TempDir
    Path tempDir;

    @Test
    void scalarAndShaderEditsSwapOnlyCompleteValidCandidates() throws Exception {

        writeShader("opaque-baseline", "vertex-one", "fragment-one");
        OpenGlThreadGuard guard = boundGuard();
        NativeResourceRegistry registry = new NativeResourceRegistry();
        FakeBackend backend = new FakeBackend();
        MutableMaterialHandle handle = new MutableMaterialHandle(material("opaque-baseline", 1.0f));

        DevelopmentMaterialHotReloader reloader = DevelopmentMaterialHotReloader.create(handle, tempDir, guard, registry, backend, 101, 102);
        int initialProgram = reloader.programHandle();
        assertThat(reloader.material().scalars().redMultiplier()).isEqualTo(1.0f);

        handle.set(material("opaque-baseline", 0.25f));
        reloader.poll();
        assertThat(reloader.programHandle()).isEqualTo(initialProgram);
        assertThat(reloader.material().scalars().redMultiplier()).isEqualTo(0.25f);
        assertThat(backend.createdPrograms).isEqualTo(1);

        writeShader("opaque-baseline", "vertex-two", "fragment-one");
        reloader.poll();
        int replacementProgram = reloader.programHandle();
        assertThat(replacementProgram).isNotEqualTo(initialProgram);
        assertThat(backend.createdPrograms).isEqualTo(2);
        assertThat(backend.deletedPrograms).isEqualTo(1);
        assertThat(backend.deletedShaders).isEqualTo(2);

        writeShader("opaque-baseline", "broken", "fragment-two");
        backend.failCompileSource = "broken";
        reloader.poll();
        assertThat(reloader.programHandle()).isEqualTo(replacementProgram);
        assertThat(reloader.material().scalars().redMultiplier()).isEqualTo(0.25f);

        backend.failCompileSource = null;
        writeShader("opaque-baseline", "vertex-three", "fragment-three");
        backend.linkSucceeded = false;
        reloader.poll();
        assertThat(reloader.programHandle()).isEqualTo(replacementProgram);

        backend.linkSucceeded = true;
        reloader.close();
        reloader.close();
        registry.assertNoOpenResources();

    }

    @Test
    void nonOwnerPollAndCloseRejectBeforeBackendMutation() throws Exception {

        writeShader("opaque-baseline", "vertex", "fragment");
        OpenGlThreadGuard guard = boundGuard();
        NativeResourceRegistry registry = new NativeResourceRegistry();
        FakeBackend backend = new FakeBackend();
        MutableMaterialHandle handle = new MutableMaterialHandle(material("opaque-baseline", 1.0f));
        DevelopmentMaterialHotReloader reloader = DevelopmentMaterialHotReloader.create(handle, tempDir, guard, registry, backend, 101, 102);

        int callsBefore = backend.backendCalls;
        AtomicReference<Throwable> pollFailure = new AtomicReference<>();
        Thread pollWorker = Thread.ofPlatform().start(() -> {
            try {
                reloader.poll();
            } catch (Throwable failure) {
                pollFailure.set(failure);
            }
        });
        pollWorker.join(5_000L);
        assertThat(pollFailure.get()).isInstanceOf(IllegalStateException.class);
        assertThat(backend.backendCalls).isEqualTo(callsBefore);

        AtomicReference<Throwable> closeFailure = new AtomicReference<>();
        Thread closeWorker = Thread.ofPlatform().start(() -> {
            try {
                reloader.close();
            } catch (Throwable failure) {
                closeFailure.set(failure);
            }
        });
        closeWorker.join(5_000L);
        assertThat(closeFailure.get()).isInstanceOf(IllegalStateException.class);
        assertThat(backend.backendCalls).isEqualTo(callsBefore);

        reloader.close();
        registry.assertNoOpenResources();

    }

    private void writeShader(String key, String vertex, String fragment) throws Exception {

        Files.writeString(tempDir.resolve(key + ".vert"), vertex, StandardCharsets.UTF_8);
        Files.writeString(tempDir.resolve(key + ".frag"), fragment, StandardCharsets.UTF_8);

    }

    private static MaterialAsset material(String shaderKey, float red) {

        return new MaterialAsset(shaderKey, red, 0.5f, 0.75f, 1.0f);

    }

    private static OpenGlThreadGuard boundGuard() {

        GlfwWindow window = new GlfwWindow(1, 1, "guard fixture", new EngineLogger(event -> {
        }), new NativeResourceRegistry());
        OpenGlThreadGuard guard = window.openGlThreadGuard();
        try {
            Method bind = OpenGlThreadGuard.class.getDeclaredMethod("bindOwnerThread", Thread.class);
            bind.setAccessible(true);
            bind.invoke(guard, Thread.currentThread());
            return guard;
        } catch (NoSuchMethodException | IllegalAccessException failure) {
            throw new AssertionError(failure);
        } catch (InvocationTargetException failure) {
            throw new AssertionError(failure.getCause());
        }

    }

    private static final class MutableMaterialHandle implements ResourceHandle<MaterialAsset> {
        private MaterialAsset value;
        private ResourceHandleState state = ResourceHandleState.READY;

        private MutableMaterialHandle(MaterialAsset value) {

            this.value = value;

        }

        void set(MaterialAsset value) {

            this.value = value;

        }

        @Override
        public AssetId assetId() {

            return MATERIAL_ID;

        }

        @Override
        public ResourceHandleState state() {

            return state;

        }

        @Override
        public Optional<MaterialAsset> readyValue() {

            return state == ResourceHandleState.READY ? Optional.of(value) : Optional.empty();

        }

        @Override
        public MaterialAsset requireReady() {

            if (state != ResourceHandleState.READY) {
                throw new IllegalStateException();
            }
            return value;

        }

        @Override
        public void close() {

            state = ResourceHandleState.RELEASED;
            value = null;

        }
    }

    private static final class FakeBackend implements OpenGlResourceBackend {
        private int nextShader = 20;
        private int nextProgram = 30;
        private int createdPrograms;
        private int deletedPrograms;
        private int deletedShaders;
        private int backendCalls;
        private String currentSource;
        private String failCompileSource;
        private boolean linkSucceeded = true;

        @Override
        public int createBuffer() {

            throw unsupported();

        }

        @Override
        public void deleteBuffer(int handle) {

            throw unsupported();

        }

        @Override
        public void allocateDynamicBufferStorage(int handle, long capacityBytes) {

            throw unsupported();

        }

        @Override
        public void uploadBufferSubData(int handle, long offsetBytes, java.nio.ByteBuffer data) {

            throw unsupported();

        }

        @Override
        public long createFence() {

            throw unsupported();

        }

        @Override
        public FenceStatus fenceStatus(long fenceHandle) {

            throw unsupported();

        }

        @Override
        public void deleteFence(long fenceHandle) {

            throw unsupported();

        }

        @Override
        public int createVertexArray() {

            throw unsupported();

        }

        @Override
        public void deleteVertexArray(int handle) {

            throw unsupported();

        }

        @Override
        public int createTexture() {

            throw unsupported();

        }

        @Override
        public void deleteTexture(int handle) {

            throw unsupported();

        }

        @Override
        public int createSampler() {

            throw unsupported();

        }

        @Override
        public void deleteSampler(int handle) {

            throw unsupported();

        }

        @Override
        public int createFramebuffer() {

            throw unsupported();

        }

        @Override
        public void deleteFramebuffer(int handle) {

            throw unsupported();

        }

        @Override
        public int createShader(int shaderType) {

            backendCalls++;
            return nextShader++;

        }

        @Override
        public void shaderSource(int shader, String source) {

            backendCalls++;
            currentSource = source;

        }

        @Override
        public void compileShader(int shader) {

            backendCalls++;

        }

        @Override
        public boolean shaderCompileSucceeded(int shader) {

            backendCalls++;
            return failCompileSource == null || !failCompileSource.equals(currentSource);

        }

        @Override
        public String shaderInfoLog(int shader) {

            return "fixture compile failure";

        }

        @Override
        public void deleteShader(int shader) {

            backendCalls++;
            deletedShaders++;

        }

        @Override
        public int createProgram() {

            backendCalls++;
            createdPrograms++;
            return nextProgram++;

        }

        @Override
        public void attachShader(int program, int shader) {

            backendCalls++;

        }

        @Override
        public void linkProgram(int program) {

            backendCalls++;

        }

        @Override
        public boolean programLinkSucceeded(int program) {

            backendCalls++;
            return linkSucceeded;

        }

        @Override
        public String programInfoLog(int program) {

            return "fixture link failure";

        }

        @Override
        public void detachShader(int program, int shader) {

            backendCalls++;

        }

        @Override
        public void deleteProgram(int program) {

            backendCalls++;
            deletedPrograms++;

        }

        private static UnsupportedOperationException unsupported() {

            return new UnsupportedOperationException("not used by P6-T14 fixture");

        }
    }
}
