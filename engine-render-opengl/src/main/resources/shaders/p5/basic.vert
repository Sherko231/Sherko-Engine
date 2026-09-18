#version 460 core

layout(std140, binding = 0) uniform CameraBlock {
    mat4 view;
    mat4 projection;
};

layout(std140, binding = 1) uniform PerFrameBlock {
    vec4 framebufferSizeAndInverse;
};

layout(location = 0) in vec3 position;

void main() {
    gl_Position = projection * view * vec4(position, 1.0);
    gl_PointSize = max(1.0, framebufferSizeAndInverse.x * framebufferSizeAndInverse.z);
}
