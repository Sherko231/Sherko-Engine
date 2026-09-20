package com.samo.engine.core.api;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * An immutable, non-owning snapshot of named subsystem dependencies.
 *
 * <p>
 * Resolve the entire graph before invoking any subsystem lifecycle method.
 * This class never invokes subsystem hooks, inspects lifecycle state, acquires
 * resources, or performs cleanup. The caller retains all lifecycle ownership.
 * Structural immutability does not make the referenced subsystems immutable.
 */
public final class SubsystemGraph {
    private final Map<String, Registration> registrations = new LinkedHashMap<>();

    /**
     * Copies and validates the complete registration list, allowing forward references.
     *
     * @param registrations
     *            ordered subsystem declarations
     * @throws NullPointerException
     *             for a null list or entry
     * @throws IllegalArgumentException
     *             for duplicate IDs, repeated subsystem
     *             instances, or missing dependencies
     */
    public SubsystemGraph(List<Registration> registrations) {

        Map<EngineSubsystem, String> identities = new IdentityHashMap<>();
        for (Registration registration : List.copyOf(registrations)) {
            String id = registration.id();
            if (this.registrations.putIfAbsent(id, registration) != null) {
                throw new IllegalArgumentException("Duplicate subsystem ID: " + id);
            }
            String previous = identities.put(registration.subsystem(), id);
            if (previous != null) {
                throw new IllegalArgumentException("Subsystem instance registered as both " + previous + " and " + id);
            }
        }
        for (Registration registration : this.registrations.values()) {
            for (String dependency : registration.dependencies()) {
                if (!this.registrations.containsKey(dependency)) {
                    throw new IllegalArgumentException("Subsystem " + registration.id() + " depends on missing subsystem " + dependency);
                }
            }
        }

    }

    /**
     * Returns each subsystem once, after its dependencies, without invoking hooks.
     *
     * <p>
     * Order is depth-first postorder: roots follow registration order and
     * dependencies follow their declared list order. Completed nodes are skipped.
     * All traversal state is local; repeated calls have the same result. An
     * explicit traversal stack avoids dependence on Java call-stack depth.
     *
     * @return an unmodifiable list of the original subsystem references
     * @throws IllegalStateException
     *             if a cycle exists anywhere in the graph;
     *             the message contains a closed dependent-to-dependency path for the
     *             caller to print or log, and no partial order is returned
     */
    public List<EngineSubsystem> initializationOrder() {

        Map<String, Visit> visits = new HashMap<>();
        List<EngineSubsystem> ordered = new ArrayList<>();
        List<String> path = new ArrayList<>();
        ArrayDeque<Frame> stack = new ArrayDeque<>();
        for (Registration root : registrations.values()) {
            if (visits.containsKey(root.id())) {
                continue;
            }
            enter(root, visits, path, stack);
            while (!stack.isEmpty()) {
                Frame frame = stack.peek();
                if (!frame.dependencies().hasNext()) {
                    stack.pop();
                    path.removeLast();
                    visits.put(frame.registration().id(), Visit.COMPLETE);
                    ordered.add(frame.registration().subsystem());
                    continue;
                }
                String dependency = frame.dependencies().next();
                Visit visit = visits.get(dependency);
                if (visit == Visit.ACTIVE) {
                    List<String> cycle = new ArrayList<>(path.subList(path.indexOf(dependency), path.size()));
                    cycle.add(dependency);
                    throw new IllegalStateException("Subsystem dependency cycle: " + String.join(" -> ", cycle));
                }
                if (visit == null) {
                    enter(registrations.get(dependency), visits, path, stack);
                }
            }
        }
        return List.copyOf(ordered);

    }

    private static void enter(Registration registration, Map<String, Visit> visits, List<String> path, ArrayDeque<Frame> stack) {

        visits.put(registration.id(), Visit.ACTIVE);
        path.add(registration.id());
        stack.push(new Frame(registration, registration.dependencies().iterator()));

    }

    /**
     * A named, non-owning subsystem reference and its ordered prerequisite IDs.
     * IDs are exact, case-sensitive, nonblank strings; they are not normalized.
     *
     * @param id
     *            subsystem ID
     * @param subsystem
     *            original instance, whose ownership stays with the caller
     * @param dependencies
     *            prerequisite IDs, defensively copied and unmodifiable
     */
    public record Registration(String id, EngineSubsystem subsystem, List<String> dependencies) {
        /**
         * Validates a declaration without invoking its subsystem.
         *
         * @throws NullPointerException
         *             for null arguments or dependency elements
         * @throws IllegalArgumentException
         *             for blank IDs or repeated dependency IDs
         */
        public Registration {

            requireId(id);
            Objects.requireNonNull(subsystem, "subsystem");
            dependencies = List.copyOf(dependencies);
            HashSet<String> unique = new HashSet<>();
            for (String dependency : dependencies) {
                requireId(dependency);
                if (!unique.add(dependency)) {
                    throw new IllegalArgumentException("Subsystem " + id + " repeats dependency " + dependency);
                }
            }

        }

        private static void requireId(String id) {

            if (Objects.requireNonNull(id, "subsystem ID").isBlank()) {
                throw new IllegalArgumentException("Subsystem ID must not be blank");
            }

        }
    }

    private enum Visit {
        ACTIVE, COMPLETE
    }

    private record Frame(Registration registration, Iterator<String> dependencies) {
    }
}
