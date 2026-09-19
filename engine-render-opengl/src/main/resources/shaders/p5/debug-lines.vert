#version 460 core

layout(std140, binding = 0) uniform CameraBlock {
    mat4 view;
    mat4 projection;
};

layout(location = 0) in vec3 position;
layout(location = 1) in vec3 linearColor;

layout(location = 0) out vec3 debugColor;

void main() {
    debugColor = linearColor;
    gl_Position = projection * view * vec4(position, 1.0);
}
