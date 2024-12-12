#version 330
//precision highp float;

uniform sampler2D texture0;
uniform float uScreenWidth;
uniform float uScreenHeight;
uniform vec3 uBackgroundColor;

in vec2 vTexCoord;

layout (location = 0) out vec4 fragColor;

bool isBackgroundColor(vec4 color) {
    return color.r == uBackgroundColor.r && color.g == uBackgroundColor.g && color.b == uBackgroundColor.b;
}

void main(void) {
    vec2 screenPos = vec2(gl_FragCoord.x / uScreenWidth, gl_FragCoord.y / uScreenHeight);

    // flip y
    screenPos.y = 1.0 - screenPos.y;

    vec4 color4 = texture2D(texture0, screenPos);
    float pixelSize_x = 1.0/uScreenWidth;
    float pixelSize_y = 1.0/uScreenHeight;
    if(isBackgroundColor(color4)) {
        // is background color, so change for nearest color
        for(int i=-5; i<=5; i++)
        {
            for(int j=-5; j<=5; j++)
            {
                vec2 texCoord = vec2(screenPos.x + pixelSize_x * float(i), screenPos.y + pixelSize_y * float(j));
                vec4 color = texture2D(texture0, texCoord);
                if(!isBackgroundColor(color)) {
                    fragColor = color;
                    return;
                }
            }
        }

    }

    fragColor = color4;
}