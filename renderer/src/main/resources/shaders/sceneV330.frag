#version 330
//precision highp float;

uniform sampler2D texture0;

// colorMode = 0: oneColor, 1: vertexColor, 2: textureColor
uniform int uColorMode;
uniform vec4 uOneColor;
in vec4 vColor;
in vec2 vTexCoord;
in vec3 vNormal;
in vec3 vCamDir;

layout (location = 0) out vec4 fragColor;

void main(void) {
    vec4 finalColor = vec4(1.0, 1.0, 1.0, 1.0);
    if (uColorMode == 0) {
        finalColor = uOneColor;
    } else if (uColorMode == 1) {
        finalColor = vColor;
    } else if (uColorMode == 2) {
        finalColor = texture(texture0, vTexCoord);
    }

    float light = max(abs(dot(vNormal * 1.4, vCamDir)), 0.3);
    finalColor.rgb *= light;

    fragColor = finalColor;
}