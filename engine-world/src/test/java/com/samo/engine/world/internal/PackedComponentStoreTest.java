package com.samo.engine.world.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.samo.engine.world.api.EntityId;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class PackedComponentStoreTest {
    @Test
    void addsGetsAndRemovesLiveComponent() {

        EntityIdAllocator allocator = new EntityIdAllocator();
        PackedComponentStore<TestComponent> store = new PackedComponentStore<>(allocator);
        EntityId entity = allocator.create();
        TestComponent component = new TestComponent("alpha");

        assertThat(store.add(entity, component)).isTrue();
        assertThat(store.get(entity)).isSameAs(component);
        assertThat(store.size()).isEqualTo(1);

        assertThat(store.remove(entity)).isTrue();
        assertThat(store.get(entity)).isNull();
        assertThat(store.size()).isZero();

    }

    @Test
    void duplicateAddFailsWithoutReplacingOriginalValue() {

        EntityIdAllocator allocator = new EntityIdAllocator();
        PackedComponentStore<TestComponent> store = new PackedComponentStore<>(allocator);
        EntityId entity = allocator.create();
        TestComponent original = new TestComponent("original");

        assertThat(store.add(entity, original)).isTrue();
        assertThat(store.add(entity, new TestComponent("replacement"))).isFalse();

        assertThat(store.get(entity)).isSameAs(original);
        assertThat(store.size()).isEqualTo(1);

    }

    @Test
    void swapRemovalRepairsMovedEntitySparseLookup() {

        EntityIdAllocator allocator = new EntityIdAllocator();
        PackedComponentStore<TestComponent> store = new PackedComponentStore<>(allocator);
        EntityId first = allocator.create();
        EntityId middle = allocator.create();
        EntityId last = allocator.create();
        TestComponent firstComponent = new TestComponent("first");
        TestComponent lastComponent = new TestComponent("last");

        assertThat(store.add(first, firstComponent)).isTrue();
        assertThat(store.add(middle, new TestComponent("middle"))).isTrue();
        assertThat(store.add(last, lastComponent)).isTrue();

        assertThat(store.remove(middle)).isTrue();

        assertThat(store.get(first)).isSameAs(firstComponent);
        assertThat(store.get(middle)).isNull();
        assertThat(store.get(last)).isSameAs(lastComponent);
        assertThat(store.remove(last)).isTrue();
        assertThat(store.get(last)).isNull();
        assertThat(store.size()).isEqualTo(1);

    }

    @Test
    void iterationVisitsEachLivePackedEntryExactlyOnce() {

        EntityIdAllocator allocator = new EntityIdAllocator();
        PackedComponentStore<TestComponent> store = new PackedComponentStore<>(allocator);
        EntityId first = allocator.create();
        EntityId removed = allocator.create();
        EntityId third = allocator.create();

        assertThat(store.add(first, new TestComponent("first"))).isTrue();
        assertThat(store.add(removed, new TestComponent("removed"))).isTrue();
        assertThat(store.add(third, new TestComponent("third"))).isTrue();
        assertThat(store.remove(removed)).isTrue();

        List<EntityId> visitedIds = new ArrayList<>();
        List<String> visitedValues = new ArrayList<>();
        store.forEach((id, component) -> {
            visitedIds.add(id);
            visitedValues.add(component.value());
        });

        assertThat(visitedIds).containsExactlyInAnyOrder(first, third);
        assertThat(visitedIds).doesNotHaveDuplicates();
        assertThat(visitedValues).containsExactlyInAnyOrder("first", "third");

    }

    @Test
    void destroyedEntryIsExcludedFromGetSizeAndIteration() {

        EntityIdAllocator allocator = new EntityIdAllocator();
        PackedComponentStore<TestComponent> store = new PackedComponentStore<>(allocator);
        EntityId destroyed = allocator.create();
        EntityId survivor = allocator.create();

        assertThat(store.add(destroyed, new TestComponent("destroyed"))).isTrue();
        assertThat(store.add(survivor, new TestComponent("survivor"))).isTrue();
        assertThat(allocator.destroy(destroyed)).isTrue();

        assertThat(store.get(destroyed)).isNull();
        assertThat(store.size()).isEqualTo(1);

        List<EntityId> visited = new ArrayList<>();
        store.forEach((id, component) -> visited.add(id));
        assertThat(visited).containsExactly(survivor);

    }

    @Test
    void reusedIndexAcceptsReplacementWithoutRevivingStaleIdentity() {

        EntityIdAllocator allocator = new EntityIdAllocator();
        PackedComponentStore<TestComponent> store = new PackedComponentStore<>(allocator);
        EntityId original = allocator.create();

        assertThat(store.add(original, new TestComponent("old"))).isTrue();
        assertThat(allocator.destroy(original)).isTrue();
        EntityId replacement = allocator.create();
        TestComponent replacementComponent = new TestComponent("new");

        assertThat(replacement.index()).isEqualTo(original.index());
        assertThat(replacement.generation()).isEqualTo(original.generation() + 1);
        assertThat(store.add(replacement, replacementComponent)).isTrue();

        assertThat(store.get(original)).isNull();
        assertThat(store.remove(original)).isFalse();
        assertThat(store.get(replacement)).isSameAs(replacementComponent);
        assertThat(store.size()).isEqualTo(1);

    }

    @Test
    void staleUnknownAndGenerationMismatchedIdsCannotMutateLiveComponent() {

        EntityIdAllocator allocator = new EntityIdAllocator();
        PackedComponentStore<TestComponent> store = new PackedComponentStore<>(allocator);
        EntityId live = allocator.create();
        TestComponent liveComponent = new TestComponent("live");
        EntityId wrongGeneration = new EntityId(live.index(), live.generation() + 1);
        EntityId unknown = new EntityId(99, 0);

        assertThat(store.add(live, liveComponent)).isTrue();

        assertThat(store.add(wrongGeneration, new TestComponent("wrong"))).isFalse();
        assertThat(store.get(wrongGeneration)).isNull();
        assertThat(store.remove(wrongGeneration)).isFalse();
        assertThat(store.add(unknown, new TestComponent("unknown"))).isFalse();
        assertThat(store.get(unknown)).isNull();
        assertThat(store.remove(unknown)).isFalse();

        assertThat(store.get(live)).isSameAs(liveComponent);
        assertThat(store.size()).isEqualTo(1);

    }

    @Test
    void repeatedRemovalAndReuseDoNotCreateDuplicatePackedEntries() {

        EntityIdAllocator allocator = new EntityIdAllocator();
        PackedComponentStore<TestComponent> store = new PackedComponentStore<>(allocator);
        EntityId first = allocator.create();
        EntityId second = allocator.create();
        EntityId third = allocator.create();

        assertThat(store.add(first, new TestComponent("first"))).isTrue();
        assertThat(store.add(second, new TestComponent("second"))).isTrue();
        assertThat(store.add(third, new TestComponent("third"))).isTrue();
        assertThat(allocator.destroy(first)).isTrue();
        assertThat(allocator.destroy(third)).isTrue();

        EntityId replacementFirst = allocator.create();
        EntityId replacementThird = allocator.create();
        assertThat(store.add(replacementFirst, new TestComponent("replacement-first"))).isTrue();
        assertThat(store.add(replacementThird, new TestComponent("replacement-third"))).isTrue();

        List<EntityId> visited = new ArrayList<>();
        store.forEach((id, component) -> visited.add(id));

        assertThat(visited).containsExactlyInAnyOrder(second, replacementFirst, replacementThird);
        assertThat(visited).doesNotHaveDuplicates();
        assertThat(store.size()).isEqualTo(3);

    }

    @Test
    void rejectsNullInputsDeterministically() {

        EntityIdAllocator allocator = new EntityIdAllocator();
        PackedComponentStore<TestComponent> store = new PackedComponentStore<>(allocator);
        EntityId entity = allocator.create();

        assertThatThrownBy(() -> new PackedComponentStore<TestComponent>(null)).isInstanceOf(NullPointerException.class).hasMessageContaining("allocator");
        assertThatThrownBy(() -> store.add(null, new TestComponent("value"))).isInstanceOf(NullPointerException.class).hasMessageContaining("id");
        assertThatThrownBy(() -> store.add(entity, null)).isInstanceOf(NullPointerException.class).hasMessageContaining("component");
        assertThatThrownBy(() -> store.get(null)).isInstanceOf(NullPointerException.class).hasMessageContaining("id");
        assertThatThrownBy(() -> store.remove(null)).isInstanceOf(NullPointerException.class).hasMessageContaining("id");
        assertThatThrownBy(() -> store.forEach(null)).isInstanceOf(NullPointerException.class).hasMessageContaining("consumer");

    }

    @Test
    void structuralMutationDuringIterationIsRejectedWithoutCorruption() {

        EntityIdAllocator allocator = new EntityIdAllocator();
        PackedComponentStore<TestComponent> store = new PackedComponentStore<>(allocator);
        EntityId first = allocator.create();
        EntityId second = allocator.create();
        TestComponent firstComponent = new TestComponent("first");
        TestComponent secondComponent = new TestComponent("second");

        assertThat(store.add(first, firstComponent)).isTrue();
        assertThat(store.add(second, secondComponent)).isTrue();

        assertThatThrownBy(() -> store.forEach((id, component) -> store.remove(id))).isInstanceOf(IllegalStateException.class).hasMessageContaining("during iteration");

        assertThat(store.get(first)).isSameAs(firstComponent);
        assertThat(store.get(second)).isSameAs(secondComponent);
        assertThat(store.size()).isEqualTo(2);

    }

    private record TestComponent(String value) {}
}
