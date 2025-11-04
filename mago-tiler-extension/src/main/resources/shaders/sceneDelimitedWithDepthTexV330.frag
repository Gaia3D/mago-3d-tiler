#version 330
//precision highp float;

uniform sampler2D albedoTexture;
uniform sampler2D depthTexture;

// colorMode = 0: oneColor, 1: vertexColor, 2: textureColor
uniform int uColorMode;
uniform vec4 uOneColor;

uniform vec3 bboxMin;// render only the part of the scene that is inside the bounding box
uniform vec3 bboxMax;// render only the part of the scene that is inside the bounding box

in vec4 vColor;
in vec2 vTexCoord;
in vec3 vNormal;
in vec3 vCamDir;

in vec3 fragLocalPos;

layout(location = 0) out vec4 outAlbedo;
layout(location = 1) out vec4 outDepth;

float UnpackDepth32(in vec4 packedDepth) {
    // Ajusta el valor original que se agregó en el último paso de PackDepth32
    packedDepth -= 1.0 / 512.0;

    // Recupera el valor de profundidad original
    return dot(packedDepth, vec4(1.0, 1.0 / 256.0, 1.0 / (256.0 * 256.0), 1.0 / 16777216.0));
}

vec4 PackDepth32(in float depth)
{
    depth *= (16777216.0 - 1.0) / (16777216.0);
    vec4 encode = fract(depth * vec4(1.0, 256.0, 256.0*256.0, 16777216.0));// 256.0*256.0*256.0 = 16777216.0
    return vec4(encode.xyz - encode.yzw / 256.0, encode.w) + 1.0/512.0;
}

void main(void) {
    if (fragLocalPos.x < bboxMin.x || fragLocalPos.x > bboxMax.x ||
    fragLocalPos.y < bboxMin.y || fragLocalPos.y > bboxMax.y ||
    fragLocalPos.z < bboxMin.z || fragLocalPos.z > bboxMax.z) {
        discard;
    }

    // check depth texture
    float sceneDepth = UnpackDepth32(texture(depthTexture, vTexCoord));
    float fragDepth = gl_FragCoord.z;
    if (fragDepth > sceneDepth) {
        discard;
    }

    vec4 finalColor = vec4(1.0, 1.0, 1.0, 1.0);
    if (uColorMode == 0) {
        finalColor = uOneColor;
    } else if (uColorMode == 1) {
        finalColor = vColor;
    } else if (uColorMode == 2) {
        finalColor = texture(texture0, vTexCoord);
    }

    outAlbedo = finalColor;
    outDepth = PackDepth32(fragDepth);
}