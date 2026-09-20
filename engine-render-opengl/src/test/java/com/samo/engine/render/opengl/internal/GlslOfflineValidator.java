package com.samo.engine.render.opengl.internal;

import java.util.Objects;
import org.lwjgl.util.shaderc.Shaderc;

final class GlslOfflineValidator {
    record ValidationResult(long warningCount, String diagnostics) {
    }

    enum Stage {
        VERTEX(Shaderc.shaderc_glsl_vertex_shader), FRAGMENT(Shaderc.shaderc_glsl_fragment_shader);

        private final int shadercKind;

        Stage(int shadercKind) {
            this.shadercKind = shadercKind;
        }
    }

    private GlslOfflineValidator() {
    }

    static ValidationResult validate(Stage stage, String sourceName, String source) {
        Stage shaderStage = Objects.requireNonNull(stage, "stage");
        String name = Objects.requireNonNull(sourceName, "sourceName");
        String shaderSource = Objects.requireNonNull(source, "source");

        if (!shaderSource.contains("#version 460 core")) {
            throw new IllegalStateException("Offline GLSL validation failed for " + name + " [" + shaderStage + "]: " + "missing required '#version 460 core'");
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

            Shaderc.shaderc_compile_options_set_source_language(options, Shaderc.shaderc_source_language_glsl);

            // Shaderc defaults to Vulkan semantics. These are OpenGL runtime shaders,
            // so validate against Shaderc's OpenGL SPIR-V environment explicitly.
            Shaderc.shaderc_compile_options_set_target_env(options, Shaderc.shaderc_target_env_opengl, Shaderc.shaderc_env_version_opengl_4_5);
            Shaderc.shaderc_compile_options_set_target_spirv(options, Shaderc.shaderc_spirv_version_1_0);

            result = Shaderc.shaderc_compile_into_spv(compiler, shaderSource, shaderStage.shadercKind, name, "main", options);
            if (result == 0L) {
                throw new IllegalStateException("Shaderc returned no result for " + name + " [" + shaderStage + "]");
            }

            int status = Shaderc.shaderc_result_get_compilation_status(result);
            String diagnostics = Shaderc.shaderc_result_get_error_message(result);
            if (status != Shaderc.shaderc_compilation_status_success) {
                throw new IllegalStateException("Offline GLSL validation failed for " + name + " [" + shaderStage + "]: " + diagnostics);
            }
            return new ValidationResult(Shaderc.shaderc_result_get_num_warnings(result), diagnostics == null ? "" : diagnostics);
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
