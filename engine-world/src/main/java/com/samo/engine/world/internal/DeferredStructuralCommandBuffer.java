package com.samo.engine.world.internal;

import com.samo.engine.world.api.EntityId;
import java.util.ArrayDeque;
import java.util.Objects;

final class DeferredStructuralCommandBuffer {
    private final EntityIdAllocator allocator;
    private final ArrayDeque<StructuralCommand> commands = new ArrayDeque<>();
    private boolean flushing;

    DeferredStructuralCommandBuffer(EntityIdAllocator allocator) {

        this.allocator = Objects.requireNonNull(allocator, "allocator");

    }

    PendingEntityCreation createEntity() {

        requireRecordingAllowed();
        PendingEntityCreation pendingCreation = new PendingEntityCreation();
        commands.addLast(() -> pendingCreation.resolve(allocator.create()));
        return pendingCreation;

    }

    void destroyEntity(EntityId id) {

        Objects.requireNonNull(id, "id");
        requireRecordingAllowed();
        commands.addLast(() -> allocator.destroy(id));

    }

    <T> void addComponent(PackedComponentStore<T> store, EntityId id, T component) {

        Objects.requireNonNull(store, "store");
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(component, "component");
        requireRecordingAllowed();
        commands.addLast(() -> store.add(id, component));

    }

    <T> void removeComponent(PackedComponentStore<T> store, EntityId id) {

        Objects.requireNonNull(store, "store");
        Objects.requireNonNull(id, "id");
        requireRecordingAllowed();
        commands.addLast(() -> store.remove(id));

    }

    void flush() {

        if (flushing) {
            throw new IllegalStateException("structural command buffer is already flushing");
        }

        flushing = true;
        try {
            while (!commands.isEmpty()) {
                StructuralCommand command = commands.removeFirst();
                command.apply();
            }
        } finally {
            flushing = false;
        }

    }

    private void requireRecordingAllowed() {

        if (flushing) {
            throw new IllegalStateException("structural commands cannot be recorded during flush");
        }

    }

    @FunctionalInterface
    private interface StructuralCommand {
        void apply();
    }

    static final class PendingEntityCreation {
        private EntityId resolvedId;

        EntityId resolvedId() {

            if (resolvedId == null) {
                throw new IllegalStateException("entity creation has not been flushed");
            }
            return resolvedId;

        }

        private void resolve(EntityId id) {

            Objects.requireNonNull(id, "id");
            if (resolvedId != null) {
                throw new IllegalStateException("entity creation is already resolved");
            }
            resolvedId = id;

        }
    }
}
