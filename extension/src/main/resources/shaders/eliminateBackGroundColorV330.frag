#version 330
//precision highp float;

uniform sampler2D texture0;
uniform float uScreenWidth;
uniform float uScreenHeight;
uniform vec3 uBackgroundColor;

in vec2 vTexCoord;

layout (location = 0) out vec4 fragColor;

bool isBackgroundColor(vec4 color) {
    float error = 0.01f;
    return color.r > uBackgroundColor.r - error && color.r < uBackgroundColor.r + error &&
           color.g > uBackgroundColor.g - error && color.g < uBackgroundColor.g + error &&
           color.b > uBackgroundColor.b - error && color.b < uBackgroundColor.b + error;
}

void main(void) {
    vec2 screenPos = vec2(gl_FragCoord.x / uScreenWidth, gl_FragCoord.y / uScreenHeight);

    // flip y
    screenPos.y = 1.0f - screenPos.y;

    vec4 color4 = texture2D(texture0, screenPos);

    if(isBackgroundColor(color4)) {
        // is background color, so change for nearest color
        float pixelSize_x = 1.0f/uScreenWidth;
        float pixelSize_y = 1.0f/uScreenHeight;

//        fragColor = vec4(0.0f, 0.25f, 0.9f, 1.0f);
//        return;

        for(int i=-10; i<=10; i++)
        {
            for(int j=-10; j<=10; j++)
            {
                vec2 texCoord = vec2(screenPos.x + pixelSize_x * float(i), screenPos.y + pixelSize_y * float(j));

                // check texture bounds
                if(texCoord.x < 0.0f || texCoord.x > 1.0f || texCoord.y < 0.0f || texCoord.y > 1.0f) {
                    continue;
                }
                vec4 color = texture2D(texture0, texCoord);
                if(!isBackgroundColor(color)) {
                    //fragColor = vec4(1.0f, 0.25f, 0.9f, 1.0f);
                    fragColor = color;
                    return;
                }
            }
        }

    }

    fragColor = color4;
}