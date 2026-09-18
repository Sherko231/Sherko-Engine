#version 460 core

layout(binding = 0) uniform sampler2D referenceTexture;

layout(location = 0) out vec4 color;

void main() {
    color = texture(referenceTexture, vec2(0.5));
}
