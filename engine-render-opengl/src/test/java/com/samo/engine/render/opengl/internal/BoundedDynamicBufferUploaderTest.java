package com.samo.engine.render.opengl.internal;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.samo.engine.core.api.EngineLogger;
import com.samo.engine.core.api.NativeResourceRegistry;
import com.samo.engine.platform.api.GlfwWindow;
import com.samo.engine.platform.api.OpenGlThreadGuard;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class BoundedDynamicBufferUploaderTest {
    @Test
    void validatesConfigurationBeforeNativeCreation() {
        OpenGlThreadGuard guard = boundGuard();
        NativeResourceRegistry registry = new NativeResourceRegistry();
        FakeBackend backend = new FakeBackend();

        assertThrows(
                IllegalArgumentException.class,
                () -> BoundedDynamicBufferUploader.create(0, 4, guard, registry, backend));
        assertThrows(
                IllegalArgumentException.class,
                () -> BoundedDynamicBufferUploader.create(1, 0, guard, registry, backend));
        assertThrows(
                IllegalArgumentException.class,
                () -> BoundedDynamicBufferUploader.create(2, Long.MAX_VALUE, guard, registry, backend));

        assertEquals(0, backend.createdBuffers);
        registry.assertNoOpenResources();
    }

    @Test
    void creationRollbackKeepsSameThrowablePrimaryWithoutSelfSuppression() {
        OpenGlThreadGuard guard = boundGuard();
        NativeResourceRegistry registry = new NativeResourceRegistry();
        FakeBackend backend = new FakeBackend();
        RuntimeException primary = new IllegalStateException("shared allocation/delete failure");
        backend.allocationFailure = primary;
        backend.bufferDeleteFailure = primary;

        RuntimeException actual = assertThrows(
                RuntimeException.class,
                () -> BoundedDynamicBufferUploader.create(1, 4, guard, registry, backend));

        assertSame(primary, actual);
        assertEquals(0, actual.getSuppressed().length);
        assertEquals(1, backend.deletedBuffers);
        IllegalStateException registryFailure =
                assertThrows(IllegalStateException.class, registry::assertNoOpenResources);
        assertTrue(registryFailure.getMessage().contains("CLOSE_FAILED"));
    }

    @Test
    void exactCapacityUsesStrictRoundRobinOffsetsAndReusesSignaledSlot() {
        OpenGlThreadGuard guard = boundGuard();
        NativeResourceRegistry registry = new NativeResourceRegistry();
        FakeBackend backend = new FakeBackend();
        try (BoundedDynamicBufferUploader uploader =
                BoundedDynamicBufferUploader.create(3, 4, guard, registry, backend)) {
            BoundedDynamicBufferUploader.Slice first = uploader.upload(bytes(1, 2, 3, 4));
            BoundedDynamicBufferUploader.Slice second = uploader.upload(bytes(5));
            BoundedDynamicBufferUploader.Slice third = uploader.upload(bytes(6, 7));

            assertEquals(0L, first.offsetBytes());
            assertEquals(4L, second.offsetBytes());
            assertEquals(8L, third.offsetBytes());
            assertEquals(4, first.lengthBytes());
            assertArrayEquals(new byte[] {1, 2, 3, 4}, backend.uploads.get(0L));

            uploader.markSubmitted(first);
            uploader.markSubmitted(second);
            uploader.markSubmitted(third);
            backend.fenceStatus = OpenGlResourceBackend.FenceStatus.SIGNALED;

            BoundedDynamicBufferUploader.Slice wrapped = uploader.upload(bytes(9));
            assertEquals(0L, wrapped.offsetBytes());
            assertEquals(1L, wrapped.generation() - first.generation());
        }
        registry.assertNoOpenResources();
    }

    @Test
    void timeoutFailsBeforeUploadAndDoesNotAdvanceRing() {
        OpenGlThreadGuard guard = boundGuard();
        NativeResourceRegistry registry = new NativeResourceRegistry();
        FakeBackend backend = new FakeBackend();
        try (BoundedDynamicBufferUploader uploader =
                BoundedDynamicBufferUploader.create(1, 8, guard, registry, backend)) {
            BoundedDynamicBufferUploader.Slice first = uploader.upload(bytes(1, 2));
            uploader.markSubmitted(first);
            backend.fenceStatus = OpenGlResourceBackend.FenceStatus.TIMEOUT;
            int uploadCalls = backend.uploadCalls;

            assertThrows(IllegalStateException.class, () -> uploader.upload(bytes(3)));
            assertEquals(uploadCalls, backend.uploadCalls);
            assertEquals(0, backend.deletedFences);

            backend.fenceStatus = OpenGlResourceBackend.FenceStatus.SIGNALED;
            BoundedDynamicBufferUploader.Slice retry = uploader.upload(bytes(4));
            assertEquals(0L, retry.offsetBytes());
            assertEquals(uploadCalls + 1, backend.uploadCalls);
        }
        registry.assertNoOpenResources();
    }

    @Test
    void uploadedButUnsubmittedSlotCannotBeOverwrittenOnWrap() {
        OpenGlThreadGuard guard = boundGuard();
        NativeResourceRegistry registry = new NativeResourceRegistry();
        FakeBackend backend = new FakeBackend();
        try (BoundedDynamicBufferUploader uploader =
                BoundedDynamicBufferUploader.create(1, 4, guard, registry, backend)) {
            uploader.upload(bytes(1));
            int uploadCalls = backend.uploadCalls;

            IllegalStateException failure =
                    assertThrows(IllegalStateException.class, () -> uploader.upload(bytes(2)));

            assertTrue(failure.getMessage().contains("not submitted"));
            assertEquals(uploadCalls, backend.uploadCalls);
        }
        registry.assertNoOpenResources();
    }

    @Test
    void rejectsZeroLengthAndOverCapacityBeforeUpload() {
        OpenGlThreadGuard guard = boundGuard();
        NativeResourceRegistry registry = new NativeResourceRegistry();
        FakeBackend backend = new FakeBackend();
        try (BoundedDynamicBufferUploader uploader =
                BoundedDynamicBufferUploader.create(2, 4, guard, registry, backend)) {
            assertThrows(IllegalArgumentException.class, () -> uploader.upload(ByteBuffer.allocate(0)));
            assertThrows(IllegalArgumentException.class, () -> uploader.upload(ByteBuffer.allocate(5)));
            assertEquals(0, backend.uploadCalls);
        }
        registry.assertNoOpenResources();
    }

    @Test
    void staleSubmissionIsRejectedWithoutCreatingAnotherFence() {
        OpenGlThreadGuard guard = boundGuard();
        NativeResourceRegistry registry = new NativeResourceRegistry();
        FakeBackend backend = new FakeBackend();
        try (BoundedDynamicBufferUploader uploader =
                BoundedDynamicBufferUploader.create(1, 4, guard, registry, backend)) {
            BoundedDynamicBufferUploader.Slice slice = uploader.upload(bytes(1));
            uploader.markSubmitted(slice);
            int fenceCalls = backend.createdFences;

            assertThrows(IllegalStateException.class, () -> uploader.markSubmitted(slice));
            assertEquals(fenceCalls, backend.createdFences);
        }
        registry.assertNoOpenResources();
    }

    @Test
    void fenceRegistrationFailureDeletesNewFenceAndLeavesUploadUnsubmitted() {
        OpenGlThreadGuard guard = boundGuard();
        NativeResourceRegistry registry = new NativeResourceRegistry();
        FakeBackend backend = new FakeBackend();
        backend.nextFence = 100L;
        NativeResourceRegistry.Registration duplicate =
                registry.register("OpenGL sync", 100L, () -> { });

        try (BoundedDynamicBufferUploader uploader =
                BoundedDynamicBufferUploader.create(1, 4, guard, registry, backend)) {
            BoundedDynamicBufferUploader.Slice slice = uploader.upload(bytes(1));

            assertThrows(IllegalStateException.class, () -> uploader.markSubmitted(slice));
            assertEquals(1, backend.deletedFences);
        } finally {
            duplicate.close();
        }
        registry.assertNoOpenResources();
    }

    @Test
    void fenceRegistrationRollbackKeepsDistinctDeleteFailureSuppressed() {
        OpenGlThreadGuard guard = boundGuard();
        NativeResourceRegistry registry = new NativeResourceRegistry();
        FakeBackend backend = new FakeBackend();
        backend.nextFence = 100L;
        RuntimeException cleanupFailure = new IllegalStateException("fence delete failed");
        backend.fenceDeleteFailure = cleanupFailure;
        NativeResourceRegistry.Registration duplicate =
                registry.register("OpenGL sync", 100L, () -> { });

        try (BoundedDynamicBufferUploader uploader =
                BoundedDynamicBufferUploader.create(1, 4, guard, registry, backend)) {
            BoundedDynamicBufferUploader.Slice slice = uploader.upload(bytes(1));

            RuntimeException actual =
                    assertThrows(RuntimeException.class, () -> uploader.markSubmitted(slice));

            assertSame(cleanupFailure, actual.getSuppressed()[0]);
            assertEquals(1, actual.getSuppressed().length);
            assertEquals(1, backend.deletedFences);
        } finally {
            duplicate.close();
        }
        registry.assertNoOpenResources();
    }

    @Test
    void wrongThreadOperationsRejectBeforeBackendMutationAndOwnerCanContinue() throws Exception {
        OpenGlThreadGuard guard = boundGuard();
        NativeResourceRegistry registry = new NativeResourceRegistry();
        FakeBackend backend = new FakeBackend();
        BoundedDynamicBufferUploader uploader =
                BoundedDynamicBufferUploader.create(2, 4, guard, registry, backend);
        BoundedDynamicBufferUploader.Slice slice = uploader.upload(bytes(1));
        int uploadCalls = backend.uploadCalls;
        int fenceCalls = backend.createdFences;

        AtomicReference<Throwable> uploadFailure = runWorker(() -> uploader.upload(bytes(2)));
        AtomicReference<Throwable> submitFailure = runWorker(() -> uploader.markSubmitted(slice));
        AtomicReference<Throwable> closeFailure = runWorker(uploader::close);

        assertTrue(uploadFailure.get() instanceof IllegalStateException);
        assertTrue(submitFailure.get() instanceof IllegalStateException);
        assertTrue(closeFailure.get() instanceof IllegalStateException);
        assertEquals(uploadCalls, backend.uploadCalls);
        assertEquals(fenceCalls, backend.createdFences);
        assertEquals(0, backend.deletedBuffers);

        uploader.markSubmitted(slice);
        uploader.close();
        registry.assertNoOpenResources();
    }

    @Test
    void fenceWaitFailureDoesNotUploadOrAdvance() {
        OpenGlThreadGuard guard = boundGuard();
        NativeResourceRegistry registry = new NativeResourceRegistry();
        FakeBackend backend = new FakeBackend();
        try (BoundedDynamicBufferUploader uploader =
                BoundedDynamicBufferUploader.create(1, 4, guard, registry, backend)) {
            BoundedDynamicBufferUploader.Slice first = uploader.upload(bytes(1));
            uploader.markSubmitted(first);
            backend.fenceStatus = OpenGlResourceBackend.FenceStatus.FAILED;
            int uploads = backend.uploadCalls;

            assertThrows(IllegalStateException.class, () -> uploader.upload(bytes(2)));
            assertEquals(uploads, backend.uploadCalls);
        }
        registry.assertNoOpenResources();
    }

    private static AtomicReference<Throwable> runWorker(Runnable operation) throws InterruptedException {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread worker = Thread.ofPlatform().start(() -> {
            try {
                operation.run();
            } catch (Throwable actual) {
                failure.set(actual);
            }
        });
        worker.join(5_000L);
        return failure;
    }

    private static ByteBuffer bytes(int... values) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(values.length);
        for (int value : values) {
            buffer.put((byte) value);
        }
        return buffer.flip();
    }

    private static OpenGlThreadGuard boundGuard() {
        GlfwWindow window = new GlfwWindow(
                1,
                1,
                "guard fixture",
                new EngineLogger(event -> { }),
                new NativeResourceRegistry());
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

    private static final class FakeBackend implements OpenGlResourceBackend {
        private int bufferHandle = 41;
        private long nextFence = 100L;
        private int createdBuffers;
        private int deletedBuffers;
        private int uploadCalls;
        private int createdFences;
        private int deletedFences;
        private long allocatedCapacity;
        private RuntimeException allocationFailure;
        private RuntimeException bufferDeleteFailure;
        private RuntimeException fenceDeleteFailure;
        private FenceStatus fenceStatus = FenceStatus.SIGNALED;
        private final Map<Long, byte[]> uploads = new HashMap<>();

        @Override
        public int createBuffer() {
            createdBuffers++;
            return bufferHandle;
        }

        @Override
        public void deleteBuffer(int handle) {
            deletedBuffers++;
            if (bufferDeleteFailure != null) {
                throw bufferDeleteFailure;
            }
        }

        @Override
        public void allocateDynamicBufferStorage(int handle, long capacityBytes) {
            allocatedCapacity = capacityBytes;
            if (allocationFailure != null) {
                throw allocationFailure;
            }
        }

        @Override
        public void uploadBufferSubData(int handle, long offsetBytes, ByteBuffer data) {
            uploadCalls++;
            ByteBuffer copy = data.duplicate();
            byte[] bytes = new byte[copy.remaining()];
            copy.get(bytes);
            uploads.put(offsetBytes, bytes);
        }

        @Override
        public long createFence() {
            createdFences++;
            return nextFence++;
        }

        @Override
        public FenceStatus fenceStatus(long fenceHandle) {
            return fenceStatus;
        }

        @Override
        public void deleteFence(long fenceHandle) {
            deletedFences++;
            if (fenceDeleteFailure != null) {
                throw fenceDeleteFailure;
            }
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
            return new UnsupportedOperationException("not used by P5-T04");
        }
    }
}
