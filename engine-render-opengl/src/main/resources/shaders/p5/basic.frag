#version 460 core

layout(binding = 0) uniform sampler2D referenceTexture;
layout(location = 0) uniform vec4 materialColorMultiplier;
layout(location = 1) uniform vec3 directionalLightDirection;
layout(location = 2) uniform vec3 directionalLightColor;
layout(location = 3) uniform float directionalLightIntensity;

layout(std140, binding = 2) uniform LocalLightBlock {
    vec4 localLightPositionRange[8];
    vec4 localLightDirectionType[8];
    vec4 localLightColorIntensity[8];
    vec4 localLightConeCosines[8];
    ivec4 localLightMeta;
};

layout(location = 0) in vec3 worldNormal;
layout(location = 1) in vec3 worldPosition;
layout(location = 0) out vec4 color;

vec3 linearToSrgb(vec3 linearColor) {
    bvec3 cutoff = lessThanEqual(linearColor, vec3(0.0031308));
    vec3 lower = linearColor * 12.92;
    vec3 upper = 1.055 * pow(linearColor, vec3(1.0 / 2.4)) - 0.055;
    return mix(upper, lower, cutoff);
}

vec3 localLightContribution(int index, vec3 normal) {
    vec3 lightOffset = localLightPositionRange[index].xyz - worldPosition;
    float distanceToLight = length(lightOffset);
    float rangeMeters = localLightPositionRange[index].w;
    if (distanceToLight <= 0.0 || distanceToLight >= rangeMeters) {
        return vec3(0.0);
    }

    vec3 surfaceToLight = lightOffset / distanceToLight;
    float lambert = max(dot(normal, surfaceToLight), 0.0);
    if (lambert <= 0.0) {
        return vec3(0.0);
    }

    float normalizedDistance = distanceToLight / rangeMeters;
    float rangeAttenuation = 1.0 - normalizedDistance;
    rangeAttenuation *= rangeAttenuation;

    float coneAttenuation = 1.0;
    if (localLightDirectionType[index].w > 0.5) {
        vec3 lightToSurface = -surfaceToLight;
        float coneAlignment = dot(lightToSurface, localLightDirectionType[index].xyz);
        float innerCosine = localLightConeCosines[index].x;
        float outerCosine = localLightConeCosines[index].y;
        coneAttenuation = smoothstep(outerCosine, innerCosine, coneAlignment);
    }

    vec4 colorIntensity = localLightColorIntensity[index];
    return colorIntensity.rgb
        * colorIntensity.a
        * lambert
        * rangeAttenuation
        * coneAttenuation;
}

void main() {
    vec4 sampled = texture(referenceTexture, vec2(0.5));
    vec4 materialColor = sampled * materialColorMultiplier;
    vec3 normal = normalize(worldNormal);
    float directionalDiffuse = max(
        dot(normal, normalize(-directionalLightDirection)),
        0.0);
    vec3 illumination =
        directionalLightColor * directionalLightIntensity * directionalDiffuse;

    int localLightCount = clamp(localLightMeta.x, 0, 8);
    for (int index = 0; index < localLightCount; index++) {
        illumination += localLightContribution(index, normal);
    }

    materialColor.rgb *= clamp(illumination, vec3(0.0), vec3(1.0));
#ifdef SHERKO_MANUAL_SRGB_ENCODE
    color = vec4(linearToSrgb(materialColor.rgb), materialColor.a);
#else
    color = materialColor;
#endif
}
