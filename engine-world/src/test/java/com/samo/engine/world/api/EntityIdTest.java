package com.samo.engine.world.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class EntityIdTest {
    @Test
    void preservesIndexAndGenerationAsCompleteValueIdentity() {

        EntityId first = new EntityId(12, 7);
        EntityId same = new EntityId(12, 7);
        EntityId differentGeneration = new EntityId(12, 8);

        assertThat(first.index()).isEqualTo(12);
        assertThat(first.generation()).isEqualTo(7);
        assertThat(first).isEqualTo(same).hasSameHashCodeAs(same);
        assertThat(first).isNotEqualTo(differentGeneration);

    }

    @Test
    void acceptsCompleteNonNegativeIntDomain() {

        EntityId maximum = new EntityId(Integer.MAX_VALUE, Integer.MAX_VALUE);

        assertThat(maximum.index()).isEqualTo(Integer.MAX_VALUE);
        assertThat(maximum.generation()).isEqualTo(Integer.MAX_VALUE);

    }

    @Test
    void rejectsNegativeIndexOrGeneration() {

        assertThatThrownBy(() -> new EntityId(-1, 0)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("index");
        assertThatThrownBy(() -> new EntityId(0, -1)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("generation");

    }
}
