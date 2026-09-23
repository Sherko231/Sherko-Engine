package com.samo.engine.render.opengl.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.samo.engine.assets.api.AssetId;
import com.samo.engine.assets.api.AssetLoader;
import com.samo.engine.assets.api.AssetLoaders;
import com.samo.engine.assets.api.MeshAsset;
import com.samo.engine.assets.api.ResourceHandle;
import com.samo.engine.assets.api.ResourceHandleState;
import com.samo.engine.core.api.EngineLogger;
import com.samo.engine.core.api.NativeResourceRegistry;
import com.samo.engine.platform.api.GlfwWindow;
import com.samo.engine.platform.api.OpenGlThreadGuard;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.CRC32C;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RuntimeMeshGpuUploaderTest {
    private static final AssetId ASSET_ID = AssetId.parse("01234567-89ab-cdef-fedc-ba9876543210");
    private static final int VERTEX_COUNT = 90_000;
    private static final int INDEX_COUNT = 90_000;

    @TempDir
    Path tempDir;

    @Test
    void largeCookedMeshLoadsOffThreadWithoutBackendCallsThenUploadsOnlyOnOwnerThread() throws Exception {

        byte[] cooked = largeSmes();
        assertThat(cooked.length).isGreaterThan(1024 * 1024);
        createCache(cooked);

        RecordingBackend backend = new RecordingBackend();
        NativeResourceRegistry registry = new NativeResourceRegistry();
        OpenGlThreadGuard guard = boundGuard();

        try (AssetLoader loader = AssetLoaders.open(tempDir)) {
            Thread caller = Thread.currentThread();
            ResourceHandle<MeshAsset> handle = loader.loadMesh(ASSET_ID);
            assertThat(handle.state()).isIn(ResourceHandleState.LOADING, ResourceHandleState.READY);

            awaitReady(handle);

            assertThat(handle.state()).isEqualTo(ResourceHandleState.READY);
            assertThat(backend.calls).isEmpty();
            MeshAsset.Primitive primitive = handle.requireReady().primitives().getFirst();
            assertThat(primitive.positions().length).isEqualTo(VERTEX_COUNT * 3);
            assertThat(primitive.indices().length).isEqualTo(INDEX_COUNT);
            assertThat(primitive.positions()).startsWith(-1.0f, 0.0f, -2.0f, 1.0f, 0.0f, -2.0f, 0.0f, 1.0f, -2.0f);
            assertThat(primitive.indices()).startsWith(0, 1, 2, 3, 4, 5);

            RuntimeMeshGpuUploader uploader = new RuntimeMeshGpuUploader(guard, registry, backend);
            try (RuntimeMeshGpuUploader.UploadedRuntimeMesh uploaded = uploader.upload(handle)) {
                assertThat(uploaded.primitives()).hasSize(1);
                assertThat(uploaded.primitives().getFirst().indexCount()).isEqualTo(INDEX_COUNT);
                assertThat(backend.allocations).containsExactly((long) VERTEX_COUNT * 3 * Float.BYTES, (long) INDEX_COUNT * Integer.BYTES);
                assertThat(backend.uploads).hasSize(2);
                assertThat(backend.uploads.get(0).bytes.length).isEqualTo(VERTEX_COUNT * 3 * Float.BYTES);
                assertThat(backend.uploads.get(1).bytes.length).isEqualTo(INDEX_COUNT * Integer.BYTES);
                ByteBuffer vertices = ByteBuffer.wrap(backend.uploads.get(0).bytes).order(ByteOrder.nativeOrder());
                assertThat(vertices.getFloat()).isEqualTo(-1.0f);
                assertThat(vertices.getFloat()).isZero();
                assertThat(vertices.getFloat()).isEqualTo(-2.0f);
                ByteBuffer indices = ByteBuffer.wrap(backend.uploads.get(1).bytes).order(ByteOrder.nativeOrder());
                assertThat(indices.getInt()).isZero();
                assertThat(indices.getInt()).isEqualTo(1);
                assertThat(indices.getInt()).isEqualTo(2);
                assertThat(backend.callThreads).allMatch(thread -> thread == caller);
            }
        }

        registry.assertNoOpenResources();

    }

    @Test
    void wrongThreadUploadFailsBeforeAnyBackendMutation() throws Exception {

        RecordingBackend backend = new RecordingBackend();
        NativeResourceRegistry registry = new NativeResourceRegistry();
        OpenGlThreadGuard guard = boundGuard();
        RuntimeMeshGpuUploader uploader = new RuntimeMeshGpuUploader(guard, registry, backend);
        ResourceHandle<MeshAsset> handle = readyHandle(simpleMesh());
        AtomicReference<Throwable> failure = new AtomicReference<>();

        Thread worker = Thread.ofPlatform().start(() -> {
            try {
                uploader.upload(handle);
            } catch (Throwable actual) {
                failure.set(actual);
            }
        });
        worker.join(5_000L);

        assertThat(failure.get()).isInstanceOf(IllegalStateException.class);
        assertThat(backend.calls).isEmpty();
        registry.assertNoOpenResources();

    }

    @Test
    void partialUploadFailureRollsBackOwnedBuffersExactlyOnce() {

        RecordingBackend backend = new RecordingBackend();
        backend.failAllocationCall = 2;
        NativeResourceRegistry registry = new NativeResourceRegistry();
        RuntimeMeshGpuUploader uploader = new RuntimeMeshGpuUploader(boundGuard(), registry, backend);

        assertThatThrownBy(() -> uploader.upload(readyHandle(simpleMesh()))).isInstanceOf(IllegalStateException.class).hasMessageContaining("fixture allocation failure");

        assertThat(backend.deletedBuffers).isEqualTo(2);
        registry.assertNoOpenResources();

    }

    @Test
    void uploadedMeshCloseIsIdempotentAndOwnerThreadGuarded() throws Exception {

        RecordingBackend backend = new RecordingBackend();
        NativeResourceRegistry registry = new NativeResourceRegistry();
        OpenGlThreadGuard guard = boundGuard();
        RuntimeMeshGpuUploader uploader = new RuntimeMeshGpuUploader(guard, registry, backend);
        RuntimeMeshGpuUploader.UploadedRuntimeMesh uploaded = uploader.upload(readyHandle(simpleMesh()));
        AtomicReference<Throwable> failure = new AtomicReference<>();

        Thread worker = Thread.ofPlatform().start(() -> {
            try {
                uploaded.close();
            } catch (Throwable actual) {
                failure.set(actual);
            }
        });
        worker.join(5_000L);

        assertThat(failure.get()).isInstanceOf(IllegalStateException.class);
        assertThat(backend.deletedBuffers).isZero();

        uploaded.close();
        uploaded.close();
        assertThat(backend.deletedBuffers).isEqualTo(2);
        registry.assertNoOpenResources();

    }

    private void createCache(byte[] cooked) throws Exception {

        Path assets = Files.createDirectories(tempDir.resolve("assets"));
        Files.write(assets.resolve(ASSET_ID + ".bin"), cooked);
        Files.writeString(tempDir.resolve("manifest.json"), """
            {
              "schemaVersion":1,
              "assets":[
                {
                  "assetId":"%s",
                  "assetType":"MESH",
                  "sourcePath":"large.gltf",
                  "cookedPath":"assets/%s.bin",
                  "byteSize":%d
                }
              ]
            }
            """.formatted(ASSET_ID, ASSET_ID, cooked.length));

    }

    private static byte[] largeSmes() {

        byte[] name = "LargeMesh".getBytes(StandardCharsets.UTF_8);
        int vertexBytes = VERTEX_COUNT * 3 * Float.BYTES;
        int indexBytes = INDEX_COUNT * Integer.BYTES;
        int bodyLength = 48 + name.length + vertexBytes + indexBytes;
        ByteBuffer body = ByteBuffer.allocate(bodyLength).order(ByteOrder.LITTLE_ENDIAN);
        body.putInt(0);
        body.putInt(name.length);
        body.putInt(VERTEX_COUNT);
        body.putInt(INDEX_COUNT);
        body.putInt(0);
        body.putInt(3 * Float.BYTES);
        body.putFloat(-1.0f).putFloat(0.0f).putFloat(-2.0f);
        body.putFloat(1.0f).putFloat(1.0f).putFloat(-2.0f);
        body.put(name);
        for (int vertex = 0; vertex < VERTEX_COUNT; vertex++) {
            switch (vertex % 3) {
                case 0 -> body.putFloat(-1.0f).putFloat(0.0f).putFloat(-2.0f);
                case 1 -> body.putFloat(1.0f).putFloat(0.0f).putFloat(-2.0f);
                default -> body.putFloat(0.0f).putFloat(1.0f).putFloat(-2.0f);
            }
        }
        for (int index = 0; index < INDEX_COUNT; index++) {
            body.putInt(index);
        }

        CRC32C crc = new CRC32C();
        crc.update(body.array(), 0, bodyLength);
        ByteBuffer file = ByteBuffer.allocate(20 + bodyLength).order(ByteOrder.LITTLE_ENDIAN);
        file.put(new byte[]{'S', 'M', 'E', 'S'});
        file.putInt(1);
        file.putInt(1);
        file.putInt(bodyLength);
        file.putInt((int) crc.getValue());
        file.put(body.array());
        return file.array();

    }

    private static void awaitReady(ResourceHandle<MeshAsset> handle) throws InterruptedException {

        long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
        while (handle.state() == ResourceHandleState.LOADING && System.nanoTime() < deadline) {
            Thread.sleep(5L);
        }
        assertThat(handle.state()).isEqualTo(ResourceHandleState.READY);

    }

    private static ResourceHandle<MeshAsset> readyHandle(MeshAsset asset) {

        return new ResourceHandle<>() {
            @Override
            public AssetId assetId() {

                return ASSET_ID;

            }

            @Override
            public ResourceHandleState state() {

                return ResourceHandleState.READY;

            }

            @Override
            public java.util.Optional<MeshAsset> readyValue() {

                return java.util.Optional.of(asset);

            }

            @Override
            public MeshAsset requireReady() {

                return asset;

            }

            @Override
            public void close() {

            }
        };

    }

    private static MeshAsset simpleMesh() {

        return new MeshAsset(
            List.of(new MeshAsset.Primitive(0, "Triangle", new float[]{-1.0f, 0.0f, -2.0f, 1.0f, 0.0f, -2.0f, 0.0f, 1.0f, -2.0f}, null, null, null, null, new int[]{0, 1, 2})));

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

    private record Upload(int handle, byte[] bytes) {
    }

    private static final class RecordingBackend implements OpenGlResourceBackend {
        private int nextBuffer = 10;
        private int allocationCalls;
        private int failAllocationCall;
        private int deletedBuffers;
        private final List<String> calls = new ArrayList<>();
        private final List<Thread> callThreads = new ArrayList<>();
        private final List<Long> allocations = new ArrayList<>();
        private final List<Upload> uploads = new ArrayList<>();

        @Override
        public int createBuffer() {

            record("createBuffer");
            return nextBuffer++;

        }

        @Override
        public void deleteBuffer(int handle) {

            record("deleteBuffer");
            deletedBuffers++;

        }

        @Override
        public void allocateDynamicBufferStorage(int handle, long capacityBytes) {

            record("allocate");
            allocationCalls++;
            if (allocationCalls == failAllocationCall) {
                throw new IllegalStateException("fixture allocation failure");
            }
            allocations.add(capacityBytes);

        }

        @Override
        public void uploadBufferSubData(int handle, long offsetBytes, ByteBuffer data) {

            record("upload");
            ByteBuffer copy = data.duplicate();
            byte[] bytes = new byte[copy.remaining()];
            copy.get(bytes);
            uploads.add(new Upload(handle, bytes));

        }

        private void record(String call) {

            calls.add(call);
            callThreads.add(Thread.currentThread());

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

            throw unsupported();

        }

        @Override
        public void shaderSource(int shader, String source) {

            throw unsupported();

        }

        @Override
        public void compileShader(int shader) {

            throw unsupported();

        }

        @Override
        public boolean shaderCompileSucceeded(int shader) {

            throw unsupported();

        }

        @Override
        public String shaderInfoLog(int shader) {

            throw unsupported();

        }

        @Override
        public void deleteShader(int shader) {

            throw unsupported();

        }

        @Override
        public int createProgram() {

            throw unsupported();

        }

        @Override
        public void attachShader(int program, int shader) {

            throw unsupported();

        }

        @Override
        public void linkProgram(int program) {

            throw unsupported();

        }

        @Override
        public boolean programLinkSucceeded(int program) {

            throw unsupported();

        }

        @Override
        public String programInfoLog(int program) {

            throw unsupported();

        }

        @Override
        public void detachShader(int program, int shader) {

            throw unsupported();

        }

        @Override
        public void deleteProgram(int program) {

            throw unsupported();

        }

        private static UnsupportedOperationException unsupported() {

            return new UnsupportedOperationException("not used by P6-T13 fixture");

        }
    }
}
