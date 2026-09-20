package com.samo.engine.core.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SubsystemGraphTest {
    @Test
    void emptyAndSingletonGraphsResolveWithoutLifecycleCalls() {

        assertEquals(List.of(), new SubsystemGraph(List.of()).initializationOrder());
        Probe only = new Probe("only", new ArrayList<>());
        assertEquals(List.of(only), graph(registration("only", only)).initializationOrder());
        assertTrue(only.calls.isEmpty());

    }

    @Test
    void ownerUsesForwardReferenceOrderWithRealLifecycleGuards() {

        List<String> calls = new ArrayList<>();
        Probe gameplay = new Probe("gameplay", calls);
        Probe renderer = new Probe("renderer", calls);
        Probe assets = new Probe("assets", calls);
        SubsystemGraph graph = graph(registration("gameplay", gameplay, "renderer"), registration("renderer", renderer, "assets"), registration("assets", assets));

        List<EngineSubsystem> ordered = graph.initializationOrder();
        assertEquals(List.of(assets, renderer, gameplay), ordered);
        assertTrue(calls.isEmpty(), "Resolution must not perform any lifecycle work");
        try {
            ordered.forEach(EngineSubsystem::initialize);
            ordered.forEach(EngineSubsystem::start);
            ordered.reversed().forEach(EngineSubsystem::stop);
        } finally {
            // The test caller owns this successful composition; no rollback manager is introduced.
            ordered.reversed().forEach(EngineSubsystem::close);
        }
        assertEquals(List.of("assets.initialize", "renderer.initialize", "gameplay.initialize", "assets.start", "renderer.start", "gameplay.start", "gameplay.stop",
            "renderer.stop", "assets.stop", "gameplay.close", "renderer.close", "assets.close"), calls);
        assertEquals(ordered, graph.initializationOrder(), "Resolution does not inspect lifecycle state");
        assertEquals(12, calls.size());

    }

    @Test
    void diamondAndDisconnectedRootsFollowDeclaredDepthFirstOrder() {

        List<String> calls = new ArrayList<>();
        Probe app = new Probe("app", calls);
        Probe left = new Probe("left", calls);
        Probe right = new Probe("right", calls);
        Probe shared = new Probe("shared", calls);
        Probe isolated = new Probe("isolated", calls);
        SubsystemGraph graph = graph(registration("app", app, "right", "left"), registration("isolated", isolated), registration("left", left, "shared"),
            registration("right", right, "shared"), registration("shared", shared));

        assertEquals(List.of(shared, right, left, app, isolated), graph.initializationOrder());
        assertEquals(List.of(shared, right, left, app, isolated), graph.initializationOrder());
        assertTrue(calls.isEmpty());

    }

    @Test
    void independentRootsKeepRegistrationOrderRatherThanLexicographicOrder() {

        Probe z = new Probe("z", new ArrayList<>());
        Probe a = new Probe("a", new ArrayList<>());
        assertEquals(List.of(z, a), graph(registration("z", z), registration("a", a)).initializationOrder());

    }

    @Test
    void cycleInLaterComponentFailsBeforeOwnerInitializesEvenValidRoots() {

        List<String> calls = new ArrayList<>();
        SubsystemGraph graph = graph(registration("valid", new Probe("valid", calls)), registration("tail", new Probe("tail", calls), "A"),
            registration("A", new Probe("A", calls), "B"), registration("B", new Probe("B", calls), "C"), registration("C", new Probe("C", calls), "A"));

        for (int attempt = 0; attempt < 2; attempt++) {
            IllegalStateException failure = assertThrows(IllegalStateException.class, () -> graph.initializationOrder().forEach(EngineSubsystem::initialize));
            StringWriter diagnostic = new StringWriter();
            new PrintWriter(diagnostic).println(failure.getMessage());
            assertEquals("Subsystem dependency cycle: A -> B -> C -> A" + System.lineSeparator(), diagnostic.toString());
            System.err.print(diagnostic);
            assertTrue(calls.isEmpty(), "No hooks may run, including for the earlier valid component");
        }

    }

    @Test
    void selfDependencyReportsAClosedCycleWithoutHooks() {

        Probe self = new Probe("self", new ArrayList<>());
        SubsystemGraph graph = graph(registration("self", self, "self"));
        assertEquals("Subsystem dependency cycle: self -> self", assertThrows(IllegalStateException.class, graph::initializationOrder).getMessage());
        assertTrue(self.calls.isEmpty());

    }

    @Test
    void rejectsMissingDependencyBeforeLifecycleWork() {

        Probe caller = new Probe("caller", new ArrayList<>());
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class, () -> graph(registration("caller", caller, "missing")));
        assertEquals("Subsystem caller depends on missing subsystem missing", failure.getMessage());
        assertTrue(caller.calls.isEmpty());

    }

    @Test
    void rejectsDuplicateIdsAndDuplicateInstancesWithoutHooks() {

        List<String> calls = new ArrayList<>();
        Probe first = new Probe("first", calls);
        Probe second = new Probe("second", calls);
        assertEquals("Duplicate subsystem ID: same",
            assertThrows(IllegalArgumentException.class, () -> graph(registration("same", first), registration("same", second))).getMessage());
        assertEquals("Subsystem instance registered as both first and alias",
            assertThrows(IllegalArgumentException.class, () -> graph(registration("first", first), registration("alias", first))).getMessage());
        assertTrue(calls.isEmpty());

    }

    @Test
    void distinctInstancesThatCompareEqualAreNotDeduplicated() {

        List<String> calls = new ArrayList<>();
        EqualProbe first = new EqualProbe("first", calls);
        EqualProbe second = new EqualProbe("second", calls);
        List<EngineSubsystem> ordered = graph(registration("first", first, "second"), registration("second", second)).initializationOrder();
        assertEquals(2, ordered.size());
        assertSame(second, ordered.get(0));
        assertSame(first, ordered.get(1));
        assertTrue(calls.isEmpty());

    }

    @Test
    void snapshotsBothListsAndReturnsUnmodifiableViews() {

        Probe dependent = new Probe("dependent", new ArrayList<>());
        Probe dependency = new Probe("dependency", new ArrayList<>());
        List<String> dependencies = new ArrayList<>(List.of("dependency"));
        SubsystemGraph.Registration declaration = new SubsystemGraph.Registration("dependent", dependent, dependencies);
        List<SubsystemGraph.Registration> declarations = new ArrayList<>(List.of(declaration, registration("dependency", dependency)));
        SubsystemGraph graph = new SubsystemGraph(declarations);
        dependencies.clear();
        dependencies.add("missing");
        declarations.clear();

        assertEquals(List.of("dependency"), declaration.dependencies());
        assertThrows(UnsupportedOperationException.class, () -> declaration.dependencies().add("other"));
        List<EngineSubsystem> order = graph.initializationOrder();
        assertEquals(List.of(dependency, dependent), order);
        assertThrows(UnsupportedOperationException.class, order::clear);
        assertEquals(List.of(dependency, dependent), graph.initializationOrder());

    }

    @Test
    void idsRemainCaseSensitiveAndAreNotTrimmed() {

        Probe upper = new Probe("A", new ArrayList<>());
        Probe lower = new Probe("a", new ArrayList<>());
        Probe spaced = new Probe(" A ", new ArrayList<>());
        assertEquals(List.of(spaced, lower, upper), graph(registration("A", upper, "a"), registration("a", lower, " A "), registration(" A ", spaced)).initializationOrder());
        assertThrows(IllegalArgumentException.class, () -> graph(registration("A", upper, "a")));
        assertThrows(IllegalArgumentException.class, () -> graph(registration("A", upper, " A ")));

    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\t", "\n", "\u2003"})
    void rejectsBlankRegistrationAndDependencyIds(String blank) {

        Probe probe = new Probe("probe", new ArrayList<>());
        assertThrows(IllegalArgumentException.class, () -> registration(blank, probe));
        assertThrows(IllegalArgumentException.class, () -> registration("probe", probe, blank));
        assertTrue(probe.calls.isEmpty());

    }

    @Test
    void rejectsRepeatedDependencyIdsAtDeclarationTime() {

        Probe probe = new Probe("probe", new ArrayList<>());
        assertEquals("Subsystem probe repeats dependency base", assertThrows(IllegalArgumentException.class, () -> registration("probe", probe, "base", "base")).getMessage());
        assertTrue(probe.calls.isEmpty());

    }

    @Test
    void rejectsAllNullInputs() {

        Probe probe = new Probe("probe", new ArrayList<>());
        assertThrows(NullPointerException.class, () -> new SubsystemGraph(null));
        assertThrows(NullPointerException.class, () -> new SubsystemGraph(Arrays.asList((SubsystemGraph.Registration) null)));
        assertThrows(NullPointerException.class, () -> registration(null, probe));
        assertThrows(NullPointerException.class, () -> registration("probe", null));
        assertThrows(NullPointerException.class, () -> new SubsystemGraph.Registration("probe", probe, null));
        assertThrows(NullPointerException.class, () -> registration("probe", probe, (String) null));
        assertTrue(probe.calls.isEmpty());

    }

    @Test
    void longForwardChainDoesNotUseTheJavaCallStack() {

        int count = 10_000;
        List<Probe> probes = new ArrayList<>();
        List<SubsystemGraph.Registration> registrations = new ArrayList<>();
        List<String> calls = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            Probe probe = new Probe("node" + index, calls);
            probes.add(probe);
            registrations.add(new SubsystemGraph.Registration("node" + index, probe, index + 1 < count ? List.of("node" + (index + 1)) : List.of()));
        }
        List<EngineSubsystem> order = new SubsystemGraph(registrations).initializationOrder();
        assertEquals(count, order.size());
        for (int index = 0; index < count; index++) {
            assertSame(probes.get(count - index - 1), order.get(index));
        }
        assertTrue(calls.isEmpty());

    }

    private static SubsystemGraph graph(SubsystemGraph.Registration... registrations) {

        return new SubsystemGraph(List.of(registrations));

    }

    private static SubsystemGraph.Registration registration(String id, EngineSubsystem subsystem, String... dependencies) {

        return new SubsystemGraph.Registration(id, subsystem, Arrays.asList(dependencies));

    }

    private static class Probe extends EngineSubsystem {
        private final String name;
        private final List<String> calls;

        private Probe(String name, List<String> calls) {

            this.name = name;
            this.calls = calls;

        }

        @Override
        protected void onInitialize() {

            calls.add(name + ".initialize");

        }

        @Override
        protected void onStart() {

            calls.add(name + ".start");

        }

        @Override
        protected void onStop() {

            calls.add(name + ".stop");

        }

        @Override
        protected void onClose() {

            calls.add(name + ".close");

        }
    }

    private static final class EqualProbe extends Probe {
        private EqualProbe(String name, List<String> calls) {

            super(name, calls);

        }

        @Override
        public boolean equals(Object other) {

            return other instanceof EqualProbe;

        }

        @Override
        public int hashCode() {

            return 1;

        }
    }
}
