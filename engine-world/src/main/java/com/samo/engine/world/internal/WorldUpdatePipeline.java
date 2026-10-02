package com.samo.engine.world.internal;

import java.util.List;
import java.util.Objects;

final class WorldUpdatePipeline {
    private static final List<WorldUpdatePhase> PHASE_ORDER = List.of(
        WorldUpdatePhase.INPUT,
        WorldUpdatePhase.PRE_PHYSICS,
        WorldUpdatePhase.PHYSICS,
        WorldUpdatePhase.POST_PHYSICS,
        WorldUpdatePhase.GAMEPLAY,
        WorldUpdatePhase.REPLICATION_CAPTURE,
        WorldUpdatePhase.PRESENTATION_EXTRACTION);

    private final DeferredStructuralCommandBuffer commands;
    private boolean updating;

    WorldUpdatePipeline(DeferredStructuralCommandBuffer commands) {

        this.commands = Objects.requireNonNull(commands, "commands");

    }

    void update(PhaseCallback callback) {

        Objects.requireNonNull(callback, "callback");
        if (updating) {
            throw new IllegalStateException("world update pipeline is already updating");
        }

        updating = true;
        try {
            for (WorldUpdatePhase phase : PHASE_ORDER) {
                callback.execute(phase);
                commands.flush();
            }
        } finally {
            updating = false;
        }

    }

    @FunctionalInterface
    interface PhaseCallback {
        void execute(WorldUpdatePhase phase);
    }
}
