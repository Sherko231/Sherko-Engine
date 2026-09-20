package com.samo.engine.render.opengl.internal;

import com.samo.engine.core.api.NativeResourceRegistry;
import com.samo.engine.platform.api.OpenGlThreadGuard;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

final class BoundedDynamicBufferUploader implements AutoCloseable {
    private enum SlotState {
        FREE, UPLOADED, SUBMITTED
    }

    static final class Slice {
        private final BoundedDynamicBufferUploader owner;
        private final int slotIndex;
        private final long offsetBytes;
        private final int lengthBytes;
        private final long generation;

        private Slice(BoundedDynamicBufferUploader owner, int slotIndex, long offsetBytes, int lengthBytes, long generation) {

            this.owner = owner;
            this.slotIndex = slotIndex;
            this.offsetBytes = offsetBytes;
            this.lengthBytes = lengthBytes;
            this.generation = generation;

        }

        int slotIndex() {

            return slotIndex;

        }

        long offsetBytes() {

            return offsetBytes;

        }

        int lengthBytes() {

            return lengthBytes;

        }

        long generation() {

            return generation;

        }
    }

    private static final class Slot {
        private SlotState state = SlotState.FREE;
        private long generation;
        private int lengthBytes;
        private long fenceHandle;
        private NativeResourceRegistry.Registration fenceRegistration;
    }

    private final OpenGlThreadGuard threadGuard;
    private final NativeResourceRegistry registry;
    private final OpenGlResourceBackend backend;
    private final OpenGlBuffer buffer;
    private final Slot[] slots;
    private final long slotCapacityBytes;
    private boolean closeAttempted;
    private int nextSlot;

    private BoundedDynamicBufferUploader(OpenGlThreadGuard threadGuard, NativeResourceRegistry registry, OpenGlResourceBackend backend, OpenGlBuffer buffer, int slotCount,
        long slotCapacityBytes) {

        this.threadGuard = threadGuard;
        this.registry = registry;
        this.backend = backend;
        this.buffer = buffer;
        this.slotCapacityBytes = slotCapacityBytes;
        this.slots = new Slot[slotCount];
        for (int index = 0; index < slotCount; index++) {
            slots[index] = new Slot();
        }

    }

    static BoundedDynamicBufferUploader create(int slotCount, long slotCapacityBytes, OpenGlThreadGuard threadGuard, NativeResourceRegistry registry,
        OpenGlResourceBackend backend) {

        OpenGlThreadGuard guard = Objects.requireNonNull(threadGuard, "threadGuard");
        NativeResourceRegistry resources = Objects.requireNonNull(registry, "registry");
        OpenGlResourceBackend gl = Objects.requireNonNull(backend, "backend");

        guard.assertOwnerThread();
        if (slotCount <= 0) {
            throw new IllegalArgumentException("slotCount must be positive");
        }
        if (slotCapacityBytes <= 0) {
            throw new IllegalArgumentException("slotCapacityBytes must be positive");
        }

        long totalCapacity;
        try {
            totalCapacity = Math.multiplyExact((long) slotCount, slotCapacityBytes);
        } catch (ArithmeticException overflow) {
            throw new IllegalArgumentException("total upload capacity overflows long", overflow);
        }

        OpenGlBuffer buffer = OpenGlBuffer.create(guard, resources, gl);
        try {
            gl.allocateDynamicBufferStorage(buffer.handle(), totalCapacity);
            return new BoundedDynamicBufferUploader(guard, resources, gl, buffer, slotCount, slotCapacityBytes);
        } catch (RuntimeException | Error failure) {
            CleanupFailureSuppression.runAndSuppress(failure, buffer::close);
            throw failure;
        }

    }

    Slice upload(ByteBuffer data) {

        threadGuard.assertOwnerThread();
        requireOpen();
        ByteBuffer source = Objects.requireNonNull(data, "data");
        int lengthBytes = source.remaining();
        if (lengthBytes <= 0) {
            throw new IllegalArgumentException("upload data must contain at least one byte");
        }
        if ((long) lengthBytes > slotCapacityBytes) {
            throw new IllegalArgumentException("upload exceeds slot capacity: bytes=" + lengthBytes + ", capacity=" + slotCapacityBytes);
        }

        Slot slot = slots[nextSlot];
        prepareSlotForReuse(slot, nextSlot);

        long offsetBytes = Math.multiplyExact((long) nextSlot, slotCapacityBytes);
        backend.uploadBufferSubData(buffer.handle(), offsetBytes, source);

        slot.generation++;
        slot.lengthBytes = lengthBytes;
        slot.state = SlotState.UPLOADED;

        Slice slice = new Slice(this, nextSlot, offsetBytes, lengthBytes, slot.generation);
        nextSlot = (nextSlot + 1) % slots.length;
        return slice;

    }

