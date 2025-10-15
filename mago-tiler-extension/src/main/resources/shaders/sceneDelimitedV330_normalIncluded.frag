#version 330
//precision highp int;

uniform sampler2D albedoTexture;// texture0
uniform sampler2D normalTexture;// texture1

// colorMode = 0: oneColor, 1: vertexColor, 2: textureColor
uniform int uColorMode;
uniform vec4 uOneColor;

uniform vec3 bboxMin;// render only the part of the scene that is inside the bounding box
uniform vec3 bboxMax;// render only the part of the scene that is inside the bounding box

in vec4 vColor;
in vec2 vTexCoord;
in vec3 vNormal;
in vec3 vTangent;
in vec3 vBitangent;
in vec3 vCamDir;
in vec3 vCamUp;

in vec3 fragLocalPos;

layout(location = 0) out vec4 outAlbedo;
layout(location = 1) out vec4 outNormal;

vec3 srgbToLinear(vec3 srgbColor) {
    vec3 bLess = step(vec3(0.04045), srgbColor);
    vec3 lower = srgbColor / 12.92;
    vec3 higher = pow((srgbColor + 0.055) / 1.055, vec3(2.4));
    return mix(lower, higher, bLess);
}

vec3 decodedNormal(vec2 texCoord){
    vec3 n = texture(normalTexture, texCoord).rgb * 2.0 - 1.0;
    return normalize(n);
}

void main(void) {
    if (fragLocalPos.x < bboxMin.x || fragLocalPos.x > bboxMax.x ||
    fragLocalPos.y < bboxMin.y || fragLocalPos.y > bboxMax.y ||
    fragLocalPos.z < bboxMin.z || fragLocalPos.z > bboxMax.z) {
        discard;
    }

    vec4 finalColor = vec4(0.9, 0.5, 0.25, 1.0);
    vec4 finalNormal = texture(normalTexture, vTexCoord);

    if (uColorMode == 0) {
        finalColor = uOneColor;
    } else if (uColorMode == 1) {
        finalColor = vColor;
    } else if (uColorMode == 2) {
        finalColor = texture(albedoTexture, vTexCoord);
    }

    outAlbedo = finalColor;

    // Transform normal from tangent to world space
    mat3 TBN = mat3(vTangent, vBitangent, vNormal);
    //mat3 tbnInverse = inverse(TBN);
    vec3 n = decodedNormal(vTexCoord);
    n = normalize(TBN * n);

    // now, with camDir and camUp make TBN2 matrix to transform from world to view space
    //    vec3 camRight = normalize(cross(vCamDir, vCamUp));
    //    vec3 camUpCorrected = normalize(cross(camRight, vCamDir));
    //    mat3 TBN2 = mat3(camRight, camUpCorrected, -vCamDir);
    //    mat3 TBN2Inverse = inverse(TBN2);
    //    n = normalize(TBN2Inverse * n);

    vec3 srgbNormal = (srgbToLinear(n));
    finalNormal = vec4(n * 0.5 + 0.5, 1.0);

    outNormal = finalNormal;
}