package com.samo.engine.render.opengl.internal;

import com.samo.engine.core.api.Aabb3f;
import com.samo.engine.core.api.Frustum3f;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.joml.Matrix4fc;
import org.joml.Vector3f;

final class ReferenceSceneVisibilityPlanner {
    private static final int PROGRAM_KEY_REFERENCE = 0;
    private static final int MATERIAL_KEY_BASELINE = 0;
    private static final int MESH_KEY_REFERENCE = 0;

    private final RendererMaterial baselineMaterial;
    private final CpuFrustumCuller frustumCuller;
    private final DrawSubmissionSorter submissionSorter;

    ReferenceSceneVisibilityPlanner(
            RendererMaterial baselineMaterial,
            CpuFrustumCuller frustumCuller,
            DrawSubmissionSorter submissionSorter) {
        this.baselineMaterial = Objects.requireNonNull(baselineMaterial, "baselineMaterial");
        this.frustumCuller = Objects.requireNonNull(frustumCuller, "frustumCuller");
        this.submissionSorter = Objects.requireNonNull(submissionSorter, "submissionSorter");
    }

    Frustum3f extractFrustum(Matrix4fc viewMatrix, Matrix4fc projectionMatrix) {
        return ViewFrustumExtractor.extract(viewMatrix, projectionMatrix);
    }

    VisibilityPlan plan(
            Matrix4fc viewMatrix,
            Frustum3f frustum,
            int framebufferWidth,
            int framebufferHeight) {
        ArrayList<DrawSubmission> visibleSubmissions = new ArrayList<>(1);
        float referenceDepth = cameraDepth(viewMatrix, ReferenceRoomFixture.WORLD_BOUNDS);

        int testedCandidates = 1;
        int visibleCandidates = 0;
        int culledCandidates = 0;
        if (frustumCuller.isVisible(frustum, ReferenceRoomFixture.WORLD_BOUNDS)) {
            visibleCandidates = 1;
            visibleSubmissions.add(new DrawSubmission(
                    baselineMaterial,
                    PROGRAM_KEY_REFERENCE,
                    MATERIAL_KEY_BASELINE,
                    MESH_KEY_REFERENCE,
                    referenceDepth,
                    0,
                    0,
                    0,
                    framebufferWidth,
                    framebufferHeight));
        } else {
            culledCandidates = 1;
        }

        return new VisibilityPlan(
                submissionSorter.sort(visibleSubmissions),
                testedCandidates,
                visibleCandidates,
                culledCandidates);
    }

    private static float cameraDepth(Matrix4fc viewMatrix, Aabb3f worldBounds) {
        Vector3f minimum = worldBounds.minimum(new Vector3f());
        Vector3f maximum = worldBounds.maximum(new Vector3f());
        Vector3f center = new Vector3f(
                (minimum.x() + maximum.x()) * 0.5f,
                (minimum.y() + maximum.y()) * 0.5f,
                (minimum.z() + maximum.z()) * 0.5f);
        viewMatrix.transformPosition(center);
        float depth = -center.z();
        if (!Float.isFinite(depth)) {
            throw new IllegalArgumentException("camera-space submission depth must be finite");
        }
        return depth;
    }

    record VisibilityPlan(
            List<DrawSubmission> orderedSubmissions,
            int testedCandidates,
            int visibleCandidates,
            int culledCandidates) {

        VisibilityPlan {
            orderedSubmissions = List.copyOf(Objects.requireNonNull(orderedSubmissions, "orderedSubmissions"));
        }
    }
}
