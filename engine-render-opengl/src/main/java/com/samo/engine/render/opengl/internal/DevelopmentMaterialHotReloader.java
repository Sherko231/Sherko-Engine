package com.samo.engine.render.opengl.internal;

import com.samo.engine.assets.api.MaterialAsset;
import com.samo.engine.assets.api.ResourceHandle;
import com.samo.engine.assets.api.ResourceHandleState;
import com.samo.engine.core.api.NativeResourceRegistry;
import com.samo.engine.platform.api.OpenGlThreadGuard;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

final class DevelopmentMaterialHotReloader implements AutoCloseable {
    private final ResourceHandle<MaterialAsset> materialHandle;
    private final Path shaderRoot;
    private final OpenGlThreadGuard guard;
    private final NativeResourceRegistry registry;
    private final OpenGlResourceBackend backend;
    private final int textureHandle;
    private final int samplerHandle;
    private ProgramBundle program;
    private RenderMaterialDescriptor material;
    private MaterialAsset acceptedMaterial;
    private byte[] acceptedVertexBytes;
    private byte[] acceptedFragmentBytes;
    private boolean closed;

    private DevelopmentMaterialHotReloader(ResourceHandle<MaterialAsset> materialHandle, Path shaderRoot, OpenGlThreadGuard guard, NativeResourceRegistry registry,
        OpenGlResourceBackend backend, int textureHandle, int samplerHandle) {

        this.materialHandle = Objects.requireNonNull(materialHandle, "materialHandle");
        this.shaderRoot = requireShaderRoot(shaderRoot);
        this.guard = Objects.requireNonNull(guard, "guard");
        this.registry = Objects.requireNonNull(registry, "registry");
        this.backend = Objects.requireNonNull(backend, "backend");
        if (textureHandle <= 0 || samplerHandle <= 0) {
            throw new IllegalArgumentException("textureHandle and samplerHandle must be positive");
        }
        this.textureHandle = textureHandle;
        this.samplerHandle = samplerHandle;

        guard.assertOwnerThread();
        MaterialAsset initial = requireReadyMaterial();
        ShaderSources sources = readShaderSources(initial.shaderKey());
        ProgramBundle initialProgram = ProgramBundle.create(initial.shaderKey(), sources, guard, registry, backend);
        this.program = initialProgram;
        this.material = descriptor(initial);
        this.acceptedMaterial = initial;
        this.acceptedVertexBytes = sources.vertexBytes();
        this.acceptedFragmentBytes = sources.fragmentBytes();

    }

    static DevelopmentMaterialHotReloader create(ResourceHandle<MaterialAsset> materialHandle, Path shaderRoot, OpenGlThreadGuard guard, NativeResourceRegistry registry,
        OpenGlResourceBackend backend, int textureHandle, int samplerHandle) {

        return new DevelopmentMaterialHotReloader(materialHandle, shaderRoot, guard, registry, backend, textureHandle, samplerHandle);

    }

    void poll() {

        guard.assertOwnerThread();
        requireOpen();

        MaterialAsset candidateMaterial = requireReadyMaterial();
        ShaderSources candidateSources;
        try {
            candidateSources = readShaderSources(candidateMaterial.shaderKey());
        } catch (RuntimeException failure) {
            return;
        }

        boolean shaderChanged = !candidateMaterial.shaderKey().equals(acceptedMaterial.shaderKey()) || !Arrays.equals(candidateSources.vertexBytes(), acceptedVertexBytes)
            || !Arrays.equals(candidateSources.fragmentBytes(), acceptedFragmentBytes);
        if (shaderChanged) {
            ProgramBundle candidateProgram;
            try {
                candidateProgram = ProgramBundle.create(candidateMaterial.shaderKey(), candidateSources, guard, registry, backend);
            } catch (RuntimeException | Error failure) {
                return;
            }

            ProgramBundle previous = program;
            program = candidateProgram;
            material = descriptor(candidateMaterial);
            acceptedMaterial = candidateMaterial;
            acceptedVertexBytes = candidateSources.vertexBytes();
            acceptedFragmentBytes = candidateSources.fragmentBytes();
            previous.close();
            return;
        }

        if (!candidateMaterial.equals(acceptedMaterial)) {
            material = descriptor(candidateMaterial);
            acceptedMaterial = candidateMaterial;
        }

    }

    int programHandle() {

        requireOpen();
        return program.program().handle();

    }

    RenderMaterialDescriptor material() {

        requireOpen();
        return material;

    }

    @Override
    public void close() {

        if (closed) {
            return;
        }
        guard.assertOwnerThread();
        closed = true;
        program.close();

    }

