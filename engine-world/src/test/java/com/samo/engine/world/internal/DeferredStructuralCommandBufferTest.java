package com.samo.engine.world.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.samo.engine.world.api.EntityId;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class DeferredStructuralCommandBufferTest {
    @Test
    void queuedCurrentEntityDestroyDoesNotCorruptActivePackedIteration() {

        EntityIdAllocator allocator = new EntityIdAllocator();
        PackedComponentStore<TestComponent> store = new PackedComponentStore<>(allocator);
        DeferredStructuralCommandBuffer commands = new DeferredStructuralCommandBuffer(allocator);
        EntityId first = allocator.create();
        EntityId destroyed = allocator.create();
        EntityId third = allocator.create();
        TestComponent firstComponent = new TestComponent("first");
        TestComponent destroyedComponent = new TestComponent("destroyed");
        TestComponent thirdComponent = new TestComponent("third");

        assertThat(store.add(first, firstComponent)).isTrue();
        assertThat(store.add(destroyed, destroyedComponent)).isTrue();
        assertThat(store.add(third, thirdComponent)).isTrue();

        List<EntityId> visited = new ArrayList<>();
        store.forEach((id, component) -> {
            visited.add(id);
            if (id.equals(destroyed)) {
                commands.deferEntityDestruction(id);
                assertThat(allocator.isAlive(destroyed)).isTrue();
                assertThat(store.get(destroyed)).isSameAs(destroyedComponent);
            }
        });

        assertThat(visited).containsExactlyInAnyOrder(first, destroyed, third);
        assertThat(visited).doesNotHaveDuplicates();
        assertThat(allocator.isAlive(destroyed)).isTrue();
        assertThat(store.get(destroyed)).isSameAs(destroyedComponent);

        commands.flush();

        assertThat(allocator.isAlive(destroyed)).isFalse();
        assertThat(store.get(destroyed)).isNull();
        assertThat(store.get(first)).isSameAs(firstComponent);
        assertThat(store.get(third)).isSameAs(thirdComponent);
        assertThat(store.size()).isEqualTo(2);

    }

    @Test
    void staleMismatchedAndUnknownQueuedDestroyCannotAffectReplacementEntity() {

        EntityIdAllocator allocator = new EntityIdAllocator();
        DeferredStructuralCommandBuffer commands = new DeferredStructuralCommandBuffer(allocator);
        EntityId original = allocator.create();

        assertThat(allocator.destroy(original)).isTrue();
        EntityId replacement = allocator.create();
        EntityId wrongGeneration = new EntityId(replacement.index(), replacement.generation() + 1);
        EntityId unknown = new EntityId(99, 0);

        commands.deferEntityDestruction(original);
        commands.deferEntityDestruction(wrongGeneration);
        commands.deferEntityDestruction(unknown);
        commands.flush();

        assertThat(allocator.isAlive(replacement)).isTrue();

    }

    @Test
    void deferredCreateAllocatesOnlyWhenFlushedAndIsNeverReplayed() {

        EntityIdAllocator allocator = new EntityIdAllocator();
        DeferredStructuralCommandBuffer commands = new DeferredStructuralCommandBuffer(allocator);

        DeferredStructuralCommandBuffer.PendingEntityCreation pending = commands.deferEntityCreation();
        assertThatThrownBy(pending::resolvedId).isInstanceOf(IllegalStateException.class).hasMessageContaining("has not been flushed");

        EntityId immediate = allocator.create();
        assertThat(immediate.index()).isZero();

        commands.flush();
        EntityId created = pending.resolvedId();

        assertThat(created.index()).isEqualTo(1);
        assertThat(created.generation()).isZero();
        assertThat(allocator.isAlive(created)).isTrue();

        commands.flush();
        assertThat(pending.resolvedId()).isEqualTo(created);
        assertThat(allocator.create().index()).isEqualTo(2);

    }

    @Test
    void deferredComponentAddRecordedDuringIterationAppearsOnlyAfterFlush() {

        EntityIdAllocator allocator = new EntityIdAllocator();
        PackedComponentStore<TestComponent> store = new PackedComponentStore<>(allocator);
        DeferredStructuralCommandBuffer commands = new DeferredStructuralCommandBuffer(allocator);
        EntityId iterated = allocator.create();
        EntityId target = allocator.create();
        TestComponent targetComponent = new TestComponent("target");

        assertThat(store.add(iterated, new TestComponent("iterated"))).isTrue();

        store.forEach((id, component) -> {
            commands.deferComponentAdd(store, target, targetComponent);
            assertThat(store.get(target)).isNull();
        });

        assertThat(store.get(target)).isNull();
        commands.flush();
        assertThat(store.get(target)).isSameAs(targetComponent);

    }

    @Test
    void deferredComponentRemoveRecordedDuringIterationAppliesOnlyAfterFlush() {

        EntityIdAllocator allocator = new EntityIdAllocator();
        PackedComponentStore<TestComponent> store = new PackedComponentStore<>(allocator);
        DeferredStructuralCommandBuffer commands = new DeferredStructuralCommandBuffer(allocator);
        EntityId first = allocator.create();
        EntityId removed = allocator.create();
        TestComponent removedComponent = new TestComponent("removed");

        assertThat(store.add(first, new TestComponent("first"))).isTrue();
        assertThat(store.add(removed, removedComponent)).isTrue();

        store.forEach((id, component) -> {
            if (id.equals(removed)) {
                commands.deferComponentRemoval(store, removed);
                assertThat(store.get(removed)).isSameAs(removedComponent);
            }
        });

        assertThat(store.get(removed)).isSameAs(removedComponent);
        commands.flush();
        assertThat(store.get(removed)).isNull();
        assertThat(store.size()).isEqualTo(1);

    }

    @Test
    void conflictingComponentCommandsExecuteInFifoOrder() {

        EntityIdAllocator allocator = new EntityIdAllocator();
        PackedComponentStore<TestComponent> store = new PackedComponentStore<>(allocator);
        DeferredStructuralCommandBuffer commands = new DeferredStructuralCommandBuffer(allocator);
        EntityId entity = allocator.create();
        TestComponent first = new TestComponent("first");
        TestComponent second = new TestComponent("second");

        commands.deferComponentAdd(store, entity, first);
        commands.deferComponentRemoval(store, entity);
        commands.flush();
        assertThat(store.get(entity)).isNull();

        commands.deferComponentRemoval(store, entity);
        commands.deferComponentAdd(store, entity, second);
        commands.flush();
        assertThat(store.get(entity)).isSameAs(second);

    }

    @Test
    void emptyFlushIsHarmless() {

        EntityIdAllocator allocator = new EntityIdAllocator();
        DeferredStructuralCommandBuffer commands = new DeferredStructuralCommandBuffer(allocator);

        commands.flush();
        commands.flush();

        EntityId first = allocator.create();
        assertThat(first.index()).isZero();
        assertThat(allocator.isAlive(first)).isTrue();

    }

    @Test
    void failedCommandIsConsumedWhileUntouchedTailRemainsQueued() {

        EntityIdAllocator allocator = new EntityIdAllocator();
        PackedComponentStore<TestComponent> store = new PackedComponentStore<>(allocator);
        DeferredStructuralCommandBuffer commands = new DeferredStructuralCommandBuffer(allocator);
        EntityId iterated = allocator.create();
        EntityId appliedBeforeFailure = allocator.create();
        EntityId deferredTail = allocator.create();
        TestComponent original = new TestComponent("original");

        assertThat(store.add(iterated, original)).isTrue();
        commands.deferEntityDestruction(appliedBeforeFailure);
        commands.deferComponentAdd(store, iterated, new TestComponent("cannot-add-during-iteration"));
        commands.deferEntityDestruction(deferredTail);

        assertThatThrownBy(() -> store.forEach((id, component) -> commands.flush())).isInstanceOf(IllegalStateException.class).hasMessageContaining("during iteration");

        assertThat(allocator.isAlive(appliedBeforeFailure)).isFalse();
        assertThat(allocator.isAlive(deferredTail)).isTrue();
        assertThat(store.get(iterated)).isSameAs(original);

        commands.flush();

        assertThat(allocator.isAlive(deferredTail)).isFalse();
        assertThat(store.get(iterated)).isSameAs(original);

    }

    @Test
    void activeFlushRejectsRecursiveFlushAndFurtherRecording() throws ReflectiveOperationException {

        EntityIdAllocator allocator = new EntityIdAllocator();
        PackedComponentStore<TestComponent> store = new PackedComponentStore<>(allocator);
        DeferredStructuralCommandBuffer commands = new DeferredStructuralCommandBuffer(allocator);
        EntityId entity = allocator.create();
        Field flushingField = DeferredStructuralCommandBuffer.class.getDeclaredField("flushing");
        flushingField.setAccessible(true);
        flushingField.setBoolean(commands, true);

        try {
            assertThatThrownBy(commands::flush).isInstanceOf(IllegalStateException.class).hasMessageContaining("already flushing");
            assertThatThrownBy(commands::deferEntityCreation).isInstanceOf(IllegalStateException.class).hasMessageContaining("during flush");
            assertThatThrownBy(() -> commands.deferEntityDestruction(entity)).isInstanceOf(IllegalStateException.class).hasMessageContaining("during flush");
            assertThatThrownBy(() -> commands.deferComponentAdd(store, entity, new TestComponent("value"))).isInstanceOf(IllegalStateException.class).hasMessageContaining("during flush");
            assertThatThrownBy(() -> commands.deferComponentRemoval(store, entity)).isInstanceOf(IllegalStateException.class).hasMessageContaining("during flush");
        } finally {
            flushingField.setBoolean(commands, false);
        }

    }

    @Test
    void rejectsNullInputsDeterministically() {

        EntityIdAllocator allocator = new EntityIdAllocator();
        PackedComponentStore<TestComponent> store = new PackedComponentStore<>(allocator);
        DeferredStructuralCommandBuffer commands = new DeferredStructuralCommandBuffer(allocator);
        EntityId entity = allocator.create();

        assertThatThrownBy(() -> new DeferredStructuralCommandBuffer(null)).isInstanceOf(NullPointerException.class).hasMessageContaining("allocator");
        assertThatThrownBy(() -> commands.deferEntityDestruction(null)).isInstanceOf(NullPointerException.class).hasMessageContaining("id");
        assertThatThrownBy(() -> commands.deferComponentAdd(null, entity, new TestComponent("value"))).isInstanceOf(NullPointerException.class).hasMessageContaining("store");
        assertThatThrownBy(() -> commands.deferComponentAdd(store, null, new TestComponent("value"))).isInstanceOf(NullPointerException.class).hasMessageContaining("id");
        assertThatThrownBy(() -> commands.deferComponentAdd(store, entity, null)).isInstanceOf(NullPointerException.class).hasMessageContaining("component");
        assertThatThrownBy(() -> commands.deferComponentRemoval(null, entity)).isInstanceOf(NullPointerException.class).hasMessageContaining("store");
        assertThatThrownBy(() -> commands.deferComponentRemoval(store, null)).isInstanceOf(NullPointerException.class).hasMessageContaining("id");

    }

    private record TestComponent(String value) {
    }
}
