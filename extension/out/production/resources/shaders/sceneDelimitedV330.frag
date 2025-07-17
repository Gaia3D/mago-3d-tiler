#version 330
//precision highp float;

uniform sampler2D texture0;

// colorMode = 0: oneColor, 1: vertexColor, 2: textureColor
uniform int uColorMode;
uniform vec4 uOneColor;

uniform vec3 bboxMin; // render only the part of the scene that is inside the bounding box
uniform vec3 bboxMax; // render only the part of the scene that is inside the bounding box

in vec4 vColor;
in vec2 vTexCoord;
in vec3 vNormal;
in vec3 vCamDir;

in vec3 fragLocalPos;

layout (location = 0) out vec4 fragColor;

void main(void) {
    if (fragLocalPos.x < bboxMin.x || fragLocalPos.x > bboxMax.x ||
        fragLocalPos.y < bboxMin.y || fragLocalPos.y > bboxMax.y ||
        fragLocalPos.z < bboxMin.z || fragLocalPos.z > bboxMax.z) {
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

    fragColor = finalColor;
}