    private MaterialAsset requireReadyMaterial() {

        if (materialHandle.state() != ResourceHandleState.READY) {
            throw new IllegalStateException("Development material handle must remain READY");
        }
        return materialHandle.requireReady();

    }

    private ShaderSources readShaderSources(String shaderKey) {

        Path vertex = shaderRoot.resolve(shaderKey + ".vert").normalize();
        Path fragment = shaderRoot.resolve(shaderKey + ".frag").normalize();
        if (!vertex.startsWith(shaderRoot) || !fragment.startsWith(shaderRoot)) {
            throw new IllegalArgumentException("shaderKey escaped development shader root");
        }
        try {
            byte[] vertexBytes = Files.readAllBytes(vertex);
            byte[] fragmentBytes = Files.readAllBytes(fragment);
            if (vertexBytes.length == 0 || fragmentBytes.length == 0) {
                throw new IllegalArgumentException("Development shader sources must be nonempty");
            }
            return new ShaderSources(vertexBytes, fragmentBytes);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read development shader pair for " + shaderKey, exception);
        }

    }

    private RenderMaterialDescriptor descriptor(MaterialAsset source) {

        return new RenderMaterialDescriptor(MaterialShaderVariant.TEXTURED_REFERENCE, List.of(new MaterialTextureBinding(0, textureHandle, samplerHandle)),
            new MaterialScalars(source.redMultiplier(), source.greenMultiplier(), source.blueMultiplier(), source.alphaMultiplier()), MaterialBlendMode.OPAQUE,
            MaterialDepthMode.TEST_WRITE, MaterialCullMode.BACK);

    }

    private void requireOpen() {

        if (closed) {
            throw new IllegalStateException("Development material hot reloader is closed");
        }

    }

    private static Path requireShaderRoot(Path path) {

        Path root = Objects.requireNonNull(path, "shaderRoot").toAbsolutePath().normalize();
        if (Files.isSymbolicLink(root) || !Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS) || !Files.isReadable(root)) {
            throw new IllegalArgumentException(root + ": development shader root must be a readable non-symbolic-link directory");
        }
        return root;

    }

    private record ShaderSources(byte[] vertexBytes, byte[] fragmentBytes) {
        private ShaderSources {

            vertexBytes = vertexBytes.clone();
            fragmentBytes = fragmentBytes.clone();

        }

        String vertexSource() {

            return new String(vertexBytes, StandardCharsets.UTF_8);

        }

        String fragmentSource() {

            return new String(fragmentBytes, StandardCharsets.UTF_8);

        }

        @Override
        public byte[] vertexBytes() {

            return vertexBytes.clone();

        }

        @Override
        public byte[] fragmentBytes() {

            return fragmentBytes.clone();

        }
    }

    private record ProgramBundle(OpenGlShader vertex, OpenGlShader fragment, OpenGlProgram program) implements AutoCloseable {
        static ProgramBundle create(String shaderKey, ShaderSources sources, OpenGlThreadGuard guard, NativeResourceRegistry registry, OpenGlResourceBackend backend) {

            OpenGlShader vertex = null;
            OpenGlShader fragment = null;
            try {
                vertex = OpenGlShader.compile(OpenGlShader.Stage.VERTEX, shaderKey + ".vert", sources.vertexSource(), guard, registry, backend);
                fragment = OpenGlShader.compile(OpenGlShader.Stage.FRAGMENT, shaderKey + ".frag", sources.fragmentSource(), guard, registry, backend);
                OpenGlProgram program = OpenGlProgram.link("development:" + shaderKey, vertex, fragment, guard, registry, backend);
                return new ProgramBundle(vertex, fragment, program);
            } catch (RuntimeException | Error failure) {
                if (fragment != null) {
                    CleanupFailureSuppression.runAndSuppress(failure, fragment::close);
                }
                if (vertex != null) {
                    CleanupFailureSuppression.runAndSuppress(failure, vertex::close);
                }
                throw failure;
            }

        }

        @Override
        public void close() {

            RuntimeException failure = null;
            try {
                program.close();
            } catch (RuntimeException exception) {
                failure = exception;
            }
            try {
                fragment.close();
            } catch (RuntimeException exception) {
                if (failure == null) {
                    failure = exception;
                } else {
                    CleanupFailureSuppression.runAndSuppress(failure, () -> {
                        throw exception;
                    });
                }
            }
            try {
                vertex.close();
            } catch (RuntimeException exception) {
                if (failure == null) {
                    failure = exception;
                } else {
                    CleanupFailureSuppression.runAndSuppress(failure, () -> {
                        throw exception;
                    });
                }
            }
            if (failure != null) {
                throw failure;
            }

        }
    }
}
