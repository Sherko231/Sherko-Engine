package com.samo.engine.world.internal;

import com.samo.engine.world.api.EntityId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.BiConsumer;

final class PackedComponentStore<T> {
    private static final int INITIAL_SPARSE_CAPACITY = 4;

    private final EntityIdAllocator allocator;
    private final ArrayList<EntityId> denseEntities = new ArrayList<>();
    private final ArrayList<T> denseComponents = new ArrayList<>();
    private int[] sparsePositions = new int[0];
    private boolean iterating;

    PackedComponentStore(EntityIdAllocator allocator) {

        this.allocator = Objects.requireNonNull(allocator, "allocator");

    }

    boolean add(EntityId id, T component) {

        requireStructuralMutationAllowed();
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(component, "component");
        if (!allocator.isAlive(id)) {
            return false;
        }

        int index = id.index();
        ensureSparseCapacity(index);
        int existingPosition = sparsePosition(index);
        if (existingPosition >= 0) {
            EntityId existingId = denseEntities.get(existingPosition);
            if (existingId.equals(id)) {
                return false;
            }
            removeAt(existingPosition);
        }

        int densePosition = denseEntities.size();
        denseEntities.add(id);
        denseComponents.add(component);
        sparsePositions[index] = densePosition + 1;
        return true;

    }

    T get(EntityId id) {

        Objects.requireNonNull(id, "id");
        if (!allocator.isAlive(id)) {
            return null;
        }

        int position = exactPosition(id);
        return position >= 0 ? denseComponents.get(position) : null;

    }

    boolean remove(EntityId id) {

        requireStructuralMutationAllowed();
        Objects.requireNonNull(id, "id");
        if (!allocator.isAlive(id)) {
            return false;
        }

        int position = exactPosition(id);
        if (position < 0) {
            return false;
        }

        removeAt(position);
        return true;

    }

    int size() {

        if (iterating) {
            int liveCount = 0;
            for (EntityId id : denseEntities) {
                if (allocator.isAlive(id)) {
                    liveCount++;
                }
            }
            return liveCount;
        }

        purgeStaleEntries();
        return denseEntities.size();

    }

    void forEach(BiConsumer<? super EntityId, ? super T> consumer) {

        Objects.requireNonNull(consumer, "consumer");
        if (iterating) {
            throw new IllegalStateException("component store is already iterating");
        }

        purgeStaleEntries();
        iterating = true;
        try {
            int entryCount = denseEntities.size();
            for (int index = 0; index < entryCount; index++) {
                EntityId id = denseEntities.get(index);
                if (allocator.isAlive(id)) {
                    consumer.accept(id, denseComponents.get(index));
                }
            }
        } finally {
            iterating = false;
            purgeStaleEntries();
        }

    }

    private void purgeStaleEntries() {

        int position = 0;
        while (position < denseEntities.size()) {
            if (allocator.isAlive(denseEntities.get(position))) {
                position++;
            } else {
                removeAt(position);
            }
        }

    }

    private int exactPosition(EntityId id) {

        int position = sparsePosition(id.index());
        if (position < 0 || !denseEntities.get(position).equals(id)) {
            return -1;
        }
        return position;

    }

    private int sparsePosition(int entityIndex) {

        if (entityIndex >= sparsePositions.length) {
            return -1;
        }
        return sparsePositions[entityIndex] - 1;

    }

    private void removeAt(int position) {

        int lastPosition = denseEntities.size() - 1;
        EntityId removedId = denseEntities.get(position);
        sparsePositions[removedId.index()] = 0;

        if (position != lastPosition) {
            EntityId movedId = denseEntities.get(lastPosition);
            T movedComponent = denseComponents.get(lastPosition);
            denseEntities.set(position, movedId);
            denseComponents.set(position, movedComponent);
            sparsePositions[movedId.index()] = position + 1;
        }

        denseEntities.remove(lastPosition);
        denseComponents.remove(lastPosition);

    }

    private void ensureSparseCapacity(int entityIndex) {

        if (entityIndex < sparsePositions.length) {
            return;
        }

        int requiredLength = entityIndex + 1;
        int newLength = sparsePositions.length == 0 ? INITIAL_SPARSE_CAPACITY : sparsePositions.length;
        while (newLength < requiredLength) {
            if (newLength > Integer.MAX_VALUE / 2) {
                newLength = requiredLength;
                break;
            }
            newLength *= 2;
        }
        sparsePositions = Arrays.copyOf(sparsePositions, newLength);

    }

    private void requireStructuralMutationAllowed() {

        if (iterating) {
            throw new IllegalStateException("component store cannot be structurally modified during iteration");
        }

    }
}
