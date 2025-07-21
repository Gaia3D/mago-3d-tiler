precision highp float;

uniform sampler2D texture0;

// colorMode = 0: oneColor, 1: vertexColor, 2: textureColor
uniform int uColorMode;
uniform vec4 uOneColor;
varying vec4 vColor;
varying vec2 vTexCoord;

void main(void) {
    vec4 finalColor = vec4(1.0, 1.0, 1.0, 1.0);
    if (uColorMode == 0) {
        finalColor = uOneColor;
    } else if (uColorMode == 1) {
        finalColor = vColor;
    } else if (uColorMode == 2) {
        finalColor = texture2D(texture0, vTexCoord);

        if (finalColor.r == 0.0 && finalColor.g == 0.0 && finalColor.b == 0.0)
        {
            //finalColor.r = vTexCoord.x - floor(vTexCoord.x);
            //finalColor.g = vTexCoord.y - floor(vTexCoord.y);
            ////finalColor.b = 0.95;
        }
        else
        {
            //finalColor.r = 0.25;
            //finalColor.g = 0.5;
            //finalColor.b = 1.0;

        }
    }

    //finalColor = vec4(vTexCoord.x, vTexCoord.y, finalColor.z * 0.01, 1.0);

    gl_FragColor = vec4(finalColor.rgb, 1.0);
}