package com.samo.engine.world.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class EntityGuidTest {
    @Test
    void explicitBitsRoundTripCanonicalText() {

        EntityGuid guid = new EntityGuid(0x0011223344556677L, 0x8899aabbccddeeffL);

        assertThat(guid.toString()).isEqualTo("00112233-4455-6677-8899-aabbccddeeff");
        assertThat(EntityGuid.parse("00112233-4455-6677-8899-aabbccddeeff")).isEqualTo(guid);

    }

    @Test
    void generatedGuidRoundTripsThroughCanonicalText() {

        EntityGuid generated = EntityGuid.generate();
        String text = generated.toString();

        assertThat(text).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
        assertThat(EntityGuid.parse(text)).isEqualTo(generated);

    }

    @Test
    void rejectsNonCanonicalText() {

        String[] invalid = {"00112233-4455-6677-8899-AABBCCDDEEFF", "00112233445566778899aabbccddeeff", "00112233-4455-6677-8899-aabbccddee",
            "00112233-4455-6677-8899-aabbccddeeff0", "not-a-guid"};

        for (String text : invalid) {
            assertThatThrownBy(() -> EntityGuid.parse(text)).isInstanceOf(IllegalArgumentException.class);
        }

    }

    @Test
    void rejectsNullText() {

        assertThatThrownBy(() -> EntityGuid.parse(null)).isInstanceOf(NullPointerException.class).hasMessageContaining("text");

    }
}
