#version 330
//precision highp float;

uniform sampler2D texture0;

in vec2 vTexCoord;

layout (location = 0) out vec4 fragColor;

void main(void) {
    vec4 finalColor = texture(texture0, vTexCoord);
    fragColor = finalColor;
}