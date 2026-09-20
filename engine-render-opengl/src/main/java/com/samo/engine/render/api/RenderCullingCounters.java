package com.samo.engine.render.api;

/** Immutable diagnostic counters for one successfully rendered frame. */
public record RenderCullingCounters(int testedCandidates, int visibleCandidates, int culledCandidates, int submittedDraws) {

    public static final RenderCullingCounters EMPTY = new RenderCullingCounters(0, 0, 0, 0);

    public RenderCullingCounters {
        if (testedCandidates < 0) {
            throw new IllegalArgumentException("testedCandidates must be non-negative");
        }
        if (visibleCandidates < 0) {
            throw new IllegalArgumentException("visibleCandidates must be non-negative");
        }
        if (culledCandidates < 0) {
            throw new IllegalArgumentException("culledCandidates must be non-negative");
        }
        if (submittedDraws < 0) {
            throw new IllegalArgumentException("submittedDraws must be non-negative");
        }
        if (visibleCandidates + culledCandidates != testedCandidates) {
            throw new IllegalArgumentException("visibleCandidates + culledCandidates must equal testedCandidates");
        }
        if (submittedDraws > visibleCandidates) {
            throw new IllegalArgumentException("submittedDraws must not exceed visibleCandidates");
        }
    }
}
