#version 330
//precision highp float;

uniform sampler2D texture0;

// colorMode = 0: oneColor, 1: vertexColor, 2: textureColor
uniform int uColorMode;
uniform vec4 uOneColor;
in vec4 vColor;
in vec2 vTexCoord;

layout (location = 0) out vec4 fragColor;

void main(void) {
    vec4 finalColor = vec4(1.0, 1.0, 1.0, 1.0);
    if (uColorMode == 0) {
        finalColor = uOneColor;
    } else if (uColorMode == 1) {
        finalColor = vColor;
    } else if (uColorMode == 2) {
        vec2 uv = vec2(vTexCoord.x - floor(vTexCoord.x), vTexCoord.y - floor(vTexCoord.y));
        finalColor = texture(texture0, uv);
        if(finalColor.r == 0.0 && finalColor.g == 0.0 && finalColor.b == 0.0)
        {
            finalColor.r = vTexCoord.x - floor(vTexCoord.x);
            finalColor.g = vTexCoord.y - floor(vTexCoord.y);
            ////finalColor.b = 0.95;
        }
    }

    fragColor = vec4(finalColor.rgb, 1.0);
}