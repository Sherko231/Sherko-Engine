package com.samo.engine.render.opengl.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

final class DrawSubmissionSorter {
    List<DrawSubmission> sort(List<DrawSubmission> submissions) {
        List<DrawSubmission> source = List.copyOf(Objects.requireNonNull(submissions, "submissions"));
        ArrayList<DrawSubmission> ordered = new ArrayList<>(source);
        ordered.sort(this::compare);
        return List.copyOf(ordered);
    }

    private int compare(DrawSubmission left, DrawSubmission right) {
        boolean leftTransparent = left.transparent();
        boolean rightTransparent = right.transparent();
        if (leftTransparent != rightTransparent) {
            return leftTransparent ? 1 : -1;
        }

        if (leftTransparent) {
            int depth = Float.compare(right.cameraDepth(), left.cameraDepth());
            if (depth != 0) {
                return depth;
            }
        }

        int program = Integer.compare(left.programKey(), right.programKey());
        if (program != 0) {
            return program;
        }
        int material = Integer.compare(left.materialKey(), right.materialKey());
        if (material != 0) {
            return material;
        }
        int mesh = Integer.compare(left.meshKey(), right.meshKey());
        if (mesh != 0) {
            return mesh;
        }
        return Integer.compare(left.sequence(), right.sequence());
    }
}
