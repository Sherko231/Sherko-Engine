package com.samo.engine.world.api;

/**
 * Immutable world-entity identity composed of a reusable index and its generation.
 *
 * <p>
 * Both components are non-negative. An index may be reused only with a different generation, so
 * callers must treat the complete pair as the identity value.
 */
public record EntityId(int index, int generation) {
    public EntityId {

        if (index < 0) {
            throw new IllegalArgumentException("index must be non-negative");
        }
        if (generation < 0) {
            throw new IllegalArgumentException("generation must be non-negative");
        }

    }
}
