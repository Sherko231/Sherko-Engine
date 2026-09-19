#version 460 core

layout(location = 0) in vec3 viewModelColor;
layout(location = 0) out vec4 color;

vec3 linearToSrgb(vec3 linearColor) {
    bvec3 cutoff = lessThanEqual(linearColor, vec3(0.0031308));
    vec3 lower = linearColor * 12.92;
    vec3 upper = 1.055 * pow(linearColor, vec3(1.0 / 2.4)) - 0.055;
    return mix(upper, lower, cutoff);
}

void main() {
#ifdef SHERKO_MANUAL_SRGB_ENCODE
    color = vec4(linearToSrgb(viewModelColor), 1.0);
#else
    color = vec4(viewModelColor, 1.0);
#endif
}
