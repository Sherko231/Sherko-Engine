#version 460 core

layout(std140, binding = 0) uniform CameraBlock {
    mat4 view;
    mat4 projection;
};

layout(std140, binding = 1) uniform PerFrameBlock {
    vec4 framebufferSizeAndInverse;
};

layout(location = 0) in vec3 position;
layout(location = 1) in vec3 normal;
layout(location = 2) in vec2 texCoord;

layout(location = 0) out vec3 worldNormal;
layout(location = 1) out vec3 worldPosition;
layout(location = 2) out vec2 roomTexCoord;

void main() {
    worldNormal = normal;
    worldPosition = position;
    roomTexCoord = texCoord;
    gl_Position = projection * view * vec4(position, 1.0);
    gl_PointSize = max(1.0, framebufferSizeAndInverse.x * framebufferSizeAndInverse.z);
}
