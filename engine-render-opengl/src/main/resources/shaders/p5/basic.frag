#version 460 core

layout(binding = 0) uniform sampler2D referenceTexture;
layout(location = 0) uniform vec4 materialColorMultiplier;
layout(location = 1) uniform vec3 directionalLightDirection;
layout(location = 2) uniform vec3 directionalLightColor;
layout(location = 3) uniform float directionalLightIntensity;

layout(location = 0) in vec3 worldNormal;
layout(location = 0) out vec4 color;

vec3 linearToSrgb(vec3 linearColor) {
    bvec3 cutoff = lessThanEqual(linearColor, vec3(0.0031308));
    vec3 lower = linearColor * 12.92;
    vec3 upper = 1.055 * pow(linearColor, vec3(1.0 / 2.4)) - 0.055;
    return mix(upper, lower, cutoff);
}

void main() {
    vec4 sampled = texture(referenceTexture, vec2(0.5));
    vec4 materialColor = sampled * materialColorMultiplier;
    float diffuse = max(
        dot(normalize(worldNormal), normalize(-directionalLightDirection)),
        0.0);
    materialColor.rgb *= directionalLightColor * directionalLightIntensity * diffuse;
#ifdef SHERKO_MANUAL_SRGB_ENCODE
    color = vec4(linearToSrgb(materialColor.rgb), materialColor.a);
#else
    color = materialColor;
#endif
}
