#version 460 core

layout(binding = 0) uniform sampler2D referenceTexture;
layout(location = 0) uniform int manualSrgbEncode;

layout(location = 0) out vec4 color;

vec3 linearToSrgb(vec3 linearColor) {
    bvec3 cutoff = lessThanEqual(linearColor, vec3(0.0031308));
    vec3 lower = linearColor * 12.92;
    vec3 upper = 1.055 * pow(linearColor, vec3(1.0 / 2.4)) - 0.055;
    return mix(upper, lower, cutoff);
}

void main() {
    vec4 sampled = texture(referenceTexture, vec2(0.5));
    color = manualSrgbEncode != 0
            ? vec4(linearToSrgb(sampled.rgb), sampled.a)
            : sampled;
}
