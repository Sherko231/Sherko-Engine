package com.samo.engine.world.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.samo.engine.world.api.EntityId;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class WorldUpdatePipelineTest {
    @Test
    void executesEveryPhaseExactlyOnceInTheFixedOrder() {

        EntityIdAllocator allocator = new EntityIdAllocator();
        DeferredStructuralCommandBuffer commands = new DeferredStructuralCommandBuffer(allocator);
        WorldUpdatePipeline pipeline = new WorldUpdatePipeline(commands);
        List<WorldUpdatePhase> visited = new ArrayList<>();

        pipeline.update(visited::add);

        assertThat(visited).containsExactly(WorldUpdatePhase.INPUT, WorldUpdatePhase.PRE_PHYSICS, WorldUpdatePhase.PHYSICS, WorldUpdatePhase.POST_PHYSICS,
            WorldUpdatePhase.GAMEPLAY, WorldUpdatePhase.REPLICATION_CAPTURE, WorldUpdatePhase.PRESENTATION_EXTRACTION);

    }

    @Test
    void structuralCommandsBecomeVisibleOnlyAcrossSuccessfulPhaseBoundaries() {

        EntityIdAllocator allocator = new EntityIdAllocator();
        DeferredStructuralCommandBuffer commands = new DeferredStructuralCommandBuffer(allocator);
        WorldUpdatePipeline pipeline = new WorldUpdatePipeline(commands);
        PackedComponentStore<TestComponent> store = new PackedComponentStore<>(allocator);
        EntityId entity = allocator.create();
        TestComponent component = new TestComponent("phase-visible");

        pipeline.update(phase -> {
            if (phase == WorldUpdatePhase.PRE_PHYSICS) {
                commands.deferComponentAdd(store, entity, component);
                assertThat(store.get(entity)).isNull();
            } else if (phase == WorldUpdatePhase.PHYSICS) {
                assertThat(store.get(entity)).isSameAs(component);
            } else if (phase == WorldUpdatePhase.PRESENTATION_EXTRACTION) {
                commands.deferComponentRemoval(store, entity);
                assertThat(store.get(entity)).isSameAs(component);
            }
        });

        assertThat(store.get(entity)).isNull();

    }

    @Test
    void callbackFailureStopsLaterPhasesAndDoesNotFlushTheFailedPhase() {

        EntityIdAllocator allocator = new EntityIdAllocator();
        DeferredStructuralCommandBuffer commands = new DeferredStructuralCommandBuffer(allocator);
        WorldUpdatePipeline pipeline = new WorldUpdatePipeline(commands);
        EntityId entity = allocator.create();
        List<WorldUpdatePhase> visited = new ArrayList<>();

        assertThatThrownBy(() -> pipeline.update(phase -> {
            visited.add(phase);
            if (phase == WorldUpdatePhase.PRE_PHYSICS) {
                commands.deferEntityDestruction(entity);
                throw new TestFailure("phase failed");
            }
        })).isInstanceOf(TestFailure.class).hasMessageContaining("phase failed");

        assertThat(visited).containsExactly(WorldUpdatePhase.INPUT, WorldUpdatePhase.PRE_PHYSICS);
        assertThat(allocator.isAlive(entity)).isTrue();

        commands.flush();
        assertThat(allocator.isAlive(entity)).isFalse();

    }

    @Test
    void flushFailureStopsLaterPhasesWithoutDiscardingQueuedCommands() throws ReflectiveOperationException {

        EntityIdAllocator allocator = new EntityIdAllocator();
        DeferredStructuralCommandBuffer commands = new DeferredStructuralCommandBuffer(allocator);
        WorldUpdatePipeline pipeline = new WorldUpdatePipeline(commands);
        EntityId entity = allocator.create();
        List<WorldUpdatePhase> visited = new ArrayList<>();
        Field flushingField = DeferredStructuralCommandBuffer.class.getDeclaredField("flushing");
        flushingField.setAccessible(true);

        commands.deferEntityDestruction(entity);
        flushingField.setBoolean(commands, true);
        try {
            assertThatThrownBy(() -> pipeline.update(visited::add)).isInstanceOf(IllegalStateException.class).hasMessageContaining("already flushing");
        } finally {
            flushingField.setBoolean(commands, false);
        }

        assertThat(visited).containsExactly(WorldUpdatePhase.INPUT);
        assertThat(allocator.isAlive(entity)).isTrue();

        commands.flush();
        assertThat(allocator.isAlive(entity)).isFalse();

    }

    @Test
    void recursiveUpdateIsRejectedAndGuardRecoversAfterFailure() {

        EntityIdAllocator allocator = new EntityIdAllocator();
        DeferredStructuralCommandBuffer commands = new DeferredStructuralCommandBuffer(allocator);
        WorldUpdatePipeline pipeline = new WorldUpdatePipeline(commands);

        assertThatThrownBy(() -> pipeline.update(phase -> {
            if (phase == WorldUpdatePhase.INPUT) {
                pipeline.update(ignored -> {
                });
            }
        })).isInstanceOf(IllegalStateException.class).hasMessageContaining("already updating");

        List<WorldUpdatePhase> retryVisited = new ArrayList<>();
        pipeline.update(retryVisited::add);

        assertThat(retryVisited).containsExactly(WorldUpdatePhase.INPUT, WorldUpdatePhase.PRE_PHYSICS, WorldUpdatePhase.PHYSICS, WorldUpdatePhase.POST_PHYSICS,
            WorldUpdatePhase.GAMEPLAY, WorldUpdatePhase.REPLICATION_CAPTURE, WorldUpdatePhase.PRESENTATION_EXTRACTION);

    }

    @Test
    void rejectsNullInputsDeterministically() {

        EntityIdAllocator allocator = new EntityIdAllocator();
        DeferredStructuralCommandBuffer commands = new DeferredStructuralCommandBuffer(allocator);
        WorldUpdatePipeline pipeline = new WorldUpdatePipeline(commands);

        assertThatThrownBy(() -> new WorldUpdatePipeline(null)).isInstanceOf(NullPointerException.class).hasMessageContaining("commands");
        assertThatThrownBy(() -> pipeline.update(null)).isInstanceOf(NullPointerException.class).hasMessageContaining("callback");

    }

    private record TestComponent(String value) {
    }

    private static final class TestFailure extends RuntimeException {
        private TestFailure(String message) {

            super(message);

        }
    }
}
