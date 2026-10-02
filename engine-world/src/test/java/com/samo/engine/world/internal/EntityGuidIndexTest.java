package com.samo.engine.world.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.samo.engine.world.api.EntityGuid;
import com.samo.engine.world.api.EntityId;
import org.junit.jupiter.api.Test;

class EntityGuidIndexTest {
    @Test
    void bindsLiveEntityAndResolvesBothDirections() {

        EntityIdAllocator allocator = new EntityIdAllocator();
        EntityGuidIndex index = new EntityGuidIndex(allocator);
        EntityGuid guid = guid(1L);
        EntityId entityId = allocator.create();

        index.bind(guid, entityId);

        assertThat(index.resolve(guid)).isEqualTo(entityId);
        assertThat(index.guidFor(entityId)).isEqualTo(guid);
        assertThat(index.unbind(entityId)).isTrue();
        assertThat(index.resolve(guid)).isNull();
        assertThat(index.guidFor(entityId)).isNull();
        assertThat(index.unbind(entityId)).isFalse();

    }

    @Test
    void rejectsUnknownAndStaleRuntimeIdsWithoutChangingExistingBindings() {

        EntityIdAllocator allocator = new EntityIdAllocator();
        EntityGuidIndex index = new EntityGuidIndex(allocator);
        EntityGuid existingGuid = guid(1L);
        EntityId existingEntity = allocator.create();
        index.bind(existingGuid, existingEntity);

        EntityId staleEntity = allocator.create();
        assertThat(allocator.destroy(staleEntity)).isTrue();
        EntityId unknownEntity = new EntityId(99, 0);

        assertThatThrownBy(() -> index.bind(guid(2L), staleEntity)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("live");
        assertThatThrownBy(() -> index.bind(guid(3L), unknownEntity)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("live");
        assertThat(index.resolve(existingGuid)).isEqualTo(existingEntity);
        assertThat(index.guidFor(existingEntity)).isEqualTo(existingGuid);

    }

    @Test
    void duplicateGuidFailsAndPreservesOriginalBinding() {

        EntityIdAllocator allocator = new EntityIdAllocator();
        EntityGuidIndex index = new EntityGuidIndex(allocator);
        EntityGuid guid = guid(1L);
        EntityId first = allocator.create();
        EntityId second = allocator.create();
        index.bind(guid, first);

        assertThatThrownBy(() -> index.bind(guid, second)).isInstanceOf(IllegalStateException.class).hasMessageContaining("guid");
        assertThat(index.resolve(guid)).isEqualTo(first);
        assertThat(index.guidFor(first)).isEqualTo(guid);
        assertThat(index.guidFor(second)).isNull();

    }

    @Test
    void duplicateEntityFailsAndPreservesOriginalBinding() {

        EntityIdAllocator allocator = new EntityIdAllocator();
        EntityGuidIndex index = new EntityGuidIndex(allocator);
        EntityGuid firstGuid = guid(1L);
        EntityGuid secondGuid = guid(2L);
        EntityId entityId = allocator.create();
        index.bind(firstGuid, entityId);

        assertThatThrownBy(() -> index.bind(secondGuid, entityId)).isInstanceOf(IllegalStateException.class).hasMessageContaining("entityId");
        assertThat(index.resolve(firstGuid)).isEqualTo(entityId);
        assertThat(index.resolve(secondGuid)).isNull();
        assertThat(index.guidFor(entityId)).isEqualTo(firstGuid);

    }

    @Test
    void destructionInvalidatesResolutionAndIndexReuseDoesNotInheritGuid() {

        EntityIdAllocator allocator = new EntityIdAllocator();
        EntityGuidIndex index = new EntityGuidIndex(allocator);
        EntityGuid guid = guid(1L);
        EntityId original = allocator.create();
        index.bind(guid, original);

        assertThat(allocator.destroy(original)).isTrue();
        EntityId replacement = allocator.create();

        assertThat(replacement.index()).isEqualTo(original.index());
        assertThat(replacement.generation()).isNotEqualTo(original.generation());
        assertThat(index.resolve(guid)).isNull();
        assertThat(index.guidFor(original)).isNull();
        assertThat(index.guidFor(replacement)).isNull();

    }

    @Test
    void staleUnbindCannotRemoveReplacementGenerationBinding() {

        EntityIdAllocator allocator = new EntityIdAllocator();
        EntityGuidIndex index = new EntityGuidIndex(allocator);
        EntityGuid originalGuid = guid(1L);
        EntityGuid replacementGuid = guid(2L);
        EntityId original = allocator.create();
        index.bind(originalGuid, original);

        assertThat(allocator.destroy(original)).isTrue();
        EntityId replacement = allocator.create();
        index.bind(replacementGuid, replacement);

        assertThat(index.unbind(original)).isTrue();
        assertThat(index.resolve(originalGuid)).isNull();
        assertThat(index.resolve(replacementGuid)).isEqualTo(replacement);
        assertThat(index.guidFor(replacement)).isEqualTo(replacementGuid);

    }

    @Test
    void savedCrossEntityReferenceResolvesAfterRuntimeIdsChange() {

        EntityIdAllocator allocator = new EntityIdAllocator();
        EntityGuidIndex index = new EntityGuidIndex(allocator);
        EntityGuid aGuid = new EntityGuid(0x0011223344556677L, 0x1122334455667788L);
        EntityGuid bGuid = new EntityGuid(0x2233445566778899L, 0x33445566778899aaL);

        EntityId firstA = allocator.create();
        EntityId firstB = allocator.create();
        index.bind(aGuid, firstA);
        index.bind(bGuid, firstB);
        assertThat(index.resolve(aGuid)).isEqualTo(firstA);

        SavedEntity savedB = new SavedEntity(bGuid.toString(), aGuid.toString());

        assertThat(allocator.destroy(firstA)).isTrue();
        assertThat(allocator.destroy(firstB)).isTrue();

        EntityId reloadedB = allocator.create();
        EntityId reloadedA = allocator.create();
        assertThat(reloadedA).isNotEqualTo(firstA);
        assertThat(reloadedB).isNotEqualTo(firstB);

        EntityGuid parsedBGuid = EntityGuid.parse(savedB.entityGuid());
        EntityGuid parsedTargetGuid = EntityGuid.parse(savedB.targetGuid());
        index.bind(parsedBGuid, reloadedB);
        index.bind(parsedTargetGuid, reloadedA);

        assertThat(index.resolve(parsedTargetGuid)).isEqualTo(reloadedA);
        assertThat(index.resolve(parsedTargetGuid)).isNotEqualTo(firstA);
        assertThat(index.guidFor(reloadedA)).isEqualTo(aGuid);

    }

    @Test
    void rejectsNullArguments() {

        EntityIdAllocator allocator = new EntityIdAllocator();
        EntityGuidIndex index = new EntityGuidIndex(allocator);
        EntityGuid guid = guid(1L);
        EntityId entityId = allocator.create();

        assertThatThrownBy(() -> new EntityGuidIndex(null)).isInstanceOf(NullPointerException.class).hasMessageContaining("allocator");
        assertThatThrownBy(() -> index.bind(null, entityId)).isInstanceOf(NullPointerException.class).hasMessageContaining("guid");
        assertThatThrownBy(() -> index.bind(guid, null)).isInstanceOf(NullPointerException.class).hasMessageContaining("entityId");
        assertThatThrownBy(() -> index.resolve(null)).isInstanceOf(NullPointerException.class).hasMessageContaining("guid");
        assertThatThrownBy(() -> index.guidFor(null)).isInstanceOf(NullPointerException.class).hasMessageContaining("entityId");
        assertThatThrownBy(() -> index.unbind(null)).isInstanceOf(NullPointerException.class).hasMessageContaining("entityId");

    }

    private static EntityGuid guid(long value) {

        return new EntityGuid(value, ~value);

    }

    private record SavedEntity(String entityGuid, String targetGuid) {
    }
}
