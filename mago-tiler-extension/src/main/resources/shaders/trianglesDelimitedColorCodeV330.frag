#version 330
//precision highp float;

uniform vec3 bboxMin; // render only the part of the scene that is inside the bounding box
uniform vec3 bboxMax; // render only the part of the scene that is inside the bounding box

// colorMode = 0: oneColor, 1: vertexColor, 2: textureColor
in vec4 vColor;

in vec3 fragLocalPos;

layout (location = 0) out vec4 fragColor;

void main(void) {
    if (fragLocalPos.x < bboxMin.x || fragLocalPos.x > bboxMax.x ||
        fragLocalPos.y < bboxMin.y || fragLocalPos.y > bboxMax.y ||
        fragLocalPos.z < bboxMin.z || fragLocalPos.z > bboxMax.z) {
        discard;
    }

    fragColor = vColor;

}