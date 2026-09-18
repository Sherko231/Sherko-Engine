package com.samo.engine.render.opengl.internal;

import java.util.Objects;
import org.lwjgl.util.shaderc.Shaderc;

final class GlslOfflineValidator {
    enum Stage {
        VERTEX(Shaderc.shaderc_glsl_vertex_shader),
        FRAGMENT(Shaderc.shaderc_glsl_fragment_shader);

        private final int shadercKind;

        Stage(int shadercKind) {
            this.shadercKind = shadercKind;
        }
    }

    private GlslOfflineValidator() {
    }

    static void validate(Stage stage, String sourceName, String source) {
        Stage shaderStage = Objects.requireNonNull(stage, "stage");
        String name = Objects.requireNonNull(sourceName, "sourceName");
        String shaderSource = Objects.requireNonNull(source, "source");

        if (!shaderSource.contains("#version 460 core")) {
            throw new IllegalStateException(
                    "Offline GLSL validation failed for " + name + " [" + shaderStage + "]: "
                            + "missing required '#version 460 core'");
        }

        long compiler = Shaderc.shaderc_compiler_initialize();
        if (compiler == 0L) {
            throw new IllegalStateException("Unable to initialize Shaderc compiler for " + name);
        }

        long options = 0L;
        long result = 0L;
        try {
            options = Shaderc.shaderc_compile_options_initialize();
            if (options == 0L) {
                throw new IllegalStateException("Unable to initialize Shaderc options for " + name);
            }

            result = Shaderc.shaderc_compile_into_spv(
                    compiler,
                    shaderSource,
                    shaderStage.shadercKind,
                    name,
                    "main",
                    options);
            if (result == 0L) {
                throw new IllegalStateException(
                        "Shaderc returned no result for " + name + " [" + shaderStage + "]");
            }

            int status = Shaderc.shaderc_result_get_compilation_status(result);
            if (status != Shaderc.shaderc_compilation_status_success) {
                String diagnostics = Shaderc.shaderc_result_get_error_message(result);
                throw new IllegalStateException(
                        "Offline GLSL validation failed for " + name + " [" + shaderStage + "]: "
                                + diagnostics);
            }
        } finally {
            if (result != 0L) {
                Shaderc.shaderc_result_release(result);
            }
            if (options != 0L) {
                Shaderc.shaderc_compile_options_release(options);
            }
            Shaderc.shaderc_compiler_release(compiler);
        }
    }
}
