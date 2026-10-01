package com.samo.engine.world.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.samo.engine.world.api.EntityId;
import java.util.Set;
import org.junit.jupiter.api.Test;

class EntityIdAllocatorTest {
    @Test
    void createsFreshLiveIdsWithGenerationZero() {

        EntityIdAllocator allocator = new EntityIdAllocator();

        EntityId first = allocator.create();
        EntityId second = allocator.create();

        assertThat(first).isEqualTo(new EntityId(0, 0));
        assertThat(second).isEqualTo(new EntityId(1, 0));
        assertThat(allocator.isAlive(first)).isTrue();
        assertThat(allocator.isAlive(second)).isTrue();

    }

    @Test
    void destroyingAndReusingIndexNeverRevivesOldId() {

        EntityIdAllocator allocator = new EntityIdAllocator();
        EntityId original = allocator.create();

        assertThat(allocator.destroy(original)).isTrue();
        assertThat(allocator.isAlive(original)).isFalse();

        EntityId replacement = allocator.create();

        assertThat(replacement.index()).isEqualTo(original.index());
        assertThat(replacement.generation()).isEqualTo(original.generation() + 1);
        assertThat(allocator.isAlive(replacement)).isTrue();
        assertThat(allocator.isAlive(original)).isFalse();

    }

    @Test
    void repeatedDestroyBeforeReuseDoesNotDuplicateFreeIndex() {

        EntityIdAllocator allocator = new EntityIdAllocator();
        EntityId original = allocator.create();

        assertThat(allocator.destroy(original)).isTrue();
        assertThat(allocator.destroy(original)).isFalse();

        EntityId replacement = allocator.create();
        EntityId nextFresh = allocator.create();

        assertThat(replacement.index()).isEqualTo(original.index());
        assertThat(replacement.generation()).isEqualTo(original.generation() + 1);
        assertThat(nextFresh).isEqualTo(new EntityId(1, 0));
        assertThat(allocator.isAlive(replacement)).isTrue();
        assertThat(allocator.isAlive(nextFresh)).isTrue();

    }

    @Test
    void staleDestroyCannotAffectCurrentOccupant() {

        EntityIdAllocator allocator = new EntityIdAllocator();
        EntityId original = allocator.create();
        assertThat(allocator.destroy(original)).isTrue();
        EntityId replacement = allocator.create();

        assertThat(allocator.destroy(original)).isFalse();
        assertThat(allocator.isAlive(replacement)).isTrue();
        assertThat(allocator.destroy(replacement)).isTrue();

    }

    @Test
    void generationMismatchCannotDestroyLiveSlot() {

        EntityIdAllocator allocator = new EntityIdAllocator();
        EntityId current = allocator.create();
        EntityId wrongGeneration = new EntityId(current.index(), current.generation() + 1);

        assertThat(allocator.isAlive(wrongGeneration)).isFalse();
        assertThat(allocator.destroy(wrongGeneration)).isFalse();
        assertThat(allocator.isAlive(current)).isTrue();

    }

    @Test
    void unknownIndexCannotMutateAllocatorState() {

        EntityIdAllocator allocator = new EntityIdAllocator();
        EntityId current = allocator.create();
        EntityId unknown = new EntityId(99, 0);

        assertThat(allocator.isAlive(unknown)).isFalse();
        assertThat(allocator.destroy(unknown)).isFalse();
        assertThat(allocator.isAlive(current)).isTrue();
        assertThat(allocator.create()).isEqualTo(new EntityId(1, 0));

    }

    @Test
    void multipleFreeIndicesReuseWithoutDuplicateLiveIds() {

        EntityIdAllocator allocator = new EntityIdAllocator();
        EntityId first = allocator.create();
        EntityId second = allocator.create();
        EntityId third = allocator.create();

        assertThat(allocator.destroy(first)).isTrue();
        assertThat(allocator.destroy(third)).isTrue();

        EntityId reusedFirst = allocator.create();
        EntityId reusedThird = allocator.create();

        assertThat(Set.of(second, reusedFirst, reusedThird)).hasSize(3);
        assertThat(Set.of(reusedFirst.index(), reusedThird.index())).containsExactlyInAnyOrder(first.index(), third.index());
        assertThat(allocator.isAlive(second)).isTrue();
        assertThat(allocator.isAlive(reusedFirst)).isTrue();
        assertThat(allocator.isAlive(reusedThird)).isTrue();

    }

    @Test
    void maximumGenerationRetiresSlotInsteadOfWrapping() {

        EntitySlot slot = new EntitySlot(Integer.MAX_VALUE - 1);
        EntityId penultimate = slot.activate(4);

        assertThat(slot.destroy(penultimate)).isTrue();
        assertThat(slot.generation()).isEqualTo(Integer.MAX_VALUE);
        assertThat(slot.reusable()).isTrue();

        EntityId maximum = slot.activate(4);
        assertThat(maximum.generation()).isEqualTo(Integer.MAX_VALUE);

        assertThat(slot.destroy(maximum)).isTrue();
        assertThat(slot.generation()).isEqualTo(Integer.MAX_VALUE);
        assertThat(slot.retired()).isTrue();
        assertThat(slot.reusable()).isFalse();
        assertThatThrownBy(() -> slot.activate(4)).isInstanceOf(IllegalStateException.class);

    }

    @Test
    void freshIndexCapacityFailsBeforeSignedWraparound() {

        EntityIdAllocator.requireFreshIndexCapacity(Integer.MAX_VALUE - 1);

        assertThatThrownBy(() -> EntityIdAllocator.requireFreshIndexCapacity(Integer.MAX_VALUE)).isInstanceOf(IllegalStateException.class).hasMessageContaining("exhausted");
        assertThatThrownBy(() -> EntityIdAllocator.requireFreshIndexCapacity(-1)).isInstanceOf(IllegalArgumentException.class);

    }

    @Test
    void rejectsNullIdentityQueriesAndDestroy() {

        EntityIdAllocator allocator = new EntityIdAllocator();

        assertThatThrownBy(() -> allocator.isAlive(null)).isInstanceOf(NullPointerException.class).hasMessageContaining("id");
        assertThatThrownBy(() -> allocator.destroy(null)).isInstanceOf(NullPointerException.class).hasMessageContaining("id");

    }
}
