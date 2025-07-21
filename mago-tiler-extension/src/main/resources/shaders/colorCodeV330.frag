#version 330
//precision highp float;
// colorMode = 0: oneColor, 1: vertexColor, 2: textureColor
in vec4 vColor;

layout (location = 0) out vec4 fragColor;

void main(void) {
    fragColor = vColor;
}