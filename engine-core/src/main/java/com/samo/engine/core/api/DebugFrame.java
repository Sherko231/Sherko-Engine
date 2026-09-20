package com.samo.engine.core.api;

import java.util.List;
import java.util.Objects;

/** Immutable bounded per-frame debug submission snapshot. */
public final class DebugFrame {
    public static final int MAX_PRIMITIVES = 64;
    public static final int MAX_TEXT_COUNTERS = 16;
    public static final DebugFrame EMPTY = new DebugFrame(List.of(), List.of());

    private final List<DebugPrimitive> primitives;
    private final List<DebugTextCounter> textCounters;

    public DebugFrame(List<? extends DebugPrimitive> primitives, List<DebugTextCounter> textCounters) {

        List<? extends DebugPrimitive> primitiveValues = Objects.requireNonNull(primitives, "primitives");
        List<DebugTextCounter> counterValues = Objects.requireNonNull(textCounters, "textCounters");
        if (primitiveValues.size() > MAX_PRIMITIVES) {
            throw new IllegalArgumentException("primitives exceeds maximum " + MAX_PRIMITIVES);
        }
        if (counterValues.size() > MAX_TEXT_COUNTERS) {
            throw new IllegalArgumentException("textCounters exceeds maximum " + MAX_TEXT_COUNTERS);
        }
        for (DebugPrimitive primitive : primitiveValues) {
            Objects.requireNonNull(primitive, "primitives must not contain null");
        }
        for (DebugTextCounter counter : counterValues) {
            Objects.requireNonNull(counter, "textCounters must not contain null");
        }
        this.primitives = List.copyOf(primitiveValues);
        this.textCounters = List.copyOf(counterValues);

    }

    public List<DebugPrimitive> primitives() {

        return primitives;

    }

    public List<DebugTextCounter> textCounters() {

        return textCounters;

    }
}
