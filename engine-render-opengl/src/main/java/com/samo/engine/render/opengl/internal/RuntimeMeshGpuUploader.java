package com.samo.engine.render.opengl.internal;

import com.samo.engine.assets.api.MeshAsset;
import com.samo.engine.assets.api.ResourceHandle;
import com.samo.engine.core.api.NativeResourceRegistry;
import com.samo.engine.platform.api.OpenGlThreadGuard;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

final class RuntimeMeshGpuUploader {
    private final OpenGlThreadGuard threadGuard;
    private final NativeResourceRegistry registry;
    private final OpenGlResourceBackend backend;

    RuntimeMeshGpuUploader(OpenGlThreadGuard threadGuard, NativeResourceRegistry registry, OpenGlResourceBackend backend) {

        this.threadGuard = Objects.requireNonNull(threadGuard, "threadGuard");
        this.registry = Objects.requireNonNull(registry, "registry");
        this.backend = Objects.requireNonNull(backend, "backend");

    }

    UploadedRuntimeMesh upload(ResourceHandle<MeshAsset> handle) {

        threadGuard.assertOwnerThread();
        MeshAsset asset = Objects.requireNonNull(handle, "handle").requireReady();
        ArrayList<UploadedPrimitive> uploaded = new ArrayList<>(asset.primitives().size());
        try {
            for (MeshAsset.Primitive primitive : asset.primitives()) {
                uploaded.add(uploadPrimitive(primitive));
            }
            return new UploadedRuntimeMesh(threadGuard, uploaded);
        } catch (RuntimeException | Error failure) {
            for (int index = uploaded.size() - 1; index >= 0; index--) {
                UploadedPrimitive primitive = uploaded.get(index);
                CleanupFailureSuppression.runAndSuppress(failure, primitive::close);
            }
            throw failure;
        }

    }

    private UploadedPrimitive uploadPrimitive(MeshAsset.Primitive primitive) {

        ByteBuffer vertices = packVertices(primitive);
        ByteBuffer indices = packIndices(primitive.indices());
        OpenGlBuffer vertexBuffer = null;
        OpenGlBuffer indexBuffer = null;
        try {
            vertexBuffer = OpenGlBuffer.create(threadGuard, registry, backend);
            backend.allocateDynamicBufferStorage(vertexBuffer.handle(), vertices.remaining());
            backend.uploadBufferSubData(vertexBuffer.handle(), 0L, vertices);

            indexBuffer = OpenGlBuffer.create(threadGuard, registry, backend);
            backend.allocateDynamicBufferStorage(indexBuffer.handle(), indices.remaining());
            backend.uploadBufferSubData(indexBuffer.handle(), 0L, indices);
            return new UploadedPrimitive(threadGuard, vertexBuffer, indexBuffer, primitive.indices().length);
        } catch (RuntimeException | Error failure) {
            if (indexBuffer != null) {
                CleanupFailureSuppression.runAndSuppress(failure, indexBuffer::close);
            }
            if (vertexBuffer != null) {
                CleanupFailureSuppression.runAndSuppress(failure, vertexBuffer::close);
            }
            throw failure;
        }

    }

    private static ByteBuffer packVertices(MeshAsset.Primitive primitive) {

        float[] positions = primitive.positions();
        float[] normals = primitive.normals();
        float[] tangents = primitive.tangents();
        float[] tangentSigns = primitive.tangentSigns();
        float[] uv0 = primitive.uv0();
        int vertexCount = positions.length / 3;
        int floatsPerVertex = 3 + (normals == null ? 0 : 3) + (tangents == null ? 0 : 4) + (uv0 == null ? 0 : 2);
        ByteBuffer bytes = ByteBuffer.allocateDirect(Math.multiplyExact(Math.multiplyExact(vertexCount, floatsPerVertex), Float.BYTES)).order(ByteOrder.nativeOrder());
        for (int vertex = 0; vertex < vertexCount; vertex++) {
            putVec3(bytes, positions, vertex);
            if (normals != null) {
                putVec3(bytes, normals, vertex);
            }
            if (tangents != null) {
                putVec3(bytes, tangents, vertex);
                bytes.putFloat(tangentSigns[vertex]);
            }
            if (uv0 != null) {
                int base = vertex * 2;
                bytes.putFloat(uv0[base]);
                bytes.putFloat(uv0[base + 1]);
            }
        }
        return bytes.flip();

    }

    private static ByteBuffer packIndices(int[] indices) {

        ByteBuffer bytes = ByteBuffer.allocateDirect(Math.multiplyExact(indices.length, Integer.BYTES)).order(ByteOrder.nativeOrder());
        for (int index : indices) {
            bytes.putInt(index);
        }
        return bytes.flip();

    }

    private static void putVec3(ByteBuffer target, float[] values, int vertex) {

        int base = vertex * 3;
        target.putFloat(values[base]);
        target.putFloat(values[base + 1]);
        target.putFloat(values[base + 2]);

    }

    private static void rethrow(Throwable failure) {

        if (failure instanceof RuntimeException runtimeException) {
            throw runtimeException;
        }
        if (failure instanceof Error error) {
            throw error;
        }

    }

    static final class UploadedRuntimeMesh implements AutoCloseable {
        private final OpenGlThreadGuard threadGuard;
        private final List<UploadedPrimitive> primitives;
        private boolean closeAttempted;

        private UploadedRuntimeMesh(OpenGlThreadGuard threadGuard, List<UploadedPrimitive> primitives) {

            this.threadGuard = threadGuard;
            this.primitives = List.copyOf(primitives);

        }

        List<UploadedPrimitive> primitives() {

            return primitives;

        }

        @Override
        public void close() {

            if (closeAttempted) {
                return;
            }
            threadGuard.assertOwnerThread();
            closeAttempted = true;
            Throwable firstFailure = null;
            for (int index = primitives.size() - 1; index >= 0; index--) {
                try {
                    primitives.get(index).close();
                } catch (RuntimeException | Error failure) {
                    if (firstFailure == null) {
                        firstFailure = failure;
                    } else {
                        CleanupFailureSuppression.addSuppressedUnlessSame(firstFailure, failure);
                    }
                }
            }
            rethrow(firstFailure);

        }
    }

    static final class UploadedPrimitive implements AutoCloseable {
        private final OpenGlThreadGuard threadGuard;
        private final OpenGlBuffer vertexBuffer;
        private final OpenGlBuffer indexBuffer;
        private final int indexCount;
        private boolean closeAttempted;

        private UploadedPrimitive(OpenGlThreadGuard threadGuard, OpenGlBuffer vertexBuffer, OpenGlBuffer indexBuffer, int indexCount) {

            this.threadGuard = threadGuard;
            this.vertexBuffer = vertexBuffer;
            this.indexBuffer = indexBuffer;
            this.indexCount = indexCount;

        }

        int vertexBufferHandle() {

            return vertexBuffer.handle();

        }

        int indexBufferHandle() {

            return indexBuffer.handle();

        }

        int indexCount() {

            return indexCount;

        }

        @Override
        public void close() {

            if (closeAttempted) {
                return;
            }
            threadGuard.assertOwnerThread();
            closeAttempted = true;
            Throwable firstFailure = null;
            try {
                indexBuffer.close();
            } catch (RuntimeException | Error failure) {
                firstFailure = failure;
            }
            try {
                vertexBuffer.close();
            } catch (RuntimeException | Error failure) {
                if (firstFailure == null) {
                    firstFailure = failure;
                } else {
                    CleanupFailureSuppression.addSuppressedUnlessSame(firstFailure, failure);
                }
            }
            rethrow(firstFailure);

        }
    }
}