    void markSubmitted(Slice slice) {

        threadGuard.assertOwnerThread();
        requireOpen();
        Slice actual = Objects.requireNonNull(slice, "slice");
        if (actual.owner != this) {
            throw new IllegalArgumentException("slice belongs to a different uploader");
        }

        Slot slot = slots[actual.slotIndex];
        if (slot.state != SlotState.UPLOADED || slot.generation != actual.generation || slot.lengthBytes != actual.lengthBytes
            || actual.offsetBytes != Math.multiplyExact((long) actual.slotIndex, slotCapacityBytes)) {
            throw new IllegalStateException("slice is stale or is not the current uploaded slot generation");
        }

        long fenceHandle = backend.createFence();
        if (fenceHandle == 0L) {
            throw new IllegalStateException("OpenGL fence creation returned handle 0");
        }

        NativeResourceRegistry.Registration registration;
        try {
            registration = registry.register("OpenGL sync", fenceHandle, () -> {
                threadGuard.assertOwnerThread();
                backend.deleteFence(fenceHandle);
            });
        } catch (RuntimeException | Error failure) {
            CleanupFailureSuppression.runAndSuppress(failure, () -> backend.deleteFence(fenceHandle));
            throw failure;
        }

        slot.fenceHandle = fenceHandle;
        slot.fenceRegistration = registration;
        slot.state = SlotState.SUBMITTED;

    }

    private void prepareSlotForReuse(Slot slot, int slotIndex) {

        if (slot.state == SlotState.UPLOADED) {
            throw new IllegalStateException("slot " + slotIndex + " was uploaded but not submitted");
        }
        if (slot.state != SlotState.SUBMITTED) {
            return;
        }

        OpenGlResourceBackend.FenceStatus status = backend.fenceStatus(slot.fenceHandle);
        if (status == OpenGlResourceBackend.FenceStatus.TIMEOUT) {
            throw new IllegalStateException("slot " + slotIndex + " is still GPU-visible");
        }
        if (status == OpenGlResourceBackend.FenceStatus.FAILED) {
            throw new IllegalStateException("OpenGL fence wait failed for slot " + slotIndex);
        }

        releaseFence(slot);
        slot.state = SlotState.FREE;
        slot.lengthBytes = 0;

    }

    private void releaseFence(Slot slot) {

        NativeResourceRegistry.Registration registration = slot.fenceRegistration;
        if (registration == null) {
            throw new IllegalStateException("submitted slot is missing its sync registration");
        }
        slot.fenceRegistration = null;
        slot.fenceHandle = 0L;
        registration.close();

    }

    int bufferHandle() {

        return buffer.handle();

    }

    private void requireOpen() {

        if (closeAttempted) {
            throw new IllegalStateException("dynamic buffer uploader is closed");
        }

    }

    @Override
    public void close() {

        if (closeAttempted) {
            return;
        }
        threadGuard.assertOwnerThread();
        closeAttempted = true;

        List<Throwable> failures = new ArrayList<>();
        for (Slot slot : slots) {
            if (slot.fenceRegistration != null) {
                try {
                    releaseFence(slot);
                } catch (RuntimeException | Error failure) {
                    failures.add(failure);
                }
            }
            slot.state = SlotState.FREE;
            slot.lengthBytes = 0;
        }

        try {
            buffer.close();
        } catch (RuntimeException | Error failure) {
            failures.add(failure);
        }

        throwCleanupFailure(failures);

    }

    private static void throwCleanupFailure(List<Throwable> failures) {

        if (failures.isEmpty()) {
            return;
        }
        Throwable first = failures.getFirst();
        for (int index = 1; index < failures.size(); index++) {
            Throwable suppressed = failures.get(index);
            if (suppressed != first) {
                first.addSuppressed(suppressed);
            }
        }
        if (first instanceof RuntimeException runtimeFailure) {
            throw runtimeFailure;
        }
        throw (Error) first;

    }
}
