package com.samo.engine.world.internal;

import com.samo.engine.world.api.EntityId;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Objects;

final class EntityIdAllocator {
    private final ArrayList<EntitySlot> slots = new ArrayList<>();
    private final ArrayDeque<Integer> freeIndices = new ArrayDeque<>();

    EntityId create() {

        if (!freeIndices.isEmpty()) {
            int index = freeIndices.removeFirst();
            return slots.get(index).activate(index);
        }

        requireFreshIndexCapacity(slots.size());
        int index = slots.size();
        EntitySlot slot = new EntitySlot(0);
        slots.add(slot);
        return slot.activate(index);

    }

    boolean isAlive(EntityId id) {

        Objects.requireNonNull(id, "id");
        int index = id.index();
        return index < slots.size() && slots.get(index).matches(id);

    }

    boolean destroy(EntityId id) {

        Objects.requireNonNull(id, "id");
        int index = id.index();
        if (index >= slots.size()) {
            return false;
        }

        EntitySlot slot = slots.get(index);
        if (!slot.destroy(id)) {
            return false;
        }
        if (slot.reusable()) {
            freeIndices.addLast(index);
        }
        return true;

    }

    static void requireFreshIndexCapacity(int slotCount) {

        if (slotCount < 0) {
            throw new IllegalArgumentException("slotCount must be non-negative");
        }
        if (slotCount == Integer.MAX_VALUE) {
            throw new IllegalStateException("entity index space is exhausted");
        }

    }
}

final class EntitySlot {
    private int generation;
    private boolean live;
    private boolean retired;

    EntitySlot(int generation) {

        if (generation < 0) {
            throw new IllegalArgumentException("generation must be non-negative");
        }
        this.generation = generation;

    }

    EntityId activate(int index) {

        if (live || retired) {
            throw new IllegalStateException("entity slot is not reusable");
        }
        live = true;
        return new EntityId(index, generation);

    }

    boolean matches(EntityId id) {

        return live && id.generation() == generation;

    }

    boolean destroy(EntityId id) {

        if (!matches(id)) {
            return false;
        }

        live = false;
        if (generation == Integer.MAX_VALUE) {
            retired = true;
        } else {
            generation++;
        }
        return true;

    }

    boolean reusable() {

        return !live && !retired;

    }

    int generation() {

        return generation;

    }

    boolean retired() {

        return retired;

    }
}
